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
package com.flexive.shared;

import com.flexive.shared.value.FxString;

import java.io.Serializable;
import java.util.Locale;

/**
 * Languages
 * Provides mapping functions between language system constants , iso codes and english descriptions.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxLanguage extends AbstractSelectableObjectWithName implements Serializable, SelectableObjectWithLabel {
    private static final long serialVersionUID = -7060915362180672721L;
    /**
     * language set for groups or not localized properties
     */
    public static final long SYSTEM_ID = 0;
    /**
     * default language (english)
     */
    public static final long DEFAULT_ID = 1;
    /**
     * default language ISO code
     */
    public static final String DEFAULT_ISO = "en";
    /**
     * default language object
     */
    public static final FxLanguage DEFAULT = new FxLanguage(DEFAULT_ID, DEFAULT_ISO, new FxString("English"), true);

    private long id;
    private String iso2digit;
    private FxString name;
    private boolean licensed;

    /**
     * pre defined language english
     */
    public static final long ENGLISH = 1;

    /**
     * pre defined language german
     */
    public static final long GERMAN = 2;

    /**
     * pre defined language french
     */
    public static final long FRENCH = 3;

    /**
     * pre defined language italian
     */
    public static final long ITALIAN = 4;

    /**
     * Language constructor.
     *
     * @param id        the language ID
     * @param iso2digit the 2-digit ISO code (en, de, fr, ...)
     * @param name      the localized language name
     * @param licensed  if the language is licensed for the current server installation
     */
    public FxLanguage(long id, String iso2digit, FxString name, boolean licensed) {
        this.id = id;
        this.iso2digit = iso2digit;
        this.licensed = licensed;
        this.name = name;
    }

    public FxLanguage(String localeIso) {
        if (localeIso.startsWith("en")) {
            this.id = ENGLISH;
            this.name = new FxString("English");
        } else if (localeIso.startsWith("de")) {
            this.id = GERMAN;
            this.name = new FxString("German");
        } else if (localeIso.startsWith("fr")) {
            this.id = FRENCH;
            this.name = new FxString("French");
        } else if (localeIso.startsWith("it")) {
            this.id = ITALIAN;
            this.name = new FxString("Italian");
        }
        this.iso2digit = localeIso.substring(0, 2);
    }

    public long getId() {
        return id;
    }

    public String getIso2digit() {
        return iso2digit;
    }

    public Locale getLocale() {
        return new Locale(getIso2digit());
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name.getDefaultTranslation();
    }

    /**
     * {@inheritDoc}
     */
    public FxString getLabel() {
        return name;
    }

    public boolean isLicensed() {
        return licensed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return iso2digit + "(" + name + ")";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FxLanguage)) return false;
        FxLanguage comp = (FxLanguage) obj;
        return (comp.getIso2digit().equals(this.getIso2digit()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return iso2digit.hashCode();
    }


}
