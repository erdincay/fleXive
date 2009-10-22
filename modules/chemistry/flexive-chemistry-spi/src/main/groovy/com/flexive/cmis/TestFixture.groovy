/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.cmis

import com.flexive.cmis.spi.FlexiveRepository
import com.flexive.shared.CacheAdmin
import com.flexive.shared.EJBLookup
import com.flexive.shared.configuration.SystemParameters
import com.flexive.shared.scripting.groovy.GroovyTypeBuilder
import com.flexive.shared.structure.FxDataType
import com.flexive.shared.tree.FxTreeMode
import com.flexive.shared.value.FxString
import org.apache.chemistry.Connection
import org.apache.chemistry.Document
import org.apache.chemistry.Folder
import org.apache.chemistry.Repository
import com.flexive.shared.exceptions.FxRuntimeException
import org.apache.chemistry.ContentStream
import org.apache.chemistry.impl.simple.SimpleContentStream
import com.flexive.shared.FxContext
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * An utility class for creating the test fixture for Chemistry's basic test suite.
 * Adapted from org.apache.chemistry.test.BasicHelper.makeRepository
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class TestFixture {
  private static final Log LOG = LogFactory.getLog(TestFixture.class)
  private static initialized = false

  def TestFixture() {
    synchronized (TestFixture.class) {
      if (!initialized) {
        System.setProperty("openejb.base", "../openejb/")
        FxContext.initializeSystem(1, "chemistry-spi-tests")
        initialized = true;

        LOG.info("Creating CMIS/Chemistry test fixture...")
        // create test fixture
        try {
          FxContext.startRunningAsSystem();
          create(true);
        } catch (Exception e) {
          e.printStackTrace(System.err);
          throw new RuntimeException(e);
        } finally {
          FxContext.stopRunningAsSystem();
        }
        LOG.info("Test fixture created successfully.")
      }
    }
  }

  def create(boolean keepStructures) {
    wipe(keepStructures)
    createStructures()
    createData()
  }

  def wipe(boolean keepStructures) {
    // remove old instances of the structures and all their instances
    def env = CacheAdmin.environment
    try {
      EJBLookup.contentEngine.removeForType(env.getType("fold").id)
      EJBLookup.contentEngine.removeForType(env.getType("doc").id)
      if (!keepStructures) {
        EJBLookup.typeEngine.remove(env.getType("fold").id)
        EJBLookup.typeEngine.remove(env.getType("doc").id)
      }
    } catch (FxRuntimeException e) {
      // no structures found
    }
  }

  def createStructures() {
    try {
      CacheAdmin.environment.getType("fold")
    } catch (FxRuntimeException e) {
      // Fold type
      new GroovyTypeBuilder().fold(parentTypeName: "FOLDER", label: new FxString("Fold"), description: new FxString("My Folder Type")) {
        title(
                label: new FxString("Title"),
                defaultValue: new FxString(false, "(no title)")
        )
        description(
                label: new FxString("Description"),
                defaultValue: new FxString(false, "")
        )
      }
    }

    try {
      CacheAdmin.environment.getType("doc")
    } catch (FxRuntimeException e) {
      // Doc type
      new GroovyTypeBuilder().doc(label: new FxString("Doc"), description: new FxString("My Doc Type")) {
        // use the caption for mapping the CMIS document name
        name(
                assignment: CacheAdmin.environment.getAssignment((Long) EJBLookup.configurationEngine.get(SystemParameters.TREE_CAPTION_ROOTASSIGNMENT)),
                label: new FxString("Name")
        )

        document(
                label: new FxString("Document"),
                dataType: FxDataType.Binary
        )

        title(
                label: new FxString("Title"),
                defaultValue: new FxString(false, "(no title)")
        )
        description(
                label: new FxString("Description"),
                defaultValue: new FxString(false, "")
        )
        date(
                label: new FxString("Date"),
                dataType: FxDataType.DateTime
        )
      }
    }
  }

  def createData() {
// clear tree
    EJBLookup.treeEngine.clear(FxTreeMode.Edit)

// init repository

    Repository repo = new FlexiveRepository(null)
    Connection conn = repo.getConnection()
    Folder root = conn.getRootFolder()

    Folder folder1 = root.newFolder("fold");
    folder1.setName("folder 1");
    folder1.setValue("title", "The folder 1 description");
    folder1.setValue("description", "folder 1 title");
    folder1.save();

    Folder folder2 = folder1.newFolder("fold");
    folder2.setName("folder 2");
    folder2.setValue("title", "The folder 2 description");
    folder2.setValue("description", "folder 2 title");
    folder2.save();

    Document doc1 = folder1.newDocument("doc");
    doc1.setName("doc 1");
    doc1.setValue("title", "doc 1 title");
    doc1.setValue("description", "The doc 1 descr");
    doc1.save();

    Document doc2 = folder2.newDocument("doc");
    doc2.setName("doc 2");
    doc2.setValue("title", "doc 2 title");
    doc2.setValue("description", "The doc 2 descr");
    doc2.save();

    Document doc3 = folder2.newDocument("doc");
    doc3.setName("doc 3");
    // no title, description or date
    // inlined document from BasicHelper.TEST_FILE_CONTENT
    ContentStream cs = new SimpleContentStream(
            "This is a test file.\nTesting, testing...\n".getBytes("UTF-8"), "text/plain", "doc3.txt");
    doc3.setContentStream(cs);
    doc3.save();

    Document doc4 = folder2.newDocument("doc");
    doc4.setName("dog.jpg");
    doc4.setValue("title", "A Dog");
    doc4.setValue("description", "This is a small dog");
    // avoid compile-time dependency on chemistry-tests
    InputStream stream = Class.forName("org.apache.chemistry.test.BasicTestCase").getResourceAsStream("/dog.jpg");
    cs = new SimpleContentStream(stream, "image/jpeg", "dog.jpg");
    doc4.setContentStream(cs);
    doc4.save();

    conn.close();
  }
}
