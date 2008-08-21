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
package com.flexive.war.webdav;


import javax.naming.*;
import javax.naming.directory.*;
import java.io.Serializable;
import java.util.*;

/**
 * Memory impementation ofthe DirContext.
 */
public class MemoryDirContext extends DirContextStringImpl implements DirContext, Serializable {
    static final long serialVersionUID = 2547463437468465948L;
    private static NameParser nameParser = DefaultName.getNameParser();
    private HashMap<String, Object> bindings = new HashMap<String, Object>();
    private HashMap<String, Attributes> bindingAttrs = new HashMap<String, Attributes>();
    private MemoryDirContext parent;
    private String contextName;
    private Hashtable env;

    /**
     * Constructor
     */
    public MemoryDirContext() {
        this.contextName = "";
    }

    /**
     * Constructor.
     *
     * @param contextName
     * @param parent
     * @param attributes
     * @throws NamingException
     */
    public MemoryDirContext(String contextName, MemoryDirContext parent, Attributes attributes) throws NamingException {
        this(contextName, parent, attributes, null);
    }

    /**
     * Constructor.
     *
     * @param contextName
     * @param parent
     * @param attributes
     * @param env
     * @throws NamingException
     */
    public MemoryDirContext(String contextName, MemoryDirContext parent, Attributes attributes, Hashtable env) throws NamingException {
        this.contextName = contextName == null ? "" : contextName;
        this.parent = parent;
        bindingAttrs.put("", (Attributes) attributes.clone());
        if (parent != null)
            parent.bind(contextName, this);
        this.env = env != null ? (Hashtable) env.clone() : new Hashtable();
    }

    /**
     * Returns a String representation of the object.
     *
     * @return a String representation of the object.
     */
    public String toString() {
        try {
            return getFullName().toString();
        } catch (NamingException e) {
            return e.getMessage();
        }
    }

    /**
     * Returns the name.
     *
     * @return the name
     */

    String getName() {
        return contextName;
    }

    /**
     * Sets the name.
     *
     * @param contextName the new name
     */
    void setName(String contextName) {
        this.contextName = contextName;
    }

    /**
     * Returns the full name.
     *
     * @return the full name
     * @throws NamingException
     */
    Name getFullName() throws NamingException {
        CompositeName name = new CompositeName(getName());
        MemoryDirContext context = parent;
        if (context == null)
            return name;

        try {
            while (context.parent != null) {
                name.add(0, context.getName());
                context = context.parent;
            }
        } catch (NamingException e) {/*ignore*/}
        return name;
    }


    public Object addToEnvironment(String p1, Object p2) throws NamingException {
        return null;
    }

    public Object removeFromEnvironment(String p1) throws NamingException {
        return null;
    }

    public void bind(Name name, Object value) throws NamingException {
        bind(name, value, null);
    }

    public void bind(Name name, Object value, Attributes attributes) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot bind empty name");
        }

        internalBind(name, value, attributes, true);
    }

    public void close() throws NamingException {
    }

    public Name composeName(Name p1, Name p2) throws NamingException {
        return null;
    }

    public Context createSubcontext(Name name) throws NamingException {
        return createSubcontext(name, null);
    }

    public DirContext createSubcontext(Name name, Attributes attributes) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot createSubcontext with empty name");
        }

        DirContext subctx;
        String atom = name.get(0);
        if (name.size() == 1) {
            subctx = new MemoryDirContext(atom, this, attributes, env);
        } else {
            DirContext context = (DirContext) bindings.get(atom);
            subctx = context.createSubcontext(name.getSuffix(1), attributes);
        }

        return subctx;
    }

    public void destroySubcontext(Name name) throws NamingException {
        unbind(name);
    }

    public Attributes getAttributes(Name name) throws NamingException {
        return getAttributes(name, null);
    }

    public Attributes getAttributes(Name name, String[] attrIDs) throws NamingException {
        Attributes nameAttributes;
        String atom = name.isEmpty() ? "" : name.get(0);
        if (name.isEmpty()) {
            nameAttributes = bindingAttrs.get("");
        } else if (name.size() == 1) {
            Object binding = bindings.get(atom);
            if (binding != null) {
                if (binding instanceof DirContext) {
                    DirContext dirCtx = (DirContext) binding;
                    try {
                        return dirCtx.getAttributes(name.getSuffix(1), attrIDs);
                    } catch (Exception exc) {
                        return new BasicAttributes();
                    }
                }
            }
            nameAttributes = bindingAttrs.get(atom);
        } else {
            DirContext context = (DirContext) bindings.get(atom);
            nameAttributes = context.getAttributes(name.getSuffix(1), attrIDs);
        }

        if (nameAttributes != null && attrIDs != null) {
            BasicAttributes matches = new BasicAttributes(nameAttributes.isCaseIgnored());
            for (String attrID : attrIDs) {
                Attribute attr = nameAttributes.get(attrID);
                if (attr != null) matches.put(attr);
            }
            nameAttributes = matches;
        }
        return nameAttributes;
    }

    public java.util.Hashtable<?, ?> getEnvironment() throws NamingException {
        return (Hashtable<?, ?>) Collections.unmodifiableMap(env);
    }

    public String getNameInNamespace() throws NamingException {
        return toString();
    }

    public NameParser getNameParser(Name p1) throws NamingException {
        return nameParser;
    }

    public DirContext getSchema(Name p1) throws NamingException {
        throw new OperationNotSupportedException("Not implemented yet");
    }

    public DirContext getSchemaClassDefinition(Name p1) throws NamingException {
        throw new OperationNotSupportedException("Not implemented yet");
    }

    public NamingEnumeration<NameClassPair> list(Name p1) throws NamingException {
        NamingEnumeration<Binding> result = listBindings(p1);
        // Cast -> NameClassPair
        ArrayList<NameClassPair> tmp = new ArrayList<NameClassPair>(50);
        while (result.hasMore()) {
            tmp.add((NameClassPair) result.next());
        }
        return new NameBindingIterator(tmp.iterator(), this);
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        NamingEnumeration<Binding> iter;

        if (name.isEmpty()) {
            Iterator keys = bindings.keySet().iterator();
            ArrayList<DirBinding> tmp = new ArrayList<DirBinding>();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Object value = bindings.get(key);
                Attributes attributes = bindingAttrs.get(key);
                DirBinding tuple = new DirBinding(key, value, attributes);
                tmp.add(tuple);
            }
            iter = new NameBindingIterator(tmp.iterator(), this);
        } else {
            String atom = name.get(0);
            Context context = (Context) bindings.get(atom);
            iter = context.listBindings(name.getSuffix(1));
        }

        return iter;
    }

    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty())
            return this;

        String atom = name.get(0);
        Object binding = bindings.get(atom);
        if (name.size() == 1) {   /* Need to check that binding is null and atom is not a key
                since a null value could have been bound.
            */
            if (binding == null && !bindings.containsKey(atom)) {
                NameNotFoundException e = new NameNotFoundException("Failed to find: " + atom);
                e.setRemainingName(name);
                e.setResolvedObj(this);
                throw e;
            }
        } else if ((binding instanceof Context)) {
            Context context = (Context) binding;
            binding = context.lookup(name.getSuffix(1));
        } else {
            NotContextException e = new NotContextException(atom + " does not name a directory context that supports attributes");
            e.setRemainingName(name);
            e.setResolvedObj(binding);
            throw e;
        }
        return binding;
    }

    public Object lookupLink(Name p1) throws NamingException {
        throw new OperationNotSupportedException("Not implemented yet");
    }

    public void modifyAttributes(Name p1, ModificationItem[] p2) throws NamingException {
        throw new OperationNotSupportedException("Not implemented yet");
    }

    public void modifyAttributes(Name p1, int p2, Attributes p3) throws NamingException {
        throw new OperationNotSupportedException("Not implemented yet");
    }

    public void rebind(Name name, Object value) throws NamingException {
        rebind(name, value, null);
    }

    public void rebind(Name name, Object value, Attributes attributes) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot bind empty name");
        }

        internalBind(name, value, attributes, false);
    }

    public void rename(Name p1, Name p2) throws NamingException {
        throw new OperationNotSupportedException("Not implemented yet");
    }

    public NamingEnumeration<javax.naming.directory.SearchResult> search(Name p1, Attributes p2) throws NamingException {
        throw new OperationNotSupportedException("Not implemented yet");
    }

    public NamingEnumeration<javax.naming.directory.SearchResult> search(Name p1, String p2, SearchControls p3) throws NamingException {
        throw new OperationNotSupportedException("Not implemented yet");
    }

    public NamingEnumeration<javax.naming.directory.SearchResult> search(Name p1, Attributes p2, String[] p3) throws NamingException {
        throw new OperationNotSupportedException("Not implemented yet");
    }

    public NamingEnumeration<javax.naming.directory.SearchResult> search(Name p1, String p2, Object[] p3, SearchControls p4) throws NamingException {
        throw new OperationNotSupportedException("Not implemented yet");
    }

    public void unbind(Name name) throws NamingException {
        if (name.isEmpty()) {
            throw new InvalidNameException("Cannot unbind empty name");
        }

        String atom = name.get(0);
        Object binding = bindings.get(atom);
        if (name.size() == 1) {   /* Need to check that binding is null and atom is not a key
                since a null value could have been bound.
            */
            if (binding == null && !bindings.containsKey(atom)) {
                NameNotFoundException e = new NameNotFoundException("Failed to find: " + atom);
                e.setRemainingName(name);
                e.setResolvedObj(this);
                throw e;
            }
            bindings.remove(atom);
            bindingAttrs.remove(atom);
        } else if ((binding instanceof Context)) {
            Context context = (Context) binding;
            context.unbind(name.getSuffix(1));
        } else {
            NotContextException e = new NotContextException(atom + " does not name a directory context that supports attributes");
            e.setRemainingName(name);
            e.setResolvedObj(binding);
            throw e;
        }
    }
// ---

    private void internalBind(Name name, Object value, Attributes attributes, boolean isBind) throws NamingException {
        String atom = name.get(0);
        Object binding = bindings.get(atom);

        if (name.size() == 1) {
            if (binding != null && !isBind) {
                throw new NameAlreadyBoundException("Use rebind to override");
            }

            // Add object to internal data structure
            bindings.put(atom, value);

            // Add attributes
            if (attributes != null) {
                bindingAttrs.put(atom, attributes);
            }
        } else {
            // Intermediate name: Consume name in this context and continue
            if (!(binding instanceof Context)) {
                NotContextException e = new NotContextException(atom + " does not name a context");
                e.setRemainingName(name);
                e.setResolvedObj(binding);
                throw e;
            }

            if (attributes == null) {
                Context context = (Context) binding;
                if (isBind)
                    context.bind(name.getSuffix(1), value);
                else
                    context.rebind(name.getSuffix(1), value);
            } else if (!(binding instanceof DirContext)) {
                NotContextException e = new NotContextException(atom + " does not name a directory context that supports attributes");
                e.setRemainingName(name);
                e.setResolvedObj(binding);
                throw e;
            } else {
                DirContext context = (DirContext) binding;
                if (isBind)
                    context.bind(name.getSuffix(1), value, attributes);
                else
                    context.rebind(name.getSuffix(1), value, attributes);
            }
        }
    }
}
