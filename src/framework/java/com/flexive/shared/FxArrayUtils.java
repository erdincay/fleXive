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
package com.flexive.shared;

import com.flexive.shared.exceptions.FxInvalidParameterException;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Utility functions for arrays.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxArrayUtils {

    /**
     * Converts a SelectableObjectWithLabel array to a string list containing the id's.
     *
     * @param values    the array
     * @param separator the separator to use between the id's
     * @return the array as string list
     */
    public static String toSeparatedList(SelectableObjectWithLabel[] values, char separator) {
        if (values == null) return "";
        StringBuilder res = new StringBuilder(values.length * 5);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) res.append(separator);
            res.append(values[i].getId());
        }
        return res.toString();
    }

    /**
     * Converts a int array to a string list.
     *
     * @param values    the array
     * @param separator the separator to use between the numbers
     * @return the array as string list
     */
    public static String toSeparatedList(int[] values, char separator) {
        if (values == null) return "";
        StringBuilder res = new StringBuilder(values.length * 5);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) res.append(separator);
            res.append(values[i]);
        }
        return res.toString();
    }

    /**
     * Converts a byte array to a string list.
     *
     * @param values    the array
     * @param separator the separator to use between the numbers
     * @return the array as string list
     */
    public static String toSeparatedList(byte[] values, char separator) {
        if (values == null) return "";
        StringBuilder res = new StringBuilder(values.length * 5);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) res.append(separator);
            res.append(values[i]);
        }
        return res.toString();
    }


    /**
     * Converts a long array to a string list.
     *
     * @param values    the array
     * @param separator the separator to use between the numbers
     * @return the array as string list
     */
    public static String toSeparatedList(long[] values, char separator) {
        if (values == null) return "";
        StringBuilder res = new StringBuilder(values.length * 5);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) res.append(separator);
            res.append(values[i]);
        }
        return res.toString();
    }

    /**
     * Join a list of elements using the given delimiter.
     *
     * @param elements Elements to be joined
     * @param delim    Delimiter between elements
     * @return Joined string
     */
    public static String toSeparatedList(List elements, String delim) {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for(Object s:elements) {
            out.append(((first ? "" : delim) + s));
            first = false;
        }
        return out.toString();
    }

    /**
     * Join an array of elements using the given delimiter.
     *
     * @param elements Elements to be joined
     * @param delim    Delimiter between elements
     * @return Joined string
     */
    public static String toSeparatedList(String[] elements, String delim) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < elements.length; i++)
            out.append(((i > 0 ? delim : "") + elements[i]));
        return out.toString();
    }

    /**
     * Converts a list with items separated by a specific delimeter to a array.
     * <p/>
     * A empty array will be returned if the list is null or a empty string.
     *
     * @param list      the list
     * @param separator the separator character
     * @return the array
     */
    public static String[] toArray(String list, char separator) {
        if (list == null || list.length() == 0) return new String[0];
        StringTokenizer st = new StringTokenizer(list, ("" + separator), false);
        ArrayList<String>al = new ArrayList<String>(100);
        while (st.hasMoreTokens()) {
            al.add(st.nextToken());
        }
        return al.toArray(new String[al.size()]);
    }


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
        String sInts[] = toArray(list, separator);
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

    public static int[] toIntArray(Integer list[]) {
        if (list==null || list.length==0) {
            return new int[0];
        }
        int result[] = new int[list.length];
        int pos=0;
        for (Integer ele:list) {
            result[pos++]=ele;
        }
        return result;
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
        String sInts[] = toArray(list, separator);
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
     * Removes the given element from the list.
     *
     * @param list    the list the element should be removed from
     * @param element the element to remove
     * @return the list without the given element
     */
    public static int[] removeElementFromList(int[] list, final int element) {

        if (list != null) {
            int elementFound = 0;
            while (elementFound != -1) {
                // Look for the elemenet
                elementFound = -1;
                for (int i = 0; i < list.length; i++) {
                    if (list[i] == element) {
                        elementFound = i;
                        break;
                    }
                }
                // Delete the element
                if (elementFound != -1) {
                    int tmp[] = new int[list.length - 1];
                    int pos = 0;
                    for (int i = 0; i < list.length; i++) {
                        if (i != elementFound) tmp[pos++] = list[i];
                    }
                    list = tmp;
                }
            }
        }
        return list;

    }

    /**
     * Removes the given elements from the list.
     *
     * @param list     the list the element should be removed from
     * @param elements the elements to remove
     * @return the list without the given element
     */
    public static int[] removeElementsFromList(int[] list, final int elements[]) {
        for (int ele:elements) {
            list = removeElementFromList(list, ele);
        }
        return list;
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
        Hashtable<Integer, Boolean>tbl = new Hashtable<Integer, Boolean>(list.length);
        for (int ele:list) {
            tbl.put(ele, Boolean.FALSE);
        }
        int[] result = new int[tbl.size()];
        int pos = 0;
        for (Enumeration e = tbl.keys(); e.hasMoreElements();) {
            result[pos++] = (Integer)e.nextElement();
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
        Hashtable<Long, Boolean>tbl = new Hashtable<Long, Boolean>(list.length);
        for (long ele:list) {
            tbl.put(ele, Boolean.FALSE);
        }
        long[] result = new long[tbl.size()];
        int pos = 0;
        for (long element: Collections.list(tbl.keys())) {
            result[pos++] = element;
        }
        return result;
    }

    /**
     * Adds a element to the end of the array, if it is not already contained.
     *
     * @param list original list
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
     * @param list original list
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
     * @param list original list
     * @param element the element to add
     * @param unique    true if the element should only be added if it is not already contained
     * @return the new list
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] addElement(T[] list, T element, boolean unique) {
        // Avoid null pointer exception
        if (list == null) {
            if( element == null ) {
                return null;
            }
            list = (T[])Array.newInstance(element.getClass(), 1);
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
        T[] result = (T[])Array.newInstance(element.getClass(), list.length + 1);
        System.arraycopy(list, 0, result, 0, list.length);
        result[result.length - 1] = element;
        return result;
    }

    /**
     * Adds a element to the end of the array.
     *
     * @param list original list
     * @param element the element to add
     * @return the new list
     */
    public static <T> T[] addElement(T[] list, T element) {
        return addElement(list, element, false);
    }

    /**
     * Adds every element in a list to the end of the array, if it is not already contained.
     *
     * @param elements the elements to add
     * @param list the list to add the element to
     * @return the new list
     */
    public static int[] addElements(int[] list, int[] elements) {
        if (elements == null) return list;
        for (int element : elements)
            list = addElement(list, element);
        return list;
    }

    /**
     * Return true if the list contains the given element.
     *
     * @param list    the list
     * @param element the element to look for
     * @return true if the list contains the given element
     */
    public static boolean containsElement(int[] list, int element) {
        if (list == null) return false;
        for (int aList : list)
            if (aList == element) return true;
        return false;
    }

    /**
     * Return true if the list contains the given element.
     *
     * @param list    the list
     * @param element the element to look for
     * @return true if the list contains the given element
     */
    public static boolean containsElement(SelectableObject[] list, SelectableObject element) {
        if (list == null) return false;
        for (SelectableObject aList : list)
            if (aList.getId() == element.getId()) return true;
        return false;
    }

    /**
     * Return true if the list contains the given element.
     *
     * @param list    the list
     * @param element the element to look for
     * @return true if the list contains the given element
     */
    public static boolean containsElement(byte[] list, byte element) {
        if (list == null) return false;
        for (byte aList : list)
            if (aList == element) return true;
        return false;
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
        int pos =0;
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
     * Return true if the list contains the given element.
     *
     * @param list    the list
     * @param element the element to look for
     * @return true if the list contains the given element
     */
    public static boolean containsElement(long[] list, long element) {
        for (long aList : list)
            if (aList == element) return true;
        return false;
    }

    /**
     * Generic shallow array clone function (until commons ArrayUtils is generified)
     *
     * @param <T>   array type
     * @param array the array to be cloned (shallow copy)
     * @return  the cloned array (shallow copy)
     */
    public static <T> T[] clone(T[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }
}

