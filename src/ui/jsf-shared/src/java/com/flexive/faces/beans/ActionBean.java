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
package com.flexive.faces.beans;

import com.flexive.shared.exceptions.FxApplicationException;

/**
 * <p>
 * Beans that implement this interface allow to invoke (some of) their functionality
 * by setting an "action" parameter in the HTTP request. Useful for invoking beans actions
 * via javascript without a JSF command link. Note that it is not possible to control the
 * resulting page forward with this method, the JSF page to be rendered has already
 * been determined by the request URL.
 * </p>
 * <p>
 * This is a workaround due to the frame-based architecture of the administration GUI.
 * Usually you'd want to use more advanced concepts like managed properties, Seam
 * page actions, or the JSF 2 event system.
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public interface ActionBean {
    /**
     * Parse the parameters and invoke any action that was requested.
     * This method has to be accessed at the top of the JSF page that requested from
     * the browser, e.g. via
     * <code>&lt;c:if test="#{empty myActionBean.parseRequestParameters}"&gt; &lt;/c:if&gt;</code>
     *
     * @return (irrelevant)
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          if an application error occured. Note that
     *          throwing an exception here will display a top-level Facelets error page.
     */
    String getParseRequestParameters() throws FxApplicationException;
}
