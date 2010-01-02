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
package com.flexive.shared.mbeans;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.cache.FxBackingCache;
import com.flexive.shared.cache.FxCacheException;
import org.jboss.cache.Cache;

import javax.management.*;
import java.util.Set;

/**
 * Proxy for the FxCache MBean (only for internal use!)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxCacheProxy implements FxCacheMBean {
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
    public void create() throws Exception {
        server.invoke(name, "create", new Object[0], new String[0]);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() throws Exception {
        server.invoke(name, "destroy", new Object[0], new String[0]);
    }

    /**
     * {@inheritDoc}
     */
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
    public Object get(String path, Object key) throws FxCacheException {
        try {
            return server.invoke(name, "get", new Object[]{path, key},
                    new String[]{"java.lang.String", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void put(String path, Object key, Object value) throws FxCacheException {
        try {
            server.invoke(name, "put", new Object[]{path, key, value},
                    new String[]{"java.lang.String", "java.lang.Object", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(String path) throws FxCacheException {
        try {
            server.invoke(name, "remove", new Object[]{path},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(String path, Object key) throws FxCacheException {
        try {
            server.invoke(name, "remove", new Object[]{path, key},
                    new String[]{"java.lang.String", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set getKeys(String path) throws FxCacheException {
        try {
            return (Set) server.invoke(name, "getKeys", new Object[]{path},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set globalGetKeys(String path) throws FxCacheException {
        try {
            return (Set) server.invoke(name, "globalGetKeys", new Object[]{path},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object globalGet(String path, Object key) throws FxCacheException {
        try {
            return server.invoke(name, "globalGet", new Object[]{path, key},
                    new String[]{"java.lang.String", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean globalExists(String path, Object key) throws FxCacheException {
        try {
            return (Boolean) server.invoke(name, "globalExists", new Object[]{path, key},
                    new String[]{"java.lang.String", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(String path, Object key) throws FxCacheException {
        try {
            return (Boolean) server.invoke(name, "exists", new Object[]{path, key},
                    new String[]{"java.lang.String", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void globalPut(String path, Object key, Object value) throws FxCacheException {
        try {
            server.invoke(name, "globalPut", new Object[]{path, key, value},
                    new String[]{"java.lang.String", "java.lang.Object", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void globalRemove(String path) throws FxCacheException {
        try {
            server.invoke(name, "globalRemove", new Object[]{path},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void globalRemove(String path, Object key) throws FxCacheException {
        try {
            server.invoke(name, "globalRemove", new Object[]{path, key},
                    new String[]{"java.lang.String", "java.lang.Object"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set getChildrenNames(String path) throws FxCacheException {
        try {
            return (Set) server.invoke(name, "getChildrenNames", new Object[]{path},
                    new String[]{"java.lang.String"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
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
    public void reloadEnvironment(Integer divisionId) throws Exception {
        try {
            server.invoke(name, "reloadEnvironment", new Object[]{divisionId},
                    new String[]{"java.lang.Integer"});
        } catch (InstanceNotFoundException e) {
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
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
            throw new FxCacheException("No FxCache instance found!");
        } catch (MBeanException e) {
            throw new FxCacheException("Cache error invoking the managed bean: " + e.getMessage() + ", TargetException: " +
                    (e.getTargetException() == null ? "(unknown)" : e.getTargetException().getMessage()));
        } catch (ReflectionException e) {
            throw new FxCacheException("Could not invoke operation on FxCache (reflection error): " + e.getMessage());
        }
    }
}
