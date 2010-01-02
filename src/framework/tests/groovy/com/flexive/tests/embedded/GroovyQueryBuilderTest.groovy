/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.tests.embedded

import com.flexive.shared.scripting.groovy.GroovyQueryBuilder
import com.flexive.shared.search.query.SqlQueryBuilder
import com.flexive.shared.FxFormatUtils;
import org.testng.annotations.Test
import com.flexive.shared.value.FxDateTime
import org.testng.Assert

/**
 * Test scripts for the query builder test bean
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
class GroovyQueryBuilderTest {

    @Test (groups = ["scripting", "ejb", "search"])
    def simpleBuilderTest() {
        def root = new GroovyQueryBuilder().select(["@pk", "caption", "*"]) {
            eq("caption", "bla")
            not_empty("filename")
            or {
                gt("id", 0)
                lt("id", 100)
            }
            lt("created_at", new Date(0))
        }
        def cond = "(CAPTION = 'bla' AND FILENAME IS NOT NULL AND (ID > 0 OR ID < 100) AND CREATED_AT < " + FxFormatUtils.escapeForSql(new FxDateTime(new Date(0))) + ")"
        def result = root.sqlQuery
        Assert.assertTrue(result.toUpperCase().indexOf(cond.toUpperCase()) > 0, "Expected condition: " + cond + ", \ngot:\n" + result)
    }

    @Test (groups = ["scripting", "ejb", "search"])
    def repeatableSqlQueryTest() {
        def root = new GroovyQueryBuilder().select(["caption"]) {
            like("caption", "test%")
            gt("id", 0)
        }
        def query = root.sqlQuery
        def query2 = root.sqlQuery
        Assert.assertEquals(query, query2, "Second call to getSqlQuery() returned a different result, \n[1]: " + query + "\n[2]: " + query2)
    }

    @Test (groups = ["scripting", "ejb", "search"])
    def filterBriefcaseTest() {
        def root = new GroovyQueryBuilder().select(["@pk"]) {
            filterBriefcase(21)
        }
        def query = root.sqlQuery
        Assert.assertTrue(query.indexOf("briefcase=21") > 0, "Query should contain briefcase=21 filter: " + query)
        // check if search can be submitted
        root.queryBuilder.getResult()
    }

    @Test (groups = ["scripting", "ejb", "search"])
    def filterBriefcaseTestExternalBuilder() {
        def builder = new SqlQueryBuilder()
        new GroovyQueryBuilder(builder).select(["@pk"]) {
            filterBriefcase(21)
        }
        Assert.assertTrue(builder.query.indexOf("briefcase=21") > 0, "Query should contain briefcase=21 filter: " + builder.query)
        // check if search can be submitted
        builder.getResult()
    }
}
