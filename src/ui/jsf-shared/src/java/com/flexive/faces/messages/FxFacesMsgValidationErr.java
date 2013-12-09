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
package com.flexive.faces.messages;

import com.flexive.faces.components.input.FxValueInputValidator;
import com.flexive.shared.value.FxValue;

/**
 * Faces message created by {@link FxValueInputValidator}. It contains the value causing the violation,
 * allowing an application e.g. to determine the XPath without parsing the localized message text.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1.5
 */
public class FxFacesMsgValidationErr extends FxFacesMsgErr {
    private static final long serialVersionUID = -4251850393455801774L;

    private final FxValue value;
    private final String summaryKey;
    private final Object[] summaryParams;

    public FxFacesMsgValidationErr(FxValue value, String summaryKey, Object... summaryParams) {
        super(summaryKey, summaryParams);
        this.value = value;
        this.summaryKey = summaryKey;
        this.summaryParams = summaryParams;
    }

    /**
     * Returns the value that caused the validation error.
     *
     * @return  the value that caused the validation error.
     */
    public FxValue getValue() {
        return value;
    }

    /**
     * @return  the message key of the message
     * @since 3.2.0
     */
    public String getSummaryKey() {
        return summaryKey;
    }

    /**
     * @return  the parameters for the message key
     * @since 3.2.0
     */
    public Object[] getSummaryParams() {
        return summaryParams;
    }
}
