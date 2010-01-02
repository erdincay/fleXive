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
package com.flexive.shared.interfaces;

import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.exceptions.*;

import javax.ejb.Remote;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.util.List;

/**
 * Global configuration interface. Stores parameters valid for all divisions
 * and the division configuration itself.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface GlobalConfigurationEngine extends GenericConfigurationEngine {
    /**
     * JMX management method.
     * @throws Exception    if an error occured
     */
    void create() throws Exception;

    /**
     * JMX management method.
     * @throws Exception    if an error occured
     */
    void destroy() throws Exception;

    /**
     * Return the root user name.
     * @return	the root login name.
     * @throws FxApplicationException TODO
     * @throws FxLoadException  if a database error occured
     * @throws FxNotFoundException  if the root login is not set in the configuration
     */
    String getRootLogin() throws FxApplicationException;

    /**
     * Set the root user name.
     * @param value		the new root user name
     * @throws FxApplicationException TODO
     * @throws FxUpdateException	if the user name could not be updated
     * @throws FxNoAccessException  if the calling user is not permitted to change to root long
     */
    void setRootLogin(String value) throws FxApplicationException;

    /**
     * Get the SHA1-hashed root password.
     * @return	the SHA1-hashed root password.
     * @throws FxApplicationException TODO
     * @throws FxLoadException	if the parameter could not be loaded successfully
     * @throws FxNotFoundException	if the root login is not set
     */
    String getRootPassword() throws FxApplicationException;

    /**
     * Return true if the given plain-text password matches the stored
     * (and hashed) password.
     *
     * @param userPassword	the password to be checked
     * @return	true if the given plain-text password matches the stored
     * (and hashed) password.
     * @throws FxApplicationException TODO
     * @throws FxLoadException  if the root password could not be loaded
     * @throws FxNotFoundException  if the root password is not set in the global configuration
     */
    boolean isMatchingRootPassword(String userPassword) throws FxApplicationException;

    /**
     * Set the root password.
     * @param value	the root password in plain-text.
     * @throws FxApplicationException TODO
     * @throws FxUpdateException	if the password could not be updated
     * @throws FxNoAccessException  if the calling user is not permitted to change the password
     */
    void setRootPassword(String value) throws FxApplicationException;

    /**
     * Returns the configuration data for a given division ID.
     *
     * @param division	the division ID.
     * @return	the configuration data for a given division ID.
     * @throws FxApplicationException TODO
     * @throws FxLoadException	if an error occured reading the division configuration
     * @throws FxNotFoundException	if the division was not found
     */
    DivisionData getDivisionData(int division) throws FxApplicationException;

    /**
     * Returns the first matching division for the given server name,
     * or -1 if no division was found.
     *
     * @param serverName	the server name to be matched
     * @return		the ID of the first division that matches
     * @throws FxApplicationException TODO
     * @throws FxLoadException  if an error occured reading the division configuration
     */
    int getDivisionId(String serverName) throws FxApplicationException;

    /**
     * Return all configured division IDs.
     * @return	all configured division IDs
     * @throws FxApplicationException TODO
     * @throws FxLoadException	if an error occured reading the division configuration
     */
    int[] getDivisionIds() throws FxApplicationException;

    /**
     * Return the division data for all configured divisions.
     * @return  division data for all configured divisions.
     * @throws FxApplicationException TODO
     * @throws FxLoadException  if an error occured reading the division configuration
     */
    DivisionData[] getDivisions() throws FxApplicationException;

    /**
     * Clear the division cache.
     */
    void clearDivisionCache();

    void registerCacheMBean(ObjectName name) throws MBeanRegistrationException, NotCompliantMBeanException, InstanceAlreadyExistsException;

    /**
     * Tests the database connection for the given dataSource JNDI name and returns a
     * {@link DivisionData} object with the current database vendor and version values.
     *
     * @param divisionId    the division ID
     * @param dataSource    the datasource JNDI path
     * @param domainRegEx   the regular expression used for matching domains
     * @return  the {@link DivisionData} object with database information
     */
    DivisionData createDivisionData(int divisionId, String dataSource, String domainRegEx);

    /**
     * Replaces the existing division configuration.
     *
     * @param divisions the new division data
     * @throws FxApplicationException   if the divisions could not be updated.
     */
    void saveDivisions(List<? extends DivisionData> divisions) throws FxApplicationException;
}
