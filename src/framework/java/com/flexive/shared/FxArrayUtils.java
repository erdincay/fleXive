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
     * Convert a Long list to a primitive long array
     *
     * @param groups List<Long>
     * @return long[]
     */
    public static long[] toLongArray(List<Long> groups) {
        long[] res = new long[groups.size()];
        for (int i = 0; i < res.length; i++)
            res[i] = groups.get(i);
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

    public static long[] toPrimitiveLongArray(Long[] array) {
        long[] res = new long[array.length];
        for(int i=0;i<array.length;i++)
            res[i] = array[i];
        return res;
    }
}

