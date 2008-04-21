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
package com.flexive.war.webdav;

/*
* JBoss, the OpenSource J2EE webOS
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.spi.DirectoryManager;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An implementation of NamingEnumeration for listing the Bindings
 * in a context. It accepts an Iterator of DirBindings and transforms
 * the raw object and attributes into the output object using the
 * DirectoryManager.getObjectInstance method.
 *
 * @author Scott_Stark@displayscape.com
 * @version $Rev$
 * @see DirBinding
 */
public class NameBindingIterator implements NamingEnumeration {
    private Iterator bindings;
    private DirContext context;

    /**
     * Creates new NameBindingIterator for enumerating a list of Bindings.
     * This is the name and raw object data/attributes that should be input into
     * DirectoryManager.getObjectInstance().
     */
    public NameBindingIterator(Iterator bindings, DirContext context) {
        this.bindings = bindings;
        this.context = context;
    }

    public void close() throws NamingException {
    }

    public boolean hasMore() throws NamingException {
        return bindings.hasNext();
    }

    public Object next() throws NamingException {
        DirBinding binding = (DirBinding) bindings.next();
        Object rawObject = binding.getObject();
        Name name = new DefaultName(binding.getName());
        Hashtable env = context.getEnvironment();
        try {
            Object instanceObject = DirectoryManager.getObjectInstance(rawObject,
                    name, context, env, binding.getAttributes());
            binding.setObject(instanceObject);
        }
        catch (Exception e) {
            NamingException ne = new NamingException("getObjectInstance failed");
            ne.setRootCause(e);
            throw ne;
        }
        return binding;
    }

    public boolean hasMoreElements() {
        boolean hasMore = false;
        try {
            hasMore = hasMore();
        }
        catch (NamingException e) {
        }
        return hasMore;
    }

    public Object nextElement() {
        Object next = null;
        try {
            next = next();
        }
        catch (NamingException e) {
            throw new NoSuchElementException(e.toString());
        }
        return next;
    }
}