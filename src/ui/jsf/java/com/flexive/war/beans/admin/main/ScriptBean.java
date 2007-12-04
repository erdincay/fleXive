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
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.interfaces.ScriptingEngine;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptMapping;
import com.flexive.shared.scripting.FxScriptMappingEntry;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import java.sql.SQLException;
import java.util.*;

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
    private FxScriptInfo[] scripts;
    private FxScriptInfo sinfo;
    private ScriptingEngine scriptInterface;
    private FxScriptEvent scriptEvent;
    private FxScriptMapping mapping;
    private Map<Long, String> typeMappingNames;

    private static final String ID_CACHE_KEY = ScriptBean.class + "_id";


    // constructor
    public ScriptBean() {
        this.scriptInterface = EJBLookup.getScriptingEngine();
        this.sinfo = new FxScriptInfo();
    }


    public FxScriptInfo[] getScripts() {
        if (scripts == null) {
            try {
                scripts = scriptInterface.getScriptInfos();
            } catch (FxApplicationException e) {
                new FxFacesMsgErr("Script.err.loadAll").addToContext();
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return scripts;
    }

    public List<SelectItem> getScriptsAsSL() {
        if (scripts == null) {
            try {
                scripts = scriptInterface.getScriptInfos();
            } catch (FxApplicationException e) {
                new FxFacesMsgErr("Script.err.loadAll").addToContext();
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return FxJsfUtils.asSelectList(Arrays.asList(scripts), true);
    }


    public List<SelectItem> getScriptsAsID() {
        if (scripts == null) {
            try {
                scripts = scriptInterface.getScriptInfos();
            } catch (FxApplicationException e) {
                new FxFacesMsgErr("Script.err.loadAll").addToContext();
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        ArrayList<SelectItem> retScripts = new ArrayList<SelectItem>(scripts.length);
        for (FxScriptInfo si : scripts) {
            retScripts.add(new SelectItem(si.getId(), si.getName()));
        }
        return retScripts;
    }


    // gets all scripts belonging to the type defined in the "scriptType" member
    public FxScriptInfo[] getList() {
        // if no filtering needs to be done return the whole script list..
        if (scriptEvent == null) {
            return getScripts();
        } else {
            // get all scripts (only the IDs) belonging to the defined type
            List<Long> scriptList = scriptInterface.getByScriptType(scriptEvent);
            ArrayList<FxScriptInfo> result = new ArrayList<FxScriptInfo>(scriptList.size());
            // get the info to every script
            for (Long l : scriptList) {
                try {
                    result.add(scriptInterface.getScriptInfo(l));
                } catch (FxApplicationException e) {
                    // do nothing...
                }
            }
            return result.toArray(new FxScriptInfo[result.size()]);
        }
    }

    public List<Map.Entry<Long, String>> getTypeMappingNames() {
        ArrayList<Map.Entry<Long, String>> list = new ArrayList<Map.Entry<Long, String>>(this.typeMappingNames.entrySet().size());

        for (Map.Entry<Long, String> entry : this.typeMappingNames.entrySet()) {
            list.add(entry);
        }
        return list;
        //Set<Map.Entry<Long, String>> set = typeMappingNames.entrySet();
        //return set;
    }


    public Set getSet() {
        return typeMappingNames.entrySet();
    }

    private void ensureScriptIdSet() {
        if (this.id <= 0) {
            this.id = (Long) FxJsfUtils.getSessionAttribute(ID_CACHE_KEY);
        }
    }

    /**
     * Loads the script specified by the parameter id.
     *
     * @return the next page to render
     */
    public String editScript() {
        ensureScriptIdSet();
        setSinfo(CacheAdmin.getEnvironment().getScript(id));
        try {
            this.mapping = scriptInterface.loadScriptMapping(null, id);
            this.typeMappingNames = new HashMap<Long, String>(mapping.getMappedAssignments().size() + mapping.getMappedTypes().size());
            // we need the type names for the user interface and the type ids for the links
            for (FxScriptMappingEntry entry : this.mapping.getMappedTypes()) {
                this.typeMappingNames.put(entry.getId(), CacheAdmin.getEnvironment().getType(entry.getId()).getName());
            }
        } catch (FxLoadException e) {
            new FxFacesMsgErr("Script.err.loadMap").addToContext();
        } catch (SQLException e) {
            new FxFacesMsgErr("Script.err.loadMap").addToContext();
        }
        setName(sinfo.getName());
        setDesc(sinfo.getDescription());
        setCode(sinfo.getCode());
        return "scriptEdit";
    }

    public FxScriptMapping getMapping() {
        return mapping;
    }

    public FxScriptEvent getScriptEvent() {
        return scriptEvent;
    }

    public void setScriptEvent(FxScriptEvent scriptEvent) {
        this.scriptEvent = scriptEvent;
    }

    public FxScriptInfo getSinfo() {
        return sinfo;
    }

    public void setSinfo(FxScriptInfo sinfo) {
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // ensures that the scriptinfos will be loaded from cache on the next request form them (so changes will be displayed immediately)
    private void updateScriptList() {
        scripts = null;
    }

    /**
     * Deletes a script, with the id specified by id.
     *
     * @return the next pageto render
     */
    public String deleteScript() {

        final UserTicket ticket = FxContext.get().getTicket();
        if (!ticket.isInRole(Role.ScriptManagement)) {
            new FxFacesMsgErr("Script.err.deletePerm").addToContext();
            return "scriptOverview";
        }

        ensureScriptIdSet();
        try {
            scriptInterface.removeScript(id);
            // display updated script list
            updateScriptList();
            new FxFacesMsgInfo("Script.nfo.deleted").addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("Script.err.delete").addToContext();
        }
        return "scriptOverview";
    }

    /**
     * Executes a script, with the id specified by id.
     *
     * @return the next page to render
     */
    public String runScript() {

        final UserTicket ticket = FxContext.get().getTicket();
        if (!ticket.isInRole(Role.ScriptExecution)) {
            new FxFacesMsgErr("Script.err.runPerm").addToContext();
            return "scriptOverview";
        }

        ensureScriptIdSet();
        try {
            scriptInterface.runScript(id);
            new FxFacesMsgInfo("Script.nfo.executed", CacheAdmin.getEnvironment().getScript(id).getName()).addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("Script.err.run").addToContext();
        }
        return "scriptOverview";
    }

    /**
     * Saves the edited script
     *
     * @return the next page to render
     */
    public String saveScript() {

        final UserTicket ticket = FxContext.get().getTicket();
        if (!ticket.isInRole(Role.ScriptManagement)) {
            new FxFacesMsgErr("Script.err.editPerm").addToContext();
            return "scriptOverview";
        }

        ensureScriptIdSet();

        try {
            scriptInterface.updateScriptInfo(id, sinfo.getEvent(), sinfo.getName(), sinfo.getDescription(), sinfo.getCode());
            //updateScriptList(); needed (see mandators) ???
            new FxFacesMsgInfo("Script.nfo.updated").addToContext();
            return "scriptOverview";
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("Script.err.save").addToContext();
            return "scriptOverview";
        }
    }

    /**
     * Creates a new script from the beans data.
     *
     * @return the next jsf page to render
     */
    public String createScript() {

        final UserTicket ticket = FxContext.get().getTicket();
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
            setSinfo(CacheAdmin.getEnvironment().getScript(id));
            // display updated script list
            updateScriptList();
            new FxFacesMsgInfo("Script.nfo.created", sinfo.getName()).addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("Script.err.create").addToContext();
        }
        return "scriptOverview";
    }

    // called from the structure editor -> get the oid of the script to show from the request parameters
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

}
