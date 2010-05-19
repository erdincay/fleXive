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
package com.flexive.faces.beans;

import com.flexive.shared.*;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Account;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.workflow.StepDefinition;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper beans containing miscellaneous maps for
 * associative lookups via JSF EL.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class MapBean implements Serializable {
    private static final long serialVersionUID = -4851802085773447097L;
    private Map<Long, Mandator> mandatorMap = null;
    private Map<Long, StepDefinition> stepDefinitionMap = null;
    private Map<Long, ACL> aclMap = null;
    private Map<Long, UserGroup> userGroupsMap = null;
    private Map<Long, FxType> typesMap = null;
    private Map<String, FxType> typesByNameMap = null;
    private Map<Long, FxProperty> propertiesMap = null;
    private Map<String, FxProperty> propertiesByNameMap = null;
    private Map<Long, FxAssignment> assignmentsMap = null;
    private Map<Long, Account> accountMap = null;
    private Map<Long, String> dateTimeMap = null;
    private Map<Long, FxLanguage> languagesMap = null;
    private Map<Long, FxString> stepNameMap = null;
    private Map<Object, FxSelectList> selectListMap;
    private FxEnvironment environment;

    /**
     * Constructs a new map beans.
     */
    public MapBean() {
        environment = CacheAdmin.getFilteredEnvironment();
    }

    /**
     * Return all active mandators.
     *
     * @return all active mandators.
     */
    public Map<Long, Mandator> getMandators() {
        if (mandatorMap != null) {
            return mandatorMap;
        }
        mandatorMap = new HashMap<Long, Mandator>();
        return populateMap(mandatorMap, environment.getMandators(true, false));
    }

    /**
     * Return all available step definitions.
     *
     * @return all available step definitions.
     */
    public Map<Long, StepDefinition> getStepDefinitions() {
        if (stepDefinitionMap != null) {
            return stepDefinitionMap;
        }
        stepDefinitionMap = new HashMap<Long, StepDefinition>();
        return populateMapWithLabel(stepDefinitionMap, environment.getStepDefinitions());
    }

    /**
     * Return all available ACLs
     *
     * @return all available ACLs
     */
    public Map<Long, ACL> getAcls() {
        if (aclMap != null) {
            return aclMap;
        }
        aclMap = new HashMap<Long, ACL>();
        return populateMap(aclMap, environment.getACLs());
    }

    /**
     * Return all user groups for the current mandator.
     *
     * @return all user groups for the current mandator.
     * @throws FxApplicationException if an error occured
     */
    public Map<Long, UserGroup> getUserGroups() throws FxApplicationException {
        if (userGroupsMap != null) {
            return userGroupsMap;
        }
        userGroupsMap = new HashMap<Long, UserGroup>();
        return populateMap(userGroupsMap, EJBLookup.getUserGroupEngine().loadAll(-1));
    }

    /**
     * Return all defined types.
     *
     * @return all defined types.
     */
    public Map<Long, FxType> getTypes() {
        if (typesMap != null) {
            return typesMap;
        }
        typesMap = new HashMap<Long, FxType>();
        return populateMapWithLabel(typesMap, environment.getTypes(true, true, true, true));
    }

    /**
     * Return all defined types, lookup by name (case-insensitive).
     *
     * @return all defined types, lookup by name (case-insensitive).
     */
    public Map<String, FxType> getTypesByName() {
        if (typesByNameMap == null) {
            typesByNameMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, FxType>() {
                public FxType get(Object key) {
                    if (key == null) {
                        return null;
                    }
                    return environment.getType(key.toString());
                }
            }, true);
        }
        return typesByNameMap;
    }

    /**
     * Return all defined properties.
     *
     * @return all defined properties.
     */
    public Map<Long, FxProperty> getProperties() {
        if (propertiesMap == null) {
            propertiesMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Long, FxProperty>() {
                public FxProperty get(Object key) {
                    if (key == null) {
                        return null;
                    }
                    return environment.getProperty((Long) key);
                }
            }, true);
        }
        return propertiesMap;
    }

    /**
     * Return all defined properties, lookup by name (case-insensitive).
     *
     * @return all defined properties, lookup by name (case-insensitive).
     */
    public Map<String, FxProperty> getPropertiesByName() {
        if (propertiesByNameMap == null) {
            propertiesByNameMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, FxProperty>() {
                public FxProperty get(Object key) {
                    if (key == null) {
                        return null;
                    }
                    return environment.getProperty(key.toString());
                }
            }, true);
        }
        return propertiesByNameMap;
    }

    /**
     * Return all defined assignments.
     *
     * @return all defined assignments.
     */
    public Map<Long, FxAssignment> getAssignments() {
        if (assignmentsMap == null) {
            assignmentsMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Long, FxAssignment>() {
                public FxAssignment get(Object key) {
                    if (key == null) {
                        return null;
                    }
                    return environment.getAssignment((Long) key);
                }
            }, true);
        }
        return assignmentsMap;
    }

    /**
     * Return step label by id
     *
     * @return step label by id
     */
    public Map<Long, FxString> getStep() {
        if (stepNameMap == null) {
            stepNameMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Long, FxString>() {
                public FxString get(Object key) {
                    if (key == null) {
                        return null;
                    }
                    return environment.getStepDefinition(environment.getStep((Long)key).getStepDefinitionId()).getLabel();
                }
            }, true);
        }
        return stepNameMap;
    }


    /**
     * Get an account by id
     *
     * @return map containing accounts by id
     */
    public Map<Long, Account> getAccount() {
        if (accountMap == null) {
            accountMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Long, Account>() {
                private Map<Long, Account> cache = new HashMap<Long, Account>(10);

                @SuppressWarnings({"SuspiciousMethodCalls"})
                public Account get(Object key) {
                    if (key == null || !(key instanceof Long)) {
                        return null;
                    }
                    if (cache.containsKey(key)) {
                        return cache.get(key);
                    }
                    try {
                        cache.put((Long) key, EJBLookup.getAccountEngine().load((Long) key));
                        return cache.get(key);
                    } catch (FxApplicationException e) {
                        throw e.asRuntimeException();
                    }
                }
            }, true);
        }
        return accountMap;
    }

    /**
     * A date/time formatter
     *
     * @return date/time formatter
     */
    public Map<Long, String> getDateTime() {
        if (dateTimeMap == null) {
            dateTimeMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Long, String>() {

                public String get(Object key) {
                    if (key == null || !(key instanceof Long)) {
                        return null;
                    }
                    return FxFormatUtils.getDateTimeFormat().format(new Date((Long) key));
                }
            }, true);
        }
        return dateTimeMap;
    }

    /**
     * Provides access to the system languages by ID.
     *
     * @return  the languages by ID
     */
    public Map<Long, FxLanguage> getLanguage() {
        if (languagesMap == null) {
            languagesMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Long, FxLanguage>() {
                public FxLanguage get(Object key) {
                    if (key == null) {
                        return null;
                    }
                    final long id = key instanceof Long ? (Long) key : Long.valueOf(key.toString());
                    return CacheAdmin.getEnvironment().getLanguage(id);
                }
            }, true);
        }
        return languagesMap;
    }

    /**
     * Provide access to user-defined select lists by ID or (unique) select list name.
     * 
     * @return  the select list
     */
    public Map<Object, FxSelectList> getSelectList() {
        if (selectListMap == null) {
            selectListMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Object, FxSelectList>() {
                public FxSelectList get(Object key) {
                    if (key == null) {
                        return null;
                    }
                    if (key instanceof Number) {
                        return CacheAdmin.getEnvironment().getSelectList(((Number) key).longValue());
                    }
                    final String keyVal = key.toString();
                    if (StringUtils.isNumeric(keyVal)) {
                        return CacheAdmin.getEnvironment().getSelectList(Long.valueOf(keyVal));
                    } else {
                        return CacheAdmin.getEnvironment().getSelectList(keyVal);
                    }
                }
            }, true);
        }
        return selectListMap;
    }

    /**
     * Put all objects in the given hash map.
     *
     * @param <T>   item type parameter
     * @param map   the map to be populated
     * @param items all items to be added to the map.
     * @return the populated map
     */
    private <T extends SelectableObjectWithName> Map<Long, T> populateMap(Map<Long, T> map, List<T> items) {
        for (T item : items) {
            map.put(item.getId(), item);
        }
        return map;
    }

    /**
     * Put all objects in the given hash map.
     *
     * @param <T>   item type parameter
     * @param map   the map to be populated
     * @param items all items to be added to the map.
     * @return the populated map
     */
    private <T extends SelectableObjectWithLabel> Map<Long, T> populateMapWithLabel(
            Map<Long, T> map, List<T> items) {
        for (T item : items) {
            map.put(item.getId(), item);
        }
        return map;
    }

}
