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
package com.flexive.shared.configuration;

/**
 * Known Database vendors
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public enum DBVendor {
    /** Unknown SQL database vendor. */
    Unknown(0),
    /** MySQL 5+ */
    MySQL(1),
    /** H2: www.h2database.com */
    H2(2);

    private int id;

    /**
     * Create a new vendor.
     * 
     * @param id    the internal DB vendor ID 
     */
    private DBVendor(int id) {
        this.id = id;
    }


    public int getId() {
        return id;
    }

    /**
     * Get enum constant based on the vendor reported by the Connections MetaData
     *
     * @param dbVendor  the database vendor string
     * @return DBVendor the associated vendor enum value
     */
    public static DBVendor getVendor(String dbVendor) {
        if ("MySQL".equals(dbVendor))
            return MySQL;
        else if("H2".equals(dbVendor))
            return H2;
        else
            return Unknown;
    }
}
