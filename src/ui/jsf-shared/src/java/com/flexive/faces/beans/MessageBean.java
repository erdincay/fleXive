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

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.value.renderer.FxValueRendererFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>A generic localization beans for messages displayed in the UI. The MessageBean wraps
 * one or more {@link ResourceBundle ResourceBundles} that provide localized messages for
 * web applications or plugins. By providing a localized resource bundle with a fixed name
 * in your plugin/application JAR file, these messages will be automatically detected during
 * startup and can be accessed through the message beans.</p>
 * <p/>
 * <p>
 * Include the resource bundle with one of the following base names in the root directory
 * of a JAR file deployed with the application:
 * <p/>
 * <table>
 * <tr>
 * <th>{@link #BUNDLE_APPLICATIONS}</th>
 * <td>for web applications</td>
 * </tr>
 * <tr>
 * <th>{@link #BUNDLE_PLUGINS}</th>
 * <td>for plugins (or other JAR files providing localized messages)</td>
 * </tr>
 * </table>
 * <p/>
 * Currently both resource bundle types are treated equally, except that all application resource
 * bundles are queried before the first plugin resource bundle.
 * </p>
 * <p/>
 * <p><b>Usage:</b> fxMessageBean[key] to get the translation
 * of the given property name in the user's language.
 * Parameters in the localized message can be replaced too, by placing EL expressions inside the lookup string:
 * Using a message declaration of "my.message.key=1+1 is: {0}", the placeholder {0} will be replaced
 * using the following EL code:
 * <pre>
 * fxMessageBean['my.message.key,#{1+1}']
 * </pre>
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class MessageBean extends HashMap {
    private static final long serialVersionUID = 2176514561683264331L;

    /**
     * Resource bundle name for plugin packages.
     */
    public static final String BUNDLE_PLUGINS = "PluginMessages";
    /**
     * Resource bundle name for web applications.
     */
    public static final String BUNDLE_APPLICATIONS = "ApplicationMessages";
    /**
     * Resource bundle name for exceptions.
     */
    public static final String BUNDLE_EXCEPTIONS = "FxExceptionMessages";

    private static final Log LOG = LogFactory.getLog(MessageBean.class);
    private final List<FxSharedUtils.BundleReference> resourceBundles = new CopyOnWriteArrayList<FxSharedUtils.BundleReference>();
    private final ConcurrentMap<String, ResourceBundle> cachedBundles = new ConcurrentHashMap<String, ResourceBundle>();
    private final ConcurrentMap<FxSharedUtils.MessageKey, String> cachedMessages = new ConcurrentHashMap<FxSharedUtils.MessageKey, String>();
    private volatile boolean initialized = false;

    public MessageBean() {
        initialize();
    }

    /**
     * Return the managed instance of the message beans.
     *
     * @return the managed instance of the message beans.
     */
    public static MessageBean getInstance() {
        return (MessageBean) FxJsfUtils.getManagedBean("fxMessageBean");
    }

    /**
     * Return the localized message, replacing {0}...{n} with the given args.
     * < b/>
     * Parameters may be contained in the key and are comma separated. JSF EL expressions
     * are evaluated in the current faces context.<br/>
     * Examples: <br/>
     * key="xx.yy.myMessage,myParam1,myParam2" <br/>
     * key="xx.yy.myMessage,#{1+2},#{myBean.value},'some literal string value'"
     *
     * @param key the message key (optional with parameters)
     * @return the formatted message
     */
    @Override
    public Object get(Object key) {
        // The key may contain parameters, which are comma separated
        String sKey = String.valueOf(key);
        String sParams[] = null;
        try {
            String[] arr = FxSharedUtils.splitLiterals(sKey);
            sKey = arr[0];
            if (arr.length > 1) {
                sParams = new String[arr.length - 1];
                System.arraycopy(arr, 1, sParams, 0, sParams.length);
                for (int i = 1; i < arr.length; i++) {
                    // evaluate parameters
                    Object value;
                    try {
                        value = FxJsfUtils.evalObject(arr[i]);
                    } catch (Exception e) {
                        LOG.warn("Failed to evaluate parameter " + arr[i] + ": " + e.getMessage(), e);
                        value = "";
                    }
                    //noinspection unchecked
                    sParams[i - 1] = value != null
                            ? (value instanceof FxValue
                            ? FxValueRendererFactory.getInstance().format((FxValue) value)
                            : value.toString())
                            : null;
                }
            }
        } catch (Throwable t) {
            LOG.error("Failed to convert parameters (ignored): " + t.getMessage(), t);
        }

        return getMessage(sKey, (Object[]) sParams);
    }


    /**
     * Return the localized message, replacing {0}...{n} with the given args. FxString
     * objects will be translated in the user's locale automatically.
     *
     * @param key  message key
     * @param args optional arguments for
     * @return the formatted message
     */
    public String getMessage(String key, Object... args) {
        String result;
        try {
            result = getResource(key);
            if (args != null && args.length > 0) {
                result = FxFormatUtils.formatResource(result, FxContext.get().getLanguage().getId(), args);
            }
            return result;
        } catch (MissingResourceException e) {
            LOG.warn("Unknown message key: " + key);
            return "??" + key + "??";
        }
    }

    /**
     * Returns the resource translation in the current user's language.
     *
     * @param key resource key
     * @return the resource translation
     */
    public String getResource(String key) {
        final Locale locale = FxContext.get().getLocale();
        return getResource(key, locale);
    }

    /**
     * Returns the resource translation in the given locale.
     *
     * @param key       resource key
     * @param locale    the requested locale
     * @return the resource translation
     * @since 3.1.7
     */
    public String getResource(String key, Locale locale) {
        if (!initialized) {
            initialize();
        }
        final FxSharedUtils.MessageKey messageKey = new FxSharedUtils.MessageKey(locale, key);
        String cachedMessage = cachedMessages.get(messageKey);
        if (cachedMessage != null) {
            return cachedMessage;
        }
        for (FxSharedUtils.BundleReference bundleReference : resourceBundles) {
            try {
                final ResourceBundle bundle = getResources(bundleReference, locale);
                String message = bundle.getString(key);
                cachedMessage = cachedMessages.putIfAbsent(messageKey, message);
                return cachedMessage != null ? cachedMessage : message;
            } catch (MissingResourceException e) {
                // continue with next bundle
            }
        }
        if (!locale.equals(Locale.ENGLISH)) {
            //try to find the locale in english as last resort
            //this is a fix for using PropertyResourceBundles which can only handle one locale (have to use them thanks to JBoss 5...)
            for (FxSharedUtils.BundleReference bundleReference : resourceBundles) {
                try {
                    final ResourceBundle bundle = getResources(bundleReference, Locale.ENGLISH);
                    String message = bundle.getString(key);
                    cachedMessage = cachedMessages.putIfAbsent(messageKey, message);
                    return cachedMessage != null ? cachedMessage : message;
                } catch (MissingResourceException e) {
                    // continue with next bundle
                }
            }
        }
        throw new MissingResourceException("Resource not found", "MessageBean", key);
    }


    /**
     * Return the resource bundle in the given locale. Uses caching to speed up
     * lookups.
     *
     * @param bundleReference the bundle reference object
     * @param locale          the requested locale
     * @return the resource bundle in the requested locale
     */
    private ResourceBundle getResources(FxSharedUtils.BundleReference bundleReference, Locale locale) {
        final String key = bundleReference.getCacheKey(locale);
        ResourceBundle bundle = cachedBundles.get(key);
        if (bundle == null) {
            final ResourceBundle cachedBundle = cachedBundles.putIfAbsent(key, bundle = bundleReference.getBundle(locale));
            if (cachedBundle != null) {
                return cachedBundle;
            }
        }
        return bundle;
    }


    /**
     * Initialize the application resource bundles. Scans the classpath for resource bundles
     * for a predefined set of names ({@link #BUNDLE_APPLICATIONS} and {@link #BUNDLE_PLUGINS}),
     * and then adds resource references that use {@link URLClassLoader URLClassLoaders} for loading
     * the associated resource bundles.
     */
    private void initialize() {
        if (initialized) {
            return;
        }
        try {
            resourceBundles.addAll(FxSharedUtils.addMessageResources(BUNDLE_APPLICATIONS));
            resourceBundles.addAll(FxSharedUtils.addMessageResources(BUNDLE_PLUGINS));
            resourceBundles.addAll(FxSharedUtils.addMessageResources(BUNDLE_EXCEPTIONS));
            resourceBundles.addAll(FxSharedUtils.addMessageResources(FxSharedUtils.SHARED_BUNDLE));
        } catch (IOException e) {
            LOG.error("Failed to initialize plugin message resources: " + e.getMessage(), e);
        } finally {
            initialized = true;
        }
    }

}
