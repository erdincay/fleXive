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
package com.flexive.extractor;

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.stream.FxStreamUtils;
import com.flexive.shared.value.BinaryDescriptor;
import java.io.InputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class to bundle the document-extractor dependencies outside DocumentParser and flexive-extractor.jar.
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1.2
 */
class DocumentExtractor {
    private static final Log LOG = LogFactory.getLog(DocumentExtractor.class);

    /**
     * Extract metadata from the document (doc will be loaded from the binary storage)
     *
     * @param desc an instance of the BinaryDescriptor for the file t.b. examined
     * @return the (updated) BinaryDescriptor
     */
    public static BinaryDescriptor extractDocumentMetaData(BinaryDescriptor desc) {
        // retrieve the binary
        InputStream inputStream = null;
        try {
            inputStream = FxStreamUtils.getBinaryStream(desc, BinaryDescriptor.PreviewSizes.ORIGINAL);
            final Extractor.DocumentType documentType = getDocumentType(desc.getMimeType());
            final ExtractedData extractedData = Extractor.extractData(inputStream, documentType);

            if (extractedData != null) {
                return new BinaryDescriptor(desc.getHandle(), desc.getName(), desc.getSize(),
                        desc.getMimeType(), extractedData.toXML());
            }

        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not download binary file or create InputStream " + e.getMessage(), e);
            }
        } finally {
            FxSharedUtils.close(inputStream);
        }
        return desc;
    }

    /**
     * Conversion to DOCUMENT type: retrieve the actual Extractor.DocumentType
     * (see also relevant BinaryPreviewProcess script)
     *
     * @param mimeType the mime type as a String
     * @return the Extractor.DocumentType or null if not match is found
     */
    private static Extractor.DocumentType getDocumentType(String mimeType) {
        if ("application/msword".equals(mimeType)) {
            return Extractor.DocumentType.Word;
        } else if ("application/mspowerpoint".equals(mimeType)) {
            return Extractor.DocumentType.Powerpoint;
        } else if ("application/msexcel".equals(mimeType)) {
            return Extractor.DocumentType.Excel;
        } else if ("application/pdf".equals(mimeType)) {
            return Extractor.DocumentType.PDF;
        } else if ("text/html".equals(mimeType)) {
            return Extractor.DocumentType.HTML;
        } else {
            // possible future change: on-the-fly creation of relevant text type?
            return null;
        }
    }
}
