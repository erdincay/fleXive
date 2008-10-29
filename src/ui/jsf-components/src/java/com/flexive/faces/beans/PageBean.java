package com.flexive.faces.beans;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.SystemParameterPaths;
import com.flexive.shared.configuration.ParameterScope;
import com.flexive.shared.configuration.parameters.ParameterFactory;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.faces.FxJsfUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

/**
 * <p>Page controller that holds general information about the current page being displayed that is stored in
 * the URL (for example the output language).</p>
 *
 * This includes
 * <ul>
 * <li>the locale (possibly encoded in the URL),</li>
 * <li>an internal page ID (usually treated as tree node or content ID),</li>
 * <li>and the tree path (for creating readable URLs based on tree FQNs).</li>
 * </ul>
 * <p>
 * To allow customization of the page controller, this bean is <strong>NOT</strong> included
 * in the components {@code faces-config.xml},
 * but must be added to your own application's <code>faces-config.xml</code>, for example using:
 * <pre>{@code <managed-bean>
 * <managed-bean-name>fxPageBean</managed-bean-name>
 * <managed-bean-class>com.flexive.faces.beans.PageBean</managed-bean-class>
 * <managed-bean-scope>request</managed-bean-scope>
 * <managed-property>
 * <property-name>languageCode</property-name>
 * <value>#{param.fxLanguageCode}</value>
 * </managed-property>
 * <managed-property>
 * <property-name>pageId</property-name>
 * <value>#{param.fxPageId}</value>
 * </managed-property>
 * <managed-property>
 * <property-name>treePath</property-name>
 * <value>#{param.fxTreePath}</value>
 * </managed-property>
 * <managed-property>
 * <property-name>url</property-name>
 * <value>#{param.fxUrl}</value>
 * </managed-property>
 * </managed-bean>}</pre>
 * <p/>
 * By changing the implementation class ({@code managed-bean-class}) you can use your own implementation
 * which will also be used for the JSF-EL methods for generating URLs
 * ({@code fx:url} and {@code fx:urlWithLocale}).
 * </p>
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class PageBean implements Serializable {
    private static final long serialVersionUID = -4274184717568826256L;
    private static final Log LOG = LogFactory.getLog(PageBean.class);

    protected String languageCode;
    protected long pageId = -1;
    protected String treePath;
    protected String url;

    /**
     * Return the page bean for the current request. Note that the implementation class
     * is configured in your application's {@code faces-config.xml}, so you can overwrite
     * or modify the behaviour to fit your application's requirements. The page bean
     * is accessible through the EL name {@code fxPageBean}.
     *
     * @return the page bean instance for the current request.
     */
    public static PageBean getInstance() {
        return FxJsfUtils.getManagedBean(PageBean.class);
    }

    /**
     * Create an absolute URL for the current page settings and the specified locale.
     *
     * @param languageCode the language code (e.g. "de", "en")
     * @param url          the URL (will be treated as absolute URL in the context path)
     * @return an absolute URL for the current page settings and the specified locale.
     */
    public String createAbsoluteUrl(String languageCode, String url) {
        // don't include the locale by default, just return an absolute URL
        return FxJsfUtils.getRequest().getContextPath() + "/"
                + (url.startsWith("/") ? url.substring(1) : url);
    }

    /**
     * Create an absolute URL for the current context and page settings (locale, ...).
     *
     * @param url the URL to be formatted
     * @return an absolute URL for the current page
     */
    public String createAbsoluteUrl(String url) {
        return createAbsoluteUrl(languageCode, url);
    }

    /**
     * Return the language code of the current page. This corresponds to the value returned by
     * {@link com.flexive.shared.FxLanguage#getIso2digit()} (in lowercase).
     *
     * @return  the language code of the current page
     */
    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode != null ? languageCode.toLowerCase() : null;
        if (StringUtils.isNotBlank(languageCode)) {
            try {
                FxContext.get().getTicket().setLanguage(
                        EJBLookup.getLanguageEngine().load(languageCode)
                );
            } catch (FxApplicationException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to set request locale from URL: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Return the page ID. It can be interpreted arbitrarily, usually it will identify
     * a tree node or content instance.
     *
     * @return  the page ID. If no page ID is set, -1 will be returned.
     */
    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId != null ? pageId : -1;
    }

    /**
     * Return the tree path of the current page, if set. Tree paths can be used to identify
     * tree nodes (and their referenced content) through a human-readable path built from
     * the content FQNs of the path nodes.
     *
     * @return  the tree path of the current page
     */
    public String getTreePath() {
        return treePath;
    }

    public void setTreePath(String treePath) {
        this.treePath = treePath;
    }

    /**
     * Return the actual page URL, <strong>without</strong> the locale information.
     * In particular, {@code createAbsoluteUrl(url)} should return the original external request URL
     * (prior to rewriting).
     *
     * @return the page URL
     */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Adds the context path and the current page locale to the URL.
     * Available in JSF-EL via <code>fx:urlWithLocale</code>.
     *
     * @param languageCode the language code, e.g. "en" or "de"
     * @param url          the URL, e.g. "index.xhtml"
     * @return an absolute URL including the context path and locale, e.g. "/flexive/en/index.xhtml"
     */
    public static String absoluteUrl(String languageCode, String url) {
        return getInstance().createAbsoluteUrl(languageCode, url);
    }

    /**
     * Adds the context path and the current page locale to the URL.
     * Available in JSF-EL via <code>fx:url</code>.
     *
     * @param url the URL, e.g. "index.xhtml"
     * @return an absolute URL including the context path and locale, e.g. "/flexive/en/index.xhtml"
     */
    public static String absoluteUrl(String url) {
        return absoluteUrl(getInstance().getLanguageCode(), url);
    }
}
