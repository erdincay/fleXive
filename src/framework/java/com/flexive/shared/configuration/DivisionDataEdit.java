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

import java.util.regex.Pattern;

/**
 * Editable division data class, used for division data setup forms.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class DivisionDataEdit extends DivisionData {
    private static final long serialVersionUID = 9017722116723955308L;

    /**
     * Create an editable division data object that is independent from its source object.
     *
     * @param data  the source division data.
     */
    public DivisionDataEdit(DivisionData data) {
        super(data.id, data.available, data.dataSource, data.domainRegEx, data.dbVendor, data.dbVersion);
    }


    public void setId(int id) {
        this.id = id;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public void setDomainRegEx(String domainRegEx) {
        this.domainPattern = Pattern.compile(domainRegEx);
        this.domainRegEx = domainRegEx; // update regex only if it successfully compiles
    }

    public void setDbVendor(DBVendor dbVendor) {
        this.dbVendor = dbVendor;
    }

    public void setDbVersion(String dbVersion) {
        this.dbVersion = dbVersion;
    }

}
