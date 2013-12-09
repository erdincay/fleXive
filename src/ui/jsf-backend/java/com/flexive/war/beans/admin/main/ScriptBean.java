/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2010
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
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.scripting.*;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import groovy.lang.GroovyShell;
import org.apache.commons.lang.StringUtils;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.*;

import static com.flexive.shared.EJBLookup.getScriptingEngine;

/**
 * JSF scripting bean
 *
 * @author Johannes Wernig-Pichler (johannes.wernig-pichler@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ScriptBean implements Serializable {
    private static final long serialVersionUID = -3764559730042052584L;

    private ScriptHolder currentData = new ScriptHolder();
    private static final String SCRIPT_EDIT = "scriptEdit";
    private static final String SCRIPT_OVERVIEW = "scriptOverview";

    /**
     * A holder class for a Script object
     * @since 3.1.4
     */
    private static class ScriptHolder implements Serializable {
        private long id = -1;
        private String name;
        private String desc;
        private String code;
        private boolean active;
        private FxScriptInfoEdit sinfo;
        private FxScriptMapping mapping;
        private Map<Long, String> typeMappingNames;
        private Map<Long, String> assignmentMappingNames;
        private FxScriptScope selectedScope = null;
        private long selectedScriptEventId = -1;
        private FxScriptRunInfo currentRunInfo;
        private String nameErrorMsg;
        private boolean inputFieldEnabled = false;
        private boolean verifyButtonEnabled = false;
        private boolean executeButtonEnabled = false;
        private long executionTime;
        private String userLang = "en";
        private String language = "groovy";
    }

    private int overviewPageNumber = 1;
    private int overviewRowNumber = 10;
    private String sortColumn;
    private String sortOrder;

    private transient Object result;
    
    private static final String ID_CACHE_KEY = ScriptBean.class + "_id";

    /**
     * @return true if the edit tab should be opened
     * @since 3.1.4
     */
    public boolean isOpenTab() {
        return currentData != null && currentData.id  >= 0;
    }

    /**
     * Opens the edit mandator in a tab
     * @return the name where to navigate
     * @since 3.1.4
     */
    public String openEditTab() {
        if (!isOpenTab()) return editScript();
        return SCRIPT_EDIT;
    }

    public ScriptHolder getCurrentData() {
        return currentData;
    }

    public void setCurrentData(ScriptHolder currentData) {
        this.currentData = currentData;
    }

    /**
     * Navigate back to the overview and remembers the changes of the mandator
     *
     * @return overview page
     * @since 3.1.4
     */
    public String overview() {
        return SCRIPT_OVERVIEW;
    }

    // constructor
    public ScriptBean() {
        this.currentData.sinfo = new FxScriptInfo().asEditable();
    }

    public FxScriptScope getSelectedScope() {
        if (currentData.selectedScope == null)
            currentData.selectedScope = FxScriptScope.All;
        return currentData.selectedScope;
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
            this.currentData.selectedScriptEventId = -1;

        return eventsForScope;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setSelectedScope(FxScriptScope selectedScope) {
        this.currentData.selectedScope = selectedScope;
    }

    public long getSelectedScriptEventId() {
        return currentData.selectedScriptEventId;
    }

    public void setSelectedScriptEventId(long selectedScriptEventId) {
        this.currentData.selectedScriptEventId = selectedScriptEventId;
    }

    /**
     * Retrieves the list of all scripts for the given event
     *
     * @return returns the List<FxScriptInfo> for the given event
     */
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
        return currentData.id;
    }

    public void setId(long id) {
        this.currentData.id = id;
    }

    public boolean isActive() {
        return currentData.active;
    }

    public void setActive(boolean active) {
        this.currentData.active = active;
    }

    public void setLanguage(String language) {
        this.currentData.language = language;
    }

    public String getLanguage() {
        return currentData.language;
    }

    /**
     * Loads the script specified by the parameter id.
     *
     * @return the next page to render
     */
    public String editScript() {
        ensureScriptIdSet();
        setSinfo(CacheAdmin.getEnvironment().getScript(currentData.id).asEditable());
        try {
            this.currentData.mapping = getScriptingEngine().loadScriptMapping(currentData.id);
            HashMap<Long, String> typeMappingNames = new HashMap<Long, String>();
            currentData.typeMappingNames = typeMappingNames;
            // we need the type names for the user interface and the type ids for the links
            for (FxScriptMappingEntry entry : this.currentData.mapping.getMappedTypes()) {
                typeMappingNames.put(entry.getId(), CacheAdmin.getEnvironment().getType(entry.getId()).getName());
                for (long id : entry.getDerivedIds())
                    typeMappingNames.put(id, CacheAdmin.getEnvironment().getType(id).getName());
            }
            HashMap<Long, String> assignmentMappingNames = new HashMap<Long, String>();
            currentData.assignmentMappingNames = assignmentMappingNames;
            for (FxScriptMappingEntry entry : this.currentData.mapping.getMappedAssignments()) {
                assignmentMappingNames.put(entry.getId(), CacheAdmin.getEnvironment().getAssignment(entry.getId()).getXPath());
                for (long id : entry.getDerivedIds())
                    assignmentMappingNames.put(id, CacheAdmin.getEnvironment().getAssignment(id).getXPath());
            }
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        } catch (SQLException e) {
            new FxFacesMsgErr("Script.err.loadMap").addToContext();
        }
        FxScriptInfoEdit sinfo = currentData.sinfo;
        setName(sinfo.getName());
        setDesc(sinfo.getDescription());
        setCode(sinfo.getCode());
        setActive(sinfo.isActive());
        verifyScriptName();
        return SCRIPT_EDIT;
    }

    public FxScriptInfo getSinfo() {
        return currentData.sinfo;
    }

    public void setSinfo(FxScriptInfoEdit sinfo) {
        this.currentData.sinfo = sinfo;
    }

    public String getName() {
        return currentData.name;
    }

    public void setName(String name) {
        this.currentData.name = name;
    }

    public String getDesc() {
        return currentData.desc;
    }

    public void setDesc(String desc) {
        this.currentData.desc = desc;
    }

    public String getCode() {
        return currentData.code;
    }

    public void setCode(String code) {
        this.currentData.code = code;
    }

    private void ensureScriptIdSet() {
        if (this.currentData.id <= 0) {
            this.currentData.id = (Long) FxJsfUtils.getSessionAttribute(ID_CACHE_KEY);
        }
    }

    public int getOverviewPageNumber() {
        return overviewPageNumber;
    }

    public void setOverviewPageNumber(int overviewPageNumber) {
        this.overviewPageNumber = overviewPageNumber;
    }

    public int getOverviewRowNumber() {
        return overviewRowNumber;
    }

    public void setOverviewRowNumber(int overviewRowNumber) {
        this.overviewRowNumber = overviewRowNumber;
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
            return SCRIPT_OVERVIEW;
        }

        ensureScriptIdSet();
        try {
            getScriptingEngine().remove(currentData.id);
            // display updated script list  -->handled via a4j now
            //updateScriptList();  -->handled via a4j now
            new FxFacesMsgInfo("Script.nfo.deleted").addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return SCRIPT_OVERVIEW;
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
            return SCRIPT_OVERVIEW;
        }

        ensureScriptIdSet();
        try {
            getScriptingEngine().runScript(currentData.id);
            new FxFacesMsgInfo("Script.nfo.executed", CacheAdmin.getEnvironment().getScript(currentData.id).getName()).addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        } catch (Throwable t) {
            new FxFacesMsgErr("Script.err.run").addToContext();
        }
        return SCRIPT_OVERVIEW;
    }

    /**
     * Runs the given script code in the console
     */
    public void runScriptInConsole() {
        FxScriptInfoEdit sinfo = currentData.sinfo;
        if (StringUtils.isBlank(sinfo.getCode())) {
            new FxFacesMsgErr("Script.err.noCodeProvided").addToContext();
        } else {
            long start = System.currentTimeMillis();
            try {
                result = ScriptConsoleBean.runScript(sinfo.getCode(), sinfo.getName(), false);
            } catch (Exception e) {
                final StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                final String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                result = new Formatter().format("Exception caught: %s%n%s", msg, writer.getBuffer());
            } finally {
                currentData.executionTime = System.currentTimeMillis() - start;
            }
        }
    }

    /**
     * Returns the result object
     *
     * @return Object result
     */
    public Object getResult() {
        return result;
    }

    /**
     * Returns the time (ms) it took to execute the script
     *
     * @return long executionTime
     */
    public long getExecutionTime() {
        return currentData.executionTime;
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
            return SCRIPT_OVERVIEW;
        }

        FxScriptInfoEdit sinfo = currentData.sinfo;
        if (StringUtils.isBlank(sinfo.getName()) || sinfo.getEvent() == null) {
            new FxFacesMsgErr("Script.err.createMiss").addToContext();
            return "scriptCreate";
        }

        try {
            if (!isRenderCachedSelect())
                sinfo.setCached(false);
            setId(getScriptingEngine().createScript(sinfo).getId());
            setSinfo(CacheAdmin.getEnvironment().getScript(currentData.id).asEditable());
            // display updated script list
            //updateScriptList();
            new FxFacesMsgInfo("Script.nfo.created", sinfo.getName()).addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
            return "scriptCreate";
        }
        return SCRIPT_OVERVIEW;
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
            FxScriptInfoEdit sinfo = currentData.sinfo;
            getScriptingEngine().updateScriptInfo(new FxScriptInfoEdit(currentData.id, sinfo.getEvent(), sinfo.getName(),
                    sinfo.getDescription(), sinfo.getCode(), sinfo.isActive(),
                    isRenderCachedSelect() && sinfo.isCached()));
            //updateScriptList(); needed (see mandators) ???
            new FxFacesMsgInfo("Script.nfo.updated").addToContext();
            return null;
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
            return null;
        }
    }

    /**
     * Returns the list of types which the script is mapped to
     *
     * @return returns the List<Map.Entry<Long, String>> of type mapping names
     */
    public List<Map.Entry<Long, String>> getTypeMappingNames() {
        if (this.currentData.typeMappingNames == null)
            this.currentData.typeMappingNames = new HashMap<Long, String>();
        ArrayList<Map.Entry<Long, String>> list = new ArrayList<Map.Entry<Long, String>>(this.currentData.typeMappingNames.entrySet().size());
        for (Map.Entry<Long, String> entry : this.currentData.typeMappingNames.entrySet()) {
            list.add(entry);
        }
        return list;
    }

    /**
     * Returns the list of assignments which the script is mapped to
     *
     * @return returns the List<Map.Entry<Long, String>> of assignment mapping names
     */
    public List<Map.Entry<Long, String>> getAssignmentMappingNames() {
        if (this.currentData.assignmentMappingNames == null)
            this.currentData.assignmentMappingNames = new HashMap<Long, String>();
        ArrayList<Map.Entry<Long, String>> list = new ArrayList<Map.Entry<Long, String>>(this.currentData.assignmentMappingNames.entrySet().size());
        for (Map.Entry<Long, String> entry : this.currentData.assignmentMappingNames.entrySet()) {
            list.add(entry);
        }
        return list;
    }

    public FxScriptMapping getMapping() {
        return currentData.mapping;
    }

    public List<WrappedRunOnceInfo> getRunOnceInformation() {
        List<WrappedRunOnceInfo> runInfo = new ArrayList<WrappedRunOnceInfo>();
        try {
            for (FxScriptRunInfo ri : getScriptingEngine().getRunOnceInformation())
                runInfo.add(new WrappedRunOnceInfo(ri));
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t.getMessage()).addToContext();
        }
        return runInfo;
    }

    public FxScriptRunInfo getCurrentRunInfo() {
        return currentData.currentRunInfo;
    }

    public void setCurrentRunInfo(FxScriptRunInfo currentRunInfo) {
        this.currentData.currentRunInfo = currentRunInfo;
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
            oid = Long.parseLong(requestParams.get("oid").toString());
        }
        if (oid != -1) {
            // this specifies the script to work on...
            this.currentData.id = oid;
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
            if (runInfo.getErrorMessage() != null && runInfo.getErrorMessage().length() > 25) {
                return runInfo.getErrorMessage().substring(0, 25) + "..";
            } else
                return runInfo.getErrorMessage();
        }
    }

    /**
     * Sets the error message
     *
     * @param nameErrorMsg String containing the error message
     */
    public void setNameErrorMsg(String nameErrorMsg) {
        this.currentData.nameErrorMsg = nameErrorMsg;
    }

    /**
     * Returns the error message after script name verification
     *
     * @return Returns the error message
     */
    public String getNameErrorMsg() {
        return this.currentData.nameErrorMsg;
    }

    /**
     * Sets the boolean inputFieldDisabled
     *
     * @param inputFieldEnabled boolean value enabling or disabling the input fields
     */
    public void setInputFieldEnabled(boolean inputFieldEnabled) {
        this.currentData.inputFieldEnabled = inputFieldEnabled;
    }

    /**
     * @return boolean inputFieldDisabled
     */
    public boolean isInputFieldEnabled() {
        return this.currentData.inputFieldEnabled;
    }

    /**
     * @param verifyButtonEnabled Sets the verifyButtonEnabled
     */
    public void setVerifyButtonEnabled(boolean verifyButtonEnabled) {
        this.currentData.verifyButtonEnabled = verifyButtonEnabled;
    }

    /**
     * @return Returns true to enable the syntax check button
     */
    public boolean isVerifyButtonEnabled() {
        return this.currentData.verifyButtonEnabled;
    }

    /**
     * @param executeButtonEnabled sets the execute/ run script button
     */
    public void setExecuteButtonEnabled(boolean executeButtonEnabled) {
        this.currentData.executeButtonEnabled = executeButtonEnabled;
    }

    /**
     * @return returns true if the script event is set to FxScriptEvent.Manual
     */
    public boolean isExecuteButtonEnabled() {
        return currentData.executeButtonEnabled;
    }

    /**
     * Check the current script event and set a default if necessary
     */
    public void checkScriptEvent() {
        FxScriptInfoEdit sinfo = currentData.sinfo;
        if (sinfo.getEvent() == null)
            sinfo.setEvent(FxScriptEvent.Manual); // set a default
        currentData.executeButtonEnabled = sinfo.getEvent() == FxScriptEvent.Manual;
    }

    /**
     * Action method to verify the script name (i.e. the file extension) against the available script engines
     * - sets the various boolean field / button enablers
     */
    public void verifyScriptName() {
        final String name = currentData.sinfo.getName();
        if (StringUtils.isBlank(name)) {
            currentData.nameErrorMsg = null;
            currentData.inputFieldEnabled = false;
            currentData.verifyButtonEnabled = false;
            currentData.executeButtonEnabled = false;
        } else if (!FxSharedUtils.isGroovyScript(name) && !checkScriptEngineExtensions(name)) {
            currentData.nameErrorMsg = (MessageBean.getInstance().getMessage("Script.err.name"));
            currentData.inputFieldEnabled = false;
            currentData.verifyButtonEnabled = false;
            currentData.executeButtonEnabled = false;
        } else {
            currentData.nameErrorMsg = null;
            currentData.inputFieldEnabled = true;
            checkScriptEvent();
            // separately enable the Groovy syntax verification button
            if (FxSharedUtils.isGroovyScript(name)) {
                currentData.verifyButtonEnabled = true;
                currentData.language = "gy";
            } else // set language = extension
                currentData.language = name.substring(name.lastIndexOf(".") + 1, name.length());

        }
    }

    /**
     * Checks all available extensions from the ScriptEngineFactory and sets the current language
     *
     * @param scriptName Name of the script
     * @return true if the given script name contains a valid extension found in the scripting engine
     */
    private boolean checkScriptEngineExtensions(String scriptName) {
        try {
            // use scripting engine and avoid JDK6 dependency
            for (String[] engine : EJBLookup.getScriptingEngine().getAvailableScriptEngines()) {
                // check if extension (first element) matches scriptName
                if (scriptName.toLowerCase().endsWith(engine[0])) {
                    return true;
                }
            }
            return false;
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    /**
     * Verifies the syntax of a given groovy script
     */
    public void checkScriptSyntax() {
        FxScriptInfoEdit sinfo = currentData.sinfo;
        checkScriptSyntax(sinfo.getName(), sinfo.getCode());
    }

    /**
     * Static method to verify the syntax of a given Groovy script
     *
     * @param name The script name
     * @param code The script code
     */
    static void checkScriptSyntax(String name, String code) {
        if (!FxSharedUtils.isGroovyScript(name) || StringUtils.isBlank(code)) {
            new FxFacesMsgErr("Script.err.verifyFailed", name).addToContext();
        } else {
            try {
                new GroovyShell().parse(code);
                new FxFacesMsgInfo("Script.nfo.scriptVerified").addToContext();
            } catch (Exception e) {
                new FxFacesMsgErr("Script.err.syntaxError").addToContext();
                new FxFacesMsgErr(e).addToContext();
            }
        }
    }

    /**
     * This method adds the default [fleXive] imports for a given script
     * (Configured in Script.properties and Script_de.properties: Script.defaultImports.[script extension],
     * e.g. "Script.defaultImports.groovy")
     * @throws FxApplicationException if the code could not be loaded
     */
    public void addDefaultImports() throws FxApplicationException {
        FxScriptInfoEdit sinfo = currentData.sinfo;
        String code = sinfo.getCode();
        final String name = sinfo.getName();
        if (StringUtils.isNotBlank(name)) {
            sinfo.setCode(getClassImports(name.substring(name.lastIndexOf(".") + 1, name.length())) + code);
        }
    }

    /**
     * This method retrieves the default imports for a given scripttype
     *
     * @param scriptType The type of the script by extension, e.g.: "groovy" or "js"
     * @return Returns the default imports for a scripting engine known by [fleXive]
     */
    static String getClassImports(String scriptType) {
        scriptType = scriptType.replaceAll("gy", "groovy");
        String importProperty = "Script.defaultImports." + scriptType;
        String defImports = "";
        if (!scriptType.equals("")) {
            defImports = MessageBean.getInstance().getMessage(importProperty);
            if (defImports.equals("??" + importProperty + "??")) {
                defImports = "";
                new FxFacesMsgErr("Script.err.noImports", scriptType).addToContext();
            }
        }
        return defImports;
    }

    /**
     * @param userLang set the current user's language
     */
    public void setUserLang(String userLang) {
        this.currentData.userLang = userLang;
    }

    /**
     * @return returns the current user's language
     */
    public String getUserLang() {
        currentData.userLang = FxContext.getUserTicket().getLanguage().getIso2digit();
        return currentData.userLang;
    }

    public boolean isRenderCachedSelect() {
        FxScriptInfoEdit sinfo = currentData.sinfo;
        return sinfo != null && StringUtils.isNotBlank(sinfo.getName()) && FxSharedUtils.isGroovyScript(sinfo.getName());
    }
}
