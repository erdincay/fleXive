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
package com.flexive.core.storage.genericSQL;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxCreateException;
import com.flexive.shared.FxSystemSequencer;
import com.flexive.core.storage.SequencerStorage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generic sequencer storage implementation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public abstract class GenericSequencerStorage implements SequencerStorage {
    private static final Log LOG = LogFactory.getLog(GenericSequencerStorage.class);


    /**
     * {@inheritDoc}
     */
    public long getId(FxSystemSequencer type) throws FxApplicationException {
        return fetchId(type.getSequencerName(), type.isAllowRollover());
    }

    /**
     * {@inheritDoc}
     */
    public long getId(String sequencer) throws FxApplicationException {
        if (StringUtils.isEmpty(sequencer) || sequencer.toUpperCase().trim().startsWith("SYS_"))
            throw new FxCreateException(LOG, "ex.sequencer.fetch.invalid", sequencer);
        return fetchId(sequencer, false);
    }

}
