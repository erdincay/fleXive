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
package com.flexive.faces.plugin;

/**
 * A collection of all extension points of the flexive admin interface, to be used by
 * all applications and plugins.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public final class AdmExtensionPoints {
    /**
     * Tree extension point for the main navigation tree. Use this extension to add
     * new menu items or subfolders.
     */
    public static final ExtensionPoint<TreePluginExecutor> ADM_MAIN_NAVIGATION =
            new ExtensionPoint<TreePluginExecutor>("/tree/adm/main") {
            };

    /**
     * Extension point for all toolbars of the content interface. The matching buttons
     * will be evaluated at run-time by the executor implementation, so there is only one
     * extension point for all pages.
     */
    public static final ExtensionPoint<ToolbarPluginExecutor> ADM_TOOLBAR_PLUGINS =
            new ExtensionPoint<ToolbarPluginExecutor>("/toolbar/adm") {
            };

    private AdmExtensionPoints() {
    }
}
