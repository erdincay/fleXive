/** *************************************************************
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
 ************************************************************** */
package com.flexive.tests.embedded

import com.flexive.shared.scripting.groovy.GroovySelectListBuilder
import com.flexive.shared.structure.FxSelectListEdit
import com.flexive.shared.value.FxString
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Tests for the   {@link com.flexive.shared.scripting.groovy.GroovySelectListBuilder GroovySelectListBuilder}   class.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class GroovySelectListBuilderTest {
  @BeforeClass (groups = ["ejb", "scripting", "structure"])
  void beforeClass() {
    com.flexive.tests.embedded.FxTestUtils.login(TestUsers.SUPERVISOR);
  }

  @AfterClass (groups = ["ejb", "scripting", "structure"])
  void afterClass() {
    com.flexive.tests.embedded.FxTestUtils.logout()
  }

  @Test (groups = ["ejb", "scripting", "structure"])
  void simpleListBuilder() {
    def builderList = new GroovySelectListBuilder().testList(
            name: "TestList",
            label: new FxString("List Label"),
            description: new FxString("List description"),
            dynamicCreation: false,
            breadcrumbSeparator: " >> ",
            selectOnlySameLevel: true) {
      element1(
              name: "TestListElement1",
              label: new FxString("List element1 Label"),
              data: "ListElement1Data",
              color: "#112233") {
        element1_1(
                name: "TestListElement1_1",
                label: new FxString("List element1_1 Label"),
                data: "ListElement1_1Data",
                color: "#112233")
      }
      element2(
              name: "TestListElement2",
              label: new FxString("List element2 Label"),
              data: "ListElement2Data",
              color: "#112233")
    }
    Assert.assertTrue(builderList != null, "No list created")
    Assert.assertTrue(builderList instanceof FxSelectListEdit, "Expected list to be instance of FxSelectListEdit")
    FxSelectListEdit list = (FxSelectListEdit) builderList;
    Assert.assertEquals(list.getName(), "TestList");
    Assert.assertEquals(list.getItems().size(), 3);
    Assert.assertNotNull(list.getItem("TestListElement1_1"), "Item TestListElement1_1does not exist");
    Assert.assertEquals(list.getItem("TestListElement1_1").getParentItem(), list.getItem("TestListElement1"), "Wrong parent item");
  }
}