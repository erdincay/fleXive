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
package com.flexive.tutorial.persistance;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxString;
import com.flexive.shared.workflow.Step;

import java.util.List;

/**
 * Tutorial on how to work with FxContents
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ContentTutorialExample {

    //Customer FxType
    public final static String CUSTOMER = "Customer";
    //Hardware FxType
    public final static String HARDWARE = "Hardware";
    //Software FxType
    public final static String SOFTWARE = "Software";
    //Relation Customer->Hardware,Software
    public final static String CUSTOMER_PRODUCT = "CustProd";

    //ContentEngine reference
    private ContentEngine ce;
    //AssignmentEngine reference
    private AssignmentEngine ae;
    //TypeEngine reference
    private TypeEngine te;

    //used types
    private FxType customerType, hardwareType, softwareType, relationType;


    /**
     * Initialize references to needed 'engines'
     */
    public ContentTutorialExample() {
        this.ce = EJBLookup.getContentEngine();
        this.ae = EJBLookup.getAssignmentEngine();
        this.te = EJBLookup.getTypeEngine();
        this.customerType = CacheAdmin.getEnvironment().getType(CUSTOMER);
        this.hardwareType = CacheAdmin.getEnvironment().getType(HARDWARE);
        this.softwareType = CacheAdmin.getEnvironment().getType(SOFTWARE);
        this.relationType = CacheAdmin.getEnvironment().getType(CUSTOMER_PRODUCT);
    }

    /**
     * Create a new customer instance
     *
     * @return primary key of the customer
     * @throws FxApplicationException on errors
     */
    public FxPK createInstance() throws FxApplicationException {
        FxContent customer = ce.initialize(customerType.getId());
        customer.setValue("/Name", new FxString(false, "John Doe"));
        customer.setValue("/BillingAddress/Street", new FxString(false, "Downingstreet 42"));
        customer.setValue("/BillingAddress/ZIP", new FxString(false, "0815"));
        return ce.save(customer);
    }

    /**
     * Create a new version for Customer
     *
     * @param pk primary key
     * @return primary key of the new version
     * @throws FxApplicationException on errors
     */
    public FxPK createVersion(FxPK pk) throws FxApplicationException {
        FxContent customer = ce.load(pk);
        return ce.createNewVersion(customer);
    }

    /**
     * Change the step of the customer identified by its primary key
     * to the first step available from his workflow routing.
     * If no route is available, do nothing.
     *
     * @param pk primary key of the customer
     * @throws FxApplicationException on errors
     */
    public void changeWorkflowStep(FxPK pk) throws FxApplicationException {
        FxContent customer = ce.load(pk);
        //get the step the customer should be set to
        //we just retrieve the first available from the customers
        //valid target steps
        List<Step> targets = EJBLookup.getWorkflowRouteEngine().getTargets(customer.getStepId());
        if (targets.size() > 0) {
            customer.setStepId(targets.get(0).getId());
            ce.save(customer);
        }
    }

    /**
     * Add a delivery address to a customer
     *
     * @param pk primary key
     * @throws FxApplicationException on errors
     */
    public void addDeliveryAddress(FxPK pk) throws FxApplicationException {
        FxContent customer = ce.load(pk);
        customer.setValue("/DeliveryAddress/Street", new FxString(false, "Knightsbridge 84"));
        customer.setValue("/DeliveryAddress/ZIP", new FxString(false, "4711"));
        ce.save(customer);
    }

    /**
     * Change the delivery address property
     *
     * @param pk primary key
     * @throws FxApplicationException on errors
     */
    public void changeDeliveryAddress(FxPK pk) throws FxApplicationException {
        FxContent customer = ce.load(pk);
        //variant1 - set a new value
        customer.setValue("/DeliveryAddress/Street", new FxString(false, "Knightsbridge 85"));
        //variant2 - update existing value
        FxString street = (FxString) customer.getValue("/DeliveryAddress/Street");
        street.setValue("Knightsbridge 85");
        ce.save(customer);
    }

    /**
     * Create a piece of software or hardware
     *
     * @param type either the software or hardware type
     * @param name name of the product
     * @return primary key of the product
     * @throws FxApplicationException on errors
     */
    private FxPK createProduct(FxType type, String name) throws FxApplicationException {
        FxContent product = ce.initialize(type.getId());
        product.setValue("/Name", new FxString(true, name));
        return ce.save(product);
    }

    /**
     * Create a relation between a customer and products
     *
     * @return primary key of the customer
     * @throws FxApplicationException on errors
     */
    public FxPK relate() throws FxApplicationException {
        FxPK customer = createInstance();
        FxPK software = createProduct(softwareType, "[fleXive]");
        FxPK hardware = createProduct(hardwareType, "Fluxcompensator");

        FxContent relation = ce.initialize(relationType.getId());
        relation.setRelatedSource(customer).setRelatedDestination(software);
        ce.save(relation);
        relation = ce.initialize(relationType.getId());
        relation.setRelatedSource(customer).setRelatedDestination(hardware);
        ce.save(relation);
        return customer;
    }
}
