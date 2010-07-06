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
package com.flexive.faces.components.input;

import com.flexive.shared.exceptions.FxUpdateException;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxBinary;
import com.flexive.shared.value.FxValue;
import javax.faces.context.FacesContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.custom.fileupload.HtmlInputFileUpload;
import org.apache.myfaces.custom.fileupload.UploadedFile;

/**
 * JSF1 renderer implementation for {@code fxValueInput}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class Jsf1FxValueInputRenderer extends AbstractFxValueInputRenderer<Jsf1FxValueInput> {
    private static final Log LOG = LogFactory.getLog(Jsf1FxValueInputRenderer.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public RenderHelper getRenderHelper(FacesContext context, Jsf1FxValueInput component, FxValue value, boolean editMode) {
        return editMode
                ? new Jsf1EditModeHelper(component, component.getClientId(context), value)
                : new Jsf1ReadOnlyModeHelper(component, component.getClientId(context), value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processBinary(FacesContext context, AbstractFxValueInput input, String inputId, FxBinary value, long languageId) {
        final HtmlInputFileUpload upload = (HtmlInputFileUpload) input.findComponent(stripNamingContainers(inputId));
        if (upload != null) {
            try {
                upload.processDecodes(context);
                final UploadedFile file = (UploadedFile) upload.getSubmittedValue();
                if (file != null && file.getSize() >= 0 && !StringUtils.isEmpty(file.getName())) {
                    String name = file.getName();
                    if (name.indexOf('\\') > 0) {
                        name = name.substring(name.lastIndexOf('\\') + 1);
                    }
                    value.setTranslation(languageId, new BinaryDescriptor(name, file.getSize(), file.getInputStream()));
                }
            } catch (Exception e) {
                //noinspection ThrowableInstanceNeverThrown
                throw new FxUpdateException(LOG, e, "ex.jsf.valueInput.file.upload.io", e).asRuntimeException();
            }
        }
    }
}
