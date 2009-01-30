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
package com.flexive.shared;

import com.flexive.shared.exceptions.FxLookupException;
import com.flexive.shared.interfaces.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.SessionContext;
import javax.naming.*;
import javax.transaction.TransactionManager;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Utility class for EJB lookups.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class EJBLookup {
    private static final Log LOG = LogFactory.getLog(EJBLookup.class);
    private static String APPNAME = "flexive";

    private enum STRATEGY {
        APP_SIMPLENAME_LOCAL,
        JAVA_COMP_ENV,
        APP_SIMPLENAME_REMOTE,
        COMPLEXNAME,
        SIMPLENAME_LOCAL,
        SIMPLENAME_REMOTE,
        GERONIMO_LOCAL,
        GERONIMO_REMOTE,
        WEBLOGIC_REMOTE,
        UNKNOWN
    }

    private static STRATEGY used_strategy = null;
    private static final ConcurrentMap<String, Object> ejbCache = new ConcurrentHashMap<String, Object>(100);

    /**
     * Protected default constructor to avoid instantiation.
     */
    protected EJBLookup() {
    }

    /**
     * Lookup of the AccountEngine EJB.
     *
     * @return a reference to the AccountEngine EJB
     */
    public static AccountEngine getAccountEngine() {
        return getEngine(AccountEngine.class);
    }

    /**
     * Lookup of the SqlSearch EJB.
     *
     * @return a reference to the SqlSearch EJB
     */
    public static SearchEngine getSearchEngine() {
        return getEngine(SearchEngine.class);
    }

    /**
     * Lookup of the Briefcase EJB.
     *
     * @return a reference to the Briefcase EJB
     */
    public static BriefcaseEngine getBriefcaseEngine() {
        return getEngine(BriefcaseEngine.class);
    }

    /**
     * Lookup of the GroupEngine EJB.
     *
     * @return a reference to the GroupEngine EJB
     */
    public static UserGroupEngine getUserGroupEngine() {
        return getEngine(UserGroupEngine.class);
    }

    /**
     * Lookup of the MandatorEngine EJB.
     *
     * @return a reference to the MandatorEngine EJB
     */
    public static MandatorEngine getMandatorEngine() {
        return getEngine(MandatorEngine.class);
    }

    /**
     * Lookup of the ACLEngine EJB.
     *
     * @return a reference to the ACLEngine EJB
     */
    public static ACLEngine getAclEngine() {
        return getEngine(ACLEngine.class);
    }


    /**
     * Lookup of the Language EJB.
     *
     * @return a reference to the Language EJB
     */
    public static LanguageEngine getLanguageEngine() {
        return getEngine(LanguageEngine.class);
    }

    /**
     * Lookup of the UserConfigurationEngine EJB.
     *
     * @return a reference to the UserConfigurationEngine EJB
     */
    public static UserConfigurationEngine getUserConfigurationEngine() {
        return getEngine(UserConfigurationEngine.class);
    }

    /**
     * Lookup of the DivisionConfigurationEngine EJB.
     *
     * @return a reference to the DivisionConfigurationEngine EJB
     */
    public static DivisionConfigurationEngine getDivisionConfigurationEngine() {
        return getEngine(DivisionConfigurationEngine.class);
    }

    /**
     * Lookup of the GlobalConfigurationBean EJB.
     *
     * @return a reference to the GlobalConfigurationBean EJB.
     */
    public static GlobalConfigurationEngine getGlobalConfigurationEngine() {
        return getEngine(GlobalConfigurationEngine.class);
    }

    /**
     * Lookup of the FxConfiguration EJB.
     *
     * @return a reference to the FxConfiguration EJB.
     */
    public static ConfigurationEngine getConfigurationEngine() {
        return getEngine(ConfigurationEngine.class);
    }

    /**
     * Lookup of the AssignmentEngine EJB.
     *
     * @return a reference to the AssignmentEngine EJB.
     */
    public static AssignmentEngine getAssignmentEngine() {
        return getEngine(AssignmentEngine.class);
    }

    /**
     * Lookup of the TypeEngine EJB.
     *
     * @return a reference to the TypeEngine EJB.
     */
    public static TypeEngine getTypeEngine() {
        return getEngine(TypeEngine.class);
    }

    /**
     * Lookup of the SelectListEngine EJB.
     *
     * @return a reference to the SelectListEngine EJB
     */
    public static SelectListEngine getSelectListEngine() {
        return getEngine(SelectListEngine.class);
    }

    /**
     * Lookup of the WorkflowEngine EJB.
     *
     * @return a reference to the WorkflowEngine EJB.
     */
    public static WorkflowEngine getWorkflowEngine() {
        return getEngine(WorkflowEngine.class);
    }

    /**
     * Lookup of the StepEngine EJB.
     *
     * @return a reference to the StepEngine EJB.
     */
    public static StepEngine getWorkflowStepEngine() {
        return getEngine(StepEngine.class);
    }

    /**
     * Lookup of the RouteEngine EJB.
     *
     * @return a reference to the RouteEngine EJB.
     */
    public static RouteEngine getWorkflowRouteEngine() {
        return getEngine(RouteEngine.class);
    }

    /**
     * Lookup of the StepDefinitionEngine EJB.
     *
     * @return a reference to the StepDefinitionEngine EJB.
     */
    public static StepDefinitionEngine getWorkflowStepDefinitionEngine() {
        return getEngine(StepDefinitionEngine.class);
    }

    /**
     * Lookup of the ContentEngine EJB.
     *
     * @return a reference to the ContentEngine EJB.
     */
    public static ContentEngine getContentEngine() {
        return getEngine(ContentEngine.class);
    }

    /**
     * Lookup of the StatelessTest EJB.
     *
     * @return a reference to the StatelessTest EJB.
     */
    public static StatelessTest getStatelessTestInterface() {
        return getEngine(StatelessTest.class);
    }

    /**
     * Lookup of the SequencerEngine EJB.
     *
     * @return a reference to the SequencerEngine EJB.
     */
    public static SequencerEngine getSequencerEngine() {
        return getEngine(SequencerEngine.class);
    }

    /**
     * Lookup of the ScriptingEngine EJB
     *
     * @return a reference to the ScriptingEngine EJB
     */
    public static ScriptingEngine getScriptingEngine() {
        return getEngine(ScriptingEngine.class);
    }

    /**
     * Lookup of the FxTimerService EJB
     *
     * @return a reference to the FxTimerService EJB
     */
    public static FxTimerService getTimerService() {
        return getEngine(FxTimerService.class);
    }

    /**
     * Lookup of the FxTree EJB
     *
     * @return a reference to the FxTree EJB
     */
    public static TreeEngine getTreeEngine() {
        return getEngine(TreeEngine.class);
    }

    /**
     * Lookup of the ResultPreferencesEngine EJB.
     *
     * @return a reference to the ResultPreferencesEngine EJB.
     */
    public static ResultPreferencesEngine getResultPreferencesEngine() {
        return getEngine(ResultPreferencesEngine.class);
    }

    /**
     * Lookup of the HistoryTrackerEngine EJB.
     *
     * @return a reference to the HistoryTrackerEngine EJB
     */
    public static HistoryTrackerEngine getHistoryTrackerEngine() {
        return getEngine(HistoryTrackerEngine.class);
    }

    /**
     * Get a reference of the transaction manager
     *
     * @return TransactionManager
     */
    public static TransactionManager getTransactionManager() {
        try {
            InitialContext ctx = new InitialContext();
            try {
                return (TransactionManager) ctx.lookup("java:TransactionManager");
            } catch (NamingException e) {
                try {
                    return (TransactionManager) ctx.lookup("java:comp/TransactionManager");
                } catch (NamingException e2) {
                    return (TransactionManager) ctx.lookup("java:appserver/TransactionManager");
                }
            }
        } catch (Exception e) {
            throw new FxLookupException("Failed to lookup transaction manager: " + e.getMessage(), e).asRuntimeException();
        }
    }

    /**
     * Get a reference of the current EJB session context.
     *
     * @return the EJB session context
     */
    public static SessionContext getSessionContext() {
        try {
            final InitialContext ctx = new InitialContext();
            return (SessionContext) ctx.lookup("java:comp/EJBContext");
        } catch (NamingException e) {
            throw new FxLookupException("Failed to lookup session context: " + e.getMessage(), e).asRuntimeException();
        }
    }

    /**
     * Lookup the EJB found under the given Class's name. Uses default flexive naming scheme.
     *
     * @param type        EJB interface class instance
     * @param appName     EJB application name
     * @param environment optional environment for creating the initial context
     * @param <T>         EJB interface type
     * @return a reference to the given EJB
     */
    protected static <T> T getInterface(Class<T> type, String appName, Hashtable<String, String> environment) {
        // Try to obtain interface from the lookup cache
        Object ointerface = ejbCache.get(type.getName());
        if (ointerface != null) {
            return type.cast(ointerface);
        }

        // Cache miss: obtain interface and store it in the cache
        synchronized (EJBLookup.class) {
            String name;
            InitialContext ctx = null;
            try {
                if (used_strategy == null) {
                    appName = discoverStrategy(appName, environment, type);
                    if (used_strategy != null) {
                        LOG.info("Working lookup strategy: " + used_strategy);
                    } else {
                        LOG.error("No working lookup strategy found! Possibly because of pending redeployment.");
                    }
                }
                if (environment == null)
                    environment = new Hashtable<String, String>();
                prepareEnvironment(used_strategy, environment);
                ctx = new InitialContext(environment);
                name = buildName(appName, type);
                ointerface = ctx.lookup(name);
                ejbCache.putIfAbsent(type.getName(), ointerface);

                return type.cast(ointerface);
            } catch (Exception exc) {
                throw new FxLookupException(LOG, exc, "ex.ejb.lookup.failure", type, exc).asRuntimeException();
            } finally {
                if (ctx != null)
                    try {
                        ctx.close();
                    } catch (NamingException e) {
                        LOG.error("Failed to close context: " + e.getMessage(), e);
                    }
            }
        }
    }

    /**
     * Discover which lookup strategy works for the given class
     *
     * @param appName     EJB application name
     * @param environment properties passed to the initial context
     * @param type        the class
     * @return appName (may have changed)
     */
    private static <T> String discoverStrategy(String appName, final Hashtable<String, String> environment, Class<T> type) {
        InitialContext ctx = null;
        for (STRATEGY strat : STRATEGY.values()) {
            if (strat == STRATEGY.UNKNOWN)
                continue;
            used_strategy = strat;
            try {
                final Hashtable<String, String> env = environment != null ? environment : new Hashtable<String, String>();
                prepareEnvironment(strat, env);
                ctx = new InitialContext(env);
                ctx.lookup(buildName(appName, type));
                return appName;
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Strategy " + strat + " failed: " + e.getMessage(), e);
                }
                //ignore and try next
            }
        }
        //houston, we have a problem - try locale and remote with appname again iterating through all "root" ctx bindings
        //this can happen if the ear is not named flexive.ear
        try {
            if (ctx == null)
                ctx = new InitialContext(environment);
            NamingEnumeration<NameClassPair> ncpe = ctx.list("");
            while (ncpe.hasMore()) {
                NameClassPair ncp = ncpe.next();
                if (ncp.getClassName().endsWith("NamingContext")) {
                    appName = ncp.getName();
                    try {
                        used_strategy = STRATEGY.APP_SIMPLENAME_LOCAL;
                        ctx.lookup(buildName(ncp.getName(), type));
                        APPNAME = ncp.getName();
                        LOG.info("Using application name [" + appName + "] for lookups!");
                        return APPNAME;
                    } catch (Exception e) {
                        //ignore and try remote
                    }
                    try {
                        used_strategy = STRATEGY.APP_SIMPLENAME_REMOTE;
                        ctx.lookup(buildName(ncp.getName(), type));
                        APPNAME = ncp.getName();
                        LOG.info("Using application name [" + appName + "] for lookups!");
                        return APPNAME;
                    } catch (Exception e) {
                        //ignore and try remote
                    }
                }
            }

        } catch (Exception e) {
            LOG.warn(e);
        }
        used_strategy = null;
        return appName;
    }

    /**
     * Adjust the environment with strategy/application server specific settings
     *
     * @param strat       strategy
     * @param environment environment
     */
    private static void prepareEnvironment(STRATEGY strat, Hashtable<String, String> environment) {
        if (strat == STRATEGY.GERONIMO_LOCAL || strat == STRATEGY.GERONIMO_REMOTE) {
            environment.put("java.naming.factory.initial", strat == STRATEGY.GERONIMO_LOCAL
                    ? "org.apache.openejb.client.LocalInitialContextFactory"
                    : "org.apache.openejb.client.RemoteInitialContextFactory");
            if (!StringUtils.isEmpty(System.getProperty("java.naming.provider.url")))
                environment.put("java.naming.provider.url", System.getProperty("java.naming.provider.url"));
            else
                environment.put("java.naming.provider.url", "localhost:4201");
        }
    }

    /**
     * Adjust the environment with strategy/application server specific settings
     *
     * @return Context
     * @throws NamingException on errors
     */
    public static Context getInitialContext() throws NamingException {
        if (used_strategy == STRATEGY.GERONIMO_LOCAL || used_strategy == STRATEGY.GERONIMO_REMOTE) {
            return getOpenEJBRootContext();

        } else
            return new InitialContext();
    }

    private static Context getOpenEJBRootContext() {
        // use reflection to return OpenEJB's system context to enable access to the configured datasource
        // in every bean - the standard OpenEJB context leads to quirky workarounds for the datasources
        try {
            final Class<?> clsSystem = Class.forName("org.apache.openejb.loader.SystemInstance");

            // invoke static method SystemInstance.get()
            final Object systemInstance = clsSystem.getMethod("get").invoke(null);

            // invoke getComponent on systemInstance
            final Class<?> clsContainer = Class.forName("org.apache.openejb.spi.ContainerSystem");
            final Object containerSystem =
                    clsSystem.getMethod("getComponent", Class.class)
                            .invoke(systemInstance, clsContainer);

            // return system JNDI context - containerSystem.getJNDIContext()
            return (Context) clsContainer.getMethod("getJNDIContext").invoke(containerSystem);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Lookup strategy is OpenEJB, but OpenEJB classes missing: "
                    + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Build the correct JNDI name to request for lookups depending on the discovered lookup strategy
     *
     * @param appName EJB application name
     * @param type    the class to lookup
     * @return JNDI name
     */
    private static <T> String buildName(String appName, Class<T> type) {
        switch (used_strategy) {
            case APP_SIMPLENAME_LOCAL:
                return appName + "/" + type.getSimpleName() + "/local";
            case APP_SIMPLENAME_REMOTE:
                return appName + "/" + type.getSimpleName() + "/remote";
            case COMPLEXNAME:
                return type.getCanonicalName();
            case SIMPLENAME_LOCAL:
                return type.getSimpleName() + "/local";
            case SIMPLENAME_REMOTE:
                return type.getSimpleName() + "/remote";
            case JAVA_COMP_ENV:
                return "java:comp/env/" + type.getSimpleName();
            case GERONIMO_LOCAL:
                return "/" + type.getSimpleName() + "Local";
            case GERONIMO_REMOTE:
                return "/" + type.getSimpleName() + "Remote";
            case WEBLOGIC_REMOTE:
                return type.getSimpleName() + "#" + type.getCanonicalName();
            default:
                throw new FxLookupException("Unsupported/unknown lookup strategy " + used_strategy + "!").asRuntimeException();
        }
    }

    /**
     * Lookup the EJB found under the given Class's name. Uses default flexive naming scheme.
     *
     * @param appName EJB application name
     * @param type    EJB interface class instance
     * @param <T>     EJB interface type
     * @return a reference to the given EJB
     */
    public static <T> T getEngine(String appName, Class<T> type) {
        return getInterface(type, appName, null);
    }

    /**
     * Lookup the EJB found under the given Class's name. Uses default flexive naming scheme.
     *
     * @param type EJB interface class instance
     * @param <T>  EJB interface type
     * @return a reference to the given EJB
     */
    public static <T> T getEngine(Class<T> type) {
        return getInterface(type, APPNAME, null);
    }
}
