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

import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.FxXMLUtils;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;


public class ExtractedData implements Serializable {
    private FxSummaryInformation si;
    private String text;
    private String compressed = null;

    protected ExtractedData(FxSummaryInformation si, String text) {
        this.si = si;
        if( text == null ) {
            this.text = "";
            return;
        }
        this.text = FxFormatUtils.removeCommandChars(text.trim());
    }

    /**
     * Returns the meta information extracted from the document.
     *
     * @return the meta information extracted from the document
     */
    public FxSummaryInformation getSummaryInformation() {
        return si;
    }

    /**
     * Returns the text extracted from the document.
     *
     * @return the text extracted from the document
     */
    public String getText() {
        return text;
    }

    /**
     * Returns a compressed form of the extracted text that only contains words with at least
     * 4 and at most 30 characters, and contains every distinct uppercase word only one time.
     * Additional text (eg text from html tag attributes like 'title' and 'alt') stored in the
     * FxSummaryInformation will be included.
     *
     * @return a compressed form of the extracted text
     */
    public String getCompressedText() {
        if (compressed == null) {
            Hashtable<String, Boolean> words = new Hashtable<String, Boolean>(5000);
            StringBuffer concateWords = new StringBuffer(30000);
            // Split by whitespaces and other chars, also remove dups.
            // Only keep words that have more than 3 characters
            String txt = this.text;
            if (si != null)
                txt += (si.getTitle() == null ? "" : " " + si.getTitle()) +
                        (si.getKeywords() == null ? "" : " " + si.getKeywords()) +
                        (si.getAuthor() == null ? "" : " " + si.getAuthor()) +
                        (si.getComments() == null ? "" : " " + si.getComments()) +
                        (si.getRevNumber() == null ? "" : " " + si.getRevNumber()) +
                        (si.getAdditionalText() == null ? " " : si.getAdditionalText());

            txt = txt.replace("?", " ").replace(".", " ").replace("!", " ");
            String[] sw = txt.split("[\\s,;:=\\(\\)\"-']");
            for (String word : sw) {
                if (word.length() < 4 || word.length() > 30) continue;
                words.put(word.toUpperCase(), Boolean.TRUE);
            }
            for (Enumeration e = words.keys(); e.hasMoreElements();) {
                concateWords.append(" ").append(e.nextElement());
            }
            compressed = concateWords.toString();
        }
        return compressed;
    }

    public String toXML() {
        StringBuilder sb = new StringBuilder(1000);
        sb.append(FxXMLUtils.XML_HEADER);
        sb.append("<extract>\n");
        if (getSummaryInformation() != null)
            sb.append(getSummaryInformation().toXML());
//        text = text.replaceAll("<!\\[CDATA\\[", "&lt;![CDATA[").replaceAll("\\]\\]>", "]]&gt;");
        sb.append("<text>").append(StringEscapeUtils.escapeXml(getText())).append("</text>\n");
        sb.append("<compressed>").append(StringEscapeUtils.escapeXml(getCompressedText())).append("</compressed>\n");
        sb.append("</extract>\n");
        return sb.toString();
    }

    public static String toEmptyXML() {
        return "<extract>\n\t<summary>\n\t</summary>\n\t<compressed>\n\t</compressed>\n</extract>\n";
    }
}
