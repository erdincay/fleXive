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
package com.flexive.extractor.htmlExtractor;

import com.flexive.shared.FxFormatUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * HTML Text Extractor.
 * Part of the fleXive 3.X Framework
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class HtmlExtractor {

    boolean convertSpecialHtmlChars;
    private StringBuffer result;
    private StringBuffer tagText;
    private Exception error = null;    
    private Hashtable meta = new Hashtable(25);
    private int characterCount;
    private int wordCount=0;
    private final static String META_AUTHOR = "AUTHOR";
    private final static String META_DESCRIPTION = "DESCRIPTION";
    private final static String META_KEYWORDS = "KEYWORDS";
    private final static String META_DATE = "DATE";
    private final static String META_TITLE = "TITLE";
    private final static String META_CREATOR = "CREATOR";
    private final static String META_SUBJECT = "SUBJECT";
    private final static String META_PUBLISHER = "PUBLISHER";
    private final static String META_CONTRIBUTER = "CONTRIBUTER";
    private final static String META_TYPE = "TYPE";
    private final static String META_LANGUAGE = "LANGUAGE";
    private final static String META_ROBOTS = "ROBOTS";
    protected final static String META_CREATED = "CREATED";
    protected final static String META_LAST_MODIFIED = "LAST_MODIFIED";
    private final static String META_GENERATOR = "GENERATOR";

    final static String META_TAGS[] = {
            META_AUTHOR,META_DESCRIPTION,META_KEYWORDS,META_DATE,META_TITLE,META_CREATOR
            ,META_SUBJECT,META_PUBLISHER,META_CONTRIBUTER,META_TYPE,META_LANGUAGE,META_ROBOTS,META_GENERATOR
    };

    /**
     * Returns the number of words in the EXTRACTED text.
     *
     * @return the number of words in the EXTRACTED text.
     */
    public int getWordCount() {
        return wordCount;
    }

    /**
     * The total number of characters in the HTML file.
     *
     * @return the total number of characters in the HTML file
     */
    public int getCharacterCount() {
        return characterCount;
    }


    /**
     * Constructor.
     *
     * @param convertSpecialHtmlChars if set to true special HTML characters are replaced
     * to a readable form in text files (eg german umlaute).
     * @param html the html to parse
     */
    public HtmlExtractor(String html,boolean convertSpecialHtmlChars) {
        this.convertSpecialHtmlChars = convertSpecialHtmlChars;
        extract(html);
    }

    /**
     * Constructor.
     *
     * @param convertSpecialHtmlChars if set to true special HTML characters are replaced
     * to a readable form in text files (eg german umlaute).
     * @param in the html to parse
     */
    public HtmlExtractor(InputStream in,boolean convertSpecialHtmlChars) {
        this.convertSpecialHtmlChars = convertSpecialHtmlChars;
        StringBuffer buffer = null;
        try {
            buffer = new StringBuffer(in.available());
            int achar;
            while((achar=in.read())!=-1) {
                buffer.append((char)achar);
            }
        } catch (Exception exc) {
            //
        }
        extract(buffer==null?"":buffer.toString());
    }

    /**
     * Extracts the text informations from the html file.
     *
     * @param html the HTML
     */
    private void extract(final String html) {
        // Store character count
        this.characterCount = html.length();
        // Get TEXT
        this.result = new StringBuffer(html.length()/5);
        this.tagText = new StringBuffer(1024);
        try {
            ByteArrayInputStream byis = new ByteArrayInputStream(html.getBytes("UTF-8"));
            new HtmlExtractorParser(byis,"UTF-8").extract(this);
        } catch (Exception exc) {
            exc.printStackTrace();
            error = exc;
        }
        // Store word count
        StringTokenizer st = new StringTokenizer(result.toString()," ",false);
        while (st.hasMoreTokens()) {
            this.wordCount++;
            st.nextToken();
        }
    }

    protected void setTitle(Token tk) {
        String title = tk.image;
        title = title.substring("<title>".length(),title.length()-"</title>".length());
        //noinspection unchecked
        meta.put(META_TITLE,title.trim());
    }

    /**
     * Returns the extracted text.
     *
     * @return the extracted text
     */
    public String getText() {
        return FxFormatUtils.removeCommandChars(this.result.toString().trim());
    }

    /**
     * Returns the text extracted from tag attributes like 'title' and 'alt'.
     *
     * @return the text extracted from tag attributes like 'title' and 'alt'.
     */
    public String getTagText() {
        return tagText.toString();
    }


    public String getAuthor() {
        return (String)meta.get(META_AUTHOR);
    }

    public String getGenerator() {
        return (String)meta.get(META_GENERATOR);
    }

    public Date getCreated() {
        return metaToDate((String)meta.get(META_CREATED));
    }

    public String getKeywords() {
        return (String)meta.get(META_KEYWORDS);
    }

    public String getTitle() {
        return (String)meta.get(META_TITLE);
    }

    private Date metaToDate(String value) {
        if (value==null) {
            return null;
        }
        // eg "6 Feb 1999 09:31:30+01:00 "
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
        try {
            return sdf.parse(value.trim());
        } catch (Exception exc) {
            /* ignore */
            return null;
        }
    }


    protected void addMeta(String key,String value) {
        //noinspection unchecked
        meta.put(key,value);
    }

    protected void append(Token text) {
        append(text.image);
    }

    protected void append(String text) {
        this.result.append(getValue(text));
    }

    protected void appendTagText(String text) {
        if (tagText.length()>0) {
            tagText.append(" ");
        }
        tagText.append(getValue(text).trim());
    }


    private String getValue(String value) {
        if (value.length()==0) {
            return "";
        }
        if (convertSpecialHtmlChars) {
            value = value.replaceAll("&#8217;","'");
            value = value.replaceAll("&nbsp;"," ");
            value = value.replaceAll("&amp;","&");
            value = value.replaceAll("&quot;","\"");
            // kl Umlaute
            value = value.replaceAll("&auml;","\u00E4");
            value = value.replaceAll("&uuml;","\u00FC");
            value = value.replaceAll("&ouml;","\u00F6");
            // gr Umlaute
            value = value.replaceAll("&Auml;","\u00C4");
            value = value.replaceAll("&Uuml;","\u00DC");
            value = value.replaceAll("&Ouml;","\u00D6");
            // Scharfes s
            value = value.replaceAll("&szlig;","\u00DF");
            // greater / less than
            value = value.replaceAll("&gt;",">");
            value = value.replaceAll("&lt;","<");
            // Euro
            value = value.replaceAll("&euro;","?");
            value = value.replaceAll("&#8364;","?");
            value = value.replaceAll("&#x20AC;","?");
        }
        return value;
    }


    /**
     * Returns true if a error occured during the parseing - in this case only the
     * text extracted up to the error is returned.
     *
     * @return true if a error occured during the parseing
     */
    public boolean hadError() {
        return this.error!=null;
    }

    /**
     * Returns null if the parser was successfully, or the parser error.
     *
     * @return null if the parser was successfully, or the parser error
     */
    public Exception getError() {
        return error;
    }

}

