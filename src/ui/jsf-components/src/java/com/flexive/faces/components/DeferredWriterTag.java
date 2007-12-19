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

import javax.faces.component.UIOutput;
import javax.faces.component.UIComponent;
import javax.faces.context.ResponseWriter;
import javax.faces.context.FacesContext;
import java.io.IOException;

/**
 * <p>
 * A "deferred writer" tag. Takes a single-method object that writes custom output using the
 * output writer, and invokes it in the JSF component's encodeBegin phase. This is
 * useful for writing custom HTML code that cannot be produced directly using JSF components,
 * or rendering javascript, while keeping the right output ordering in relation to "real"
 * JSF components. This component is only meaningful for programmatic view creation,
 * so it isn't registered in the faces-config.xml or .taglib.xml.
 * </p>
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class DeferredWriterTag extends UIOutput {
    /**
     * A custom writer to be embedded in the JSF component tree.
     */
    public static interface DeferredWriter {
        /**
         * Render the output to the given response writer.
         *
         * @param out   the response writer
         * @throws java.io.IOException  on I/O errors
         */
        void render(ResponseWriter out) throws IOException;
    }

    private DeferredWriter deferredWriter;

    public DeferredWriterTag() {
        setRendererType(null);
    }

    public DeferredWriterTag(UIComponent parent, DeferredWriter deferredWriter) {
        this.deferredWriter = deferredWriter;
        parent.getChildren().add(this); // small hack, just like the whole component  
    }

    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        if (deferredWriter != null) {
            deferredWriter.render(facesContext.getResponseWriter());
        }
    }
}
