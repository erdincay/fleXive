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

import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.FxValue;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>
 * Default FxValue renderer implementation for a specific language. Forwards requests to an internal
 * map of {@link FxValueFormatter} instances.</p>
 * <p>
 * This renderer stores the target language and a map of {@link FxValueFormatter} instances
 * that can be used to override the default formatters. The renderers returned by
 * the {@link FxValueRendererFactory} do not use this functionality, but use the
 * standard formatters for all instances.
 * </p>
 * <p>
 * <b>Note:</b>: this class is deliberately not public, since this would allow
 * static renderer instances to be manipulated through the {@link #put} method.
 * </p>
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class FxValueRendererImpl implements FxValueRenderer {
    private final FxLanguage language;
    private final ConcurrentMap<Class, FxValueFormatter> rendererMap = new ConcurrentHashMap<Class, FxValueFormatter>();

    /**
     * Create a new renderer for the given language.
     * @param language  the language
     */
    FxValueRendererImpl(FxLanguage language) {
        this.language = language;
    }

    /**
     * Create a new renderer in the calling user's language.
     */
    FxValueRendererImpl() {
        this(FxContext.get().getTicket().getLanguage());
    }

    /** {@inheritDoc} */
    public String format(FxValue value) {
        return format(value, language);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    public String format(FxValue value, FxLanguage translationLanguage) {
        final FxValueFormatter formatter = get(value.getClass());
        if (formatter != null) {
            return formatter.format(value, value.getBestTranslation(translationLanguage), language);
        }
        // use generic object formatter as fallback
        return get(FxValue.class).format(value, value.getBestTranslation(translationLanguage), language);
    }

    /** {@inheritDoc} */
    public FxValueRenderer render(Writer out, FxValue value) throws IOException {
        out.write(format(value));
        return this;
    }

    /** {@inheritDoc} */
    public FxValueRenderer render(Writer out, FxValue value, FxLanguage translationLanguage) throws IOException {
        out.write(format(value, translationLanguage));
        return this;
    }

    <DT, T extends FxValue<DT, T>> void put(Class<T> type, FxValueFormatter<DT, T> formatter) {
        rendererMap.put(type, formatter);
    }

    FxValueFormatter get(Class valueType) {
        //noinspection unchecked
        final FxValueFormatter formatter = rendererMap.get(valueType);   // safe because put(...) method is bounded
        return formatter != null ? formatter    // return formatter
                : (!language.equals(FxValueRendererFactory.DEFAULT) 
                ? FxValueRendererFactory.getDefaultFormatter(valueType): null); // use fallback
    }
}
