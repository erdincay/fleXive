package com.flexive.shared.structure.export;

import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxGroupAssignment;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidStateException;

import java.util.List;
import java.util.Map;

/**
 * Generate a flattened export hierarchy for a given type ant its assignments and (optionally) any other types / assignments which
 * the given type depends on.<br/><br/>
 * Usage Description (incl. sample code):<br/>
 *
 * Initialise a StructureExporter:<br/>
 * <pre>StructureExporterCallback sec = StructureExporter.newInstance(typeId, ignoreDependencies);</pre> <br/><br/>
 *
 * If ignoreDependencies = true, only the given type's assignments will be exported ignoring any FxAssignments
 * which are derived from another type.<br/>
 * If ignoreDependencies = false, the given type's assignments and all structures which the current type depends on
 * will be available for export.<br/><br/>
 *
 * Retrieve all types and their immediate assignments as a Map of &lt;FxType, List&lt;FxAssignment&gt;&gt;:<br/>
 * <pre>Map&lt;FxType, List&lt;FxAssignment&gt;&gt; typeAssignments = exp.getTypeAssignments(); </pre> <br/>
 * <b>Important!</b><br/>
 * The #getTypeAssignments() returns a LinkedHashMap. The order of the given keys can / should be used as
 * for re-creating structures from a given export. However, types with several circular dependencies might yield different
 * type orders, depending on the type which is used as the "starting point" for the export. Since all derived FxAssignments will
 * be found in the dependency structure, it is only of importance to use first the type assignments
 * and only then the outcome of <pre>#getDependencyStructures</pre><br/>
 * Given a Map having keys (FxTypes) A, B, C (#keySet()), the order of structure creation must be A, B, C as well in order
 * to maintain integrity between referring type assignments.<br/>
 * This means, that any assignments retrieved from <pre>#getDependencyStructures()</pre> must also be created in this
 * order.<br/><br/>
 *
 * Retrieve all FxGroupAssignments and their Child Assignments as a Map of &lt;FxGroupAssignment, List&lt;FxAssignment&gt;&gt;:<br/>
 * <pre> Map&lt;FxGroupAssignment, List&lt;FxAssignment&gt;&gt; groupAssignments = exp.getGroupAssignments(); </pre>
 * Any Groups which are child assignments to a given group will be a key in the given map.<br/>
 * Any Groups having empty Lists as values, either have no child assignments OR their child assignments are dependent on other
 * structures and will be available by a call to <pre>getDependencyStructures();</pre> given that <pre>getHasDependencies();</pre>
 * returns true. <br/><br/>
 *
 * Check whether dependencies for the given export type exist:<br/>
 * <pre>sec.getHasDependencies();</pre>
 * This will return true IFF ignoreDependencies=false && dependencies on other structures were found.<br/><br/>
 *
 * Optional:<br>
 * Retrieve all dependencies, i.e. all Types and their (flattened) FxAssignments as a Map of &lt;FxType, List&lt;FxAssignment&gt;&gt;:<br/>
 * Note: the Map will return null if <pre>getHasDependencies() == false</pre>
 * <pre>Map&lt;FxType, List&lt;FxAssignment&gt;&gt; dependencies = exp.getDependencies();</pre> <br/><br/>
 *
 * Retrieve a structure of dependencies, i.e. a representation of all types and their respective assignments
 * for all types / assignments which the current type depends on:<br/>
 * <pre>List&lt;StructureExporterCallback&gt; mutualDepStructure = sec.getDependencyStructures();
 * for (StructureExporterCallback callback : mutualDepStruct) {
 *      typeAssignments = callback.getTypeAssignments(); // reassign
 *      groupAssignments = callback.getGroupAssignments();
 *      // ... do stuff here ...
 * }
 * </pre> <br/><br/>
 *
 * <b>Usage summary</b><br/>
 * Obtain a StructureExporter instance:
 * <pre>StructureExporter.newInstance(typeId, ignoreDependencies);</pre>
 * Retrieve the various immediate type assignments ...
 * <pre>#getTypeAssignments();</pre>
 * ... and any given group assignments
 * <pre>#getGroupAssignments();</pre>
 * If dependencies are found (unless ignored initially) walk through the dependency structure ...
 * <pre>
 * if(#getHasDependencies()) {
 *      #getDependencyStructures();</pre>
 *      ... and add the various derived assignments ([circular] dependencies) to the exported types.
 * <pre>    // Do stuff here //
 * }</pre><br/>
 *
 * Additional methods are provided to<br/>
 * - access scripts assigned to type events<br/>
 * - access script attached to assignment events<br/>
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface StructureExporterCallback {

    /**
     * @return returns the initial typeId
     */
    long getTypeId();

    /**
     * Set the (mutual) dependencies: a Map containing ids (Long) of FxTypes (keys) and Lists of ids (Long) of FxAssignments
     * (values) (disregarding their hierarchical positions within a type) which are (mutually) dependent on other types
     * This function must accumulate all dependencies in the case of a multiple type call
     *
     * @param dependencies Map&lt;Long, List&lt;Long&gt;&gt; of mutual dependencies
     */
    void setDependencies(Map<Long, List<Long>> dependencies);

    /**
     * Get the (mutual) dependencies for (a) given type(s). Returns NULL IFF getHasDependencies() returns false
     * This method returns the dependent assignments w/o their respective parents (structure-dependent)
     *
     * @return returns the Map&lt;FxType, List&lt;FxAssignment&gt;&gt; of (mutual) dependencies
     * @throws FxApplicationException on errors
     */
    Map<FxType, List<FxAssignment>> getDependencies() throws FxApplicationException;

    /**
     * Sets the Map of <FxTypes> (keys) and a List of FxAssignments (values) which reside in the root of
     * a given type. Will automatically remove system types if the StructureExporter.ignoreFlexiveSystemTypes is set
     *
     * @param typeAssignments the Map&lt;FxType>, List&lt;FxAssignment&gt;&gt;
     */
    void setTypeAssignments(Map<FxType, List<FxAssignment>> typeAssignments);

    /**
     * Set the group assignments: a Map&lt;FxGroupAssignment&gt; (keys) having the respective group's child assignments
     * as values
     *
     * @param groupAssignments Map&lt;FxGroupAssignment, List&lt;FxAssignment&gt;&gt;
     */
    void setGroupAssignments(Map<FxGroupAssignment, List<FxAssignment>> groupAssignments);

    /**
     * Returns instances of StructureExporterCallback containing the export structure of dependencies.
     * This method can only be called if the previous call to GenericStructureExporter was done with ignoreDependencies = false
     * and if the subsequent evaluation returned (mutual) dependencies
     *
     * @return returns an instance of StructureExporterCallback
     * @throws FxInvalidStateException on errors
     */
    List<StructureExporterCallback> getDependencyStructures() throws FxInvalidStateException;

    /**
     * Returns a LinkedHashMap of immediate (root) assignments for the FxTypes considered. If a type has no Assignments,
     * the List of values will be null.
     * IMPORTANT: In the case of mutual dependencies, multiple FxTypes will be returned in the map.
     * The ORDER of the the keys == order in which the FxTypes have to be created when reimporting via script / code
     *
     * @return returns Map&lt;FxType, List&lt;FxAssignment&gt;&gt;
     */
    Map<FxType, List<FxAssignment>> getTypeAssignments();

    /**
     * Returns all groups and their respective child assignments. IMPORTANT: If a group has no assignments, the
     * List of values will be null; If a type contains no groups, the returned Map will be null.
     *
     * @return returns Map&lt;FxGroupAssignment&gt;, List&lt;FxAssignment&gt;&gt;
     */
    Map<FxGroupAssignment, List<FxAssignment>> getGroupAssignments();

    /**
     * @return Returns true if the current type has dependencies, false otherwise and if called with "ignoreDependencies=true" or
     * if no dependencies were found in the evaluation phase
     */
    boolean getHasDependencies();

    /**
     * Returns a dependencyStructure for a given type's id
     * 
     * @param typeId the type Id for which a dependecy structure should be returned
     * @return returns an instance of StructureExporterCallback if the structure was found, null otherwise
     * @throws FxInvalidStateException on errors
     */
    StructureExporterCallback getDependencyStructure(long typeId) throws FxInvalidStateException;

    /**
     * @return Returns true if the call was made for multiple types
     */
    boolean getIsMultipleTypeCall();

    /**
     * @return returns the List of initial typeIds
     */
    List<Long> getTypeIds();

    /**
     * @param dependentOnMapping set the Map of assignment ids dependent on other assignments
     */
    void setDependentOnMapping(Map<Long, Long> dependentOnMapping);

    /**
     * @return returns the Map of assignment ids (keys) which are derived (dependent on) other assignments (values - ids)
     */
    Map<Long, Long> getDependentOnMapping();

    /**
     * @return Returns the script mappings for each type: type id (key) --> event (key) --> script ids (value List)
     */
    Map<Long, Map<String, List<Long>>> getTypeScriptMapping();

    /**
     * @return Returns the script mappings for each assignment: assignment id (key) --> event (key) --> script ids (value List)
     */
    Map<Long, Map<String, List<Long>>> getAssignmentScriptMapping();
}
