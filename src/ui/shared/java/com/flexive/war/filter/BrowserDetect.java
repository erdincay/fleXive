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
package com.flexive.war.filter;

import com.flexive.war.FxRequest;
import static com.flexive.war.FxRequest.Browser;
import com.google.common.collect.Maps;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class BrowserDetect {
    private static final Log LOG = LogFactory.getLog(BrowserDetect.class);

    private static final String WINDOWS = "windows";
    private static final String MAC = "mac";
    private static final String LINUX = "linux";
    private static final String UNIX = "unix";

    private static final Pattern P_VERSION = Pattern.compile("\\d+(\\.\\d+)?");
    private static final Map<FxRequest.Browser, String> BROWSER_IDS;
    private static final Map<FxRequest.Browser, String> BROWSER_VERSION_OVERRIDES;
    static {
        final Map<FxRequest.Browser, String> ids = Maps.newLinkedHashMap();
        ids.put(Browser.OPERA, "opera");    // check opera before MSIE
        ids.put(Browser.IE, "msie");
        ids.put(Browser.FIREFOX, "firefox");
        ids.put(Browser.SAFARI, "safari");
        ids.put(Browser.KONQUEROR, "konqueror");
        ids.put(Browser.CHROME, "chrome");
        ids.put(Browser.GALEON, "galeon");
        ids.put(Browser.EPIPHANY, "epiphany");
        ids.put(Browser.CAMINO, "camino");
        ids.put(Browser.MOZILLA, "mozilla");    // fallback for mozilla-compatible browsers
        ids.put(Browser.UNKNOWN, "");
        BROWSER_IDS = Collections.unmodifiableMap(ids);

        final Map<FxRequest.Browser, String> versionIdOverrides = Maps.newHashMap();
        //versionIdOverrides.put(Browser.SAFARI, "version");
        BROWSER_VERSION_OVERRIDES = Collections.unmodifiableMap(versionIdOverrides);
    }

    private String ua;
    private FxRequest.OperatingSystem os;
    private FxRequest.Browser browser;
    private double browserVersion = -1;
    private static final double DEFAULT_VERSION = 1.0;


    public BrowserDetect(String userAgent) {
        ua = userAgent != null ? userAgent.toLowerCase() : null;
        if (ua == null) {
            os = FxRequest.OperatingSystem.UNKNOWN;
            browser = Browser.UNKNOWN;
            browserVersion = DEFAULT_VERSION;
        }
    }

    public BrowserDetect(HttpServletRequest request) {
        this(request.getHeader("User-Agent"));
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
            browser = Browser.UNKNOWN;
            for (Map.Entry<Browser, String> entry : BROWSER_IDS.entrySet()) {
                if (ua.indexOf(entry.getValue()) != -1) {
                    browser = entry.getKey();
                    break;
                }
            }
        }
        return browser;
    }

    /**
     * Return the browser version, if available.
     *
     * @return  the browser version, if available.
     * @since 3.1
     */
    public double getBrowserVersion() {
        if (browserVersion < 0) {
            final String versionString;
            final Browser browser = getBrowser();
            if (BROWSER_VERSION_OVERRIDES.containsKey(browser)) {
                versionString = BROWSER_VERSION_OVERRIDES.get(browser);
            } else {
                versionString = BROWSER_IDS.get(browser);
            }
            final int pos = ua.indexOf(versionString);
            browserVersion = DEFAULT_VERSION;
            if (pos != -1) {
                final Matcher matcher = P_VERSION.matcher(ua);
                if (matcher.find(pos + versionString.length() + 1)) {
                    try {
                        browserVersion = Double.parseDouble(ua.substring(matcher.start(), matcher.end()));
                    } catch (NumberFormatException e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Failed to extract browser version from user agent: '" + ua + "'");
                        }
                        browserVersion = DEFAULT_VERSION;
                    }
                }
            }
        }
        return browserVersion;
    }
}
