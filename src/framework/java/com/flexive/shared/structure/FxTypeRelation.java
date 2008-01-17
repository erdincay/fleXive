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
package com.flexive.shared.structure;

import com.flexive.shared.exceptions.FxNotFoundException;

import java.io.Serializable;

/**
 * Extension of FxType to hold relation mappings
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxTypeRelation implements Serializable {
    private static final long serialVersionUID = 5059296100111806264L;
    private FxType source;
    private FxType destination;
    protected int maxSource;
    protected int maxDestination;

    /**
     * Constructor, only used internally.
     * Update relation mappings using FxTypeEdit!
     *
     * @param source         source type
     * @param destination    destination type
     * @param maxSource      maximum number of allowed source instances, unlimited if 0
     * @param maxDestination maximum number of allowed destination instances, unlimited if 0
     */
    public FxTypeRelation(FxType source, FxType destination, int maxSource, int maxDestination) {
        if( source == null || destination == null )
            throw new IllegalArgumentException("source and destination must not be null!");
        this.source = source;
        this.destination = destination;
        this.maxSource = maxSource;
        this.maxDestination = maxDestination;
    }

    /**
     * Get the source FxType
     *
     * @return Source FxType
     */
    public FxType getSource() {
        return source;
    }

    /**
     * Get the destination FxType
     *
     * @return Destination FxType
     */
    public FxType getDestination() {
        return destination;
    }

    /**
     * Get the maximum number of allowed source instances, unlimited if 0
     *
     * @return maximum number of allowed source instances, unlimited if 0
     */
    public int getMaxSource() {
        return maxSource;
    }

    /**
     * Get the maximum number of allowed destination instances, unlimited if 0
     *
     * @return maximum number of allowed destination instances, unlimited if 0
     */
    public int getMaxDestination() {
        return maxDestination;
    }

    /**
     * Is there a limit on source instances?
     *
     * @return limit on source instances?
     */
    public boolean isSourceLimited() {
        return this.maxSource != 0;
    }

    /**
     * Is there a limit on destination instances?
     *
     * @return limit on destination instances?
     */
    public boolean isDestinationLimited() {
        return this.maxDestination != 0;
    }

    /**
     * Resolve references after initial loading
     *
     * @param env env
     * @throws FxNotFoundException on errors
     */
    public void resolveReferences(FxEnvironment env) throws FxNotFoundException {
        if (getSource().getMode() == TypeMode.Preload && getDestination().getMode() == TypeMode.Preload) {
            this.source = env.getType(getSource().getId());
            this.destination = env.getType(getDestination().getId());
        }
    }

    /**
     * Get this FxTypeRelation as editable
     *
     * @return FxTypeRelationEdit
     */
    public FxTypeRelationEdit asEditable() {
        return new FxTypeRelationEdit(this);
    }

    /**
     * Overrides Objet's eqials method.
     * Compares two relations for same source and destination,
     * comparing multiplicites is intentionally neglected
     *
     * @return <code>this.getSource().getId() == other.getSource().getId() &&
     *               this.getDestination().getId() == other.getDestination().getId()</code>
     */
    @Override
    public boolean equals(Object o) {
        if (o==null || !(o instanceof FxTypeRelation))
            return false;
        if (o == this)
            return true;
        if (this.getSource().getId() == ((FxTypeRelation)o).getSource().getId() &&
                    this.getDestination().getId() == ((FxTypeRelation)o).getDestination().getId())
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        int result;
        result = source.hashCode();
        result = 31 * result + destination.hashCode();
        return result;
    }

    /*
     * Compares two relations for same source and destination,
     * and same multiplicites.
     *
     * @return <code>this.equals(other) && this.getMaxSource() == other.getMaxSource()
     *           && this.getMaxDestination() == other.getMaxDestination()</code>
     */

    public boolean equalsCompletely(Object o) {
        if (this.equals(o) && this.getMaxSource() == ((FxTypeRelation)o).getMaxSource()
                && this.getMaxDestination() == ((FxTypeRelation)o).getMaxDestination())
            return true;

       return false;
    }
}
