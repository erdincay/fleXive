/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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

import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.EJBLookup;
import com.flexive.war.beans.admin.content.ContentEditorBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;


/**
 * Donwload servlet used by the ContentEditorBean.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class CeFileDownload implements Servlet {

    private ServletConfig servletConfig = null;

    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
    }

    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public String getServletInfo() {
        return this.getClass().getName();
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Downloads a file specified by the currently loaded content beans in the content editor and a xpath
     * that is provided in the url.
     * The "If-Modified-Since" tag is not handled since we do not want any caching when downloading the files.
     *
     * @param servletRequest  the servlet request
     * @param servletResponse the servlet response
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if a io error occurs
     */
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        final ContentEditorBean ceb = ContentEditorBean.getSingleton().getInstance(request);
        if (ceb == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No active contentEditor bean found in the session");
            return;
        }

        String downloadFileName = null;
        String xpath = null;
        String lang = null;
        try {
            // Example URI: '/flexive/cefiledownload/xpath:|SUBGROUP[1]|FILE[1]/demo.jpg'
            String uri[] = URLDecoder.decode(request.getRequestURI(), "UTF8").split("/");
            downloadFileName = uri[uri.length - 1].contains(":") ? null : uri[uri.length - 1];
            for (String param : uri) {
                try {
                    String paramSplit[] = param.split(":");
                    if (paramSplit[0].equalsIgnoreCase("xpath")) {
                        xpath = paramSplit[1].replace('|', '/');
                    } else if (paramSplit[0].equalsIgnoreCase("lang")) {
                        lang = paramSplit[1];
                    }
                } catch (Throwable t) {
                    System.err.println(this.getClass() + ": ignoring url parameter " + param);
                }
            }
        } catch (Throwable t) {
            System.err.println(this.getClass() + " ERROR: " + t.getMessage());
        }

        // Send the data
        ServletOutputStream sos = null;
        try {
            BinaryDescriptor bd;
            if( lang == null )
                bd = ((BinaryDescriptor) ceb.getContent().getValue(xpath).getBestTranslation());
            else
                bd = ((BinaryDescriptor) ceb.getContent().getValue(xpath).getTranslation(EJBLookup.getLanguageEngine().load(lang)));
            response.setContentType(bd.getMimeType());
            response.setContentLength((int) bd.getSize());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + bd.getName() + "\";");
            sos = response.getOutputStream();
            bd.download(sos);
        } catch (Throwable t) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to send data for file '" +
                    downloadFileName + "': " + t.getMessage());
        } finally {
            if (sos != null) sos.close();
        }

    }
}
