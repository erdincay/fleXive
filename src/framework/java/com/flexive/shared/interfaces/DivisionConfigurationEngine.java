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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.FxApplicationException;

import javax.ejb.Remote;


/**
 * Division configuration interface
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface DivisionConfigurationEngine extends GenericConfigurationEngine {

    /**
     * Install a binary file contained in fxresources/binaries in the archive as a binary with the requested id.
     * If an entry with the given id already exists it will be overwritten.
     * This functionality is intended to be used by runone or startup scripts!
     *
     * @param binaryId requested binary id
     * @param resourceName name of the resource file relative to fxresources/binaries
     * @throws FxApplicationException on errors
     */
    void installBinary(long binaryId, String resourceName) throws FxApplicationException;

    /**
     * Perform a database patch if needed
     *
     * @throws FxApplicationException on errors
     */
    void patchDatabase() throws FxApplicationException;

    /**
     * Get information about the used database (name and version)
     *
     * @return information about the used database (name and version)
     */
    String getDatabaseInfo();
}
