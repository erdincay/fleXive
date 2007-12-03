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
package com.flexive.faces.beans;

import com.flexive.shared.*;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.workflow.StepDefinition;

import java.io.Serializable;
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
     * @throws FxApplicationException if an error occured
     * @return all user groups for the current mandator.
     */
    public Map<Long, UserGroup> getUserGroups() throws FxApplicationException {
        if (userGroupsMap != null) {
            return userGroupsMap;
        }
        userGroupsMap = new HashMap<Long, UserGroup>();
        return populateMap(userGroupsMap, EJBLookup.getUserGroupEngine().loadAll(-1).getList());
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
            });
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
            });
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
            });
        }
        return propertiesByNameMap;
    }

    /**
     * Put all objects in the given hash map.
     *
     * @param <T>   item type parameter
     * @param map   the map to be populated
     * @param items all items to be added to the map.
     * @return the populated map
     */
    private <T extends SelectableObject> Map<Long, T> populateMap(Map<Long, T> map, List<T> items) {
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
		for (T item: items) {
			map.put(item.getId(), item);
		}
		return map;
	}

}
