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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


public class Extractor {

    public static enum DocumentType {
        Word,
        Excel,
        PDF,
        Powerpoint,
        HTML
    }

    /**
     * Extracts data from a given file.
     *
     * @param filename the filename
     * @param type     the type of the document
     * @return the extracted data
     */
    public static ExtractedData extractData(final String filename, final DocumentType type) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(filename);
            return extractData(input, type);
        } catch (Exception ex) {
            return null;
        } finally {
            try {
                if (input != null) input.close();
            } catch (Exception exc) {/*ignore*/}
        }
    }

    /**
     * Extracts data from a given file.
     *
     * @param file the file
     * @param type the type of the document
     * @return the extracted data
     */
    public static ExtractedData extractData(final File file, final DocumentType type) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            return extractData(input, type);
        } catch (Exception ex) {
            return null;
        } finally {
            try {
                if (input != null) input.close();
            } catch (Exception exc) {/*ignore*/}
        }
    }

    /**
     * Extracts data from a given input stream.
     *
     * @param in   the input stream to read from, it is not closed at the end
     * @param type the type of the document
     * @return the extracted data
     */
    public static ExtractedData extractData(final InputStream in, final DocumentType type) {
        switch (type) {
            case Word:
                return new WordExtractor().extract(in);
            case Powerpoint:
                return new PowerpointExtractor().extract(in);
            case Excel:
                return new ExcelExtractor().extract(in);
            case PDF:
                return new PdfExtractor().extract(in);
            case HTML:
                return HtmlExtractor.extract(in);
            default:
                return null;
        }
    }
}
