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
package com.flexive.faces;

/**
 * JSF2-related constant definitions.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxJsf2Const {

    /**
     * Name of the resource library for serving component-related resources (e.g. flexiveComponents.js).
     */
    public static final String RESOURCE_LIBRARY = "flexive-faces";

    /**
     * Base JS for flexive components.
     */
    public static final String RESOURCE_JS_COMPONENTS = "js/flexiveComponents.js";

    /**
     * Initialize the flexive javascript components (target: body).
     */
    public static final String RESOURCE_JS_INIT = "js/init.js";

    /**
     * HTML editor javascript resource
     */
    public static final String RESOURCE_JS_HTMLEDITOR = "js/tiny_mce/tiny_mce.js";

    /**
     * YUI (Yahoo User Interface library) 2.x javascript resource
     */
    public static final String RESOURCE_JS_YUILOADER = "js/yui/yuiloader/yuiloader-min.js";

    /**
     * Base CSS file for flexive components.
     */
    public static final String RESOURCE_CSS_COMPONENTS = "css/components.css";

    private FxJsf2Const() {
    }
}
