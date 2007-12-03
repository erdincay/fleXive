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
 * @version $Id: NameBindingIterator.java,v 1.1.24.1 2005/04/06 21:25:10 starksm Exp $
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