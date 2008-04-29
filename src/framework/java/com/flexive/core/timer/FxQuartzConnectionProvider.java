/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.core.timer;

import com.flexive.core.Database;
import com.flexive.shared.FxContext;

import java.sql.SQLException;
import java.sql.Connection;

import org.quartz.utils.ConnectionProvider;

/**
 * A connection provider for the Quartz scheduler
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxQuartzConnectionProvider implements ConnectionProvider {

    private FxContext savedCtx;

    /**
     * {@inheritDoc}
     */
    public Connection getConnection() throws SQLException {
//        System.out.println("Quartz requested a TX connection ... Thread: " + Thread.currentThread() + "; I am: " + this);
        if( savedCtx == null )
            savedCtx = FxContext.get();
        if(FxContext.get().getTicket() == null ) {
            FxContext.replace(savedCtx);
        }

        return Database.getDbConnection();
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() throws SQLException {
        //nothing to do for us
    }
}
