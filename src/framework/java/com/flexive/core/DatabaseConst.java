/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Database constants.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public final class DatabaseConst {

    private static final String DEFAULT_CONFIGSCHEMA = "flexiveConfiguration";
    /**
     * Datasource for the global configuration
     */
    public static final String DS_GLOBAL_CONFIG = "jdbc/flexiveConfiguration";
    /**
     * Name of the configuration schema
     */
    private static String configSchema = null;

    /**
     * Briefcase table
     */
    public static final String TBL_BRIEFCASE = "FXS_BRIEFCASE";
    /**
     * Briefcase data table
     */
    public static final String TBL_BRIEFCASE_DATA = "FXS_BRIEFCASE_DATA";
    /**
     * Briefcase item meta data table
     */
    public static final String TBL_BRIEFCASE_DATA_ITEM = "FXS_BRIEFCASE_DATA_ITEM";
    /**
     * In-memory search cache
     */
    public static final String TBL_SEARCHCACHE_MEMORY = "FXS_SEARCHCACHE_MEMORY";
    /**
     * Permanent search cache
     */
    public static final String TBL_SEARCHCACHE_PERM = "FXS_SEARCHCACHE_PERM";
    /**
     * Accounts table
     */
    public static final String TBL_ACCOUNTS = "FXS_ACCOUNTS";
    /**
     * Account details table
     */
    public static final String TBL_ACCOUNT_DETAILS = "FXS_ACCOUNT_DETAILS";
    /**
     * User groups to accounts table
     */
    public static final String TBL_ASSIGN_GROUPS = "FXS_USERGROUPMEMBERS";
    /**
     * Roles to accounts table
     */
    public static final String TBL_ROLE_MAPPING = "FXS_ROLEMAPPING";
    /**
     * History tracker table
     */
    public static final String TBL_HISTORY = "FXS_HISTORY";
    /**
     * ACL table
     */
    public static final String TBL_ACLS = "FXS_ACL";
    /**
     * ACL to usergroups table
     */
    public static final String TBL_ACLS_ASSIGNMENT = "FXS_ACLASSIGNMENTS";
    
    /**
     * Workflow step definitions table
     */
    public static final String TBL_WORKFLOW_STEPDEFINITION = "FXS_WF_STEPDEFS";
    /**
     * Workflow steps table
     */
    public static final String TBL_WORKFLOW_STEP = "FXS_WF_STEPS";
    /**
     * Workflow table
     */
    public static final String TBL_WORKFLOW = "FXS_WORKFLOWS";
    /**
     * Workflow routes table
     */
    public static final String TBL_WORKFLOW_ROUTES = "FXS_WF_ROUTES";
    /**
     * User groups table
     */
    public static final String TBL_USERGROUPS = "FXS_USERGROUPS";
    /**
     * Mandator definitions table
     */
    public static final String TBL_MANDATORS = "FXS_MANDATOR";
    /**
     * Instance lock table
     */
    public static final String TBL_LOCKS = "FXS_LOCK";
    /**
     * Global configuration table
     */
    public static final String TBL_CONFIG_GLOBAL = "FXS_CONFIGURATION";
    /**
     * User configuration table
     */
    public static final String TBL_CONFIG_USER = "FXS_USERCONFIGURATION";
    /**
     * Division configuration table
     */
    public static final String TBL_CONFIG_DIVISION = "FXS_DIVISIONCONFIGURATION";
    /**
     * Application configuration table
     */
    public static final String TBL_CONFIG_APPLICATION = "FXS_APPLICATIONCONFIGURATION";
    /**
     * Mandator configuration table
     * @since   3.1.6
     */
    public static final String TBL_CONFIG_MANDATOR = "FXS_MANDATORCONFIGURATION";
    /**
     * Node configuration table
     */
    public static final String TBL_CONFIG_NODE = "FXS_NODECONFIGURATION";
    /**
     * Language definition table
     */
    public static final String TBL_LANG = "FXS_LANG";
    /**
     * Data types table
     */
    public static final String TBL_STRUCT_DATATYPES = "FXS_DATATYPES";
    /**
     * Structure groups table
     */
    public static final String TBL_STRUCT_GROUPS = "FXS_TYPEGROUPS";
    /**
     * Group options
     */
    public static final String TBL_STRUCT_GROUP_OPTIONS = "FXS_GROUP_OPT";
    /**
     * Structure properties table
     */
    public static final String TBL_STRUCT_PROPERTIES = "FXS_TYPEPROPS";
    /**
     * Property options
     */
    public static final String TBL_STRUCT_PROPERTY_OPTIONS = "FXS_PROP_OPT";
    /**
     * Structure type assignments table
     */
    public static final String TBL_STRUCT_ASSIGNMENTS = "FXS_ASSIGNMENTS";
    /**
     * Structure types table
     */
    public static final String TBL_STRUCT_TYPES = "FXS_TYPEDEF";
    /**
     * Structure relations table
     */
    public static final String TBL_STRUCT_TYPERELATIONS = "FXS_TYPERELS";
    /**
     * Type options
     */
    public static final String TBL_STRUCT_TYPES_OPTIONS = "FXS_TYPE_OPT";
    /**
     * Structure flat storeage mapping table
     */
    public static final String TBL_STRUCT_FLATSTORE_MAPPING = "FXS_FLAT_MAPPING";
    /**
     * Structure flat storage info table
     */
    public static final String TBL_STRUCT_FLATSTORE_INFO = "FXS_FLAT_STORAGES";
    /**
     * Select lists table
     */
    public static final String TBL_STRUCT_SELECTLIST = "FXS_SELECTLIST";
    /**
     * Select list items table
     */
    public static final String TBL_STRUCT_SELECTLIST_ITEM = "FXS_SELECTLIST_ITEM";
    /**
     * Main content table
     */
    public static final String TBL_CONTENT = "FX_CONTENT";
    /**
     * Content data table
     */
    public static final String TBL_CONTENT_DATA = "FX_CONTENT_DATA";
    /**
     * Fulltext mirror table
     */
    public static final String TBL_CONTENT_DATA_FT = "FX_CONTENT_DATA_FT";
    /**
     * ACL table for contents with multiple ACLs.
     * @since 3.1
     */
    public static final String TBL_CONTENT_ACLS = "FX_CONTENT_ACLS";
    /**
     * Content binary table
     */
    public static final String TBL_CONTENT_BINARY = "FX_BINARY";
    /**
     * Binary transit table
     */
    public static final String TBL_BINARY_TRANSIT = "FXS_BINARY_TRANSIT";
    /**
     * Script table
     */
    public static final String TBL_SCRIPTS = "FXS_SCRIPTS";
    /**
     * Script schedule table
     * @since 3.1.2
     */
    public static final String TBL_SCRIPT_SCHEDULES = "FXS_SCRIPT_SCHEDULES";
    /**
     * Script mapping table for assignments
     */
    public static final String TBL_SCRIPT_MAPPING_ASSIGN = "FXS_SCRIPT_ASS_MAPPING";
    /**
     * Script mapping table for types
     */
    public static final String TBL_SCRIPT_MAPPING_TYPES = "FXS_SCRIPT_TYPE_MAPPING";
    /**
     * Tree Base table
     */
    public static final String TBL_TREE = "FXS_TREE";
    /**
     * Resource table
     */
    public static final String TBL_RESOURCES = "FX_RES";
    /**
     * Phrase table
     */
    public static final String TBL_PHRASE = "FX_PHRASE";
    /**
     * Phrase values table
     */
    public static final String TBL_PHRASE_VALUES = "FX_PHRASE_VAL";
    /**
     * Phrase tree table
     */
    public static final String TBL_PHRASE_TREE = "FX_PHRASE_TREE";
    /**
     * Phrase mapping table
     */
    public static final String TBL_PHRASE_MAP = "FX_PHRASE_MAP";

    /**
     * Lists all tables that keep user-based lifecycle information.
     *
     * @see com.flexive.shared.security.LifeCycleInfo
     */
    public static final List<String> TABLES_WITH_LCI = Collections.unmodifiableList(Arrays.asList(
            TBL_CONTENT, TBL_MANDATORS, TBL_ACCOUNTS, TBL_USERGROUPS, TBL_ACLS, TBL_ACLS_ASSIGNMENT,
            TBL_STRUCT_TYPES, TBL_STRUCT_SELECTLIST_ITEM, TBL_BRIEFCASE
    ));

    /**
     * General table name extension for translation tables
     */
    public static final String ML = "_T";

    /**
     * Private default constructor to prevent instantion.
     */
    private DatabaseConst() {
        // empty default constructor
    }

    /**
     * Returns the database configuration schema to use.
     *
     * @return the database configuration schema to use
     */
    public static String getConfigSchema() {
        if (configSchema != null) {
            return configSchema;
        }
        synchronized (DatabaseConst.class) {
            configSchema = System.getProperty("FxConfigSchema");
            if (configSchema == null)
                configSchema = DEFAULT_CONFIGSCHEMA;
            else {
                System.out.println("[Database] ConfigSchema set to [" + configSchema + "]");
            }
            if (configSchema.endsWith(".")) //remove trailing "."
                configSchema = configSchema.substring(0, configSchema.length() - 1);
            if (".".equals(configSchema)) configSchema = "";
            return configSchema;
        }
    }

}
