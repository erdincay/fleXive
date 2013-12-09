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
package com.flexive.shared.interfaces;

import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.ParameterData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.thoughtworks.xstream.XStream;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Extension of {@link GenericConfigurationEngine} configurations with an arbitrary domain field
 * (e.g. user ID).
 * <p/>
 * When using the methods of {@link GenericConfigurationEngine}, the domain is automatically determined
 * (e.g. the current user ID). This interface adds methods to store parameters for a specific domain that
 * is not the own. For these methods, additional access restraints apply. Usually the calling user must be
 * a global supervisors to read or update any foreign domain.
 *
 * @param <T> The ID type
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public interface CustomDomainConfigurationEngine<T extends Serializable> extends GenericConfigurationEngine {

    /**
     * Retrieves all domains that have configuration entries.
     *
     * @return  all domains that have configuration entries.
     * @throws FxApplicationException   on errors
     * @throws com.flexive.shared.exceptions.FxNoAccessException    if the caller may not retrieve the list of domains
     */
    List<T> getDomains() throws FxApplicationException;

    /**
     * Retrieves the domains that have configuration entries for the given parameter.
     *
     * @param parameter	parameter data containing the path and key
     * @return			the domains that have configuration entries for the given parameter.
     * @throws com.flexive.shared.exceptions.FxApplicationException on errors
     * @throws com.flexive.shared.exceptions.FxLoadException        if the domains could not be loaded
     * @throws com.flexive.shared.exceptions.FxNoAccessException    if the caller may not retrieve the list of domains
     */
    List<T> getDomains(Parameter parameter) throws FxApplicationException;

    /**
     * Retrieves the domains that have configuration entries for the given parameter.
     *
     * @param parameter	parameter data containing the path
     * @param key       the parameter key to be used. If null, all keys in the parameter path will be included.
     * @return			the domains that have configuration entries for the given parameter.
     * @throws com.flexive.shared.exceptions.FxApplicationException on errors
     * @throws com.flexive.shared.exceptions.FxLoadException        if the domains could not be loaded
     * @throws com.flexive.shared.exceptions.FxNoAccessException    if the caller may not retrieve the list of domains
     */
    List<T> getDomains(Parameter parameter, String key) throws FxApplicationException;

    /**
     * Retrieves the value for the given parameter using the given key, for a specific domain. This method is
     * useful for getting a single value of aggregate parameters. When all values
     * for an aggregate parameter should be retrieved, use {@link #getAll(com.flexive.shared.configuration.Parameter)}  instead.
     * The default value may be disabled through the <code>ignoreDefault</code> parameter,
     * making this method more useful for checking if a parameter exists.
     *
     * @param <PT>       value type of the parameter
     * @param domain        the target domain
     * @param parameter parameter data containing the path
     * @param key       key to be used
     * @param ignoreDefault if the parameter's default value should be used default
     * @return          the value stored under the given path and key
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxLoadException      if the value could not be loaded
     * @throws com.flexive.shared.exceptions.FxNotFoundException  if the parameter is not set
     */
    <PT extends Serializable> PT get(T domain, Parameter<PT> parameter, String key, boolean ignoreDefault)
        throws FxApplicationException;

    /**
     * Return all configuration properties for a specific domain.
     *
     * @param domain    the requested domain
     * @return          all parameters for the given domain
     * @since           3.1.6
     */
    Map<ParameterData, Serializable> getAll(T domain) throws FxApplicationException;

    /**
     * Return all configuration properties for a specific domain using the provided XStream instance for conversions..
     *
     * @param domain the requested domain
     * @param instances XStream instances to use for conversions (mapped to class names - a wildcard parameter can be used for prefix matches (e.g. "my.config.*"))
     * @return all parameters for the given domain
     * @since 3.2.0
     */
    Map<ParameterData, Serializable> getAllWithXStream(T domain, Map<String, XStream> instances) throws FxApplicationException;

	/**
	 * Set a parameter containing a value of type <code>T</code> using
	 * the given key. This method is useful for setting aggregate parameters with
	 * varying keys.
	 * Use <code>getParameters(Parameter&lt;T&g;t parameter)</code> to retrieve a
	 * map with all keys stored under the path of the given parameter.
     * <p/>
     * Note that usually only superusers may update parameters for foreign domains (e.g. foreign users
     * or applications). 
	 *
	 * @param <PT>		value type to be set
     * @param domain   the target domain
	 * @param parameter	parameter data containing the path
	 * @param key		key to be used for this value
	 * @param value		value to be set
	 * @throws FxApplicationException TODO
	 * @throws com.flexive.shared.exceptions.FxUpdateException    if the value could not be updated
     * @throws com.flexive.shared.exceptions.FxNoAccessException  if the caller is not allowed to update/set this parameter
	 */
	<PT extends Serializable> void put(T domain, Parameter<PT> parameter, String key, PT value)
    throws FxApplicationException;

    /**
     * Removes a parameter from the database using the given key.
     * @param <PT>   value type of the parameter
     * @param domain        the target domain
     * @param parameter parameter containing the path to be removed
     * @param key   the key to be removed
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxRemoveException    if the parameter could not be removed
     * @throws com.flexive.shared.exceptions.FxNoAccessException  if the caller is not allowed to remove this parameter
     */
    <PT extends Serializable> void remove(T domain, Parameter<PT> parameter, String key) throws FxApplicationException;

    /**
     * Remove all parameters stored under this parameter's path.
     * @param <PT>   value type of the parameter
     * @param domain        the target domain
     * @param parameter the parameter to be removed
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxRemoveException    if the parameters could not be removed
     * @throws com.flexive.shared.exceptions.FxNoAccessException  if the caller is not allowed to remove this parameter
     */
    <PT extends Serializable> void removeAll(T domain, Parameter<PT> parameter) throws FxApplicationException;
}
