/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.shared.structure;

import com.flexive.shared.value.FxString;

import java.io.Serializable;
import java.util.List;

/**
 * (Structure) Group definition
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxGroup extends FxStructureElement implements Serializable {
    private static final long serialVersionUID = -5940207140757192022L;

    /**
     * Ctor
     *
     * @param id                       internl id
     * @param name                     name
     * @param label                    label
     * @param hint                     hint
     * @param overrideBaseMultiplicity may assignments override the multiplicity?
     * @param multiplicity             default multiplictiy
     * @param options                  options for this group
     */
    public FxGroup(long id, String name, FxString label, FxString hint, boolean overrideBaseMultiplicity,
                   FxMultiplicity multiplicity, List<FxStructureOption> options) {
        super(id, name, label, hint, overrideBaseMultiplicity, multiplicity, options);
    }

    /**
     * Get an editable instance (FxGroupEdit) of this instance
     *
     * @return editable instance
     */
    public FxGroupEdit asEditable() {
        return new FxGroupEdit(this);
    }
}
