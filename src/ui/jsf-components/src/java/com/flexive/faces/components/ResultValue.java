/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
import com.flexive.faces.components.input.FxValueInput;
import com.flexive.shared.ContentLinkFormatter;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.value.FxValue;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import java.io.IOException;

/**
 * <p>Renders a single result value returned from the flexive SQL search.
 * At some point in the future, this will also include support for
 * inline-editing of results.</p>
 * <p>The value to be rendered is passed in the "value" attribute.</p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ResultValue extends UIOutput {
    public static final String COMPONENT_TYPE = "flexive.ResultValue";

    private String contentLinkFormat = null;
    private String itemLinkFormat = null;
    private ContentLinkFormatter linkFormatter = null;

    public ResultValue() {
        setRendererType(null);
    }

    public String getContentLinkFormat() {
        if (contentLinkFormat == null) {
            contentLinkFormat = FxJsfComponentUtils.getStringValue(this, "contentLinkFormat");
        }
        return contentLinkFormat;
    }

    public void setContentLinkFormat(String contentLinkFormat) {
        this.contentLinkFormat = contentLinkFormat;
    }

    public String getItemLinkFormat() {
        if (itemLinkFormat == null) {
            itemLinkFormat = FxJsfComponentUtils.getStringValue(this, "itemLinkFormat");
        }
        return itemLinkFormat;
    }

    public void setItemLinkFormat(String itemLinkFormat) {
        this.itemLinkFormat = itemLinkFormat;
    }

    public ContentLinkFormatter getLinkFormatter() {
        if (linkFormatter == null) {
            linkFormatter = (ContentLinkFormatter) FxJsfComponentUtils.getValue(this, "linkFormatter");
        }
        if (linkFormatter == null) {
            linkFormatter = ContentLinkFormatter.getInstance();
        }
        return linkFormatter;
    }

    public void setLinkFormatter(ContentLinkFormatter linkFormatter) {
        this.linkFormatter = linkFormatter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        final Object value = getValue();
        if (value instanceof FxValue) {
            // use FxValueInput component to support inline-editing
            final FxValueInput input;
            if (getChildren().size() == 1 && getChildren().get(0) instanceof FxValueInput) {
                // reuse components in a data table
                input = (FxValueInput) getChildren().get(0);
            } else {
                input = (FxValueInput) FxJsfUtils.addChildComponent(this, FxValueInput.COMPONENT_TYPE);
            }
            input.setValue(value);
            input.setReadOnly(true);
        } else {
            FxSharedUtils.writeResultValue(context.getResponseWriter(), value, getLinkFormatter(),
                    getContentLinkFormat(), getItemLinkFormat());
        }

    }
}
