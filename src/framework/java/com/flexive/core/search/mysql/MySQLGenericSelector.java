/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
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
package com.flexive.core.search.mysql;

import com.flexive.core.Database;
import com.flexive.core.search.FieldSelector;
import com.flexive.core.search.PropertyEntry;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.FxSharedUtils;
import com.flexive.sqlParser.Property;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Map;
import java.util.HashMap;

/**
 * A generic MySQL selector
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class MySQLGenericSelector implements FieldSelector {
    private static final Log LOG = LogFactory.getLog(MySQLGenericSelector.class);

    private final Map<String, FxDataType> columns = new HashMap<String, FxDataType>();
    private final String tableName;
    private final String linksOn;

    protected MySQLGenericSelector(String tableName, String linksOn) {
        FxSharedUtils.checkParameterNull(tableName, "tableName");
        FxSharedUtils.checkParameterNull(linksOn, "linksOn");
        Connection con = null;
        Statement stmt = null;
        this.tableName = tableName;
        this.linksOn = linksOn;
        try {
            con = Database.getDbConnection();
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 0");
            ResultSetMetaData md = rs.getMetaData();
            for (int pos = 1; pos <= md.getColumnCount(); pos++) {
                String columnName = md.getColumnName(pos);
                FxDataType columnType;
                switch (md.getColumnType(pos)) {
                    case java.sql.Types.CHAR:
                    case java.sql.Types.VARCHAR:
                    case java.sql.Types.LONGVARCHAR:
                        columnType = FxDataType.String1024;
                        break;
                    case java.sql.Types.BOOLEAN:
                        columnType = FxDataType.Boolean;
                        break;
                    case java.sql.Types.TINYINT:
                    case java.sql.Types.SMALLINT:
                    case java.sql.Types.INTEGER:
                    case java.sql.Types.BIT:
                        columnType = FxDataType.Number;
                        break;
                    case java.sql.Types.DECIMAL:
                        columnType = FxDataType.Double;
                        break;
                    case java.sql.Types.FLOAT:
                        columnType = FxDataType.Float;
                        break;
                    case java.sql.Types.BIGINT:
                        if ("CREATED_AT".equalsIgnoreCase(columnName) || "MODIFIED_AT".equalsIgnoreCase(columnName))
                            columnType = FxDataType.DateTime;
                        else
                            columnType = FxDataType.LargeNumber;
                        break;
                    case java.sql.Types.DATE:
                        columnType = FxDataType.Date;
                        break;
                    case java.sql.Types.TIME:
                    case java.sql.Types.TIMESTAMP:
                        columnType = FxDataType.DateTime;
                        break;
                    default:
                        columnType = FxDataType.String1024;
                }
                columns.put(columnName.toUpperCase(), columnType);
            }

        } catch (Throwable t) {
            FxSqlSearchException ex = new FxSqlSearchException(LOG, "ex.sqlSearch.fieldSelector.initializeFailed",
                    tableName, t.getMessage());
            LOG.error(ex.getMessage(), ex);
            throw ex.asRuntimeException();
        } finally {
            Database.closeObjects(MySQLGenericSelector.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void apply(Property prop, PropertyEntry entry, StringBuffer statement) throws FxSqlSearchException {
        FxDataType type = columns.get(prop.getField().toUpperCase());
        if (type == null) {
            // This field does not exist
            throw new FxSqlSearchException(LOG, "ex.sqlSearch.query.undefinedField", prop.getField(), prop.getPropertyName(),
                    getAllowedFields());
        } else {
            statement.insert(0, "(select " + prop.getField() + " from " + tableName + " where " + linksOn + "=").append(")");
            entry.overrideDataType(type);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getAllowedFields() {
        return StringUtils.join(columns.keySet(), ',');
    }
}
