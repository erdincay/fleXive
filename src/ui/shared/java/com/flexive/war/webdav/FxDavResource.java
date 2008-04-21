/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.war.webdav;

import java.util.Date;

/**
 * A Dav entry with a content length
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxDavResource extends FxDavEntry {

    /**
     * Consructor.
     *
     * @param displayname   the name of the resource
     * @param creationdate  the creation date
     * @param lastmodified  the last modofication date
     * @param contentlength the size of the resource in bytes.
     */
    public FxDavResource(String displayname, Date creationdate, Date lastmodified, long contentlength) {
        super(false, creationdate, displayname, lastmodified, contentlength);
    }

    /**
     * Returns the size of the resource in bytes.
     *
     * @return the size of the resource in bytes
     */
    public long getContentlength() {
        return contentlength;
    }

}
