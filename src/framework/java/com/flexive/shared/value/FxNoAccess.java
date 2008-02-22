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

import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.security.UserTicket;

/**
 * A replacement for FxValue objects if the calling user has no access to this value
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public final class FxNoAccess extends FxValue<Object, FxNoAccess> {

    private static final long serialVersionUID = 7693998143844598522L;

    private String noAccess;
    /**
     * the wrapped value
     */
    private FxValue wrappedValue;
    private UserTicket ticket;

    /**
     * Constructor
     *
     * @param multiLanguage   multilanguage value?
     * @param defaultLanguage the default language
     */
    public FxNoAccess(long defaultLanguage, boolean multiLanguage) {
        super(defaultLanguage, multiLanguage);
    }

    /**
     * Constructor
     *
     * @param ticket       to get the correct tranlsation of the no access text (if null a fallback will be used)
     * @param wrappedValue FxValue to wrap
     */
    public FxNoAccess(UserTicket ticket, FxValue wrappedValue) {
        super(wrappedValue.getDefaultLanguage(), wrappedValue.isMultiLanguage());
        this.wrappedValue = wrappedValue;
        this.ticket = ticket;
        if (ticket != null)
            this.noAccess = FxSharedUtils.getLocalizedMessage(FxSharedUtils.SHARED_BUNDLE, ticket.getLanguage().getId(),
                    ticket.getLanguage().getIso2digit(), "shared.noAccess");
        else
            this.noAccess = "Access denied";
    }

    /**
     * Get the wrapped value ie for save operations.
     * The real wrapped value is only returned if the calling user is a global supervisor, else the
     * FxNoAccess value itself is returned
     *
     * @return wrapped value or this FxNoAccess value depending on the calling user
     */
    public FxValue getWrappedValue() {
        return FxContext.get().getRunAsSystem() ? wrappedValue : this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long getDefaultLanguage() {
        return wrappedValue.getDefaultLanguage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultTranslation() {
        return noAccess;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTranslation(FxLanguage lang) {
        return noAccess;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBestTranslation(long lang) {
        return noAccess;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long[] getTranslatedLanguages() {
        return wrappedValue.getTranslatedLanguages();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean translationExists(long languageId) {
        return wrappedValue.translationExists(languageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return wrappedValue.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultiLanguage() {
        return wrappedValue.isMultiLanguage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object fromString(String value) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxNoAccess copy() {
        return new FxNoAccess(ticket, wrappedValue);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public Class getValueClass() {
        return wrappedValue.getValueClass();
    }
}
