/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.tests.embedded.cmis;

import com.flexive.core.search.cmis.parser.CmisSqlUtils;
import com.flexive.shared.exceptions.FxCmisSqlParseException;
import org.antlr.runtime.RecognitionException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Pure parser tests for the CMIS SQL parser.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class CmisSqlParserTest {

    @Test(groups = {"search", "cmis"})
    public void inTree() throws RecognitionException {
        for (String prefix: new String[] { "WHITE_PAPER, ", "" }) {
            for (String fun : new String[] { "IN_TREE", "IN_FOLDER" }) {
                assertValid("SELECT TITLE, AUTHORS, DATE\n"
                        + "FROM WHITE_PAPER\n"
                        + "WHERE (" + fun + "(" + prefix + "'ID00283213')) AND ('SMITH' = ANY AUTHORS)");
            }
        }
    }

    @Test(groups = {"search", "cmis"})
    public void containsScore() throws RecognitionException {
        assertValid("SELECT OBJECT_ID, SCORE() AS X, DESTINATION, DEPARTURE_DATES\n" +
                "FROM TRAVEL_BROCHURE\n" +
                "WHERE (CONTAINS('CARRIBEAN CRUISE TOUR')) AND\n" +
                "      ('2010-1-1' < ANY DEPARTURE_DATES)\n" +
                "ORDER BY X DESC"
        );
    }

    @Test(groups = {"search", "cmis"})
    public void joinLike() throws RecognitionException {
        assertValid("SELECT Y.CLAIM_NUM, X.PROPERTY_ADDRESS, Y.DAMAGE_ESTIMATES\n" +
                "FROM POLICY AS X JOIN CLAIMS AS Y ON (X.POLICE_NUM = Y.POLICY_NUM)\n" +
                "WHERE (10000 <= ANY Y.DAMAGE_ESTIMATES) AND (Y.CASE NOT LIKE '%Katrina%')"
        );
    }

    @Test(groups = {"search", "cmis"})
    public void validQueries() {
        final String[] queries = {
                "SELECT X FROM TABLE",
                "SELECT X.Y FROM TABLE AS X",
                "SELECT X.Y FROM TABLE AS X JOIN TABLE2 AS Z",
                "SELECT X FROM Y WHERE (X > 21)",
                "SELECT person.* FROM person",
                "SELECT x FROM table WHERE x < 10 AND x > 5 AND z = 'abc'",
                "SELECT x FROM table WHERE x IN (1,2,3)",
                "SELECT x FROM table WHERE x NOT IN (1,2,3)",
                "SELECT x FROM table WHERE x NOT IN ('apple', 'peach', 'grapefruit')",
                "SELECT UPPER(x) FROM table",
                "SELECT * FROM TABLE",
                "SELECT a.*, b.* FROM TABLE AS a JOIN TABLE2 AS b ON (a.id = b.id)"
        };
        for (String query : queries) {
            assertValid(query);
        }
    }

    @Test(groups = {"search", "cmis"})
    public void invalidQueries() {
        final String[] queries = {
                "SELECT X()",               // invalid fun
                "SELECT X FROM Y WHERE",    // empty where clause
                "SELECT X.Y FROM TABLE X",  // invalid table alias
                "SELECT X.Y FROM TABLE AS X JOIN TABLE2 Y", // invalid alias for table2
                "SELECT X FROM Y WHERE (X > 21",    // unmatched parens
                "SELECT X FROM Y WHERE X < 21)",    // unmatched parens
        };
        for (String query : queries) {
            assertInvalid(query);
        }
    }

    private void assertValid(String query) {
        try {
            System.out.println("Query:\n" + query);
            System.out.println("--> " + CmisSqlUtils.parse(query).toStringTree());
        } catch (FxCmisSqlParseException e) {
            assertTrue(false, "Query should be valid, but the parser returned an error:\n"
                    + query + "\n\nError message:" + e.getMessage());
        }
    }

    private void assertInvalid(String query) {
        try {
            CmisSqlUtils.parse(query);
            assertTrue(false, "Query should be invalid, but the parser returned no errors:\n" + query);
        } catch (FxCmisSqlParseException e) {
            // pass
        }
    }
}
