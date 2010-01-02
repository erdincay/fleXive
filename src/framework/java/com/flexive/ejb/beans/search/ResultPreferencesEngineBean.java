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
package com.flexive.ejb.beans.search;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.search.*;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.*;
import java.util.Arrays;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@TransactionManagement(TransactionManagementType.CONTAINER)
@Stateless(name = "ResultPreferencesEngine", mappedName="ResultPreferencesEngine")
public class ResultPreferencesEngineBean implements ResultPreferencesEngine, ResultPreferencesEngineLocal {
    private static final Log LOG = LogFactory.getLog(ResultPreferencesEngineBean.class);
    @EJB
    ConfigurationEngineLocal configuration;
    @EJB
    DivisionConfigurationEngineLocal divisionConfiguration;
    @EJB
    UserConfigurationEngineLocal userConfiguration;

    /** {@inheritDoc} */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultPreferences load(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException {
        return loadWithType(typeId, viewType, location).getPreferences();
    }

    private LoadResult loadWithType(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException {
        ResultPreferences preferences;
        try {
            preferences = configuration.get(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location));
        } catch (FxNotFoundException e) {
            if (typeId >= 0) {
                final FxType type = CacheAdmin.getEnvironment().getType(typeId);
                // use parent type or global default (FX-482)
                return loadWithType(type.getParent() != null ? type.getParent().getId() : -1, viewType, location);
            } else if (typeId == -1) {
                // if no global default is defined, use hardcoded default settings
                preferences = new ResultPreferences(Arrays.asList(
                        new ResultColumnInfo(Table.CONTENT, "@pk", null),
                        new ResultColumnInfo(Table.CONTENT, "typedef", "description"),
                        new ResultColumnInfo(Table.CONTENT, "caption", null),
                        new ResultColumnInfo(Table.CONTENT, "modified_at", null),
                        new ResultColumnInfo(Table.CONTENT, "modified_by", "username"),
                        new ResultColumnInfo(Table.CONTENT, "@lock", null)
                ), Arrays.asList(
                        new ResultOrderByInfo(Table.CONTENT, "@pk", null, SortDirection.ASCENDING)
                ), 25, 100);
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
            return new LoadResult(typeId, checkedPreferences);
        }
        return new LoadResult(typeId, preferences);
    }

    /** {@inheritDoc} */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultPreferences loadSystemDefault(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException {
        try {
            return divisionConfiguration.get(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location));
        } catch (FxNotFoundException e) {
            // return empty result preferences
            return new ResultPreferences();
        }
    }

    /** {@inheritDoc} */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean isCustomized(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException {
        try {
            configuration.get(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location));
            return true;
        } catch (FxNotFoundException e) {
            return false;
        }
    }

    /** {@inheritDoc} */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void save(ResultPreferences preferences, long typeId, ResultViewType viewType,ResultLocation location) throws FxApplicationException {
        if (preferences.getSelectedColumns().isEmpty()) {
            throw new FxInvalidParameterException("preferences", "ex.ResultPreferences.save.empty");
        }
        configuration.put(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location), preferences);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void saveInSource(ResultPreferences preferences, long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException {
        final LoadResult result = loadWithType(typeId, viewType, location);
        // save for the root type
        save(preferences, result.getTypeId(), viewType, location);
    }

    /** {@inheritDoc} */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void saveSystemDefault(ResultPreferences preferences, long typeId, ResultViewType viewType, ResultLocation... locations) throws FxApplicationException {
        if (preferences.getSelectedColumns().isEmpty()) {
            throw new FxInvalidParameterException("preferences", "ex.ResultPreferences.save.empty");
        }
        for (ResultLocation location: locations) {
            // div conf already checks for supervisor access
            divisionConfiguration.put(SystemParameters.USER_RESULT_PREFERENCES, getKey(typeId, viewType, location), preferences);
        }
    }

    /** {@inheritDoc} */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
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
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        if (preferences.getLastChecked() >= environment.getTimeStamp()) {
            return null;
        }
        ResultPreferencesEdit rpe = preferences.getEditObject();

        // clean up selected columns
        for (ResultColumnInfo info: preferences.getSelectedColumns()) {
            if (!info.isStructureNode()) {
                continue;
            }
            try {
                if (info.getPropertyName().startsWith("#")) {
                    environment.getAssignment(stripTableSelectors(info.getPropertyName().substring(1)));
                } else {
                    environment.getProperty(stripTableSelectors(info.getPropertyName()));
                }
            } catch (FxRuntimeException e) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Removing property " + info.getPropertyName()
                            + " for user " + FxContext.getUserTicket());
                }
                rpe.removeSelectedColumn(info);
            }
        }

        // clean up order by columns
        for (ResultOrderByInfo info: preferences.getOrderByColumns()) {
            if (!info.isStructureNode()) {
                continue;
            }
            try {
                if (info.getPropertyName().startsWith("#")) {
                    environment.getAssignment(stripTableSelectors(info.getPropertyName().substring(1)));
                } else {
                    environment.getProperty(stripTableSelectors(info.getPropertyName()));
                }
            } catch (FxRuntimeException e) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Removing property " + info.getPropertyName()
                            + " for user " + FxContext.getUserTicket());
                }
                rpe.removeOrderByColumn(info);
            }
        }
        rpe.setLastChecked(System.currentTimeMillis());
        return rpe;
    }

    private String stripTableSelectors(String name) {
        final int pos = name.indexOf('.');
        return pos != -1 ? name.substring(0, pos) : name;
    }

    private static class LoadResult {
        private final long typeId;
        private final ResultPreferences preferences;

        private LoadResult(long typeId, ResultPreferences preferences) {
            this.typeId = typeId;
            this.preferences = preferences;
        }

        public long getTypeId() {
            return typeId;
        }

        public ResultPreferences getPreferences() {
            return preferences;
        }
    }

}
