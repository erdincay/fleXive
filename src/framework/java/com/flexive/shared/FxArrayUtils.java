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

import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * Utility functions for arrays.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxArrayUtils {

    /**
     * Converts a list with integer items separated by a specific delimeter to a array.
     * <p/>
     * A empty array will be returned if the list is null or a empty string.
     *
     * @param list      the list
     * @param separator the separator character
     * @return the array
     * @throws FxInvalidParameterException if the list can not be converted, the exception's getParameterName()
     *                                     function will return the token that caused the exception
     */
    public static int[] toIntArray(String list, char separator) throws FxInvalidParameterException {
        if (list == null || list.length() == 0) return new int[0];
        String sInts[] = StringUtils.split(list, separator);
        int iInts[] = new int[sInts.length];
        for (int i = 0; i < sInts.length; i++) {
            try {
                iInts[i] = Integer.parseInt(sInts[i]);
            } catch (Exception exc) {
                throw new FxInvalidParameterException("'" + list + "' can not be converted to a int[] array using separator '" +
                        separator + "'", sInts[i]);
            }
        }
        return iInts;
    }

    /**
     * Converts a list with long items separated by a specific delimeter to a array.
     * <p/>
     * A empty array will be returned if the list is null or a empty string.
     *
     * @param list      the list
     * @param separator the separator character
     * @return the array
     * @throws FxInvalidParameterException if the list can not be converted, the exception's getParameterName()
     *                                     function will return the token that caused the exception
     */
    public static long[] toLongArray(String list, char separator) throws FxInvalidParameterException {
        if (list == null || list.length() == 0) return new long[0];
        String sInts[] = StringUtils.split(list, separator);
        long iInts[] = new long[sInts.length];
        for (int i = 0; i < sInts.length; i++) {
            try {
                iInts[i] = new Long(sInts[i]).intValue();
            } catch (Exception exc) {
                throw new FxInvalidParameterException("'" + list + "' can not be converted to a long[] array using separator '" +
                        separator + "'", sInts[i]);
            }
        }
        return iInts;
    }

    /**
     * Convert a Long list to a Long array
     *
     * @param items List<Long>
     * @return Long[]
     */
    public static Long[] toLongArray(List<Long> items) {
        Long[] res = new Long[items.size()];
        for (int i = 0; i < res.length; i++)
            res[i] = items.get(i);
        return res;
    }

    /**
     * Convert a Long list to a long array
     *
     * @param items List<Long>
     * @return long[]
     * @since 3.1
     */
    public static long[] toPrimitiveLongArray(List<Long> items) {
        final long[] res = new long[items.size()];
        for (int i = 0; i < res.length; i++)
            res[i] = items.get(i);
        return res;
    }

    /**
     * Removes dupicated entries from the list.
     *
     * @param list the list
     * @return the list without any duplicated entries
     */
    public static int[] removeDuplicates(int[] list) {
        if (list == null || list.length == 0) {
            return new int[0];
        }
        Hashtable<Integer, Boolean> tbl = new Hashtable<Integer, Boolean>(list.length);
        for (int ele : list) {
            tbl.put(ele, Boolean.FALSE);
        }
        int[] result = new int[tbl.size()];
        int pos = 0;
        for (Enumeration e = tbl.keys(); e.hasMoreElements();) {
            result[pos++] = (Integer) e.nextElement();
        }
        return result;
    }

    /**
     * Removes dupicated entries from the list.
     *
     * @param list the list
     * @return the list without any duplicated entries
     */
    public static long[] removeDuplicates(long[] list) {
        if (list == null || list.length == 0) {
            return new long[0];
        }
        Hashtable<Long, Boolean> tbl = new Hashtable<Long, Boolean>(list.length);
        for (long ele : list) {
            tbl.put(ele, Boolean.FALSE);
        }
        long[] result = new long[tbl.size()];
        int pos = 0;
        for (long element : Collections.list(tbl.keys())) {
            result[pos++] = element;
        }
        return result;
    }

    /**
     * Adds a element to the end of the array, if it is not already contained.
     *
     * @param list    original list
     * @param element the element to add
     * @return the new list
     */
    public static int[] addElement(int[] list, int element) {
        // Avoid null pointer exception
        if (list == null) list = new int[0];

        // Determine if the element is already part of the list.
        // If so do nothing
        for (int aList : list)
            if (aList == element) return list;

        // Add the element to the end of the list
        int[] result = new int[list.length + 1];
        System.arraycopy(list, 0, result, 0, list.length);
        result[result.length - 1] = element;
        return result;
    }

    /**
     * Adds a element to the end of the array, if it is not already contained.
     *
     * @param list    original list
     * @param element the element to add
     * @return the new list
     */
    public static long[] addElement(long[] list, long element) {
        // Avoid null pointer exception
        if (list == null) list = new long[0];

        // Determine if the element is already part of the list.
        // If so do nothing
        for (long aList : list)
            if (aList == element) return list;

        // Add the element to the end of the list
        long[] result = new long[list.length + 1];
        System.arraycopy(list, 0, result, 0, list.length);
        result[result.length - 1] = element;
        return result;
    }

    /**
     * Adds a element to the end of the array.
     *
     * @param list    original list
     * @param element the element to add
     * @param unique  true if the element should only be added if it is not already contained
     * @return the new list
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] addElement(T[] list, T element, boolean unique) {
        // Avoid null pointer exception
        if (list == null) {
            if (element == null) {
                return null;
            }
            list = (T[]) Array.newInstance(element.getClass(), 1);
            list[0] = element;
            return list;
        }

        if (unique) {
            // Determine if the element is already part of the list.
            // If so do nothing
            for (T aList : list)
                if (aList == element) return list;
        }

        // Add the element to the end of the list
        T[] result = (T[]) Array.newInstance(element.getClass(), list.length + 1);
        System.arraycopy(list, 0, result, 0, list.length);
        result[result.length - 1] = element;
        return result;
    }

    /**
     * Return true if the list contains the given element.
     *
     * @param list       the list
     * @param element    the element to look for
     * @param ignoreCase if set to false the compare is done case sensitive
     * @return true if the list contains the given element
     */
    public static boolean containsElement(String[] list, String element, boolean ignoreCase) {
        if (list == null) return false;
        for (String aList : list) {
            if (aList == null && element == null) return true;
            if (aList == null) return false;
            if (element == null) return false;
            if (ignoreCase) {
                if (aList.equalsIgnoreCase(element)) return true;
            } else {
                if (aList.equals(element)) return true;
            }
        }
        return false;
    }

    /**
     * Returns the first occurence of the given element.
     *
     * @param list       the list
     * @param element    the element to look for
     * @param ignoreCase if set to false the compare is done case sensitive
     * @return true the first position, or -1 if the element was not found
     */
    public static int indexOf(String[] list, String element, boolean ignoreCase) {
        if (list == null) return -1;
        int pos = 0;
        for (String aList : list) {
            if (aList == null && element == null) return pos;
            if (aList == null) return -1;
            if (element == null) return -1;
            if (ignoreCase) {
                if (aList.equalsIgnoreCase(element)) return pos;
            } else {
                if (aList.equals(element)) return pos;
            }
            pos++;
        }
        return -1;
    }

    /**
     * Convert an object array to a string array with the given separator
     *
     * @param elements  elements to convert to a String
     * @param separator separator for the resulting String
     * @return String representation of the array
     */
    public static String toStringArray(Object[] elements, char separator) {
        StringBuilder sb = new StringBuilder((elements.length + 20));
        for (Object element : elements) {
            sb.append(String.valueOf(element));
            sb.append(separator);
        }
        return sb.substring(0, sb.length() - 1);
    }

    /**
     * Convert an object array to a string array with the given separator String
     *
     * @param elements  elements to convert to a String
     * @param separator separator for the resulting String
     * @return String representation of the array
     */
    public static String toStringArray(Object[] elements, String separator) {
        StringBuilder sb = new StringBuilder((elements.length + separator.length() * 20));
        for (Object element : elements) {
            sb.append(String.valueOf(element));
            sb.append(separator);
        }
        return sb.substring(0, sb.length() - separator.length());
    }

    /**
     * Replace an array element
     *
     * @param array     array containing elements
     * @param separator separator
     * @param index     index of the element to replace
     * @param newValue  replacement value
     * @return array with the replaced value
     */
    public static String replaceElement(String array, char separator, int index, String newValue) {
        StringBuilder sb = new StringBuilder(array.length() + newValue.length());
        int cur = 0;
        boolean inrep = index == 0;
        if (index == 0)
            sb.append(newValue);
        char[] charArray = array.toCharArray();
        for (int i = 0, charArrayLength = charArray.length; i < charArrayLength; i++) {
            char c = charArray[i];
            if (c == separator) {
                sb.append(c);
                cur++;
                if (cur == index) {
                    sb.append(newValue);
                    inrep = !(charArrayLength > (i + 1) && charArray[i + 1] == separator);
                } else
                    inrep = false;
            } else if (!inrep)
                sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Create an empty array string containing only separators
     *
     * @param length    desired array length
     * @param separator separator to use
     * @return String containing <code>length-1</code> separators
     */
    public static String createEmptyStringArray(int length, char separator) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length - 1; i++) {
            sb.append(separator);
        }
        return sb.toString();
    }


    /**
     * Get an integer element from a string array
     *
     * @param array     the string array
     * @param separator seperator character
     * @param index     index to get the element at
     * @return element  or <code>Integer.MIN_VALUE</code> if not set
     */
    public static int getIntElementAt(String array, char separator, int index) {
        try {
            if (index == 0)
                return Integer.valueOf(array.substring(0, array.indexOf(separator)));
            int curr = 0;
            int start = 0;
            for (char c : array.toCharArray()) {
                if (c == separator)
                    curr++;
                if (curr == index) {
                    int end = array.substring(start + 1).indexOf(',');
                    if (end == -1)
                        return Integer.valueOf(array.substring(start + 1));
                    return Integer.valueOf(array.substring(start + 1, start + end + 1));
                }
                start++;
            }
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
        throw new ArrayIndexOutOfBoundsException("Index " + index + " is out of bounds for [" + array + "]");
    }

    /**
     * Get an integer element from a string array containing hexadecimal values
     *
     * @param array     the string array containing hexadecimal values
     * @param separator seperator character
     * @param index     index to get the element at
     * @return element  or <code>null</code> if not set
     */
    public static Integer getHexIntElementAt(String array, char separator, int index) {
        try {
            if (StringUtils.isBlank(array))
                return null;
            if (index == 0)
                return evalIntValue(array.substring(0, array.indexOf(separator)), 16);
            int curr = 0;
            int start = 0;
            for (char c : array.toCharArray()) {
                if (c == separator)
                    curr++;
                if (curr == index) {
                    int end = array.substring(start + 1).indexOf(',');
                    if (end == -1)
                        return evalIntValue(array.substring(start + 1), 16);
                    return evalIntValue(array.substring(start + 1, start + end + 1), 16);
                }
                start++;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        throw new ArrayIndexOutOfBoundsException("Index " + index + " is out of bounds for [" + array + "]");
    }

    /**
     * Evaluate an integer value, returning <code>null</code> if not set
     *
     * @param value the integer value to set
     * @param radix radix to use
     * @return integer value or <code>null</code> if not set
     */
    private static Integer evalIntValue(String value, int radix) {
        if (StringUtils.isBlank(value))
            return null;
        return Integer.parseInt(value, radix);
    }
}

