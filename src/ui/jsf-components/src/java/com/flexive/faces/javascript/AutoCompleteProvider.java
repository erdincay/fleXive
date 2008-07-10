/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.Account;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Provides autocomplete query methods for the fx:fxValueInput tag.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class AutoCompleteProvider {
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
        final Account[] accounts = EJBLookup.getAccountEngine().loadAll();
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
        final StringWriter out = new StringWriter();
        final JsonWriter writer = new JsonWriter(out);
        writer.startArray();
        writeResponseLines(writer, result);
        return writer.closeArray().finishResponse().toString();
    }

    private boolean match(String value, String upperCaseQuery) {
        if (value == null) {
            return false;
        }
        final String ucValue = value.toUpperCase();
        return ucValue.startsWith(upperCaseQuery) || (upperCaseQuery.length() > 1 && ucValue.contains(upperCaseQuery));
    }

    private void writeResponseLines(JsonWriter out, List<ResponseLine> responseLines) throws IOException {
        for (ResponseLine line : responseLines) {
            out.startArray().writeLiteral(line.getValue()).writeLiteral(line.getLabel()).closeArray();
        }
    }
}
