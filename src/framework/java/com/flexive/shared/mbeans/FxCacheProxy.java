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
package com.flexive.shared.mbeans;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.cache.FxBackingCache;
import com.flexive.shared.cache.FxCacheException;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.Cache;

import javax.management.*;
import java.io.Serializable;
import java.util.Set;

/**
 * Proxy for the FxCache MBean (only for internal use!)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxCacheProxy implements FxCacheMBean {
    private static final Log LOG = LogFactory.getLog(FxCacheProxy.class);

    private MBeanServer server;
    private ObjectName name;



    public FxCacheProxy(MBeanServer server) throws MalformedObjectNameException {
        this.server = server;
        this.name = new ObjectName(CacheAdmin.CACHE_SERVICE_NAME);
    }


    public ObjectName getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() throws Exception {
        server.invoke(name, "create", new Object[0], new String[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception {
        server.invoke(name, "destroy", new Object[0], new String[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cache<Object, Object> getCache() throws FxCacheException {
        return getBackingCache().getCache();
    }

    private FxBackingCache getBackingCache() {
        try {
            return (FxBackingCache) server.getAttribute(name, "FxCache");
        } catch (MBeanException e) {
            throw new RuntimeException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (AttributeNotFoundException e) {
            throw new RuntimeException("Attribute FxCache not found in managed cache bean: " + e.getMessage());
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("No FxCache instance found!");
        } catch (ReflectionException e) {
            throw new RuntimeException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(String path, Object key) throws FxCacheException {
        try {
            return unmarshal(server.invoke(name, "get", new Object[]{divisionEncodePath(path), key},
                    new String[]{"java.lang.String", "java.lang.Object"}));
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String path, Object key, Object value) throws FxCacheException {
        try {
            server.invoke(name, "put", new Object[]{divisionEncodePath(path), key, marshal(value)},
                    new String[]{"java.lang.String", "java.lang.Object", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(String path) throws FxCacheException {
        try {
            server.invoke(name, "remove", new Object[]{divisionEncodePath(path)},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(String path, Object key) throws FxCacheException {
        try {
            server.invoke(name, "remove", new Object[]{divisionEncodePath(path), key},
                    new String[]{"java.lang.String", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set getKeys(String path) throws FxCacheException {
        try {
            return (Set) server.invoke(name, "getKeys", new Object[]{divisionEncodePath(path)},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set globalGetKeys(String path) throws FxCacheException {
        try {
            return (Set) server.invoke(name, "globalGetKeys", new Object[]{globalDivisionEncodePath(path)},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object globalGet(String path, Object key) throws FxCacheException {
        try {
            return unmarshal(server.invoke(name, "globalGet", new Object[]{globalDivisionEncodePath(path), key},
                    new String[]{"java.lang.String", "java.lang.Object"}));
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean globalExists(String path, Object key) throws FxCacheException {
        try {
            return (Boolean) server.invoke(name, "globalExists", new Object[]{globalDivisionEncodePath(path), key},
                    new String[]{"java.lang.String", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(String path, Object key) throws FxCacheException {
        try {
            return (Boolean) server.invoke(name, "exists", new Object[]{divisionEncodePath(path), key},
                    new String[]{"java.lang.String", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void globalPut(String path, Object key, Object value) throws FxCacheException {
        try {
            server.invoke(name, "globalPut", new Object[]{globalDivisionEncodePath(path), key, marshal(value)},
                    new String[]{"java.lang.String", "java.lang.Object", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void globalRemove(String path) throws FxCacheException {
        try {
            server.invoke(name, "globalRemove", new Object[]{globalDivisionEncodePath(path)},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void globalRemove(String path, Object key) throws FxCacheException {
        try {
            server.invoke(name, "globalRemove", new Object[]{globalDivisionEncodePath(path), key},
                    new String[]{"java.lang.String", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set getChildrenNames(String path) throws FxCacheException {
        try {
            return (Set) server.invoke(name, "getChildrenNames", new Object[]{divisionEncodePath(path)},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    @Override
    public boolean isPathLockedInTx(String path) throws FxCacheException {
        try {
            return (Boolean) server.invoke(name, "isPathLockedInTx", new Object[]{divisionEncodePath(path)},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDeploymentId() {
        try {
            return (String) server.getAttribute(name, "DeploymentId");
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new RuntimeException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new RuntimeException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        } catch (AttributeNotFoundException e) {
            throw new RuntimeException("Attribute DeploymentId not found in managed cache bean: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemStartTime() {
        try {
            return (Long) server.getAttribute(name, "SystemStartTime");
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new RuntimeException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new RuntimeException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        } catch (AttributeNotFoundException e) {
            throw new RuntimeException("Attribute SystemStartTime not found in managed cache bean: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNodeStartTime() {
        try {
            return (Long) server.getAttribute(name, "NodeStartTime");
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new RuntimeException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new RuntimeException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        } catch (AttributeNotFoundException e) {
            throw new RuntimeException("Attribute NodeStartTime not found in managed cache bean: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadEnvironment(Integer divisionId) throws Exception {
        try {
            server.invoke(name, "reloadEnvironment", new Object[]{divisionId},
                    new String[]{"java.lang.Integer"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEvictionStrategy(Integer divisionId, String path, Integer maxContents, Integer timeToIdle,
                                    Integer timeToLive) {
        try {
            server.invoke(name, "setEvictionStrategy", new Object[]{
                    divisionId, path, maxContents, timeToIdle, timeToLive},
                    new String[]{
                            "java.lang.Integer",
                            "java.lang.String",
                            "java.lang.Integer",
                            "java.lang.Integer",
                            "java.lang.Integer"});
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new RuntimeException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new RuntimeException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEvictionStrategy(Integer divisionId, String path, Integer maxContents, Integer timeToIdle, Integer timeToLive, Boolean overwrite) throws FxCacheException {
        try {
            server.invoke(name, "setEvictionStrategy", new Object[]{
                    divisionId, path, maxContents, timeToIdle, timeToLive, overwrite},
                    new String[]{
                            "java.lang.Integer",
                            "java.lang.String",
                            "java.lang.Integer",
                            "java.lang.Integer",
                            "java.lang.Integer",
                            "java.lang.Boolean"});
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    @Override
    public void cleanupAfterRequest() throws FxCacheException {
        try {
            server.invoke(name, "cleanupAfterRequest", new Object[0], new String[0]);
        } catch (InstanceNotFoundException e) {
            throw newNotFoundException(e);
        } catch (MBeanException e) {
            throw newMBeanException(e);
        } catch (ReflectionException e) {
            throw newReflectionException(e);
        }
    }

    private FxCacheException newReflectionException(ReflectionException e) {
        return new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage(), e);
    }

    private FxCacheException newMBeanException(MBeanException e) {
        return new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()), e.getTargetException());
    }

    private FxCacheException newNotFoundException(InstanceNotFoundException e) {
        return new FxCacheException("No FxCache instance found!", e);
    }


    /**
     * Marshal a value before writing it to the cache, if necessary.
     *
     * <p>If the cache is shared between several deployments (class loaders) in a VM, we need
     * to serialize all values before writing them to the cache to avoid ClassCastExceptions.
     * This obviously has a grave performance penalty, but really seems to be the only solution in this scenario.
     * For optimal cache performance, deploy your applications in a single EAR
     * or disabled the shared cache if you deploy only a single WAR file.</p>

     * @param value   the value to be written
     * @return        the marshaled value
     * @since         3.1.4
     * @see           CacheAdmin#isSharedCache() 
     */
    public static Object marshal(Object value) {

        // marshal only if the cache is shared between deployments, otherwise let JBoss Cache handle this
        return CacheAdmin.isSharedCache() ? SerializationUtils.serialize((Serializable) value) : value;
    }

    /**
     * Un-marshal a value processed by {@link #marshal(Object)} before.
     *
     * @param value the value to be de-marshaled
     * @return      the original value
     * @since       3.1.4
     */
    public static Object unmarshal(Object value) {
        return value != null ? (CacheAdmin.isSharedCache() ? SerializationUtils.deserialize((byte[]) value) : value) : null;
    }


    /**
     * Includes the division id into the path.
     *
     * @param path the path to encode
     * @return the encoded path
     * @throws FxCacheException if the division id could not be resolved
     */
    private String divisionEncodePath(String path) throws FxCacheException {
        try {
            int divId;
            //#<id>  - purposely undocumented hack to force a division ;) - used during environment loading
            if (path.charAt(0) == '#') {
                try {
                    divId = Integer.parseInt(path.substring(1, path.indexOf('/')));
                    path = path.substring(path.indexOf('/'));
                } catch (Exception e) {
                    throw new FxCacheException("Invalid Division Id in path [" + path + "]!");
                }
            } else {
                FxContext ri = FxContext.get();
                if (ri.getDivisionId() == -1) {
                    throw new FxCacheException("Division ID missing in request information [" + ri.getRequestURI() + "]");
                }
                divId = ri.getDivisionId();
            }
            return "/Division" + divId + (path.startsWith("/") ? "" : "/") + path;
        } catch (Throwable t) {
            LOG.error("Unable to encode division ID in cache path: " + t.getMessage(), t);
            throw new FxCacheException("Unable to encode path: " + t.getMessage());
        }
    }

    /**
     * Includes the global division id into the path.
     *
     * @param path the path to encode
     * @return the encoded path
     */
    public static String globalDivisionEncodePath(final String path) {
        return "/GlobalConfiguration" + (path.startsWith("/") ? "" : "/") + path;
    }


}
