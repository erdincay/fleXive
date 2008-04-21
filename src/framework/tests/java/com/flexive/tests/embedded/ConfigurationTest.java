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
package com.flexive.tests.embedded;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.*;
import com.flexive.shared.configuration.parameters.ParameterFactory;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.security.UserTicket;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.shared.TestParameters;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.*;

import java.io.Serializable;
import java.util.*;

/**
 * Configuration test suite
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "configuration"})
public class ConfigurationTest {

    private TestUser user;
    private GlobalConfigurationEngine globalConfiguration;
    private DivisionConfigurationEngine divisionConfiguration;
    private UserConfigurationEngine userConfiguration;
    private ConfigurationEngine configuration;

    public ConfigurationTest() {
    }

    public ConfigurationTest(TestUser user) {
        this.user = user;
    }

    @Factory
    public Object[] createTestInstances() throws FxApplicationException {
        List<Object> result = new ArrayList<Object>();
        for (TestUser user : TestUsers.getConfiguredTestUsers()) {
            result.add(new ConfigurationTest(user));
        }
        if (result.size() > 1) {
            // TODO add configuration management role
            //result.add(new ConfigurationTest(TestUsers.createUser("CONFIGROLE", Role.GlobalSupervisor)));
        }
        return result.toArray();
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        globalConfiguration = EJBLookup.getGlobalConfigurationEngine();
        divisionConfiguration = EJBLookup.getDivisionConfigurationEngine();
        userConfiguration = EJBLookup.getUserConfigurationEngine();
        configuration = EJBLookup.getConfigurationEngine();
    }

    @BeforeMethod
    public void beforeTestMethod() throws FxLoginFailedException, FxAccountInUseException {
        System.out.println("Login: " + user.getUserName());
        login(user);
    }

    @AfterMethod
    public void afterTestMethod() throws Exception {
        System.out.println("Logout: " + user.getUserName());
        logout();
    }

    /**
     * Generic test class
     *
     * @param <T>   type parameter
     */
    public static class GenericConfigurationTest<T extends Serializable> {
        private GenericConfigurationEngine configuration;
        private Parameter<T> parameter;
        private ParameterDataEditBean<T> data;
        private T value;

        /**
         * Create a new test class for the given parameter.
         *
         * @param configuration the configuration instance to be tested
         * @param parameter     the parameter to be used for unit tests
         * @param value         the value to be stored in the given parameter
         */
        public GenericConfigurationTest(GenericConfigurationEngine configuration, Parameter<T> parameter, T value) {
            this.configuration = configuration;
            this.parameter = parameter;
            this.data = (ParameterDataEditBean<T>) parameter.getData();
            this.value = value;
        }

        /**
         * Run all tests for this instance
         *
         * @throws Exception if an exception is thrown
         */
        public void runTests() throws Exception {
            ParameterPath oldPath = data.getPath();
            try {
                for (SystemParameterPaths path : SystemParameterPaths.getTestPaths()) {
                    if (!path.getScope().hasScope(ParameterScope.GLOBAL)) {
                        continue;
                    }
                    // cycle through all available scopes
                    data.setPath(path);
                    testDefaultValue();
                    testPut();
                    testDelete();
                    testUpdate();
                    testGetAll();
                }
            } finally {
                // restore values
                data.setPath(oldPath);
            }
        }

        /**
         * Check if the default parameter value is returned properly.
         *
         * @throws Exception if an error occured
         */
        private void testDefaultValue() throws Exception {
            try {
                configuration.remove(parameter);
                if (!mayUpdateConfig())
                    assert false : "Should not be permitted to delete parameter " + parameter + " in " + configuration;
                assert parameter.getDefaultValue().equals(configuration.get(parameter)) : "Default value incorrect.";
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig())
                    assert false : "Failed to delete parameter although privileges exist.";
            }
        }

        /**
         * Test storing a parameter in the configuration
         *
         * @throws Exception if an error occured
         */
        private void testPut() throws Exception {
            try {
                configuration.put(parameter, value);
                if (!mayUpdateConfig())
                    assert false : "Should not be permitted to set parameter " + parameter + " in " + configuration;

                T dbValue = configuration.get(parameter);
                assert !((dbValue == null && value != null) || (dbValue != null && value == null));
                assert dbValue == null || dbValue.equals(value) : "Parameter value not stored correctly or incorrect implementation of \"Object#equals\"";
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig())
                    assert false : "Failed to put parameter although privileges exist for parameter " + parameter + " in " + configuration;

            } finally {
                safeDelete();
            }
        }

        /**
         * Test deleting a parameter
         *
         * @throws Exception if an error occured
         */
        private void testDelete() throws Exception {
            try {
                configuration.put(parameter, value);
                if (!mayUpdateConfig())
                    assert false : "Should not be permitted to set parameter " + parameter + " in " + configuration;

                configuration.get(parameter);
                configuration.remove(parameter);
                if (!mayUpdateConfig())
                    assert false : "Should not be permitted to delete parameter " + parameter + " in " + configuration;

                try {
                    configuration.get(parameter, parameter.getKey(), true);
                    assert false : "Parameter deleted but still retrievable with get(): " + parameter;
                } catch (FxNotFoundException e) {
                    // succeed
                }
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig())
                    assert false : "Failed to update/delete parameter although privileges exist "
                            + "for parameter " + parameter + " in " + configuration;
            } finally {
                safeDelete();
            }
        }

        /**
         * Test updating a parameter
         *
         * @throws Exception if an error occured
         */
        private void testUpdate() throws Exception {
            try {
                assert !parameter.getDefaultValue().equals(value) : "Default value and value must not be the same!";
                configuration.put(parameter, parameter.getDefaultValue());
                assert parameter.getDefaultValue().equals(configuration.get(parameter)) : "Failed to load value.";
                configuration.put(parameter, value);
                if (!mayUpdateConfig())
                    assert false : "Should not be permitted to set parameter " + parameter + " in " + configuration;
                assert (configuration.get(parameter) == null && value == null) || configuration.get(parameter).equals(value) : "Failed to load updated value.";
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig())
                    assert false : "Failed to update parameter although privileges exist "
                            + "for parameter " + parameter + " in " + configuration;
            } finally {
                safeDelete();
            }
        }

        /**
         * Test retrieving a group of parameters stored in a path.
         *
         * @throws Exception if an error occured
         */
        private void testGetAll() throws Exception {
            try {
                configuration.put(parameter, "key1", parameter.getDefaultValue());
                configuration.put(parameter, "key2", value);
                configuration.put(parameter, "key3", value);
                configuration.put(parameter, "key4", value);
                if (!mayUpdateConfig())
                    assert false : "Should not be permitted to set parameter " + parameter
                            + " in " + configuration;

                Map<String, T> params = configuration.getAll(parameter);
                assert 4 == params.entrySet().size() : "Should have retrieved four parameters.";
                int ctr = 1;
                final String[] keyValues = {"key1", "key2", "key3", "key4"};
                for (String key : keyValues) {
                    if (params.get(key) != null) {
                        assert params.get(key).equals(ctr == 1 ? parameter.getDefaultValue() : value) : "Invalid parameter value";
                    }
                    ctr++;
                }

                final Collection<String> keys = configuration.getKeys(parameter);
                assert 4 == keys.size() : "Should have retrieved for parameters.";
                for (String key: keyValues) {
                    assert keys.contains(key) : "Key " + key + " not found in result returned by getKeys.";
                }
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig()) {
                    assert false : "Failed to update/delete parameter although privileges exist "
                            + "for parameter " + parameter + " in " + configuration;
                }
            } finally {
                safeDelete();
            }
        }

        /**
         * Security-aware "cleanup" for parameters assuming that one can
         * only delete parameters if one can set them...
         *
         * @throws FxApplicationException
         */
        private void safeDelete() throws FxApplicationException {
            try {
                configuration.removeAll(parameter);
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig()) {
                    assert false : "Failed to delete parameter although privileges exist.";
                }
            }
        }

        /**
         * Returns true if the current user may update this test's config
         *
         * @return true if the current user may update this test's config
         * @throws FxLookupException if a lookup error occured
         */
        private boolean mayUpdateConfig() throws FxLookupException {
            final UserTicket ticket = FxContext.get().getTicket();
            final ParameterScope scope = parameter.getScope();
            GenericConfigurationEngine checkConfiguration = configuration;
            if (configuration instanceof ConfigurationEngine) {
                // get "primary" config for current parameter for security checks
                if (scope == ParameterScope.GLOBAL) {
                    checkConfiguration = EJBLookup.getGlobalConfigurationEngine();
                } else if (scope == ParameterScope.DIVISION || scope == ParameterScope.DIVISION_ONLY) {
                    checkConfiguration = EJBLookup.getDivisionConfigurationEngine();
                } else if (scope == ParameterScope.USER || scope == ParameterScope.USER_ONLY) {
                    checkConfiguration = EJBLookup.getUserConfigurationEngine();
                }
            }
            return (checkConfiguration instanceof GlobalConfigurationEngine
                    && FxContext.get().isGlobalAuthenticated())
                    || (checkConfiguration instanceof DivisionConfigurationEngine && ticket.isGlobalSupervisor())
                    || (checkConfiguration instanceof UserConfigurationEngine);
        }


    }

    /**
     * Test the global configuration
     *
     * @throws Exception if an error occured
     */
    @Test
    public void globalConfiguration() throws Exception {
        try {
            FxContext.get().setGlobalAuthenticated(false);
            globalConfiguration.put(TestParameters.CACTUS_TEST, "test");
            assert false : "Global configuration modifiable without global admin login!";
        } catch (FxNoAccessException e) {
            // pass
        }
        final GlobalConfigurationEngine config = EJBLookup.getGlobalConfigurationEngine();
        // check if privileges are respected for all config actions
        testGenericConfiguration(config);
        try {
            FxContext.get().setGlobalAuthenticated(true);
            testGenericConfiguration(config);
        } finally {
            FxContext.get().setGlobalAuthenticated(false);
        }
        // test division infos
        int[] divisionIds = config.getDivisionIds();
        int ctr = 0;
        for (DivisionData data : config.getDivisions()) {
            assert ArrayUtils.contains(divisionIds, data.getId()) : "Division ID not returned by getDivisionIds().";
            assert StringUtils.isNotBlank(data.getDataSource()) : "Division data source not returned";
            assert StringUtils.isNotBlank(data.getDomainRegEx()) : "Domain regexp missing";
            if (data.isAvailable()) {
                assert StringUtils.isNotBlank(data.getDbVersion()) : "DB version missing";
                assert data.getDbVendor().getId() >= 0 : "DB vendor missing";
            }
            config.clearDivisionCache();
            assert config.getDivisionData(data.getId()).equals(data);
            ctr++;
        }
        assert ctr == divisionIds.length : "getDivisions() and getDivisionIds() array length don't match";

        // test division table update
        final DivisionData[] orig = config.getDivisions();
        FxContext.get().setGlobalAuthenticated(true);
        try {
            final DivisionData newDivision = new DivisionData(1, false, "test", "xxx", DBVendor.Unknown, "1.2");
            config.saveDivisions(Arrays.asList(newDivision));
            assert config.getDivisions().length == 1 : "Division table not updated";
            assert config.getDivisions()[0].equals(newDivision) : "New division not written properly";
        } finally {
            config.saveDivisions(Arrays.asList(orig));
            assert Arrays.equals(config.getDivisions(), orig);
            FxContext.get().setGlobalAuthenticated(false);
        }
    }

    /**
     * Test the per-division configuration
     *
     * @throws Exception if an error occured
     */
    @Test
    public void divisionConfiguration() throws Exception {
        testGenericConfiguration(divisionConfiguration);
    }

    /**
     * Test the per-user configuration
     *
     * @throws Exception if an error occured
     */
    @Test
    public void userConfiguration() throws Exception {
        testGenericConfiguration(userConfiguration);
    }

    /**
     * Test the FxConfiguration wrapper
     *
     * @throws Exception if an error occured
     */
    @Test
    public void fxConfiguration() throws Exception {
        try {
            FxContext.get().setGlobalAuthenticated(false);
            configuration.put(TestParameters.CACTUS_TEST, "test");
            assert false : "User allowed to set global config parameters without auth.";
        } catch (FxNoAccessException e) {
            // pass
        }
        // test fallbacks
        try {
            FxContext.get().setGlobalAuthenticated(true);
            testFallbacks(TestParameters.CACTUS_TEST, "test");
            testFallbacks(TestParameters.CACTUS_TEST, null);
        } finally {
            FxContext.get().setGlobalAuthenticated(false);
        }
        // run generic config tests
        try {
            FxContext.get().setGlobalAuthenticated(true);
            testGenericConfiguration(configuration);
        } finally {
            FxContext.get().setGlobalAuthenticated(false);
        }
    }

    @Test
    public void putInSourceTest() throws FxApplicationException {
        final Parameter<Boolean> param = ParameterFactory.newInstance(Boolean.class, SystemParameterPaths.TEST_DIVISION_ONLY, "testKey", true);
        try {
            configuration.putInSource(param, false); // put in source without an existing database entry
            assert !configuration.get(param);
            configuration.putInSource(param, true);  // overwrite existing value
            assert configuration.get(param);
        } finally {
            try {
                EJBLookup.getConfigurationEngine().remove(param);
            } catch (FxApplicationException e) {
                // ignore
            }
        }
    }

    /**
     * Generic configuration test. Tests all known types of parameters
     * for the given configuration.
     *
     * @param configuration The configuration to be tested
     * @throws Exception if an error occurs
     */
    private void testGenericConfiguration(GenericConfigurationEngine configuration) throws Exception {
        // test string parameters
        new GenericConfigurationTest<String>(configuration, TestParameters.CACTUS_TEST, "test").runTests();
        new GenericConfigurationTest<String>(configuration, TestParameters.CACTUS_TEST, "").runTests();
        new GenericConfigurationTest<String>(configuration, TestParameters.CACTUS_TEST, null).runTests();
        // test int parameters
        new GenericConfigurationTest<Integer>(configuration, TestParameters.CACTUS_TEST_INT, 255329).runTests();
        new GenericConfigurationTest<Integer>(configuration, TestParameters.CACTUS_TEST_INT, -1032412).runTests();
        try {
            new GenericConfigurationTest<Integer>(configuration, TestParameters.CACTUS_TEST_INT, null).runTests();
            assert false : "Should not be able to put null values in int parameters.";
        } catch (Exception e) {
            // pass
        }
        // test long parameters
        new GenericConfigurationTest<Long>(configuration, TestParameters.CACTUS_TEST_LONG, 255329L).runTests();
        new GenericConfigurationTest<Long>(configuration, TestParameters.CACTUS_TEST_LONG, -1032412L).runTests();
        try {
            new GenericConfigurationTest<Long>(configuration, TestParameters.CACTUS_TEST_LONG, null).runTests();
            assert false : "Should not be able to put null values in long parameters.";
        } catch (Exception e) {
            // pass
        }
        // test boolean parameters
        ParameterDataEditBean<Boolean> data = (ParameterDataEditBean<Boolean>)
                TestParameters.CACTUS_TEST_BOOL.getData();
        data.setDefaultValue(false);
        new GenericConfigurationTest<Boolean>(configuration, TestParameters.CACTUS_TEST_BOOL, true).runTests();
        data.setDefaultValue(true);
        new GenericConfigurationTest<Boolean>(configuration, TestParameters.CACTUS_TEST_BOOL, false).runTests();
        try {
            new GenericConfigurationTest<Boolean>(configuration, TestParameters.CACTUS_TEST_BOOL, null).runTests();
            assert false : "Should not be able to put null values in boolean parameters.";
        } catch (Exception e) {
            // pass
        }
        // test object parameters with a simple POJO
        new GenericConfigurationTest<FxPK>(configuration, TestParameters.TEST_OBJ, new FxPK(5, 1)).runTests();
        try {
            new GenericConfigurationTest<FxPK>(configuration, TestParameters.TEST_OBJ, null).runTests();
            assert false : "Should not be able to put null values in object parameters.";
        } catch (Exception e) {
            // pass
        }
    }

    /**
     * Test all kinds of fallbacks of FxConfiguration
     *
     * @param <T>       parameter value type to be tested
     * @param parameter parameter to be tested
     * @param value     the value to be tested
     * @throws Exception if an error occured
     */
    private <T extends Serializable> void testFallbacks(Parameter<T> parameter, T value) throws Exception {
        cleanup(parameter);
        for (ParameterPath path : SystemParameterPaths.getTestPaths()) {
            ParameterPath origPath = updatePath(parameter, path);
            try {
                configuration.put(parameter, value);
                assertAccess(parameter);
                T dbValue = configuration.get(parameter);
                checkFallbacks(parameter, dbValue);
            } catch (FxNoAccessException e) {
                assertNoAccess(parameter);
            } finally {
                cleanup(parameter);
                updatePath(parameter, origPath);
            }
        }
    }

    /**
     * Purges the given test parameter from all known configurations.
     *
     * @param parameter the parameter to be deleted
     */
    private void cleanup(Parameter<? extends Serializable> parameter) {
        try {
            globalConfiguration.remove(parameter);
            assertAccess(parameter, ParameterScope.GLOBAL);
        } catch (FxNoAccessException e) {
            assertNoAccess(parameter, ParameterScope.GLOBAL);
        } catch (Exception e) {
            System.out.println("Failed to delete parameter from global config: " + e.getMessage());
        }
        try {
            divisionConfiguration.remove(parameter);
            assertAccess(parameter, ParameterScope.DIVISION);
        } catch (FxNoAccessException e) {
            assertNoAccess(parameter, ParameterScope.DIVISION);
        } catch (Exception e) {
            System.out.println("Failed to delete parameter from division config: " + e.getMessage());
        }
        try {
            userConfiguration.remove(parameter);
            assertAccess(parameter, ParameterScope.USER);
        } catch (FxNoAccessException e) {
            assertNoAccess(parameter, ParameterScope.USER);
        } catch (Exception e) {
            System.out.println("Failed to delete parameter from user config: " + e.getMessage());
        }
    }

    /**
     * Helper method to manually check the fallback value
     *
     * @param parameter the parameter to be checked
     * @param expected  expected parameter value
     * @param <T>       value type
     * @throws FxApplicationException
     */
    private <T extends Serializable> void checkFallbacks(Parameter<T> parameter, T expected)
            throws FxApplicationException {
        ArrayList<ParameterScope> fallbacks = new ArrayList<ParameterScope>();
        fallbacks.add(0, parameter.getScope());
        fallbacks.addAll(parameter.getScope().getFallbacks());
        for (ParameterScope scope : fallbacks) {
            try {
                T value = null;
                if (scope == ParameterScope.GLOBAL) {
                    value = globalConfiguration.get(parameter, parameter.getData().getKey(), true);
                } else if (scope == ParameterScope.DIVISION || scope == ParameterScope.DIVISION_ONLY) {
                    value = divisionConfiguration.get(parameter, parameter.getData().getKey(), true);
                } else if (scope == ParameterScope.USER || scope == ParameterScope.USER_ONLY) {
                    value = userConfiguration.get(parameter, parameter.getData().getKey(), true);
                } else {
                    assert false : "Unexpected parameter scope: " + scope;
                }
                // parameter exists in config, check value and return
                assert (expected == null && value == null) || expected.equals(value) : "Unexpected parameter value";
                return;
            } catch (FxNotFoundException e) {
                // continue with next configuration
            }
        }
        // parameter does not exist in db - check default value
        if (parameter.getDefaultValue() != null) {
            assert parameter.getDefaultValue().equals(expected) : "Configuration should have returned the default value.";
        } else {
            assert false : "Parameter does not found: " + parameter;
        }
    }

    /**
     * Update the path of test paraemeters.
     *
     * @param parameter the parameter to be updated
     * @param path      the path to be set
     * @return the previously set path
     */
    private ParameterPath updatePath(Parameter<?> parameter, ParameterPath path) {
        ParameterPath oldPath = parameter.getPath();
        ((ParameterDataEditBean<?>) parameter.getData()).setPath(path);
        return oldPath;
    }

    /**
     * Assert that the current user may not access (update) the given parameter.
     *
     * @param parameter the parameter to be checked
     */
    private void assertNoAccess(Parameter<?> parameter, ParameterScope scope) {
        UserTicket ticket = FxContext.get().getTicket();
        if (scope == ParameterScope.USER || scope == ParameterScope.USER_ONLY) {
            assert false : "User parameters should always be writable for the user: " + parameter;
        }
        if ((scope == ParameterScope.DIVISION || scope == ParameterScope.DIVISION_ONLY)
                && ticket.isGlobalSupervisor()) {
            assert false : "User is global supervisor, but cannot update division parameter: " + parameter;
        }
        if (scope == ParameterScope.GLOBAL && FxContext.get().isGlobalAuthenticated()) {
            assert false : "User is authenticated for global config, but may not update parameter: " + parameter;
        }
    }

    private void assertNoAccess(Parameter<?> parameter) {
        assertNoAccess(parameter, parameter.getScope());
    }

    /**
     * Assert that the current user may access the given parameter.
     *
     * @param parameter the parameter to be checked
     */
    private void assertAccess(Parameter<?> parameter, ParameterScope scope) {
        UserTicket ticket = FxContext.get().getTicket();
        if ((scope == ParameterScope.DIVISION || scope == ParameterScope.DIVISION_ONLY)
                && !ticket.isGlobalSupervisor()) {
            assert false : "User is NOT global supervisor, but can update division parameter: " + parameter;
        }
        if (scope == ParameterScope.GLOBAL && !FxContext.get().isGlobalAuthenticated()) {
            assert false : "User is NOT authenticated for global config, but may update global parameter: " + parameter;
        }
    }

    private void assertAccess(Parameter<?> parameter) {
        assertAccess(parameter, parameter.getScope());
    }

}
