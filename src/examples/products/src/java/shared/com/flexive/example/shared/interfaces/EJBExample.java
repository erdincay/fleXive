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
package com.flexive.example.shared.interfaces;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxType;

import javax.ejb.Remote;
import java.util.Map;

/**
 * <p>A demo interface for an EJB bean.</p>
 * <p>
 * This EJB engine provides a method that returns the instance counts by type for the
 * current installation.
 * </p>
 * <p>
 * The common interface should always be marked as a Remote interface, since this is more
 * portable across application servers for classes that are not using EJB dependency injection.
 * </p>
 * <p>
 * The easiest way to do this is to extend the remote interface and tag it with the @Local annotation.
 * Flexive's {@link com.flexive.shared.EJBLookup EJBLookup} automatically uses the local interface
 * if it's available.
 * </p>
 */
@Remote
public interface EJBExample {

    /**
     * Return the instance counts for all registered {@link FxType FxTypes}.
     *
     * @return the instance counts for all registered {@link FxType FxTypes}.
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          if the instance counts could not be determined
     */
    Map<FxType, Integer> getInstanceCounts() throws FxApplicationException;
}
