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

package com.flexive.shared.structure.export;

import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxGroupAssignment;
import static com.flexive.shared.structure.export.StructureExporterTools.*;
import com.flexive.shared.exceptions.FxInvalidStateException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptMapping;
import com.flexive.shared.scripting.FxScriptMappingEntry;
import com.flexive.shared.CacheAdmin;

import java.util.*;

/**
 * Generate an export hierarchy for (a) type(s).
 * The StructureExporter creates a "flattened" view of a given structure (and its dependent assignments)
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see com.flexive.shared.structure.export.StructureExporterCallback;
 */
public class StructureExporter implements StructureExporterCallback {
    private Map<FxType, List<FxAssignment>> dependencies;
    private List<Long> differingDerivedAssignments;
    private long typeId = -1;
    private boolean isMultiTypeCall = false;
    private List<Long> typeIds;
    private List<StructureExporterCallback> dependencyStructures;
    private boolean dependencyStructureInitialised = false;
    private boolean dependenciesInitialised = false;
    private boolean ignoreDependencies;
    private GenericStructureExporter gse;
    private ResultObject result;
    private Map<Long, List<Long>> typeAssignmentIds;
    private Map<Long, List<Long>> groupAssignmentIds;
    private Map<Long, Map<String, List<Long>>> typeScriptMapping;
    private Map<Long, Map<String, List<Long>>> assignmentScriptMapping;

    /**
     * Constructor
     *
     * @param typeId             the type's id
     * @param ignoreDependencies set to true if mutual dependencies should be ignored
     */
    private StructureExporter(long typeId, boolean ignoreDependencies) {
        this.typeId = typeId;
        this.ignoreDependencies = ignoreDependencies;
        this.result = new ResultObject();
        gse = GenericStructureExporter.newInstance(this, typeId, ignoreDependencies);
        try {
            gse.run();
            gse = null; // clean up
        } finally {
            gse = null;
        }
    }

    /**
     * Private constructor for making "whiteList" calls to enable structure analysis of mutual dependencies
     *
     * @param typeId    the FxType's id
     * @param whiteList the List of FxAssignments which should be considered for the call to GenericStructureExporter (remaining FxAssignments will be ignored)
     */
    private StructureExporter(long typeId, List<FxAssignment> whiteList) {
        this.typeId = typeId;
        this.ignoreDependencies = true;
        this.result = new ResultObject();
        gse = GenericStructureExporter.newInstance(this, typeId, whiteList);
        try {
            gse.run();
            gse = null; // clean up
        } finally {
            gse = null;
        }
    }

    /**
     * Private constructor for calls with multiple type ids
     *
     * @param typeIds            the List of type ids
     * @param ignoreDependencies set to true if dependencies should be ignored
     */
    private StructureExporter(List<Long> typeIds, boolean ignoreDependencies) {
        this.typeIds = typeIds;
        this.ignoreDependencies = ignoreDependencies;
        isMultiTypeCall = true;
        final MultipleStructureExporter mex = new MultipleStructureExporter(typeIds, ignoreDependencies);
        result = mex.evaluateResultObjects();
    }

    /**
     * Returns an instance of the StructureExporter
     *
     * @param typeId             the id of the type to be exported
     * @param ignoreDependencies set to true if dependencies on other types / assignments should be ignored
     * @return returns an instance of the StructureExporter
     */
    public static StructureExporter newInstance(long typeId, boolean ignoreDependencies) {
        return new StructureExporter(typeId, ignoreDependencies);
    }

    /**
     * Returns an instance of the StructureExporter for a call with multiple type Ids
     *
     * @param typeIds            the List of type ids to be exported
     * @param ignoreDependencies set to true of dependencies on other types / assignments should be ignored
     * @return returns an instance of the StructureExporter containing the accumulated results
     */
    public static StructureExporter newInstance(List<Long> typeIds, boolean ignoreDependencies) {
        return new StructureExporter(typeIds, ignoreDependencies);
    }

    /**
     * Returns an instance of the StructureExporter
     * Makes whiteList calls for structure walkthroughs of dependency maps
     *
     * @param typeId    the type's id
     * @param whiteList the whiteList (List of FxAssignments to be returned as a flattened structure)
     * @return returns itself
     */
    private static StructureExporter newInstance(long typeId, List<FxAssignment> whiteList) {
        return new StructureExporter(typeId, whiteList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTypeId() {
        return typeId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDependencies(Map<Long, List<Long>> dependencies) {
        result.cachedDependencies = dependencies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<FxType, List<FxAssignment>> getDependencies() throws FxApplicationException {
        if (!getHasDependencies())
            return null;
        // lazy caller for dependency map conversion
        if (!dependenciesInitialised)
            populateDependencies();
        return dependencies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StructureExporterCallback> getDependencyStructures() throws FxInvalidStateException {
        if (!getHasDependencies())
            throw new FxInvalidStateException("StructureExport.ex.noDependencies");
        // initialise if not done already
        if (!dependenciesInitialised)
            populateDependencies();

        // initialise dependency structure if not done already
        if (!dependencyStructureInitialised)
            populateDependencyStructure();

        return dependencyStructures;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> getDifferingDerivedAssignments() throws FxInvalidStateException {
        if(!getHasDependencies() && !dependencyStructureInitialised)
            throw new FxInvalidStateException("StructureExport.ex.noDependencies");

        return differingDerivedAssignments;
    }

    /**
     * Lazy initialiser for the dependencyStructure. Derived types' dependencies on their parent types
     * are filtered out
     */
    private void populateDependencyStructure() {
        if (dependencyStructures == null)
            dependencyStructures = new ArrayList<StructureExporterCallback>();

        for (FxType type : dependencies.keySet()) {
            List<FxAssignment> whiteList = new ArrayList<FxAssignment>();

            // reassign the current key's id list to a List of FxAssignments
            for (FxAssignment assignment : dependencies.get(type)) {
                whiteList.add(assignment);
                // add the parent group assignments for each assignment, unless it's already in the list
                final List<FxAssignment> parents = recursiveParentGroupAssignmentSearch(assignment);
                if (parents.size() > 0) {
                    for (FxAssignment a : parents) {
                        if (!whiteList.contains(a))
                            whiteList.add(a);
                    }
                }
            }

            // DERIVED TYPES: remove all dependencies on the parent type, unless the assignment differs
            // from the "original"
            if (type.isDerived()) {
                if(differingDerivedAssignments == null)
                    differingDerivedAssignments = new ArrayList<Long>(0);

                final long parentTypeId = type.getParent().getId();
                final List<FxAssignment> filteredWhiteList = new ArrayList<FxAssignment>();
                for (FxAssignment a : whiteList) {
                    final long baseAssignmentId = StructureExporterTools.getBaseTypeId(a);
                    if (baseAssignmentId != parentTypeId) {
                        filteredWhiteList.add(a);
                    } else {
                        if(AssignmentDifferenceAnalyser.analyse(a, true).size() > 0) {
                            if(!differingDerivedAssignments.contains(a.getId()))
                                differingDerivedAssignments.add(a.getId());
                            filteredWhiteList.add(a);
                        }
                    }
                }
                whiteList = new ArrayList<FxAssignment>(filteredWhiteList);
            }

            // only add something if the whiteList has any entries
            if (whiteList.size() > 0) {
                final StructureExporterCallback callback = StructureExporter.newInstance(type.getId(), whiteList);
                dependencyStructures.add(callback);
            }
        }
        dependencyStructureInitialised = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTypeAssignments(Map<FxType, List<FxAssignment>> typeAssignments) {
        result.typeAssignments = typeAssignments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGroupAssignments(Map<FxGroupAssignment, List<FxAssignment>> groupAssignments) {
        result.groupAssignments = groupAssignments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<FxType, List<FxAssignment>> getTypeAssignments() {
        return result.typeAssignments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<FxGroupAssignment, List<FxAssignment>> getGroupAssignments() {
        return result.groupAssignments;
    }

    @Override
    public boolean getHasDependencies() {
        return !this.ignoreDependencies && result.cachedDependencies != null;
    }

    /**
     * Lazy initialiser for the population of the dependency map.
     * IMPORTANT: if the child assignment of a given FxGroupAssignment is unique to the given property
     * (i.e. not derived) it will also be added to this map (s.t. it can be added to the
     * "whiteList" when called from the callback interface), since the MutualDependencyAnalyser ignores those properties
     * and they would be missing in the callback interface's get method.
     */
    private void populateDependencies() {
        if (dependencies == null)
            dependencies = new HashMap<FxType, List<FxAssignment>>();

        for (Long typeId : result.cachedDependencies.keySet()) {
            final List<Long> tmp = result.cachedDependencies.get(typeId);
            // convert ids to FxAssignments
            final List<FxAssignment> l = convertIdsToAssignments(tmp);
            if (l != null) {
                // iterate through assignments, retrieve their assigned type and add this to the dependencies
                for (FxAssignment a : l) {
                    final FxType t = a.getAssignedType();
                    if (dependencies.containsKey(t)) {
                        final List<FxAssignment> current = new ArrayList<FxAssignment>();
                        current.addAll(dependencies.get(t));
                        if (!current.contains(a)) {
                            current.add(a);
                            if (a instanceof FxGroupAssignment) { // get groupAssignment children
                                final List<FxAssignment> uniqueChildren = getAllUniqueAssignmentsForGroup((FxGroupAssignment) a);
                                if (uniqueChildren != null)
                                    current.addAll(uniqueChildren);
                            }
                            dependencies.put(t, current);
                        }

                    } else {
                        final List<FxAssignment> current = new ArrayList<FxAssignment>();
                        current.add(a);
                        if (a instanceof FxGroupAssignment) {
                            final List<FxAssignment> uniqueChildren = getAllUniqueAssignmentsForGroup((FxGroupAssignment) a);
                            if (uniqueChildren != null)
                                current.addAll(uniqueChildren);
                        }
                        dependencies.put(t, current);
                    }
                }
            }
        }
        dependenciesInitialised = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureExporterCallback getDependencyStructure(long typeId) throws FxInvalidStateException {
        if (!getHasDependencies())
            throw new FxInvalidStateException("StructureExport.ex.noDependencies");

        if (!dependencyStructureInitialised)
            populateDependencyStructure();

        for (StructureExporterCallback callback : dependencyStructures) {
            if (callback.getTypeId() == typeId)
                return callback;
        }
        return null;
    }

    /**
     * @return returns the ResultObject
     */
    ResultObject getResult() {
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getIsMultipleTypeCall() {
        return isMultiTypeCall;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> getTypeIds() {
        if (!isMultiTypeCall)
            return null;
        return typeIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDependentOnMapping(Map<Long, Long> dependentOnMapping) {
        result.dependentOnMapping = dependentOnMapping;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Map<String, List<Long>>> getTypeScriptMapping() {
        populateTypeScriptMapping();
        return typeScriptMapping;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Map<String, List<Long>>> getAssignmentScriptMapping() {
        populateAssignmentScriptMapping();
        return assignmentScriptMapping;
    }

    /**
     * Populate the typeScriptMapping
     */
    private void populateTypeScriptMapping() {

        initTypeAssignmentIds();
        if (typeScriptMapping == null) {
            typeScriptMapping = new HashMap<Long, Map<String, List<Long>>>();

            for (FxScriptInfo si : CacheAdmin.getEnvironment().getScripts()) {
                final long scriptId = si.getId();
                // retrieve the script mapping for the given script
                final FxScriptMapping sm = CacheAdmin.getEnvironment().getScriptMapping(scriptId);

                final Map<String, List<Long>> eventScriptMap = new HashMap<String, List<Long>>();


                for (FxScriptMappingEntry sme : sm.getMappedTypes()) {
                    final long sTypeId = sme.getId();
                    // types
                    if (typeAssignmentIds.containsKey(sTypeId)) {
                        final String event = sme.getScriptEvent().getName();

                        // if we have an entry for the relevant type already
                        if (typeScriptMapping.containsKey(sTypeId)) {
                            final Map<String, List<Long>> eventScriptMapTmp = typeScriptMapping.get(sTypeId);
                            // see if we have an entry for the given event
                            if (eventScriptMapTmp.containsKey(event)) {
                                // pull the list, then update
                                final List<Long> scriptIdListTmp = new ArrayList<Long>(eventScriptMapTmp.get(event));
                                // if the script is already in there, don't do anything
                                if (!scriptIdListTmp.contains(scriptId))
                                    scriptIdListTmp.add(scriptId); // add
                                eventScriptMapTmp.put(event, scriptIdListTmp); // write back
                            } else {
                                // just add to the map
                                eventScriptMapTmp.put(event, Arrays.asList(scriptId));
                            }
                        } else {
                            // add event and script to the map
                            eventScriptMap.put(event, Arrays.asList(scriptId));
                            // add this for the relevant type
                            typeScriptMapping.put(sTypeId, eventScriptMap);
                        }
                    }
                }
            }
        }
    }

    /**
     * Populates the assignmentScriptMapping
     */
    private void populateAssignmentScriptMapping() {
        initGroupAssignmentIds();
        if (assignmentScriptMapping == null) {
            assignmentScriptMapping = new HashMap<Long, Map<String, List<Long>>>();

            final Set<Long> allAssIds = getAllExportedAssignmentIdsAsList();

            for (FxScriptInfo si : CacheAdmin.getEnvironment().getScripts()) {
                final long scriptId = si.getId();
                // retrieve the script mapping for the given script
                final FxScriptMapping sm = CacheAdmin.getEnvironment().getScriptMapping(scriptId);

                final Map<String, List<Long>> eventScriptMap = new HashMap<String, List<Long>>();

                for (FxScriptMappingEntry sme : sm.getMappedAssignments()) {
                    final long sAssId = sme.getId();

                    if (allAssIds.contains(sme.getId())) {
                        final String event = sme.getScriptEvent().getName();

                        // if we have an entry for the relevant assignment already
                        if (assignmentScriptMapping.containsKey(sAssId)) {
                            final Map<String, List<Long>> eventScriptMapTmp = assignmentScriptMapping.get(sAssId);
                            // see if we have an entry for the given event
                            if (eventScriptMapTmp.containsKey(event)) {
                                // pull the list, then update
                                final List<Long> scriptIdListTmp = new ArrayList<Long>(eventScriptMapTmp.get(event));
                                // if the script is already in there, don't do anything
                                if (!scriptIdListTmp.contains(scriptId)) {
                                    scriptIdListTmp.add(scriptId); // add
                                    eventScriptMapTmp.put(event, scriptIdListTmp); // write back
                                }
                            } else {
                                // just add to the map
                                eventScriptMapTmp.put(event, Arrays.asList(scriptId));
                            }
                        } else {
                            // add event and script to the map
                            eventScriptMap.put(event, Arrays.asList(scriptId));
                            // add this for the relevant type
                            assignmentScriptMapping.put(sAssId, eventScriptMap);
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Long> getDependentOnMapping() {
        return result.dependentOnMapping;
    }

    /**
     * Utility method returns the resulting type assignments as a Map&lt;Long, List&lt;Long&gt;&gt; of the types' ids (keys) and their root assignments (value List)
     *
     * @return returns the typeAssignments as a Map of Long ids
     */
    public Map<Long, List<Long>> getTypeAssignmentIds() {
        initTypeAssignmentIds();
        return typeAssignmentIds;
    }

    /**
     * Utility method returns the resulting group assignments as a Map&lt;Long, List&lt;Long&gt;&gt; of the groups' ids (keys) and their assigned children's ids (value List)
     *
     * @return returns the groupAssignments as a Map of Long ids
     */
    public Map<Long, List<Long>> getGroupAssignmentIds() {
        initGroupAssignmentIds();
        return groupAssignmentIds;
    }

    /**
     * Initialiser for the typeAssignmentIds
     */
    private void initTypeAssignmentIds() {
        if (typeAssignmentIds == null) {
            typeAssignmentIds = new HashMap<Long, List<Long>>(result.typeAssignments.size());

            for (FxType t : result.typeAssignments.keySet()) {
                final List<Long> assignmentIds = new ArrayList<Long>();
                if (result.typeAssignments.get(t) != null) {
                    for (FxAssignment a : result.typeAssignments.get(t)) {
                        assignmentIds.add(a.getId());
                    }
                }
                typeAssignmentIds.put(t.getId(), assignmentIds);
            }
        }
    }

    /**
     * Initialiser for the groupAssignmentIds
     */
    private void initGroupAssignmentIds() {
        if (groupAssignmentIds == null) {
            if (result.groupAssignments != null) {
                groupAssignmentIds = new HashMap<Long, List<Long>>(result.groupAssignments.size());

                for (FxGroupAssignment ga : result.groupAssignments.keySet()) {
                    final List<Long> assignmentIds = new ArrayList<Long>();
                    if (result.groupAssignments.get(ga) != null) {
                        for (FxAssignment a : result.groupAssignments.get(ga)) {
                            assignmentIds.add(a.getId());
                        }
                    }
                    groupAssignmentIds.put(ga.getId(), assignmentIds);
                }

            } else {
                // just create an empty map
                groupAssignmentIds = new HashMap<Long, List<Long>>();
            }
        }
    }

    /**
     * @return returns a compiled Set of all exported assignment ids
     */
    private Set<Long> getAllExportedAssignmentIdsAsList() {
        final Set<Long> allAssIds = new HashSet<Long>();
        // type assignments
        initTypeAssignmentIds();
        for (Long typeId : typeAssignmentIds.keySet()) {
            if (typeAssignmentIds.get(typeId) != null)
                allAssIds.addAll(typeAssignmentIds.get(typeId));
        }

        // group assignments
        initGroupAssignmentIds();
        for (Long groupId : groupAssignmentIds.keySet()) {
            if (groupAssignmentIds.get(groupId) != null)
                allAssIds.addAll(groupAssignmentIds.get(groupId));
        }

        // dependencies
        if (getHasDependencies()) {
            allAssIds.addAll(result.cachedDependencies.keySet());
            for (Long id : result.cachedDependencies.keySet()) {
                if (result.cachedDependencies.get(id) != null)
                    allAssIds.addAll(result.cachedDependencies.get(id));
            }
        }

        return allAssIds;
    }

    /**
     * A class to store result objects (from the GenericStructureExporter)
     */
    static class ResultObject {
        private Map<FxType, List<FxAssignment>> typeAssignments;
        private Map<FxGroupAssignment, List<FxAssignment>> groupAssignments;
        private Map<Long, List<Long>> cachedDependencies;
        private Map<Long, Long> dependentOnMapping;
    }

    /**
     * A class to handle multiple structureExporter requests (i.e. exporting more than 1 node)
     */
    class MultipleStructureExporter {
        private List<ResultObject> resultList;
        private StructureExporter exp;
        private Map<FxType, List<FxAssignment>> typeAssignments;
        private Map<FxGroupAssignment, List<FxAssignment>> groupAssignments;
        private Map<Long, List<Long>> cachedDependencies;
        private Map<Long, Long> dependentOnMapping;

        /**
         * @param typeIds            the List of typeIds
         * @param ignoreDependencies ignore dependencies
         */
        MultipleStructureExporter(List<Long> typeIds, boolean ignoreDependencies) {
            resultList = new ArrayList<ResultObject>();
            for (Long typeId : typeIds) {
                exp = StructureExporter.newInstance(typeId, ignoreDependencies);
                resultList.add(exp.getResult());
            }
        }

        /**
         * This method compares the different result objects and returns a single, combined result
         *
         * @return returns a ResultObject
         */
        ResultObject evaluateResultObjects() {
            if (resultList.size() == 0) {
                return new ResultObject(); // return an empty object
            }

            final ResultObject out = new ResultObject();

            for (int i = 0; i < resultList.size(); i++) {
                if (i == 0) { // add all during the first iteration
                    this.typeAssignments = resultList.get(i).typeAssignments;
                    if (resultList.get(i).groupAssignments != null)
                        this.groupAssignments = resultList.get(i).groupAssignments;
                    if (resultList.get(i).cachedDependencies != null)
                        this.cachedDependencies = resultList.get(i).cachedDependencies;
                    if (resultList.get(i).dependentOnMapping != null)
                        this.dependentOnMapping = resultList.get(i).dependentOnMapping;

                } else {
                    // compare this with the rest of the resultList
                    compareAndUpdate(i);
                }

            }

            out.typeAssignments = this.typeAssignments;
            out.groupAssignments = this.groupAssignments;
            out.cachedDependencies = this.cachedDependencies;
            out.dependentOnMapping = this.dependentOnMapping;
            return out;
        }

        /**
         * Compares the the local assignments with a result object given an index
         *
         * @param idx the index of resultList to be checked
         */
        private void compareAndUpdate(int idx) {
            // compare type assignments
            final ResultObject currentResult = resultList.get(idx);
            for (FxType t : currentResult.typeAssignments.keySet()) {
                if (this.typeAssignments == null) // should never be null
                    this.typeAssignments = new HashMap<FxType, List<FxAssignment>>();

                if (!this.typeAssignments.containsKey(t)) {
                    this.typeAssignments.put(t, currentResult.typeAssignments.get(t));
                }
            }

            // compare groupAssignments
            if (currentResult.groupAssignments != null) {
                for (FxGroupAssignment ga : currentResult.groupAssignments.keySet()) {
                    if (this.groupAssignments == null)
                        this.groupAssignments = new HashMap<FxGroupAssignment, List<FxAssignment>>();

                    if (!this.groupAssignments.containsKey(ga)) {
                        this.groupAssignments.put(ga, currentResult.groupAssignments.get(ga));
                    }
                }
            }

            // compare dependencies --> must be aggregated! (multiple dependencies on the same type can exist)
            if (currentResult.cachedDependencies != null) {
                for (Long id : currentResult.cachedDependencies.keySet()) {
                    if (this.cachedDependencies == null)
                        cachedDependencies = new HashMap<Long, List<Long>>();

                    if (!this.cachedDependencies.containsKey(id)) {
                        this.cachedDependencies.put(id, currentResult.cachedDependencies.get(id));

                    } else { // retrieve the current dependencies, remove duplicates and add to map
                        final Set<Long> s = new HashSet<Long>();
                        s.addAll(this.cachedDependencies.get(id));
                        s.addAll(currentResult.cachedDependencies.get(id));
                        final ArrayList<Long> unique = new ArrayList<Long>(s.size());
                        unique.addAll(s);
                        this.cachedDependencies.put(id, unique);
                    }
                }
            }

            // compare the dependentOnMapping
            if (currentResult.dependentOnMapping != null) {
                for (Long id : currentResult.dependentOnMapping.keySet()) {
                    if (this.dependentOnMapping == null)
                        dependentOnMapping = new HashMap<Long, Long>();

                    this.dependentOnMapping.put(id, currentResult.dependentOnMapping.get(id));
                }
            }
        }
    }
}
