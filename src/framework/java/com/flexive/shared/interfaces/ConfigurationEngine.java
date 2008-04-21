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

import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.exceptions.FxApplicationException;

import javax.ejb.Remote;
import java.io.Serializable;


/**
 * Wrapper for accessing parameters transparently with their intrinsic scope.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface ConfigurationEngine extends GenericConfigurationEngine {

    /**
     * <p>Store the value in the source configuration EJB for the given parameter. For example, if
     * a parameter in user scope has a division fallback and is not present for the calling user,
     * the division configuration will be updated.</p>
     * <p>Note that this may require higher privileges than for reading the given parameter,
     * since a fallback configuration (division or global) may be used.</p>
     *
     * @param parameter the parameter to be checked
     * @param key       the key to be used
     * @param value     the value to be stored
     * @throws com.flexive.shared.exceptions.FxApplicationException if the parameter value could not be updated
     */
    <T> void putInSource(Parameter<T> parameter, String key, T value) throws FxApplicationException;

    /**
     * <p>Store the value in the source configuration EJB for the given parameter. For example, if
     * a parameter in user scope has a division fallback and is not present for the calling user,
     * the division configuration will be updated.</p>
     * <p>Note that this may require higher privileges than for reading the given parameter,
     * since a fallback configuration (division or global) may be used.</p>
     *
     * @param parameter the parameter to be checked
     * @param value     the value to be stored
     * @throws com.flexive.shared.exceptions.FxApplicationException if the parameter value could not be updated
     */
    <T> void putInSource(Parameter<T> parameter, T value) throws FxApplicationException;
}
