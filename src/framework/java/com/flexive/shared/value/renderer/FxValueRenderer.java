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
package com.flexive.shared.value.renderer;

import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.FxValue;

import java.io.IOException;
import java.io.Writer;

/**
 * A locale-specific renderer for FxValue objects.  
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public interface FxValueRenderer {
    /**
     * Formats the given value in the renderer's locale. If <code>value</code> is
     * multi-lingual, then the translation in the renderer's locale is used.
     * If no translation is available, the default translation is used
     * (see {@link com.flexive.shared.value.FxValue#getBestTranslation()} }).
     *
     * @param value the value to be formatted
     * @return  the formatted value
     */
    String format(FxValue value);

    /**
     * Formats the given value in the renderer's locale. If <code>value</code> is multi-lingual,
     * then the translation for the given translationLanguage is used.
     * If no translation is available, the default translation is used
     * (see {@link com.flexive.shared.value.FxValue#getBestTranslation()} }).
     *
     * @param value the value to be formatted
     * @param translationLanguage   the translation which should be retrieved from the value
     * @return  the formatted value
     */
    String format(FxValue value, FxLanguage translationLanguage);

    /**
     * Renders the given value in the renderer's locale to the output writer. If <code>value</code> is
     * multi-lingual, then the translation in the renderer's locale is used.
     * If no translation is available, the default translation is used
     * (see {@link com.flexive.shared.value.FxValue#getBestTranslation()} }).
     *
     * @param out   the output writer
     * @param value the value to be rendered
     * @return  the FxValueRenderer instance
     * @throws java.io.IOException  if the value could not be written 
     */
    FxValueRenderer render(Writer out, FxValue value) throws IOException;

    /**
     * Renders the given value in the renderer's locale to the output writer. If <code>value</code> is
     * multi-lingual, then the translation for the given translationLanguage is used.
     * If no translation is available, the default translation is used
     * (see {@link com.flexive.shared.value.FxValue#getBestTranslation()} }).
     *
     * @param out   the output writer
     * @param value the value to be rendered
     * @param translationLanguage   the translation which should be retrieved from the value
     * @return  the FxValueRenderer instance
     * @throws java.io.IOException  if the value could not be written
     */
    FxValueRenderer render(Writer out, FxValue value, FxLanguage translationLanguage) throws IOException;
}
