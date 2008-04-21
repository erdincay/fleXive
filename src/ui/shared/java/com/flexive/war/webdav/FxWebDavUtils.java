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
package com.flexive.war.webdav;

import com.flexive.shared.FxSharedUtils;
import com.flexive.war.filter.FxRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

/**
 * Static WebDav helper functions
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxWebDavUtils {
    private static final String WEBDAV_METHOD_PROPFIND = "PROPFIND";
    private static final String WEBDAV_METHOD_PROPPATCH = "PROPPATCH";
    private static final String WEBDAV_METHOD_MKCOL = "MKCOL";
    private static final String WEBDAV_METHOD_COPY = "COPY";
    private static final String WEBDAV_METHOD_MOVE = "MOVE";
    private static final String WEBDAV_METHOD_LOCK = "LOCK";
    private static final String WEBDAV_METHOD_UNLOCK = "UNLOCK";
    private static final String WEBDAV_METHOD_OPTIONS = "OPTIONS";
    private static final String HTTP_HEADER_LAST_MODIFIED = "Last-Modified";
    private static final String HTTP_MODIFIED_AT_FORMATER_TXT = "EEE, dd MMM yyyy HH:mm:ss z";
    private static SimpleDateFormat HTTP_MODIFIED_AT_FORMATER = new SimpleDateFormat(HTTP_MODIFIED_AT_FORMATER_TXT);

    /**
     * Determine if the given request is supposed to be a WebDav request by inspecting the context path (=/webdav)
     *
     * @param request the request to examine
     * @return if this request is supposed to be a WebDav request
     */
    public static boolean isWebDavRequest(final HttpServletRequest request) {
        // Already resolved?
        if (request instanceof FxRequestWrapper) {
            ((FxRequestWrapper) request).isWebdavRequest();
        }
        // Resolve it
        if (request.getServletPath() == null)
            return false;
        String requestUriNoContext = request.getRequestURI().substring(request.getContextPath().length());
        return (requestUriNoContext.length() == 7 && requestUriNoContext.equalsIgnoreCase("/webdav")) ||
                (requestUriNoContext.length() >= 8 && requestUriNoContext.substring(0, 8).equalsIgnoreCase("/webdav/"));
    }

    /**
     * Check if this request is a WebDav method dealing with properties
     *
     * @param request the request to check
     * @return request is a WebDav method dealing with properties
     */
    public static boolean isWebDavPropertyMethod(HttpServletRequest request) {
        return request.getMethod().equals(WEBDAV_METHOD_PROPFIND) ||
                request.getMethod().equals(WEBDAV_METHOD_PROPPATCH) ||
                (request.getHeader("USER-AGENT") != null && request.getHeader("USER-AGENT").indexOf("neon/0.") > 0);
    }

    /**
     * Check if the given method is WebDav related
     *
     * @param method the method (request.getMethod())
     * @return method is WebDav related
     */
    public static boolean isWebDavMethod(String method) {
        return method.equals(FxWebDavUtils.WEBDAV_METHOD_PROPFIND) ||
                method.equals(FxWebDavUtils.WEBDAV_METHOD_PROPPATCH) ||
                method.equals(FxWebDavUtils.WEBDAV_METHOD_MKCOL) ||
                method.equals(FxWebDavUtils.WEBDAV_METHOD_COPY) ||
                method.equals(FxWebDavUtils.WEBDAV_METHOD_MOVE) ||
                method.equals(FxWebDavUtils.WEBDAV_METHOD_LOCK) ||
                method.equals(FxWebDavUtils.WEBDAV_METHOD_OPTIONS) ||
                method.equals(FxWebDavUtils.WEBDAV_METHOD_UNLOCK);
    }

    /**
     * Decode and return the specified URL-encoded String.
     * When the byte array is converted to a string, the system default
     * character encoding is used...  This may be different than some other
     * servers.
     *
     * @param str The url-encoded string
     * @throws IllegalArgumentException if a '%' character is not followed
     *                                  by a valid 2-digit hexadecimal number
     */
    public static String URLDecode(String str) {

        return URLDecode(str, null);

    }


    /**
     * Decode and return the specified URL-encoded String.
     *
     * @param str The url-encoded string
     * @param enc The encoding to use; if null, the default encoding is used
     * @throws IllegalArgumentException if a '%' character is not followed
     *                                  by a valid 2-digit hexadecimal number
     */
    public static String URLDecode(String str, String enc) {

        if (str == null)
            return (null);

        // use the specified encoding to extract bytes out of the
        // given string so that the encoding is not lost. If an
        // encoding is not specified, let it use platform default
        byte[] bytes = null;
        if (enc == null) {
            bytes = FxSharedUtils.getBytes(str);
        } else {
            bytes = FxSharedUtils.getBytes(str);
        }

        return URLDecode(bytes, enc);

    }


    /**
     * Decode and return the specified URL-encoded byte array.
     *
     * @param bytes The url-encoded byte array
     * @throws IllegalArgumentException if a '%' character is not followed
     *                                  by a valid 2-digit hexadecimal number
     */
    public static String URLDecode(byte[] bytes) {
        return URLDecode(bytes, null);
    }


    /**
     * Decode and return the specified URL-encoded byte array.
     *
     * @param bytes The url-encoded byte array
     * @param enc   The encoding to use; if null, the default encoding is used
     * @throws IllegalArgumentException if a '%' character is not followed
     *                                  by a valid 2-digit hexadecimal number
     */
    public static String URLDecode(byte[] bytes, String enc) {

        if (bytes == null)
            return (null);

        int len = bytes.length;
        int ix = 0;
        int ox = 0;
        while (ix < len) {
            byte b = bytes[ix++];     // Get byte to test
            if (b == '+') {
                b = (byte) ' ';
            } else if (b == '%') {
                b = (byte) ((convertHexDigit(bytes[ix++]) << 4)
                        + convertHexDigit(bytes[ix++]));
            }
            bytes[ox++] = b;
        }
        if (enc != null) {
            try {
                return new String(bytes, 0, ox, enc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new String(bytes, 0, ox);

    }


    /**
     * Convert a byte character value to hexidecimal digit value.
     *
     * @param b the character value byte
     */
    private static byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9')) return (byte) (b - '0');
        if ((b >= 'a') && (b <= 'f')) return (byte) (b - 'a' + 10);
        if ((b >= 'A') && (b <= 'F')) return (byte) (b - 'A' + 10);
        return 0;
    }

    private static final char[] hexadecimal =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'a', 'b', 'c', 'd', 'e', 'f'};

    // --------------------------------------------------------- Public Methods


    /**
     * Encodes the 128 bit (16 bytes) MD5 into a 32 character String.
     *
     * @param binaryData Array containing the digest
     * @return Encoded MD5, or null if encoding failed
     */
    public static String MD5encode(byte[] binaryData) {

        if (binaryData.length != 16)
            return null;

        char[] buffer = new char[32];

        for (int i = 0; i < 16; i++) {
            int low = binaryData[i] & 0x0f;
            int high = (binaryData[i] & 0xf0) >> 4;
            buffer[i * 2] = hexadecimal[high];
            buffer[i * 2 + 1] = hexadecimal[low];
        }

        return new String(buffer);

    }

    /**
     * Get the WebDav path that is appended to the servlet
     *
     * @param request
     * @return Dav path
     */
    public static String getDavPath(HttpServletRequest request) {
        String result = request.getRequestURI();
        int ctxPathLen = request.getContextPath() == null ? 0 : request.getContextPath().length();
        if (ctxPathLen > 0) {
            result = result.substring(ctxPathLen);
        }
        result = result.substring(request.getServletPath().length());
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return FxWebDavUtils.URLDecode(result, "UTF8");
    }

    /**
     * Return a context-relative path, beginning with a "/", that represents
     * the canonical version of the specified path after ".." and "." elements
     * are resolved out.  If the specified path attempts to go outside the
     * boundaries of the current context (i.e. too many ".." path elements
     * are present), return <code>null</code> instead.
     *
     * @param path Path to be normalized
     */
    public static String normalize(String path) {

        if (path == null)
            return null;

        // Create a place for the normalized path
        String normalized = path;

        if (normalized.equals("/."))
            return "/";

        // Normalize the slashes and add leading slash if necessary
        if (normalized.indexOf('\\') >= 0)
            normalized = normalized.replace('\\', '/');
        if (!normalized.startsWith("/"))
            normalized = "/" + normalized;

        // Resolve occurrences of "//" in the normalized path
        while (true) {
            int index = normalized.indexOf("//");
            if (index < 0)
                break;
            normalized = normalized.substring(0, index) +
                    normalized.substring(index + 1);
        }

        // Resolve occurrences of "/./" in the normalized path
        while (true) {
            int index = normalized.indexOf("/./");
            if (index < 0)
                break;
            normalized = normalized.substring(0, index) +
                    normalized.substring(index + 2);
        }

        // Resolve occurrences of "/../" in the normalized path
        while (true) {
            int index = normalized.indexOf("/../");
            if (index < 0)
                break;
            if (index == 0)
                return (null);  // Trying to go outside our context
            int index2 = normalized.lastIndexOf('/', index - 1);
            normalized = normalized.substring(0, index2) +
                    normalized.substring(index + 3);
        }

        // Return the normalized path that we have completed
        return (normalized);
    }

    /**
     * Decode a given path, removing all url encodings, etc. and normalizes it
     *
     * @param request the current request
     * @param path    the path to decode
     * @return decoded path
     */
    public static String decodePath(HttpServletRequest request, String path) {
        // Remove url encoding from destination
        path = FxWebDavUtils.URLDecode(path, "UTF8");

        int protocolIndex = path.indexOf("://");
        if (protocolIndex >= 0) {
            // if the Destination URL contains the protocol, we can safely
            // trim everything upto the first "/" character after "://"
            int firstSeparator =
                    path.indexOf("/", protocolIndex + 4);
            if (firstSeparator < 0) {
                path = "/";
            } else {
                path = path.substring(firstSeparator);
            }
        } else {
            if ((request.getServerName() != null) && (path.startsWith(request.getServerName()))) {
                path = path.substring(request.getServerName().length());
            }
            int portIndex = path.indexOf(":");
            if (portIndex >= 0) {
                path = path.substring(portIndex);
            }
            if (path.startsWith(":")) {
                int firstSeparator = path.indexOf("/");
                if (firstSeparator < 0) {
                    path = "/";
                } else {
                    path =
                            path.substring(firstSeparator);
                }
            }
        }
        // Normalize destination path (remove '.' and '..')
        path = FxWebDavUtils.normalize(path);
        if (request.getContextPath() != null && path.startsWith(request.getContextPath()))
            path = path.substring(request.getContextPath().length());

        if (request.getPathInfo() != null && request.getServletPath() != null &&
                path.startsWith(request.getServletPath()))
            path = path.substring(request.getServletPath().length());
        return path;
    }

    private static Hashtable<String, String> contentType_mapping = null;

    /**
     * Get the content type mapping for a given file extension
     *
     * @param extension
     * @return the mapping
     */
    public static synchronized String getContentTypeMapping(String extension) {
        if (contentType_mapping == null) {
            contentType_mapping = new Hashtable<String, String>(50);
            // Image section
            contentType_mapping.put("gif", "image/gif");
            contentType_mapping.put("jpg", "image/jpeg");
            contentType_mapping.put("jpeg", "image/jpeg");
            contentType_mapping.put("jpe", "image/jpeg");
            contentType_mapping.put("ief", "image/ief");
            contentType_mapping.put("tiff", "image/tiff");
            contentType_mapping.put("tif", "image/tiff");
            contentType_mapping.put("bmp", "image/bitmap");
            // Text section
            contentType_mapping.put("htm", "text/html");
            contentType_mapping.put("html", "text/html");
            contentType_mapping.put("css", "text/css");
            contentType_mapping.put("txt", "text/plain");
            contentType_mapping.put("text", "text/plain");
            contentType_mapping.put("jsp", "text/html");
            contentType_mapping.put("js", "text/javascript");
            contentType_mapping.put("rtx", "text/richtext");
            contentType_mapping.put("sgm", "text/richtext");
            contentType_mapping.put("sgml", "text/x-sgml");
            contentType_mapping.put("etx", "text/x-setext");
            // Video section
            contentType_mapping.put("mpg", "video/mpeg");
            contentType_mapping.put("mpeg", "video/mpeg");
            contentType_mapping.put("mpe", "video/mpeg");
            contentType_mapping.put("mov", "video/quicktime");
            contentType_mapping.put("qt", "video/quicktime");
            contentType_mapping.put("avi", "video/x-msvideo");
            contentType_mapping.put("movie", "video/x-sgi-movie");
            // Audio section
            contentType_mapping.put("m3u", "audio/m3u");
            contentType_mapping.put("mp3", "audio/mp3");
            contentType_mapping.put("wav", "audio/wave");
            contentType_mapping.put("au", "audio/basic");
            contentType_mapping.put("aif", "audio/x-aiff");
            contentType_mapping.put("aiff", "audio/x-aiff");
            contentType_mapping.put("aifc", "audio/x-aiff");
            contentType_mapping.put("snd", "audio/basic");
            contentType_mapping.put("midi", "audio/x-midi");
            contentType_mapping.put("mid", "audio/x-midi");
            contentType_mapping.put("ram", "audio/x-pn-realaudio");
            contentType_mapping.put("ra", "audio/x-pn-realaudio");
            contentType_mapping.put("rpm", "audio/x-pn-realaudio-plugin");
            // Application section
            contentType_mapping.put("doc", "application/msword");
            contentType_mapping.put("dot", "application/msword");
            contentType_mapping.put("pdf", "application/pdf");
            contentType_mapping.put("rtf", "application/rtf");
            contentType_mapping.put("xls", "application/excel");
            contentType_mapping.put("xla", "application/excel");
            contentType_mapping.put("ppt", "application/powerpoint");
            contentType_mapping.put("pot", "application/powerpoint");
            contentType_mapping.put("pps", "application/powerpoint");
            contentType_mapping.put("ppz", "application/powerpoint");
            contentType_mapping.put("ai", "application/postscript");
            contentType_mapping.put("eps", "application/postscript");
            contentType_mapping.put("ps", "application/postscript");
            contentType_mapping.put("tar", "application/x-tar");
            contentType_mapping.put("zip", "application/x-compressed");
            contentType_mapping.put("sh", "application/x-sh");
            contentType_mapping.put("csh", "application/x-csh");
            contentType_mapping.put("latex", "application/x-latex");
            contentType_mapping.put("ustar", "application/x-ustar");
            contentType_mapping.put("shar", "application/x-shar");
            contentType_mapping.put("mif", "application/mif");
            contentType_mapping.put("com", "application/octet-stream");
            contentType_mapping.put("exe", "application/octet-stream");
            contentType_mapping.put("bin", "application/octet-stream");
            contentType_mapping.put("dll", "application/octet-stream");
            contentType_mapping.put("class", "application/octet-stream");
            contentType_mapping.put("jar", "application/octet-stream");
            contentType_mapping.put("hlp", "application/mshelp");
            contentType_mapping.put("chm", "application/mshelp");
            contentType_mapping.put("pm6", "application/pagemaker");
            // Others section
            contentType_mapping.put("wrl", "x-world/x-vrml");
        }
        String type = contentType_mapping.get(extension.toLowerCase());
        if (type == null)
            type = "application/octet-string";
        return type;
    }

    /**
     * Sets the modified at date in the response to the given date.
     *
     * @param date the date to use
     */
    public static void setModifiedAtDate(HttpServletResponse response, Date date) {
        response.setHeader(HTTP_HEADER_LAST_MODIFIED, buildModifiedAtDate(date));
    }

    /**
     * Returns the modified-at date for a http response with
     * the given time.
     * <p/>
     * This function uses the HTTP standard time format ("Sat, 07 Apr 2001 00:58:08 GMT")
     *
     * @return the modified-at date with the current time
     */
    public static String buildModifiedAtDate(Date date) {
        // HTTP standard time format: "Sat, 07 Apr 2001 00:58:08 GMT"
        // Apache server SSI format: Saturday, 08-Sep-2001 21:46:40 EDT
        return HTTP_MODIFIED_AT_FORMATER.format(date);
    }
}
