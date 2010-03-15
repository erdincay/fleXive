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
package com.flexive.core.structure;

import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.TypeMode;

import java.io.Serializable;


/**
 * Dummy FxType used for bootstraping types while preloading.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxPreloadType extends FxType implements Serializable {
    private static final long serialVersionUID = -2090546406012929252L;

    /**
     * Constructor - used for building the environment
     *
     * @param id id of the type
     */
    public FxPreloadType(long id) {
        super(id, null, null, "preloaded", null, null, null, null, TypeMode.Preload,
                null, null, (byte) 0, false, true, false, 0, 0, 0, 0, null, null, null, null);
    }

    /**
     * Constructor - used for import
     *
     * @param name name of the type
     */
    public FxPreloadType(String name) {
        super(-1, null, null, name, null, null, null, null, TypeMode.Preload,
                null, null, (byte) 0, false, true, false, 0, 0, 0, 0, null, null, null, null);
    }
}
