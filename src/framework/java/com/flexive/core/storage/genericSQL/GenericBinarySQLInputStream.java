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
package com.flexive.core.storage.genericSQL;

import com.flexive.core.Database;
import com.flexive.core.storage.binary.BinaryInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * BinaryInputStream implementation for generic databases (to close the connection)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class GenericBinarySQLInputStream extends BinaryInputStream {
    private Connection con;
    private PreparedStatement ps;

    /**
     * Ctor
     *
     * @param con         connection to be closed on #close()
     * @param ps          prepared statement to be closed on #close()
     * @param binaryFound binary was found?
     * @param stream      binary stream
     * @param mimeType    mimeType
     * @param size        size
     */
    public GenericBinarySQLInputStream(Connection con, PreparedStatement ps, boolean binaryFound, InputStream stream, String mimeType, int size) {
        super(binaryFound, stream, mimeType, size);
        this.con = con;
        this.ps = ps;
    }

    /**
     * Ctor if the requested binary was not found
     *
     * @param binaryFound binary found
     */
    public GenericBinarySQLInputStream(boolean binaryFound) {
        super(false, null, null, 0);
        this.con = null;
        this.ps = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        super.close();
        Database.closeObjects(GenericBinarySQLInputStream.class, con, ps);
    }
}
