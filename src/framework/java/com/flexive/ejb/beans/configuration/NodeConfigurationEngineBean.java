/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.ejb.beans.configuration;

import com.flexive.core.DatabaseConst;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.configuration.ParameterScope;
import com.flexive.shared.interfaces.NodeConfigurationEngine;
import com.flexive.shared.interfaces.NodeConfigurationEngineLocal;

import javax.ejb.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * NodeConfigurationEngine implementation
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
@Stateless(name = "NodeConfigurationEngine", mappedName = "NodeConfigurationEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class NodeConfigurationEngineBean extends CustomDomainConfigurationImpl<String> implements NodeConfigurationEngine, NodeConfigurationEngineLocal {
    /**
     * Ctor
     */
    public NodeConfigurationEngineBean() {
        super("node", DatabaseConst.TBL_CONFIG_NODE, "node_id", true);
    }


    @Override
    protected ParameterScope getDefaultScope() {
        return ParameterScope.NODE;
    }

    @Override
    public String getCurrentDomain() {
        return FxSharedUtils.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setDomain(PreparedStatement stmt, int column, String domains) throws SQLException {
        stmt.setString(column, domains);
    }

    @Override
    protected String getDomain(ResultSet rs, int column) throws SQLException {
        return rs.getString(column);
    }

    /**
     * {@inheritDoc}
     */
    public String getNodeName() {
        return getCurrentDomain();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean mayListDomains() {
        return true;    // list of nodes may be seen by anyone
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean mayUpdate() {
        return FxContext.getUserTicket().isGlobalSupervisor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean mayUpdateForeignDomains() {
        return FxContext.getUserTicket().isGlobalSupervisor();
    }
}
