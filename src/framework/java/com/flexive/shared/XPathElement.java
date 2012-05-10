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
package com.flexive.shared;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single XPath element (alias and multiplicity).
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class XPathElement implements Serializable {
    private static final long serialVersionUID = 2037392183607142045L;
    private static final String PK = "@(pk|PK)=(NEW|\\d*\\.(LIVE|MAX|\\d*))";
    /**
     * First element must start with a "/",
     * an XPath element must start with a letter or underscore followed by an optional letter/number/underscore combination
     * and may end with an optional multiplicity like [x] where x is a number
     */
//    private static final Pattern XPathPattern = Pattern.compile("([A-Z_][A-Z_0-9 _]*(\\[" + PK + "\\])?)?(/[A-Z][A-Z_0-9]*(\\[[0-9]+\\])?)+");
    private static final Pattern PKPattern = Pattern.compile(PK);
    private static final Pattern doubleSlashPattern = Pattern.compile("/{2,}");
    private static final List<XPathElement> EMPTY = Collections.unmodifiableList(new ArrayList<XPathElement>(0));

    private String alias;
    private int index;
    private boolean indexDefined;

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
     * <p>For performance reasons, this method expects that the xPath is already in upper case form.</p>
     *
     * @param XPath the XPath
     * @return XPathElement array
     */
    public static List<XPathElement> split(String XPath) {
        if (StringUtils.isEmpty(XPath))
            return EMPTY;
        if (XPath.charAt(0) != '/' && XPath.indexOf('/') > 0) {
            //we have a full qualified XPath with type name that needs to be stripped
            XPath = XPath.substring(XPath.indexOf('/'));
        }
        if (!isValidXPath(XPath))
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", XPath).asRuntimeException();
        String[] xp = StringUtils.split(XPath.substring(1), '/'); //skip first '/' to avoid empty entries
        List<XPathElement> elements = new ArrayList<XPathElement>(xp.length);
        for (String xpcurr : xp) {
            elements.add(toElement(XPath, xpcurr));
        }
        return elements;
    }

    /**
     * Get the last (rightmost) element of an XPath.
     *
     * <p>For performance reasons, this method expects that the xPath is already in upper case form.</p>
     *
     * @param XPath the XPath
     * @return last (rightmost) element of an XPath
     */
    public static XPathElement lastElement(String XPath) {
        if (StringUtils.isEmpty(XPath) || !isValidXPath(XPath))
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", XPath).asRuntimeException();
        return toElement(XPath, XPath.substring(XPath.lastIndexOf('/') + 1));
    }

    /**
     * Convert an alias of an XPath to an element
     *
     * @param XPath full XPath, only used if exception is thrown
     * @param alias alias to convert to an XPathElement
     * @return XPathElement
     */
    public static XPathElement toElement(String XPath, String alias) {
        if (StringUtils.isEmpty(alias) || alias.indexOf('/') >= 0)
            throw new FxInvalidParameterException("XPATH", "ex.xpath.element.invalid", alias, XPath).asRuntimeException();
        try {
            StringBuilder sbAlias = new StringBuilder(alias.length());
            int index = 0;
            boolean inIdx = false;
            byte mult = 0;
            for (int i = 0; i < alias.length(); i++) {
                char c = alias.charAt(i);
                switch (c) {
                    case '[':
                        inIdx = true;
                        break;
                    case ']':
                        inIdx = false;
                        break;
                    default:
                        if (inIdx) {
                            if (c < '0' || c > '9')
                                continue;
                            if (mult++ > 0)
                                index *= 10;
                            index += c - '0';
                        } else {
                            if (c >= 'a' && c <= 'z')
                                sbAlias.append((char) (c - 32));
                            else
                                sbAlias.append(c);
                        }
                }
            }
            return new XPathElement(sbAlias.toString(), index == 0 ? 1 : index, index > 0);
        } catch (Exception e) {
            throw new FxInvalidParameterException("XPATH", "ex.xpath.element.invalid", alias, XPath).asRuntimeException();
        }
    }

    /**
     * Check if this XPath is valid.
     *
     * <p>For performance reasons, this method expects that the xPath is already in upper case form.</p>
     * 
     * @param XPath the XPath
     * @return valid or not
     */
    public static boolean isValidXPath(String XPath) {
        try {
//            slow version using reqular expressions:
//            return "/".equals(XPath) || !StringUtils.isEmpty(XPath) && XPathPattern.matcher(XPath).matches();
            if (XPath == null)
                return false;
            char[] xp = XPath.toCharArray();
            if (xp.length == 1 && xp[0] == '/')
                return true;
            int pos = -1;
            if (xp[0] != '/') {
                //check for correct type
                boolean inBr = false; //in bracket
                boolean hadBr = false; //already had a bracket
                while (++pos < xp.length) {
                    if (xp[pos] == '/')
                        break;//end of type
                    if (xp[pos] >= '0' && xp[pos] <= '9' && pos > 1 && !inBr) //first letter must not be a number
                        continue;
                    if (((xp[pos] >= 'A' && xp[pos] <= 'Z') || xp[pos] == '_' || xp[pos] == ' ') && !inBr) //only A-Z, underscore and space allowed in name
                        continue;
                    if (xp[pos] == '[') {
                        if (inBr || hadBr || xp[pos + 1] != '@') //in type bracket has to be followed by "@"
                            return false;
                        inBr = true;
                        hadBr = true;
                        continue;
                    }
                    if (xp[pos] == '@') {
                        switch (xp[++pos]) {
                            case 'p':
                            case 'P':
                                break;
                            default:
                                return false;
                        }
                        switch (xp[++pos]) {
                            case 'k':
                            case 'K':
                                break;
                            default:
                                return false;
                        }
                        if (xp[++pos] != '=')
                            return false;
                        //@pk=NEW
                        if (xp[pos + 1] == 'N' && xp[pos + 2] == 'E' && xp[pos + 3] == 'W' && xp[pos + 4] == ']') {
                            pos += 3;
                            continue;
                        }
                        boolean hasNum = false;
                        //@pk=<number>.
                        while (xp[++pos] >= '0' && xp[pos] <= '9') {
                            hasNum = true;
                        }
                        if (!hasNum)
                            return false;
                        if (xp[pos] != '.')
                            return false;
                        if (xp[pos + 1] >= '0' && xp[pos + 1] <= '9') {
                            //@pk=<number>.<number>
                            while (xp[++pos] >= '0' && xp[pos] <= '9') {
                            }
                            --pos; //one back as we reached ']
                            continue;
                        }
                        if (xp[pos + 1] == 'L' && xp[pos + 2] == 'I' && xp[pos + 3] == 'V' && xp[pos + 4] == 'E') {
                            pos += 4;
                            continue;
                        }
                        if (xp[pos + 1] == 'M' && xp[pos + 2] == 'A' && xp[pos + 3] == 'X') {
                            pos += 3;
                            continue;
                        }
                        return false;
                    }
                    if (xp[pos] == ']') {
                        if (!inBr)
                            return false;
                        inBr = false;
                        continue;
                    }
                    return false;
                }
                if (inBr)
                    return false;
                pos--;
            } else //end type check
                pos = -1;
            if ((pos + 1) == xp.length) //empty or name only is not valid
                return false;
            while (++pos < xp.length) {
                if (xp[pos] == '/') { //element start
                    if (pos == xp.length)
                        return false; //may not end with '/'
                    if (!(xp[pos + 1] >= 'A' && xp[pos + 1] <= 'Z'))
                        return false; //element must start with A-Z
                    pos++;
                    while ((xp[pos] >= 'A' && xp[pos] <= 'Z') || (xp[pos] >= '0' && xp[pos] <= '9') || xp[pos] == '_') {
                        if ((pos + 1) == xp.length)
                            return true;
                        pos++;
                    }
                    if (pos == xp.length)
                        return true;
                    if (xp[pos] == '[') { //index is optional and may only exist here
                        boolean hasNum = false;
                        //@pk=<number>.
                        while (xp[++pos] >= '0' && xp[pos] <= '9') {
                            hasNum = true;
                        }
                        if (!hasNum)
                            return false;
                        if (xp[pos] != ']')
                            return false; //index has to end with '['
                    } else
                        pos--;
                } else
                    return false; //expected an element start
            }
            //element check, allowed is only [A-Z] as first letter followed by [A-Z0-9] and an optional index
            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
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
        StringBuilder XPath = new StringBuilder(100);
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
        StringBuilder XPath = new StringBuilder(100);
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
     */
    public static String toXPathMult(String XPath) {
        if (StringUtils.isEmpty(XPath) || "/".equals(XPath))
            return "/";
        XPath = xpToUpperCase(XPath);
        String type = null;
        if (XPath.charAt(0) != '/' && XPath.indexOf('/') > 0) {
            //we have a full qualified XPath with type name that needs to be stripped temporarily
            type = XPath.substring(0, XPath.indexOf('/'));
            XPath = XPath.substring(XPath.indexOf('/'));
        }
        if (!isValidXPath(XPath))
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", XPath).asRuntimeException();
        String[] xp = XPath.substring(1).split("\\/"); //skip first '/' to avoid empty entries
        StringBuilder xpc = new StringBuilder(XPath.length() + 10);
        for (String xpcurr : xp) {
            xpc.append('/');
            if (xpcurr.indexOf('[') > 0)
                xpc.append(xpcurr);
            else
                xpc.append(xpcurr).append("[1]");
        }
        if (type != null)
            return type + xpc.toString();
        return xpc.toString();
    }

    /**
     * Get the given XPath with full multiplicity information.
     * Please note that no checks are performed and that the XPath is assumed to be uppercase and valid
     *
     * @param XPath   valid XPath without type information and without indices
     * @param indices indices to apply (comma separated)
     * @return XPath with full multiplicity information
     */
    public static String toXPathMult(String XPath, String indices) {
        if (XPath == null || "/".equals(XPath) || XPath.length() == 0)
            return "/";
        if (XPath.charAt(0) != '/')
            XPath = XPath.substring(XPath.indexOf('/'));
        String[] ind = indices.split(",");
        StringBuilder xp = new StringBuilder(XPath.length() + ind.length * 3);
        int curr = -1;
        try {
            for (char c : XPath.toCharArray()) {
                if (c == '/') {
                    if (curr >= 0)
                        xp.append('[').append(ind[curr]).append(']');
                    curr++;
                }
                xp.append(c);
            }
            xp.append('[').append(ind[curr]).append(']');
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", XPath).asRuntimeException();
        }
        return xp.toString();
    }

    /**
     * Get the given XPath with no indices
     *
     * @param XPath XPath with indices
     * @return XPath with indices stripped, in upper case
     */
    public static String toXPathNoMult(String XPath) {
        if (StringUtils.isEmpty(XPath) || "/".equals(XPath))
            return "/";
        XPath = xpToUpperCase(XPath);
        if (!isValidXPath(XPath))
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", XPath).asRuntimeException();
        if (XPath.indexOf('[') == -1) {
            // fast path for the case where we don't have to do anything because the XPath does not contain
            // a type parameter (PK) or multiplicities
            return XPath;
        }
        String type = null;
        final int firstSep = XPath.indexOf('/');
        if (XPath.charAt(0) != '/' && firstSep > 0) {
            //we have a full qualified XPath with type name that needs to be stripped temporarily
            type = XPath.substring(0, firstSep);
            final int typeParamSep = type.indexOf('[');
            if (typeParamSep > 0)
                type = type.substring(0, typeParamSep);
            XPath = XPath.substring(firstSep);
        }
        final String[] xp = XPath.substring(1).split("\\/"); //skip first '/' to avoid empty entries
        final StringBuilder xpc = new StringBuilder(XPath.length() + 10);
        for (String xpcurr : xp) {
            xpc.append('/');
            final int multSep = xpcurr.indexOf('[');
            if (multSep > 0)
                xpc.append(xpcurr.substring(0, multSep));
            else
                xpc.append(xpcurr);
        }
        if (type != null)
            return type + xpc.toString();
        return xpc.toString();
    }


    /**
     * Get the FQ indices of an XPath as an int array.
     * This method is optimized for performance and does not check for validity!
     *
     * @param XPath the xpath to examine
     * @return FQ indices of an XPath as an int array
     */
    public static int[] getIndices(String XPath) {
        int[] mult = new int[10];
        byte curr = 0;
        int lastOpen = -1;
        char[] c = XPath.toCharArray();
        for (int i = 0; i < c.length; i++) {
            switch (c[i]) {
                case '/':
                    if (i == 0)
                        break;
                    if (c[i - 1] == ']')
                        break;
                    mult[curr++] = 1;
                    if (mult.length <= curr) {
                        //resize mult
                        int[] tmp = new int[mult.length + 5];
                        System.arraycopy(mult, 0, tmp, 0, mult.length);
                        mult = tmp;
                    }
                    break;
                case '[':
                    lastOpen = i;
                    break;
                case ']':
                    int m = 1;
                    for (int w = (i - 1); w > lastOpen; w--) {
                        mult[curr] += (c[w] - (byte) '0') * m;
                        m *= 10;
                    }
                    curr++;
                    if (mult.length <= curr) {
                        //resize mult
                        int[] tmp = new int[mult.length + 5];
                        System.arraycopy(mult, 0, tmp, 0, mult.length);
                        mult = tmp;
                    }
            }
        }
        if (c[c.length - 1] != ']' && c[c.length - 1] != '/') {
            if (mult.length < curr) {
                //resize mult
                int[] tmp = new int[mult.length + 5];
                System.arraycopy(mult, 0, tmp, 0, mult.length);
                mult = tmp;
            }
            mult[curr++] = 1;
        }
        if (curr == 0)
            return new int[0];
        int start = (c[0] == '/') ? 0 : 1;
        int[] ret = new int[curr - start];
        System.arraycopy(mult, start, ret, 0, curr - start);
        return ret;
    }

    /**
     * Build an XPath from the given elements
     *
     * @param leadingSlash prepend a leading slash character?
     * @param elements     elements that build the XPath
     * @return XPath
     */
    public static String buildXPath(boolean leadingSlash, String... elements) {
        StringBuilder XPath = new StringBuilder(100);
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
        return xpToUpperCase(doubleSlashPattern.matcher(XPath).replaceAll("/"));
    }

    /**
     * Strip leading types from an XPath if present, and return
     * the XPath in upper case.
     *
     * @param XPath the XPath
     * @return XPath without leading type
     */
    public static String stripType(String XPath) {
        assert XPath != null : "XPath was null!";
        final int pos = XPath.indexOf('/');
        return pos != -1 ? xpToUpperCase(XPath.substring(pos)) : "";
    }

    /**
     * Strip the last element (usually property) from an XPath
     *
     * @param XPath the XPath
     * @return XPath without the last element
     */
    public static String stripLastElement(String XPath) {
        if (XPath == null)
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", "null").asRuntimeException();
        if (XPath.lastIndexOf('/') == 0)
            return "/";
        XPath = xpToUpperCase(XPath);
        if (!isValidXPath(XPath))
            throw new FxInvalidParameterException("XPATH", "ex.xpath.invalid", XPath).asRuntimeException();
        return XPath.substring(0, XPath.lastIndexOf('/'));
    }

    /**
     * Extract the primary key stored in the given XPath. If no PK is contained
     * in the XPath, a FxRuntimeException is thrown.
     *
     * @param xPath the xpath
     * @return the primary key stored in the given XPath
     * @throws com.flexive.shared.exceptions.FxRuntimeException
     *          if the given xpath is invalid or contains no PK
     */
    public static FxPK getPK(String xPath) {
        FxSharedUtils.checkParameterEmpty(xPath, "xpath");
        final Matcher matcher = PKPattern.matcher(xPath);
        if (!matcher.find()) {
            //noinspection ThrowableInstanceNeverThrown
            throw new FxInvalidParameterException("xpath", "ex.xpath.element.noPk", xPath).asRuntimeException();
        }
        return FxPK.fromString(matcher.group(2));
    }

    /**
     * Change the index of an xpath (requires an xpath with all explicit indices set!)
     * This method is optimized for performance and does not check for validity!
     *
     * @param XPath the xpath with all indices set
     * @param pos   position of the element (0-based)
     * @param index the new index to apply
     * @return xpath with the new index at the requested position
     * @since 3.1.5
     */
    public static String changeIndex(String XPath, int pos, int index) {
        byte curr = 0;
        int lastOpen = -1;
        StringBuilder res = new StringBuilder(XPath);
        for (int i = 0; i < res.length(); i++) {
            switch (res.charAt(i)) {
                case '[':
                    lastOpen = i;
                    break;
                case ']':
                    if (curr++ != pos)
                        break;
                    res.replace(lastOpen + 1, i, String.valueOf(index));
                    return res.toString();
            }
        }
        return XPath; //not found, return original
    }
    
    /**
     * Optimized uppercase method for XPaths (characters are limited to a-z).
     * 
     * @param xpath the XPath
     * @return      the uppercased XPath
     * @since 3.1.7
     */
    public static String xpToUpperCase(String xpath) {
        StringBuilder out = null;
        final int len = xpath.length();
        for (int i = 0; i < len; i++) {
            final char ch = xpath.charAt(i);
            if (ch >= 'a' && ch <= 'z') {
                if (out == null) {
                    out = new StringBuilder(len);
                    out.append(xpath.substring(0, i));
                }
                out.append((char) (ch - 32));
            } else if (out != null) {
                out.append(ch);
            }
        }
        return out != null ? out.toString() : xpath;
    }
}
