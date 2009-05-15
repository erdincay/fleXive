package com.flexive.war.filter;

import com.flexive.shared.FxSharedUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.net.URLEncoder;

/**
 * Removes an optional flexive version identifier used for creating unique resource URLs from the request.
 * <p>
 * The version can be embedded in any place of the URL and is prefixed with "$VF_". Only versions matching the
 * exact version as returned by {@link com.flexive.shared.FxSharedUtils#getFlexiveVersion()} are replaced.
 * </p>
 * <p>
 * Similar to Weblets, -SNAPSHOT versions disable the browser cache.
 * </p>
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 * @since 3.1
 */
public class VersionUrlFilter implements Filter {
    public static final String URL_PATTERN;
    private static final Log LOG = LogFactory.getLog(VersionUrlFilter.class);

    private FilterConfig config;

    static {
        try {
            URL_PATTERN = "$VF_" + URLEncoder.encode(FxSharedUtils.getFlexiveVersion(), "UTF-8");
        } catch(UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Failed to create URL pattern: " + e.getMessage(), e);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final String requestUri = request.getRequestURI().substring(request.getContextPath().length());
        final int pos = requestUri.indexOf(URL_PATTERN);
        if (pos != -1) {
            // remove version from uri and forward to real path
            final String uri = requestUri.substring(0, pos) + requestUri.substring(pos + URL_PATTERN.length());
            if (LOG.isTraceEnabled()) {
                LOG.trace("Removed version string from URI '" + requestUri + "', new URI: '" + uri + "'.");
            }
            // disable cache for -SNAPSHOT versions (similar to Weblets)
            final HttpServletResponse response = (HttpServletResponse) servletResponse;
            if (FxSharedUtils.isSnapshotVersion()) {
                response.setDateHeader("Expires", 0);
                response.setDateHeader("Last-modified", new Date().getTime());
            } else {
                // set expiry date far in the future, since we're certain the contents won't change
                response.setDateHeader("Expires", new Date().getTime() + 30L * 24 * 3600 * 1000);
            }
            // forward to real path
            request.getRequestDispatcher(uri).forward(servletRequest, servletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    public void destroy() {
        this.config = null;
    }
}
