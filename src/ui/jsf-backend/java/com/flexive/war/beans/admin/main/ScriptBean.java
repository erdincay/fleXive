/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
import com.flexive.faces.beans.MessageBean;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.ScriptingEngine;
import com.flexive.shared.scripting.*;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSF scripting bean
 *
 * @author Johannes Wernig-Pichler (johannes.wernig-pichler@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ScriptBean {

    private long id = -1;
    private String name;
    private String desc;
    private String code;
    private boolean active;
    private FxScriptInfoEdit sinfo;
    private ScriptingEngine scriptInterface;
    private FxScriptMapping mapping;
    private Map<Long, String> typeMappingNames;
    private Map<Long, String> assignmentMappingNames;
    private FxScriptScope selectedScope = null;
    private long selectedScriptEventId = -1;
    private FxScriptRunInfo currentRunInfo;

    private static final String ID_CACHE_KEY = ScriptBean.class + "_id";


    // constructor
    public ScriptBean() {
        this.scriptInterface = EJBLookup.getScriptingEngine();
        this.sinfo = new FxScriptInfo().asEditable();
    }

    public FxScriptScope getSelectedScope() {
        if (selectedScope == null)
            selectedScope = FxScriptScope.All;
        return selectedScope;
    }

    public List<SelectItem> getEventsForScope() {
        //if the selected event is not in the list of events for the currently selected scope
        //reset the selected event id to default -1==all events
        boolean selectedEventFound = false;
        FxScriptScope scope = getSelectedScope();
        List<SelectItem> eventsForScope = new ArrayList<SelectItem>();
        eventsForScope.add(new SelectItem(-1, MessageBean.getInstance().getMessage("Script.selectItem.allEvents")));
        for (FxScriptEvent e : FxScriptEvent.values()) {
            if (e.getScope().compareTo(scope) == 0 || scope.compareTo(FxScriptScope.All) == 0) {
                eventsForScope.add(new SelectItem(e.getId(), e.getName()));
                if (getSelectedScriptEventId() == e.getId())
                    selectedEventFound = true;
            }
        }

        if (!selectedEventFound)
            this.selectedScriptEventId = -1;

        return eventsForScope;
    }

    public void setSelectedScope(FxScriptScope selectedScope) {
        this.selectedScope = selectedScope;
    }

    public long getSelectedScriptEventId() {
        return selectedScriptEventId;
    }

    public void setSelectedScriptEventId(long selectedScriptEventId) {
        this.selectedScriptEventId = selectedScriptEventId;
    }

    public List<FxScriptInfo> getScriptsForEvent() {
        long eventId = getSelectedScriptEventId();
        List<FxScriptInfo> scriptsForEvent = new ArrayList<FxScriptInfo>();
        for (FxScriptInfo s : CacheAdmin.getFilteredEnvironment().getScripts())
            if ((eventId == -1 && getSelectedScope().compareTo(FxScriptScope.All) == 0)
                    || (eventId == -1 && s.getEvent().getScope().compareTo(getSelectedScope()) == 0)
                    || (s.getEvent().getId() == eventId && getSelectedScope().compareTo(FxScriptScope.All) == 0)
                    || (s.getEvent().getId() == eventId && s.getEvent().getScope().compareTo(getSelectedScope()) == 0))
                scriptsForEvent.add(s);
        return scriptsForEvent;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Loads the script specified by the parameter id.
     *
     * @return the next page to render
     */

    public String editScript() {
        ensureScriptIdSet();
        setSinfo(CacheAdmin.getEnvironment().getScript(id).asEditable());
        try {
            this.mapping = scriptInterface.loadScriptMapping(id);
            this.typeMappingNames = new HashMap<Long, String>();
            // we need the type names for the user interface and the type ids for the links
            for (FxScriptMappingEntry entry : this.mapping.getMappedTypes()) {
                this.typeMappingNames.put(entry.getId(), CacheAdmin.getEnvironment().getType(entry.getId()).getName());
                for (long id : entry.getDerivedIds())
                    this.typeMappingNames.put(id, CacheAdmin.getEnvironment().getType(id).getName());
            }
            this.assignmentMappingNames = new HashMap<Long, String>();
            for (FxScriptMappingEntry entry : this.mapping.getMappedAssignments()) {
                this.assignmentMappingNames.put(entry.getId(), CacheAdmin.getEnvironment().getAssignment(entry.getId()).getXPath());
                for (long id : entry.getDerivedIds())
                    this.assignmentMappingNames.put(id, CacheAdmin.getEnvironment().getAssignment(id).getXPath());
            }
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        } catch (SQLException e) {
            new FxFacesMsgErr("Script.err.loadMap").addToContext();
        }
        setName(sinfo.getName());
        setDesc(sinfo.getDescription());
        setCode(sinfo.getCode());
        setActive(sinfo.isActive());
        return "scriptEdit";
    }

    public FxScriptInfo getSinfo() {
        return sinfo;
    }

    public void setSinfo(FxScriptInfoEdit sinfo) {
        this.sinfo = sinfo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private void ensureScriptIdSet() {
        if (this.id <= 0) {
            this.id = (Long) FxJsfUtils.getSessionAttribute(ID_CACHE_KEY);
        }
    }

    /**
     * Deletes a script, with the id specified by id.
     *
     * @return the next pageto render
     */
    public String deleteScript() {

        final UserTicket ticket = FxContext.getUserTicket();
        if (!ticket.isInRole(Role.ScriptManagement)) {
            new FxFacesMsgErr("Script.err.deletePerm").addToContext();
            return "scriptOverview";
        }

        ensureScriptIdSet();
        try {
            scriptInterface.remove(id);
            // display updated script list  -->handled via a4j now
            //updateScriptList();  -->handled via a4j now
            new FxFacesMsgInfo("Script.nfo.deleted").addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return "scriptOverview";
    }

    /**
     * Executes a script, with the id specified by id.
     *
     * @return the next page to render
     */
    public String runScript() {

        final UserTicket ticket = FxContext.getUserTicket();
        if (!ticket.isInRole(Role.ScriptExecution)) {
            new FxFacesMsgErr("Script.err.runPerm").addToContext();
            return "scriptOverview";
        }

        ensureScriptIdSet();
        try {
            scriptInterface.runScript(id);
            new FxFacesMsgInfo("Script.nfo.executed", CacheAdmin.getEnvironment().getScript(id).getName()).addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        } catch (Throwable t) {
            new FxFacesMsgErr("Script.err.run").addToContext();
        }
        return "scriptOverview";
    }

    /**
     * Creates a new script from the beans data.
     *
     * @return the next jsf page to render
     */
    public String createScript() {
        final UserTicket ticket = FxContext.getUserTicket();
        if (!ticket.isInRole(Role.ScriptManagement)) {
            new FxFacesMsgErr("Script.err.createPerm").addToContext();
            return "scriptOverview";
        }

        if (sinfo.getName().length() < 1 || sinfo.getEvent() == null) {
            new FxFacesMsgErr("Script.err.createMiss").addToContext();
            return "scriptCreate";
        }

        try {
            setId(scriptInterface.createScript(sinfo.getEvent(), sinfo.getName(), sinfo.getDescription(), sinfo.getCode()).getId());
            setSinfo(CacheAdmin.getEnvironment().getScript(id).asEditable());
            // display updated script list
            //updateScriptList();
            new FxFacesMsgInfo("Script.nfo.created", sinfo.getName()).addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
            return "scriptCreate";
        }
        return "scriptOverview";
    }


    /**
     * Saves the edited script
     *
     * @return the next page to render
     */
    public String saveScript() {

        final UserTicket ticket = FxContext.getUserTicket();
        if (!ticket.isInRole(Role.ScriptManagement)) {
            new FxFacesMsgErr("Script.err.editPerm").addToContext();
            return null;
        }

        ensureScriptIdSet();

        try {
            scriptInterface.updateScriptInfo(id, sinfo.getEvent(), sinfo.getName(), sinfo.getDescription(), sinfo.getCode(), sinfo.isActive());
            //updateScriptList(); needed (see mandators) ???
            new FxFacesMsgInfo("Script.nfo.updated").addToContext();
            return null;
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
            return null;
        }
    }

    public List<Map.Entry<Long, String>> getTypeMappingNames() {
        if( this.typeMappingNames == null )
            this.typeMappingNames = new HashMap<Long, String>();
        ArrayList<Map.Entry<Long, String>> list = new ArrayList<Map.Entry<Long, String>>(this.typeMappingNames.entrySet().size());
        for (Map.Entry<Long, String> entry : this.typeMappingNames.entrySet()) {
            list.add(entry);
        }
        return list;
    }

    public List<Map.Entry<Long, String>> getAssignmentMappingNames() {
        if( this.assignmentMappingNames == null )
            this.assignmentMappingNames = new HashMap<Long, String>();
        ArrayList<Map.Entry<Long, String>> list = new ArrayList<Map.Entry<Long, String>>(this.assignmentMappingNames.entrySet().size());
        for (Map.Entry<Long, String> entry : this.assignmentMappingNames.entrySet()) {
            list.add(entry);
        }
        return list;
    }

    public FxScriptMapping getMapping() {
        return mapping;
    }

    public List<WrappedRunOnceInfo> getRunOnceInformation() {
        List<WrappedRunOnceInfo>runInfo = new ArrayList<WrappedRunOnceInfo>();
        try {
            for (FxScriptRunInfo ri : EJBLookup.getScriptingEngine().getRunOnceInformation())
                runInfo.add(new WrappedRunOnceInfo(ri)); 
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t.getMessage()).addToContext();
        }
        return runInfo;
    }

    public FxScriptRunInfo getCurrentRunInfo() {
        return currentRunInfo;
    }

    public void setCurrentRunInfo(FxScriptRunInfo currentRunInfo) {
        this.currentRunInfo = currentRunInfo;
    }

    /**
     * called from the structure editor -> get the oid of the script to show from the request parameters
     *
     * @param e event
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void structureListen(ActionEvent e) {
        FacesContext context = FacesContext.getCurrentInstance();
        Map requestParams = context.getExternalContext().getRequestParameterMap();
        long oid = -1;
        if (requestParams.get("oid") != null) {
            oid = Long.valueOf(requestParams.get("oid").toString());
        }
        if (oid != -1) {
            // this specifies the script to work on...
            this.id = oid;
        }
    }

    public static class WrappedRunOnceInfo {
        private FxScriptRunInfo runInfo;

        public WrappedRunOnceInfo(FxScriptRunInfo runInfo) {
            this.runInfo = runInfo;
        }

        public FxScriptRunInfo getRunInfo() {
            return runInfo;
        }

        public void setRunInfo(FxScriptRunInfo runInfo) {
            this.runInfo = runInfo;
        }

        public String getShortErrorMessage() {
            if (runInfo.getErrorMessage() != null && runInfo.getErrorMessage().length() >25) {
                return runInfo.getErrorMessage().substring(0,25)+"..";
            }
            else
                return runInfo.getErrorMessage();
        }
    }

}
