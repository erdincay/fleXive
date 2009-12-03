package com.flexive.ejb.beans.configuration;

import com.flexive.shared.interfaces.ApplicationConfigurationEngine;
import com.flexive.shared.interfaces.ApplicationConfigurationEngineLocal;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.ParameterScope;
import com.flexive.core.DatabaseConst;

import javax.ejb.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Stateless(name = "ApplicationConfigurationEngine", mappedName="ApplicationConfigurationEngine")
public class ApplicationConfigurationEngineBean
        extends CustomDomainConfigurationImpl<String>
        implements ApplicationConfigurationEngine, ApplicationConfigurationEngineLocal {


    public ApplicationConfigurationEngineBean() {
        super("application", DatabaseConst.TBL_APPLICATION_CONFIG, "application_id", true);
    }

    @Override
    protected ParameterScope getDefaultScope() {
        return ParameterScope.APPLICATION;
    }

    @Override
    protected String getCurrentDomain() {
        return FxContext.get().getApplicationId();
    }

    @Override
    protected void setDomain(PreparedStatement stmt, int column, String domain) throws SQLException {
        stmt.setString(column, domain);
    }

    @Override
    protected String getDomain(ResultSet rs, int column) throws SQLException {
        return rs.getString(column);
    }

    @Override
    protected boolean mayUpdate() {
        return FxContext.getUserTicket().isGlobalSupervisor();
    }

    @Override
    protected boolean mayListDomains() {
        return true;    // list of applications can be seen by anyone
    }

    @Override
    protected boolean mayUpdateForeignDomains() {
        return FxContext.getUserTicket().isGlobalSupervisor();
    }
}
