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
package com.flexive.shared.interfaces;

import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.scripting.*;

import javax.ejb.Remote;
import java.sql.Connection;
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
     * Get all available information about a script
     *
     * @param scriptId id of the requested script
     * @return FxScriptInfo
     * @throws FxApplicationException on errors
     */
    public FxScriptInfo getScriptInfo(long scriptId) throws FxApplicationException;

    /**
     * Get all available information for all existing scripts
     *
     *
     * @return FxScriptInfo
     * @throws FxApplicationException on errors
     */
    public FxScriptInfo[] getScriptInfos() throws FxApplicationException;

    /**
     * Update a scripts info
     *
     * @param scriptId    requested script id
     * @param event     requested script event
     * @param name        new name (or <code>null</code> if unchanged)
     * @param description new description (or <code>null</code> if unchanged)
     * @param code        the code
     * @param active      if the script is active
     * @throws FxApplicationException on errors
     * @see com.flexive.shared.scripting.FxScriptEvent
     */
    void updateScriptInfo(long scriptId, FxScriptEvent event, String name, String description, String code, boolean active) throws FxApplicationException;

    /**
     * Update script info
     *
     * @param scriptInfo    the edited script info
     * @throws com.flexive.shared.exceptions.FxApplicationException     on errors
     */
    void updateScriptInfo(FxScriptInfoEdit scriptInfo) throws FxApplicationException;

    /**
     * Convenience method to update a scripts code
     *
     * @param scriptId requested script id
     * @param code     the code
     * @throws FxApplicationException on errors
     * @see #updateScriptInfo(long, com.flexive.shared.scripting.FxScriptEvent , String, String, String, boolean)
     */
    void updateScriptCode(long scriptId, String code) throws FxApplicationException;

    /**
     * Get scripts by their event
     *
     * @param scriptEvent     requested script event
     * @return array of id's by event
     */
    List<Long> getByScriptEvent(FxScriptEvent scriptEvent);

    /**
     * Create a new script
     * (newly created scripts are set to active per default).
     *
     * @param event     script event
     * @param name        (unique) name
     * @param description description
     * @param code        code
     * @return FxScriptInfo for the new created script
     * @throws FxApplicationException on errors
     * @see com.flexive.shared.scripting.FxScriptEvent
     */
    FxScriptInfo createScript(FxScriptEvent event, String name, String description, String code) throws FxApplicationException;

    /**
     * Create a new script based on a script from the library
     * (newly created scripts are set to active per default).
     * @param event     script event
     * @param libraryname name of the script in the script library
     * @param name        (unique) name
     * @param description description
     * @return FxScriptInfo for the new created script
     * @throws FxApplicationException on errors
     * @see com.flexive.shared.scripting.FxScriptEvent
     */
    FxScriptInfo createScriptFromLibrary(FxScriptEvent event, String libraryname, String name, String description) throws FxApplicationException;

    /**
     * Create a new script based on a script from the library
     * (newly created scripts are set to active per default).
     *
     * @param dropName    name of the drop to use as repository
     * @param event     script event
     * @param libraryname name of the script in the script library
     * @param name        (unique) name
     * @param description description
     * @return FxScriptInfo for the new created script
     * @throws FxApplicationException on errors
     * @see com.flexive.shared.scripting.FxScriptEvent
     */
    FxScriptInfo createScriptFromDropLibrary(String dropName, FxScriptEvent event, String libraryname, String name, String description) throws FxApplicationException;

    /**
     * Remove a script (will remove all mappings for this script as well)
     *
     * @param scriptId id of the script and its mappings to remove
     * @throws FxApplicationException on errors
     */
    void removeScript(long scriptId) throws FxApplicationException;

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
    public FxScriptResult runScript(String name, FxScriptBinding binding, String code) throws FxApplicationException;

    /**
     * Get a list containing script extension and script engine info as 2-dimensional String array
     *
     * @return list containing script extension and script engine info as 2-dimensional String array
     * @throws FxApplicationException on errors
     */
    public List<String[]> getAvailableScriptEngines() throws FxApplicationException;

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
     * @param _con  the database connection
     * @param scriptId  the script
     * @return  the script mappings
     * @throws FxLoadException on errors
     * @throws SQLException on errors
     */
    FxScriptMapping loadScriptMapping(Connection _con, long scriptId) throws FxLoadException, SQLException;

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
     * @param scriptEvent   FxScriptEvent for this mapping (on create, save, remove, etc.)
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
     * @param event         the script event
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
     * Update a mapping for assignments
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
     * Update a mapping for types
     *
     * @param scriptId      id of the script
     * @param typeId        id of the type
     * @param event         the script event
     * @param active        mapping is active?
     * @param derivedUsage  mapping used in derived types?
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
}
