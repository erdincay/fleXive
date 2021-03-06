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
package com.flexive.shared.interfaces;

import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxApplicationException;

import javax.ejb.Remote;
import java.util.List;

/**
 * [fleXive] language engine interface.
 * This engine should not be used to load languages as they are available from the environment!
 * Its purpose is to enable/disable, initially load and manage (position, etc.) languages
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Remote
public interface LanguageEngine {

    /**
     * Loads a language defined by its unique id.
     *
     * @param languageId the unqiue id of the language to load
     * @return the language object
     * @throws FxApplicationException on errors
     */
    FxLanguage load(long languageId) throws FxApplicationException;

    /**
     * Loads a language defined by is  iso code.
     *
     * @param languageIsoCode the iso code of the language to load
     * @return the language object
     * @throws FxApplicationException on errors
     */
    FxLanguage load(String languageIsoCode) throws FxApplicationException;

    /**
     * Loads all available languages.
     *
     * @return an array with all available languages
     * @throws FxApplicationException on errors
     */
    List<FxLanguage> loadAvailable() throws FxApplicationException;

    /**
     * Loads all disabled languages.
     *
     * @return a list with all disabled languages
     * @throws FxApplicationException on errors
     */
    List<FxLanguage> loadDisabled() throws FxApplicationException;

    /**
     * Loads all available languages.
     *
     * @param excludeSystemLanguage if true the system language is exluded from the result
     * @return a list with all available language objects
     * @throws FxApplicationException if the function fails
     */
    public List<FxLanguage> loadAvailable(boolean excludeSystemLanguage) throws FxApplicationException;

    /**
     * Returns true if the language referenced by its unique id is valid.
     * <p/>
     * A language is valid if it may be used according to the used license.
     *
     * @param languageId the unqique id of the language to check
     * @return a array with all available language objects
     */
    boolean isValid(long languageId);

    /**
     * Activate a language (convenience method)
     *
     * @param language the language to activate
     * @throws FxApplicationException on errors
     * @since 3.1.4
     */
    void activateLanguage(FxLanguage language) throws FxApplicationException;

    /**
     * Set all available languages
     *
     * @param available   list containing all available languages
     * @param ignoreUsage ignore if a language that is no longer available after calling this method is in use
     * @throws FxApplicationException on errors
     */
    public void setAvailable(List<FxLanguage> available, boolean ignoreUsage) throws FxApplicationException;
}
