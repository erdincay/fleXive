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
package com.flexive.war.servlet;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import com.google.common.base.Charsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * XML Export servlet
 * <p/>
 * Format:
 * <p/>
 * /export/type/name
 * /export/content/pk
 * /export/exportGroovyScript/StringObj
 * <p/>
 * TODO:
 * /export/tree/startnode
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ExportServlet implements Servlet {
    private static final Log LOG = LogFactory.getLog(ExportServlet.class);

    public final static String EXPORT_TYPE = "type";
    public final static String EXPORT_TYPE_GROOVY = "exportGroovyScript";
    public final static String EXPORT_CONTENT = "content";

    private final static String BASEURL = "/export/";
    private ServletConfig servletConfig;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletInfo() {
        return "ExportServlet";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
    }

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String[] params = URLDecoder.decode(request.getRequestURI().substring(request.getContextPath().length() + BASEURL.length()), "UTF-8").split("/");

        // do not check the type requests if we are dealing with a content export
        final List<Long> typeRequests = new ArrayList<Long>(params.length);
        if (!EXPORT_CONTENT.equals(params[0])) {
            for (int i = 1; i < params.length; i++) {
                typeRequests.add(Long.parseLong(params[i]));
            }
        }

        if (params.length == 1 && EXPORT_TYPE_GROOVY.equals(params[0])) {
            exportGroovy(request, response);
            return;
        } else if (params.length >= 2 && EXPORT_TYPE.equals(params[0])) {
            exportType(request, response, typeRequests);
            return;
        } else if (params.length == 2 && EXPORT_CONTENT.equals(params[0])) {
            exportContent(request, response, params[1]);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * Export a type
     *
     * @param request  request
     * @param response reponse
     * @param types    type name
     * @throws IOException on errors
     */
    private void exportType(HttpServletRequest request, HttpServletResponse response, List<Long> types) throws IOException {
        final UserTicket ticket = FxContext.getUserTicket();
        if (!ticket.isInRole(Role.StructureManagement)) {
            LOG.warn("Tried to export type(s) [" + types + "] without being in role StructureManagment!");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        final StringBuilder xmlBuild = new StringBuilder(1000).append("<export>");
        for (Long id : types) {
            try {
                xmlBuild.append(EJBLookup.getTypeEngine().export(CacheAdmin.getEnvironment().getType(id).getId()))
                        .append("\n")
                        .append("<!-- NEXT TYPE -->\n");
            } catch (FxApplicationException e) {
                LOG.error(e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
        }
        xmlBuild.append("</export>");
        final String xml = xmlBuild.toString();
        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");
        final String fileName = "xmlExport";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".xml\";");
        try {
            response.getOutputStream().write(xml.getBytes(Charsets.UTF_8));
        } finally {
            response.getOutputStream().close();
        }
    }

    /**
     * Export types as a Groovy script; request session attribute "groovyScriptCode" set from StructureExportBean
     *
     * @param request  request
     * @param response reponse
     * @throws IOException on errors
     */
    private void exportGroovy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String groovyCode = (String)request.getSession().getAttribute("groovyScriptCode");
        final String fileName = "structures";
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".groovy;");
        try {
            response.getOutputStream().write(groovyCode.getBytes(Charsets.UTF_8));
        } finally {
            response.getOutputStream().close();
        }
    }

    /**
     * Export a content (one version)
     *
     * @param request  request
     * @param response reponse
     * @param pk       primary key
     * @throws IOException on errors
     */
    private void exportContent(HttpServletRequest request, HttpServletResponse response, String pk) throws IOException {
        String xml;
        try {
            ContentEngine co = EJBLookup.getContentEngine();
            final FxContent content = co.load(FxPK.fromString(pk));
            xml = co.exportContent(content);
            pk = content.getPk().toString(); //get exact version
        } catch (FxNoAccessException e) {
            LOG.warn("No access to export [" + pk + "]!");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (FxApplicationException e) {
            LOG.warn("Error exporting [" + pk + "]: " + e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }
        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"content_" + pk + ".xml\";");
        try {
            response.getOutputStream().write(xml.getBytes(Charsets.UTF_8));
        } finally {
            response.getOutputStream().close();
        }
    }

}
