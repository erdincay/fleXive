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
package com.flexive.shared.mbeans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.flexive.shared.Pair;

/**
 * MBean Helper class
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public final class MBeanHelper {
    private static final transient Log LOG = LogFactory.getLog(MBeanHelper.class);

    //TODO: create a real deployment id at compile time!
    public final static String DEPLOYMENT_ID = "" + System.currentTimeMillis();

    static {
        LOG.info("### Deployment Id: " + DEPLOYMENT_ID + " (" + new Date(Long.parseLong(DEPLOYMENT_ID)) + ")");
    }

    public static MBeanServer locateServer() {
        final List<MBeanServer> servers = findMBeanServers();
        if (LOG.isInfoEnabled())
            for (MBeanServer server : servers) {
                LOG.info("MBeanServer: " + server.getDefaultDomain() + "/" + server.getMBeanCount());
            }
        return servers.get(0);
    }

    /**
     * Searches all available MBean servers for the given object name.
     *
     * @param name  the object name
     * @return  the MBeanInfo of the first server that contains an entry of this name
     * @throws javax.management.InstanceNotFoundException   if the given object was not found on any server
     * @throws javax.management.IntrospectionException  thrown by server factory
     * @throws javax.management.ReflectionException thrown by server factory
     */
    public static Pair<MBeanServer, MBeanInfo> getMBeanInfo(ObjectName name) throws ReflectionException, IntrospectionException, InstanceNotFoundException {
        InstanceNotFoundException notFoundException = null;
        for (MBeanServer server: findMBeanServers()) {
            try {
                return new Pair<MBeanServer, MBeanInfo>(server, server.getMBeanInfo(name));
            } catch (InstanceNotFoundException e) {
                notFoundException = e;
            }
        }
        throw notFoundException;
    }

    private static List<MBeanServer> findMBeanServers() {
        return MBeanServerFactory.findMBeanServer(null);
    }

}
