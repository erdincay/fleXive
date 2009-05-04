#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxFoundType;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxType;

import javax.ejb.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A demo EJB bean.
 */
@Stateless(name = "EJBExample")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class EJBExampleBean implements EJBExample, EJBExampleLocal {

    /**
     * {@inheritDoc}
     */
    public Map<FxType, Integer> getInstanceCounts() throws FxApplicationException {
        final Map<FxType, Integer> result = new HashMap<FxType, Integer>();
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        for (FxFoundType foundType : new SqlQueryBuilder().select("@pk").getResult().getContentTypes()) {
            result.put(environment.getType(foundType.getContentTypeId()), foundType.getFoundEntries());
        }
        return result;
    }
}
