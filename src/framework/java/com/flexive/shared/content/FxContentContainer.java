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
package com.flexive.shared.content;

import com.flexive.shared.exceptions.FxNotFoundException;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * FxContentContainer is a container for every existing version of a FxContent.
 * It is similar to <code>FxCachedContentContainer</code> which is used internally and which only contains
 * previsously requested versions.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @see FxCachedContentContainer
 */
public class FxContentContainer implements Serializable {

    private static final long serialVersionUID = -3814666243342672605L;
    private FxContentVersionInfo versionInfo;
    //list is ordered by version
    private List<FxContent> content;

    /**
     * Ctor
     *
     * @param versionInfo version info
     * @param content     list of contents, ordered by version
     */
    public FxContentContainer(FxContentVersionInfo versionInfo, List<FxContent> content) {
        this.versionInfo = versionInfo;
        this.content = content;
    }

    /**
     * Get version specific information
     *
     * @return version specific information
     */
    public FxContentVersionInfo getVersionInfo() {
        return versionInfo;
    }

    /**
     * Get an Iterator for all versions (in order)
     *
     * @return iterator for all versions (in order)
     */
    public Iterator<FxContent> getVersions() {
        return content.iterator();
    }

    /**
     * Get a requested version of the content
     *
     * @param version requested version
     * @return FxContent
     * @throws FxNotFoundException if the version does not exist
     */
    public FxContent getVersion(int version) throws FxNotFoundException {
        for (FxContent co : content)
            if (co.getVersion() == version)
                return co;
        throw new FxNotFoundException("ex.content.notFound", new FxPK(versionInfo.getId(), version));
    }
}
