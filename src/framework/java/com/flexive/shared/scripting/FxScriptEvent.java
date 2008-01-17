/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.shared.scripting;

import com.flexive.shared.exceptions.FxNotFoundException;

/**
 * All kinds of scripts used in flexive,
 * depending on the type scripts are initialized with different bindings
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public enum FxScriptEvent {

    /**
     * Manually execute script, not dependent on any 'trigger point' - will be executed on demand
     */
    Manual(1, FxScriptScope.All),

    /**
     * Fired before an existing content is saved
     */
    BeforeContentSave(2, FxScriptScope.Type,
            "FxContent content"),

    /**
     * Fired after an existing content is saved
     */
    AfterContentSave(3, FxScriptScope.Type,
            "FxPK pk"),

    /**
     * Fired before a new content is created
     */
    BeforeContentCreate(4, FxScriptScope.Type,
            "FxContent content"),

    /**
     * Fired after a new content is created
     */
    AfterContentCreate(5, FxScriptScope.Type,
            "FxPK pk"),

    /**
     * Fired before a content is removed
     */
    BeforeContentRemove(6, FxScriptScope.Type,
            "FxPK pk", "FxContentSecurityInfo securityInfo"),

    /**
     * Fired after a content is removed
     */
    AfterContentRemove(7, FxScriptScope.Type,
            "FxPK pk"),

    /**
     * Fired after a content is loaded
     */
    AfterContentLoad(8, FxScriptScope.Type,
            "FxContent content"),

    /**
     * Fired after a new content instance is initialized with default values
     */
    AfterContentInitialize(9, FxScriptScope.Type,
            "FxContent content"),

    /**
     * Fired for ContentEngine.prepareSave() for new contents
     */
    PrepareContentCreate(10, FxScriptScope.Type,
            "FxContent content"),

    /**
     * Fired for ContentEngine.prepareSave() for existing contents
     */
    PrepareContentSave(11, FxScriptScope.Type,
            "FxContent content"),

    /**
     * Fired before a FxData update of an existing existance
     */
    BeforeDataChangeUpdate(12, FxScriptScope.Assignment,
            "FxContent content", "FxDeltaChange change"),

    /**
     * Fired after a FxData update of an existing existance
     */
    AfterDataChangeUpdate(13, FxScriptScope.Assignment,
            "FxContent content", "FxDeltaChange change"),

    /**
     * Fired before a FxData is removed from an existing existance, instance will <b>not</b> be removed!
     */
    BeforeDataChangeDelete(14, FxScriptScope.Assignment,
            "FxContent content", "FxDeltaChange change"),

    /**
     * Fired after a FxData is removed from an existing existance, instance will <b>not</b> be removed!
     */
    AfterDataChangeDelete(15, FxScriptScope.Assignment,
            "FxContent content", "FxDeltaChange change"),

    /**
     * Fired before a FxData is updated in an existing existance
     */
    BeforeDataChangeAdd(16, FxScriptScope.Assignment,
            "FxContent content", "FxDeltaChange change"),

    /**
     * Fired after a FxData is updated in an existing existance
     */
    AfterDataChangeAdd(17, FxScriptScope.Assignment,
            "FxContent content", "FxDeltaChange change"),

    /**
     * Fired before a new instance is created that will contain the assignment
     */
    BeforeAssignmentDataCreate(18, FxScriptScope.Assignment,
            "FxContent content", "FxAssignment assignment"),

    /**
     * Fired after a new instance is created that will contain the assignment
     */
    AfterAssignmentDataCreate(19, FxScriptScope.Assignment,
            "FxPK pk", "FxAssignment assignment"),

    /**
     * Fired before an existing instance is saved that contains the assignment
     */
    BeforeAssignmentDataSave(20, FxScriptScope.Assignment,
            "FxContent content", "FxAssignment assignment"),

    /**
     * Fired after an existing instance is saved that contains the assignment
     */
    AfterAssignmentDataSave(21, FxScriptScope.Assignment,
            "FxPK pk", "FxAssignment assignment"),


    /**
     * Fired before an instance is deleted <b>could</b> contain the assignment,
     * no guarantee can be made that the instance actually contains FxData for the assignment!
     */
    BeforeAssignmentDataDelete(22, FxScriptScope.Assignment,
            "FxPK pk", "FxAssignment assignment"),

    /**
     * Fired after an instance is deleted <b>could</b> contain the assignment,
     * no guarantee can be made that the instance actually contains FxData for the assignment!
     */
    AfterAssignmentDataDelete(23, FxScriptScope.Assignment,
            "FxPK pk", "FxAssignment assignment"),

    BinaryPreviewProcess(24, FxScriptScope.BinaryProcessing,
            "boolean processed",
            "boolean useDefaultPreview",
            "int defaultId",
            "String mimeType",
            "String metaData",
            "File binaryFile",
            "File previewFile1",
            "File previewFile2",
            "File previewFile3",
            "int[] dimensionPreview1",
            "int[] dimensionPreview2",
            "int[] dimensionPreview3"
    ),

    /**
     * Fired after a new user account has been created. The account ID and the
     * contact data PK are bound as "accountId" and "pk" .
     */
    AfterAccountCreate(25, FxScriptScope.Accounts, "long accountId", "FxPK pk");


    private long id;
    private FxScriptScope scope;
    private String[] bindingInfo;

    /**
     * Constructore
     *
     * @param id          type id
     * @param scope       script scope
     * @param bindingInfo provided variable bindings
     */
    FxScriptEvent(int id, FxScriptScope scope, String... bindingInfo) {
        this.id = id;
        this.scope = scope;
        this.bindingInfo = bindingInfo;
    }

    /**
     * Get the scope of this script, ie if it is independent, for types or assignments
     *
     * @return scope
     */
    public FxScriptScope getScope() {
        return scope;
    }

    /**
     * Get a list of all bindings available to the script.
     * Binding descriptions are human readable in the form "datatype variablename"
     *
     * @return list of all bindings available to the script
     */
    public String[] getBindingInfo() {
        return bindingInfo.clone();
    }

    /**
     * Getter for the internal id
     *
     * @return internal id
     */
    public long getId() {
        return id;
    }

    public String getName(){
        if(id >= 1){
            try {
                return getById(id).name();
            } catch (FxNotFoundException e) {
                return "Not_Defined";
            }
        }
        return "";
    }

    /**
     * Get a FxScriptEvent by its id
     *
     * @param id requested id
     * @return FxScriptEvent
     * @throws FxNotFoundException on errors
     */
    public static FxScriptEvent getById(long id) throws FxNotFoundException {
        for (FxScriptEvent event : FxScriptEvent.values())
            if (event.id == id)
                return event;
        throw new FxNotFoundException("ex.scripting.type.notFound", id);
    }
}
