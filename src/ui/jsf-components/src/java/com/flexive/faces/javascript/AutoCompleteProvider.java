/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
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
package com.flexive.faces.javascript;

import com.flexive.war.JsonWriter;
import com.flexive.war.servlet.ThumbnailServlet;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.value.mapper.NumberQueryInputMapper;
import static com.flexive.shared.value.mapper.NumberQueryInputMapper.ReferenceQueryInputMapper;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.SortDirection;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.FxResultRow;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.Account;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Serializable;
import java.net.URLDecoder;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Provides autocomplete query methods for the fx:fxValueInput tag.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class AutoCompleteProvider implements Serializable {
    private static final long serialVersionUID = 8437543826523719386L;
    private static final int MAX_ITEMS = 10;

    private static class ResponseLine {
        private String value;
        private String label;

        private ResponseLine(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * Submits a query for users with the given login/account name.
     *
     * @param query the query to be submitted
     * @return  an autocomplete response
     * @throws com.flexive.shared.exceptions.FxApplicationException on application errors
     * @throws java.io.IOException  if the response could not be created
     */
    public String userQuery(String query) throws FxApplicationException, IOException {
        final List<Account> accounts = EJBLookup.getAccountEngine().loadAll();
        final List<ResponseLine> result = new ArrayList<ResponseLine>();
        final String ucQuery = query != null ? query.toUpperCase() : "";
        for (Account account : accounts) {
            if (match(account.getLoginName(), ucQuery) || match(account.getEmail(), ucQuery)) {
                result.add(new ResponseLine(account.getLoginName(), 
                        account.getLoginName() + " (" + account.getEmail() + ")"));
                if (result.size() > MAX_ITEMS) {
                    break;
                }
            }
        }
        return writeAutocompleteResponse(result);
    }


    /**
     * <p>Submits a query for contents, either by primary key (if a numeric value is given)
     * or by a prefix match on the caption. The output value has the format</p>
     * <code>"id.version - caption"</code>
     *
     * @param request   the current request (injected)
     * @param rawQuery the query, either a number (matching an instance ID) or a fulltext query
     * @param typeId    the type filter (set to -1 to disable)
     * @return  the autocomplete response
     * @throws com.flexive.shared.exceptions.FxApplicationException on application errors
     * @throws java.io.IOException  if the response could not be created
     */
    public String pkQuery(HttpServletRequest request, String rawQuery, long typeId) throws FxApplicationException, IOException {
        // get the actual text query also for "encoded" queries (that include the PK, which we don't need here)
        final String query = ReferenceQueryInputMapper.getReferencedContent(URLDecoder.decode(rawQuery, "UTF-8")).getCaption();
        final SqlQueryBuilder builder = new SqlQueryBuilder()
                .select("@pk", "caption")
                .orderBy("caption", SortDirection.ASCENDING);
        // filter by content type
        if (typeId != -1) {
            builder.type(typeId);
        }
        // prefix search in the caption property
        builder.orSub().condition("caption", PropertyValueComparator.LIKE, query + "%");
        if (StringUtils.isNumeric(query)) {
            // query by ID
            builder.condition("id", PropertyValueComparator.EQ, Long.parseLong(query));
        } 
        builder.closeSub();
        final List<ResponseLine> result = new ArrayList<ResponseLine>(MAX_ITEMS);
        for (FxResultRow row : builder.maxRows(MAX_ITEMS).getResult().getResultRows()) {
            final FxPK pk = row.getPk(1);
            final String caption = row.getString(2);
            final String value = pk + " - " + caption;
            final String label = "<div class=\"pkAutoComplete\"><img src=\""
                    + request.getContextPath()
                    + ThumbnailServlet.getLink(pk, BinaryDescriptor.PreviewSizes.PREVIEW1)
                    + "\"/>"
                    + "<div class=\"caption\">" + caption + "</div>"
                    + "<div class=\"property\">" + pk + "</div>"
                    + "</div>";
            result.add(new ResponseLine(value, label));
        }
        return writeAutocompleteResponse(result);
    }

    private boolean match(String value, String upperCaseQuery) {
        if (value == null) {
            return false;
        }
        final String ucValue = value.toUpperCase();
        return ucValue.startsWith(upperCaseQuery) || (upperCaseQuery.length() > 1 && ucValue.contains(upperCaseQuery));
    }


    private String writeAutocompleteResponse(List<ResponseLine> result) throws IOException {
        final StringWriter out = new StringWriter();
        final JsonWriter writer = new JsonWriter(out);
        writer.startArray();
        writeResponseLines(writer, result);
        return writer.closeArray().finishResponse().toString();
    }

    private void writeResponseLines(JsonWriter out, List<ResponseLine> responseLines) throws IOException {
        for (ResponseLine line : responseLines) {
            out.startArray().writeLiteral(line.getValue()).writeLiteral(line.getLabel()).closeArray();
        }
    }
}
