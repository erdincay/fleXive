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
 * @since 3.1.7
 */
public class FxBasicFilter implements Filter {
    private static final String CTX_GUEST_ACCESS_ALLOWED = "FxBasicFilter.guestAllowed";

    private volatile boolean guestAccessAllowed;

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

    public void destroy() {
        // nop
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        this.guestAccessAllowed = "true".equalsIgnoreCase(filterConfig.getInitParameter("allowGuestAccess"));
    }
}
