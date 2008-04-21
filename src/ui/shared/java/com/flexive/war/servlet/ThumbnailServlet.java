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

import com.flexive.shared.*;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.exceptions.FxStreamException;
import com.flexive.shared.stream.BinaryDownloadCallback;
import com.flexive.shared.stream.FxStreamUtils;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.war.FxThumbnailURIConfigurator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Thumbnail servlet.
 * Usage: <code>/thumbnail/[options][/filename.extension]</code>
 * <br/>
 * Options are optional but must at least contain a way to identify the thumbnail to display.
 * There can be multiple options seperated by a "/".
 * Options may not contain spaces and values have to be properly URL encoded!
 * <p/>
 * Valid options are:
 * <ul>
 * <li><code>pk{n.m}</code>  - id and version of the content, if no version is given the live version is used</li>
 * <li><code>xp{path}</code>  - URL encoded XPath of the property containing the image (optional, else default will be used)</li>
 * <li><code>lang{lang}</code> - 2-digit ISO language code
 * <li><code>lfb{0,1}</code> - language fallback: 0=generate error if language not found, 1=fall back to default language
 * <li><code>e{3 digit error number}u{error url}</code> - URL encoded error url to redirect for http errors specified in {error number}, if no number is given then the url is a fallback for unclassified errors
 * <li><code>s{0,1,2,3}</code>  - use a predefined image/thumbnail size</li>
 * <li><code>w{n}</code> - scale to width</li>
 * <li><code>h{n}</code> - scale to height</li>
 * <li><code>rot{90,180,270}</code> - rotate 90, 180 or 270 degrees (rotate is always executed before flip operations)</li>
 * <li><code>flip{h,v}</code> - flip horizontal or vertical (rotate is always executed before flip operations)</li>
 * <li><code>cropx{x}y{y}w{w}h{h}</code> - crop a box from image defined by x,y,w(idth),h(eight), scaling applies to cropped image!</li>
 * </ul>
 * <p/>
 * Sizes for the <code>s</code> parameters:
 * 0 ... original image
 * 1 ... image scaled to fit a 42x42 box
 * 2 ... image scaled to fit a 85x85 box
 * 3 ... image scaled to fit a 232x232 box
 * Selecting a predefined image or thumbnail disables all image manipultion parameters: the image will be served exactly how it
 * exists in the database/storage.
 * <p/>
 * <br/>
 * Examples:
 * <code>/thumbnail/pk27.1/s2/test.jpg</code>
 * <code>/thumbnail/s0/w100/h300/pk4711.MAX/vermax/rot90/test.jpg</code>
 *
 * TODO:
 * rotate, flip, scale and crop are not implemented yet and have no effect!
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ThumbnailServlet implements Servlet {
    private static final Log LOG = LogFactory.getLog(ThumbnailServlet.class);
    private static final String URI_BASE = "/thumbnail/";

    private ServletConfig servletConfig = null;

    /**
     * Callback to set mimetype and size
     */
    private static class ThumbnailBinaryCallback implements BinaryDownloadCallback {

        /**
         * current response
         */
        private HttpServletResponse response;


        /**
         * Constructor
         *
         * @param response current response
         */
        public ThumbnailBinaryCallback(HttpServletResponse response) {
            this.response = response;
        }

        /**
         * {@inheritDoc}
         */
        public void setMimeType(String mimeType) {
            response.setContentType(mimeType);
        }

        /**
         * {@inheritDoc}
         */
        public void setBinarySize(int size) {
            response.setContentLength(size);
        }
    }

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
        final String uri = request.getRequestURI();
        FxThumbnailURIConfigurator conf = new FxThumbnailURIConfigurator(URLDecoder.decode(uri, "UTF-8"));
        if (conf.getPK() == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Empty request for thumbnail servlet: " + uri);
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long binaryId;
        try {
            //authorization check and binary lookup
            binaryId = EJBLookup.getContentEngine().getBinaryId(conf.getPK(), XPathElement.stripType(conf.getXPath()),
                    conf.getLanguage());
        } catch (FxNoAccessException na) {
            binaryId = BinaryDescriptor.SYS_NOACCESS;
        } catch (FxApplicationException e) {
            binaryId = BinaryDescriptor.SYS_UNKNOWN;
        }
        try {
            response.setDateHeader("Expires", System.currentTimeMillis() + 24L * 3600 * 1000);
            FxStreamUtils.downloadBinary(new ThumbnailBinaryCallback(response),
                    CacheAdmin.getStreamServers(), response.getOutputStream(), binaryId, conf.getSize());
        } catch (FxStreamException e) {
            LOG.error(e);
            response.sendRedirect(request.getContextPath() + "/adm/images/layout/Logo_Flexive.gif");
        }
    }

    /**
     * Return a thumbnail link for the given object.
     *
     * @param pk   the object id
     * @param size the preview size (if null, the default size is used by the servlet)
     * @return a thumbnail link for the given object.
     */
    public static String getLink(FxPK pk, BinaryDescriptor.PreviewSizes size) {
        return getLink(pk, size, null);
    }

    /**
     * Return a thumbnail link for the given object.
     *
     * @param pk    the object id
     * @param size  the preview size (if null, the default size is used by the servlet)
     * @param xpath the binary xpath (if null, the default preview for the object will be used)
     * @return a thumbnail link for the given object.
     */
    public static String getLink(FxPK pk, BinaryDescriptor.PreviewSizes size, String xpath) {
        return getLink(pk, size, xpath, 0);
    }

    /**
     * Return a thumbnail link for the given object.
     *
     * @param pk    the object id
     * @param size  the preview size (if null, the default size is used by the servlet)
     * @param xpath the binary xpath (if null, the default preview for the object will be used)
     * @param timestamp the binary timestamp, to be added to the URL for fine-grained cache control
     * @return a thumbnail link for the given object.
     */
    public static String getLink(FxPK pk, BinaryDescriptor.PreviewSizes size, String xpath, long timestamp) {
        return getLink(pk, size, xpath, timestamp, null);
    }

    /**
     * Return a thumbnail link for the given object.
     *
     * @param pk    the object id
     * @param size  the preview size (if null, the default size is used by the servlet)
     * @param xpath the binary xpath (if null, the default preview for the object will be used)
     * @param timestamp the binary timestamp, to be added to the URL for fine-grained cache control
     * @param language  the language (for multi-lingual objects, otherwise the default language will be used)
     * @return a thumbnail link for the given object.
     */
    public static String getLink(FxPK pk, BinaryDescriptor.PreviewSizes size, String xpath, long timestamp, FxLanguage language) {
        try {
            return URI_BASE + "pk" + pk + (size != null ? "/s" + size.getBlobIndex() : "")
                    + (StringUtils.isNotBlank(xpath) ? "/xp"
                    + URLEncoder.encode(FxSharedUtils.escapeXPath(xpath), "UTF-8") : "")
                    + "/ts" + timestamp
                    + (language != null ? "/lang" + language.getIso2digit() : "");
        } catch (UnsupportedEncodingException e) {
            // shouldn't happen with UTF-8
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getServletInfo() {
        return getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {

    }
}
