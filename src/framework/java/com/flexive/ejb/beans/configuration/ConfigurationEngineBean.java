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
package com.flexive.ejb.beans.configuration;

import com.flexive.shared.Pair;
import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.ParameterData;
import com.flexive.shared.configuration.ParameterScope;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;

import javax.ejb.*;
import java.io.Serializable;
import java.util.*;

/**
 * Configuration wrapper implementation.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "ConfigurationEngine", mappedName="ConfigurationEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ConfigurationEngineBean implements ConfigurationEngine, ConfigurationEngineLocal {
    @EJB
    private DivisionConfigurationEngineLocal divisionConfiguration;
    @EJB
    private UserConfigurationEngineLocal userConfiguration;
    @EJB
    private GlobalConfigurationEngineLocal globalConfiguration;
    @EJB
    private ApplicationConfigurationEngineLocal applicationConfiguration;
    @EJB
    private NodeConfigurationEngineLocal nodeConfiguration;
    @EJB
    private MandatorConfigurationEngineLocal mandatorConfiguration;

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> T get(Parameter<T> parameter, String key)
            throws FxApplicationException {
        return get(parameter, key, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> T get(Parameter<T> parameter) throws FxApplicationException {
        return get(parameter, parameter.getData().getKey());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> T get(Parameter<T> parameter, String key, boolean ignoreDefault)
            throws FxApplicationException {
        for (GenericConfigurationEngine config : getAvailableConfigurations(parameter.getScope())) {
            final Pair<Boolean, T> result = config.tryGet(parameter, key, true);
            if (result.getFirst()) {
                return result.getSecond();
            }
        }
        // parameter does not exist in any configuration, use default value if available
        if (!ignoreDefault && parameter.getDefaultValue() != null) {
            return parameter.getDefaultValue();
        } else {
            throw new FxNotFoundException("ex.configuration.parameter.notfound",
                    parameter.getData().getPath().getValue(), key);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> Pair<Boolean, T> tryGet(Parameter<T> parameter, String key, boolean ignoreDefault) {
        try {
            for (GenericConfigurationEngine config : getAvailableConfigurations(parameter.getScope())) {
                final Pair<Boolean, T> result = config.tryGet(parameter, key, true);
                if (result.getFirst()) {
                    return result;
                }
            }
        } catch (FxInvalidParameterException e) {
            return new Pair<Boolean, T>(false, null);
        }
        return new Pair<Boolean, T>(false, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> void putInSource(Parameter<T> parameter, String key, T value) throws FxApplicationException {
        getSource(parameter, key).put(parameter, key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> void putInSource(Parameter<T> parameter, T value) throws FxApplicationException {
        putInSource(parameter, parameter.getData().getKey(), value);
    }

    private <T extends Serializable> GenericConfigurationEngine getSource(Parameter<T> parameter, String key) throws FxApplicationException {
        final List<GenericConfigurationEngine> availableConfigurations = getAvailableConfigurations(parameter.getScope());
        for (GenericConfigurationEngine config : availableConfigurations) {
            try {
                config.get(parameter, key, true);
                return config;
                // CHECKSTYLE:OFF
            } catch (FxNotFoundException e) {
                // try next config
                // CHECKSTYLE:ON
            }
        }
        if (!availableConfigurations.isEmpty()) {
            // return last (= most general) configuration
            return availableConfigurations.get(availableConfigurations.size() - 1);
        }
        throw new FxNotFoundException("ex.configuration.parameter.notfound", parameter.getPath().getValue(), key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Map<ParameterData, Serializable> getAll() throws FxApplicationException {
        final Map<ParameterData, Serializable> results = new HashMap<ParameterData, Serializable>();
        for (GenericConfigurationEngine config : getAllConfigurations()) {
            results.putAll(config.getAll());
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Map<ParameterData, Serializable> getAllWithXStream(Map<String, XStream> instances) throws FxApplicationException {
        final Map<ParameterData, Serializable> results = new HashMap<ParameterData, Serializable>();
        for (GenericConfigurationEngine config : getAllConfigurations()) {
            results.putAll(config.getAllWithXStream(instances));
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> Map<String, T> getAll(Parameter<T> parameter) throws FxApplicationException {
        List<GenericConfigurationEngine> configs;
        try {
            configs = getAvailableConfigurations(parameter.getScope());
        } catch (FxInvalidParameterException e) {
            throw new FxLoadException("ex.configuration.parameter.load.ex", e);
        }
        // reverse configs to process fallback configs first
        Collections.reverse(configs);
        HashMap<String, T> result = new HashMap<String, T>();
        for (GenericConfigurationEngine config : configs) {
            Map<String, T> params = config.getAll(parameter);
            result.putAll(params);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> Collection<String> getKeys(Parameter<T> parameter) throws FxApplicationException {
        List<GenericConfigurationEngine> configs;
        try {
            configs = getAvailableConfigurations(parameter.getScope());
        } catch (FxInvalidParameterException e) {
            throw new FxLoadException("ex.configuration.parameter.load.ex", e);
        }
        final List<String> result = Lists.newArrayList();
        for (GenericConfigurationEngine config : configs) {
            final Collection<String> keys = config.getKeys(parameter);
            result.addAll(keys);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> void put(Parameter<T> parameter, String key, T value)
            throws FxApplicationException {
        try {
            getPrimaryConfiguration(parameter.getScope()).put(parameter, key, value);
        } catch (FxInvalidParameterException e) {
            throw new FxUpdateException("ex.configuration.update.ex", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> void put(Parameter<T> parameter, T value)
            throws FxApplicationException {
        try {
            getPrimaryConfiguration(parameter.getScope()).put(parameter, value);
        } catch (FxInvalidParameterException e) {
            throw new FxUpdateException("ex.configuration.update.ex", e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> void remove(Parameter<T> parameter, String key)
            throws FxApplicationException {
        try {
            getPrimaryConfiguration(parameter.getScope()).remove(parameter, key);
        } catch (FxInvalidParameterException e) {
            throw new FxRemoveException("ex.configuration.delete.ex", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> void remove(Parameter<T> parameter)
            throws FxApplicationException {
        try {
            getPrimaryConfiguration(parameter.getScope()).remove(parameter);
        } catch (FxInvalidParameterException e) {
            throw new FxRemoveException("ex.configuration.delete.ex", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public <T extends Serializable> void removeAll(Parameter<T> parameter)
            throws FxApplicationException {
        try {
            getPrimaryConfiguration(parameter.getScope()).removeAll(parameter);
        } catch (FxInvalidParameterException e) {
            throw new FxRemoveException("ex.configuration.delete.ex", e);
        }
    }

    /**
     * Returns the available list of configurations to be used for looking
     * up a parameter with the given scope. The first list entry is to
     * be queried first, the second only if the parameter does not exist
     * in the first config, and so on.
     *
     * @param scope parameter scope
     * @return ordered list of configurations to be used for looking up the parameter
     * @throws FxInvalidParameterException if the given scope is invalid
     */
    private List<GenericConfigurationEngine> getAvailableConfigurations(ParameterScope scope) throws FxInvalidParameterException {
        List<GenericConfigurationEngine> result = new ArrayList<GenericConfigurationEngine>();
        result.add(getPrimaryConfiguration(scope));
        List<ParameterScope> fallbacks = scope.getFallbacks();
        for (ParameterScope fallback : fallbacks) {
            result.add(getPrimaryConfiguration(fallback));
        }
        return result;
    }

    /**
     * Returns the primary configuration to be used for the given scope.
     *
     * @param scope scope to be queried
     * @return the primary configuration to be used for the given scope.
     * @throws FxInvalidParameterException if the scope is invalid
     */
    private GenericConfigurationEngine getPrimaryConfiguration(ParameterScope scope) throws FxInvalidParameterException {
        if (scope == ParameterScope.GLOBAL) {
            return globalConfiguration;
        } else if (scope == ParameterScope.DIVISION || scope == ParameterScope.DIVISION_ONLY) {
            return divisionConfiguration;
        } else if (scope == ParameterScope.USER || scope == ParameterScope.USER_ONLY) {
            return userConfiguration;
        } else if (scope == ParameterScope.APPLICATION || scope == ParameterScope.APPLICATION_ONLY) {
            return applicationConfiguration;
        } else if (scope == ParameterScope.NODE || scope == ParameterScope.NODE_ONLY) {
            return nodeConfiguration;
        } else if (scope == ParameterScope.MANDATOR || scope == ParameterScope.MANDATOR_ONLY) {
            return mandatorConfiguration;
        } else {
            throw new FxInvalidParameterException("SCOPE", "ex.configuration.parameter.scope.invalid", scope);
        }
    }

    private List<GenericConfigurationEngine> getAllConfigurations () {
        return Arrays.asList(
                divisionConfiguration,
                nodeConfiguration,
                applicationConfiguration,
                userConfiguration,
                mandatorConfiguration
        );
    }
}

