/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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
package com.flexive.tutorial.persistance;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.security.ACL;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;

/**
 * Tutorial on how to work with FxTypes
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class StructureTutorialExample {


    public final static String BASE_TYPE = "Customer";

    /**
     * Create a new type for customer data
     *
     * @throws FxApplicationException on errors
     */
    public void createType() throws FxApplicationException {
        TypeEngine typeEngine = EJBLookup.getTypeEngine();

        FxString typeDesc = new FxString(true, FxLanguage.ENGLISH, "A generic customer");
        typeDesc.setTranslation(FxLanguage.GERMAN, "Ein generischer Kunde");
        ACL customerACL = CacheAdmin.getEnvironment().
                getACL(ACL.Category.STRUCTURE.getDefaultId());

        FxTypeEdit type = FxTypeEdit.createNew(BASE_TYPE, typeDesc, customerACL);

        //Type retrieval variant 1:
        typeEngine.save(type);
        FxType typeByName = CacheAdmin.getEnvironment().getType(BASE_TYPE);

        //Type retrieval variant 2:
//        long typeId = typeEngine.save(type);
//        FxType typeById = CacheAdmin.getEnvironment().getType(typeId);

        //Invalid Type retrieval - this will not work!
//        FxType typeByIdInvalid = CacheAdmin.getEnvironment().
//                getType(typeEngine.save(type));
    }

    /**
     * Add a street to the customer type
     *
     * @param typeId id of the type
     * @throws FxApplicationException on errors
     */
    public void addNameProperty(long typeId) throws FxApplicationException {
        AssignmentEngine assignmentEngine = EJBLookup.getAssignmentEngine();
        ACL customerACL = CacheAdmin.getEnvironment().
                getACL(ACL.Category.STRUCTURE.getDefaultId());

        FxPropertyEdit name =
                FxPropertyEdit.createNew("Name",
                        new FxString("Name of the person"),
                        new FxString("Enter the persons name"),
                        FxMultiplicity.MULT_1_1,
                        customerACL,
                        FxDataType.String1024);
        assignmentEngine.createProperty(typeId, name.setAutoUniquePropertyName(true), "/");
    }

    /**
     * Reuse the Caption property from the ROOT type
     *
     * @param typeId the type to assign the reused caption property
     * @throws FxApplicationException on errors
     */
    public void reuseCaptionProperty(long typeId) throws FxApplicationException {
        AssignmentEngine assignmentEngine = EJBLookup.getAssignmentEngine();

        //'regular' way to reuse a property assignment
        assignmentEngine.save(
                FxPropertyAssignmentEdit.createNew(
                        (FxPropertyAssignment) CacheAdmin.getEnvironment().
                                getAssignment("ROOT/CAPTION"),
                        CacheAdmin.getEnvironment().getType(BASE_TYPE),
                        "CAPTION",
                        "/").setEnabled(true), false);

        //same as above with a different alias using the convenience "reuse" method
        assignmentEngine.save(
                FxPropertyAssignmentEdit.reuse(
                        "ROOT/CAPTION",
                        BASE_TYPE,
                        "/",
                        "AnotherCaption"), false);
    }

    /**
     * Create a simple group for address data
     *
     * @param typeId id of the type
     * @throws FxApplicationException on errors
     */
    public void createAddressGroup(long typeId) throws FxApplicationException {
        AssignmentEngine assignmentEngine = EJBLookup.getAssignmentEngine();
        ACL customerACL = CacheAdmin.getEnvironment().
                getACL(ACL.Category.STRUCTURE.getDefaultId());

        assignmentEngine.createGroup(
                typeId,
                FxGroupEdit.createNew(
                        "Address",
                        new FxString("The customers address"),
                        new FxString("Enter the customers address here"),
                        true,
                        FxMultiplicity.MULT_1_1).
                        setAssignmentGroupMode(GroupMode.AnyOf),
                "/");

        FxPropertyEdit street =
                FxPropertyEdit.createNew("Street",
                        new FxString("Streetname"),
                        new FxString("Enter the street name"),
                        FxMultiplicity.MULT_1_1,
                        customerACL,
                        FxDataType.String1024).setAutoUniquePropertyName(true);
        FxPropertyEdit zip =
                FxPropertyEdit.createNew("ZIP",
                        new FxString("ZIP Code"),
                        new FxString("Enter the ZIP code"),
                        FxMultiplicity.MULT_1_1,
                        customerACL,
                        FxDataType.String1024).setAutoUniquePropertyName(true);
        assignmentEngine.createProperty(typeId, street, "/Address");
        assignmentEngine.createProperty(typeId, zip, "/Address");
    }

    /**
     * Reuse the address group as "DeliverAddress" and rename the original
     * address group to "BillingAddress"
     *
     * @throws FxApplicationException on errors
     */
    public void reuseGroup() throws FxApplicationException {
        AssignmentEngine assignmentEngine = EJBLookup.getAssignmentEngine();
        FxType customer = CacheAdmin.getEnvironment().getType(BASE_TYPE);

        FxGroupAssignment addressGroup = (FxGroupAssignment)
                CacheAdmin.getEnvironment().getAssignment(
                        customer.getName() + "/Address");

        assignmentEngine.save(
                FxGroupAssignmentEdit.createNew(
                        addressGroup,
                        customer,
                        "DeliveryAddress",
                        "/").setMultiplicity(FxMultiplicity.MULT_0_1), true);
        assignmentEngine.save(
                addressGroup.asEditable().setAlias("BillingAddress"),
                false);
    }

    /**
     * Create a "special customer" that gets a discount by inheriting
     * from the existing customer and adding a discount property.
     *
     * @throws FxApplicationException on errors
     */
    public void deriveType() throws FxApplicationException {
        TypeEngine typeEngine = EJBLookup.getTypeEngine();
        AssignmentEngine assignmentEngine = EJBLookup.getAssignmentEngine();

        //fetch base type (customer)
        FxType customer = CacheAdmin.getEnvironment().getType(BASE_TYPE);

        //create derived type
        FxTypeEdit special = FxTypeEdit.createNew(
                "SpecialCustomer",
                new FxString("A very special customer"),
                customer.getACL(),
                customer);
        long specialId = typeEngine.save(special);

        //add a discount property
        FxPropertyEdit discount =
                FxPropertyEdit.createNew("Discount",
                        new FxString("Discount in percent"),
                        new FxString("Enter the customers discount"),
                        FxMultiplicity.MULT_0_1,
                        customer.getACL(),
                        FxDataType.Float).setAutoUniquePropertyName(true);
        assignmentEngine.createProperty(specialId, discount, "/");
    }

    /**
     * Create a relation between Customers, Software and Hardware
     *
     * @throws FxApplicationException on errors
     */
    public void relation() throws FxApplicationException {
        TypeEngine typeEngine = EJBLookup.getTypeEngine();

        FxType customer = CacheAdmin.getEnvironment().getType(BASE_TYPE);
        FxType software = CacheAdmin.getEnvironment().getType("Software");
        FxType hardware = CacheAdmin.getEnvironment().getType("Hardware");

        FxString relDesc = new FxString("Relation Customer<->Products");
        ACL relationACL = CacheAdmin.getEnvironment().
                getACL(ACL.Category.STRUCTURE.getDefaultId());

        FxTypeEdit relEdit = FxTypeEdit.createNew(
                "CustProd", relDesc, relationACL).
                setMaxRelSource(0).setMaxRelDestination(5).
                setMode(TypeMode.Relation).
                addRelation(FxTypeRelationEdit.createNew(customer, software)).
                addRelation(FxTypeRelationEdit.createNew(customer, hardware));

        long typeId = typeEngine.save(relEdit);
        FxType rel = CacheAdmin.getEnvironment().getType(typeId);
    }

    /**
     * Remove the customer type.
     * Please note that no relations or references to
     * customers may exist.
     *
     * @throws FxApplicationException on errors
     */
    public void deleteCustomer() throws FxApplicationException {
        TypeEngine typeEngine = EJBLookup.getTypeEngine();
        ContentEngine contentEngine = EJBLookup.getContentEngine();

        FxType customer = CacheAdmin.getEnvironment().getType(BASE_TYPE);
        contentEngine.removeForType(customer.getId());
        typeEngine.remove(customer.getId());
    }

    //helper methods to make this tutorial actually run

    /**
     * Create the needed products - Hardware and Software
     *
     * @throws FxApplicationException on errors
     */
    public void createProducts() throws FxApplicationException {
        TypeEngine typeEngine = EJBLookup.getTypeEngine();
        AssignmentEngine assignmentEngine = EJBLookup.getAssignmentEngine();

        FxString productDesc = new FxString("Product description ...");
        ACL productACL = CacheAdmin.getEnvironment().
                getACL(ACL.Category.STRUCTURE.getDefaultId());
        //create hard- and software
        typeEngine.save(FxTypeEdit.createNew("Hardware", productDesc, productACL));
        typeEngine.save(FxTypeEdit.createNew("Software", productDesc, productACL));

        //reuse caption as name
        assignmentEngine.save(FxPropertyAssignmentEdit.reuse("ROOT/CAPTION",
                "Hardware", "/", "Name"), false);
        assignmentEngine.save(FxPropertyAssignmentEdit.reuse("ROOT/CAPTION",
                "Software", "/", "Name"), false);
    }

    /**
     * Clean up everything except customer
     *
     * @throws FxApplicationException on errors
     */
    public void cleanUp() throws FxApplicationException {
        _clean("CustProd");
        _clean("Hardware");
        _clean("Software");
        _clean("SpecialCustomer");
    }

    /**
     * Remove a type
     *
     * @param type name of the type
     * @throws FxApplicationException on errors
     */
    private void _clean(String type) throws FxApplicationException {
        TypeEngine typeEngine = EJBLookup.getTypeEngine();
        ContentEngine contentEngine = EJBLookup.getContentEngine();

        FxType t = CacheAdmin.getEnvironment().getType(type);
        contentEngine.removeForType(t.getId());
        typeEngine.remove(t.getId());
    }
}
