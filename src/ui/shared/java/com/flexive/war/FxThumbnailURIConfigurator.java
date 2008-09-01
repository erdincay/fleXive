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
package com.flexive.war;

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.media.FxMediaSelector;
import com.flexive.shared.value.BinaryDescriptor;

import java.util.regex.Pattern;

/**
 * Configuration helper that parses a URI for thumbnail configuration parameters
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxThumbnailURIConfigurator extends FxMediaSelector {

    private final static Pattern pPK = Pattern.compile("^pk\\d+(\\.(\\d+|MAX|LIVE))?");
    private final static Pattern pXPath = Pattern.compile("^xp.+");
    private final static Pattern pSize = Pattern.compile("s[0123]");
    private final static Pattern pWidth = Pattern.compile("w\\d+");
    private final static Pattern pHeight = Pattern.compile("h\\d+");
    private final static Pattern pLang = Pattern.compile("^lang[a-zA-Z]{2}");
    private final static Pattern pLangFallback = Pattern.compile("^lfb[01]");


    private String URI;
    private String err500 = null;
    private String err404 = null;
    private String err403 = null;

    /**
     * Ctor from URI
     *
     * @param URI the servlet URI containing the configuration options
     */
    public FxThumbnailURIConfigurator(String URI) {
        this.URI = URI;
        parse();
    }

    /**
     * Valid options are:
     * <ul>
     * <li><code>pk{n.m}</code>  - id and version of the content, if no version is given the live version is used</li>
     * <li><code>xp{path}</code>  - URL encoded XPath of the property containing the image (optional, else default will be used)</li>
     * <li><code>lang{lang}</code> - 2-digit ISO language code
     * <li><code>lfb{0,1}</code> - language fallback: 0=generate error if language not found, 1=fall back to default language
     * <li><code>e{3 digit error number}u{error url}</code> - URL encoded error url to redirect for http errors specified in {error number}, if no number is given then the url is a fallback for unclassified errors
     * <li><code>s{0,1,2,3}</code>  - use a predefined image/thumbnail size, 0=default</li>
     * <li><code>w{n}</code> - scale to width</li>
     * <li><code>h{n}</code> - scale to height</li>
     * <li><code>rot{90,180,270}</code> - rotate 90, 180 or 270 degrees (rotate is always executed before flip operations)</li>
     * <li><code>flip{h,v}</code> - flip horizontal or vertical (rotate is always executed before flip operations)</li>
     * <li><code>cropx{x}y{y}w{w}h{h}</code> - crop a box from image defined by x,y,w(idth),h(eight), scaling applies to cropped image!</li>
     * </ul>
     */
    private void parse() {
        String[] elements = URI.split("\\/");
        for (String element : elements) {
            if (getPK() == null && pPK.matcher(element).matches()) {
                setPK(FxPK.fromString(element.substring(2)));
            }
            if (getXPath() == null && pXPath.matcher(element).matches()) {
                setXPath(FxSharedUtils.decodeXPath(element.substring(2)));
            }
            if (getSize() == BinaryDescriptor.PreviewSizes.ORIGINAL && pSize.matcher(element).matches())
                setSize(BinaryDescriptor.PreviewSizes.fromString(element.substring(1)));
            if (getScaleHeight() == -1 && pHeight.matcher(element).matches()) {
                setScaleHeight(Integer.parseInt(element.substring(1)));
            }
            if (getScaleWidth() == -1 && pWidth.matcher(element).matches()) {
                setScaleWidth(Integer.parseInt(element.substring(1)));
            }
            if (getLanguageIso() == null && pLang.matcher(element).matches())
                setLang(element.substring(4));
            if (!useLangFallback() && pLangFallback.matcher(element).matches())
                setLangFallback("1".equals(element.substring(3)));
        }
        if (getXPath() == null)
            setXPath("/");
    }

    /**
     * Get the original configuration URI
     *
     * @return the original configuration URI
     */
    public String getURI() {
        return URI;
    }
}
