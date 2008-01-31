/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.core.search;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.storage.ContentStorage;
import com.flexive.core.storage.StorageManager;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.structure.*;
import com.flexive.sqlParser.FxStatement;
import com.flexive.sqlParser.Property;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;

/**
 * A resolver for properties
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class PropertyResolver {
    public static enum Table {
        T_CONTENT,
        T_CONTENT_DATA
    }

    private static final Log LOG = LogFactory.getLog(PropertyResolver.class);
    private static Map<String, FxDataType> CONTENT_PROPS = null;

    private ContentStorage hierarchicalStorage;
    private FxEnvironment environment;
    private Map<String, Entry> cache = new HashMap<String, Entry>(50);
    private List<Entry> resultSetColumns;
    private int resultSetPos = 4;
    private int resultSetColumnCount = 0;

    public static class Entry {
        private int positionInResultSet = -1;
        private String readColumns[];
        private String filterColumn;
        private String table;
        private FxProperty property;
        private FxPropertyAssignment assignment;
        private Table tbl;
        private Type type;
        private FxDataType overrideDataType = null;
        private boolean multilanguage;

        public static enum Type {
            PROPERTY_REF,
            PK,
            PATH,
            NODE_POSITION
        }

        public Entry(Type type) throws FxSqlSearchException {
            switch (type) {
                case NODE_POSITION:
                    this.readColumns = new String[]{""};
                    this.table = DatabaseConst.TBL_CONTENT;
                    this.filterColumn = null;
                    this.property = null;
                    this.type = Type.NODE_POSITION;
                    this.tbl = Table.T_CONTENT;
                    this.multilanguage = false;
                    this.overrideDataType = FxDataType.Number;
                    break;
                case PATH:
                    this.readColumns = new String[]{""};
                    this.table = DatabaseConst.TBL_CONTENT;
                    this.filterColumn = null;
                    this.property = null;
                    this.type = Type.PATH;
                    this.tbl = Table.T_CONTENT;
                    this.multilanguage = false;
                    break;
                case PK:
                    this.readColumns = new String[]{"ID", "VERSION"};
                    this.table = DatabaseConst.TBL_CONTENT;
                    this.filterColumn = null;
                    this.property = null;
                    this.multilanguage = false;
                    this.type = Type.PK;
                    this.tbl = Table.T_CONTENT;
                    break;
                default:
                    throw new FxSqlSearchException("TODO: virtual"); // TODO
            }
        }

        public Entry(FxProperty property, FxPropertyAssignment assignment, String readColumns[], String filterColumn, String table) throws FxSqlSearchException {
            this.readColumns = readColumns;
            this.table = table;
            this.multilanguage = property.isMultiLang();
            this.filterColumn = filterColumn;
            this.assignment = assignment;
            this.property = property;
            this.type = Type.PROPERTY_REF;
            if (table.equalsIgnoreCase(DatabaseConst.TBL_CONTENT)) {
                this.tbl = Table.T_CONTENT;
            } else if (table.equalsIgnoreCase(DatabaseConst.TBL_CONTENT_DATA)) {
                this.tbl = Table.T_CONTENT_DATA;
            } else {
                throw new FxSqlSearchException(LOG, "ex.sqlSearch.err.unknownPropertyTable", property, table);
            }
        }


        public boolean isMultilanguage() {
            return multilanguage;
        }

        public Type getType() {
            return type;
        }


        public void overrideDataType(FxDataType type) {
            this.overrideDataType = type;
        }

        public FxDataType getDataType() {
            return overrideDataType == null ? property.getDataType() : overrideDataType;
        }

        public int getPositionInResultSet() {
            return positionInResultSet;
        }

        void setPositionInResultSet(int positionInResultSet) {
            this.positionInResultSet = positionInResultSet;
        }

        public Table getTableType() {
            return tbl;
        }

        /**
         * The column to read the result from.
         *
         * @return the column to read the result from
         */
        public String[] getReadColumns() {
            return readColumns;
        }

        public String getFilterColumn() {
            return filterColumn;
        }

        public String getTable() {
            return table;
        }

        public FxProperty getProperty() {
            return property;
        }

        /**
         * If this entry refers to a property, is it an assignment?
         *
         * @return is this an assignment=
         */
        public boolean isAssignment() {
            return assignment != null;
        }

        /**
         * Getter for the FxPropertyAssignment
         *
         * @return assignment
         */
        public FxPropertyAssignment getAssignment() {
            return assignment;
        }
    }

    protected PropertyResolver() throws FxSqlSearchException {
        try {
            this.environment = CacheAdmin.getEnvironment();
            this.hierarchicalStorage = StorageManager.getContentStorage(TypeStorageMode.Hierarchical);
            this.resultSetColumns = new ArrayList<Entry>(50);
        } catch (FxNotFoundException e) {
            throw new FxSqlSearchException(LOG, e);
        } catch (Exception e) {
            throw new FxSqlSearchException(LOG, "Init error:" + e.getMessage());
        }
        init();
    }

    public void addResultSetColumn(Entry e) {
        // Store actuall resultset position
        e.setPositionInResultSet(resultSetPos);
        this.resultSetColumns.add(e);
        // compute next position to use
        resultSetPos += e.getReadColumns().length;
        if (e.getTableType() == Table.T_CONTENT_DATA) {
            // also select XPATH 
            resultSetPos += 1;
        }
        // compue total result set columns
        resultSetColumnCount += e.getReadColumns().length;
    }


    public int getResultSetColumnCount() {
        return resultSetColumnCount;
    }

    public List<Entry> getResultSetColumns() {
        return resultSetColumns;
    }

    public Entry get(FxStatement fx_stmt, Property prop) throws FxSqlSearchException {
        final com.flexive.sqlParser.Table tbl = fx_stmt.getTableByAlias(prop.getTableAlias());
        switch (tbl.getType()) {
            case CONTENT:
                return getByName(fx_stmt, prop);
            default:
                throw new FxSqlSearchException(LOG, "ex.sqlSearch.table.typeNotSupported", tbl.getType());
        }
    }

    private Entry getByName(final FxStatement fx_stmt, final Property _prop) throws FxSqlSearchException {
        final String key = fx_stmt.getIgnoreCase() + "_" + _prop.getPropertyName() +
                (_prop.hasField() ? "_" + _prop.getField() : "");
        Entry entry = cache.get(key);
        if (entry == null) {
            if (_prop.getPropertyName().equalsIgnoreCase("@NODE_POSITION")) {
                entry = new Entry(Entry.Type.NODE_POSITION);
                cache.put(key, entry);
            } else if (_prop.getPropertyName().equalsIgnoreCase("@PATH")) {
                entry = new Entry(Entry.Type.PATH);
                cache.put(key, entry);
            } else if (_prop.getPropertyName().equalsIgnoreCase("@PK")) {
                entry = new Entry(Entry.Type.PK);
                cache.put(key, entry);
            } else {
                try {
                    FxProperty prop;
                    FxPropertyAssignment as;

                    if (_prop.isAssignment()) {
                        try {
                            if (_prop.getPropertyName().indexOf('/') > 0) {
                                //XPath
                                as = (FxPropertyAssignment) environment.getAssignment(_prop.getPropertyName());
                            } else {
                                //#<id>
                                as = (FxPropertyAssignment) environment.getAssignment(Long.valueOf(_prop.getPropertyName().substring(1)));
                            }
                        } catch (ClassCastException ce) {
                            throw new FxSqlSearchException(LOG, ce, "ex.sqlSearch.query.unknownAssignment",
                                    _prop.getPropertyName());
                        } catch (Throwable t) {
                            boolean notFound = ((FxRuntimeException) t).getConverted() instanceof FxNotFoundException;
                            if (notFound) {
                                throw new FxSqlSearchException(LOG, t, "ex.sqlSearch.query.unknownAssignment",
                                        _prop.getPropertyName());
                            } else {
                                throw new FxSqlSearchException(LOG, t, "ex.sqlSearch.query.failedToResolveAssignment",
                                        _prop.getPropertyName(), t.getMessage());
                            }
                        }
                        prop = as.getProperty();
                    } else {
                        as = null;
                        prop = environment.getProperty(_prop.getPropertyName());
                    }


                    final String dbColumns[] = hierarchicalStorage.getColumns(prop);
                    final String tbl = hierarchicalStorage.getTableName(prop);
                    String fColumn = fx_stmt.getIgnoreCase()
                            ? hierarchicalStorage.getUppercaseColumn(prop)
                            : dbColumns[0];
                    if (fColumn == null) {
                        fColumn = dbColumns == null ? null : dbColumns[0];
                    }
                    if (fColumn == null) {
                        throw new FxSqlSearchException(LOG, "ex.sqlSearch.init.propertyDoesNotHaveColumnMapping",
                                _prop.getPropertyName());
                    }
                    entry = new Entry(prop, as, dbColumns, fColumn, tbl);
                    cache.put(key, entry);
                } catch (FxSqlSearchException exc) {
                    throw exc;
                } catch (Throwable exc) {
                    throw new FxSqlSearchException(LOG, exc, "ex.sqlSearch.init.propertyLookupFailed",
                            _prop.getPropertyName(), exc.getMessage());
                }
            }
        }
        return entry;
    }

    /**
     * Initializes the column informations.
     *
     * @throws FxSqlSearchException if the init fails
     */
    protected final synchronized void init() throws FxSqlSearchException {
        if (CONTENT_PROPS != null) {
            return;
        }
        Connection con = null;
        Statement stmt = null;
        try {
            con = Database.getDbConnection();
            stmt = con.createStatement();
            final ResultSet rs = stmt.executeQuery("select * from " + DatabaseConst.TBL_CONTENT + " where 1=2");
            final ResultSetMetaData rsmd = rs.getMetaData();
            CONTENT_PROPS = new Hashtable<String, FxDataType>(10);
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                final String columnName = rsmd.getColumnName(i + 1).trim().toUpperCase();
                final int type = rsmd.getColumnType(i + 1);
                final FxDataType dt;
                switch (type) {
                    case java.sql.Types.BIGINT:
                        if( columnName.endsWith("_ED_AT")) { //CREATED_AT, MODIFIED_AT
                            dt = FxDataType.DateTime;
                            break;
                        }
                    case java.sql.Types.NUMERIC:
                        dt = FxDataType.LargeNumber;
                        break;
                    case java.sql.Types.INTEGER:
                    case java.sql.Types.SMALLINT:
                    case java.sql.Types.TINYINT:
                        dt = FxDataType.Number;
                        break;
                    case java.sql.Types.TIMESTAMP:
                        dt = FxDataType.DateTime;
                        break;
                    case java.sql.Types.VARCHAR:
                        dt = FxDataType.String1024;
                        break;
                    case java.sql.Types.BIT:
                    case java.sql.Types.BOOLEAN:
                        dt = FxDataType.Boolean;
                        break;
                    default:
                        throw new FxSqlSearchException(LOG, "ex.sqlSearch.init.unknowColumnType", columnName,
                                DatabaseConst.TBL_CONTENT, type);
                }
                CONTENT_PROPS.put(columnName, dt);
            }
        } catch (FxSqlSearchException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new FxSqlSearchException(LOG, "ex.sqlSearch.init.failedToReadTableMetadata", DatabaseConst.TBL_CONTENT);
        } finally {
            Database.closeObjects(PropertyResolver.class, con, stmt);
        }
    }
}
