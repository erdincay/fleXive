/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;


public class PdfExtractor {
    private static final Log LOG = LogFactory.getLog(PdfExtractor.class);

    /**
     * Extracts the text informations from the pdf file.
     *
     * @param in the input stream to read from
     * @return the extracted information, or null if no text extraction was possible
     */
    public ExtractedData extract(final InputStream in) {
        ByteArrayOutputStream baos=null;
        PrintWriter writer =null;
        PDDocument document = null;
        try {
            baos = new ByteArrayOutputStream();
            writer= new PrintWriter(baos);
            document = PDDocument.load( in );
            PDFTextStripper stripper = new PDFTextStripper();
            try {
                stripper.writeText( document, writer );
            } catch (IOException e) {
                // usually because text extraction is not allowed
                LOG.warn("Failed to extract text from PDF file: " + e.getMessage());
            }
            FxSummaryInformation fxsi = new FxSummaryInformation(document);
            writer.write(fxsi.getFTIndexInformations());
            writer.flush();
            return new ExtractedData(fxsi,baos.toString());
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        } finally{
            try { if (writer!=null) writer.close(); } catch(Exception exc) {/*ignore*/}
            try { if (baos!=null) baos.close(); } catch(Exception exc) {/*ignore*/}
            try { if (document!=null) document.close(); } catch(Exception exc) {/*ignore*/}
        }

    }

}
