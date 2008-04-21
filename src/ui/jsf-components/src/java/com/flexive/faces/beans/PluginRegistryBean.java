/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.plugin.ExtensionPoint;
import com.flexive.faces.plugin.Plugin;
import com.flexive.faces.plugin.PluginExecutor;
import com.flexive.faces.plugin.PluginFactory;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An application-scoped beans that serves as a plugin registry.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class PluginRegistryBean implements Serializable {
    private static final long serialVersionUID = -1931267445285693491L;
    private static final transient Log LOG = LogFactory.getLog(PluginRegistryBean.class);
    private static final String CONFIG_FILENAME = "META-INF/flexive-plugins-config.xml";

    private final ConcurrentMap<ExtensionPoint, List<Plugin>> plugins
            = new ConcurrentHashMap<ExtensionPoint, List<Plugin>>();
    private final Map<ExtensionPoint, Class> pluginExecutorTypes = new HashMap<ExtensionPoint, Class>();

    /**
     * Constructor. Search the classpath for plugin config files and initialize all plugins.
     */
    @SuppressWarnings({"unchecked"})
    public PluginRegistryBean() {
        try {
            // find all config files in our application's classpath
            final Enumeration<URL> configFiles = Thread.currentThread().getContextClassLoader().getResources(CONFIG_FILENAME);
            while (configFiles.hasMoreElements()) {
                final URL configFile = configFiles.nextElement();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Processing plugin config: " + configFile.getPath());
                }
                final Document document = getXmlDocument(configFile);
                // get plugin-factory elements
                final NodeList factories = document.getElementsByTagName("plugin-factory");
                for (int i = 0; i < factories.getLength(); i++) {
                    // get the FQCN of the PluginFactory instance
                    final String className = StringUtils.trim(factories.item(i).getTextContent());
                    try {
                        // load the factory class
                        final Class<?> factoryClass = Thread.currentThread().getContextClassLoader().loadClass(className);
                        if (!(PluginFactory.class.isAssignableFrom(factoryClass))) {
                            if (LOG.isErrorEnabled()) {
                                LOG.error("Plugin factory " + className + " does not implement "
                                        + PluginFactory.class.getCanonicalName() + " (ignored).");
                            }
                            continue;
                        }
                        try {
                            // instantiate factory
                            final PluginFactory factory = ((Class<PluginFactory>) factoryClass).newInstance();
                            // initialize plugins
                            if (LOG.isInfoEnabled()) {
                                LOG.info("Adding flexive plugin callbacks from " + className + "...");
                            }
                            // add plugin callbacks
                            factory.initialize(this);
                        } catch (Exception e) {
                            LOG.error("Failed to instantiate plugin factory (ignored): " + e.getMessage(), e);
                        }
                    } catch (ClassNotFoundException e) {
                        LOG.error("Plugin factory class not found (ignored): " + className);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to initialize plugin registry bean: " + e.getMessage(), e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Return the singleton (application-scoped) instance of this managed beans.
     *
     * @return the singleton (application-scoped) instance of this managed beans.
     */
    public static PluginRegistryBean getInstance() {
        return (PluginRegistryBean) FxJsfUtils.getManagedBean("fxPluginRegistryBean");
    }

    /**
     * Register a plugin for the given extension point. The extension point must have
     * its executor type statically bound, i.e. it must not be generic (see {@link ExtensionPoint}). This
     * method is most likely called from a {@link com.flexive.faces.plugin.PluginFactory} to register plugin callbacks.
     *
     * @param extensionPoint the extension point
     * @param callback       the plugin handler to be added
     */
    public <PEX extends PluginExecutor> void registerPlugin(ExtensionPoint<PEX> extensionPoint, Plugin<PEX> callback) {
        // check if the extension point type parameter has been bound
        if (!(extensionPoint.getClass().getGenericSuperclass() instanceof ParameterizedType)
                || !(((ParameterizedType) extensionPoint.getClass().getGenericSuperclass()).getActualTypeArguments()[0] instanceof Class)) {
            throw new FxInvalidParameterException("extensionPoint", LOG, "ex.jsf.pluginRegistry.executorTypeNotBound",
                    extensionPoint.getClass().getCanonicalName()).asRuntimeException();
        }
        // get the executor class from the extension point. this works since we know that
        // ExtensionPoint has to be subclassed and needs its type parameter to be bound
        final Class executorClass = (Class) ((ParameterizedType) extensionPoint.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        synchronized (plugins) {
            if (!pluginExecutorTypes.containsKey(extensionPoint)) {
                pluginExecutorTypes.put(extensionPoint, executorClass);
            }
            if (!pluginExecutorTypes.get(extensionPoint).isAssignableFrom(executorClass)) {
                throw new FxInvalidParameterException("extensionPoint", LOG, "ex.jsf.pluginRegistry.incompatibleExecutor",
                        pluginExecutorTypes.get(extensionPoint).getCanonicalName(),
                        callback.getClass().getCanonicalName()).asRuntimeException();
            }
            if (!plugins.containsKey(extensionPoint)) {
                plugins.put(extensionPoint, new ArrayList<Plugin>());
            }
            plugins.get(extensionPoint).add(callback);
        }
    }

    /**
     * Return all plugins registered for the given extension point.
     *
     * @param extensionPoint the extension point
     * @return all plugins registered for the given extension point.
     */
    @SuppressWarnings({"unchecked"})
    public <PEX extends PluginExecutor> List<Plugin<PEX>> getPlugins(ExtensionPoint<PEX> extensionPoint) {
        final List<? extends Plugin> extensionPoints = plugins.get(extensionPoint);
        return extensionPoints == null ? new ArrayList<Plugin<PEX>>() : (List<Plugin<PEX>>) extensionPoints;
    }

    /**
     * Execute all registered plugins for {@code extensionPoint} using the given {@code executor} object.
     *
     * @param extensionPoint the extension point
     * @param executor       the executor to handle all registered plugins
     */
    public <PEX extends PluginExecutor> void execute(ExtensionPoint<PEX> extensionPoint, PEX executor) {
        final List<Plugin<PEX>> callbacks = getPlugins(extensionPoint);
        for (Plugin<PEX> callback : callbacks) {
            callback.apply(executor);
        }
    }

    /**
     * Remove all plugins of the given extension point.
     *
     * @param extensionPoint the extension point to be cleared.
     */
    public void clearPlugins(ExtensionPoint extensionPoint) {
        plugins.remove(extensionPoint);
    }

    /**
     * Return the XML document represented by the given URL.
     *
     * @param configFile the config file URL
     * @return the XML document represented by the given URL.
     * @throws ParserConfigurationException if the parser could not be instantiated
     * @throws SAXException                 if the document could not be parsed
     * @throws IOException                  if the file could not be read
     */
    private Document getXmlDocument(URL configFile) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setValidating(false);
        final DocumentBuilder builder = builderFactory.newDocumentBuilder();
        return builder.parse(configFile.openStream());
    }

}
