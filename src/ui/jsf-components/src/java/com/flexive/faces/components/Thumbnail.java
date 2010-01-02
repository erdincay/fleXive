/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.faces.components;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.XPathElement;
import com.flexive.shared.media.FxMediaSelector;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxBinary;
import com.flexive.war.servlet.ThumbnailServlet;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.util.Arrays;

/**
 * A basic component for displaying thumbnails of content instances. Renders URLs
 * to the {@link com.flexive.war.servlet.ThumbnailServlet}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class Thumbnail extends UIOutput {
    public static final String COMPONENT_TYPE = "flexive.Thumbnail";
    /**
     * CSS class of the image being rendered.
     * @since 3.1
     */
    public static final String CSS_CLASS = "fxThumbnail";
    
    private FxPK pk;
    private FxBinary binary;
    private UIComponent body;
    private boolean urlOnly;
    private int width = -1;
    private int height = -1;
    private int rot = 0;
    private boolean flipH = false;
    private boolean flipV = false;
    private String forceVersion;
    private String previewSize = BinaryDescriptor.PreviewSizes.PREVIEW2.name();

    public Thumbnail() {
        setRendererType(null);
    }

    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        final String link;
        final BinaryDescriptor.PreviewSizes previewSize;
        try {
            previewSize = BinaryDescriptor.PreviewSizes.valueOf(getPreviewSize().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid preview size: " + getPreviewSize()
                    + ", possible values are: " + Arrays.toString(BinaryDescriptor.PreviewSizes.values()));
        }
        FxMediaSelector selector = new FxMediaSelector();
        if (getPk() != null) {
            selector.setPK(getPk());
        } else if (getBinary() != null) {
            selector.setPK(XPathElement.getPK(getBinary().getXPath()));
            selector.setXPath(getBinary().getXPath());
        } else {
            throw new FxInvalidParameterException("pk", "ex.jsf.thumbnail.empty").asRuntimeException();
        }
        
        // override with forceVersion
        if ("max".equalsIgnoreCase(getForceVersion())) {
            selector.setPK(new FxPK(selector.getPK().getId(), FxPK.MAX));
        } else if ("live".equalsIgnoreCase(getForceVersion())) {
            selector.setPK(new FxPK(selector.getPK().getId(), FxPK.LIVE));
        }

        selector.setSize(previewSize);
        if( width != -1 )
            selector.setScaleWidth(width);
        if( height != -1 )
            selector.setScaleHeight(height);
        if( rot % 360 != 0)
            selector.setRotationAngle(rot);
        if( flipH )
            selector.setFlipHorizontal(true);
        if( flipV )
            selector.setFlipVertical(true);
        link = ThumbnailServlet.getLink(selector);
        if (isUrlOnly()) {
            body = FxJsfUtils.addChildComponent(this, HtmlOutputText.COMPONENT_TYPE);
            ((HtmlOutputText) body).setEscape(false);
            ((HtmlOutputText) body).setValue(link.substring(1));    // don't return absolute URLs to the client
        } else {
            body = FxJsfUtils.addChildComponent(this, HtmlGraphicImage.COMPONENT_TYPE);
            final HtmlGraphicImage image = (HtmlGraphicImage) body;
            image.setUrl(link);
            image.setStyleClass(CSS_CLASS);
            image.setAlt(FxJsfComponentUtils.getStringValue(this, "alt", ""));
            image.setTitle(FxJsfComponentUtils.getStringValue(this, "title", null));
        }
    }

    @Override
    public void encodeEnd(FacesContext facesContext) throws IOException {
        getChildren().remove(body);
    }

    public FxPK getPk() {
        if (pk == null) {
            return (FxPK) FxJsfComponentUtils.getValue(this, "pk");
        }
        return pk;
    }

    public void setPk(FxPK pk) {
        this.pk = pk;
    }

    public FxBinary getBinary() {
        if (binary == null) {
            return (FxBinary) FxJsfComponentUtils.getValue(this, "binary");
        }
        return binary;
    }

    public void setBinary(FxBinary binary) {
        this.binary = binary;
    }

    public boolean isUrlOnly() {
        return FxJsfComponentUtils.getBooleanValue(this, "urlOnly", urlOnly);
    }

    public void setUrlOnly(boolean urlOnly) {
        this.urlOnly = urlOnly;
    }

    public String getPreviewSize() {
        return FxJsfComponentUtils.getStringValue(this, "previewSize", previewSize);
    }

    public void setPreviewSize(String previewSize) {
        this.previewSize = previewSize;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getRot() {
        return rot;
    }

    public void setRot(int rot) {
        this.rot = rot;
    }

    public boolean isFlipH() {
        return flipH;
    }

    public void setFlipH(boolean flipH) {
        this.flipH = flipH;
    }

    public boolean isFlipV() {
        return flipV;
    }

    public void setFlipV(boolean flipV) {
        this.flipV = flipV;
    }

    public String getForceVersion() {
        if (forceVersion == null) {
            return FxJsfComponentUtils.getStringValue(this, "forceVersion");
        }
        return forceVersion;
    }

    public void setForceVersion(String forceVersion) {
        this.forceVersion = forceVersion;
    }

    @Override
    public Object saveState(FacesContext context) {
        final Object[] state = new Object[11];
        state[0] = super.saveState(context);
        state[1] = pk;
        state[2] = binary;
        state[3] = urlOnly;
        state[4] = previewSize;
        state[5] = width;
        state[6] = height;
        state[7] = rot;
        state[8] = flipH;
        state[9] = flipV;
        state[10] = forceVersion;
        return state;
    }

    @Override
    public void restoreState(FacesContext context, Object oState) {
        final Object[] state = (Object[]) oState;
        super.restoreState(context, state[0]);
        pk = (FxPK) state[1];
        binary = (FxBinary) state[2];
        urlOnly = state[3] != null && (Boolean) state[3];
        previewSize = (String) state[4];
        width = (Integer)state[5];
        height = (Integer)state[6];
        rot = (Integer)state[7];
        flipH = (Boolean)state[8];
        flipV = (Boolean)state[9];
        forceVersion = (String) state[10];
    }
}
