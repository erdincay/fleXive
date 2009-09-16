/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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

package com.flexive.shared.structure.export;

import com.flexive.shared.structure.*;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.scripting.FxScriptEvent;

import java.util.*;

import static org.apache.commons.lang.ArrayUtils.toObject;

/**
 * Tool methods to create Groovy Script code from given structures
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com) UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public final class StructureExporterTools {

    /**
     * Enumeration of properties which will be ignored
     */
    protected enum IgnoreProps {
        ID, VERSION, TYPEDEF, MANDATOR, ACL, STEP, MAX_VER, LIVE_VER, ISMAX_VER, ISLIVE_VER,
        ISACTIVE, MAINLANG, RELSRC, RELDST, RELPOS_SRC, RELPOS_DST, CREATED_BY, CREATED_AT,
        MODIFIED_BY, MODIFIED_AT
    }

    // a var for "simple" datatypes and their instantiations
    public static final Map<String, String> DATATYPESSIMPLE = new HashMap<String, String>();
    public static final List<String> DATATYPES = Arrays.asList("SelectOne", "SelectMany", "Date", "DateTime", "DateRange",
            "DateTimeRange", "Reference", "InlineReference", "Binary");

    static {
        for (FxDataType d : FxDataType.values()) {
            if (!DATATYPES.contains(d.name()))
                DATATYPESSIMPLE.put(d.name(), "new " + d.getValueClass().getSimpleName());
        }
    }

    private StructureExporterTools() { // p.i.
    }

    /**
     * This method returns all derived assignments (groups and properties) for a given type
     * (all assignments in groups and subgroups as well).
     *
     * @param type the FxType
     * @return returns a list of all derived assignments for the type
     */
    public static List<FxAssignment> getAllDerivedAssignmentsForType(FxType type) {
        List<FxAssignment> allDerivedAssignmentsForType = new ArrayList<FxAssignment>();
        List<FxPropertyAssignment> props = type.getAssignedProperties();
        List<FxGroupAssignment> groups = type.getAssignedGroups();
        // properties directly assigned to the type
        if (props.size() > 0) {
            for (FxPropertyAssignment p : props) {
                if (p.isDerivedAssignment() && !isSystemProperty(p.getXPath())) // add if derived
                    allDerivedAssignmentsForType.add(p);
            }
        }
        // groups and properties
        if (groups.size() > 0) {
            for (FxGroupAssignment g : groups) {
                if (g.isDerivedAssignment()) // add if derived
                    allDerivedAssignmentsForType.add(g);
                // then search recursively through the group
                allDerivedAssignmentsForType.addAll(recursiveDerivedAssignmentSearch(g));
            }
        }

        return allDerivedAssignmentsForType;
    }

    /**
     * Helper method - a recursive search of all child assignments of a group,
     * returns all derived assignments
     *
     * @param a a given FxGroupAssignment
     * @return a list of FxAssignments containing only derived child assignments
     */
    public static List<FxAssignment> recursiveDerivedAssignmentSearch(FxGroupAssignment a) {
        List<FxAssignment> l = new ArrayList<FxAssignment>();
        for (FxAssignment ass : a.getAssignments()) {
            if (ass instanceof FxPropertyAssignment && ass.isDerivedAssignment()) {
                l.add(ass);
            } else if (ass instanceof FxGroupAssignment) { // && ass.isDerivedAssignment()) {
                if (ass.isDerivedAssignment())
                    l.add(ass);

                l.addAll(recursiveDerivedAssignmentSearch((FxGroupAssignment) ass));
            }
        }
        return l;
    }

    /**
     * This method returns a List<FxAssignment> of all unique (= not derived from another property or group)
     * assignments for a given group.
     *
     * @param g an FxGroupAssignment
     * @return returns a List<FxAssignment> of unique properties / groups or NULL if no such assignments were found.
     */
    public static List<FxAssignment> getAllUniqueAssignmentsForGroup(FxGroupAssignment g) {
        final List<FxAssignment> out = new ArrayList<FxAssignment>();
        for (FxAssignment a : g.getAllChildAssignments()) {
            if (!a.isDerivedAssignment()) {
                out.add(a);
            }
        }

        if (out.size() > 0)
            return out;
        else
            return null;
    }

    /**
     * Returns all parents for a given assignment, or an empty list if none are found.
     *
     * @param a a given FxAssignment
     * @return returns a List of parent (group) FxAssignments
     */
    public static List<FxAssignment> recursiveParentGroupAssignmentSearch(FxAssignment a) {
        List<FxAssignment> l = new ArrayList<FxAssignment>();
        if (a.hasParentGroupAssignment()) {
            final FxAssignment parent = a.getParentGroupAssignment();
            l.add(parent);
            l.addAll(recursiveParentGroupAssignmentSearch(parent));
        }

        return l;
    }

    /**
     * Returns a Set of base FxType IDs for a List of FxAssignments
     * (Set s.t. no duplicates will be returned)
     *
     * @param l a list of FxAssignments
     * @return returns a Set of typeIds
     */
    public static Set<Long> getDerivedAssTypeIds(List<FxAssignment> l) {
        final Set<Long> typeIds = new HashSet<Long>();
        if (l.size() > 0) {
            for (FxAssignment a : l) {
                final long id = getBaseTypeId(a);
                typeIds.add(id);
            }
        }
        return typeIds;
    }

    /**
     * Returns a Map of base FxType IDs for a List of FxAssignments
     * The map keys are the type Ids, the values are Lists of assignment ids
     *
     * @param l a list of FxAssignments
     * @return returns the Map of typeids vs. assignmentIds
     */
    public static Map<Long, List<Long>> getBaseTypeIds(List<FxAssignment> l) {
        final Map<Long, List<Long>> typeIds = new HashMap<Long, List<Long>>();
        if (l.size() > 0) {
            for (FxAssignment a : l) {
                final long id = getBaseTypeId(a);
//                final long id = a.getAssignedType().getId();
                if (!typeIds.containsKey(id)) {
                    List<Long> tmp = new ArrayList<Long>();
                    tmp.add(a.getId());
                    typeIds.put(id, tmp);
                } else {
                    List<Long> tmp = typeIds.get(id);
                    tmp.add(a.getId());
                    typeIds.put(id, tmp);
                }
            }
        }

        return typeIds;
    }

    /**
     * Method to get an assignment's base type
     *
     * @param a FxAssignment
     * @return returns the id of the base property's type
     */
    public static long getBaseTypeId(FxAssignment a) {
        return CacheAdmin.getEnvironment()
                .getAssignment(a.getBaseAssignmentId())
                .getAssignedType()
                .getId();
    }

    /**
     * Iterates through the enum IgnoreProps
     *
     * @param xPath XPath of a given assignment
     * @return returns true if the xPath should be ignored
     */
    public static boolean isSystemProperty(String xPath) {
        for (IgnoreProps i : IgnoreProps.values()) {
            if (xPath.contains(i.toString()))
                return true;
        }
        return false;
    }

    /**
     * Returns the base XPath of an assignment
     *
     * @param a the FxAssignment
     * @return returns the XPath as a String
     */
    public static String getBaseXPath(FxAssignment a) {
        return CacheAdmin.getEnvironment().getAssignment(a.getBaseAssignmentId()).getXPath();
    }

    /**
     * Checks whether a type has any assignments (properties or groups)
     *
     * @param type the given FxType
     * @return returns true if the type has assignments
     */
    public static boolean typeHasAssignments(FxType type) {
        return type.getAssignedGroups().size() > 0 || type.getAssignedProperties().size() > 0;
    }

    /**
     * Examines a given FxAssignments for ScriptEvents --> scripts and returns a Map containing that information
     *
     * @param assignment the FxAssignment
     * @return returns a Map containing all events their respective script assignments for a given FxAssignment
     */
    public static Map<FxScriptEvent, Long[]> getScript(FxAssignment assignment) {
        Map<FxScriptEvent, Long[]> assignmentScripts = new HashMap<FxScriptEvent, Long[]>();
        if (assignment.hasScriptMappings()) {
            for (FxScriptEvent event : FxScriptEvent.values()) {
                if (assignment.hasScriptMapping(event)) {
                    assignmentScripts.put(event, toObject(assignment.getScriptMapping(event)));
                }
            }
        }
        return assignmentScripts;
    }

    /**
     * Takes a List of FxPropertyAssignments and a List of FxGroupAssignments as parameters and returns a combined
     * List of both (FxAssignment) sorted by their positions
     *
     * @param p List of FxPropertyAssignments
     * @param g List of FxGroupAssignments
     * @return returns sorted List of FxAssignments combining both inputs
     */
    public static List<FxAssignment> getCombinedAssignments(List<FxPropertyAssignment> p, List<FxGroupAssignment> g) {
        List<FxAssignment> combined = new ArrayList<FxAssignment>();
        if (p != null)
            combined.addAll(p);
        if (g != null)
            combined.addAll(g);

        return FxAssignment.sort(combined);
    }

    /**
     * Performs a linear list comparison and returns a combination of both,
     *
     * @param src the source List<T> (of, for example, assignment ids)
     * @param cmp the List<T> with which src will be compared
     * @return returns null if both parameters are null, returns src if cmp is null, returns cmp if src is null or returns the combined List<T>
     */
    public static <T> List<T> listCompare(List<T> src, List<T> cmp) {
        if (src != null) {
            if (cmp != null) {
                for (T i : cmp) {
                    if (!src.contains(i))
                        src.add(i);
                }
                return src;
            }
        }
        return cmp;
    }

    /**
     * Converts a List<Long> of assignment ids to a List<FxAssignment>
     *
     * @param assignIds the List of assignment ids
     * @return returns a List of <FxAssignment> or null if the input is null
     */
    public static List<FxAssignment> convertIdsToAssignments(List<Long> assignIds) {
        if (assignIds == null) {
            return null;
        }
        final List<FxAssignment> l = new ArrayList<FxAssignment>(assignIds.size());
        for (Long id : assignIds) {
            l.add(CacheAdmin.getEnvironment().getAssignment(id));
        }
        return l;
    }

    /**
     * Converts a List<FxAssignment> List<Long> of ids
     *
     * @param assignments the List of FxAssignments
     * @return returns a List of <Long> ids or null if the input is null
     */
    public static List<Long> convertAssignmentsToIds(List<? extends FxAssignment> assignments) {
        if (assignments == null) {
            return null;
        }
        final List<Long> l = new ArrayList<Long>(assignments.size());
        for (FxAssignment a : assignments) {
            l.add(a.getId());
        }
        return l;
    }
}
