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

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Response Wrapper to provide access to the content length and status.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxResponseWrapper extends HttpServletResponseWrapper {

    private static final String HTTP_MODIFIED_AT_FORMATER_TXT = "EEE, dd MMM yyyy HH:mm:ss z";
    private static final String HTTP_HEADER_EXPIRES = "Expires";
    private static final String HTTP_HEADER_LAST_MODIFIED = "Last-Modified";
    private static final String HTTP_HEADER_X_POWERED_BY = "X-Powered-By";
    private static final String HTTP_HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HTTP_HEADER_PRAGMA = "Pragma";


    FxOutputStream fos = null;
    HttpServletResponse resp = null;
    PrintWriter pw = null;
    FxWriter fxWriter = null;
    int length = -1;
    int status = HttpServletResponse.SC_OK;
    String status_msg = null;
    public int level = 0;
    boolean catchContent = false;
    private long createdAt = System.currentTimeMillis();
    private static long PAGE_ID_GEN = 0;
    private long pageId = getPageId();
    private String contentType = null;

    private static synchronized long getPageId() {
        if (PAGE_ID_GEN == Long.MIN_VALUE) {
            PAGE_ID_GEN = 0;
        }
        return PAGE_ID_GEN++;

    }

    public static enum CacheControl {
        NO_CACHE, PUBLIC, PRIVATE
    }

    private static SimpleDateFormat getHttpDateFormat() {
        return new SimpleDateFormat(HTTP_MODIFIED_AT_FORMATER_TXT);
    }

    /**
     * Returns the modified-at date for a http response with
     * the given time.
     * <p/>
     * This function uses the HTTP standard time format ("Sat, 07 Apr 2001 00:58:08 GMT")
     *
     * @param date the date to use
     * @return the modified-at date with the current time
     */
    protected static String buildModifiedAtDate(Date date) {
        // HTTP standard time format: "Sat, 07 Apr 2001 00:58:08 GMT"
        // Apache server SSI format: Saturday, 08-Sep-2001 21:46:40 EDT
        return getHttpDateFormat().format(date);
    }

    /**
     * Returns the setting of the ClientWriteThrough option.
     *
     * @return the setting of the ClientWriteThrough option.
     */
    public boolean isClientWriteThrough() {
        return !catchContent;
    }

    /**
     * Sets the modified at date in the response to the given date.
     *
     * @param date the date to use
     */
    public void setModifiedAtDate(Date date) {
        setHeader(HTTP_HEADER_LAST_MODIFIED, buildModifiedAtDate(date));
    }

    /**
     * Sets the x powered by header.
     *
     * @param value the value
     */
    public void setXPoweredBy(String value) {
        setHeader(HTTP_HEADER_X_POWERED_BY, value);
    }

    /**
     * Force the browser to disable its cache.
     * <p/>
     * Ths functions sets the expires, cache control and pragma="no cache" headers
     */
    public void disableBrowserCache() {
        setModifiedAtDate(new Date());
        setHeader(HTTP_HEADER_EXPIRES, getHttpDateFormat().format(new Date(0)));
        setHeader(HTTP_HEADER_CACHE_CONTROL, "no-cache, must-revalidate");
        setHeader(HTTP_HEADER_PRAGMA, "no-cache");
    }

    /**
     * Enables the browser cache.
     *
     * @param ct             the type, CacheControl.PUBLIC or CacheControl.PRIVATE
     * @param maxAge         in seconds, use null if you dont want this to be set - specifies the maximum amount of
     *                       time that an object will be considered fresh.
     *                       Similar to Expires, this directive allows more flexibility.
     *                       [seconds] is the number of seconds from the time of the request you wish the object to be fresh for.
     * @param mustRevalidate tells caches that they must obey any freshness information you give them about an object.
     *                       The HTTP allows caches to take liberties with the freshness of objects; by specifying this header, you're
     *                       telling the cache that you want it to strictly follow your rules.
     */
    public void enableBrowserCache(CacheControl ct, Integer maxAge, boolean mustRevalidate) {
        String sCacheControl = ct == CacheControl.PUBLIC ? "public" : "private";
        if (maxAge != null) sCacheControl += ", max-age==" + maxAge;
        if (mustRevalidate) sCacheControl += ", must-revalidate";
        setHeader(HTTP_HEADER_CACHE_CONTROL, sCacheControl);
        setHeader(HTTP_HEADER_PRAGMA, "cache");
        setDateHeader(HTTP_HEADER_EXPIRES, System.currentTimeMillis() + 24 * 3600 * 1000);
        setModifiedAtDate(new Date());
    }


    @Override
    public void setDateHeader(String s, long l) {
        resp.setDateHeader(s, l);
    }

    @Override
    public void setHeader(String s, String s1) {
        resp.setHeader(s, s1);
    }

    /**
     * Returns the wrapped response.
     *
     * @return the wrapped response
     */
    public HttpServletResponse getWrappedResponse() {
        return resp;
    }

    /**
     * Returns true if generating the response has at least one error.
     *
     * @return true if generating the response has at least one error
     */
    public boolean hadError() {
        return (this.getStatus() != HttpServletResponse.SC_OK);
    }

    /**
     * The status message delivered to the client, may be null.
     *
     * @return the status message delivered to the client
     */
    public String getStatusMsg() {
        return this.status_msg == null ? "" : this.status_msg;
    }

    /**
     * The http response status delivered to the client.
     *
     * @return The http response status delivered to the client
     */
    public int getStatus() {
        return this.status;
    }

    @Override
    public void reset() {
        super.reset();
        resetBuffer();
    }

    @Override
    public void setContentType(String s) {
        if (s.equals("text/plain") && contentType != null) {
            return;
        }
        super.setContentType(s);
        this.resp.setContentType(s);
        this.contentType = s;
    }

    @Override
    public void resetBuffer() {
        super.resetBuffer();
        pw = null;  // reset our buffered response
        fos = null;
    }

    @Override
    public int getBufferSize() {
        return super.getBufferSize();
    }

    @Override
    public void setBufferSize(int i) {
        super.setBufferSize(i);
    }

    @Override
    public void sendRedirect(String s) throws IOException {
        super.sendRedirect(s);
    }


    /**
     * Constructor.
     *
     * @param resp      the original response object
     * @param catchData Has to be set to true if getData() is called later on.
     */
    public FxResponseWrapper(ServletResponse resp, boolean catchData) {
        super((HttpServletResponse) resp);
        this.resp = (HttpServletResponse) resp;
        this.catchContent = catchData;
    }


    /**
     * Returns a PrintWriter object that can send character text to the client.
     *
     * @return the writer
     * @throws IOException
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (pw == null) {
            // Content type HAS to be set before the print writer is used
            forceContentType(contentType);
            // obtain and store the printwriter
            fxWriter = new FxWriter(catchContent ? null : resp.getWriter(), catchContent);
            pw = new PrintWriter(fxWriter);
        }
        return pw;
    }

    /**
     * Forces the contentype directly at the wrapped catalina response, since the
     * wrapper is not always allowing the type to change.
     *
     * @param type the content type
     */
    private void forceContentType(String type) {
        if (type == null || type.length() == 0) return;
        this.resp.setContentType(type);
    }

    /**
     * Returns a ServletOutputStream suitable for writing binary data in the response.
     *
     * @return the output stream
     * @throws IOException
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (fos == null) {
            fos = new FxOutputStream(catchContent ? null : resp.getOutputStream(), catchContent, !catchContent);
        }
        return fos;
    }

    /**
     * Manual setter for the content length.
     *
     * @param i the length
     */
    @Override
    public void setContentLength(int i) {
        super.setContentLength(i);
        this.resp.setContentLength(i);
        length = i;
    }


    /**
     * Get the content length delivered to the client (without headers).
     *
     * @return the content length delivered to the client
     */
    public long getContentLength() {
        return _getContentLength() + ((status_msg == null) ? 0 : status_msg.length());
    }

    /**
     * Get the content length delivered to the client (without headers and error messages).
     *
     * @return the content length delivered to the client (without headers and error messages).
     */
    private long _getContentLength() {
        // overrides
        if (length > -1) {
            return length;
        }
        //
        if (fos == null && fxWriter == null) return 0;
        if (fos != null) return fos.getContentLength();
        return fxWriter.getContentLength();
    }

    /**
     * Returns the data sent to the client.
     * <p/>
     * The option catchContent has to be set to true in the constructor, or a empty byte array is returned.
     *
     * @return the data sent to the client.
     */
    public byte[] getData() {
        if (!catchContent || (fos == null && fxWriter == null)) return new byte[0];

        if (fos != null) return fos.getData();
        return fxWriter.getData();
    }

    /**
     * Writes the cached data to the underlying response, or the given string if the
     * override parameter is set.
     * <p/>
     * This function may only be called if ClientWriteThrough was not set to true
     * at creation time.
     *
     * @param override overrides the data if set
     * @throws IOException            if an io exception occured
     * @throws FxApplicationException if an exception occured
     */
    public void writeToUnderlyingResponse(String override) throws IOException, FxApplicationException {
        if (!catchContent) {
            throw new FxApplicationException("Cannot write to underlying response, ClientWriteThrough option is set");
        }
        byte _data[] = override != null ? FxSharedUtils.getBytes(override) : this.getData();
        final OutputStream out = this.resp.getOutputStream();
        this.resp.setStatus(this.getStatus());
        this.resp.setContentLength(_data.length);
        if (contentType != null) this.resp.setContentType(contentType);
        this.resp.setContentType(contentType);
        out.write(_data);
    }


    /**
     * Return the wrapped ServletResponse object.
     *
     * @return the wrapped response object
     */
    @Override
    public ServletResponse getResponse() {
        return this.resp;
    }

    /**
     * Sets the wrapped ServletResponse object.
     *
     * @param servletResponse the wrapped response
     */
    @Override
    public void setResponse(ServletResponse servletResponse) {
        this.resp = (HttpServletResponse) servletResponse;
    }


    /**
     * Sets the status code for this response
     *
     * @param i the status
     */
    @Override
    public void setStatus(int i) {
        if (!catchContent) resp.setStatus(i);
    }

    /**
     * Sets the status code for this response
     *
     * @param i the status
     * @param s the status message
     * @deprecated
     */
    @Override
    public void setStatus(int i, String s) {
        if (!catchContent) //noinspection deprecation
            resp.setStatus(i, s);
        this.status = i;
        this.status_msg = s;
    }

    /**
     * Sends a error to the client.
     *
     * @param i the error code
     * @param s the error message
     * @throws IOException if a io exception occured
     */
    @Override
    public void sendError(int i, String s) throws IOException {
        if (!catchContent) resp.sendError(i, s);
        this.status = i;
        this.status_msg = s;
    }

    /**
     * Sends a error to the client.
     *
     * @param i the error code
     * @param t the error
     * @throws IOException if a io exception occured
     */
    public void sendError(int i, Throwable t) throws IOException {
        String message = t != null ? t.getMessage() : null;
        if (message == null) {
            message = "Exception occured: " + (t != null ? t.getClass().getName() : "null");
        }
        message += "\n\n" + getStackTrace(t);
        if (!catchContent) resp.sendError(i, message);
        this.status = i;
        this.status_msg = message;
    }

    /**
     * Gets the stacktrace as a string from a throwable.
     *
     * @param th the throwable
     * @return the stacktrace as string
     */
    private String getStackTrace(Throwable th) {
        if (th == null) {
            return "";
        }
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            return sw.toString();
        } catch (Throwable t) {
            return "n/a";
        } finally {
            try {
                if (sw != null) sw.close();
            } catch (Throwable t) {/*ignore*/}
            try {
                if (pw != null) pw.close();
            } catch (Throwable t) {/*ignore*/}
        }
    }

    /**
     * Sends a error to the client.
     *
     * @param i the error code
     * @throws IOException if a exception occurs
     */
    @Override
    public void sendError(int i) throws IOException {
        if (!catchContent) resp.sendError(i);
        this.status = i;
    }

    /**
     * Flushes the buffers.
     *
     * @throws IOException
     */
    @Override
    public void flushBuffer() throws IOException {
        super.flushBuffer();
        if (fos != null) fos.flush();
        if (pw != null) pw.flush();
    }

    /**
     * Returns true if the reponse was commited.
     *
     * @return true if the reponse was commited
     */
    @Override
    public boolean isCommitted() {
        if (fos != null) return fos.isCommited();
        if (fxWriter != null) return fxWriter.isCommited();
        return resp.isCommitted();
    }

    /**
     * Adds the specified cookie to the response. This method can be called multiple times to set more than one cookie.
     *
     * @param cookie the cookie to return to the client
     */
    @Override
    public void addCookie(Cookie cookie) {
        resp.addCookie(cookie);
    }

    /**
     * Returns a boolean indicating whether the named response header has already been set.
     *
     * @param s the header name
     * @return a boolean indicating whether the named response header has already been set
     */
    @Override
    public boolean containsHeader(String s) {
        return resp.containsHeader(s);
    }

    /**
     * Returns the time that this response object was created at.
     *
     * @return the time that this response object was created at
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the unique responseWrapper id.
     * <p/>
     * IDs are reused when MAX_LONGVALUE is reached.
     *
     * @return the unique responseWrapper id
     */
    public long getId() {
        return pageId;
    }
}

