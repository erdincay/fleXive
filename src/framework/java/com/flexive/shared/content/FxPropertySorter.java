/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.shared.content;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.structure.FxGroupAssignment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;

import java.util.*;


/**
 * Get the order of a GroupData or a tpyeID (recursive) and can apply it to other GroupData
 *
 * @author Laszlo Hernadi lhernadi@ucs.at
 * @since 3.1.1
 */
public class FxPropertySorter {
    // A table with the group as first key and xpath (complete) as second key and position as value
    private Map<String, Map<String, Integer>> currentOrder;

    private final static String ROOT_NAME = "ROOT";
    private final static String NEXT_NAME = "next";
    private final static String DIRTY_NAME = "dirty";
    private final int INIT_LEN;
    private boolean isDirty = false;

    private FxPropertySorter(int init_len) {
        INIT_LEN = init_len;
        this.currentOrder = new HashMap<String, Map<String, Integer>>();
        this.currentOrder.put(ROOT_NAME, new HashMap<String, Integer>());
    }

    private String normalizeXPath(String xPath) {
        return xPath.substring(INIT_LEN);
    }

    /**
     * Initiates the sorter by a given type id
     *
     * @param typeId The type id where to get the order from
     * @return The created PropertySorter, ready to sort the properties of the type
     */
    public static FxPropertySorter getSorterForType(long typeId) {
        final FxType currentType = CacheAdmin.getEnvironment().getType(typeId);
        FxPropertySorter currSorter = new FxPropertySorter(currentType.getName().length());
        Map<String, Integer> currProps = currSorter.currentOrder.get(ROOT_NAME);

        // First add all properties (not groups) to the root-list with its positions
        for (FxPropertyAssignment currAss : currentType.getAssignedProperties()) {
            if (!currAss.isSystemInternal()) {
                // we only need to know the XPath from the current element not the element it self
                // so every entry begins with a /
                currProps.put(currSorter.normalizeXPath(currAss.getXPath()), currAss.getPosition());
            }
        }

        // Then add all groups with their positions to the root-list and find the properties and groups of these groups
        for (FxGroupAssignment currGroup : currentType.getAssignedGroups()) {
            Map<String, Integer> tmpGroup = new HashMap<String, Integer>();
            String key = currSorter.normalizeXPath(currGroup.getXPath());
            currProps.put(key, currGroup.getPosition());
            currSorter.currentOrder.put(key, tmpGroup);
            currSorter.addGroups(currGroup, tmpGroup);
        }

        return currSorter;
    }

    /**
     * Recursively add the groups
     *
     * @param currGroup The group of which all properties and groups should be added
     * @param currProps The map in which the current group should be "saved"
     */
    private void addGroups(FxGroupAssignment currGroup, Map<String, Integer> currProps) {
        // Again first add all the properties
        for (FxPropertyAssignment currAss : currGroup.getAssignedProperties()) {
            if (!currAss.isSystemInternal()) {
                currProps.put(normalizeXPath(currAss.getXPath()), currAss.getPosition());
            }
        }

        // Then add all groups, and their groups...
        for (FxGroupAssignment currGroupAss : currGroup.getAssignedGroups()) {
            Map<String, Integer> tmpGroup = new HashMap<String, Integer>();
            String key = normalizeXPath(currGroupAss.getXPath());
            currProps.put(key, currGroupAss.getPosition());
            currentOrder.put(key, tmpGroup);
            addGroups(currGroupAss, tmpGroup);
        }
    }

    /**
     * Recursively add elements of a group into the right map
     *
     * @param currentGroupName The name of the current group
     * @param data             The list of FxData representing the group
     */
    private void addSubElements(final String currentGroupName, List<FxData> data) {
        int currentPosition = 0;
        Map<String, Integer> currentTable = currentOrder.get(currentGroupName);
        // add all non system internal properties and groups (only once) to the map
        for (FxData currData : data) {
            if (!currData.isSystemInternal()) {
                if (currData.isGroup()) {
                    // if there is a group first we need to get a "clean" XPath (without any indexes ([1])
                    String tmpXPath = currData.getXPath();
                    // then check if we already "know" that group and if not, then get it known
                    if (currentOrder.get(tmpXPath) == null) {
                        currentOrder.put(tmpXPath, new HashMap<String, Integer>());
                        addSubElements(tmpXPath, ((FxGroupData) currData).getChildren());
                    }
                }
                String tmpXPath = currData.getXPath();
                // befor we add the current property (or group) to the current list, we check if it is already there,
                // since we only need one position of a property / group
                if (currentTable.get(tmpXPath) == null)
                    currentTable.put(tmpXPath, currentPosition++);
            }
        }
        currentTable.put(NEXT_NAME, currentPosition);
    }

    /**
     * Apply the saved order to the current group data
     *
     * @param groupData The group data to apply the order to
     */
    public void applyOrder(FxGroupData groupData) {
        // Just start the recursion and check if the isDirty flag is set (indicates that there was unknown elements in the group)
        applyOrder(groupData, ROOT_NAME);
        if (isDirty) {
            // If there was any unknown element in the data, we take this order of properties and groups for future sorts
            currentOrder.clear();
            currentOrder.put(ROOT_NAME, new HashMap<String, Integer>());
            addSubElements(ROOT_NAME, groupData.getChildren());
        }
        isDirty = false;
    }

    /**
     * The recursion to apply the order
     *
     * @param groupData The current groupData
     * @param path      The path of the current data
     */
    private void applyOrder(FxGroupData groupData, String path) {
        List<FxData> nonInternalProperties = new ArrayList<FxData>(groupData.getChildrenWithoutInternal());
        List<FxData> allChildren = groupData.getChildren();

        FxData[] sortedData = new FxData[allChildren.size()];
        sortedData = allChildren.toArray(sortedData);

        // We need to know how many System internals are in the current list, so that we could skip them
        final int numSysInternals = allChildren.size() - nonInternalProperties.size();
        // To do the actual sorting, we use the Arrays.sort with an own Comparator and tell them to start at
        // the position of the first non-systemInternal property
        Arrays.sort(sortedData, numSysInternals, allChildren.size(), new FxDataComparator(currentOrder.get(path)));

        // If the diry value is set in the map (can only set by the comparator if there is any unknown value)
        // we set the isDirty flag
        isDirty |= currentOrder.get(path).containsKey(DIRTY_NAME);

        // Finally we remove all the non system internal properties and add them in the orderd order and set the position right
        allChildren.removeAll(nonInternalProperties);
        //noinspection ForLoopReplaceableByForEach
        for (int i = numSysInternals; i < sortedData.length; i++) {
            FxData currData = sortedData[i];
            currData.setPos(i);
            allChildren.add(currData);
            if (currData.isGroup())
                applyOrder(((FxGroupData) currData), currData.getXPath());
        }
    }

    /**
     * The comparator for the Arrays.sort
     */
    private static class FxDataComparator implements Comparator<FxData> {

        // The list according to which to order
        private final Map<String, Integer> currentOrder;

        public FxDataComparator(Map<String, Integer> currentOrder) {
            this.currentOrder = currentOrder;
        }

        /**
         * Compare 2 FxData according to the order in the Map
         * Every data that is missing in the map is sorted to the end of the List
         *
         * @param data1 The one data element
         * @param data2 The other data element
         * @return The result of the two positions compared
         */
        public int compare(FxData data1, FxData data2) {
            Integer i1, i2;
            i1 = currentOrder.get(data1.getXPath());
            i2 = currentOrder.get(data2.getXPath());
            if (i1 == null) {
                i1 = Integer.MAX_VALUE;
                currentOrder.put(DIRTY_NAME, i1);
            }
            if (i2 == null) {
                i2 = Integer.MAX_VALUE;
                currentOrder.put(DIRTY_NAME, i2);
            }
            return i1.compareTo(i2);
        }
    }
}
