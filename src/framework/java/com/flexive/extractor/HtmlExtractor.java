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
package com.flexive.extractor;

import java.io.InputStream;

/**
 * This class allows meta data and text extraction from a HTML stream (file).
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class HtmlExtractor {

    /**
     * Extracts the text informations from the html stream.
     *
     * @param in the input stream to read from
     * @return the extraxted informations, or null if no text extraction was possible
     */
    public static ExtractedData extract(final InputStream in) {
        com.flexive.extractor.htmlExtractor.HtmlExtractor result =
                new com.flexive.extractor.htmlExtractor.HtmlExtractor(in, true);
        FxSummaryInformation si = new FxSummaryInformation(
                result.getAuthor()/*author*/, result.getGenerator()/*appName*/, result.getCharacterCount(), ""/*comments*/,
                result.getCreated()/*createdAt*/, result.getCreated()/*editTime*/, result.getKeywords(),
                result.getAuthor()/*lastModifiedBy*/,
                null/*lastPrintedAt*/, result.getTitle(), result.getCreated()/*lastModifiedAt*/, 1/*pageCount*/,
                null/*revNumber*/, result.getWordCount(), false/*encrypted*/, result.getTagText());
        return new ExtractedData(si, result.getText());
    }

    /**
     * Extracts the text informations from the html stream.
     *
     * @param html the HTML data
     * @return the extraxted informations, or null if no text extraction was possible
     */
    public static ExtractedData extract(final String html) {
        com.flexive.extractor.htmlExtractor.HtmlExtractor result =
                new com.flexive.extractor.htmlExtractor.HtmlExtractor(html, true);
        FxSummaryInformation si = new FxSummaryInformation(
                result.getAuthor()/*author*/, result.getGenerator()/*appName*/, result.getCharacterCount(), ""/*comments*/,
                result.getCreated()/*createdAt*/, result.getCreated()/*editTime*/, result.getKeywords(),
                result.getAuthor()/*lastModifiedBy*/,
                null/*lastPrintedAt*/, result.getTitle(), result.getCreated()/*lastModifiedAt*/, 1/*pageCount*/,
                null/*revNumber*/, result.getWordCount(), false/*encrypted*/, result.getTagText());
        return new ExtractedData(si, result.getText());
    }
}
