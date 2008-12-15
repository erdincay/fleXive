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
package com.flexive.faces.plugin;

/**
 * <p>
 * An extension point where plugins can be registered. An extension point specifies
 * a specific subinterface of {@link PluginExecutor}. Plugins registered with this extension
 * point then use the PluginExecutor's methods to modify the application's behaviour.
 * </p>
 * <p>
 * This class must be subclassed to bind its type parameters but is usually used anonymously
 * (similar to a ThreadLocal variable), e.g.
 * </p>
 * <pre>
 * public static final ExtensionPoint&lt;MyPluginExecutorInterface> EXTENSION =
 * <b>new ExtensionPoint&lt;MyPluginExecutorInterface>("myExtensionName") { }</b>;
 * </pre>
 * <p><i>Note: it should be possible to use ExtensionPoint directly through a generic
 * instantiation (i.e. {@code new ExtensionPoint<MyExecutor>()}), however this requires a
 * more powerful type resolver than the plugin registry's simple implementation. JDK7
 * will have this.</i></p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class ExtensionPoint<PEX extends PluginExecutor> {
    private final String name;

    /**
     * Create a new extension point. The name is the unique identifier of the
     * extension point.
     *
     * @param name the extension point name. Used to identify an extension point, must
     *             be unique for the application.
     */
    public ExtensionPoint(String name) {
        this.name = name;
    }

    /**
     * Returns the unique name of this extension point.
     *
     * @return the unique name of this extension point.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ExtensionPoint)) return false;
        return name.equals(((ExtensionPoint) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
