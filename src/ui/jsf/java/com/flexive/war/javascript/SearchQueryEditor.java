package com.flexive.war.javascript;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.search.AdminResultLocations;
import static com.flexive.shared.search.AdminResultLocations.ADMIN;
import com.flexive.war.JsonWriter;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements JSON/RPC methods for the search query panel.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class SearchQueryEditor implements Serializable {
    private static final long serialVersionUID = -1665068866212914870L;

    /**
     * Renders all stored search queries of the calling user.
     *
     * @return all search queries of the calling user (in JSON format).
     * @throws Exception on server-side errors
     */
    public String renderSearchQueries() throws Exception {
        final StringWriter out = new StringWriter();
        final JsonWriter writer = new JsonWriter(out);
        writer.startArray();
        final List<String> names = new ArrayList<String>(EJBLookup.getSearchEngine().loadNames(ADMIN));
        Collections.sort(names);
        for (String name: names) {
            writer.startMap();
            writer.writeAttribute("name", name);
            writer.closeMap();
        }
        writer.closeArray().finishResponse();
        return out.toString();
    }

    /**
     * Remove an admin search query definition.
     *
     * @param name  the query name
     * @return  nothing
     * @throws Exception    if the query was not found or could not be deleted
     */
    public String remove(String name) throws Exception {
        EJBLookup.getSearchEngine().remove(ADMIN, name);
        return "[]";
    }
}
