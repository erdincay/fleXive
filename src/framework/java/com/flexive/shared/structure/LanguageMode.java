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

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.ObjectWithLabel;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.value.FxString;

import java.io.Serializable;

/**
 * Definition how languages are handled by a type
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public enum LanguageMode implements Serializable, ObjectWithLabel {

    /**
     * Content is not language dependent
     */
    None(0),

    /**
     * One language per content may be defined
     */
    Single(1),

    /**
     * Every property may exist in different languages
     */
    Multiple(2);

    private int id;

    /**
     * Ctor
     *
     * @param id internal id
     */
    LanguageMode(int id) {
        this.id = id;
    }

    /**
     * Getter for the internal id
     *
     * @return internal id
     */
    public int getId() {
        return id;
    }

    /**
     * Get a LanguageMode by its id
     *
     * @param id internal id
     * @return LanguageMode
     * @throws FxNotFoundException on errors
     */
    public static LanguageMode getById(int id) throws FxNotFoundException {
        for (LanguageMode langMode : LanguageMode.values())
            if (langMode.id == id)
                return langMode;
        throw new FxNotFoundException("ex.structure.languageMode.notFound.id", id);
    }

    /**
     * Check if the current mode can be upgraded to newMode
     *
     * @param newMode mode to upgrade to
     * @return upgradeable
     */
    public boolean isUpgradeable(LanguageMode newMode) {
        return newMode.getId() > this.getId();  
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxString getLabel() {
        return FxSharedUtils.getEnumLabel(this);
    }
}
