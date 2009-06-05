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
package com.flexive.war.servlet;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.value.BinaryDescriptor;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * <p>Provides streaming downloads for all binary objects ({@link com.flexive.shared.value.FxBinary FxBinary}).
 * The requested value is identified by its XPath.</p>
 * <p>Link format:</p>
 * <pre>/download/pk{n.m}/xpath/filename.ext</pre>
 * <p>
 * When no XPath is provided, the first mandatory binary property of the instance's type is chosen.
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class DownloadServlet implements Servlet {
    private final static String BASEURL = "/download/";
    private ServletConfig servletConfig;

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

    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String uri = FxServletUtils.stripSessionId(
                URLDecoder.decode(request.getRequestURI().substring(request.getContextPath().length() + BASEURL.length()), "UTF-8")
        );
        // get PK
        if (!uri.startsWith("pk")) {
            FxServletUtils.sendErrorMessage(response, "Invalid download getResult: " + uri);
            return;
        }
        final FxPK pk;
        try {
            pk = FxPK.fromString(uri.substring(2, uri.indexOf('/')));
        } catch (IllegalArgumentException e) {
            FxServletUtils.sendErrorMessage(response, "Invalid primary key in getResult: " + uri);
            return;
        }
        // load content
        final FxContent content;
        try {
            content = EJBLookup.getContentEngine().load(pk);
        } catch (FxApplicationException e) {
            FxServletUtils.sendErrorMessage(response, "Failed to load content: " + e.getMessage());
            return;
        }
        // extract xpath
        String xpath = null;
        try {
            xpath = FxSharedUtils.decodeXPath(uri.substring(uri.indexOf('/') + 1, uri.lastIndexOf('/')));
        } catch (IndexOutOfBoundsException e) {
            // no XPath provided, check if a mandatory binary property exists
            final FxType type = CacheAdmin.getEnvironment().getType(content.getTypeId());
            for (FxPropertyAssignment assignment : type.getAllProperties()) {
                if (assignment.getProperty().getDataType() == FxDataType.Binary && assignment.getMultiplicity().getMin() >= 1) {
                    xpath = assignment.getXPath();
                    break;
                }
            }
            if (xpath == null) {
                FxServletUtils.sendErrorMessage(response, "Invalid xpath/filename in getResult: " + uri);
                return;
            }
        }
        // get binary descriptor
        final BinaryDescriptor descriptor;
        try {
            descriptor = (BinaryDescriptor) content.getValue(xpath).getBestTranslation();
        } catch (Exception e) {
            FxServletUtils.sendErrorMessage(response, "Failed to load binary value: " + e.getMessage());
            return;
        }
        // stream content
        try {
            response.setContentType(descriptor.getMimeType());
            response.setContentLength((int) descriptor.getSize());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + descriptor.getName() + "\";");
            descriptor.download(response.getOutputStream());
        } catch (Exception e) {
            FxServletUtils.sendErrorMessage(response, "Download failed: " + e.getMessage());
            //noinspection UnnecessaryReturnStatement
            return;
        } finally {
            response.getOutputStream().close();
        }
    }

    /**
     * Returns a link (absolute to the server context) to download the binary stored under
     * the given XPath for the given object. <code>fileName</code> is the filename visible to
     * the browser, the actual name of the downloaded file is determined by the stored filename.
     *
     * @param pk       the object pk
     * @param xpath    the XPath of the binary property to be downloaded
     * @param fileName the filename visible to the browser
     * @return a link (absolute to the server context) to download the given binary
     */
    public static String getLink(FxPK pk, String xpath, String fileName) {
        try {
            return BASEURL + "pk" + pk + "/" + URLEncoder.encode(FxSharedUtils.escapeXPath(xpath), "UTF-8") + "/" + fileName;
        } catch (UnsupportedEncodingException e) {
            // shouldn't happen with UTF-8
            throw new IllegalArgumentException(e);
        }
    }
}
