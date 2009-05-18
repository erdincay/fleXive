package com.flexive.ejb.beans.configuration;

import com.flexive.shared.interfaces.NodeConfigurationEngineLocal;
import com.flexive.shared.interfaces.NodeConfigurationEngine;
import com.flexive.shared.FxContext;
import com.flexive.core.DatabaseConst;

import javax.ejb.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Stateless(name = "NodeConfigurationEngine", mappedName = "NodeConfigurationEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class NodeConfigurationEngineBean extends CustomIdConfigurationImpl implements NodeConfigurationEngine, NodeConfigurationEngineLocal {
    private static final Log LOG = LogFactory.getLog(NodeConfigurationEngineBean.class);
    private static final String NODE_ID = _getHostName();

    public NodeConfigurationEngineBean() {
        super("node", DatabaseConst.TBL_NODE_CONFIG, "node_id", true);
    }

    @Override
    protected void setId(PreparedStatement stmt, int column) throws SQLException {
        stmt.setString(column, NODE_ID);
    }

    @Override
    protected boolean mayUpdate() {
        return FxContext.getUserTicket().isGlobalSupervisor();
    }

    /**
     * {@inheritDoc}
     */
    public String getNodeName() {
        return NODE_ID;
    }

    private static String _getHostName() {
        String id;
        try {
            id = StringUtils.defaultString(System.getProperty("flexive.nodename"), InetAddress.getLocalHost().getHostName());
            if (StringUtils.isBlank(id)) {
                id = "localhost";
                LOG.warn("Hostname was empty, using \"localhost\" (override with system property flexive.nodename.");
            }
        } catch (UnknownHostException e) {
            LOG.warn("Failed to determine node ID, using \"localhost\" (override with system property flexive.nodename): " + e.getMessage(), e);
            id = "localhost";
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Determined nodename (override with system property flexive.nodename): " + id);
        }
        return id;
    }
}
