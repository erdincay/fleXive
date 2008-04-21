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
package com.flexive.example.ejb;

import com.flexive.example.shared.interfaces.EJBExample;
import com.flexive.example.shared.interfaces.EJBExampleLocal;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxFoundType;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxType;

import javax.ejb.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A demo EJB bean.
 */
@Stateless(name = "EJBExample")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class EJBExampleBean implements EJBExample, EJBExampleLocal {

    /**
     * {@inheritDoc}
     */
    public Map<FxType, Integer> getInstanceCounts() throws FxApplicationException {
        final Map<FxType, Integer> result = new HashMap<FxType, Integer>();
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        for (FxFoundType foundType : new SqlQueryBuilder().select("@pk").getResult().getContentTypes()) {
            result.put(environment.getType(foundType.getContentTypeId()), foundType.getFoundEntries());
        }
        return result;
    }
}
