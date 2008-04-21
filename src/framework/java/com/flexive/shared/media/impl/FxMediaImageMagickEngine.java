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
package com.flexive.shared.media.impl;

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.util.regex.Pattern;
import java.util.StringTokenizer;

/**
 * ImageMagick media engine
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxMediaImageMagickEngine {

    private static final transient Log LOG = LogFactory.getLog(FxMediaImageMagickEngine.class);

    public static final boolean IM_AVAILABLE;
    public static final boolean IM_IDENTIFY_POSSIBLE;
    public static final String IM_VERSION;
    public static final int IM_MAJOR;
    public static final int IM_MINOR;
    public static final int IM_SUB;

    static {
        FxSharedUtils.ProcessResult res = FxSharedUtils.executeCommand("convert", "-version");
        IM_AVAILABLE = !(res.getExitCode() != 0 || res.getStdOut().indexOf("ImageMagick") <= 0);

        if (IM_AVAILABLE) {
            StringTokenizer tok = new StringTokenizer(res.getStdOut(), " ", false);
            if (tok.hasMoreElements())
                tok.nextElement();
            if (tok.hasMoreElements())
                tok.nextElement();
            if (tok.hasMoreElements())
                IM_VERSION = (String) tok.nextElement();
            else
                IM_VERSION = "unknown";
            String[] ver = IM_VERSION.split("\\.");
            if (ver.length == 3) {
                int i = 0, j = 0, k = 0;
                try {
                    i = Integer.parseInt(ver[0]);
                    j = Integer.parseInt(ver[1]);
                    k = Integer.parseInt(ver[2]);
                } catch (NumberFormatException e) {
                    LOG.error(e);
                }
                IM_MAJOR = i;
                IM_MINOR = j;
                IM_SUB = k;
            } else {
                IM_MAJOR = 0;
                IM_MINOR = 0;
                IM_SUB = 0;
            }
        } else {
            IM_VERSION = "unknown";
            IM_MAJOR = 0;
            IM_MINOR = 0;
            IM_SUB = 0;
        }
        IM_IDENTIFY_POSSIBLE = IM_AVAILABLE && (IM_MAJOR > 6 || (IM_MAJOR == 6 && IM_MINOR >= 3));
    }

    // Name of the identify executeable
    public final static String IDENTIFY_BINARY = "identify";
    // Name of the convert executeable
    public final static String CONVERT_BINARY = "convert";

    /**
     * Scale an image and return the dimensions (width and height) as int array
     *
     * @param original  original file
     * @param scaled    scaled file
     * @param extension extension
     * @param width     desired width
     * @param height    desired height
     * @return actual width ([0]) and height ([1]) of scaled image
     * @throws FxApplicationException on errors
     */
    public static int[] scale(File original, File scaled, String extension, int width, int height) throws FxApplicationException {
        FxSharedUtils.ProcessResult res = FxSharedUtils.executeCommand(CONVERT_BINARY, "-scale",
                width + "x" + height,
                original.getAbsolutePath(), scaled.getAbsolutePath());
        if (res.getExitCode() != 0)
            throw new FxApplicationException("ex.executeCommand.failed", CONVERT_BINARY, res.getStdErr());
        res = FxSharedUtils.executeCommand(IDENTIFY_BINARY, "-ping",
                FxSharedUtils.escapePath(scaled.getAbsolutePath()));
        if (res.getExitCode() != 0)
            throw new FxApplicationException("ex.executeCommand.failed", IDENTIFY_BINARY, res.getStdErr());
        return getPingDimensions(extension, res.getStdOut());
    }

    /**
     * Parse a ping response from ImageMagick for image dimensions
     *
     * @param extension extension of the file
     * @param line      the response from ImageMagick's ping command
     * @return array containing dimensions or {0,0} if an error occured
     */
    public static int[] getPingDimensions(String extension, String line) {
        try {
            int start = 0;
            if (extension.equals(".JPG"))
                start = line.indexOf(" JPEG ") + 1;
            if (start <= 0 && extension.equals(".PNG"))
                start = line.indexOf(" PNG ") + 1;
            if (start <= 0 && extension.equals(".GIF"))
                start = line.indexOf(" GIF ") + 1;
            if (start <= 0) {
                String[] tmp = line.split(" ");
                if (tmp[2].indexOf('x') > 0) {
                    String[] dim = tmp[2].split("x");
                    return new int[]{Integer.parseInt(dim[0]), Integer.parseInt(dim[1])};
                }
            }
            if (start > 0) {
                String[] data = line.substring(start).split(" ");
                String[] dim = data[1].split("x");
                return new int[]{Integer.parseInt(dim[0]), Integer.parseInt(dim[1])};
            }
        } catch (Exception e) {
            return new int[]{0, 0};
        }
        return new int[]{0, 0};
    }

    /**
     * Get the identation depth of the current line (2 characters = 1 level)
     *
     * @param data line to examine
     * @return identation depth
     */
    private static int getLevel(String data) {
        if (data == null || data.length() == 0)
            return 0;
        int ident = 0;
        for (int i = 0; i < data.length(); i++) {
            if (data.charAt(i) != ' ')
                return ident / 2;
            ident++;
        }
        if (ident == 0)
            return ident;
        return ident / 2;
    }

    static Pattern pNumeric = Pattern.compile("^\\s*\\d+\\: .*");
    static Pattern pColormap = Pattern.compile("^\\s*Colormap\\: \\d+");
//    private final static Pattern pSkip = Pattern.compile("^\\s*\\d+\\:.*|^0x.*|^\\s*unknown.*|^\\s*Custom Field.*");


    /**
     * Parse an identify stdOut result (from in) and convert it to an XML content
     *
     * @param in identify response
     * @return XML content
     * @throws XMLStreamException on errors
     * @throws IOException        on errors
     */
    public static String parse(InputStream in) throws XMLStreamException, IOException {
        StringWriter sw = new StringWriter(2000);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(sw);
        writer.writeStartDocument();

        int lastLevel = 0, level, lastNonValueLevel = 1;
        boolean valueEntry;
        String curr = null;
        String[] entry;
        try {
            while ((curr = br.readLine()) != null) {
                level = getLevel(curr);
                if (level == 0 && curr.startsWith("Image:")) {
                    writer.writeStartElement("Image");
                    entry = curr.split(": ");
                    if (entry.length >= 2)
                        writer.writeAttribute("source", entry[1]);
                    lastLevel = level;
                    continue;
                }
                if (!(valueEntry = pNumeric.matcher(curr).matches())) {
                    while (level < lastLevel--)
                        writer.writeEndElement();
                    lastNonValueLevel = level;
                } else
                    level = lastNonValueLevel + 1;
                if (curr.endsWith(":")) {
                    writer.writeStartElement(curr.substring(0, curr.lastIndexOf(':')).trim().replaceAll("[ :]", "-"));
                    lastLevel = level + 1;
                    continue;
                } else if (pColormap.matcher(curr).matches()) {
                    writer.writeStartElement(curr.substring(0, curr.lastIndexOf(':')).trim().replaceAll("[ :]", "-"));
                    writer.writeAttribute("colors", curr.split(": ")[1].trim());
                    lastLevel = level + 1;
                    continue;
                }
                entry = curr.split(": ");
                if (entry.length == 2) {
                    if (!valueEntry) {
                        writer.writeStartElement(entry[0].trim().replaceAll("[ :]", "-"));
                        writer.writeCharacters(entry[1]);
                        writer.writeEndElement();
                    } else {
                        writer.writeEmptyElement("value");
                        writer.writeAttribute("key", entry[0].trim().replaceAll("[ :]", "-"));
                        writer.writeAttribute("data", entry[1]);
//                        writer.writeEndElement();
                    }
                } else {
//                    System.out.println("unknown line: "+curr);
                }
                lastLevel = level;
            }
        } catch (Exception e) {
            LOG.error("Error at [" + curr + "]:" + e.getMessage());
        }
        writer.writeEndDocument();
        writer.flush();
        writer.close();
        return sw.getBuffer().toString();
    }

}
