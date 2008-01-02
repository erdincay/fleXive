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
package com.flexive.shared.security;

import com.flexive.shared.AbstractSelectableObjectWithName;

import java.io.Serializable;

/**
 * A Mandator
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Mandator extends AbstractSelectableObjectWithName implements Serializable {
    private static final long serialVersionUID = 4821229397499448319L;

    /**
     * The default flexive mandator
     */
    public static final int MANDATOR_FLEXIVE = 0;

    // Internal Id
    private long id = -1;
    // Mandators name
    private String name = null;
    // Metadata reference id
    private long metadataId = -1;
    // Active flag
    private boolean active = false;
    private LifeCycleInfo lci = null;

    /**
     * Get the Id of the Mandators
     *
     * @return Id
     */
    public long getId() {
        return id;
    }

    /**
     * Get the Mandators name
     *
     * @return Mandators name
     */
    public String getName() {
        return name;
    }

    /**
     * The metadata reference id.
     *
     * @return The metadata reference id
     */
    public long getMetadataId() {
        return this.metadataId;
    }

    /**
     * Are meta data attached to this mandator?
     *
     * @return if meta data are attached to this mandator
     */
    public boolean hasMetadata() {
        return this.metadataId >= 0;
    }

    /**
     * Returns true if the mandator is active.
     *
     * @return true if the mandator is active.
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Get the LifeCycleInfoImpl
     *
     * @return LifeCycleInfoImpl
     */
    public LifeCycleInfo getLifeCycleInfo() {
        return lci;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMetadataId(long metadataId) {
        this.metadataId = metadataId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setLci(LifeCycleInfo lci) {
        this.lci = lci;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String getDisplay() {
        return getName() + (this.active ? "" : " (inactive)");
    }

    public Mandator(){
        /* empty constructor */
    }

    /**
     * Constructor.
     *
     * @param id         the unique id of the mandator
     * @param name       the name of the mandator
     * @param metadataId the metadate reference id
     * @param active     is this mandator active?
     * @param lci        lifecycle info
     */
    public Mandator(long id, String name, long metadataId, boolean active, LifeCycleInfo lci) {
        this.id = id;
        this.name = name;
        this.metadataId = metadataId;
        this.active = active;
        this.lci = lci;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getDisplay() + " (Id="+getId()+")";
    }
}
