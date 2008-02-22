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
package com.flexive.shared.value;

import java.util.Map;

/**
 * HTML Content, like FxString but with the optional <code>tidyHTML</code flag
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see FxString
 * @see #isTidyHTML
 */
public class FxHTML extends FxString {

    private static final long serialVersionUID = -3220473037731856950L;
    /**
     * tidy flag, if set
     */
    private boolean tidyHTML = false;

    /**
     * Content is run through JTidy (http://jtidy.sourceforge.net) before its saved
     *
     * @return if content is run through tidy before saving
     */
    public boolean isTidyHTML() {
        return tidyHTML;
    }

    /**
     * Enable or disable tidy before saving
     *
     * @param tidyHTML tidy flag
     * @return this
     */
    public FxHTML setTidyHTML(boolean tidyHTML) {
        this.tidyHTML = tidyHTML;
        return this;
    }

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     */
    public FxHTML(long defaultLanguage, boolean multiLanguage) {
        super(defaultLanguage, multiLanguage);
    }

    public FxHTML(boolean multiLanguage, Map<Long, String> translations) {
        super(multiLanguage, translations);
    }

    public FxHTML(boolean multiLanguage, long defaultLanguage, Map<Long, String> translations) {
        super(multiLanguage, defaultLanguage, translations);
    }

    public FxHTML(boolean multiLanguage, long defaultLanguage, String value) {
        super(multiLanguage, defaultLanguage, value);
    }

    public FxHTML(boolean multiLanguage, String value) {
        super(multiLanguage, value);
    }

    public FxHTML(Map<Long, String[]> translations, int pos) {
        super(translations, pos);
    }

    public FxHTML(FxHTML clone) {
        super(clone);
    }

    public FxHTML(FxString clone) {
        super(clone);
    }

    public FxHTML(Map<Long, String> translations) {
        super(translations);
    }

    public FxHTML(long defaultLanguage, Map<Long, String> translations) {
        super(defaultLanguage, translations);
    }

    public FxHTML(long defaultLanguage, String value) {
        super(defaultLanguage, value);
    }

    public FxHTML(String value) {
        super(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxHTML copy() {
        return new FxHTML(this).setTidyHTML(this.isTidyHTML());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<String> getValueClass() {
        return String.class;
	}
}
