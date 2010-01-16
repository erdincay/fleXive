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
package com.flexive.core.storage;

import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.impex.FxDivisionExportInfo;
import com.flexive.shared.impex.FxImportExportConstants;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Division Importer
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class GenericDivisionImporter implements FxImportExportConstants {

    private static GenericDivisionImporter INSTANCE = new GenericDivisionImporter();

    /**
     * Getter for the importer singleton
     *
     * @return exporter
     */
    public static GenericDivisionImporter getInstance() {
        return INSTANCE;
    }

    /**
     * Get division export information from an exported archive
     *
     * @param zip zip file containing the export
     * @return FxDivisionExportInfo
     * @throws FxApplicationException on errors
     */
    public FxDivisionExportInfo getDivisionExportInfo(ZipFile zip) throws FxApplicationException {
        ZipEntry ze = zip.getEntry(FILE_BUILD_INFOS);
        if (ze == null)
            throw new FxNotFoundException("ex.import.missingFile", FILE_BUILD_INFOS, zip.getName());
        FxDivisionExportInfo exportInfo;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(zip.getInputStream(ze));

            XPath xPath = XPathFactory.newInstance().newXPath();

            String[] drops;
            String dropsRaw = xPath.evaluate("/flexive/drops", document);
            if (dropsRaw == null || !dropsRaw.startsWith("["))
                drops = new String[0];
            else {
                dropsRaw = dropsRaw.substring(1, dropsRaw.length() - 1);
                drops = dropsRaw.split(", ");
            }
            exportInfo = new FxDivisionExportInfo(
                    Integer.parseInt(xPath.evaluate("/flexive/division", document)),
                    Integer.parseInt(xPath.evaluate("/flexive/schema", document)),
                    Integer.parseInt(xPath.evaluate("/flexive/build", document)),
                    xPath.evaluate("/flexive/verbose", document),
                    xPath.evaluate("/flexive/appserver", document),
                    xPath.evaluate("/flexive/database", document),
                    xPath.evaluate("/flexive/dbdriver", document),
                    xPath.evaluate("/flexive/domain", document),
                    drops,
                    xPath.evaluate("/flexive/user", document),
                    FxFormatUtils.getDateTimeFormat().parse(xPath.evaluate("/flexive/date", document))
            );
        } catch (Exception e) {
            throw new FxApplicationException(e, "ex.import.parseInfoFailed", e.getMessage());
        }
        return exportInfo;
    }
}
