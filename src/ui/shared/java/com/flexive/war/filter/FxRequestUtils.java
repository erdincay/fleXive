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

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.interfaces.GlobalConfigurationEngine;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FxRequestUtils {
    private static final transient Log LOG = LogFactory.getLog(FxRequestUtils.class);

    // Do not use upper case characters, URL needs to be valid
    // or IE will lose the session after every request (ie do not use
    // a URL like 'flexive_site.1'. Do not use '_' in the String!
    final static String DIV_SITE = "www.flexive-develop";
    final static String DIV_GLOBAL_ADMIN = "127.0.0.1";

    static class knownSite {
        String name;
        int division;

        public knownSite(String name, int division) {
            this.name = name;
            this.division = division;
        }
    }


    /**
     * Determines the division of the request.
     *
     * @param request the current request
     * @return the division, or -1 if the request could not be parsed
     */
    public static int getDivision(HttpServletRequest request) {

        // Check ifalready resolved
        if (request instanceof FxRequestWrapper) {
            return ((FxRequestWrapper) request).getDivisionId();
        }

        GlobalConfigurationEngine configuration = EJBLookup.getGlobalConfigurationEngine();

        // Check for global config URL
        String URI = request.getRequestURI().substring(request.getContextPath().length());
        if ((URI.startsWith("/" + FxRequestWrapper.PATH_SETUP + "/") ||
                URI.equals("/" + FxRequestWrapper.PATH_SETUP + "/"))) {
            return FxContext.DIV_GLOBAL_CONFIGURATION;
        }

        // Check for virtual site name that defines the division
        final String server = request.getServerName();
        int divisionId = -1;
        try {
            divisionId = configuration.getDivisionId(server);
        } catch (Exception e) {
            LOG.error("Could not determine division: " + e.getMessage(), e);
        }
        if (divisionId != -1)
            return divisionId;

        // Not registered
        return FxContext.DIV_UNDEFINED;
    }

    /**
     * Get the cookie with the given name. Returns null if no such cookie is defined.
     *
     * @param request the current request
     * @param name    the name of the cookie (case-sensitive)
     * @return the cookie with the given name, or null if no such cookie is defined.
     */
    public static Cookie getCookie(HttpServletRequest request, String name) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null || name == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

}
