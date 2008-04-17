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
package com.flexive.ejb.beans.search;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.search.SqlSearch;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.ParameterDataBean;
import com.flexive.shared.configuration.ParameterPathBean;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.configuration.parameters.ParameterFactory;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.search.*;
import com.flexive.shared.search.query.QueryRootNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;

@javax.ejb.TransactionManagement(javax.ejb.TransactionManagementType.CONTAINER)
@Stateless(name = "SearchEngine")
public class SearchEngineBean implements SearchEngine, SearchEngineLocal {
    private static final Log LOG = LogFactory.getLog(SearchEngineBean.class);
    private static final String DEFAULT_QUERY_NAME = SearchEngineBean.class.getName() + ".DEFAULTQUERY";


    @Resource
    javax.ejb.SessionContext ctx;
    @EJB
    ResultPreferencesEngineLocal resultPreferences;
    @EJB
    private SequencerEngineLocal seq;
    @EJB
    private BriefcaseEngineLocal briefcase;
    @EJB
    private ConfigurationEngineLocal configuration;
    @EJB
    private DivisionConfigurationEngineLocal divisionConfiguration;
    @EJB
    private TreeEngineLocal treeEngine;

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxResultSet search(String query, final int startIndex, final Integer fetchRows, FxSQLSearchParams params)
            throws FxApplicationException {
        return search(query, startIndex, fetchRows, params, AdminResultLocations.DEFAULT, ResultViewType.LIST);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxResultSet search(String query, int startIndex, Integer fetchRows, FxSQLSearchParams params, ResultLocation location, ResultViewType viewType) throws FxApplicationException {
        try {
            if (params == null) {
                params = new FxSQLSearchParams();
            }
            return new SqlSearch(seq, briefcase, treeEngine, query, startIndex, fetchRows, 
                    params, resultPreferences, location, viewType).executeQuery();
        } catch (FxSqlSearchException exc) {
            ctx.setRollbackOnly();
            throw exc;
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long getLastContentChange(boolean live) {
        Connection con = null;
        Statement stmt = null;
        try {
            con = Database.getDbConnection();
            String contentFilter = live ? " where ISLIVE_VER=true " : "";
            String sSql = "select max(modified_at) from\n" +
                    "(select\n" +
                    "(select max(modified_at) from " + DatabaseConst.TBL_CONTENT + contentFilter + ") modified_at\n" +
                    (live ? "\nunion\n(select max(modified_at) from " + DatabaseConst.TBL_TREE + "_LIVE)\n" : "") +
                    "\nunion\n(select max(modified_at) from " + DatabaseConst.TBL_TREE + ")\n" +
                    ") changes";
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sSql);
            rs.next();
            return rs.getLong(1);
        } catch (Exception e) {
            throw new FxLoadException(LOG, e, "ex.sqlSearch.lastContentChange", e).asRuntimeException();
        } finally {
            Database.closeObjects(this.getClass(), con, stmt);
        }
    }


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public QueryRootNode load(ResultLocation location, String name) throws FxApplicationException {
        return configuration.get(getConfigurationParameter(location), name);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public QueryRootNode loadDefault(ResultLocation location) throws FxApplicationException {
        try {
            return load(location, DEFAULT_QUERY_NAME);
        } catch (FxNotFoundException e) {
            return new QueryRootNode(QueryRootNode.Type.CONTENTSEARCH, location);
        } catch (FxApplicationException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public QueryRootNode loadSystemDefault(ResultLocation location) throws FxApplicationException {
        try {
            return divisionConfiguration.get(getConfigurationParameter(location), DEFAULT_QUERY_NAME);
        } catch (FxNotFoundException e) {
            return new QueryRootNode(QueryRootNode.Type.CONTENTSEARCH, location);
        } catch (FxApplicationException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Collection<String> loadNames(ResultLocation location) throws FxApplicationException {
        return configuration.getKeys(getConfigurationParameter(location));
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void save(QueryRootNode query) throws FxApplicationException {
        save(configuration, query, query.getName());
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void saveDefault(QueryRootNode query) throws FxApplicationException {
        save(configuration, query, DEFAULT_QUERY_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void saveSystemDefault(QueryRootNode query) throws FxApplicationException {
        if (!FxContext.get().getTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException(LOG, "ex.searchQuery.systemDefault.noAccess");
        }
        save(EJBLookup.getDivisionConfigurationEngine(), query, DEFAULT_QUERY_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(ResultLocation location, String name) throws FxApplicationException {
        configuration.remove(getConfigurationParameter(location), name);
    }

    private void save(GenericConfigurationEngine configuration, QueryRootNode query, String key) throws FxUpdateException {
        try {
            configuration.put(getConfigurationParameter(query.getLocation()), key, query);
        } catch (Exception e) {
            throw new FxUpdateException(LOG, "ex.searchQuery.toXml", e, e.getMessage());
        }
    }

    /**
     * Return the appropriate string parameter for the given query type.
     *
     * @param location the result location
     * @return the appropriate string parameter for the given query type.
     */
    private Parameter<QueryRootNode> getConfigurationParameter(ResultLocation location) {
        final Parameter<QueryRootNode> parameter = SystemParameters.USER_QUERIES_CONTENT;
        // append the location name to the parameter path
        final ParameterPathBean locationPath = new ParameterPathBean(parameter.getPath().getValue() + "/" + location.getName(), parameter.getScope());
        return ParameterFactory.newInstance(QueryRootNode.class, new ParameterDataBean<QueryRootNode>(locationPath, null));
    }

}
