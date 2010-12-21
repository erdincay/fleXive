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
    String FOLDER_FS_BINARY = "fsbinary";
    /**
     * folder for database binaries within the zip archive
     */
    String FOLDER_BINARY = "binary";
    //file names to store exported data in
    String FILE_LANGUAGES = "languages.xml";
    String FILE_MANDATORS = "mandators.xml";
    String FILE_SECURITY = "security.xml";
    String FILE_WORKFLOWS = "workflows.xml";
    String FILE_CONFIGURATIONS = "configurations.xml";
    String FILE_STRUCTURES = "structures.xml";
    String FILE_TREE = "tree.xml";
    String FILE_BRIEFCASES = "briefcases.xml";
    String FILE_SCRIPTS = "scripts.xml";
    String FILE_HISTORY = "history.xml";
    String FILE_RESOURCES = "resources.xml";
    String FILE_FLATSTORAGE_META = "flatstorage.xml";
    String FILE_BINARIES = "binaries.xml";
    String FILE_DATA_HIERARCHICAL = "data_hierarchical.xml";
    String FILE_DATA_FLAT = "data_flat.xml";
    String FILE_SEQUENCERS = "sequencers.xml";
    String FILE_BUILD_INFOS = "flexive.xml";

    /** JDK6: java.sql.Types.LONGNVARCHAR */
    int SQL_LONGNVARCHAR = -16;
    /** JDK6: java.sql.Types.NCHAR: */
    int SQL_NCHAR = -15;
    /** JDK6: java.sql.Types.NCLOB: */
    int SQL_NCLOB = 2011;
    /** JDK6: java.sql.Types.NVARCHAR: */
    int SQL_NVARCHAR = -9;          
}
