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
package com.flexive.core.storage;

import com.flexive.core.structure.FxEnvironmentImpl;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptMapping;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.structure.*;
import com.flexive.shared.workflow.Route;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.workflow.Workflow;

import java.sql.Connection;
import java.util.List;

/**
 * Interface for concrete environment loader implementations (types, acl, etc)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface EnvironmentLoader {

    /**
     * Load all known ACL's from the given connection
     *
     * @param con open and valid db connection
     * @return List<ACL>
     * @throws FxLoadException on errors
     */
    List<ACL> loadACLs(Connection con) throws FxLoadException;

    /**
     * Loads all defined Mandators from the given connection.
     * <p/>
     * Mandators are visible to all users, so no security checks are performed.
     * This function does not use any caching.
     *
     * @param con open and valid db connection
     * @return Array containing all known Mandators
     * @throws FxLoadException on errors
     */
    Mandator[] loadMandators(Connection con) throws FxLoadException;

    /**
     * Load all data types from the given connection
     *
     * @param con open and valid db connection
     * @return List<FxDataType>
     * @throws FxLoadException on errors
     */
    List<FxDataType> loadDataTypes(Connection con) throws FxLoadException;

    /**
     * Load all groups from the given connection
     *
     * @param con open and valid db connection
     * @return List<FxDataType>
     * @throws FxLoadException on errors
     */
    List<FxGroup> loadGroups(Connection con) throws FxLoadException;

    /**
     * Load all properties from the given connection
     *
     * @param con         open and valid db connection
     * @param environment bootstraped structure (needs ACL's, FxTypes and FxDataTypes initialized)
     * @return ArrayList<FxDataType>
     * @throws FxLoadException     on errors
     * @throws FxNotFoundException if stored properties are inconstistent in regard to unique mode, etc.
     */
    List<FxProperty> loadProperties(Connection con, FxEnvironment environment) throws FxLoadException, FxNotFoundException;

    /**
     * Load all known types and relations
     *
     * @param con         open and valid db connection
     * @param environment Environment with loaded mandators, acls and workflows
     * @return ArrayList containing all known types and relations
     * @throws FxLoadException on errors
     */
    List<FxType> loadTypes(Connection con, FxEnvironment environment) throws FxLoadException;

    /**
     * Load all known assignments for the given connection
     *
     * @param con         open and valid db connection
     * @param environment Environment for structure and ACL lookups (types, groups, properties)
     * @return FxAssignment's
     * @throws FxLoadException on errors
     */
    List<FxAssignment> loadAssignments(Connection con, FxEnvironment environment) throws FxLoadException;

    /**
     * Load all known Workflows from the given connection
     *
     * @param con         open and valid db connection
     * @param environment needed to lookup referenced steps and routes
     * @return a list of all known workflows
     * @throws FxLoadException if the workflows could not be loaded
     */
    List<Workflow> loadWorkflows(Connection con, FxEnvironment environment) throws FxLoadException;

    /**
     * Load all step definitions from the provided connection
     *
     * @param con open and valid db connection
     * @return a list of all step definitions
     * @throws FxLoadException if the steps could not be loaded
     */
    List<StepDefinition> loadStepDefinitions(Connection con) throws FxLoadException;

    /**
     * Load all steps
     *
     * @param con open and valid db connection
     * @return a list of all steps
     * @throws FxLoadException if the steps could not be loaded
     */
    List<Step> loadSteps(Connection con) throws FxLoadException;

    /**
     * Load all routes for a workflow
     *
     * @param con        open and valid db connection
     * @param workflowId workflow id
     * @return a list of all routes for the given workflow
     * @throws FxLoadException if the routes could not be loaded
     */
    List<Route> loadRoutes(Connection con, int workflowId) throws FxLoadException;

    /**
     * Load all scripts
     *
     * @param con open and valid db connection
     * @return all scripts
     * @throws FxNotFoundException         on errors
     * @throws FxLoadException             on errors
     * @throws FxInvalidParameterException on errors
     */
    List<FxScriptInfo> loadScripts(Connection con) throws FxLoadException, FxNotFoundException, FxInvalidParameterException;

    /**
     * Load all script mappings
     *
     * @param con         open and valid db connection
     * @param environment environment
     * @return all script mappings
     * @throws FxLoadException on errors
     */
    List<FxScriptMapping> loadScriptMapping(Connection con, FxEnvironmentImpl environment) throws FxLoadException;

    /**
     * Load all select lists
     *
     * @param con         open and valid db connection
     * @param environment environment, needs ACL's loaded
     * @return list containing all select lists
     * @throws FxLoadException on errors
     */
    List<FxSelectList> loadSelectLists(Connection con, FxEnvironmentImpl environment) throws FxLoadException;
}
