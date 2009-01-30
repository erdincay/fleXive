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

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.interfaces.SelectListEngine;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxSelectMany;
import com.flexive.shared.value.FxSelectOne;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.SelectMany;
import com.flexive.shared.value.renderer.FxValueRenderer;
import com.flexive.shared.value.renderer.FxValueRendererFactory;
import com.flexive.tests.embedded.FxTestUtils;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import java.util.Iterator;

/**
 * FxSelectList and FxSelectListItem tests
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "structure"})
public class SelectListTest {

    private SelectListEngine le;
    private ContentEngine ce;
    private AssignmentEngine ass;
    private TypeEngine typeEng;
    private ACL aclList, aclItem;

    /**
     * setup...
     *
     * @throws Exception on errors
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        login(TestUsers.SUPERVISOR);
        le = EJBLookup.getSelectListEngine();
        ce = EJBLookup.getContentEngine();
        ass = EJBLookup.getAssignmentEngine();
        typeEng = EJBLookup.getTypeEngine();
        //create the base type
        ACL[] tmp = FxTestUtils.createACLs(
                new String[]{
                        "LIST_" + RandomStringUtils.random(16, true, true),
                        "ITEM_" + RandomStringUtils.random(16, true, true),

                },
                new ACLCategory[]{
                        ACLCategory.SELECTLIST,
                        ACLCategory.SELECTLISTITEM
                },
                TestUsers.getTestMandator()
        );
        aclList = tmp[0];
        aclItem = tmp[1];
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        FxTestUtils.removeACL(aclList, aclItem);
        logout();
    }

    @Test
    public void selectListManipulation() throws Exception {
        final String TEST_NAME1 = "List1_" + RandomStringUtils.random(16, true, true);
        final String TEST_NAME2 = "List2_" + RandomStringUtils.random(16, true, true);
        FxSelectListEdit list = FxSelectListEdit.createNew(TEST_NAME1, new FxString("label"), new FxString("description"),
                false, aclList, aclItem);
        new FxSelectListItemEdit("item1", aclItem, list, new FxString("item1.label"), "item1.data", "item1.color");
        new FxSelectListItemEdit("item2", aclItem, list, new FxString("item2.label"), "item2.data", "item2.color");

        long list1Id = le.save(list);
        FxSelectList list1_load = CacheAdmin.getEnvironment().getSelectList(list1Id);

        FxSelectListItemEdit newItem = new FxSelectListItemEdit("item3", aclItem, list, new FxString("item3.label"), "item3.data", "item3.color");
        long selListItemId1 = le.save(newItem);
        newItem = CacheAdmin.getEnvironment().getSelectListItem(selListItemId1).asEditable();

        long selListItemId1new = le.save(newItem); // no changes
        assertEquals(selListItemId1, selListItemId1new);
        newItem.setLabel(new FxString("item3.newlabel")); // label changes
        selListItemId1new = le.save(newItem);
        assertEquals(selListItemId1, selListItemId1new);

        FxSelectListEdit edList = list1_load.asEditable();
        edList.addItem(newItem);
        edList.setDescription(new FxString("new description")); // test private method updateList
        le.save(edList);
        list1_load = CacheAdmin.getEnvironment().getSelectList(list1Id);

        assertEquals(list1_load.getItem("item3").getLabel().toString(), "item3.newlabel");

        le.remove(list1_load.getItem("item3"));
        try {
            list1_load = CacheAdmin.getEnvironment().getSelectList(list1Id);
            list1_load.getItem("item3");
            fail("Shouldn't be able to load selectlist item \"item3\"");
        } catch (FxRuntimeException e) {
            // expected
        }

        new FxSelectListItemEdit("item4", aclItem, list1_load, new FxString("item4.label"), "item4.data", "item4.color");
        le.save(list1_load.asEditable());
        assert !CacheAdmin.getEnvironment().getSelectList(list1Id).hasDefaultItem() : "No default item expected for list 1!";
        boolean found = false;
        for (FxSelectListItem item : CacheAdmin.getEnvironment().getSelectList(list1Id).getItems()) {
            found = found || item.getData().equals("item4.data");
        }
        assert found : "item4 was not saved!";
        assert TEST_NAME1.equals(list1_load.getName()) : "Wrong name! Got [" + list1_load.getName() + "] but expected [" + TEST_NAME1 + "]!";
        assert list1_load.getItems().size() == 3 : "Wrong number of items. Expected 3 but got " + list1_load.getItems().size();

        FxSelectListEdit list2 = FxSelectListEdit.createNew(list1_load, TEST_NAME2, new FxString("label2"), new FxString("description2"),
                false, aclList, aclItem);
        new FxSelectListItemEdit("item2", aclItem, list2, new FxString("list2.item1.label"),
                "list2.item1.data", "list2.item1.color")
                .setDefaultItem();
        long list2Id = le.save(list2);
        FxSelectList _list2 = CacheAdmin.getEnvironment().getSelectList(list2Id);
        assert _list2.hasDefaultItem() : "list 2 is expected to have a default item";
        assert _list2.getDefaultItem().getList().getId() == _list2.getId() : "Wrong list id for default item!";
        assert _list2.hasParentList() : "list 2 is expected to have a parent list!";
        le.save(_list2.asEditable().setDefaultItem(null));
        _list2 = CacheAdmin.getEnvironment().getSelectList(list2Id);
        assert !_list2.hasDefaultItem() : "list 2 should no longer have a default item!";
        le.remove(list1_load);
        assert !CacheAdmin.getEnvironment().getSelectList(list2Id).hasParentList() : "list 2 is not expected to have a parent list anymore!";
        le.remove(CacheAdmin.getEnvironment().getSelectList(list2Id));
    }

    @Test
    public void selectListFormatter() throws FxApplicationException {
        FxSelectListEdit list = FxSelectListEdit.createNew("selectListValueTest",
                new FxString("label"), new FxString("description"),
                false, aclList, aclItem);
        new FxSelectListItemEdit("item1", aclItem, list,
                new FxString("default label").setTranslation(FxLanguage.ENGLISH, "english item")
                        .setTranslation(FxLanguage.GERMAN, "deutscher eintrag"), "item1.data", "item1.color");
        new FxSelectListItemEdit("item2", aclItem, list,
                new FxString("default label").setTranslation(FxLanguage.ENGLISH, "english item 2")
                        .setTranslation(FxLanguage.GERMAN, "deutscher eintrag 2"), "item2.data", "item2.color");
        final long selectListId = le.save(list);
        final FxSelectList loadedList = CacheAdmin.getEnvironment().getSelectList(selectListId);
        try {
            final Iterator<FxSelectListItem> iterator = loadedList.getItems().iterator();
            final FxSelectListItem item1 = iterator.next();
            final FxSelectListItem item2 = iterator.next();
            final String label1En = item1.getLabel().getTranslation(FxLanguage.ENGLISH);
            final String label1De = item1.getLabel().getTranslation(FxLanguage.GERMAN);
            final String label2En = item2.getLabel().getTranslation(FxLanguage.ENGLISH);
            final String label2De = item2.getLabel().getTranslation(FxLanguage.GERMAN);
            final FxValueRenderer formatEn = FxValueRendererFactory.getInstance(new FxLanguage("en"));
            final FxValueRenderer formatDe = FxValueRendererFactory.getInstance(new FxLanguage("de"));

            final FxSelectOne selectOne = new FxSelectOne(item1);
            final String labelEn = formatEn.format(selectOne);
            final String labelDe = formatDe.format(selectOne);
            assert labelEn.equals(label1En) : "Expected select list label '" + label1En + "', got: " + labelEn;
            assert labelDe.equals(label1De) : "Expected select list label '" + label1De + "', got: " + labelDe;

            final SelectMany many = new SelectMany(loadedList);
            many.selectItem(item1);
            many.selectItem(item2);
            final FxSelectMany selectMany = new FxSelectMany(many);
            final String labelManyEn = formatEn.format(selectMany);
            final String labelManyDe = formatDe.format(selectMany);
            assert labelManyEn.equals(label1En.compareTo(label2En) < 0
                    ? label1En + ", " + label2En : label2En + ", " + label1En) : "Unexpected label: " + labelManyEn;
            assert labelManyDe.equals(label1De.compareTo(label2De) < 0
                    ? label1De + ", " + label2De : label2De + ", " + label1De) : "Unexpected label: " + labelManyDe;
        } finally {
            le.remove(loadedList);
        }
    }

    /**
     * Tests the getSelectListItemInstanceCount(long selectListItemId) method
     * via The Article type
     *
     * @throws FxApplicationException error
     */
    @Test
    public void instanceCounterTest() throws FxApplicationException {
        final String TEST_NAME1 = "List1_" + RandomStringUtils.random(16, true, true);
        final String TEST_TYPE = "TEST_TYPE_" + RandomStringUtils.random(16, true, true);
        final String TEST_PROPERTY = "TEST_PROPERTY_" + RandomStringUtils.random(16, true, true);

        FxSelectListEdit list = FxSelectListEdit.createNew(TEST_NAME1, new FxString("testlabel"), new FxString("testdescription"),
                false, aclList, aclItem);
        new FxSelectListItemEdit("testitem123", aclItem, list, new FxString("item1.label"), "item1.data", "item1.color");
        long listId = le.save(list);

        // create a type and the respective property referencing the selectlist
        FxSelectList loadedList = CacheAdmin.getEnvironment().getSelectList(listId);
        ACL defACL = CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId());
        FxTypeEdit testTypeEd = FxTypeEdit.createNew(TEST_TYPE);
        long typeId = typeEng.save(testTypeEd);
        FxPropertyEdit propEd = FxPropertyEdit.createNew(TEST_PROPERTY, new FxString("description"), new FxString("hint"),
                FxMultiplicity.MULT_0_1, defACL, FxDataType.SelectOne);
        propEd.setReferencedList(loadedList);
        ass.createProperty(typeId, propEd.setAutoUniquePropertyName(true), "/");

        // create two content instances for the above property
        FxContent co = ce.initialize(typeId);
        co.setValue("/" + TEST_PROPERTY, CacheAdmin.getEnvironment().getSelectListItem(listId));
        ce.save(co);
        co = ce.initialize(typeId);
        co.setValue("/" + TEST_PROPERTY, CacheAdmin.getEnvironment().getSelectListItem(listId));
        ce.save(co);

        assertEquals(le.getSelectListItemInstanceCount(listId), 2);

        // clean up
        ce.removeForType(typeId);
        typeEng.remove(typeId);
        le.remove(CacheAdmin.getEnvironment().getSelectList(listId));
    }
}
