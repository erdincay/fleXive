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
package com.flexive.tests.embedded.persistence;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.*;
import com.flexive.tests.embedded.FxTestUtils;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;

/**
 * Tests for FxValues (single/multi language, store/load)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "valuetest"})
public class ValueTest {

    private ContentEngine co;
    private TypeEngine type;
    private AssignmentEngine ass;

    private final static String TYPE_NAME = "TESTVALUES_" + RandomStringUtils.random(16, true, true);

    private ACL aclStructure, aclWorkflow, aclContent;
    private long typeId;
    private FxType REF_TYPE1, REF_TYPE2;
    private FxPK RPK1, RPK2;

    /**
     * setup...
     *
     * @throws Exception on errors
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        co = EJBLookup.getContentEngine();
        type = EJBLookup.getTypeEngine();
        ass = EJBLookup.getAssignmentEngine();
        login(TestUsers.SUPERVISOR);
        //create the base type
        ACL[] tmp = FxTestUtils.createACLs(
                new String[]{
                        "STRUCTURE_" + RandomStringUtils.random(16, true, true),
                        "WORKFLOW_" + RandomStringUtils.random(16, true, true),
                        "CONTENT_" + RandomStringUtils.random(16, true, true)

                },
                new ACLCategory[]{
                        ACLCategory.STRUCTURE,
                        ACLCategory.WORKFLOW,
                        ACLCategory.INSTANCE
                },
                TestUsers.getTestMandator()
        );
        aclStructure = tmp[0];
        aclWorkflow = tmp[1];
        aclContent = tmp[2];

        typeId = type.save(FxTypeEdit.createNew(TYPE_NAME, new FxString("Test data"), aclStructure, null));

        //create referenced types
        long rt1 = type.save(FxTypeEdit.createNew("Referenced Test Type 1", new FxString("Test data"), aclStructure, null));
        REF_TYPE1 = CacheAdmin.getEnvironment().getType(rt1);
        long rt2 = type.save(FxTypeEdit.createNew("Referenced Test Type 2", new FxString("Test data"), aclStructure, null));
        REF_TYPE2 = CacheAdmin.getEnvironment().getType(rt2);
        //create single test entries for both referenced types
        FxContent co1 = co.initialize(REF_TYPE1.getId());
        RPK1 = new FxPK(co.save(co1).getId(), 1); //fix to set it to max version instead of specific
        FxContent co2 = co.initialize(REF_TYPE2.getId());
        RPK2 = new FxPK(co.save(co2).getId(), 1); //fix to set it to max version instead of specific
    }

    private void createProperty(FxDataType dataType) throws FxApplicationException {
        FxPropertyEdit prop = FxPropertyEdit.createNew("VTS" + dataType.name(),
                new FxString("UnitTest for " + dataType.name()),
                new FxString("hint..."), new FxMultiplicity(1, 1),
                aclStructure, dataType).setMultiLang(false).setOverrideMultiLang(true);
        switch (dataType) {
            case Reference:
                prop.setReferencedType(REF_TYPE1);
                break;
            default:
                //nothing
        }
        long propId = ass.createProperty(typeId, prop, "/");
        FxPropertyAssignment paSingle = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(propId);
        FxPropertyAssignmentEdit pe = FxPropertyAssignmentEdit.
                createNew(paSingle, paSingle.getAssignedType(), "VTM" + dataType.name(), "/").setMultiLang(true);
        ass.save(pe, false);
    }

    private void removeProperty(FxDataType dataType) throws FxApplicationException {
        long assId = CacheAdmin.getEnvironment().getAssignment(TYPE_NAME.toUpperCase() + "/VTS" + dataType.name().toUpperCase()).getId();
        ass.removeAssignment(assId, true, true);
    }


    @AfterClass(dependsOnMethods = {"tearDownStructures"})
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        logout();
    }

    @AfterClass
    public void tearDownStructures() throws Exception {
        long typeId = CacheAdmin.getEnvironment().getType(TYPE_NAME).getId();
        co.removeForType(typeId);
        type.remove(typeId);
        co.remove(RPK1);
        co.remove(RPK2);
        type.remove(REF_TYPE1.getId());
        type.remove(REF_TYPE2.getId());
        FxTestUtils.removeACL(aclStructure, aclWorkflow, aclContent);
    }

    class TestData<T extends FxValue> {
        FxDataType dataType;
        T _single;
        T _multi;
        Object en_value;
        Object de_value;


        @SuppressWarnings({"unchecked"})
        public TestData(FxDataType dataType, T _single, T _multi) {
            Assert.assertTrue(!_single.isMultiLanguage(), "1st value argument has to be single language!");
            Assert.assertTrue(_multi.isMultiLanguage(), "2nd value argument has to be multi language!");
            this.dataType = dataType;
            this._single = _single;
            this._multi = _multi;
            en_value = _single.getBestTranslation();
            de_value = _multi.getBestTranslation();
            this._single.setTranslation(FxLanguage.ENGLISH, en_value);
            this._multi.setTranslation(FxLanguage.ENGLISH, en_value);
            this._multi.setTranslation(FxLanguage.GERMAN, de_value);
            this._multi.setDefaultLanguage(FxLanguage.ENGLISH);
            this._single.setValueData(42);
            this._multi.setValueData(43);
        }

        @SuppressWarnings({"unchecked"})
        public void testConsistency() throws Exception {
            Assert.assertTrue(!_single.equals(_multi));
            Assert.assertTrue(_single.getDefaultTranslation().equals(_multi.getDefaultTranslation()));
            Assert.assertTrue(_multi.getTranslation(FxLanguage.ENGLISH).equals(en_value));
            Assert.assertTrue(_multi.getTranslation(FxLanguage.GERMAN).equals(de_value));
            Assert.assertTrue(_multi.getTranslatedLanguages().length == 2);
            Assert.assertTrue(_single.getTranslatedLanguages().length == 1);
            Assert.assertTrue(_single.getTranslation(FxLanguage.GERMAN).equals(en_value));
            _single.setTranslation(FxLanguage.ENGLISH, de_value);
            Assert.assertTrue(_single.getTranslation(FxLanguage.GERMAN).equals(de_value));
            _single.setDefaultLanguage(FxLanguage.ITALIAN);
            Assert.assertTrue(!_single.hasDefaultLanguage());
            Assert.assertTrue(_single.getTranslation(FxLanguage.ENGLISH).equals(de_value));
            Assert.assertTrue(_multi.hasDefaultLanguage());
            checkValueDataClearing(_single, 42);
            checkValueDataClearing(_multi, 43);
        }

        private void checkValueDataClearing(T value, int data) {
            Assert.assertEquals((int) value.getValueData(), data);
            value.setValueData(data + 13);
            Assert.assertEquals((int) value.getValueData(), data + 13);
            Assert.assertTrue(value.hasValueData());
            value.clearValueData();
            Assert.assertFalse(value.hasValueData());
            value.setValueData(data);
            Assert.assertTrue(value.hasValueData());
        }

        public void testSingleValue(FxValue compare) throws Exception {
            if (compare instanceof FxBinary) {
                BinaryDescriptor desc = ((FxBinary) compare).getBestTranslation();
                Assert.assertTrue(desc.isImage(), "Expected an image!");
                Assert.assertTrue(desc.getWidth() == 2048, "Wrong image width!");
                Assert.assertTrue(desc.getHeight() == 1536, "Wrong image height!");
                Assert.assertTrue(desc.getResolution() == 72.0 || desc.getResolution() == 180.0, "Wrong image resolution! Expected [72.0] or [180.0] depending on ImageMagick version but got [" + desc.getResolution() + "]");
                //download
                File f = File.createTempFile("FxBinary", ".bin");
                FileOutputStream fout = new FileOutputStream(f);
                System.out.println("b4 download of " + f.getAbsolutePath() + " ...");
                long time = System.currentTimeMillis();
                ((FxBinary) compare).getBestTranslation().download(fout);
                System.out.println("download of " + f.getAbsolutePath() + " took " + (System.currentTimeMillis() - time) + "[ms]!");
                fout.close();
                f.deleteOnExit();
                return;
            } else if (compare instanceof FxHTML) {
                Assert.assertTrue(((String) compare.getBestTranslation()).indexOf((String) _single.getBestTranslation()) >= 0, "Original single value was not found in tidied value!");
                return;
            }
            //cant use regular compare since even single language values will be loaded as multi language if the assignment is multilinugual!
            Assert.assertTrue(_single.getBestTranslation().equals(compare.getBestTranslation()), _single.getClass().getCanonicalName() + " mismatch for single language value (DataType " + dataType.name() + "): " + _single.toString() + "!=" + compare.toString());
        }

        public void testMultiValue(FxValue compare) throws Exception {
            if (compare instanceof FxBinary)
                return; //cant test this yet
//            else if( compare instanceof FxHTML ) {
//                return;
//            }
            Assert.assertTrue(_multi.equals(compare), _multi.getClass().getCanonicalName() + " mismatch for multi language value (DataType " + dataType.name() + "): " + _multi.toString() + "!=" + compare.toString());
        }
    }

    @Test
    public void valueTest() throws Exception {
        GregorianCalendar gc_multi_date = new GregorianCalendar(1940, 11, 22);
        GregorianCalendar gc_multi_date2 = new GregorianCalendar(1997, 9, 21);
        GregorianCalendar gc_single_date = new GregorianCalendar(1974, 0, 12);
        GregorianCalendar gc_single_date2 = new GregorianCalendar(1974, 3, 17);
        GregorianCalendar gc_multi_datetime = new GregorianCalendar(1940, 11, 22, 3, 30, 20);
        GregorianCalendar gc_multi_datetime2 = new GregorianCalendar(1997, 9, 21, 7, 40, 30);
        GregorianCalendar gc_single_datetime = new GregorianCalendar(1974, 0, 12, 4, 35, 45);
        GregorianCalendar gc_single_datetime2 = new GregorianCalendar(1974, 3, 17, 14, 30, 15);
        String s_single = "ABC";
        String s_multi = "DEF";
        String s_single_big = RandomStringUtils.randomAlphanumeric(50000);
        String s_multi_big = RandomStringUtils.randomAlphanumeric(50000);

        File testFile = new File("test.file");
        if (!testFile.exists())
            testFile = new File("src/framework/testresources/image/Exif.JPG");
        if (!testFile.exists())
            return;
        FileInputStream fis = new FileInputStream(testFile);
        long time = System.currentTimeMillis();
        BinaryDescriptor binary = new BinaryDescriptor(testFile.getName(), testFile.length(), fis);
        System.out.println("size: " + binary.getSize() + " time: " + (System.currentTimeMillis() - time));

        System.out.println("==valueTest== Handle received for " + binary.getName() + ": " + binary.getHandle());
        fis.close();


        TestData[] data = {
                new TestData<FxHTML>(FxDataType.HTML,
                        new FxHTML(false, s_single).setTidyHTML(true),
                        new FxHTML(true, s_multi)),

                new TestData<FxString>(FxDataType.String1024,
                        new FxString(false, s_single),
                        new FxString(true, s_multi)),

                new TestData<FxString>(FxDataType.Text,
                        new FxString(false, s_single_big),
                        new FxString(true, s_multi_big)),

                new TestData<FxNumber>(FxDataType.Number,
                        new FxNumber(false, Integer.MAX_VALUE),
                        new FxNumber(true, Integer.MIN_VALUE)),

                new TestData<FxLargeNumber>(FxDataType.LargeNumber,
                        new FxLargeNumber(false, Long.MAX_VALUE),
                        new FxLargeNumber(true, Long.MIN_VALUE)),

                new TestData<FxFloat>(FxDataType.Float,
                        new FxFloat(false, 123213213213.2222221f),
                        new FxFloat(true, 1f)),

                new TestData<FxDouble>(FxDataType.Double,
                        new FxDouble(false, 0.000000000000001d),
                        new FxDouble(true, 1d)),

                new TestData<FxBoolean>(FxDataType.Boolean,
                        new FxBoolean(false, true),
                        new FxBoolean(true, false)),

                new TestData<FxDate>(FxDataType.Date,
                        new FxDate(false, gc_single_date.getTime()),
                        new FxDate(true, gc_multi_date.getTime())),

                new TestData<FxDateTime>(FxDataType.DateTime,
                        new FxDateTime(false, gc_single_datetime.getTime()),
                        new FxDateTime(true, gc_multi_datetime.getTime())),

                new TestData<FxDateRange>(FxDataType.DateRange,
                        new FxDateRange(false, new DateRange(gc_single_date.getTime(), gc_single_date2.getTime())),
                        new FxDateRange(true, new DateRange(gc_multi_date.getTime(), gc_multi_date2.getTime()))),

                new TestData<FxDateTimeRange>(FxDataType.DateTimeRange,
                        new FxDateTimeRange(false, new DateRange(gc_single_datetime.getTime(), gc_single_datetime2.getTime())),
                        new FxDateTimeRange(true, new DateRange(gc_multi_datetime.getTime(), gc_multi_datetime2.getTime()))),

                new TestData<FxBinary>(FxDataType.Binary,
                        new FxBinary(false, binary),
                        new FxBinary(true, binary)),

                new TestData<FxReference>(FxDataType.Reference,
                        new FxReference(false, new ReferencedContent(RPK1)),
                        new FxReference(true, new ReferencedContent(RPK1)))
        };
        FxType testType;
        StringBuilder sbErr = new StringBuilder(100);
        for (TestData test : data) {
            try {
                System.out.println("Testing " + test.dataType.name() + " ...");
                test.testConsistency();
                createProperty(test.dataType);
                testType = CacheAdmin.getEnvironment().getType(TYPE_NAME);
                FxContent content = co.initialize(testType.getId());
                content.setValue("/VTS" + test.dataType.name() + "[1]", test._single.copy());
//                content.getPropertyData("/VT" + test.dataType.name() + "[1]").createNew(FxPropertyData.POSITION_BOTTOM);
                content.setValue("/VTM" + test.dataType.name() + "[1]", test._multi.copy().setValueData(43));
                FxPK pk = co.save(content);
                FxContent loaded = co.load(pk);
                FxValue loadedSingle = loaded.getPropertyData("/VTS" + test.dataType.name() + "[1]").getValue();
                FxValue loadedMulti = loaded.getPropertyData("/VTM" + test.dataType.name() + "[1]").getValue();
                test.testSingleValue(loadedSingle);
                test.testMultiValue(loadedMulti);
                Assert.assertEquals((int) loadedSingle.getValueData(), 42);
                Assert.assertEquals((int) loadedMulti.getValueData(), 43);
                pk = co.save(loaded);
                loaded = co.load(pk);
                loadedSingle = loaded.getPropertyData("/VTS" + test.dataType.name() + "[1]").getValue();
                loadedMulti = loaded.getPropertyData("/VTM" + test.dataType.name() + "[1]").getValue();
                test.testSingleValue(loadedSingle);
                test.testMultiValue(loadedMulti);
                Assert.assertEquals((int) loadedSingle.getValueData(), 42);
                Assert.assertEquals((int) loadedMulti.getValueData(), 43);
                co.remove(pk);
                if (test.dataType == FxDataType.Reference) {
                    //additional tests
                    content = co.initialize(testType.getId());
                    content.setValue("/VTM" + test.dataType.name() + "[1]", new FxReference(true, new ReferencedContent(RPK1)));
                    try {
                        //set to a new pk
                        content.setValue("/VTS" + test.dataType.name() + "[1]", new FxReference(false, new ReferencedContent(new FxPK())));
                        co.save(content); //expected to fail
                        Assert.fail("Invalid PK (new) for a reference should fail!");
                    } catch (Exception e) {
                        //expected
                    }
                    try {
                        //set to a non existing pk
                        content.setValue("/VTS" + test.dataType.name() + "[1]", new FxReference(false, new ReferencedContent(new FxPK(123456))));
                        co.save(content); //expected to fail
                        Assert.fail("Invalid PK (non existant) for a reference should fail!");
                    } catch (Exception e) {
                        //expected
                    }
                    try {
                        //set to an existing pk, but wrong type
                        content.setValue("/VTS" + test.dataType.name() + "[1]", new FxReference(false, new ReferencedContent(RPK2)));
                        co.save(content); //expected to fail
                        Assert.fail("Invalid PK (wrong type) for a reference should fail!");
                    } catch (Exception e) {
                        //expected
                    }
                    try {
                        //set to an existing pk, but wrong type
                        content.setValue("/VTS" + test.dataType.name() + "[1]", RPK1);
                        co.save(content); //expected to work
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assert.fail("Correct PK for a reference should not fail!");
                        //expected
                    }
                }
            } catch (Throwable e) {
                sbErr.append("Failed DataType: [").append(test.dataType.name()).append("] with: ").append(e.getMessage()).append("\n");
                e.printStackTrace();
            } finally {
                removeProperty(test.dataType);
            }
        }
        if (sbErr.length() > 0)
            Assert.fail(sbErr.toString());
    }

    /**
     * Basic tests of FxValue's {@link Comparable} implementation
     */
    @Test
    public void compareToTest() {
        final List<FxString> strings = new ArrayList<FxString>(Arrays.asList(new FxString("b"),
                new FxString("a"), new FxString("c"), new FxString("a0")));
        Collections.sort(strings);
        final String sortedStrings = StringUtils.join(strings.iterator(), ',');
        Assert.assertTrue(sortedStrings.equals("a,a0,b,c"), "Expected lexical order 'a,a0,b,c', got: " + sortedStrings);
    }
}
