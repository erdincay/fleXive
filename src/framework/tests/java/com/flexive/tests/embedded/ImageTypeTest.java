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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxBinary;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Test and setup for the image type
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "image"})
public class ImageTypeTest {

    private static String IMAGE_TYPE = "Image";

    private ContentEngine co;


    @BeforeClass
    public void beforeClass() throws Exception {
        co = EJBLookup.getContentEngine();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        logout();
    }

    @Test
    public void importTest() throws Exception {
        FxPK imgPK = null;
        try {
            File testFile = new File("src/framework/testresources/image/Exif.JPG");
            if (!testFile.exists())
                return;

            FxType type = CacheAdmin.getEnvironment().getType(IMAGE_TYPE);

            FileInputStream fis = new FileInputStream(testFile);
            BinaryDescriptor binary = new BinaryDescriptor(testFile.getName(), testFile.length(), fis);
            FxBinary fxBin = new FxBinary(false, binary);

            FxContent img = co.initialize(type.getId());
            img.setValue("/ImageBinary", fxBin);
            img.setValue("/Filename", new FxString(false, "Exif.JPG"));
            img = co.prepareSave(img);
            imgPK = co.save(img);
        } finally {
            if( imgPK != null )
                co.remove(imgPK);
        }
    }

    @Test
    public void binaryTestLength() throws Exception {
        binaryTest(true);
    }

    @Test
    public void binaryTestNoLength() throws Exception {
        binaryTest(false);
    }

    private void binaryTest(boolean sendLength) throws Exception {
        File testFile = new File("src/framework/testresources/image/Exif.JPG");
        if (!testFile.exists())
            return;

        FxType type = CacheAdmin.getEnvironment().getType(IMAGE_TYPE);

        FileInputStream fis = new FileInputStream(testFile);
        BinaryDescriptor binary;
        if( sendLength )
            binary = new BinaryDescriptor(testFile.getName(), testFile.length(), fis);
        else
            binary = new BinaryDescriptor(testFile.getName(), fis);

        FxContent img = co.initialize(type.getId());
        img.setValue("/ImageBinary", new FxBinary(false, binary));
        img.setValue("/Filename", new FxString(false, "Exif.JPG"));
        FxPK pk = null;
        try {
            pk = co.save(img);

            FxContent loaded = co.load(pk);
            FxBinary bin = (FxBinary) loaded.getValue("/ImageBinary");
            File comp = File.createTempFile("Exif", "JPG");
            FileOutputStream fos = new FileOutputStream(comp);
            bin.getBestTranslation().download(fos);
            fos.close();
            assert comp.length() == testFile.length() : "Files differ in length";
            if (!comp.delete())
                comp.deleteOnExit();
        } finally {
            if( pk != null )
                co.remove(pk);
        }
    }
}
