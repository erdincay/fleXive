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
package com.flexive.extractor;

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
        this.text = text.trim().replaceAll("[\\x00-\\x09\\x0B-\\x1F]","");
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
     * 4 characters, and contains every distinct uppercase word only one time.
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
                if (word.length() < 4) continue;
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
        sb.append("<extract>");
        if (getSummaryInformation() != null)
            sb.append(getSummaryInformation().toXML());
        text = text.replaceAll("<!\\[CDATA\\[", "&lt;![CDATA[").replaceAll("\\]\\]>", "]]&gt;");
        sb.append("<text><![CDATA[").append(getText().replace('\f', ' ')).append("]]></text>");
        sb.append("<compressed><![CDATA[").append(getCompressedText()).append("]]></compressed>");
        sb.append("</extract>");
        return sb.toString();
    }

    public static String toEmptyXML() {
        return "<extract><summary></summary><compressed></compressed></extract>";
    }
}
