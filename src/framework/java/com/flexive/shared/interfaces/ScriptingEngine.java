/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.shared.interfaces;

import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.scripting.*;

import javax.ejb.Remote;
import java.sql.SQLException;
import java.util.List;

/**
 * Scripting engine
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface ScriptingEngine {


    /**
     * Load a scripts code
     *
     * @param scriptId requested script
     * @return code
     * @throws FxApplicationException on errors
     */
    String loadScriptCode(long scriptId) throws FxApplicationException;

    /**
     * Get all available information for all existing scripts
     *
     * @return FxScriptInfo
     * @throws FxApplicationException on errors
     */
    List<FxScriptInfo> getScriptInfos() throws FxApplicationException;

    /**
     * Update script info
     *
     * @param scriptInfo the edited script info
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on errors
     */
    void updateScriptInfo(FxScriptInfoEdit scriptInfo) throws FxApplicationException;

    /**
     * Convenience method to update a scripts code
     *
     * @param scriptId requested script id
     * @param code     the code
     * @throws FxApplicationException on errors
     * @see #updateScriptInfo(FxScriptInfoEdit scriptInfo)
     */
    void updateScriptCode(long scriptId, String code) throws FxApplicationException;

    /**
     * Get scripts by their event
     *
     * @param scriptEvent requested script event
     * @return array of id's by event
     */
    List<Long> getByScriptEvent(FxScriptEvent scriptEvent);

    /**
     * Constructs a new script with the data provided by FxScriptInfoEdit,
     * (the id is discarded)
     *
     * @param scriptInfo script information
     * @since 3.1.1
     * @return FxScriptInfo for the newly created script
     * @throws com.flexive.shared.exceptions.FxApplicationException on errors
     */
    FxScriptInfo createScript(FxScriptInfoEdit scriptInfo) throws FxApplicationException;

    /**
     * Create a new script based on a script from the library:<br/>
     * The script's code is loaded from the library, other
     * data needed for creating the script is provided
     * by the scriptInfo parameter (id is discarded).
     *
     * @param libraryname name of the script in the script library
     * @param scriptInfo script information
     * @return FxScriptInfo for the newly created script
     * @throws FxApplicationException on errors
     * @see com.flexive.shared.scripting.FxScriptEvent
     * @since 3.1.1
     */
    FxScriptInfo createScriptFromLibrary(String libraryname, FxScriptInfo scriptInfo) throws FxApplicationException;

    /**
     * Create a new script based on a script from a drop's library:<br/>
     * The script's code is loaded from the library, other
     * data needed for creating the script is provided
     * by the scriptInfo parameter (id is discarded).
     *
     * @param dropName name of the drop to use as repository
     * @param libraryname name of the script in drop
     * @param scriptInfo script information
     * @return FxScriptInfo for the newly created script
     * @throws FxApplicationException on errors
     * @see com.flexive.shared.scripting.FxScriptEvent
     * @since 3.1.1
     */
    FxScriptInfo createScriptFromDropLibrary(String dropName, String libraryname, FxScriptInfo scriptInfo) throws FxApplicationException;

    /**
     * Remove a script (will remove all mappings for this script as well)
     *
     * @param scriptId id of the script and its mappings to remove
     * @throws FxApplicationException on errors
     */
    void remove(long scriptId) throws FxApplicationException;

    /**
     * Run a script with the given variable binding
     *
     * @param scriptName name of the script to run
     * @param binding  variable binding to use (all bound variables have to be serializable!)
     * @return script result
     * @throws FxApplicationException on errors
     */
    FxScriptResult runScript(String scriptName, FxScriptBinding binding) throws FxApplicationException;

    /**
     * Run a script with the given variable binding
     *
     * @param scriptId id of the script to run
     * @param binding  variable binding to use (all bound variables have to be serializable!)
     * @return script result
     * @throws FxApplicationException on errors
     */
    FxScriptResult runScript(long scriptId, FxScriptBinding binding) throws FxApplicationException;

    /**
     * Execute a script
     *
     * @param name    name of the script, extension is needed to choose interpreter
     * @param binding bindings to apply
     * @param code    the script code
     * @return last script evaluation result
     * @throws FxApplicationException on errors
     */
    FxScriptResult runScript(String name, FxScriptBinding binding, String code) throws FxApplicationException;

    /**
     * Get a list containing script extension and script engine info as 2-dimensional String array
     *
     * @return list containing script extension and script engine info as 2-dimensional String array
     * @throws FxApplicationException on errors
     */
    List<String[]> getAvailableScriptEngines() throws FxApplicationException;

    /**
     * Run a script
     *
     * @param scriptId id of the script to run
     * @return script result
     * @throws FxApplicationException on errors
     */
    FxScriptResult runScript(long scriptId) throws FxApplicationException;

    /**
     * Create a new mapping for assignments with the default FxScriptEvent the script was created with
     *
     * @param scriptId     id of the script
     * @param assignmentId id of the assignment
     * @param active       mapping is active?
     * @param derivedUsage mapping used in derived assignments?
     * @return the created entry
     * @throws FxApplicationException on errors
     */
    FxScriptMappingEntry createAssignmentScriptMapping(long scriptId, long assignmentId, boolean active, boolean derivedUsage) throws FxApplicationException;

    /**
     * Create a new mapping for assignments with with a given FxScriptEvent
     *
     * @param scriptEvent  FxScriptEvent for this mapping (on create, save, remove, etc.)
     * @param scriptId     id of the script
     * @param assignmentId id of the assignment
     * @param active       mapping is active?
     * @param derivedUsage mapping used in derived assignments?
     * @return the created entry
     * @throws FxApplicationException on errors
     */
    FxScriptMappingEntry createAssignmentScriptMapping(FxScriptEvent scriptEvent, long scriptId, long assignmentId, boolean active, boolean derivedUsage) throws FxApplicationException;

    /**
     * Loads all assignment mappings for a specified script
     *
     * @param scriptId the script
     * @return the script mappings
     * @throws FxLoadException on errors
     * @throws SQLException    on errors
     */
    FxScriptMapping loadScriptMapping(long scriptId) throws FxLoadException, SQLException;

    /**
     * Create a new mapping for types with the default FxScriptEvent the script was created with
     *
     * @param scriptId     id of the script
     * @param typeId       id of the type
     * @param active       mapping is active?
     * @param derivedUsage mapping used in derived types?
     * @return the created entry
     * @throws FxApplicationException on errors
     */
    FxScriptMappingEntry createTypeScriptMapping(long scriptId, long typeId, boolean active, boolean derivedUsage) throws FxApplicationException;

    /**
     * Create a new mapping for types with a given FxScriptEvent
     *
     * @param scriptEvent  FxScriptEvent for this mapping (on create, save, remove, etc.)
     * @param scriptId     id of the script
     * @param typeId       id of the type
     * @param active       mapping is active?
     * @param derivedUsage mapping used in derived types?
     * @return the created entry
     * @throws FxApplicationException on errors
     */
    FxScriptMappingEntry createTypeScriptMapping(FxScriptEvent scriptEvent, long scriptId, long typeId, boolean active, boolean derivedUsage) throws FxApplicationException;

    /**
     * Remove a mapping from a script to an assignment (directly mapped, not via inheritance!)
     *
     * @param scriptId     id of the script
     * @param assignmentId id of the assignment
     * @throws FxApplicationException on errors
     */
    void removeAssignmentScriptMapping(long scriptId, long assignmentId) throws FxApplicationException;

    /**
     * Remove a mapping from a script to an assignment for a specific event
     * (directly mapped, not via inheritance!)
     *
     * @param scriptId     id of the script
     * @param assignmentId id of the assignment
     * @param event        the script event
     * @throws FxApplicationException on errors
     */
    void removeAssignmentScriptMappingForEvent(long scriptId, long assignmentId, FxScriptEvent event) throws FxApplicationException;

    /**
     * Remove a mapping from a script to a type (directly mapped, not via inheritance!)
     *
     * @param scriptId id of the script
     * @param typeId   id of the type
     * @throws FxApplicationException on errors
     */
    void removeTypeScriptMapping(long scriptId, long typeId) throws FxApplicationException;

    /**
     * Remove a mapping from a script to a type (directly mapped, not via inheritance!) for a specific script event
     *
     * @param scriptId id of the script
     * @param typeId   id of the type
     * @param event    the script event
     * @throws FxApplicationException on errors
     */
    void removeTypeScriptMappingForEvent(long scriptId, long typeId, FxScriptEvent event) throws FxApplicationException;

    /**
     * Update a mapping for assignments (activate or deactivate a mapping, toggle derived usage)
     *
     * @param scriptId     id of the script
     * @param assignmentId id of the assignment
     * @param event        the script event
     * @param active       mapping is active?
     * @param derivedUsage mapping used in derived assignments?
     * @return the updated entry
     * @throws FxApplicationException on errors
     */
    FxScriptMappingEntry updateAssignmentScriptMappingForEvent(long scriptId, long assignmentId, FxScriptEvent event, boolean active, boolean derivedUsage) throws FxApplicationException;

    /**
     * Update a mapping for types (activate or deactivate a mapping, toggle derived usage)
     *
     * @param scriptId     id of the script
     * @param typeId       id of the type
     * @param event        the script event
     * @param active       mapping is active?
     * @param derivedUsage mapping used in derived types?
     * @return the updated  entry
     * @throws FxApplicationException on errors
     */
    FxScriptMappingEntry updateTypeScriptMappingForEvent(long scriptId, long typeId, FxScriptEvent event, boolean active, boolean derivedUsage) throws FxApplicationException;

    /**
     * Execute run-once scripts.
     * No exceptions are throws since there is no means of user feedback.
     * If errors occur, they are written to the logfile.
     * run-once scripts are only executed once in the lifetime of a division!
     * consecutive calls have no effect and nothing will be executed!
     *
     * @throws FxApplicationException on errors
     */
    void executeRunOnceScripts() throws FxApplicationException;

    /**
     * Execute start up scripts.
     * No exceptions are throws since there is no means of user feedback.
     * If errors occur, they are written to the logfile.
     * start up scripts are only executed once each time the application server is started.
     * consecutive calls *will* execute start up scripts again!
     */
    void executeStartupScripts();

    /**
     * Execute run-once scripts for drops.
     * run-once scripts are only executed once in the lifetime of a division!
     * consecutive calls have no effect and nothing will be executed!
     * param will be set to "true" once the scripts are run and will be checked to be "false" prior to run
     *
     * @param param    boolean parameter to mark scripts as being run
     * @param dropName name of the drop (WAR archive name without extension)
     * @throws FxApplicationException if the requested drop is unknown or invalid
     */
    void executeDropRunOnceScripts(Parameter<Boolean> param, String dropName) throws FxApplicationException;

    /**
     * Execute start up scripts for a specific drop. To be called from a filter or the like.
     * No exceptions are throws since there is no means of user feedback.
     * If errors occur, they are written to the logfile.
     * start up scripts are only executed once each time the application server is started.
     * consecutive calls *will* execute start up scripts again!
     *
     * @param dropName name of the drop (WAR archive name without extension)
     * @throws FxApplicationException if the requested drop is unknown or invalid
     */
    void executeDropStartupScripts(String dropName) throws FxApplicationException;

    /**
     * Get information about running/executed runOnce scripts (including all drops)
     * Please note that this method will only return entries if scripts have been executed in the current VM and in the
     * current session (information is not persisted - yet!)
     *
     * @return information
     * @throws FxApplicationException on errors
     */
    List<FxScriptRunInfo> getRunOnceInformation() throws FxApplicationException;

    /**
     * Create a script schedule
     *
     * @param scriptSchedule script schedule edit
     * @return the created script schedule
     * @since 3.1.2
     * @throws FxApplicationException   on errors
     */
    FxScriptSchedule createScriptSchedule(FxScriptScheduleEdit scriptSchedule) throws FxApplicationException;

    /**
     * Removes an existing script schedule
     *
     * @param scheduleId script schedule id
     * @since 3.1.2
     * @throws FxApplicationException   on errors
     */
    void removeScriptSchedule(long scheduleId) throws FxApplicationException;

    /**
     * Update an existing script schedule
     *
     * @param scriptSchedule  script schedule
     * @return  the updated script schedule
     * @since 3.1.2
     * @throws FxApplicationException   on errors
     */
    FxScriptSchedule updateScriptSchedule(FxScriptScheduleEdit scriptSchedule) throws FxApplicationException;
}
