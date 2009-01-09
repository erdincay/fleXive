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
import org.quartz.utils.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A connection provider for the Quartz scheduler
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxQuartzConnectionProvider implements ConnectionProvider {

    private FxContext savedCtx;

    private int divisionId = -42;

    public void setDivisionId(String div) {
//        System.out.println("======> TX - Set division id to " + div);
        this.divisionId = Integer.parseInt(div);
    }

    /**
     * {@inheritDoc}
     */
    public Connection getConnection() throws SQLException {
//        System.out.println("Quartz requested a TX connection ("+divisionId+") ... Thread: " + Thread.currentThread() + "; I am: " + this);
        if (divisionId == -42) {
            if (savedCtx == null)
                savedCtx = FxContext.get();
            if (FxContext.getUserTicket() == null) {
                savedCtx.replace();
            }
            return Database.getDbConnection();
        } else
            return Database.getDbConnection(divisionId);
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() throws SQLException {
        //nothing to do for us
    }
}
