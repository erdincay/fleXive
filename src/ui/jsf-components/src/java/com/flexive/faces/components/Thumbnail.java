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
 * @author Daniel Lichtenberger, UCS
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
