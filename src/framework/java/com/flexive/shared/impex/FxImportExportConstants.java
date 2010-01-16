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
package com.flexive.shared.impex;

/**
 * Constants used for import/export
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface FxImportExportConstants {
    /**
     * folder for filesystem binaries within the zip archive
     */
    public final static String FOLDER_FS_BINARY = "fsbinary";
    /**
     * folder for database binaries within the zip archive
     */
    public final static String FOLDER_BINARY = "binary";

    //file names to store exported data in
    public static final String FILE_LANGUAGES = "languages.xml";
    public static final String FILE_MANDATORS = "mandators.xml";
    public static final String FILE_SECURITY = "security.xml";
    public static final String FILE_WORKFLOWS = "workflows.xml";
    public static final String FILE_CONFIGURATIONS = "configurations.xml";
    public static final String FILE_STRUCTURES = "structures.xml";
    public static final String FILE_TREE = "tree.xml";
    public static final String FILE_BRIEFCASES = "briefcases.xml";
    public static final String FILE_SCRIPTS = "scripts.xml";
    public static final String FILE_HISTORY = "history.xml";
    public static final String FILE_FLATSTORAGE_META = "flatstorage.xml";
    public static final String FILE_BINARIES = "binaries.xml";
    public static final String FILE_DATA_HIERARCHICAL = "data_hierarchical.xml";
    public static final String FILE_DATA_FLAT = "data_flat.xml";
    public static final String FILE_SEQUENCERS = "sequencers.xml";
    public static final String FILE_BUILD_INFOS = "flexive.xml";
}
