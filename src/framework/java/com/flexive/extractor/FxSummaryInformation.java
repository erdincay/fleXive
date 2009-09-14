/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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

import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDDocumentInformation;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * A class storing informations about documents (pdf, word, excel, ...)
 */
public class FxSummaryInformation implements Serializable {
    String author;
    String applicationName;
    long charCount;
    String comments;
    Date createdAt;
    Date editTime;
    String keywords;
    String lastModifiedBy;
    Date lastPrintedAt;
    String title;
    Date lastModifiedAt;
    int pageCount;
    String revNumber;
    int wordCount;
    boolean encrypted;
    String additionalText;

    public FxSummaryInformation(String author, String applicationName, long charCount, String comments,
                                Date createdAt, Date editTime, String keywords, String lastModifiedBy,
                                Date lastPrintedAt, String title, Date lastModifiedAt, int pageCount,
                                String revNumber, int wordCount, boolean encrypted, String additionalText) {
        this.author = author;
        this.applicationName = applicationName == null ? "" : applicationName;
        this.charCount = charCount;
        this.comments = comments == null ? "" : comments;
        if (createdAt != null)
            this.createdAt = (Date) createdAt.clone();
        else
            this.createdAt = new Date();
        if (editTime != null)
            this.editTime = (Date) editTime.clone();
        else
            this.editTime = new Date();
        this.keywords = keywords == null ? "" : keywords;
        this.lastModifiedBy = lastModifiedBy;
        if (lastPrintedAt != null)
            this.lastPrintedAt = (Date) lastPrintedAt.clone();
        else
            this.lastPrintedAt = new Date();
        this.title = title == null ? "" : title;
        if (lastModifiedAt != null)
            this.lastModifiedAt = (Date) lastModifiedAt.clone();
        else
            this.lastModifiedAt = new Date();
        this.pageCount = pageCount;
        this.revNumber = revNumber == null ? "" : revNumber;
        this.wordCount = wordCount;
        this.encrypted = encrypted;
        this.additionalText = additionalText == null ? "" : additionalText;
    }

    public String toString() {
        return
                "author=" + this.author + ", " +
                        "application=" + this.applicationName + ", " +
                        "charCount:" + this.charCount + ", " +
                        "comments:" + this.comments + ", " +
                        "createdAt:" + this.createdAt + ", " +
                        "editTime:" + this.editTime + ", " +
                        "keywords:" + this.keywords + ", " +
                        "lastModifiedBy:" + this.lastModifiedBy + ", " +
                        "lastPrintedAt:" + this.lastPrintedAt + ", " +
                        "title:" + this.title + ", " +
                        "lastModifiedAt:" + this.lastModifiedAt + ", " +
                        "pageCount:" + this.pageCount + ", " +
                        "revNumber:" + this.revNumber + ", " +
                        "wordCount:" + this.wordCount + ", " +
                        "encrypted:" + this.encrypted;
    }

    public String toXML() {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("<summary>");
        encodeXML(sb, "author", author);
        encodeXML(sb, "applicationName", applicationName);
        encodeXML(sb, "charCount", charCount);
        encodeXML(sb, "comments", comments);
        encodeXML(sb, "createdAt", createdAt);
        encodeXML(sb, "editTime", editTime);
        encodeXML(sb, "keywords", keywords);
        encodeXML(sb, "lastModifiedBy", lastModifiedBy);
        encodeXML(sb, "lastPrintedAt", lastPrintedAt);
        encodeXML(sb, "title", title);
        encodeXML(sb, "lastModifiedAt", lastModifiedAt);
        encodeXML(sb, "pageCount", pageCount);
        encodeXML(sb, "revNumber", revNumber);
        encodeXML(sb, "wordCount", wordCount);
        encodeXML(sb, "encrypted", encrypted);
        encodeXML(sb, "additionalText", additionalText);
        sb.append("</summary>");
        return sb.toString();
    }

    private void encodeXML(StringBuilder sb, String tag, Object data) {
        if( data != null ) {
            sb.append("<").append(tag).append(">");
            if( data instanceof String)
                sb.append("<![CDATA[").append(data).append("]]>");
            else if( data instanceof Date)
                sb.append(((Date)data).getTime());
            else
                sb.append(data);
            sb.append("</").append(tag).append(">");
        }
    }

    /**
     * Constructor.
     *
     * @param si the summary information
     */
    public FxSummaryInformation(SummaryInformation si) {
        author = si.getAuthor();
        applicationName = si.getApplicationName();
        charCount = si.getCharCount();
        comments = si.getComments();
        createdAt = si.getCreateDateTime();
        editTime = new Date(si.getEditTime());
        keywords = si.getKeywords();
        lastModifiedBy = si.getLastAuthor();
        lastPrintedAt = si.getLastPrinted();
        title = si.getTitle();
        lastModifiedAt = si.getLastSaveDateTime();
        pageCount = si.getPageCount();
        revNumber = si.getRevNumber();
        wordCount = si.getWordCount();
        encrypted = false;
    }

    public FxSummaryInformation(final PDDocument pdf) {
        final PDDocumentInformation pi = pdf.getDocumentInformation();
        author = pi.getAuthor();
        applicationName = pi.getProducer();
        charCount = -1;
        comments = "";
        try {
            createdAt = pi.getCreationDate().getTime();
        } catch (Exception exc) {
            createdAt = null;
        }
        try {
            editTime = pi.getModificationDate().getTime();
            lastModifiedAt = editTime;
        } catch (Exception exc) {
            editTime = null;
            lastModifiedAt = null;
        }
        keywords = pi.getKeywords();
        lastPrintedAt = null;
        title = pi.getTitle();
        pageCount = pdf.getNumberOfPages();
        revNumber = "";
        wordCount = -1;
        encrypted = pdf.isEncrypted();
    }


    /**
     * Reads the summary information from a document.
     *
     * @param filename the file to read
     * @return the summary information
     */
    public static FxSummaryInformation getSummaryInformation(String filename) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(filename);
            FxSummaryInformation result = getSummaryInformation(input);
            input.close();
            return result;
        } catch (Exception ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception exc) {/**/}
            }
        }
    }

    /**
     * Reads the summary information from a document.
     *
     * @param input the input stream to read from, will not be closed at the end
     * @return the summary information
     */
    public static FxSummaryInformation getSummaryInformation(InputStream input) {
        class SummaryStore implements POIFSReaderListener {
            private FxSummaryInformation fxsi = null;

            /**
             * Processes the Summary section.
             *
             * @param event the summary section event.
             */
            public void processPOIFSReaderEvent(POIFSReaderEvent event) {
                try {
                    SummaryInformation si = (SummaryInformation) PropertySetFactory.create(event.getStream());
                    fxsi = new FxSummaryInformation(si);
                } catch (Exception ex) {
                    /* ignore */
                }
            }

            protected FxSummaryInformation getFxSummaryInformation() {
                return fxsi;
            }
        }
        try {
            POIFSReader reader = new POIFSReader();
            SummaryStore st = new SummaryStore();
            reader.registerListener(st, "\005SummaryInformation");
            reader.read(input);
            return st.getFxSummaryInformation();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Returns a string that is appended to the text used by the fulltext indexer.
     *
     * @return a string that is appended to the text used by the fulltext indexer
     */
    public String getFTIndexInformations() {
        StringBuffer sb = new StringBuffer(1024);
        if (author != null && author.length() > 0) {
            sb.append(author).append(" ");
        }
        if (applicationName != null && applicationName.length() > 0) {
            sb.append(author).append(" ");
        }
        if (comments != null && comments.length() > 0) {
            sb.append(author).append(" ");
        }
        if (keywords != null && keywords.length() > 0) {
            sb.append(author).append(" ");
        }
        if (title != null && title.length() > 0) {
            sb.append(author).append(" ");
        }
        if (revNumber != null && revNumber.length() > 0) {
            sb.append(author).append(" ");
        }
        return sb.toString();
    }

    public String getAuthor() {
        return author;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public long getCharCount() {
        return charCount;
    }

    public String getComments() {
        return comments;
    }

    public Date getCreatedAt() {
        return (Date) createdAt.clone();
    }

    public Date getEditTime() {
        return (Date) editTime.clone();
    }

    public String getKeywords() {
        return keywords;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Date getLastPrintedAt() {
        return (Date) lastPrintedAt.clone();
    }

    public String getTitle() {
        return title;
    }

    public Date getLastModifiedAt() {
        return (Date) lastModifiedAt.clone();
    }

    public int getPageCount() {
        return pageCount;
    }

    public String getRevNumber() {
        return revNumber;
    }

    public int getWordCount() {
        return wordCount;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public String getAdditionalText() {
        return additionalText;
    }
}
