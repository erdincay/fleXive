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
import static com.flexive.shared.structure.export.StructureExporterTools.*;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Walks through a type's properties and groups (and its dependent types and groups) and pushes the results
 * to StructureExporter.<br>
 * This Generic Class should never be called directly but rather through the
 * StructureExporterCallback interface / StructureExporter
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see com.flexive.shared.structure.export.StructureExporterCallback
 * @see com.flexive.shared.structure.export.StructureExporter
 */
public class GenericStructureExporter {

    private StructureExporterCallback callback;
    private Map<Long, List<Long>> dependencies; // an internal map for assignments depending on other structures
    private Map<Long, Long> dependentOnMapping;
    private ConcurrentMap<Long, Boolean> typesChecked;
    protected long typeId;
    protected FxType type;
    protected boolean ignoreAssignmentDependencies = true;
    protected List<FxAssignment> whiteList;
    protected boolean whiteListPresent = false;
    protected Map<FxType, List<FxAssignment>> typeAssignments;
    protected Map<FxGroupAssignment, List<FxAssignment>> groupAssignments;

    /**
     * "Default" constructor
     *
     * @param callback                     the callback interface
     * @param typeId                       the type's id
     * @param ignoreAssignmentDependencies ignore dependent (reused) properties
     * @param dependencies                 a Map of typeIds --> FxAssignment ids which will be ignored when creating the script (and treated separately)
     * @param typesChecked                 a Map of typeIds --> boolean values = types for which dependencies exist & were scripted already
     * @param whiteList                    a List of FxAssignments which acts as a filter for all other assignments of a given type
     * @param typeAssignments              the Map if FxTypes and their root assignments (for recursive calls)
     * @param groupAssignments             the Map of GroupAssignments --> FxAssignments (for recursive calls)
     * @param dependentOnMapping           the Map if assignment ids (keys) and the assignments' ids they depend on (values)
     */
    private GenericStructureExporter(StructureExporterCallback callback, long typeId, boolean ignoreAssignmentDependencies,
                                     Map<Long, List<Long>> dependencies,
                                     ConcurrentMap<Long, Boolean> typesChecked, List<FxAssignment> whiteList,
                                     Map<FxType, List<FxAssignment>> typeAssignments,
                                     Map<FxGroupAssignment, List<FxAssignment>> groupAssignments, Map<Long, Long> dependentOnMapping) {
        this.callback = callback;
        this.typeId = typeId;
        type = CacheAdmin.getEnvironment().getType(typeId);
        this.ignoreAssignmentDependencies = ignoreAssignmentDependencies;
        this.dependencies = dependencies;
        this.dependentOnMapping = dependentOnMapping;
        this.typesChecked = typesChecked;
        // run dependency analyser
        if (!ignoreAssignmentDependencies) { // run dep analysis and populate the typesChecked map
            runDependencyAnalysis();
            if (this.typesChecked == null) { // populate only on the first call
                this.typesChecked = new ConcurrentHashMap<Long, Boolean>();
            }
            populateTypesChecked();
        }
        if (whiteList != null) {
            whiteListPresent = true;
            this.ignoreAssignmentDependencies = true;
            this.whiteList = whiteList;
        }

        if (typeAssignments == null) { // init as LinkedHashMap to maintain order of keys
            this.typeAssignments = new LinkedHashMap<FxType, List<FxAssignment>>();
        } else {
            this.typeAssignments = typeAssignments;
        }

        if (groupAssignments == null) { // init
            this.groupAssignments = new HashMap<FxGroupAssignment, List<FxAssignment>>();
        } else {
            this.groupAssignments = groupAssignments;
        }
    }

    /**
     * Returns an instance of the GenericStructureExporter
     * ignoreDependencies switch: if set to "true" it will ignore any dependencies on other types / properties,
     * i.e. such properties will be exported as "assignment:"
     * If set to "false", the reused types / properties etc. will be exported as well
     *
     * @param callback           the callback interface
     * @param typeId             the id of the type to be exported
     * @param ignoreDependencies whether dependencies on other types should be ignored
     * @return returns an instance of the GenericStructureExporter
     */
    protected static GenericStructureExporter newInstance(StructureExporterCallback callback, long typeId, boolean ignoreDependencies) {
        return new GenericStructureExporter(callback, typeId, ignoreDependencies, null, null, null, null, null, null);
    }

    /**
     * Returns an instance of the GenericStructureExporter
     * ignoreList: a List of (derived) properties which should be ignored when creating the script. Note: this affects
     * derived properties / groups only (in the case of mutual dependencies)!
     *
     * @param callback           the callback interface
     * @param typeId             the type's id
     * @param ignoreDependencies whether dependencies on other types should be ignored
     * @param dependencies       a Map of typeIds --> FxAssignment ids which will be ignored when creating the script (dependent on other types / assignments)
     * @param typesChecked       a Map of typeIds --> boolean values = types for which dependencies exist & were scripted already
     * @param typeAssignments    the Map of FxTypes and their root assignments
     * @param groupAssignments   the Map of GroupAssignments --> FxAssignments (for recursive calls)
     * @param dependentOnMapping the Map if assignment ids (keys) and the assignments' ids they depend on (values)
     * @return returns an instance of the GenericStructureExporter
     */
    protected static GenericStructureExporter newInstance(StructureExporterCallback callback, long typeId,
                                                          boolean ignoreDependencies,
                                                          Map<Long, List<Long>> dependencies, ConcurrentMap<Long, Boolean> typesChecked,
                                                          Map<FxType, List<FxAssignment>> typeAssignments,
                                                          Map<FxGroupAssignment, List<FxAssignment>> groupAssignments,
                                                          Map<Long, Long> dependentOnMapping) {
        return new GenericStructureExporter(callback, typeId, ignoreDependencies, dependencies, typesChecked, null,
                typeAssignments, groupAssignments, dependentOnMapping);
    }

    /**
     * Returns an instance of the GenericStructureExporter for examination of whiteLists (= i.e. exclusive List of FxAssignments
     * for a given type). This call is very similar to a construction call providing the typeId and the ignoreDependencies flag only,
     * except that it solely inspects the assignments given in the whitelist (for the given type) and automatically
     * ignores any dependencies (it is assumed that the whiteList is the result of a previous mutual dependency analysis).
     *
     * @param callback  the callback interface
     * @param typeId    the type's id
     * @param whiteList the List of FxAssignments for which a script will be generated (all others are ignored)
     * @return returns an instance of the GenericStructureExporter
     */
    protected static GenericStructureExporter newInstance(StructureExporter callback, long typeId, List<FxAssignment> whiteList) {
        return new GenericStructureExporter(callback, typeId, true, null, null, whiteList, null, null, null);
    }

    /**
     * Checks if a given FxAssignment is in the ignorelist
     *
     * @param input the FxAssignment to look for
     * @return returns true if the input FxAssignment was found in the ignoreList
     */
    private boolean isAssignmentInDependencies(FxAssignment input) {
        if (dependencies == null)
            return false;

        for (Long key : dependencies.keySet()) {
            final List<Long> tmp = dependencies.get(key);
            if (tmp.contains(input.getId()))
                return true;
        }
        return false;
    }

    /**
     * Runs the MutualDependencyAnalyser and in turn sets the typesChecked and mutualDependencies maps (the latter
     * will be set in the callback interface.
     */
    private void runDependencyAnalysis() {
        final DependencyAnalyser analyser = new DependencyAnalyser(CacheAdmin.getEnvironment().getType(this.typeId), this.dependencies, this.dependentOnMapping);
        analyser.analyse();
        dependencies = analyser.getDependencies();
        dependentOnMapping = analyser.getDependentOnMapping();
        // callback
        if (dependencies.size() == 0) {
            callback.setDependencies(null);
            callback.setDependentOnMapping(null);
        } else {
            callback.setDependencies(dependencies);
            callback.setDependentOnMapping(dependentOnMapping);
        }
    }

    /**
     * This (management) method walks through a type's assignments (and, if set, through dependent types' assignments)
     */
    public void run() {
        final FxType type = CacheAdmin.getEnvironment().getType(typeId);

        if (typeHasAssignments(type)) { // move on to its assignments
            // set the immediate assignments for the given type
            populateTypeAssignments(type, getTypeAssignments(type));
            callback.setTypeAssignments(typeAssignments);

            // set the group assignments if they exist
            if (type.getAssignedGroups().size() > 0) {
                for (FxGroupAssignment g : type.getAssignedGroups()) {
                    groupAssignments = getGroupAssignments(g, groupAssignments);
                }
                callback.setGroupAssignments(groupAssignments);
            }

            // check if we have any derived assignments withing the type and whether dependencies should be ignored
            if (!ignoreAssignmentDependencies) { // resolve dependencies and export base types/properties
                /*  remove the current type from the list of types to be called again if mutual dependencies exist
                    --> set the current type to "checked" */
                if (typesChecked.containsKey(typeId)) {
                    typesChecked.replace(typeId, false, true);
                }

                for (long id : typesChecked.keySet()) { // get list of types and call class again, PREPEND to outcome
                    if (dependencies.size() > 0) { // behold Sire, we have mutual dependencies!
                        if (!typesChecked.get(id)) {
                            GenericStructureExporter.newInstance(callback, id, ignoreAssignmentDependencies, dependencies,
                                    typesChecked, typeAssignments, groupAssignments, dependentOnMapping).run();
                        }
                    }
                }
            }
        }
    }

    /**
     * This method maintains the correct order of keys in the LinkedHashMap typeAssignments on
     * recursive calls, i.e. makes sure that the relevant element is prepended to the Map
     *
     * @param newType a new Map key which must be prepended
     * @param newList a new value for the given key
     */
    protected void populateTypeAssignments(FxType newType, List<FxAssignment> newList) {
        if (typeAssignments.size() > 0) {
            final Map<FxType, List<FxAssignment>> newMap = new LinkedHashMap<FxType, List<FxAssignment>>();
            newMap.put(newType, newList);
            newMap.putAll(typeAssignments);
            // reassign
            typeAssignments = newMap;

        } else {
            typeAssignments.put(newType, newList);
        }
    }

    /**
     * Retrieve all of a type's assignments, check whether they should be ignored (enum IgnoreProps in GenericStructureExporterTools)
     * and also check that they are not in a whitelist if one is present.
     *
     * @param type the FxType
     * @return returns a List of <FxAssignment> containing all Root assignments for a given FxType
     */
    protected List<FxAssignment> getTypeAssignments(FxType type) {
        final List<FxAssignment> out = new ArrayList<FxAssignment>();

        for (FxAssignment a : StructureExporterTools.getCombinedAssignments(type.getAssignedProperties(), type.getAssignedGroups())) {
            if (!whiteListPresent) { // call with a whiteList?
                if (!isSystemProperty(a.getXPath())) {
                    if (ignoreAssignmentDependencies) // depedencies ignored?
                        out.add(a);
                    else {
                        if (!isAssignmentInDependencies(a))
                            out.add(a);
                    }
                }
            }

            if (whiteListPresent) {
                if (!isSystemProperty(a.getXPath()) && whiteList.contains(a)) {
                    out.add(a);
                }
            }
        }

        return out;
    }

    /**
     * Recursive walks through a group's assignments and sub-assignments
     *
     * @param currentGroup     the current FxGroupAssignment to be examined
     * @param groupAssignments a Map of FxGroupAssignments(keys) and FxAssignments (values as List)
     * @return returns a Map FxGroupAssignment, List FxAssignment containing all groups and their respective child assignments
     */
    protected Map<FxGroupAssignment, List<FxAssignment>> getGroupAssignments(
            FxGroupAssignment currentGroup, Map<FxGroupAssignment, List<FxAssignment>> groupAssignments) {
        // recursive call
        final List<FxAssignment> assignments = currentGroup.getAssignments();
        final List<FxAssignment> tmp = new ArrayList<FxAssignment>();

        if (!whiteListPresent) { // call w/o a whiteList
            if (assignments.size() > 0) { // does the group have any assignments?
                if (ignoreAssignmentDependencies) { // dependencies ignored?
                    // walk through assignments and get the next group
                    for (FxAssignment a : assignments) {
                        if (a instanceof FxGroupAssignment) {
                            tmp.add(a);
                            groupAssignments.putAll(getGroupAssignments((FxGroupAssignment) a, groupAssignments));
                        } else if (a instanceof FxPropertyAssignment) {
                            tmp.add(a);
                        }
                    }
                    if (tmp.size() > 0)
                        groupAssignments.put(currentGroup, tmp);

                } else {
                    // walk through assignments and get the next group
                    for (FxAssignment a : assignments) {
                        if (a instanceof FxGroupAssignment && !isAssignmentInDependencies(a)) {
                            tmp.add(a);
                            groupAssignments.putAll(getGroupAssignments((FxGroupAssignment) a, groupAssignments));
                        } else if (a instanceof FxPropertyAssignment && !isAssignmentInDependencies(a)) {
                            tmp.add(a);
                        }
                    }
                    if (tmp.size() > 0)
                        groupAssignments.put(currentGroup, tmp);
                    // add the group IFF tmp == 0 && the group is not already in the groupAssignments && it is not in the dependency map
                    if (tmp.size() == 0 && !groupAssignments.containsKey(currentGroup) && !isAssignmentInDependencies(currentGroup))
                        groupAssignments.put(currentGroup, null);
                }
            } else { // group has no child assignments
                groupAssignments.put(currentGroup, null);
            }

        }
        if (whiteListPresent) { // whiteList present
            if (assignments.size() > 0) { // does the group have any assignments?
                for (FxAssignment a : assignments) {
                    if (a instanceof FxGroupAssignment && whiteList.contains(a)) {
                        tmp.add(a);
                        groupAssignments.putAll(getGroupAssignments((FxGroupAssignment) a, groupAssignments));
                    } else if (a instanceof FxPropertyAssignment && whiteList.contains(a)) {
                        tmp.add(a);
                    }

                }
                if (tmp.size() > 0)
                    groupAssignments.put(currentGroup, tmp); // add the remaining assignments to the list
                if (tmp.size() == 0 && whiteList.contains(currentGroup) && !groupAssignments.containsKey(currentGroup))
                    groupAssignments.put(currentGroup, null); // add the current group only if contained in whiteList
            } else { // no assignments for this group
                if (whiteList.contains(currentGroup)) {
                    groupAssignments.put(currentGroup, null);
                }
            }
        }

        return groupAssignments;
    }

    /**
     * Populates the typesChecked map with all connected FxType ids and "false" (- called and altered in #run() )
     */
    private void populateTypesChecked() {
//        if (typesChecked.size() < 1) {
        for (Long key : dependencies.keySet()) {
            if (!typesChecked.containsKey(key)) // only add the new ones
                typesChecked.put(key, false);
        }
//        }
    }

    /**
     * A class to analyse (circular / mutual) dependencies and derived assignments of types
     * i.e. mutual property / group reuse
     */
    static class DependencyAnalyser {

        private Map<Long, List<Long>> dependencies; // = new HashMap<Long, List<Long>>();
        private Map<Long, Long> dependentOnMapping; // key=assignment id, value=depends on this base assignment id (derived from it)
        private FxType type;

        /**
         * Constructor, takes an FxType to e analysed and a Map<Long>, List<Long>> as arguments.
         * Results of the analysis will be appended to the dependencyMap, if the dependencyMap is null,
         * a new one will be instantiated.
         *
         * @param type               an FxType which will be analysed
         * @param dependencies       the Map<Long, List<Long>> of mutual dependencies, where the keys represent the base FxType ids for
         *                           which the List<Long> of (derived) assignment ids.
         * @param dependentOnMapping the Map if assignment ids (keys) and the assignments' ids they depend on (values)
         */
        DependencyAnalyser(FxType type, Map<Long, List<Long>> dependencies, Map<Long, Long> dependentOnMapping) {
            this.type = type;
            if (dependencies == null)
                this.dependencies = new HashMap<Long, List<Long>>();
            else
                this.dependencies = dependencies;

            if (dependentOnMapping == null)
                this.dependentOnMapping = new HashMap<Long, Long>();
            else
                this.dependentOnMapping = dependentOnMapping;
        }

        /**
         * Returns the Map of typeIds --> derived assignments for which mutual dependencies exist.
         *
         * @return dependencyMap, a Map Long, List Long, where the keys are type ids and the Lists are ids of assignments
         */
        Map<Long, List<Long>> getDependencies() {
            return dependencies;
        }

        /**
         * @return returns the Map of dependent mappings
         */
        Map<Long, Long> getDependentOnMapping() {
            return this.dependentOnMapping;
        }

        /**
         * Analyse the mutual dependencies and generate the respective Map contents
         */
        void analyse() {
            final long sourceTypeId = type.getId();
            // get the relations between the base typeIds and the derivedAssignments
            final List<FxAssignment> allDerived = getAllDerivedAssignmentsForType(type);
            // populate the dependenOnMapping
            populateDependentOnMapping(allDerived);
            final Map<Long, List<Long>> derivedTypeIds = getBaseTypeIds(allDerived);

            // for each of those type ids check if we have a mutual dependency for our source type
            for (Long id : derivedTypeIds.keySet()) {
                final FxType t = CacheAdmin.getEnvironment().getType(id);
                final Map<Long, List<Long>> derivedTypeIdsOfConnectedType = getBaseTypeIds(getAllDerivedAssignmentsForType(t));
                // derived use of prop / group in connected type
                if (dependencies.containsKey(sourceTypeId)) {
                    final List<Long> assignments = listCompare(dependencies.get(sourceTypeId), derivedTypeIdsOfConnectedType.get(sourceTypeId));
                    // overwrite in map (add the source first)
                    if (assignments != null && assignments.size() > 0)
                        dependencies.put(sourceTypeId, assignments);
                } else {
                    final List<Long> assignments = derivedTypeIdsOfConnectedType.get(sourceTypeId);
                    if (assignments != null && assignments.size() > 0)
                        dependencies.put(sourceTypeId, assignments);
                }

                // derived use of prop / group in calling type
                if (dependencies.containsKey(id)) {
                    final List<Long> assignments = listCompare(dependencies.get(id), derivedTypeIds.get(id));
                    // overwrite in map
                    if (assignments != null && assignments.size() > 0)
                        dependencies.put(id, assignments);
                } else {
                    final List<Long> assignments = derivedTypeIds.get(id);
                    if (assignments != null && assignments.size() > 0)
                        dependencies.put(id, assignments);
                }
            }
        }

        /**
         * Populates the dependentOnMapping from a List of derived FxAssignments
         *
         * @param allDerived the List of derived FxAssignments
         */
        private void populateDependentOnMapping(List<FxAssignment> allDerived) {
            for (FxAssignment derivedAss : allDerived) {
                final long baseAssId = derivedAss.getBaseAssignmentId();
                dependentOnMapping.put(derivedAss.getId(), baseAssId);
            }
        }
    }
}
