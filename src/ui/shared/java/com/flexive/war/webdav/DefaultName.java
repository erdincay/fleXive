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

import javax.naming.*;
import java.util.Enumeration;
import java.util.Properties;

/**
 * A simple subclass of CompoundName that fixes the name syntax to:
 * jndi.syntax.direction = left_to_right
 * jndi.syntax.separator = "/"
 *
 * @author Scott_Stark@displayscape.com
 * @version $Rev$
 */
public class DefaultName extends CompoundName {
    /**
     * The Properties used for the project directory heirarchical names
     */
    static Name emptyName;
    static Properties nameSyntax = new Properties();

    static {
        nameSyntax.put("jndi.syntax.direction", "left_to_right");
        nameSyntax.put("jndi.syntax.separator", "/");
        try {
            emptyName = new DefaultName("");
        }
        catch (InvalidNameException e) {
        }
    }

    private static class DefaultNameParser implements NameParser {
        public Name parse(String path) throws NamingException {
            DefaultName name = new DefaultName(path);
            return name;
        }
    }

    public static NameParser getNameParser() {
        return new DefaultNameParser();
    }

    /**
     * Creates new DefaultName
     */
    public DefaultName(Enumeration comps) {
        super(comps, nameSyntax);
    }

    public DefaultName(String name) throws InvalidNameException {
        super(name, nameSyntax);
    }

    public DefaultName(Name name) {
        super(name.getAll(), nameSyntax);
    }

    public DefaultName() {
        this(emptyName);
    }

}
