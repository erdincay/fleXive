package com.flexive.war.filter;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * A minimal filter for flexive requests. Sets only a guest ticket and does not touch the HTTP session, in contrast to
 * {@link FxFilter}.
 *
 * <h3>Filter init parameters</h3>
 * <ul>
 *     <li><strong>allowGuestAccess</strong> - this init-param controls whether guest access (i.e. access without a token)
 *     to the REST-API interface is allowed. When allowed, the security restrictions for the guest will still apply (default: false)</li>
 * </ul>
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 *
 * @since 3.2.0
 */
public class FxBasicFilter implements Filter {
    private static final String CTX_GUEST_ACCESS_ALLOWED = "FxBasicFilter.guestAllowed";
    private static final String CTX_REQUEST_URI = "FxBasicFilter.uri";
    private static final String CTX_REQUEST_QUERYSTRING = "FxBasicFilter.queryString";

    private volatile boolean guestAccessAllowed;

    @Override
    public void doFilter(ServletRequest request, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        FxRequestUtils.setCharacterEncoding(request, servletResponse);

        final int divisionId = FxFilter.getDivisionId(request, servletResponse);
        if (divisionId == FxContext.DIV_UNDEFINED || !(request instanceof HttpServletRequest)) {
            return;
        }

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final boolean sessionExists = httpRequest.getSession(false) != null;

        final FxContext ctx = FxContext.storeEmptyContext(httpRequest, divisionId, false, false);
        ctx.overrideTicket(EJBLookup.getAccountEngine().getGuestTicket());

        try {
            ctx.setAttribute(CTX_GUEST_ACCESS_ALLOWED, guestAccessAllowed);
            ctx.setAttribute(CTX_REQUEST_URI, httpRequest.getRequestURI().substring(httpRequest.getContextPath().length()));
            ctx.setAttribute(CTX_REQUEST_QUERYSTRING, httpRequest.getQueryString());
            filterChain.doFilter(request, servletResponse);
        } finally {
            FxContext.cleanup();

            // make sure that no session was created for this request
            final HttpSession session = httpRequest.getSession(false);
            if (!sessionExists && session != null) {
                session.invalidate();
            }
        }
    }

    /**
     * @return  true when the filter is configured to allow guest access (i.e. without tokens)
     */
    public static boolean isGuestAccessAllowed() {
        return Boolean.TRUE == FxContext.get().getAttribute(CTX_GUEST_ACCESS_ALLOWED);
    }

    /**
     * @return  the request URI (without context)
     */
    public static String getRequestUri() {
        return (String) FxContext.get().getAttribute(CTX_REQUEST_URI);
    }

    /**
     * @return  the query string
     */
    public static String getRequestQueryString() {
        return (String) FxContext.get().getAttribute(CTX_REQUEST_QUERYSTRING);
    }

    @Override
    public void destroy() {
        // nop
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.guestAccessAllowed = "true".equalsIgnoreCase(filterConfig.getInitParameter("allowGuestAccess"));
    }
}
