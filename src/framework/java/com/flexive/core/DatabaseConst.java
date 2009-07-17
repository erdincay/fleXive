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
package com.flexive.core;

/**
 * Database constants.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public final class DatabaseConst {

    /**
     * Database vendors
     */
    public static enum Dialect {
        /**
         * MySQL (>= 5)
         */
        MySQL,
        /**
         * Oracle
         */
        Oracle
    }

    private static final String DEFAULT_CONFIGSCHEMA = "flexiveConfiguration";
    /**
     * Datasource for the global configuration
     */
    public static final String DS_GLOBAL_CONFIG = "jdbc/flexiveConfiguration";

    private static String configSchema = null;

    // Table/Sequencer definitions
    /**
     * Search cache table
     */
    public static final String TBL_BRIEFCASE = "FXS_BRIEFCASE";
    public static final String TBL_BRIEFCASE_DATA = "FXS_BRIEFCASE_DATA";
    public static final String TBL_SEARCHCACHE_MEMORY = "FXS_SEARCHCACHE_MEMORY";
    public static final String TBL_SEARCHCACHE_PERM = "FXS_SEARCHCACHE_PERM";

    /**
     * Search cache table
     */
    public static final String TBL_SEARCH_CACHE = "FXS_SEARCH_CACHE";
    /**
     * Search cache data table
     */
    public static final String TBL_SEARCH_CACHE_DATA = "FXS_SEARCH_CACHE_DATA";
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
    public static final String TBL_ASSIGN_ROLES = "FXS_ROLEMAPPING";
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
    public static final String TBL_ASSIGN_ACLS = "FXS_ACLASSIGNMENTS";
    /**
     * User roles definitions
     */
    public static final String TBL_ROLES = "FXS_ROLES";
    /**
     * Workflow step definitions table
     */
    public static final String TBL_STEPDEFINITION = "FXS_WF_STEPDEFS";
    /**
     * Workflow steps table
     */
    public static final String TBL_STEP = "FXS_WF_STEPS";
    /**
     * Workflow table
     */
    public static final String TBL_WORKFLOW = "FXS_WORKFLOWS";
    /**
     * Workflow routes table
     */
    public static final String TBL_ROUTES = "FXS_WF_ROUTES";
    /**
     * User groups table
     */
    public static final String TBL_GROUP = "FXS_USERGROUPS";
    /**
     * Mandator definitions table
     */
    public static final String TBL_MANDATORS = "FXS_MANDATOR";
    /**
     * Instance lock table
     */
    public static final String TBL_LOCK = "FXS_LOCK";
    /**
     * Global configuration table
     */
    public static final String TBL_GLOBAL_CONFIG = getConfigSchema() + "FXS_CONFIGURATION";
    /**
     * User configuration table
     */
    public static final String TBL_USER_CONFIG = "FXS_USERCONFIGURATION";
    /**
     * Division configuration table
     */
    public static final String TBL_DIVISION_CONFIG = "FXS_DIVISIONCONFIGURATION";
    /**
     * Application configuration table
     */
    public static final String TBL_APPLICATION_CONFIG = "FXS_APPLICATIONCONFIGURATION";
    /**
     * Node configuration table
     */
    public static final String TBL_NODE_CONFIG = "FXS_NODECONFIGURATION";
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
     * Structure properties table
     */
    public static final String TBL_STRUCT_PROPERTIES = "FXS_TYPEPROPS";
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
     * Structure flat storeage mapping table
     */
    public static final String TBL_STRUCT_FLATSTORE_MAPPING = "FXS_FLAT_MAPPING";
    /**
     * Structure flat storage info table
     */
    public static final String TBL_STRUCT_FLATSTORE_INFO = "FXS_FLAT_STORAGES";
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
     * Content binary table
     */
    public static final String TBL_CONTENT_BINARY = "FX_BINARY";
    /**
     * Binary transit table
     */
    public static final String TBL_BINARY_TRANSIT = "FXS_BINARY_TRANSIT";
    /**
     * ACL table for contents with multiple ACLs.
     * @since 3.1
     */
    public static final String TBL_CONTENT_ACLS = "FX_CONTENT_ACLS";
    /**
     * Script table
     */
    public static final String TBL_SCRIPTS = "FXS_SCRIPTS";
    /**
     * Script mapping table for assignments
     */
    public static final String TBL_SCRIPT_MAPPING_ASSIGN = "FXS_SCRIPT_ASS_MAPPING";
    /**
     * Script mapping table for types
     */
    public static final String TBL_SCRIPT_MAPPING_TYPES = "FXS_SCRIPT_TYPE_MAPPING";
    /**
     * Select lists table
     */
    public static final String TBL_SELECTLIST = "FXS_SELECTLIST";
    /**
     * Select list items table
     */
    public static final String TBL_SELECTLIST_ITEM = "FXS_SELECTLIST_ITEM";
    /**
     * Template table
     */
    public static final String TBL_TEMPLATE = "FXS_TEMPLATE";
    /**
     * Tag relation table
     */
    public static final String TBL_TAG_RELATIONS = "FXS_TAG_RELATIONS";
    /**
     * Tree Base table
     */
    public static final String TBL_TREE = "FXS_TREE";
    
    public static final String TBL_PROPERTY_OPTIONS = "FXS_PROP_OPT";
    public static final String TBL_GROUP_OPTIONS = "FXS_GROUP_OPT";

    /**
     * General table name extension for multilingual tables
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
            if (!configSchema.endsWith("."))
                configSchema += ".";
            if (".".equals(configSchema)) configSchema = "";
            return configSchema;
        }
    }

}
