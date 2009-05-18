package com.flexive.ejb.beans.configuration;

import com.flexive.shared.interfaces.ApplicationConfigurationEngine;
import com.flexive.shared.interfaces.ApplicationConfigurationEngineLocal;
import com.flexive.shared.FxContext;
import com.flexive.core.DatabaseConst;

import javax.ejb.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Stateless(name = "ApplicationConfigurationEngine", mappedName="ApplicationConfigurationEngine")
public class ApplicationConfigurationEngineBean
        extends CustomIdConfigurationImpl
        implements ApplicationConfigurationEngine, ApplicationConfigurationEngineLocal {


    public ApplicationConfigurationEngineBean() {
        super("application", DatabaseConst.TBL_APPLICATION_CONFIG, "application_id", true);
    }
    
    @Override
    protected void setId(PreparedStatement stmt, int column) throws SQLException {
        stmt.setString(column, FxContext.get().getApplicationId());
    }

    @Override
    protected boolean mayUpdate() {
        return FxContext.getUserTicket().isGlobalSupervisor();
    }

}
