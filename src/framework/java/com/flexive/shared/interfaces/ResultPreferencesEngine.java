/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.ResultLocation;
import com.flexive.shared.search.ResultPreferences;
import com.flexive.shared.search.ResultViewType;

import javax.ejb.Remote;

/**
 * Interface for retrieving and updating the displayed result properties.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Remote
public interface ResultPreferencesEngine {

    /**
     * Loads the result preferences for the given type for the current user. When no preferences are
     * defined for the given parameters, the default preferences are returned.
     *
     * @param typeId    the content type ID, or -1 for the "overall" properties
     * @param viewType  the view type (list, thumbs)
     * @param location      the "location" where the results will be displayed (e.g. the main admin result pages)
     * @return  the result preferences for the given type for the current user.
     * @throws com.flexive.shared.exceptions.FxApplicationException if the result preferences could not be loaded
     */
    ResultPreferences load(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException;

    /**
     * Loads the system default result preferences for the given type. When no preferences are
     * defined for the given parameters, an empty {@link ResultPreferences} object is returned.
     *
     * @param typeId    the content type ID, or -1 for the "overall" properties
     * @param viewType  the view type (list, thumbs)
     * @param location      the "location" where the results will be displayed (e.g. the main admin result pages)
     * @return  the system default result preferences for the given type.
     * @throws com.flexive.shared.exceptions.FxApplicationException if the result preferences could not be loaded
     */
    ResultPreferences loadSystemDefault(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException;

    /**
     * Returns true when the user actually stored a configuration for the given parameters. Used to check
     * if fallbacks were used when loading preferences via {@link #load}.
     *
     * @param typeId    the content type ID, or -1 for the "overall" properties
     * @param viewType  the view type (list, thumbs)
     * @param location      the "location" where the results will be displayed (e.g. the main admin result pages)
     * @return  true when the user actually stored a configuration for the given parameters
     * @throws FxApplicationException   on errors
     */
    boolean isCustomized(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException;

    /**
     * Save the given result preferences for the current user.
     *
     * @param preferences   the preferences to be saved
     * @param typeId        the content type ID
     * @param viewType      the view type (list, thumbs)
     * @param location      the "location" where the results will be displayed
     * @throws FxApplicationException   if the result preferences could not be updated
     */
    void save(ResultPreferences preferences, long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException;

    /**
     * Save the given result preferences as the default settings for the given type, viewtype, and location.
     * Only the global supervisor can do this.
     *
     * @param preferences   the preferences to be saved
     * @param typeId        the content type ID
     * @param viewType      the view type (list, thumbs)
     * @param location      the "location" where the results will be displayed
     * @throws FxApplicationException   if the result preferences could not be updated
     */
    void saveSystemDefault(ResultPreferences preferences, long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException;

    /**
     * Remove the current user's preferences for the given parameters. If no preferences exist
     * for the given parameters, no action is performed.
     *
     * @param typeId    the content type ID, or -1 for the "overall" properties
     * @param viewType  the view type (list, thumbs)
     * @param location      the "location" where the results will be displayed (e.g. the main admin result pages)
     * @throws FxApplicationException   if the user's preferences could not be removed
     */
    void remove(long typeId, ResultViewType viewType, ResultLocation location) throws FxApplicationException;
}
