package com.flexive.example.war;

import com.flexive.faces.beans.PageBean;
import com.flexive.faces.FxJsfUtils;

/**
 * Localized URLs for the product application.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class ProductsPageBean extends PageBean {
    private static final long serialVersionUID = -2038740768543452081L;

    /** {@inheritDoc} */
    @Override
    public String createAbsoluteUrl(String languageCode, String url) {
        return  // make absolute URL
                FxJsfUtils.getRequest().getContextPath() + "/"
                // add locale as first URL element (e.g. "/de/index.xhtml")
                + (languageCode != null ? languageCode + "/" : "")
                // add URL
                + (url.startsWith("/") ? url.substring(1) : url);
    }

}
