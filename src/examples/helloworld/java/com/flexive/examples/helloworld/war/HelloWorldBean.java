package com.flexive.examples.helloworld.war;

import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.SortDirection;
import com.flexive.shared.search.query.SqlQueryBuilder;

import javax.faces.model.DataModel;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class HelloWorldBean {
    private DataModel blogEntries;

    public DataModel getBlogEntries() throws FxApplicationException {
        if (blogEntries == null) {
            final FxResultSet result = new SqlQueryBuilder()
                    .select("@pk", "entryTitle", "entryText", "created_at")
                    .type("blogEntry")
                    .orderBy("created_at", SortDirection.DESCENDING)
                    .getResult();
            blogEntries = new FxResultSetDataModel(result);
        }
        return blogEntries;
    }
}
