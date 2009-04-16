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
import java.util.HashMap;
import java.util.Map;
import javax.ejb.Stateless;

/**
 * EJB 3.1 example bean retrieving some content from a fleXive EJB.
 */
@Stateless(name = "EJBExample")
public class EJBExampleBean {

    public Map<FxType, Integer> getInstanceCounts() throws FxApplicationException {
        final Map<FxType, Integer> result = new HashMap<FxType, Integer>();
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        for (FxFoundType foundType : new SqlQueryBuilder().select("@pk").getResult().getContentTypes()) {
            result.put(environment.getType(foundType.getContentTypeId()), foundType.getFoundEntries());
        }
        return result;
    }

}
