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
import com.flexive.shared.exceptions.*;
import com.flexive.shared.Pair;

import java.util.Collection;
import java.util.Map;

/**
 * Generic configuration interface. Provides a generic interface for
 * configuration methods based on the <code>Parameter</code> interface. 
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 *
 * @see com.flexive.ejb.beans.configuration.GenericConfigurationImpl
 * @see com.flexive.shared.configuration.Parameter
 */

public abstract interface GenericConfigurationEngine {
	/**
	 * Set a parameter containing a value of type <code>T</code>. 
	 * 
	 * @param <T>		value type to be set (e.g. String, Integer, ...)
	 * @param parameter	parameter data containing path and key
	 * @param value		value to be set
	 * @throws FxApplicationException TODO
	 * @throws FxUpdateException	if the value could not be set or updated
     * @throws FxNoAccessException  if the caller is not allowed to update/set this parameter
	 */
	<T> void put(Parameter<T> parameter, T value)
    throws FxApplicationException;
	
	/**
	 * Set a parameter containing a value of type <code>T</code> using
	 * the given key. This method is useful for setting aggregate parameters with
	 * varying keys.
	 * Use <code>getParameters(Parameter&lt;T&g;t parameter)</code> to retrieve a 
	 * map with all keys stored under the path of the given parameter.
	 * 
	 * @param <T>		value type to be set
	 * @param parameter	parameter data containing the path
	 * @param key		key to be used for this value
	 * @param value		value to be set
	 * @throws FxApplicationException TODO
	 * @throws FxUpdateException	if the value could not be updated
     * @throws FxNoAccessException  if the caller is not allowed to update/set this parameter
	 */
	<T> void put(Parameter<T> parameter, String key, T value)
    throws FxApplicationException;
	
	/**
	 * Retrieves the value of the given parameter.
	 * 
	 * @param <T>		value type of the parameter
	 * @param parameter	parameter data containing the path and key
	 * @return			the value stored under the given path and key
	 * @throws FxApplicationException TODO
	 * @throws FxLoadException		if the value could not be loaded 
	 * @throws FxNotFoundException	if the parameter is not set
	 */
	<T> T get(Parameter<T> parameter) throws FxApplicationException;
	
	/**
	 * Retrieves the value for the given parameter using the given key. This method is
	 * useful for getting a single value of aggregate parameters. When all values
	 * for an aggregate parameter should be retrieved, use <code>getParameters</code> instead.
	 * 
	 * @param <T>		value type of the parameter
	 * @param parameter	parameter data containing the path
	 * @param key		key to be used
	 * @return			the value stored under the given path and key
	 * @throws FxApplicationException TODO
	 * @throws FxLoadException		if the value could not be loaded
	 * @throws FxNotFoundException	if the parameter is not set
	 */
	<T> T get(Parameter<T> parameter, String key) throws FxApplicationException;

    /**
     * Retrieves the value for the given parameter using the given key. This method is
     * useful for getting a single value of aggregate parameters. When all values
     * for an aggregate parameter should be retrieved, use <code>getParameters</code> instead.
     * The default value may be disabled through the <code>ignoreDefault</code> parameter,
     * making this method more useful for checking if a parameter exists.
     * 
     * @param <T>       value type of the parameter
     * @param parameter parameter data containing the path
     * @param key       key to be used
     * @param ignoreDefault if the parameter's default value should be used default 
     * @return          the value stored under the given path and key
     * @throws FxApplicationException TODO
     * @throws FxLoadException      if the value could not be loaded
     * @throws FxNotFoundException  if the parameter is not set
     */
    <T> T get(Parameter<T> parameter, String key, boolean ignoreDefault)
        throws FxApplicationException;

    /**
     * Try to fetch the given parameter. Returns (true, [value]) when the parameter was found
     * (value can be null), (false, null) otherwise.
     * <p/>
     * This is mainly a performance optimization for {@link ConfigurationEngine} to avoid
     * throwing (possibly expensive) application exceptions from EJB calls.  
     *
     * @param parameter the parameter
     * @param ignoreDefault
     * @return  true if the configuration contains a value for the given parameter.
     * @since 3.1
     */
    <T> Pair<Boolean, T> tryGet(Parameter<T> parameter, String key, boolean ignoreDefault);

	/**
	 * Retrieves all key/value pairs stored under the path of the given parameter.
	 * 
	 * @param <T>		value type of the parameter
	 * @param parameter	parameter data containing the path
	 * @return			all key/value-pairs under the given path
	 * @throws FxApplicationException TODO
	 * @throws FxLoadException	when an error occured reading the keys
	 */
	<T> Map<String, T> getAll(Parameter<T> parameter) throws FxApplicationException;

    /**
     * Retrieves all keys stored under the path of the given parameter.
     *
     * @param <T>		value type of the parameter
     * @param parameter parameter containing the path
     * @return  all keys stored under the path of the given parameter.
     * @throws FxApplicationException   TODO
     */
    <T> Collection<String> getKeys(Parameter<T> parameter) throws FxApplicationException;
    
    /**
     * Removes a parameter from the database.
     * @param <T>   value type of the parameter
     * @param parameter parameter to be removed
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxRemoveException    if the parameter could not be removed
     * @throws FxNoAccessException  if the caller is not allowed to remove this parameter
     */
    <T> void remove(Parameter<T> parameter) throws FxApplicationException;

    /**
     * Removes a parameter from the database using the given key.
     * @param <T>   value type of the parameter
     * @param parameter parameter containing the path to be removed
     * @param key   the key to be removed
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxRemoveException    if the parameter could not be removed
     * @throws FxNoAccessException  if the caller is not allowed to remove this parameter
     */
    <T> void remove(Parameter<T> parameter, String key) throws FxApplicationException;

    /**
     * Remove all parameters stored under this parameter's path.
     * @param <T>   value type of the parameter
     * @param parameter the parameter to be removed
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxRemoveException    if the parameters could not be removed
     * @throws FxNoAccessException  if the caller is not allowed to remove this parameter
     */
    <T> void removeAll(Parameter<T> parameter) throws FxApplicationException;

}
