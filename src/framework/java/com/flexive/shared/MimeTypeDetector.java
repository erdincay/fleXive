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
package com.flexive.shared;

import org.apache.commons.lang.StringUtils;

/**
 * Mime type detector
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class MimeTypeDetector {

    /**
     * Detect the mimetype of a file based on the first n bytes and the filename
     *
     * @param header first n bytes of the file to examine
     * @return detected mimetype
     */
    public static String detect(byte[] header) {
        return detect(header, null);
    }

    /**
     * Detect the mimetype of a file based on the first n bytes and the filename
     *
     * @param header   first n bytes of the file to examine
     * @param fileName filename
     * @return detected mimetype
     */
    public static String detect(byte[] header, String fileName) {
        //TODO: code/script/complete me!
        if (!StringUtils.isEmpty(fileName) && fileName.indexOf('.') > 0) {
            //extension based detection
            fileName = fileName.trim().toUpperCase();
            if (fileName.endsWith(".JPG"))
                return "image/jpeg";
            if (fileName.endsWith(".GIF"))
                return "image/gif";
            if (fileName.endsWith(".PNG"))
                return "image/png";
            if (fileName.endsWith(".BMP"))
                return "image/bmp";
            if (fileName.endsWith(".DOC") || fileName.endsWith(".DOCX") )
                return "application/msword";
            if (fileName.endsWith(".XLS") || fileName.endsWith(".XLSX"))
                return "application/msexcel";
            if (fileName.endsWith(".PPT") || fileName.endsWith(".PPTX"))
                return "application/mspowerpoint";
            if (fileName.endsWith(".PDF"))
                return "application/pdf";
            if (fileName.endsWith(".HTM"))
                return "text/html";
            if (fileName.endsWith(".HTML"))
                return "text/html";
            if (fileName.endsWith(".TXT"))
                return "text/plain";
        }
        //byte signature based detection
        if (header != null && header.length > 5 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) { //PNG
            System.out.println("PNG detected!!!!");
            return "image/png";
        }
        return "application/unknown";
    }
}
