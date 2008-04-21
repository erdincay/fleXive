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
package com.flexive.example.war;

import com.flexive.example.shared.interfaces.EJBExample;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxType;
import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.SystemBean;

import java.util.Map;

/**
 * A JSF managed bean providing cached access to an EJB method.
 */
public class ManagedExampleBean {
    private Map<FxType, Integer> instanceCounts;

    public Map<FxType, Integer> getInstanceCounts() throws FxApplicationException {
        if (instanceCounts == null) {
            instanceCounts = EJBLookup.getEngine(EJBExample.class).getInstanceCounts();
        }
        return instanceCounts;
    }

    public String getBuildInfo() {
        // managed bean lookup, asserts that the JSF core components are available in the build path  
        return FxJsfUtils.getManagedBean(SystemBean.class).getBuildInfo();
    }
}
