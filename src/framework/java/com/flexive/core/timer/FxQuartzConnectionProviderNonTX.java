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
package com.flexive.core.timer;

import com.flexive.core.Database;
import org.quartz.utils.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A connection provider for the Quartz scheduler that serves non tx managed connections
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxQuartzConnectionProviderNonTX implements ConnectionProvider {

    /**
     * Context from the parent thread
     */
//    private volatile FxContext savedCtx = null;
    private int divisionId = -42;

    public void setDivisionId(String div) {
//        System.out.println("======> Set division id to "+div);
        this.divisionId = Integer.parseInt(div);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection getConnection() throws SQLException {
        if(divisionId == -42)
            throw new SQLException("No divisionId configured for "+FxQuartzConnectionProviderNonTX.class.getCanonicalName());
//        System.out.println("===> REQUESTED CON FOR "+divisionId);
        /*if( savedCtx == null )
            savedCtx = FxContext.get();
        if(FxContext.getUserTicket() == null ) {
//            System.out.println("Replacing context");
            savedCtx.replace();
        }*/
//        System.out.println("Quartz requested a non-TX connection ... Thread: " + Thread.currentThread() + "; I am: " + this);
//        System.out.println("Ctx-Info -> Division: " + FxContext.get().getDivisionId() + " Ticket: " + FxContext.getUserTicket());

        return Database.getNonTXDataSource(divisionId).getConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() throws SQLException {
        //nothing to do for us
    }
}