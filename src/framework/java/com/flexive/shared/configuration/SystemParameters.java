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
package com.flexive.shared.configuration;

import com.flexive.shared.configuration.parameters.ParameterFactory;
import com.flexive.shared.search.ResultPreferences;
import com.flexive.shared.search.query.QueryRootNode;

import java.util.ArrayList;

import static com.flexive.shared.configuration.SystemParameterPaths.*;

/**
 * Flexive system parameter definitions.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SystemParameters {
    /**
     * Detailed information about the execution of run-once scripts
     */
    public static final Parameter<ArrayList> DIVISION_RUNONCE_INFOS = ParameterFactory.newInstance(ArrayList.class, SystemParameterPaths.DIVISION_RUNONCE_CONFIG, "run.once.infos", new ArrayList(0));
    /**
     * Boolean parameter to determine if run-once scripts have been executed
     */
    public static final Parameter<Boolean> DIVISION_RUNONCE = ParameterFactory.newInstance(Boolean.class, SystemParameterPaths.DIVISION_RUNONCE_CONFIG, "run.once", false);
    /**
     * Configuration parameter for storing user content queries assembled in the GUI.
     */
    public static final Parameter<QueryRootNode> USER_QUERIES_CONTENT = ParameterFactory.newInstance(QueryRootNode.class,
            SystemParameterPaths.USER_QUERIES_CONTENT, "", null);
    public static final Parameter<ResultPreferences> USER_RESULT_PREFERENCES = ParameterFactory.newInstance(ResultPreferences.class,
            SystemParameterPaths.USER_RESULT_PREFERENCES, null);
    // global parameters
    /**
     * Root login parameter
     */
    public static final Parameter<String> GLOBAL_ROOT_LOGIN = ParameterFactory.newInstance(String.class, GLOBAL_CONFIG, "root_login", "admin");
    /**
     * Root password parameter
     */
    public static final Parameter<String> GLOBAL_ROOT_PASSWORD = ParameterFactory.newInstance(String.class, GLOBAL_CONFIG, "root_password", "123456");
    /**
     * Division datasource parameter
     */
    public static final Parameter<String> GLOBAL_DATASOURCES = ParameterFactory.newInstance(String.class, GLOBAL_DIVISIONS_DS, "", "");
    /**
     * Division domain matcher parameter
     */
    public static final Parameter<String> GLOBAL_DIVISIONS_DOMAINS = ParameterFactory.newInstance(String.class, SystemParameterPaths.GLOBAL_DIVISIONS_DOMAINS, "", "");
    /**
     * Database version
     */
    public static final Parameter<Long> DB_VERSION = ParameterFactory.newInstance(Long.class, DIVISION_CONFIG, "dbversion", -1L);
    /**
     * Tree Caption property id
     */
    public static final Parameter<Long> TREE_CAPTION_PROPERTY = ParameterFactory.newInstance(Long.class, DIVISION_TREE, "caption", -1L);
    /**
     * Tree Caption root assignment id
     */
    public static final Parameter<Long> TREE_CAPTION_ROOTASSIGNMENT = ParameterFactory.newInstance(Long.class, DIVISION_TREE, "caption", -1L);
    /**
     * Tree checks enabled
     */
    public static final Parameter<Boolean> TREE_CHECKS_ENABLED = ParameterFactory.newInstance(Boolean.class, DIVISION_TREE, "treeChecks", false);
    /**
     * Tree FQN property
     */
    public static final Parameter<Long> TREE_FQN_PROPERTY = ParameterFactory.newInstance(Long.class, DIVISION_TREE, "fqn", -1L);
    /**
     * Tree FQN root assignment id
     */
    public static final Parameter<Long> TREE_FQN_ROOTASSIGNMENT = ParameterFactory.newInstance(Long.class, DIVISION_TREE, "fqn", -1L);
    /**
     * Whether the live tree should be enabled in the backend content tree
     */
    public static final Parameter<Boolean> TREE_LIVE_ENABLED = ParameterFactory.newInstance(Boolean.class, DIVISION_TREE, "liveTree", true);
    /**
     * Whether to store binary transit files in the database or filesystem
     */
    public static final Parameter<Boolean> BINARY_TRANSIT_DB = ParameterFactory.newInstance(Boolean.class, DIVISION_CONFIG, "binaryTransitDB", false);
    /**
     * Size a binary has to exceed to be stored on the filesystem (<0 == always store in database, 0=always store in FS)
     */
    public static final Parameter<Long> BINARY_DB_THRESHOLD = ParameterFactory.newInstance(Long.class, DIVISION_CONFIG, "binaryDBThreshold", 10000L);
    /**
     * Size a preview of a binary has to exceed to be stored on the filesystem (<0 == always store in database, 0=always store in FS)
     */
    public static final Parameter<Long> BINARY_DB_PREVIEW_THRESHOLD = ParameterFactory.newInstance(Long.class, DIVISION_CONFIG, "binaryDBPreviewThreshold", 10000L);
    /**
     * Path on the current nodes filesystem for binary transit files
     */
    public static final Parameter<String> NODE_TRANSIT_PATH = ParameterFactory.newInstance(String.class, NODE_CONFIG, "nodeTransitPath", false, "flexive-storage/transit");
    /**
     * Path on the current nodes filesystem for binary files
     */
    public static final Parameter<String> NODE_BINARY_PATH = ParameterFactory.newInstance(String.class, NODE_CONFIG, "nodeBinaryPath", false, "flexive-storage/binaries");
    /**
     * Download URL for exports
     */
    public static final Parameter<String> EXPORT_DOWNLOAD_URL = ParameterFactory.newInstance(String.class, DIVISION_CONFIG, "exportURL", "http://localhost:8080/flexive/download");
    /**
     * The default input language for multilingual input fields
     */
    public static final Parameter<Long> USER_DEFAULTINPUTLANGUAGE = ParameterFactory.newInstance(Long.class, USER_CONFIG, "input.defaultLanguage", -1L);
    /**
     * Parameter for specifying the URL mapping for the thumbnail servlet. Set this to the prefix mapped to the thumbnail
     * servlet as specified in your application's web.xml.
     */
    public static final Parameter<String> THUMBNAIL_MAPPING
            = ParameterFactory.newInstance(String.class, APPLICATION_CONFIG, "thumbnailMapping", "/thumbnail/");
    /**
     * If the flat storage is enabled, automatically flatten matching assignments
     */
    public static final Parameter<Boolean> FLATSTORAGE_AUTO = ParameterFactory.newInstance(Boolean.class, DIVISION_CONFIG, "flatStorageAuto", Boolean.TRUE);
    
    /**
     * Set the password hash method for the division. Possible values are:
     *
     * <ul>
     * <li>{@code userid}: the default hash includes the user ID for salting (the only method until version 3.1.5).</li>
     * <li>{@code loginname} (default since 3.1.6): use the login name for the password salting, this allows to migrate accounts between
     * different databases (where user IDs might differ, thus invalidating all passwords hashed with the {@code userid} method).</li>
     * </ul>
     */
    public static final Parameter<String> PASSWORD_SALT_METHOD = ParameterFactory.newInstance(String.class, DIVISION_CONFIG, "passwordSaltMethod", "userid");

    /**
     * User specific date format
     */
    public static final Parameter<String> USER_DATEFORMAT = ParameterFactory.newInstance(String.class, SystemParameterPaths.USER_CONFIG_ONLY, "dateFormat", null);

    /**
     * User specific date/time format
     */
    public static final Parameter<String> USER_DATETIMEFORMAT = ParameterFactory.newInstance(String.class, SystemParameterPaths.USER_CONFIG_ONLY, "dateTimeFormat", null);

    /**
     * User specific time format
     */
    public static final Parameter<String> USER_TIMEFORMAT = ParameterFactory.newInstance(String.class, SystemParameterPaths.USER_CONFIG_ONLY, "timeFormat", null);

    /**
     * User specific decimal separator
     */
    public static final Parameter<String> USER_DECIMALSEPARATOR = ParameterFactory.newInstance(String.class, SystemParameterPaths.USER_CONFIG_ONLY, "decSep", null);

    /**
     * User specific grouping separator
     */
    public static final Parameter<String> USER_GROUPINGSEPARATOR = ParameterFactory.newInstance(String.class, SystemParameterPaths.USER_CONFIG_ONLY, "grpSep", null);

    /**
     * User specific flag if to use the grouping separator
     */
    public static final Parameter<Boolean> USER_USEGROUPINGSEPARATOR = ParameterFactory.newInstance(Boolean.class, SystemParameterPaths.USER_CONFIG_ONLY, "useGrpSep", null);

    /**
     * When enabled, the storage engine does not overwrite the creation date (and creator) when creating new content versions.
     * Thus the creation lifecycle data always refers to the very first version of a content, even when the first
     * version is removed from the system (default: false, which is also the behaviour of flexive versions before 3.2.1).
     *
     * @since 3.2.1
     */
    public static final Parameter<Boolean> STORAGE_KEEP_CREATION_DATES = ParameterFactory.newInstance(Boolean.class, DIVISION_CONFIG, "contentVersionKeepCreate", true, false);
}
