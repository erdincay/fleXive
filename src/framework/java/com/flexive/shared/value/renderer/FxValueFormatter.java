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
package com.flexive.shared.value.renderer;

import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.FxValue;

/**
 * Interface for a FxValue formatter. Formatters provide a datatype-specific and
 * locale-specific way for rendering FxValue instances to the user interface.
 * Formatters are usually not instantiated directly, but used by a
 * {@link FxValueRenderer} to format FxValues. A FxValueRenderer for a given language
 * can be retrieved using the {@link FxValueRendererFactory}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public interface FxValueFormatter<DataType, ValueType extends FxValue<DataType, ValueType>> {
    /**
     * Formats a FxValue object.
     *
     * @param container         the FxValue container object
     * @param value             the value to be formatted.
     * @param outputLanguage    the language to be used for rendering the value. This may
     * or may not be the language for which the value was stored in the FxValue container.
     *
     * @return  the formatted value
     */
    String format(ValueType container, DataType value, FxLanguage outputLanguage);
}
