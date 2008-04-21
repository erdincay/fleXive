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
package com.flexive.faces.components;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.XPathElement;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxBinary;
import com.flexive.war.servlet.ThumbnailServlet;

import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.context.FacesContext;
import java.io.IOException;

/**
 * A basic component for displaying thumbnails of content instances. Renders URLs
 * to the {@link com.flexive.war.servlet.ThumbnailServlet}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class Thumbnail extends UIOutput {
    private FxPK pk;
    private FxBinary binary;
    private HtmlGraphicImage image;

    public Thumbnail() {
        setRendererType(null);
    }

    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(this, HtmlGraphicImage.COMPONENT_TYPE);
        final String link;
        if (getPk() != null) {
            link = ThumbnailServlet.getLink(getPk(), BinaryDescriptor.PreviewSizes.PREVIEW2);
        } else if (getBinary() != null) {
            link = ThumbnailServlet.getLink(XPathElement.getPK(getBinary().getXPath()),
                    BinaryDescriptor.PreviewSizes.PREVIEW2,
                    getBinary().getXPath());
        } else {
            throw new FxInvalidParameterException("pk", "ex.jsf.thumbnail.empty").asRuntimeException();
        }
        image.setUrl(link);
    }

    @Override
    public void encodeEnd(FacesContext facesContext) throws IOException {
        getChildren().remove(image);
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
}
