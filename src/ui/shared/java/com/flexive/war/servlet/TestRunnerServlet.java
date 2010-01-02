/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.war.servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;

/**
 * Servlet that executes the testrunner (if the test runner plugin is packaged)
 * <p/>
 * Syntax:
 * http://&lt;IP&gt;:&lt;Port&gt;/flexive/testRunner/&lt;command&gt;
 * <p/>
 * Commands:
 * available - SC_OK or SC_SERVICE_UNAVAILABLE
 * run - parameter "outputPath" for location of test results
 * running - returns "true" or "false" if a test is currently running
 * <p/>
 * Examples:
 * http://localhost:8080/flexive/testRunner/available
 * http://localhost:8080/flexive/testRunner/run?outputPath=test
 * http://localhost:8080/flexive/testRunner/running
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class TestRunnerServlet implements Servlet {
    private static final Log LOG = LogFactory.getLog(TestRunnerServlet.class);

    private ServletConfig servletConfig;

    private static final String CMD_AVAILABLE = "available";
    private static final String CMD_RUN = "run";
    private static final String CMD_RUNNING = "running";
    private static final String PARAM_OUTPUTPATH = "outputPath";

    /**
     * {@inheritDoc}
     */
    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
    }

    /**
     * {@inheritDoc}
     */
    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    /**
     * {@inheritDoc}
     */
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        String cmd = URLDecoder.decode(request.getRequestURI(), "UTF8");
        if (cmd != null && cmd.lastIndexOf('/') > 0)
            cmd = cmd.substring(cmd.lastIndexOf('/') + 1);
        if (CMD_AVAILABLE.equals(cmd)) {
            boolean available = false;
            try {
                Class.forName("com.flexive.testRunner.FxTestRunner");
                available = true;
            } catch (Exception e) {
                LOG.error(e);
            }
            response.setStatus(available ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write(String.valueOf(available));
        } else if (CMD_RUN.equals(cmd)) {
            String outputPath = request.getParameter(PARAM_OUTPUTPATH);
            try {
                Class runner = Class.forName("com.flexive.testRunner.FxTestRunner");
                Method check = runner.getMethod("checkTestConditions", String.class, Boolean.class);
                Boolean status = false;
                if (!StringUtils.isEmpty(outputPath))
                    status = (Boolean) check.invoke(null, String.valueOf(outputPath), Boolean.FALSE);
                if (!status) {
                    response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                    response.getWriter().write("Invalid output path, assertations not enabled or test division not definied!");
                    return;
                }
                Method exec = runner.getMethod("runTests", String.class);
                exec.invoke(null, outputPath);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("Tests started.");
            } catch (Exception e) {
                LOG.error(e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error: " + e.getMessage());
            }
        } else if (CMD_RUNNING.equals(cmd)) {
            try {
                Class runner = Class.forName("com.flexive.testRunner.FxTestRunner");
                Method check = runner.getMethod("isTestInProgress");
                Boolean status = (Boolean) check.invoke(null);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(String.valueOf(status));
            } catch (Exception e) {
                LOG.error(e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error: " + e.getMessage());
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            response.getWriter().write("Unknown command: " + cmd);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getServletInfo() {
        return this.getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        //nothing to do
    }
}
