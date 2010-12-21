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
package com.flexive.core.storage.MySQL;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.storage.GenericDivisionImporter;
import com.flexive.shared.impex.FxDivisionExportInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * MySQL specific implementation of the GenericDivisionImporter
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class MySQLDivisionImporter extends GenericDivisionImporter {

    private static final Log LOG = LogFactory.getLog(MySQLDivisionImporter.class);

    @SuppressWarnings({"FieldCanBeLocal"})
    private static MySQLDivisionImporter INSTANCE = new MySQLDivisionImporter();

    /**
     * Getter for the importer singleton
     *
     * @return exporter
     */
    public static MySQLDivisionImporter getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean importRequiresNonTXConnection() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean canImportFlatStorages(FxDivisionExportInfo exportInfo) {
        //only MySQL flatstorages can be imported
        return exportInfo.getDatabaseInfo().toLowerCase().indexOf("mysql") >= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importStructures(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_STRUCTURES);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "structures/selectlists/list", DatabaseConst.TBL_STRUCT_SELECTLIST);
            importTable(stmt, zip, ze, "structures/selectlists/list_t", DatabaseConst.TBL_STRUCT_SELECTLIST + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/datatypes/type", DatabaseConst.TBL_STRUCT_DATATYPES);
            importTable(stmt, zip, ze, "structures/datatypes/type_t", DatabaseConst.TBL_STRUCT_DATATYPES + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/types/tdef", DatabaseConst.TBL_STRUCT_TYPES, true, false, "icon_ref");
            importTable(stmt, zip, ze, "structures/types/tdef_t", DatabaseConst.TBL_STRUCT_TYPES + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/types/trel", DatabaseConst.TBL_STRUCT_TYPERELATIONS);
            importTable(stmt, zip, ze, "structures/types/topts", DatabaseConst.TBL_STRUCT_TYPES_OPTIONS);
            importTable(stmt, zip, ze, "structures/properties/property", DatabaseConst.TBL_STRUCT_PROPERTIES);
            importTable(stmt, zip, ze, "structures/properties/property_t", DatabaseConst.TBL_STRUCT_PROPERTIES + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/groups/group", DatabaseConst.TBL_STRUCT_GROUPS);
            importTable(stmt, zip, ze, "structures/groups/group_t", DatabaseConst.TBL_STRUCT_GROUPS + DatabaseConst.ML);
            //mysql does not need special update logic since disabling referential integrity actually works compared to other databases ...
            importTable(stmt, zip, ze, "structures/assignments/assignment", DatabaseConst.TBL_STRUCT_ASSIGNMENTS);
            importTable(stmt, zip, ze, "structures/assignments/assignment_t", DatabaseConst.TBL_STRUCT_ASSIGNMENTS + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/selectlists/item", DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM);
            importTable(stmt, zip, ze, "structures/selectlists/item_t", DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/groups/goption", DatabaseConst.TBL_STRUCT_GROUP_OPTIONS);
            importTable(stmt, zip, ze, "structures/properties/poption", DatabaseConst.TBL_STRUCT_PROPERTY_OPTIONS);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }


}
