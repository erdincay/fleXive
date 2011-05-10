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

import com.flexive.core.flatstorage.FxFlatStorageInfo;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.impex.FxDivisionExportInfo;
import com.flexive.shared.value.FxString;

import javax.ejb.Remote;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Division configuration interface. Use this for division-specific operations and for direct manipulation
 * of parameters in the division configuration, use the {@link com.flexive.shared.interfaces.ConfigurationEngine}
 * for everything else.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface DivisionConfigurationEngine extends GenericConfigurationEngine {

    /**
     * Install a binary file contained in fxresources/binaries in the archive as a binary with the requested id.
     * If an entry with the given id already exists it will be overwritten.
     * This functionality is intended to be used by runone or startup scripts!
     *
     * @param binaryId     requested binary id
     * @param resourceName name of the resource file relative to fxresources/binaries
     * @throws FxApplicationException on errors
     */
    void installBinary(long binaryId, String resourceName) throws FxApplicationException;

    /**
     * Perform a database patch if needed
     *
     * @throws FxApplicationException on errors
     */
    void patchDatabase() throws FxApplicationException;

    /**
     * Get information about the used database (name and version)
     *
     * @return information about the used database (name and version)
     */
    String getDatabaseInfo();

    /**
     * Get information about the used database jdbc driver (name and version)
     *
     * @return information about the used database jdbc driver (name and version)
     */
    String getDatabaseDriverInfo();

    /**
     * Is the a flat storage engine enabled for this division?
     *
     * @return if a flat storage engine is enabled for this division
     */
    boolean isFlatStorageEnabled();

    /**
     * Get information about existing flat storages
     *
     * @return List containing information about existing flat storages
     * @throws FxApplicationException on errors
     */
    List<FxFlatStorageInfo> getFlatStorageInfos() throws FxApplicationException;

    /**
     * Create a flat storage
     *
     * @param name          name of the flat storage (table name)
     * @param description   flat storage description
     * @param stringColumns number of String columns
     * @param textColumns   number of Text columns
     * @param bigIntColumns number of BigInt columns
     * @param doubleColumns number of Double columns
     * @param selectColumns number of Select/Boolean columns
     * @throws FxApplicationException on errors
     */
    void createFlatStorage(String name, String description, int stringColumns, int textColumns, int bigIntColumns,
                                  int doubleColumns, int selectColumns) throws FxApplicationException;

    /**
     * Remove a flat storage, only possible if no entries exist!
     *
     * @param name name of the flat storage
     * @throws FxApplicationException on errors
     */
    void removeFlatStorage(String name) throws FxApplicationException;

    /**
     * Export the current division to a file on the local application server
     *
     * @param localFileName name (and path) of the file on the local application server
     * @throws FxApplicationException on errors
     */
    void exportDivision(String localFileName) throws FxApplicationException;


    /**
     * Get information about a previously exported division
     *
     * @param localFileName name (and path) of the file on the local application server
     * @return FxDivisionExportInfo
     * @throws FxApplicationException on errors
     */
    FxDivisionExportInfo getDivisionExportInfo(String localFileName) throws FxApplicationException;

    /**
     * Import the current division from a file on the local application server
     *
     * @param localFileName name (and path) of the file on the local application server
     * @throws FxApplicationException on errors
     */
    void importDivision(String localFileName) throws FxApplicationException;

    /**
     * Set a resource value. If the passed value is empty or <code>null</code> the entry will be removed
     *
     * @param key   unique key
     * @param value FxString value
     * @throws FxApplicationException on errors
     * @since 3.1.4
     */
    void setResourceValue(String key, FxString value) throws FxApplicationException;

    /**
     * Remove all resources whose key starts with keyPrefix
     *
     * @param keyPrefix requested key prefix
     * @throws FxApplicationException on errors
     * @since 3.1.4
     */
    void removeResourceValues(String keyPrefix) throws FxApplicationException;

    /**
     * Get a resource FxString value or <code>null</code> if not set
     *
     * @param key             unique key
     * @param defaultLanguage default language to set in the returned FxString, if not available the first found
     *                        translation is the default language
     * @return FxString value or <code>null</code> if not set
     * @throws FxApplicationException on errors
     * @since 3.1.4
     */
    FxString getResourceValue(String key, long defaultLanguage) throws FxApplicationException;

    /**
     * Get all resource FxString values that match a given key prefix.
     * e.g. if keys "test.1", "test.2" and "test.3" exist, a key prefix "test." will return these 3 entries
     *
     * @param keyPrefix       requested key prefix
     * @param defaultLanguage default language to set in the returned FxString, if not available the first found
     *                        translation is the default language
     * @return an ordered (in regards to keys) map containing all found keys and FxString values
     * @throws FxApplicationException on errors
     * @since 3.1.4
     */
    Map<String, FxString> getResourceValues(String keyPrefix, long defaultLanguage) throws FxApplicationException;

    /**
     * Get all resource FxString values that match a given key pattern.
     * e.g. if keys "test.cat.1", "test.cat.2" and "test.cat.3" exist, a key pattern ".cat." will return these 3 entries
     *
     * @param keyMatch        requested part of the key that has to match
     * @param defaultLanguage default language to set in the returned FxString, if not available the first found
     *                        translation is the default language
     * @return an ordered (in regards to keys) map containing all found keys and FxString values
     * @throws FxApplicationException on errors
     * @since 3.1.6
     */
    Map<String, FxString> getResourceValuesContains(String keyMatch, long defaultLanguage) throws FxApplicationException;

    /**
     * Get all resource keys that match a given key prefix, but do not load the associated values.
     *
     * @param keyMatch  the key prefix, or, if a wildcard ('%') is contained, the exact query string
     * @return          all resource keys that match the prefix
     * @throws FxApplicationException   on errors
     * @since 3.1.6
     */
    Set<String> getResourceKeysMatching(String keyMatch) throws FxApplicationException;
}
