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
package com.flexive.shared;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single XPath element (alias and multiplicity)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class XPathElement implements Serializable {
    private static final long serialVersionUID = 2037392183607142045L;
    private static String PK = "@pk=(NEW|\\d*\\.(LIVE|MAX|\\d*))";
    /**
     * First element must start with a "/",
     * an XPath element must start with a letter followed by an optional letter/number/underscore combination
     * and may end with an optional multiplicity like [x] where x is a number
     */
    private static Pattern XPathPattern = Pattern.compile("([A-Z][A-Z_0-9]{0,}\\[" + PK + "\\]){0,1}(\\/[A-Z][A-Z_0-9]{0,}(\\[[0-9]{1,}\\]){0,1}){1,}");
    private static Pattern PKPattern = Pattern.compile(PK);
    private static Pattern doubleSlashPattern = Pattern.compile("[\\/]{2,}");
    private String alias;
    private int index;
    private boolean indexDefined;
    private static final List<XPathElement> EMPTY = new ArrayList<XPathElement>(0);

    /**
     * Ctor
     *
     * @param alias        alias to use
     * @param index        multiplicity to apply
     * @param indexDefined was the multiplicity explicitly defined?
     */
    public XPathElement(String alias, int index, boolean indexDefined) {
        this.alias = alias;
        this.index = index;
        this.indexDefined = indexDefined;
    }

    /**
     * Getter for the alias
     *
     * @return alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Getter for the multiplicity
     *
     * @return multiplicity
     */
    public int getIndex() {
        return index;
    }

    /**
     * Setter for the multiplicity
     *
     * @param index the multiplicity to apply
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Was the multiplicity explicitly defined?
     *
     * @return multiplicity explicitly defined
     */
    public boolean isIndexDefined() {
        return indexDefined;
    }

    /**
     * get FQN of the alias
     *
     * @return FQ alias
     */
    @Override
    public String toString() {
        return alias + "[" + index + "]";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof XPathElement && ((XPathElement) obj).getAlias().equals(this.getAlias()) && ((XPathElement) obj).getIndex() == this.getIndex();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.getAlias().hashCode() + this.getIndex();
    }

    /**
     * Split an XPath into its elements
     *
     * @param XPath
     * @return XPathElement array
     * @throws FxInvalidParameterException for invalid elements
     */
    public static List<XPathElement> split(String XPath) throws FxInvalidParameterException {
        if (StringUtils.isEmpty(XPath))
            return EMPTY;
        if (XPath.charAt(0) != '/' && XPath.indexOf('/') > 0) {
            //we have a full qualified XPath with type name that needs to be stripped
            XPath = XPath.substring(XPath.indexOf('/'));
        }
        if (!isValidXPath(XPath))
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", XPath);
        String[] xp = XPath.substring(1).split("\\/"); //skip first '/' to avoid empty entries
        List<XPathElement> elements = new ArrayList<XPathElement>(xp.length);
        for (String xpcurr : xp) {
            elements.add(toElement(XPath, xpcurr));
        }
        return elements;
    }

    /**
     * Get the last (rightmost) element of an XPath
     *
     * @param XPath
     * @return last (rightmost) element of an XPath
     * @throws FxInvalidParameterException
     */
    public static XPathElement lastElement(String XPath) throws FxInvalidParameterException {
        if (StringUtils.isEmpty(XPath) || !isValidXPath(XPath))
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", XPath);
        return toElement(XPath, XPath.substring(XPath.lastIndexOf('/') + 1));
    }

    /**
     * Convert an alias of an XPath to an element
     *
     * @param XPath full XPath, only used if exception is thrown
     * @param alias alias to convert to an XPathElement
     * @return XPathElement
     * @throws FxInvalidParameterException
     */
    public static XPathElement toElement(String XPath, String alias) throws FxInvalidParameterException {
        if (StringUtils.isEmpty(alias) || alias.indexOf('/') >= 0)
            throw new FxInvalidParameterException("XPATH", "ex.xpath.element.invalid", alias, XPath);
        try {
            if (alias.indexOf('[') > 0)
                return new XPathElement(alias.substring(0, alias.indexOf('[')).toUpperCase(),
                        Integer.valueOf(alias.substring(alias.indexOf('[') + 1, alias.length() - 1)), true);
            return new XPathElement(alias.toUpperCase(), 1, false);
        } catch (Exception e) {
            throw new FxInvalidParameterException("XPATH", "ex.xpath.element.invalid", alias, XPath);
        }
    }

    /**
     * Check if this XPath is valid
     *
     * @param XPath
     * @return valid or not
     */
    public static boolean isValidXPath(String XPath) {
        if ("/".equals(XPath))
            return true;
        return !StringUtils.isEmpty(XPath) && XPathPattern.matcher(XPath).matches();
    }


    /**
     * Get the XPath of an array of XPathElements with multiplicities
     *
     * @param xpe list containing XPathElement
     * @return XPath
     */
    public static String toXPath(List<XPathElement> xpe) {
        if (xpe == null || xpe.size() == 0)
            return "/";
        StringBuffer XPath = new StringBuffer(100);
        for (XPathElement xp : xpe) {
            XPath.append('/').append(xp.getAlias()).append('[').append(xp.getIndex()).append(']');
        }
        return XPath.toString();
    }

    /**
     * Get the XPath of an array of XPathElements without multiplicities
     *
     * @param xpe list containing XPathElement
     * @return XPath
     */
    public static String toXPathNoMult(List<XPathElement> xpe) {
        if (xpe == null || xpe.size() == 0)
            return "/";
        StringBuffer XPath = new StringBuffer(100);
        for (XPathElement xp : xpe) {
            XPath.append('/').append(xp.getAlias());
        }
        return XPath.toString();
    }


    /**
     * Get the given XPath with full multiplicity information
     *
     * @param XPath XPath
     * @return XPath with full multiplicity information
     * @throws FxInvalidParameterException for invalid XPath
     */
    public static String toXPathMult(String XPath) throws FxInvalidParameterException {
        if (StringUtils.isEmpty(XPath) || "/".equals(XPath))
            return "/";
        XPath = XPath.toUpperCase();
        String type = null;
        if (XPath.charAt(0) != '/' && XPath.indexOf('/') > 0) {
            //we have a full qualified XPath with type name that needs to be stripped temporarily
            type = XPath.substring(0, XPath.indexOf('/'));
            XPath = XPath.substring(XPath.indexOf('/'));
        }
        if (!isValidXPath(XPath))
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", XPath);
        String[] xp = XPath.substring(1).split("\\/"); //skip first '/' to avoid empty entries
        StringBuffer xpc = new StringBuffer(XPath.length() + 10);
        for (String xpcurr : xp) {
            xpc.append('/');
            if (xpcurr.indexOf('[') > 0)
                xpc.append(xpcurr);
            else
                xpc.append(xpcurr).append("[1]");
        }
        if( type != null )
            return type + xpc.toString();
        return xpc.toString();
    }

    /**
     * Get the given XPath with no indices
     *
     * @param XPath XPath with indices
     * @return XPath with indices stripped
     * @throws FxInvalidParameterException for invalid XPath
     */
    public static String toXPathNoMult(String XPath) throws FxInvalidParameterException {
        if (StringUtils.isEmpty(XPath) || "/".equals(XPath))
            return "/";
        XPath = XPath.toUpperCase();
        String type = null;
        if (XPath.charAt(0) != '/' && XPath.indexOf('/') > 0) {
            //we have a full qualified XPath with type name that needs to be stripped temporarily
            type = XPath.substring(0, XPath.indexOf('/'));
            if (type.indexOf('[') > 0)
                type = type.substring(0, type.indexOf('['));
            XPath = XPath.substring(XPath.indexOf('/'));
        }
        if (!isValidXPath(XPath))
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", XPath);
        String[] xp = XPath.substring(1).split("\\/"); //skip first '/' to avoid empty entries
        StringBuffer xpc = new StringBuffer(XPath.length() + 10);
        for (String xpcurr : xp) {
            xpc.append('/');
            if (xpcurr.indexOf('[') > 0)
                xpc.append(xpcurr.substring(0, xpcurr.indexOf('[')));
            else
                xpc.append(xpcurr);
        }
        if (type != null)
            return type + xpc.toString();
        return xpc.toString();
    }


    /**
     * Get the FQ indices of an XPath as an int array
     *
     * @param XPath the xpath to examine
     * @return FQ indices of an XPath as an int array
     * @throws FxInvalidParameterException on errors
     */
    public static int[] getIndices(String XPath) throws FxInvalidParameterException {
        List<XPathElement> xpe = split(XPath);
        int[] mult = new int[xpe.size()];
        for (int i = 0; i < mult.length; i++)
            mult[i] = xpe.get(i).getIndex();
        return mult;
    }

    /**
     * Build an XPath from the given elements
     *
     * @param leadingSlash prepend a leading slash character?
     * @param elements     elements that build the XPath
     * @return XPath
     */
    public static String buildXPath(boolean leadingSlash, String... elements) {
        StringBuffer XPath = new StringBuffer(100);
        for (String element : elements) {
            if (element == null)
                continue;
            XPath.append('/');
            if (element.length() > 0 && element.charAt(0) == '/')
                XPath.append(element.substring(1));
            else
                XPath.append(element);
            if (XPath.length() > 1 && XPath.charAt(XPath.length() - 1) == '/')
                XPath.deleteCharAt(XPath.length() - 1);
        }
        if (XPath.length() > 0) {
            if (XPath.charAt(0) == '/' && !leadingSlash)
                XPath.deleteCharAt(0);
        } else if (XPath.length() == 0 && leadingSlash)
            XPath.append('/');
        return doubleSlashPattern.matcher(XPath).replaceAll("/").toUpperCase();
    }

    /**
     * Strip leading types from an XPath if present
     *
     * @param XPath the XPath
     * @return XPath without leading type
     */
    public static String stripType(String XPath) {
        assert XPath != null : "XPath was null!";
        if (!XPath.startsWith("/"))
            return XPath.substring(XPath.indexOf('/')).toUpperCase();
        return XPath.toUpperCase();
    }

    /**
     * Extract the primary key stored in the given XPath. If no PK is contained
     * in the XPath, a FxRuntimeException is thrown.
     *
     * @param xPath the xpath
     * @return  the primary key stored in the given XPath
     * @throws com.flexive.shared.exceptions.FxRuntimeException   if the given xpath is invalid or contains no PK
     */
    public static FxPK getPK(String xPath) {
        FxSharedUtils.checkParameterEmpty(xPath, "xpath");
        final Matcher matcher = PKPattern.matcher(xPath);
        if (!matcher.find()) {
            throw new FxInvalidParameterException("xpath", "ex.xpath.element.noPk", xPath).asRuntimeException();
        }
        return FxPK.fromString(matcher.group(1));
    }
}
