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
package com.flexive.chemistry.webdav.extensions;

import org.apache.chemistry.Connection;
import org.apache.chemistry.NameConstraintViolationException;
import org.apache.chemistry.ObjectId;
import org.apache.chemistry.UpdateConflictException;

/**
 * An extension for Chemistry to allow efficient copying of resources.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public interface CopyDocumentExtension {

    /**
     * Create an independent copy of the object with ID {@code id}.
     *
     * @param conn  the repository connection
     * @param id    the object ID
     * @param targetFolder  the target folder
     * @param newName   the name in the target folder
     * @param overwrite if an existing object with the same name should be overwritten
     * @parma shallow   if only the object should be copied (meaningful only for folders), or if the entire tree
     * should be cloned
     * @return      an independent copy of the object
     */
    void copy(Connection conn, ObjectId id, ObjectId targetFolder, String newName, boolean overwrite, boolean shallow) throws UpdateConflictException, NameConstraintViolationException;
    
}
