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
package com.flexive.shared.interfaces;

import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxApplicationException;

import javax.ejb.Remote;
import java.util.ArrayList;

/**
 * Flexive language interface.
 *
 * @author UCS
 */
@Remote
public interface LanguageEngine {

    /**
     * Loads a language defined by its unique id.
     *
     * @param languageId the unqiue id of the language to load
     * @return the language object
     * @throws FxApplicationException TODO
     *
     */
    FxLanguage load(long languageId) throws FxApplicationException;

    /**
     * Loads a language defined by is  iso code.
     *
     * @param languageIsoCode the iso code of the language to load
     * @return the language object
     * @throws FxApplicationException TODO
     */
    FxLanguage load(String languageIsoCode) throws FxApplicationException;

    /**
     * Loads all available languages.
     *
     * @return a array with all available language objects
     * @throws FxApplicationException TODO
     */
    FxLanguage[] loadAvailable() throws FxApplicationException;

    /**
     * Loads all available languages.
     *
     * @param excludeSystemLanguage if true the system language is exluded from the result
     * @return a array with all available language objects
     * @throws FxApplicationException if the function fails
     */
    public ArrayList<FxLanguage> loadAvailable(boolean excludeSystemLanguage) throws FxApplicationException;

    /**
     * Returns true if the language referenced by its unique id is valid.
     * <p/>
     * A language is valid if it may be used according to the used license.
     *
     * @param languageId the unqique id of the language to check
     * @return a array with all available language objects
     */
    boolean isValid(long languageId);
}
