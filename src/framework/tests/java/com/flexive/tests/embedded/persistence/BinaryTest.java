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
package com.flexive.tests.embedded.persistence;

import com.flexive.core.storage.binary.FxBinaryUtils;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxFileUtils;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.BinaryDescriptor;
import static com.flexive.shared.value.BinaryDescriptor.PreviewSizes;
import com.flexive.shared.value.FxBinary;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;

/**
 * Tests for binaries
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "content", "binary"})
public class BinaryTest {

    private static String IMAGE_TYPE = "Image";

    private ContentEngine co;

    private final static String BASE_STORAGE = "testStorage";
    private String transitDir = BASE_STORAGE + File.separator + "transit_" + RandomStringUtils.randomAlphanumeric(8);
    private String binaryDir = BASE_STORAGE + File.separator + "binary_" + RandomStringUtils.randomAlphanumeric(8);
    private final String TEST_BINARY = "src/framework/testresources/image/Exif.JPG";
    private int divisionId = 1;


    @BeforeClass
    public void beforeClass() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException {
        co = EJBLookup.getContentEngine();
        login(TestUsers.SUPERVISOR);
        EJBLookup.getConfigurationEngine().put(SystemParameters.NODE_TRANSIT_PATH, transitDir);
        EJBLookup.getConfigurationEngine().put(SystemParameters.NODE_BINARY_PATH, binaryDir);
        divisionId = FxContext.get().getDivisionId();
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        EJBLookup.getConfigurationEngine().put(SystemParameters.BINARY_DB_TRASHOLD, -1L);
        EJBLookup.getConfigurationEngine().put(SystemParameters.BINARY_DB_PREVIEW_TRASHOLD, -1L);
        EJBLookup.getConfigurationEngine().put(SystemParameters.BINARY_TRANSIT_DB, true);
        FxFileUtils.removeDirectory(BASE_STORAGE);
        logout();
    }

    @Test
    public void binaryTestLength() throws Exception {
        binaryTest(true, false, -1L, -1L);
    }

    @Test
    public void binaryTestNoLength() throws Exception {
        binaryTest(false, false, -1L, -1L);
    }

    @Test
    public void binaryFSTransitDBStorage() throws Exception {
        binaryTest(true, true, -1L, -1L);
    }

    @Test
    public void binaryDBTransitFSStorage() throws Exception {
        binaryTest(true, false, -1L, -1L);
    }

    @Test
    public void binaryFSTransitFSStorage() throws Exception {
        binaryTest(true, true, -1L, -1L);
    }

    @Test
    public void binaryFSTransitFSStorageLimit1() throws Exception {
        //binary in db, previews on fs
        binaryTest(true, true, 100000L, -1L);
    }

    @Test
    public void binaryFSTransitFSStorageLimit2() throws Exception {
        //binary in fs, previews in db
        binaryTest(true, true, -1L, 100000L);
    }

    @Test
    public void binaryFSTransitFSStorageLimit3() throws Exception {
        //binary in fs, previews on fs if > 100 byte
        binaryTest(true, true, 0L, 100L);
    }

    @Test
    public void binaryFSTransitFSStorageLimit4() throws Exception {
        //binary in fs, previews on fs if > 1000 byte
        binaryTest(true, true, 0L, 1000L);
    }

    private void binaryTest(boolean sendLength, boolean transitFS, long binTrashold, long prevTrashold) throws Exception {
        File testFile = new File(TEST_BINARY);
        if (!testFile.exists())
            Assert.fail("Test binary [" + testFile.getAbsolutePath() + "] not found!");

        EJBLookup.getConfigurationEngine().put(SystemParameters.BINARY_TRANSIT_DB, !transitFS);
        EJBLookup.getConfigurationEngine().put(SystemParameters.BINARY_DB_TRASHOLD, binTrashold);
        EJBLookup.getConfigurationEngine().put(SystemParameters.BINARY_DB_PREVIEW_TRASHOLD, prevTrashold);

        FxType type = CacheAdmin.getEnvironment().getType(IMAGE_TYPE);

        FileInputStream fis = new FileInputStream(testFile);
        BinaryDescriptor binary;
        if (sendLength)
            binary = new BinaryDescriptor(testFile.getName(), testFile.length(), fis);
        else
            binary = new BinaryDescriptor(testFile.getName(), fis);

        FxContent img = co.initialize(type.getId());
        img.setValue("/ImageBinary", new FxBinary(false, binary));
        img.setValue("/Filename", new FxString(false, "Exif.JPG"));
        FxPK pk = null;
        try {
            if (transitFS) {
                //perform an optional prepareSave to be able to retrieve the transit handle
                img = co.prepareSave(img);
                final String handle = ((BinaryDescriptor) img.getValue("/ImageBinary").getBestTranslation()).getHandle();
                File transStore = new File(FxBinaryUtils.getTransitDirectory() + File.separatorChar + String.valueOf(divisionId));
                Assert.assertTrue(transStore.exists() && transStore.isDirectory(), "transit directory [" + transStore.getAbsolutePath() + "] does not exist!");
                File[] found = transStore.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.startsWith(handle + "__");
                    }
                });
                Assert.assertTrue(found != null && found.length == 1 && found[0].exists(), "Transit file not found!");
                Assert.assertTrue(FxFileUtils.fileCompare(testFile, found[0]), "Transit file does not match test file!");
            }
            pk = co.save(img);

            FxContent loaded = co.load(pk);
            FxBinary bin = (FxBinary) loaded.getValue("/ImageBinary");
            File comp = File.createTempFile("Exif", "JPG");
            FileOutputStream fos = new FileOutputStream(comp);
            bin.getBestTranslation().download(fos);
            fos.close();
            Assert.assertTrue(FxFileUtils.fileCompare(comp, testFile), "Files do not match!");
            final BinaryDescriptor desc = ((BinaryDescriptor) img.getValue("/ImageBinary").getBestTranslation());
            if (binTrashold > testFile.length()) {
                //binary is expected to be stored on the filesystem
                File binFile = FxBinaryUtils.getBinaryFile(divisionId, desc.getId(), desc.getVersion(), desc.getQuality(), PreviewSizes.ORIGINAL.getBlobIndex());
                Assert.assertTrue(binFile != null && binFile.exists(), "Binary file for binary id [" + desc.getId() + "] does not exist!");
                Assert.assertTrue(FxFileUtils.fileCompare(testFile, binFile), "Binary file does not match test file!");
            }
            if (prevTrashold >= 0) {
                checkPreviewFile(prevTrashold, desc, bin, PreviewSizes.PREVIEW1);
                checkPreviewFile(prevTrashold, desc, bin, PreviewSizes.PREVIEW2);
                checkPreviewFile(prevTrashold, desc, bin, PreviewSizes.PREVIEW3);
            }
            FxFileUtils.removeFile(comp);
        } finally {
            if (pk != null)
                co.remove(pk);
        }
    }

    /**
     * Check if a preview file is handled correctly
     *
     * @param prevTrashold trashold
     * @param desc         descriptor
     * @param bin          binary
     * @param previewSize  evaluated size
     * @throws IOException       on errors
     * @throws FxStreamException on errors
     */
    private void checkPreviewFile(long prevTrashold, BinaryDescriptor desc, FxBinary bin, PreviewSizes previewSize) throws IOException, FxStreamException {
        File prev = File.createTempFile("PrevExif", "JPG");
        try {
            FileOutputStream fos = new FileOutputStream(prev);
            bin.getBestTranslation().download(fos, previewSize);
            fos.close();
            Assert.assertTrue(prev.exists() && prev.length() > 0, "No preview file was generated or found for size [" + previewSize.name() + "]!");
            File prevStorageFile = FxBinaryUtils.getBinaryFile(divisionId, desc.getId(), desc.getVersion(), desc.getQuality(), previewSize.getBlobIndex());
            if (prevTrashold < prev.length()) {
                //has to be stored on filesystem
                Assert.assertTrue(prevStorageFile != null && prevStorageFile.exists(),
                        "Preview file for binary id [" + desc.getId() + "], preview size [" + previewSize.name() + "] does not exist!");
                Assert.assertTrue(FxFileUtils.fileCompare(prev, prevStorageFile),
                        "Preview file for size [" + previewSize.name() + "] does not match!");
            } else {
                Assert.assertTrue(prevStorageFile == null,
                        "No preview file expected to exist in filesystem storage for size [" + previewSize.name() + "]!");
            }
        } finally {
            FxFileUtils.removeFile(prev);
        }
    }
}