/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.ejb.beans.search;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.search.*;
import com.flexive.shared.structure.FxEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import java.util.Arrays;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@TransactionManagement(TransactionManagementType.CONTAINER)
@Stateless(name = "ResultPreferencesEngine")
public class ResultPreferencesEngineBean implements ResultPreferencesEngine, ResultPreferencesEngineLocal {
    private static final Log LOG = LogFactory.getLog(ResultPreferencesEngineBean.class);

    @EJB
    ConfigurationEngineLocal configuration;
    @EJB
    DivisionConfigurationEngineLocal divisionConfiguration;
    @EJB
    UserConfigurationEngineLocal userConfiguration;

    /** {@inheritDoc} */
    public ResultPreferences load(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException {
        ResultPreferences preferences;
        try {
            preferences = configuration.get(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location));
        } catch (FxNotFoundException e) {
            if (typeId >= 0) {
                // use global default
                return load(-1, viewType, location);
            } else if (typeId == -1) {
                // if no global default is defined, use hardcoded default settings
                preferences = new ResultPreferences(Arrays.asList(new ResultColumnInfo(Table.CONTENT, "@pk", null)),
                        Arrays.asList(new ResultOrderByInfo(Table.CONTENT, "@pk", null, SortDirection.ASCENDING)),
                        25, 100);
            } else {
                throw e;
            }
        }
        final ResultPreferences checkedPreferences = checkProperties(preferences);
        if (checkedPreferences != null) {
            // check performed, store new version in configuration
            // We need to obey fallback preferences - i.e. use the source configuration for the preferences object
            try {
                FxContext.get().runAsSystem();
                // we may have to update the division configuration here, so we need superuser privileges
                configuration.putInSource(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location),
                        checkedPreferences);
            } catch (FxNotFoundException e) {
                LOG.warn("Using internal default result preferences [" + getKey(typeId, viewType, location)
                    + "], this may cause performance problems.");
            } finally {
                FxContext.get().stopRunAsSystem();
            }
            return checkedPreferences;
        }
        return preferences;
    }

    /** {@inheritDoc} */
    public boolean isCustomized(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException {
        try {
            configuration.get(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location));
            return true;
        } catch (FxNotFoundException e) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public void save(ResultPreferences preferences, long typeId, ResultViewType viewType,ResultLocation location) throws FxApplicationException {
        configuration.put(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location), preferences);
    }

    /** {@inheritDoc} */
    public void saveDefaultPreferences(ResultPreferences preferences, long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException {
        // div conf already checks for supervisor access
        divisionConfiguration.put(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location), preferences);
    }

    /** {@inheritDoc} */
    public void remove(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException {
        configuration.remove(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location));
    }

    private String getKey(long typeId, ResultViewType viewType, ResultLocation location) {
        return typeId + "/" + viewType.name() + "/" + location.getName();
    }

    /**
     * Check all properties in preferences for their validity. If no check was performed,
     * null is returned, otherwise a new result preferences object with the new timestamp is returned.
     *
     * @param preferences   the preferences to be checked
     * @return  returns null if no check was performed,
     * otherwise a new result preferences object with the new timestamp is returned.
     */
    private ResultPreferences checkProperties(ResultPreferences preferences) {
        // TODO check structure timestamp
        if (preferences.getLastChecked() != -1) {
            return null;
        }
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        ResultPreferencesEdit rpe = preferences.getEditObject();

        // clean up selected columns
        for (ResultColumnInfo info: preferences.getSelectedColumns()) {
            if (!info.isProperty()) {
                continue;
            }
            try {
                environment.getProperty(info.getPropertyName());
            } catch (FxRuntimeException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removing property " + info.getPropertyName()
                            + " for user " + FxContext.get().getTicket());
                }
                rpe.removeSelectedColumn(info);
            }
        }

        // clean up order by columns
        for (ResultOrderByInfo info: preferences.getOrderByColumns()) {
            if (!info.isProperty()) {
                continue;
            }
            try {
                environment.getProperty(info.getPropertyName());
            } catch (FxRuntimeException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removing property " + info.getPropertyName()
                            + " for user " + FxContext.get().getTicket());
                }
                rpe.removeOrderByColumn(info);
            }
        }
        rpe.setLastChecked(System.currentTimeMillis());
        return rpe;
    }
}
