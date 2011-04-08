package com.flexive.ejb.beans.configuration;

import com.flexive.core.DatabaseConst;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.ParameterScope;
import com.flexive.shared.interfaces.MandatorConfigurationEngine;
import com.flexive.shared.interfaces.MandatorConfigurationEngineLocal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

/**
 * Mandator configuration engine.
 * 
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Stateless(name = "MandatorConfigurationEngine", mappedName="MandatorConfigurationEngine")
public class MandatorConfigurationEngineBean extends CustomDomainConfigurationImpl<String>
        implements MandatorConfigurationEngine, MandatorConfigurationEngineLocal {

    public MandatorConfigurationEngineBean() {
        super("mandator", DatabaseConst.TBL_CONFIG_MANDATOR, "mandator_name", true);
    }

    @Override
    protected String getCurrentDomain() {
        return CacheAdmin.getEnvironment().getMandator(FxContext.getUserTicket().getMandatorId()).getName();
    }

    @Override
    protected String getDomain(ResultSet rs, int column) throws SQLException {
        return rs.getString(column);
    }

    @Override
    protected boolean mayListDomains() {
        // only global supervisor may see all mandators
        return FxContext.getUserTicket().isGlobalSupervisor();
    }

    @Override
    protected boolean mayUpdate() {
        return FxContext.getUserTicket().isMandatorSupervisor();
    }

    @Override
    protected boolean mayUpdateForeignDomains() {
        return FxContext.getUserTicket().isGlobalSupervisor();
    }

    @Override
    protected void setDomain(PreparedStatement stmt, int column, String domain) throws SQLException {
        stmt.setString(column, domain);
    }

    @Override
    protected ParameterScope getDefaultScope() {
        return ParameterScope.MANDATOR;
    }


}
