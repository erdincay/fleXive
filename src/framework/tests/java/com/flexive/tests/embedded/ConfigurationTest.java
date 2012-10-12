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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import static com.flexive.shared.EJBLookup.getApplicationConfigurationEngine;
import static com.flexive.shared.EJBLookup.getUserConfigurationEngine;
import static com.flexive.shared.EJBLookup.getNodeConfigurationEngine;
import static com.flexive.shared.EJBLookup.getMandatorConfigurationEngine;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.*;
import com.flexive.shared.configuration.parameters.ParameterFactory;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.security.Account;
import com.flexive.shared.security.Mandator;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.shared.TestParameters;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import static org.testng.Assert.*;
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
    private ApplicationConfigurationEngine applicationConfiguration;
    private NodeConfigurationEngine nodeConfiguration;
    private MandatorConfigurationEngine mandatorConfiguration;
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
        userConfiguration = getUserConfigurationEngine();
        configuration = EJBLookup.getConfigurationEngine();
        applicationConfiguration = getApplicationConfigurationEngine();
        nodeConfiguration = getNodeConfigurationEngine();
        mandatorConfiguration = getMandatorConfigurationEngine();
    }

    @BeforeMethod
    public void beforeTestMethod() throws FxLoginFailedException, FxAccountInUseException {
        //System.out.println("Login: " + user.getUserName());
        login(user);
    }

    @AfterMethod
    public void afterTestMethod() throws Exception {
        //System.out.println("Logout: " + user.getUserName());
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
         * @throws Exception if an error occurred
         */
        private void testDefaultValue() throws Exception {
            try {
                configuration.remove(parameter);
                if (!mayUpdateConfig())
                    fail("Should not be permitted to delete parameter " + parameter + " in " + configuration);
                assertTrue(parameter.getDefaultValue().equals(configuration.get(parameter)), "Default value incorrect.");
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig())
                    fail("Failed to delete parameter although privileges exist.");
            }
        }

        /**
         * Test storing a parameter in the configuration
         *
         * @throws Exception if an error occurred
         */
        private void testPut() throws Exception {
            try {
                configuration.put(parameter, value);
                if (!mayUpdateConfig())
                    fail("Should not be permitted to set parameter " + parameter + " in " + configuration);

                T dbValue = configuration.get(parameter);
                assertTrue(!((dbValue == null && value != null) || (dbValue != null && value == null && parameter.getDefaultValue() == null)));
                assertTrue(dbValue == null || dbValue.equals(value != null ? value : parameter.getDefaultValue()),
                        "Parameter value not stored correctly or incorrect implementation of \"Object#equals\"");
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig())
                    fail("Failed to put parameter although privileges exist for parameter " + parameter + " in " + configuration);

            } finally {
                safeDelete();
            }
        }

        /**
         * Test deleting a parameter
         *
         * @throws Exception if an error occurred
         */
        private void testDelete() throws Exception {
            try {
                configuration.put(parameter, value);
                if (!mayUpdateConfig())
                    fail("Should not be permitted to set parameter " + parameter + " in " + configuration);

                configuration.get(parameter);
                configuration.remove(parameter);
                if (!mayUpdateConfig())
                    fail("Should not be permitted to delete parameter " + parameter + " in " + configuration);

                try {
                    configuration.get(parameter, parameter.getKey(), true);
                    fail("Parameter deleted but still retrievable with get(): " + parameter);
                } catch (FxNotFoundException e) {
                    // succeed
                }
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig())
                    fail("Failed to update/delete parameter although privileges exist "
                            + "for parameter " + parameter + " in " + configuration);
            } finally {
                safeDelete();
            }
        }

        /**
         * Test updating a parameter
         *
         * @throws Exception if an error occurred
         */
        private void testUpdate() throws Exception {
            try {
                assertTrue(!parameter.getDefaultValue().equals(value), "Default value and value must not be the same!");
                configuration.put(parameter, parameter.getDefaultValue());
                assertTrue(parameter.getDefaultValue().equals(configuration.get(parameter)), "Failed to load value.");
                configuration.put(parameter, value);
                if (!mayUpdateConfig())
                    fail("Should not be permitted to set parameter " + parameter + " in " + configuration);
                assertTrue((configuration.get(parameter) == null && value == null && parameter.getDefaultValue() == null)
                        || (configuration.get(parameter) != null && value == null && parameter.getDefaultValue() != null)
                        || configuration.get(parameter).equals(value), "Failed to load updated value.");
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig())
                    fail("Failed to update parameter although privileges exist "
                            + "for parameter " + parameter + " in " + configuration);
            } finally {
                safeDelete();
            }
        }

        /**
         * Test retrieving a group of parameters stored in a path.
         *
         * @throws Exception if an error occurred
         */
        private void testGetAll() throws Exception {
            try {
                configuration.put(parameter, "key1", parameter.getDefaultValue());
                configuration.put(parameter, "key2", value);
                configuration.put(parameter, "key3", value);
                configuration.put(parameter, "key4", value);
                if (!mayUpdateConfig())
                    fail("Should not be permitted to set parameter " + parameter
                            + " in " + configuration);

                if (!(configuration instanceof GlobalConfigurationEngine)) {
                    final Map<ParameterData, Serializable> allParameters = configuration.getAll();
                    assertTrue(allParameters.size() > 0);
                }

                Map<String, T> params = configuration.getAll(parameter);
                assertTrue(4 == params.entrySet().size(), "Should have retrieved four parameters.");
                int ctr = 1;
                final String[] keyValues = {"key1", "key2", "key3", "key4"};
                for (String key : keyValues) {
                    if (params.get(key) != null) {
                        assertTrue(params.get(key).equals(ctr == 1 ? parameter.getDefaultValue() : value), "Invalid parameter value");
                    }
                    ctr++;
                }

                final Collection<String> keys = configuration.getKeys(parameter);
                assertTrue(4 == keys.size(), "Should have retrieved for parameters.");
                for (String key: keyValues) {
                    assertTrue(keys.contains(key), "Key " + key + " not found in result returned by getKeys.");
                }
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig()) {
                    fail("Failed to update/delete parameter although privileges exist "
                            + "for parameter " + parameter + " in " + configuration);
                }
            } finally {
                safeDelete();
            }
        }

        /**
         * Security-aware "cleanup" for parameters assuming that one can
         * only delete parameters if one can set them...
         *
         * @throws FxApplicationException on errors
         */
        private void safeDelete() throws FxApplicationException {
            try {
                configuration.removeAll(parameter);
            } catch (FxNoAccessException e) {
                if (mayUpdateConfig()) {
                    fail("Failed to delete parameter although privileges exist.");
                }
            }
        }

        /**
         * Returns true if the current user may update this test's config
         *
         * @return true if the current user may update this test's config
         * @throws FxLookupException if a lookup error occurred
         */
        private boolean mayUpdateConfig() throws FxLookupException {
            final UserTicket ticket = FxContext.getUserTicket();
            final ParameterScope scope = parameter.getScope();
            GenericConfigurationEngine checkConfiguration = configuration;
            if (configuration instanceof ConfigurationEngine) {
                // get "primary" config for current parameter for security checks
                if (scope == ParameterScope.GLOBAL) {
                    checkConfiguration = EJBLookup.getGlobalConfigurationEngine();
                } else if (scope == ParameterScope.DIVISION || scope == ParameterScope.DIVISION_ONLY) {
                    checkConfiguration = EJBLookup.getDivisionConfigurationEngine();
                } else if (scope == ParameterScope.USER || scope == ParameterScope.USER_ONLY) {
                    checkConfiguration = getUserConfigurationEngine();
                } else if (scope == ParameterScope.APPLICATION || scope == ParameterScope.APPLICATION_ONLY) {
                    checkConfiguration = getApplicationConfigurationEngine();
                }
            }
            return (checkConfiguration instanceof GlobalConfigurationEngine
                    && FxContext.get().isGlobalAuthenticated())
                    || (checkConfiguration instanceof DivisionConfigurationEngine && ticket.isGlobalSupervisor())
                    || (checkConfiguration instanceof ApplicationConfigurationEngine && ticket.isGlobalSupervisor())
                    || (checkConfiguration instanceof NodeConfigurationEngine && ticket.isGlobalSupervisor())
                    || (checkConfiguration instanceof UserConfigurationEngine && !ticket.isGuest())
                    || (checkConfiguration instanceof MandatorConfigurationEngine && ticket.isMandatorSupervisor());
        }


    }

    /**
     * Test the global configuration
     *
     * @throws Exception if an error occurred
     */
    @Test
    public void globalConfiguration() throws Exception {
        try {
            FxContext.get().setGlobalAuthenticated(false);
            globalConfiguration.put(TestParameters.CACTUS_TEST, "test");
            fail("Global configuration modifiable without global admin login!");
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
            assertTrue(ArrayUtils.contains(divisionIds, data.getId()), "Division ID not returned by getDivisionIds().");
            assertTrue(StringUtils.isNotBlank(data.getDataSource()), "Division data source not returned");
            assertTrue(StringUtils.isNotBlank(data.getDomainRegEx()), "Domain regexp missing");
            if (data.isAvailable()) {
                assertTrue(StringUtils.isNotBlank(data.getDbVersion()), "DB version missing");
                assertTrue(!data.getDbVendor().equals("Unknown"), "DB vendor missing");
            }
            config.clearDivisionCache();
            assertTrue(config.getDivisionData(data.getId()).equals(data));
            ctr++;
        }
        assertTrue(ctr == divisionIds.length, "getDivisions() and getDivisionIds() array length don't match");

        // test division table update
        final DivisionData[] orig = config.getDivisions();
        FxContext.get().setGlobalAuthenticated(true);
        try {
            final DivisionData newDivision = new DivisionData(1, false, "test", "xxx", "Unknown", "1.2", "Unknown");
            config.saveDivisions(Arrays.asList(newDivision));
            assertTrue(config.getDivisions().length == 1, "Division table not updated");
            assertTrue(config.getDivisions()[0].equals(newDivision), "New division not written properly");
        } finally {
            config.saveDivisions(Arrays.asList(orig));
            assertTrue(Arrays.equals(config.getDivisions(), orig));
            FxContext.get().setGlobalAuthenticated(false);
        }
    }

    /**
     * Test the per-division configuration
     *
     * @throws Exception if an error occurred
     */
    @Test
    public void divisionConfiguration() throws Exception {
        testGenericConfiguration(divisionConfiguration);
    }

    /**
     * Test the per-application configuration
     *
     * @throws Exception if an error occurred
     */
    @Test
    public void applicationConfiguration() throws Exception {
        testGenericConfiguration(applicationConfiguration);
    }

    @Test
    public void mandatorConfiguration() throws Exception {
        testGenericConfiguration(mandatorConfiguration);
    }

    /**
     * Test the per-user configuration
     *
     * @throws Exception if an error occurred
     */
    @Test
    public void userConfiguration() throws Exception {
        testGenericConfiguration(userConfiguration);
    }

    /**
     * Test the node configuration
     *
     * @throws Exception if an error occurred
     */
    @Test
    public void nodeConfiguration() throws Exception {
        testGenericConfiguration(nodeConfiguration);
    }

    /**
     * Test the FxConfiguration wrapper
     *
     * @throws Exception if an error occurred
     */
    @Test
    public void fxConfiguration() throws Exception {
        try {
            FxContext.get().setGlobalAuthenticated(false);
            configuration.put(TestParameters.CACTUS_TEST, "test");
            fail("User allowed to set global config parameters without auth.");
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
            assertAccess(param);
            assertTrue(!configuration.get(param));
            configuration.putInSource(param, true);  // overwrite existing value
            assertTrue(configuration.get(param));
        } catch (FxNoAccessException e) {
            assertNoAccess(param);
        } finally {
            try {
                EJBLookup.getConfigurationEngine().remove(param);
            } catch (FxApplicationException e) {
                // ignore
            }
        }
    }

    @Test
    public void putInForeignApplication() throws FxApplicationException {
        foreignDomainTest(getApplicationConfigurationEngine(), "otherapp");
    }

    @Test
    public void putInForeignUser() throws FxApplicationException {
        // find a user that is not our own
        final List<Account> accounts = EJBLookup.getAccountEngine().loadAll();
        long userId = -1;
        for (Account account : accounts) {
            if (account.getId() != FxContext.get().getTicket().getUserId()) {
                userId = account.getId();
                break;
            }
        }
        assertTrue(userId != -1);

        foreignDomainTest(getUserConfigurationEngine(), userId);
    }

    @Test
    public void putInForeignMandator() throws FxApplicationException {
        foreignDomainTest(mandatorConfiguration, CacheAdmin.getEnvironment().getMandator(Mandator.MANDATOR_FLEXIVE).getName());
    }

    @Test
    public void putInForeignNode() throws FxApplicationException {
        foreignDomainTest(getNodeConfigurationEngine(), "mynode");
    }

    private <T extends Serializable> void foreignDomainTest(CustomDomainConfigurationEngine<T> dce, T otherDomainValue) throws FxApplicationException {
        final Parameter<Integer> param = TestParameters.CACTUS_TEST_INT;
        final boolean supervisor = FxContext.getUserTicket().isGlobalSupervisor();
        try {
            assertEquals(dce.getDomains(param).size(), 0, "Configuration parameter in use: " + param);
            if (dce instanceof UserConfigurationEngine) {
                assertTrue(supervisor, "User configuration engine must not allow domain listings except for global supervisors.");
            }
        } catch (FxNoAccessException e) {
            assertTrue((dce instanceof UserConfigurationEngine || dce instanceof MandatorConfigurationEngine) && !supervisor,
                    "Only user or mandator configuration engine should prohibit domain listings.");
        }

        // the following block is expected to fail unless the user is a global supervisor
        final boolean expectFailure = !supervisor;
        boolean createdOwn = false;
        try {
            dce.put(param, 21);
            createdOwn = true;
            assertEquals(dce.getDomains(param).size(), 1);

            dce.put(otherDomainValue, param, param.getKey(), 22);
            final List<T> domains = dce.getDomains(param);
            assertEquals(domains.size(), 2,
                    "There should be two entries: " + param);
            assertTrue(domains.contains(otherDomainValue), "Custom domain not found in " + domains);

            assertTrue(dce.getDomains().contains(otherDomainValue), "Custom domain not found in " + dce.getDomains());

            assertEquals(dce.get(param).intValue(), 21);
            assertEquals(dce.get(otherDomainValue, param, param.getKey(), false).intValue(), 22);

            assertTrue(dce.getAll(otherDomainValue).containsValue(22), "Parameter from other domain not returned");

            for (T domain : domains) {
                dce.remove(domain, param, param.getKey());
            }
            assertEquals(dce.getDomains(param).size(), 0, "Configuration parameter not removed: " + param);
        } catch (FxNoAccessException e) {
            if (createdOwn) {
                assertTrue(dce instanceof UserConfigurationEngine,
                        "Only the user configuration should always allow creation of own instances.");
                dce.remove(param);
            }
            if (!expectFailure) {
                fail("Expected failure, but user was allowed to update foreign configuration IDs.");
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
        final Parameter<Integer> param = TestParameters.CACTUS_TEST_INT;
        new GenericConfigurationTest<Integer>(configuration, param, 255329).runTests();
        new GenericConfigurationTest<Integer>(configuration, param, -1032412).runTests();
        try {
            new GenericConfigurationTest<Integer>(configuration, param, null).runTests();
            fail("Should not be able to put null values in int parameters.");
        } catch (Exception e) {
            // pass
        }
        // test long parameters
        new GenericConfigurationTest<Long>(configuration, TestParameters.CACTUS_TEST_LONG, 255329L).runTests();
        new GenericConfigurationTest<Long>(configuration, TestParameters.CACTUS_TEST_LONG, -1032412L).runTests();
        try {
            new GenericConfigurationTest<Long>(configuration, TestParameters.CACTUS_TEST_LONG, null).runTests();
            fail("Should not be able to put null values in long parameters.");
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
            fail("Should not be able to put null values in boolean parameters.");
        } catch (Exception e) {
            // pass
        }
        // test object parameters with a simple POJO
        new GenericConfigurationTest<FxPK>(configuration, TestParameters.TEST_OBJ, new FxPK(5, 1)).runTests();
        try {
            new GenericConfigurationTest<FxPK>(configuration, TestParameters.TEST_OBJ, null).runTests();
            fail("Should not be able to put null values in object parameters.");
        } catch (Exception e) {
            // pass
        }
        // test foreign ids
        if (configuration instanceof CustomDomainConfigurationEngine) {
            final CustomDomainConfigurationEngine domainConfig = (CustomDomainConfigurationEngine) configuration;
            boolean remove = false;
            try {
                assertEquals(domainConfig.getDomains(param).size(), 0, "Old values exist for: " + param);
                try {
                    domainConfig.put(param, 21);
                    assertAccess(param, domainConfig);
                    remove = true;
                    assertEquals(domainConfig.getDomains(param).size(), 1, "There should be an ID entry: " + param);
                } catch (FxNoAccessException e) {
                    assertNoAccess(param, domainConfig);
                } finally {
                    if (remove) {
                        configuration.remove(param);
                    }
                }
            } catch (FxNoAccessException e) {
                // cannot list IDs
            }
        }
    }

    /**
     * Test all kinds of fallbacks of FxConfiguration
     *
     * @param <T>       parameter value type to be tested
     * @param parameter parameter to be tested
     * @param value     the value to be tested
     * @throws Exception if an error occurred
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
        tryRemove(parameter, globalConfiguration, ParameterScope.GLOBAL);
        tryRemove(parameter, divisionConfiguration, ParameterScope.DIVISION);
        tryRemove(parameter, userConfiguration, ParameterScope.USER);
        tryRemove(parameter, nodeConfiguration, ParameterScope.NODE);
    }

    private void tryRemove(Parameter<? extends Serializable> parameter, GenericConfigurationEngine configuration, ParameterScope scope) {
        try {
            configuration.remove(parameter);
            assertAccess(parameter, scope);
        } catch (FxNoAccessException e) {
            assertNoAccess(parameter, scope);
        } catch (Exception e) {
            System.out.println("Failed to delete parameter from " + scope.name() + " config: " + e.getMessage());
        }
    }

    /**
     * Helper method to manually check the fallback value
     *
     * @param parameter the parameter to be checked
     * @param expected  expected parameter value
     * @param <T>       value type
     * @throws FxApplicationException on errors
     */
    private <T extends Serializable> void checkFallbacks(Parameter<T> parameter, T expected)
            throws FxApplicationException {
        ArrayList<ParameterScope> fallbacks = new ArrayList<ParameterScope>();
        fallbacks.add(0, parameter.getScope());
        fallbacks.addAll(parameter.getScope().getFallbacks());
        for (ParameterScope scope : fallbacks) {
            try {
                T value;
                final GenericConfigurationEngine config;
                if (scope == ParameterScope.GLOBAL) {
                    config = globalConfiguration;
                } else if (scope == ParameterScope.DIVISION || scope == ParameterScope.DIVISION_ONLY) {
                    config = divisionConfiguration;
                } else if (scope == ParameterScope.USER || scope == ParameterScope.USER_ONLY) {
                    config = userConfiguration;
                } else if (scope == ParameterScope.APPLICATION || scope == ParameterScope.APPLICATION_ONLY) {
                    config = applicationConfiguration;
                } else if (scope == ParameterScope.NODE || scope == ParameterScope.NODE_ONLY) {
                    config = nodeConfiguration;
                } else {
                    fail("Unexpected parameter scope: " + scope);
                    throw new UnsupportedOperationException();
                }
                value = config.get(parameter, parameter.getData().getKey(), true);
                // parameter exists in config, check value and return
                assertTrue((expected == null && value == null) || expected.equals(value), "Unexpected parameter value");
                return;
            } catch (FxNotFoundException e) {
                // continue with next configuration
            }
        }
        // parameter does not exist in db - check default value
        if (parameter.getDefaultValue() != null) {
            assertTrue(parameter.getDefaultValue().equals(expected), "Configuration should have returned the default value.");
        } else {
            fail("Parameter does not found: " + parameter);
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
     * @param scope scope of the parameter
     */
    private void assertNoAccess(Parameter<?> parameter, ParameterScope scope) {
        UserTicket ticket = FxContext.getUserTicket();
        if (scope == ParameterScope.USER || scope == ParameterScope.USER_ONLY) {
            fail("User parameters should always be writable for the user: " + parameter);
        }
        if ((scope == ParameterScope.DIVISION || scope == ParameterScope.DIVISION_ONLY)
                && ticket.isGlobalSupervisor()) {
            fail("User is global supervisor, but cannot update division parameter: " + parameter);
        }
        if (scope == ParameterScope.GLOBAL && FxContext.get().isGlobalAuthenticated()) {
            fail("User is authenticated for global config, but may not update parameter: " + parameter);
        }
    }

    private void assertNoAccess(Parameter<?> parameter) {
        assertNoAccess(parameter, parameter.getScope());
    }

    /**
     * Assert that the current user may access the given parameter.
     *
     * @param parameter the parameter to be checked
     * @param scope scope of the parameter
     */
    private void assertAccess(Parameter<?> parameter, ParameterScope scope) {
        UserTicket ticket = FxContext.getUserTicket();
        if ((scope == ParameterScope.DIVISION || scope == ParameterScope.DIVISION_ONLY)
                && !ticket.isGlobalSupervisor()) {
            fail("User is NOT global supervisor, but can update division parameter: " + parameter);
        }
        if (scope == ParameterScope.GLOBAL && !FxContext.get().isGlobalAuthenticated()) {
            fail("User is NOT authenticated for global config, but may update global parameter: " + parameter);
        }
    }

    private void assertAccess(Parameter<?> parameter) {
        assertAccess(parameter, parameter.getScope());
    }

    private void assertAccess(Parameter<?> parameter, GenericConfigurationEngine configuration) {
        assertAccess(parameter, scopeForAccessCheck(parameter, configuration));
    }

    private void assertNoAccess(Parameter<?> parameter, GenericConfigurationEngine configuration) {
        assertNoAccess(parameter, scopeForAccessCheck(parameter, configuration));
    }

    private ParameterScope scopeForAccessCheck(Parameter<?> parameter, GenericConfigurationEngine configuration) {
        final ParameterScope scope;
        if (configuration instanceof ConfigurationEngine) {
            scope = parameter.getScope();
        } else if (configuration instanceof ApplicationConfigurationEngine
                || configuration instanceof NodeConfigurationEngine
                || configuration instanceof DivisionConfigurationEngine) {
            scope = ParameterScope.DIVISION;    // same rules as for division config
        } else if (configuration instanceof UserConfigurationEngine) {
            scope = ParameterScope.USER;
        } else if (configuration instanceof GlobalConfigurationEngine) {
            scope = ParameterScope.GLOBAL;
        } else if (configuration instanceof MandatorConfigurationEngine) {
            scope = ParameterScope.MANDATOR;
        } else {
            throw new IllegalArgumentException("Unknown configuration engine: " + configuration);
        }
        return scope;
    }
}
