package com.flexive.examples.tutorial01;

import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.SortDirection;
import com.flexive.shared.search.query.SqlQueryBuilder;

import javax.faces.model.DataModel;

/**
 * JSF managed bean for the tutorial01 application.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class Tutorial01Bean {
    private DataModel documents;

    public DataModel getDocuments() throws FxApplicationException {
        if (documents == null) {
            final FxResultSet result = new SqlQueryBuilder()
                    .select("@pk", "document01/file", "caption", "created_at")
                    .type("document01")
                    .orderBy("created_at", SortDirection.DESCENDING)
                    .getResult();
            documents = new FxResultSetDataModel(result);
        }
        return documents;
    }
}
