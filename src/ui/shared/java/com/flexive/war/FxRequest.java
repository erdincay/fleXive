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
package com.flexive.war;

import com.flexive.shared.security.UserTicket;

import javax.servlet.http.HttpServletRequest;

public interface FxRequest extends HttpServletRequest {

    // All recognized browsers
    enum Browser {
        KONQUEROR, IE, FIREFOX, SHIRETOKO, MOZILLA, SAFARI, OPERA, CHROME, GALEON, EPIPHANY, CAMINO, UNKNOWN
    }

    // All recognized operating systems
    enum OperatingSystem {
        WINDOWS, MAC, LINUX, UNIX, UNKNOWN
    }

    /**
     * The underlying request.
     *
     * @return the underlying request
     */
    HttpServletRequest getRequest();

    /**
     * The time the request was started at.
     *
     * @return the time the request was started at.
     */
    long getRequestStartTime();

    /**
     * The ticke of the user.
     * <p/>
     *
     * @return The ticke of the user
     */
    UserTicket getUserTicket();

    /**
     * Returns the division the request was started in.
     *
     * @return the division the request was started in
     */
    int getDivisionId();


    /**
     * Returns the operating system that generated the request.
     *
     * @return the operating system that generated the request
     */
    OperatingSystem getOperatingSystem();

    /**
     * Returns the browser that generated the request.
     *
     * @return the browser that generated the request
     */
    Browser getBrowser();

    /**
     * Returns the browser version that generated the request.
     *
     * @return the browser version that generated the request
     * @since 3.1
     */
    double getBrowserVersion();

    /**
     * Returns the request URI without the context.
     *
     * @return the request URI without the context.
     */
    String getRequestURIWithoutContext();

    /**
     * Returns true if the request is a dynamic content.
     *
     * @return true if the request is a dynamic content
     */
    boolean isDynamicContent();
}