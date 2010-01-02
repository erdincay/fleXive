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
package com.flexive.ejb.beans.configuration;

import com.flexive.core.DatabaseConst;
import com.flexive.shared.FxContext;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.configuration.ParameterScope;
import com.flexive.shared.interfaces.UserConfigurationEngine;
import com.flexive.shared.interfaces.UserConfigurationEngineLocal;

import javax.ejb.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * User configuration. Currently no security checks are included - a user
 * may always update/delete own parameters (and never modify parameters of other users).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Stateless(name = "UserConfigurationEngine", mappedName="UserConfigurationEngine")
public class UserConfigurationEngineBean extends CustomDomainConfigurationImpl<Long> implements UserConfigurationEngine, UserConfigurationEngineLocal {

    public UserConfigurationEngineBean() {
        super("user", DatabaseConst.TBL_USER_CONFIG, "user_id", true);
    }

    @Override
    protected ParameterScope getDefaultScope() {
        return ParameterScope.USER;
    }

    @Override
    protected Long getCurrentDomain() {
        return FxContext.get().getTicket().getUserId();
    }

    @Override
    protected void setDomain(PreparedStatement stmt, int column, Long domains) throws SQLException {
        stmt.setLong(column, domains);
    }

    @Override
    protected Long getDomain(ResultSet rs, int column) throws SQLException {
        return rs.getLong(column);
    }

    @Override
    protected boolean mayUpdate() {
        final UserTicket ticket = FxContext.getUserTicket();
        return !ticket.isGuest() || ticket.isGlobalSupervisor();
    }

    @Override
    protected boolean mayListDomains() {
        // only the global supervisor may get a list of (potentially) all user IDs
        return FxContext.getUserTicket().isGlobalSupervisor();
    }

    @Override
    protected boolean mayUpdateForeignDomains() {
        return FxContext.getUserTicket().isGlobalSupervisor();
    }
}
