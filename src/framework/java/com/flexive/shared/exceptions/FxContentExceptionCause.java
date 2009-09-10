/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.shared.exceptions;

import com.flexive.shared.ObjectWithLabel;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.value.FxString;

/**
 * If an exception is throws by the ContentEngine and it affects data in a FxContent instance, calling
 * getAffectedXPath() returns the XPath and getContentExceptionCause() will return an indicator of the case (this enum)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @since 3.1
 */
public enum FxContentExceptionCause implements ObjectWithLabel {
    /**
     * Worst case, nothing is known that caused the exception
     */
    Unknown,
    /**
     * A unique constraint has been violated
     */
    UniqueConstraintViolated,
    /**
     * Overriding an assignments multilanguage setting is not allowed
     */
    MultiLangOverride,
    /**
     * Tried to add an XPath to a group which has a One-Of group constraint which already contained an XPath
     */
    GroupOneOfViolated,
    /**
     * An invalid index has been set for an XPath (e.g. a max. of 2 is allowed and 3 was set)
     */
    InvalidIndex,
    /**
     * An assignment with a maximum length restriction has been assigned a value that is too large
     */
    MaxlengthViolated,
    /**
     * Not enough required occurances of an assignment
     */
    RequiredViolated,
    /**
     * Attempted to set a system internal value using an XPath which is not allowed
     */
    SysInternalAttempt,
    /**
     * A value of an invalid or incompatible data type has been assigned (e.g. String instead of Long)
     */
    InvalidValueDatatype,
    /**
     * Tried to access a FxNoAccess value
     */
    NoAccess,
    /**
     * Tried to write a read-only field
     */
    ReadOnly,
    /**
     * Group mode is not valid (e.g. more than one required property for a one-of rstriction) 
     */
    InvalidGroupMode;


    /**
     * {@inheritDoc}
     */
    public FxString getLabel() {
        return FxSharedUtils.getEnumLabel(this);
    }
}
