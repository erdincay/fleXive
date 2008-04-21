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
package com.flexive.war.filter;

import com.flexive.war.FxRequest;

import javax.servlet.http.HttpServletRequest;

class BrowserDetect {

    public static final String WINDOWS = "windows";
    public static final String MAC = "mac";
    public static final String LINUX = "linux";
    public static final String UNIX = "unix";

    public static final String IE = "msie";
    public static final String FIREFOX = "firefox";
    public static final String MOZILLA = "mozilla"; // NOT Firefox
    public static final String SAFARI = "safari";
    public static final String OPERA = "opera";
    public static final String KONQUEROR = "konqueror";

    public static final String UNKNOWN = "unknown";

    private String ua;
    private FxRequest.OperatingSystem os;
    private FxRequest.Browser browser;


    protected BrowserDetect(HttpServletRequest request) {
        ua = request.getHeader("User-Agent");
        if (ua != null) {
            ua = ua.toLowerCase();
        } else {
            os = FxRequest.OperatingSystem.UNKNOWN;
            browser = FxRequest.Browser.UNKNOWN;
        }
    }

    /**
     * Returns the client os
     *
     * @return OperatingSystem
     */
    public FxRequest.OperatingSystem getOs() {
        if (os == null) {
            if (ua.indexOf(MAC) > -1) os = FxRequest.OperatingSystem.MAC;
            else if (ua.indexOf(WINDOWS) > -1) os = FxRequest.OperatingSystem.WINDOWS;
            else if (ua.indexOf(LINUX) > -1) os = FxRequest.OperatingSystem.LINUX;
            else if (ua.indexOf(UNIX) > -1) os = FxRequest.OperatingSystem.UNIX;
            else os = FxRequest.OperatingSystem.UNKNOWN;
        }
        return os;
    }

    /**
     * Returns the browser.
     *
     * @return Browser
     */
    public FxRequest.Browser getBrowser() {
        if (browser == null) {
            /*
             * Opera's user agent string contains "msie," so check for it before checking for
             * IE. Same for checking for Firefox before Mozilla.
             */
            if (ua.indexOf(SAFARI) > -1) browser = FxRequest.Browser.SAFARI;
            else if (ua.indexOf(OPERA) > -1) browser = FxRequest.Browser.OPERA;
            else if (ua.indexOf(KONQUEROR) > -1) browser = FxRequest.Browser.KONQUEROR;
            else if (ua.indexOf(IE) > -1) browser = FxRequest.Browser.IE;
            else if (ua.indexOf(FIREFOX) > -1) browser = FxRequest.Browser.FIREFOX;
            else if (ua.indexOf(MOZILLA) > -1) browser = FxRequest.Browser.MOZILLA;
            else browser = FxRequest.Browser.UNKNOWN;
        }

        return browser;
    }


}
