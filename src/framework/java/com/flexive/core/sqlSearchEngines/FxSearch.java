/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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
package com.flexive.core.sqlSearchEngines;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.sqlSearchEngines.mysql.MySQLDataFilter;
import com.flexive.core.sqlSearchEngines.mysql.MySQLDataSelector;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.configuration.DBVendor;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.interfaces.BriefcaseEngine;
import com.flexive.shared.interfaces.ResultPreferencesEngine;
import com.flexive.shared.interfaces.SequencerEngine;
import com.flexive.shared.search.*;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxType;
import com.flexive.sqlParser.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * The main search engine class
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxSearch {

    private int startIndex;
    private int fetchRows;
    private String query;
    private FxStatement fx_stmt;
    private PropertyResolver pr;
    private int parserExecutionTime = -1;
    private long searchId = -1;
    private String cacheTbl;
    private FxSQLSearchParams params;
    private boolean hasWildcard = false;
    private FxEnvironment environment;
    private ResultPreferencesEngine conf;
    private FxLanguage language;
    private ResultLocation location;
    private ResultViewType viewType;
    private FxType typeFilter = null;

    private SequencerEngine seq;
    private BriefcaseEngine briefcase;

    /**
     * Ctor
     *
     * @param seq          reference to the sequencer
     * @param briefcase    reference to the briefcase engine
     * @param query        the query to execute
     * @param startIndex   the start index (0 based)
     * @param maxFetchRows the number of rows to return with the resultset, or null to fetch all rows
     * @param params       all aditional search parameters
     * @param conf         the result set configuration
     * @param location     the location that started the search
     * @param viewType     the view type
     * @throws com.flexive.shared.exceptions.FxSqlSearchException
     *          if the search failed
     */
    public FxSearch(SequencerEngine seq, BriefcaseEngine briefcase, String query, int startIndex, Integer maxFetchRows, FxSQLSearchParams params,
                    ResultPreferencesEngine conf, ResultLocation location, ResultViewType viewType) throws FxSqlSearchException {
        // Init
        this.seq = seq;
        this.briefcase = briefcase;
        this.conf = conf;
        this.environment = CacheAdmin.getEnvironment();
        this.params = params;
        this.startIndex = startIndex;
        this.fetchRows = maxFetchRows == null ? Integer.MAX_VALUE : maxFetchRows;
        this.query = query;
        if (this.query == null || this.query.trim().length() == 0) {
            this.query = null;
            return;
        }
        final UserTicket ticket = FxContext.get().getTicket();
        this.language = ticket.getLanguage();
        this.location = location;
        this.viewType = viewType;

        // Parameter checks
        if (this.startIndex < 0) {
            throw new FxSqlSearchException("ex.sqlSearch.parameter.invalidStartIndex", startIndex);
        }

        if (maxFetchRows != null) {
            if (maxFetchRows < 1 && maxFetchRows != -1) {
                throw new FxSqlSearchException("ex.sqlSearch.parameter.fetchRows", maxFetchRows);
            }
        }

        // Parse the statement
        try {
            long time = java.lang.System.currentTimeMillis();
            fx_stmt = FxStatement.parseSql(query);
            this.hasWildcard = this.hasWildcard();
            this.parserExecutionTime = (int) (java.lang.System.currentTimeMillis() - time);
        } catch (SqlParserException pe) {
            // Catche the parse exception and convert it to an localized one
            throw new FxSqlSearchException(pe);
        } catch (Throwable t) {
            throw new FxSqlSearchException(t, "ex.sqlSearch.parser.error", t.getMessage());
        }

        // Process content type filter
        if (fx_stmt.hasContentTypeFilter()) {
            String type = fx_stmt.getContentTypeFilter();
            try {
                try {
                    long typeId = Long.parseLong(type);
                    typeFilter = environment.getType(typeId);
                } catch (NumberFormatException nfe) {
                    typeFilter = environment.getType(type);
                }
            } catch (Throwable t) {
                throw new FxSqlSearchException(t, "ex.sqlSearch.filter.invalidContentTypeFilterValue", type);
            }
        }
    }

    /**
     * Returns the content type filter, or null if the filter is not set.
     *
     * @return the content type filter, or null
     */
    public FxType getTypeFilter() {
        return typeFilter;
    }

    /**
     * Returns the language of this search.
     *
     * @return the language of this search
     */
    public FxLanguage getLanguage() {
        return language;
    }

    /**
     * Executes the search.
     *
     * @return the resultset
     * @throws FxSqlSearchException if the search failed
     */
    public FxResultSet executeQuery() throws FxSqlSearchException {

        // Anything to do?
        if (this.query == null) {
            return new FxResultSetImpl(location, viewType);
        }

        // Check if the statement will produce any resultset at all
        if (fx_stmt.getType() == FxStatement.TYPE.EMPTY) {
            return new FxResultSetImpl(fx_stmt, null, this.parserExecutionTime, 0,
                    startIndex, fetchRows, location, viewType, null, -1, -1);
        }

        // Execute select
        Statement stmt = null;
        Connection con = null;
        FxResultSetImpl fx_result = null;
        final long startTime = java.lang.System.currentTimeMillis();
        DataSelector ds = null;
        DataFilter df = null;
        try {

            // Init
            switch (params.getCacheMode()) {
                case ON:
                    cacheTbl = DatabaseConst.TBL_SEARCHCACHE_PERM;
                    searchId = seq.getId(SequencerEngine.System.SEARCHCACHE_PERM);
                    break;
                case OFF:
                case READ_ONLY:
                    cacheTbl = DatabaseConst.TBL_SEARCHCACHE_MEMORY;
                    searchId = seq.getId(SequencerEngine.System.SEARCHCACHE_MEMORY);
                    break;
                default:
                    // Can never happen
                    cacheTbl = null;
            }

            pr = new PropertyResolver();
            con = Database.getDbConnection();

            // TODO: remove the statement below when FxSearch works with XA transactions 
            stmt = con.createStatement();
            stmt.executeQuery("SET AUTOCOMMIT=1;");
            stmt.close();

            // Find all matching objects
            df = getDataFilter(con);
            df.build();

            // Wildcard handling depending on the found entries
            if (this.hasWildcard) {
                replaceWildcard(df);
            }
            if (fx_stmt.getOrderByValues().isEmpty()) {
                // add user-defined order by
                final ResultPreferences resultPreferences = getResultPreferences(df);
                for (ResultOrderByInfo column : resultPreferences.getOrderByColumns()) {
                    fx_stmt.addOrderByValue(new OrderByValue(column.getColumnName(),
                            column.getDirection().equals(SortDirection.ASCENDING)));
                }

            }

            // If specified create a briefcase with the found data
            long createdBriefcaseId = -1;
            if (params.getWillCreateBriefcase()) {
                createdBriefcaseId = copyToBriefcase(con);
            }

            // Select all desired rows for the resultset
            ds = getDataSelector();
            String select = ds.build(con);

            stmt = con.createStatement();
            stmt.executeUpdate("set @rownr=1;");
            stmt.close();

            stmt = con.createStatement();
            stmt.setQueryTimeout(params.getQueryTimeout());

            // Fetch the result
            ResultSet rs = stmt.executeQuery(select);
            int dbSearchTime = (int) (java.lang.System.currentTimeMillis() - startTime);
            fx_result = new FxResultSetImpl(fx_stmt, pr, this.parserExecutionTime, dbSearchTime, startIndex,
                    fetchRows, location, viewType, df.getContentTypes(),
                    getTypeFilter() != null ? getTypeFilter().getId() : -1,
                    createdBriefcaseId);
            fx_result.setTotalRowCount(df.getFoundEntries());
            fx_result.setTruncated(df.isTruncated());

            final long fetchStart = java.lang.System.currentTimeMillis();
            FxResultReader reader = new FxResultReader(rs, language);
            while (rs.next()) {
                Object[] row = new Object[pr.getResultSetColumns().size()];
                int i = 0;
                for (PropertyResolver.Entry entry : pr.getResultSetColumns()) {
                    //Object val =getValue(rs,entry);
                    Object val = reader.getValue(entry);
                    row[i] = val;
                    i++;
                }
                fx_result.addRow(row);
                if (fx_result.getRowCount() == fetchRows) {
                    // Maximum fetch size reached, stop
                    break;
                }
            }
            int timeSpent = (int) (java.lang.System.currentTimeMillis() - fetchStart);
            fx_result.setFetchTime(timeSpent);
            return fx_result;
        } catch (FxSqlSearchException exc) {
            throw exc;
        } catch (SQLException exc) {
            java.lang.System.err.println(exc.getMessage());
            throw new FxSqlSearchException(exc, "ex.sqlSearch.filter.sql.Exception");
        } catch (Throwable t) {
            throw new FxSqlSearchException(t, "ex.sqlSearch.failed", t.getMessage());
        } finally {
            try {
                if (ds != null) ds.cleanup(con);
            } catch (Throwable t) {/*ignore*/}
            try {
                if (df != null) df.cleanup();
            } catch (Throwable t) {/*ignore*/}
            Database.closeObjects(FxSearch.class, con, stmt);
            if (fx_result != null) {
                int timeSpent = (int) (java.lang.System.currentTimeMillis() - startTime);
                fx_result.setTotalTime(timeSpent);
            }
        }
    }

    private long copyToBriefcase(Connection con) throws FxSqlSearchException {
        BriefcaseCreationData bcd = params.getBriefcaseCreationData();
        Statement stmt = null;
        try {
            // Create the briefcase
            long bid = briefcase.create(bcd.getName(), bcd.getDescription(), bcd.getAclId());
            stmt = con.createStatement();
            stmt.addBatch("SET @pos=0;");
            String sSql = "insert into " + DatabaseConst.TBL_BRIEFCASE_DATA +
                    "(BRIEFCASE_ID,POS,ID,AMOUNT) " +
                    "(select " + bid + ",@pos:=@pos+1 pos,data.id,1 from " + getCacheTable() + " data " +
                    "where SEARCH_ID=" + getSearchId() + ")";
            stmt.addBatch(sSql);
            stmt.executeBatch();
            return bid;
        } catch (Throwable t) {
            throw new FxSqlSearchException(t, "ex.sqlSearch.err.failedToBuildBriefcase", bcd.getName());
        } finally {
            Database.closeObjects(MySQLDataSelector.class, null, stmt);
        }
    }

    /**
     * Get the DataSelector for the sql searchengine based on the used DB
     *
     * @return DataSelector the data selecttor implementation
     * @throws FxSqlSearchException if the function fails
     */
    public DataSelector getDataSelector() throws FxSqlSearchException {
        DBVendor vendor;
        try {
            vendor = Database.getDivisionData().getDbVendor();
            switch (vendor) {
                case MySQL:
                    return new MySQLDataSelector(this);
                default:
                    throw new FxSqlSearchException("ex.db.selector.undefined", vendor);
            }
        } catch (SQLException e) {
            throw new FxSqlSearchException("ex.db.vendor.notFound", FxContext.get().getDivisionId());
        }
    }

    /**
     * Get the DataSelector for the sql searchengine based on the used DB
     *
     * @param con the connection to use
     * @return DataSelector the data selecttor implementation
     * @throws FxSqlSearchException if the function fails
     */
    public DataFilter getDataFilter(Connection con) throws FxSqlSearchException {
        DBVendor vendor;
        try {
            vendor = Database.getDivisionData().getDbVendor();
            switch (vendor) {
                case MySQL:
                    return new MySQLDataFilter(con, this);
                default:
                    throw new FxSqlSearchException("ex.db.filter.undefined", vendor);
            }
        } catch (SQLException e) {
            throw new FxSqlSearchException("ex.db.vendor.notFound", FxContext.get().getDivisionId());
        }
    }


    public int getStartIndex() {
        return startIndex;
    }

    public int getFetchRows() {
        return fetchRows;
    }

    public FxStatement getFxStatement() {
        return fx_stmt;
    }

    public PropertyResolver getPropertyResolver() {
        return pr;
    }

    public FxSQLSearchParams getParams() {
        return this.params;
    }

    public String getCacheTable() {
        return cacheTbl;
    }

    /**
     * Returns the unique id of this search.
     *
     * @return the unique id of this search
     */
    public long getSearchId() {
        return searchId;
    }

    private boolean hasWildcard() throws FxSqlSearchException {
        // Find out if we have to deal with a wildcard
        boolean hasWildcard = false;
        for (SelectedValue value : fx_stmt.getSelectedValues()) {
            if (value.getValue() instanceof Property) {
                Property prop = ((Property) value.getValue());
                if (prop.isWildcard()) {
                    if (hasWildcard) {
                        // Only one wildcard may be used per statement
                        throw new FxSqlSearchException("ex.sqlSearch.onlyOneWildcardPermitted");
                    }
                    hasWildcard = true;
                }
            }
        }
        return hasWildcard;
    }

    /**
     * Replaces the wildcard in the fx_statement by the defined properties.
     *
     * @param df the datafilter
     * @throws FxSqlSearchException if the function fails
     */
    private void replaceWildcard(DataFilter df) throws FxSqlSearchException {

        try {
            ResultPreferences prefs = getResultPreferences(df);
            ArrayList<SelectedValue> selValues = new ArrayList<SelectedValue>(
                    (fx_stmt.getSelectedValues().size() - 1) + prefs.getSelectedColumns().size());
            for (SelectedValue _value : fx_stmt.getSelectedValues()) {
                if (_value.getValue() instanceof Property && ((Property) _value.getValue()).isWildcard()) {
                    Property wildcard = (Property) _value.getValue();
                    // Wildcard, replace it with the correct values
                    for (ResultColumnInfo nfo : prefs.getSelectedColumns()) {
                        Property newProp = new Property(wildcard.getTableAlias(), nfo.getPropertyName(), nfo.getSuffix());
                        SelectedValue newSel = new SelectedValue(newProp, nfo.getLabel(this.environment));
                        selValues.add(newSel);
                    }
                } else {
                    // Normal property, use it as is
                    selValues.add(_value);
                }
            }
            fx_stmt.setSelectedValues(selValues);
        } catch (Throwable t) {
            throw new FxSqlSearchException(t, "ex.sqlSearch.wildcardProcessingFailed");
        }
    }

    private ResultPreferences getResultPreferences(DataFilter df) throws FxApplicationException {
        long cType = getTypeFilter() != null ?
                // Type filter: only one type is contained in the search, use it
                getTypeFilter().getId() :
                // No Type filter: see if we got only one type in the result, or use the default for all types
                df.getContentTypes().size() == 1 ? df.getContentTypes().get(0).getContentTypeId() : -1;

        //ArrayList<SelectedValue> selValues = new ArrayList<SelectedValue>(fx_stmt.getSelectedValues().size()+25);
        return conf.load(cType, viewType, location);
    }
}
