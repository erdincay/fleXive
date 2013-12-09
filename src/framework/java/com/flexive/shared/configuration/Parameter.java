/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
 * Configuration parameter interface. 
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @param <T>   parameter value type
 * @see com.flexive.shared.configuration.parameters.ParameterFactory ParameterFactory
 */
public interface Parameter<T> {
	/**
	 * Returns the data object containing path and key for the parameter.
	 * @return	the data object containing path and key for the parameter.
	 */
    ParameterData<T> getData();
    
    /**
     * Convert the given database value (in string representation) to
     * an object of type <code>T</code>.
     * 
     * @param dbValue	the value to be converted
     * @return			dbValue as type <code>T</code>.
     */
    T getValue(Object dbValue);
    
    /**
     * Convert the given value to a string that will be stored in the
     * configuration.
     * 
     * @param value the parameter value to be stored
     * @return  the value's string representation
     * @see Parameter#getValue(Object)
     */
    String getDatabaseValue(T value);
    
    /**
     * Returns true if the given value is a valid configuration value
     * (e.g. primitive parameters may not be null).
     * 
     * @param value the value to be checked
     * @return  true if the given value is a valid configuration value
     */
    boolean isValid(T value);
    
    /**
     * Return the parameter's default value or null if it is not set.
     * @return  the parameter's default value or null if it is not set.
     */
    T getDefaultValue();
    
    /**
     * Return the parameter's path (including its scope).
     * @return  the parameter's path (including its scope).
     */
    ParameterPath getPath();

    /**
     * Return the parameter's scope.
     * @return  the parameter's scope.
     */
    ParameterScope getScope();

    /**
     * Should this parameter be cached once it has been retrieved? By default all parameters are cached, but
     * you can disable this for individual parameters that are seldom used or very large.
     *
     * @return  true if this parameter should be cached. If false, each lookup is performed directly on the
     * database.
     * @since   3.1.4
     */
    boolean isCached();
    
    /**
     * Return the parameter's key.
     * @return  the parameter's key.
     */
    String getKey();

    /**
     * Sets the data for this parameter. Only works if the parameter instance
     * is not frozen yet (see {@link #freeze()}).
     *
     * @param data  the parameter data beans
     * @return this
     */
    Parameter<T> setData(ParameterData<T> data);

    /**
     * Freeze the parameter configuration. The parameter data cannot be modified
     * on this instance after calling this method.
     *
     * @return  this
     */
    Parameter<T> freeze();

    /**
     * Return a copy of this parameter instance. If this instance was frozen, the
     * new object will be "unfrozen". The {@link ParameterData} instance will not be
     * cloned, however you can replace it with a new instance, e.g. to update the parameter key.
     *
     * @return  a copy of this parameter instance
     */
    Parameter<T> copy();
}
