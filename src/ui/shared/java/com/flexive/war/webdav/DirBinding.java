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
/*
* JBoss, the OpenSource J2EE webOS
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/

import javax.naming.Binding;
import javax.naming.directory.Attributes;

/**
 * A subclass of Binding that adds support for Attributes. This class is used
 * to pass a contexts raw bindings to NameBindingIterator.
 *
 * @author Scott_Stark@displayscape.com
 * @version $Rev$
 */
public class DirBinding extends Binding {
    private static final long serialVersionUID = -8358283153577718942L;
    private transient Attributes attributes;

    /**
     * Constructs an instance of a Binding given its relative name, object,
     * attributes and whether the name is relative.
     *
     * @param obj        - The possibly null object bound to name.
     * @param attributes - the attributes associated with obj
     */
    public DirBinding(String name, Object obj, Attributes attributes) {
        this(name, null, obj, true, attributes);
    }

    /**
     * Constructs an instance of a Binding given its relative name, class name,
     * object, attributes and whether the name is relative.
     *
     * @param name       - The non-null string name of the object.
     * @param className  - The possibly null class name of the object bound to name.
     *                   If null, the class name of obj is returned by getClassName(). If obj is
     *                   also null, getClassName() will return null.
     * @param obj        - The possibly null object bound to name.
     * @param attributes - the attributes associated with obj
     */
    public DirBinding(String name, String className, Object obj, Attributes attributes) {
        this(name, className, obj, true, attributes);
    }

    /**
     * Constructs an instance of a Binding given its name, object, attributes
     * and whether the name is relative.
     *
     * @param name       - The non-null string name of the object.
     * @param obj        - The possibly null object bound to name.
     * @param isRelative - true if name is a name relative to the target context
     *                   (which is named by the first parameter of the listBindings() method);
     *                   false if name is a URL string.
     * @param attributes - the attributes associated with obj
     */
    public DirBinding(String name, String className, Object obj, boolean isRelative,
                      Attributes attributes) {
        super(name, className, obj, isRelative);
        this.attributes = attributes;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }
}