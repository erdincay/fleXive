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
package com.flexive.war.beans.admin.main;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.interfaces.SearchEngine;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.FxSQLSearchParams;
import com.flexive.war.beans.admin.content.ContentEditorBean;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import java.util.ArrayList;

/**
 * This Bean provides access the the sql search.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SqlSearchBean {

    private String query;
    private FxResultSet queryResult;
    private FxResultSetDataModel dataModel;
    private SearchEngine sqlSearchInterface;
    private long briefcaseAclId;
    private String briefcaseDescription;
    private String briefcaseName;
    private Boolean createBriefcase;
    private String cacheMode;
    final static String LAST_QUERY_CACHE = SqlSearchBean.class + "_LAST_QUERY";
    private String actionPk;


    public String getActionPk() {
        return actionPk;
    }

    public void setActionPk(String actionPk) {
        ContentEditorBean ceb = (ContentEditorBean) FxJsfUtils.getManagedBean("contentEditorBean");
        String pk[] = actionPk.split("\\.");
        ceb.setId(new Long(pk[0]));
        ceb.setVersion(new Integer(pk[1]));
        this.actionPk = actionPk;
    }

    public String getQuery() {
        if (query == null) {
            query = (String) FxJsfUtils.getSessionAttribute(LAST_QUERY_CACHE);
        }
        return query;
    }

    public SqlSearchBean() {
        sqlSearchInterface = EJBLookup.getSearchEngine();
        createBriefcase = false;
    }

    public void toggleCreateBriefcase(ActionEvent event) {
        // nothing to do, this is just the event that the briefcase mode was toggled
    }

    public FxResultSet getQueryResult() {
        return queryResult;
    }

    public void setQuery(String query) {
        if (query == null) {
            query = "";
        }
        this.query = query;
        FxJsfUtils.setSessionAttribute(LAST_QUERY_CACHE, query);
    }


    public String getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(String cacheMode) {
        this.cacheMode = cacheMode;
    }

    private FxSQLSearchParams.CACHE_MODE _getCacheMode() {
        try {
            if (cacheMode == null || cacheMode.trim().length() == 0) {
                return null;
            }
            return FxSQLSearchParams.CACHE_MODE.getById(Integer.valueOf(cacheMode));
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Getter for all available cache modes.
     *
     * @return all available cache modes
     */
    public ArrayList<SelectItem> getCacheModes() {
        ArrayList<SelectItem> result = new ArrayList<SelectItem>(FxSQLSearchParams.CACHE_MODE.values().length);
        for (FxSQLSearchParams.CACHE_MODE mode : FxSQLSearchParams.CACHE_MODE.values()) {
            result.add(new SelectItem(String.valueOf(mode.getId()), String.valueOf(mode)));
        }
        return result;
    }


    public Boolean getCreateBriefcase() {
        return createBriefcase;
    }

    public void setCreateBriefcase(Boolean createBriefcase) {
        this.createBriefcase = createBriefcase;
    }


    public long getBriefcaseAclId() {
        return briefcaseAclId;
    }

    public void setBriefcaseAclId(long briefcaseAclId) {
        this.briefcaseAclId = briefcaseAclId;
    }

    public String getBriefcaseDescription() {
        return briefcaseDescription;
    }

    public void setBriefcaseDescription(String briefcaseDescription) {
        this.briefcaseDescription = briefcaseDescription;
    }

    public String getBriefcaseName() {
        return briefcaseName;
    }

    public void setBriefcaseName(String briefcaseName) {
        this.briefcaseName = briefcaseName;
    }

    public FxResultSetDataModel getDataModel() {
        if (dataModel == null && getQueryResult() != null) {
            dataModel = new FxResultSetDataModel(getQueryResult());
        }
        return dataModel;
    }

    public String executeSearch() {
        FxSQLSearchParams sp = new FxSQLSearchParams();
        sp.setCacheMode(_getCacheMode());
        try {
            if (createBriefcase) {
                sp.saveResultInBriefcase(briefcaseName, briefcaseDescription, briefcaseAclId);
                queryResult = sqlSearchInterface.search(query, 0, Integer.MAX_VALUE, sp);
                new FxFacesMsgInfo("Briefcase.nfo.created", briefcaseName).addToContext();
            } else {
                queryResult = sqlSearchInterface.search(this.query, 0, null, sp);
            }
        } catch (Exception exc) {
            queryResult = null;
            new FxFacesMsgErr(exc).addToContext();
        } finally {
            createBriefcase = false;
        }
        return "sqlSearch";
    }
}
