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

import java.io.Serializable;

/**
 * Editable FxTypeRelation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see FxTypeRelation
 */
public class FxTypeRelationEdit extends FxTypeRelation implements Serializable {
    private static final long serialVersionUID = -8795200059643946635L;
    private FxTypeRelation original;

    /**
     * Constructor
     *
     * @param relation the FxTypeRelation to make editable
     */
    public FxTypeRelationEdit(FxTypeRelation relation) {
        super(relation.getSource(), relation.getDestination(), relation.getMaxSource(), relation.getMaxDestination());
        this.original = relation;
    }


    public void setMaxSource(int maxSource) {
        if (maxSource < 0)
            maxSource = 0;
        if (maxSource != this.maxSource)
            this.maxSource = maxSource;
    }

    public void setMaxDestination(int maxDestination) {
        if (maxDestination < 0)
            maxDestination = 0;
        if (maxDestination != this.maxDestination)
            this.maxDestination = maxDestination;
    }

    public boolean isChanged() {
        return this.maxSource != original.maxSource || this.maxDestination != original.maxDestination;
    }


    /**
     * Create a new FxTypeEdit with unlimited source and destination instances
     *
     * @param source      source type
     * @param destination destination type
     * @return FxTypeRelationEdit
     */
    public static FxTypeRelationEdit createNew(FxType source, FxType destination) {
        return new FxTypeRelationEdit(new FxTypeRelation(source, destination, 0, 0));
    }

    /**
     * Create a new FxTypeEdit
     *
     * @param source         source type
     * @param destination    destination type
     * @param maxSource      maximum number of allowed source instances, unlimited if 0
     * @param maxDestination maximum number of allowed destination instances, unlimited if 0
     * @return FxTypeRelationEdit
     */
    public static FxTypeRelationEdit createNew(FxType source, FxType destination, int maxSource, int maxDestination) {
        return new FxTypeRelationEdit(new FxTypeRelation(source, destination, maxSource, maxDestination));
    }
}
