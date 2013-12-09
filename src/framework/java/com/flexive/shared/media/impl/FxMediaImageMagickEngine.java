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
package com.flexive.shared.media.impl;

import com.flexive.core.IMParser;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.media.FxMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * ImageMagick media engine
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxMediaImageMagickEngine {

    private static final Log LOG = LogFactory.getLog(FxMediaImageMagickEngine.class);

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
                    i = safeFormatIMNumber(ver[0]);
                    j = safeFormatIMNumber(ver[1]);
                    k = safeFormatIMNumber(ver[2]);
                } catch (NumberFormatException e) {
                    LOG.error("Could not format: " + IM_VERSION, e);
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

    /**
     * Format numbers like 1-0
     *
     * @param s number string to format
     * @return formatted number
     */
    private static int safeFormatIMNumber(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            if (s.indexOf('-') > 0) {
                return Integer.parseInt(s.substring(0, s.indexOf('-')));
            }
            throw e;
        }
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
            LOG.error("Error at [" + curr + "]:" + e.getMessage(), e);
        }
        writer.writeEndDocument();
        writer.flush();
        writer.close();
        return sw.getBuffer().toString();
    }

    /**
     * Identify a file, returning metadata
     *
     * @param mimeType if not null it will be used to call the correct identify routine
     * @param file     the file to identify
     * @return metadata
     * @throws FxApplicationException on errors
     * @since 3.1
     */
    public static FxMetadata identify(String mimeType, File file) throws FxApplicationException {
        if (mimeType == null)
            mimeType = FxMediaNativeEngine.detectMimeType(null, file.getAbsolutePath());
        try {
            String metaData = IMParser.getMetaData(file);

            String format = "unknown";
            String formatDescription = "";
            String compressionAlgorithm = "";
            String colorType = "";
            int width = 0;
            int height = 0;
            double xRes = 0.0;
            double yRes = 0.0;
            int bpp = 0;

            DocumentBuilder builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(metaData.getBytes()));
            XPath xPath = javax.xml.xpath.XPathFactory.newInstance().newXPath();

            Node nFormat = (Node) xPath.evaluate("/Image/Format", document, javax.xml.xpath.XPathConstants.NODE);
            if (nFormat != null && nFormat.getTextContent() != null) {
                format = nFormat.getTextContent();
                if (format.indexOf(' ') > 0) {
                    formatDescription = format.substring(format.indexOf(' ') + 1);
                    if (formatDescription.indexOf('(') >= 0 && formatDescription.indexOf(')') > 0) {
                        formatDescription = formatDescription.substring(formatDescription.indexOf('(') + 1, formatDescription.indexOf(')'));
                    }
                    format = format.substring(0, format.indexOf(' '));
                }
            }

            Node nCompression = (org.w3c.dom.Node) xPath.evaluate("/Image/Compression", document, javax.xml.xpath.XPathConstants.NODE);
            if (nCompression != null && nCompression.getTextContent() != null) {
                compressionAlgorithm = nCompression.getTextContent();
            }

            Node nColorType = (Node) xPath.evaluate("/Image/Colorspace", document, javax.xml.xpath.XPathConstants.NODE);
            if (nColorType != null && nColorType.getTextContent() != null) {
                colorType = nColorType.getTextContent();
            }

            Node nGeometry = (Node) xPath.evaluate("/Image/Geometry", document, javax.xml.xpath.XPathConstants.NODE);
            if (nGeometry != null && nGeometry.getTextContent() != null) {
                String geo = nGeometry.getTextContent();
                if (geo.indexOf('+') > 0)
                    geo = geo.substring(0, geo.indexOf('+'));
                if (geo.indexOf('x') > 0) {
                    try {
                        width = Integer.parseInt(geo.substring(0, geo.indexOf('x')));
                        height = Integer.parseInt(geo.substring(geo.indexOf('x') + 1));
                    } catch (NumberFormatException ex) {
                        //failed, ignore
                    }

                }
            }

            Node nResolution = (Node) xPath.evaluate("/Image/Resolution", document, javax.xml.xpath.XPathConstants.NODE);
            if (nResolution != null && nResolution.getTextContent() != null) {
                String res = nResolution.getTextContent();
                if (res.indexOf('+') > 0)
                    res = res.substring(0, res.indexOf('+'));
                if (res.indexOf('x') > 0) {
                    try {
                        xRes = Double.parseDouble(res.substring(0, res.indexOf('x')));
                        yRes = Double.parseDouble(res.substring(res.indexOf('x') + 1));
                    } catch (NumberFormatException ex) {
                        //failed, ignore
                    }

                }
            }

            Node nDepth = (Node) xPath.evaluate("/Image/Depth", document, javax.xml.xpath.XPathConstants.NODE);
            if (nDepth != null && nDepth.getTextContent() != null) {
                String dep = nDepth.getTextContent();
                if (dep.indexOf('-') > 0)
                    dep = dep.substring(0, dep.indexOf('-'));
                try {
                    bpp = Integer.parseInt(dep);
                } catch (NumberFormatException ex) {
                    //failed, ignore
                }
            }

            List<FxMetadata.FxMetadataItem> items = new ArrayList<FxMetadata.FxMetadataItem>(50);
            NodeList nodes = (NodeList) xPath.evaluate("/Image/*", document, javax.xml.xpath.XPathConstants.NODESET);
            Node currNode;
            for (int i = 0; i < nodes.getLength(); i++) {
                currNode = nodes.item(i);
                if (currNode.hasChildNodes() && currNode.getChildNodes().getLength() > 1) {
                    for (int j = 0; j < currNode.getChildNodes().getLength(); j++)
                        items.add(new FxMetadata.FxMetadataItem(currNode.getNodeName() + "/" + currNode.getChildNodes().item(j).getNodeName(), currNode.getChildNodes().item(j).getTextContent()));
                } else
                    items.add(new FxMetadata.FxMetadataItem(currNode.getNodeName(), currNode.getTextContent()));
            }
            return new FxImageMetadataImpl(mimeType, file.getName(), items, width, height, format, formatDescription, compressionAlgorithm, xRes, yRes, colorType, false, bpp, false, false, null);
        } catch (Exception e) {
            throw new FxApplicationException(e, "ex.media.identify.error", file.getName(), mimeType, e.getMessage());
        }

    }
}
