package com.flexive.tests.embedded;

import com.flexive.shared.*;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.interfaces.PhraseEngine;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;

/**
 * PhraseEngine tests
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "phrase"})
public class PhraseTest {
    long testMandator = -1L;

    @BeforeClass
    public void beforeClass() throws Exception {
        login(TestUsers.SUPERVISOR);
        testMandator = EJBLookup.getMandatorEngine().create("MANDATOR_" + RandomStringUtils.randomAlphanumeric(10), true);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        final PhraseEngine pe = EJBLookup.getPhraseEngine();
        final long ownMandator = FxContext.get().getTicket().getMandatorId();
        pe.clearTree(ownMandator);
        pe.removeMandatorPhrases(ownMandator);
        if (testMandator != -1L) {
            pe.clearTree(testMandator);
            pe.removeMandatorPhrases(testMandator);
            EJBLookup.getMandatorEngine().remove(testMandator);
        }
        logout();
    }

    /**
     * Simple saving and loading of phrases taking mandator priority into account
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void phraseBasicValueTest() throws FxApplicationException {
        final long ownMandator = FxContext.get().getTicket().getMandatorId();
        PhraseEngine pe = EJBLookup.getPhraseEngine();
        pe.clearTree(testMandator);
        pe.removeMandatorPhrases(testMandator);
        pe.clearTree(ownMandator);
        pe.removeMandatorPhrases(ownMandator);
        FxString val1 = new FxString(true, FxLanguage.ENGLISH, "english test 1");
        val1.setTranslation(FxLanguage.GERMAN, "german test 1");
        val1.setTranslation(FxLanguage.FRENCH, "french test 1");
        val1.setTranslation(FxLanguage.ITALIAN, "italian test 1");
        pe.savePhrase("fx.test.1", val1, ownMandator);
        //create the same phrase key for testMandator
        FxString val2 = new FxString(true, FxLanguage.ENGLISH, "english test 2");
        val2.setTranslation(FxLanguage.GERMAN, "german test 2");
        val2.setTranslation(FxLanguage.FRENCH, "french test 2");
        val2.setTranslation(FxLanguage.ITALIAN, "italian test 2");
        pe.savePhrase("fx.test.1", val2, testMandator);

        FxPhrase phrase = pe.loadPhrase("fx.test.1", ownMandator, testMandator);
        Assert.assertNotNull(phrase);
        Assert.assertFalse(phrase.hasTag());
        Assert.assertEquals(phrase.getKey(), "fx.test.1");
        Assert.assertEquals(phrase.getMandator(), ownMandator);
        Assert.assertFalse(phrase.getValue().isEmpty());
        Assert.assertTrue(phrase.getValue().isMultiLanguage());
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.ENGLISH), "english test 1");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.GERMAN), "german test 1");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.FRENCH), "french test 1");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.ITALIAN), "italian test 1");

        phrase.setTag("test tag 1");
        phrase.save();
        Assert.assertTrue(phrase.hasTag());
        Assert.assertEquals(phrase.getFxTag().getTranslation(FxLanguage.DUTCH), "test tag 1"); //since its single language every translation is the same
        Assert.assertEquals(phrase.getTag(), "test tag 1");

        FxPhrase loadedPhrase = pe.loadPhrase("fx.test.1"); //should load for own mandator
        Assert.assertNotNull(loadedPhrase);
        Assert.assertEquals(loadedPhrase.getKey(), "fx.test.1");
        Assert.assertEquals(loadedPhrase.getMandator(), ownMandator);
        Assert.assertFalse(loadedPhrase.getValue().isEmpty());
        Assert.assertTrue(loadedPhrase.getValue().isMultiLanguage());
        Assert.assertEquals(loadedPhrase.getValue().getTranslation(FxLanguage.ENGLISH), "english test 1");
        Assert.assertEquals(loadedPhrase.getValue().getTranslation(FxLanguage.GERMAN), "german test 1");
        Assert.assertEquals(loadedPhrase.getValue().getTranslation(FxLanguage.FRENCH), "french test 1");
        Assert.assertEquals(loadedPhrase.getValue().getTranslation(FxLanguage.ITALIAN), "italian test 1");
        Assert.assertTrue(loadedPhrase.hasTag());
        Assert.assertEquals(loadedPhrase.getFxTag().getTranslation(FxLanguage.ITALIAN), "test tag 1"); //since its single language every translation is the same
        Assert.assertEquals(loadedPhrase.getTag(), "test tag 1");

        Assert.assertEquals(pe.loadPhraseValue(FxLanguage.ENGLISH, "fx.test.1", ownMandator, testMandator), "english test 1");
        Assert.assertEquals(pe.loadPhraseValue(FxLanguage.GERMAN, "fx.test.1", ownMandator, testMandator), "german test 1");
        Assert.assertEquals(pe.loadPhraseValue(FxLanguage.FRENCH, "fx.test.1", ownMandator, testMandator), "french test 1");
        Assert.assertEquals(pe.loadPhraseValue(FxLanguage.ITALIAN, "fx.test.1", ownMandator, testMandator), "italian test 1");
        try {
            pe.loadPhraseValue(FxLanguage.DUTCH, "fx.test.1", ownMandator, testMandator);
            Assert.fail("Dutch translation of phrase should not be found!");
        } catch (FxNotFoundException nfe) {
            //expected
        }
        FxString fxVal = pe.loadPhraseValue("fx.test.1", ownMandator, testMandator);
        Assert.assertNotNull(fxVal);
        Assert.assertFalse(fxVal.isEmpty());
        Assert.assertEquals(fxVal.getTranslation(FxLanguage.ENGLISH), "english test 1");
        Assert.assertEquals(fxVal.getTranslation(FxLanguage.GERMAN), "german test 1");
        Assert.assertEquals(fxVal.getTranslation(FxLanguage.FRENCH), "french test 1");
        Assert.assertEquals(fxVal.getTranslation(FxLanguage.ITALIAN), "italian test 1");

        //test mandator order
        phrase = pe.loadPhrase("fx.test.1", ownMandator, testMandator);
        Assert.assertNotNull(phrase);
        Assert.assertTrue(phrase.hasTag());
        Assert.assertEquals(phrase.getTag(), "test tag 1");
        Assert.assertEquals(phrase.getKey(), "fx.test.1");
        Assert.assertEquals(phrase.getMandator(), ownMandator);
        Assert.assertFalse(phrase.getValue().isEmpty());
        Assert.assertTrue(phrase.getValue().isMultiLanguage());
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.ENGLISH), "english test 1");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.GERMAN), "german test 1");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.FRENCH), "french test 1");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.ITALIAN), "italian test 1");
        phrase = pe.loadPhrase("fx.test.1", testMandator, ownMandator);
        Assert.assertNotNull(phrase);
        Assert.assertFalse(phrase.hasTag()); //phrase of testMandator should not have a tag
        Assert.assertEquals(phrase.getKey(), "fx.test.1");
        Assert.assertEquals(phrase.getMandator(), testMandator);
        Assert.assertFalse(phrase.getValue().isEmpty());
        Assert.assertTrue(phrase.getValue().isMultiLanguage());
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.ENGLISH), "english test 2");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.GERMAN), "german test 2");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.FRENCH), "french test 2");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.ITALIAN), "italian test 2");

        Assert.assertTrue(pe.removePhrase("fx.test.1", ownMandator));
        phrase = pe.loadPhrase("fx.test.1", ownMandator, testMandator);
        Assert.assertNotNull(phrase);
        Assert.assertFalse(phrase.hasTag());
        Assert.assertEquals(phrase.getKey(), "fx.test.1");
        Assert.assertEquals(phrase.getMandator(), testMandator);
        Assert.assertFalse(phrase.getValue().isEmpty());
        Assert.assertTrue(phrase.getValue().isMultiLanguage());
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.ENGLISH), "english test 2");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.GERMAN), "german test 2");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.FRENCH), "french test 2");
        Assert.assertEquals(phrase.getValue().getTranslation(FxLanguage.ITALIAN), "italian test 2");

        //double remove should have no impact as it is ignored, but should return false since it does no longer exist
        Assert.assertFalse(pe.removePhrase("fx.test.1", ownMandator));
        Assert.assertTrue(pe.removePhrase("fx.test.1", testMandator));
        try {
            pe.loadPhrase("fx.test.1", ownMandator, testMandator);
            Assert.fail("Phrase to load does not exist!");
        } catch (FxNotFoundException e) {
            //expected
        }

        for (int i = 1; i < 11; i++) {
            FxString val = new FxString(true, FxLanguage.ENGLISH, "test english " + i);
            val.setTranslation(FxLanguage.GERMAN, "test german " + i);
            val.setTranslation(FxLanguage.FRENCH, "test french " + i);
            val.setTranslation(FxLanguage.ITALIAN, "test italian " + i);
            new FxPhrase("fx.test2." + i, val, "tag " + i).save();
        }
        Assert.assertEquals(pe.loadPhrases("fx.test2.").size(), 10);
        Assert.assertEquals(pe.removePhrases("fx.test2.", ownMandator), 10);

        for (int i = 1; i < 21; i++) {
            FxString val = new FxString(true, FxLanguage.ENGLISH, "test english " + i);
            val.setTranslation(FxLanguage.GERMAN, "test german " + i);
            val.setTranslation(FxLanguage.FRENCH, "test french " + i);
            val.setTranslation(FxLanguage.ITALIAN, "test italian " + i);
            new FxPhrase("fx.test3." + i, val, "tag " + i).save();
        }
        for (int i = 10; i < 21; i++) { //keys 10-20 "overlap" for both mandators
            FxString val = new FxString(true, FxLanguage.ENGLISH, "test english " + i);
            val.setTranslation(FxLanguage.GERMAN, "test german " + i);
            val.setTranslation(FxLanguage.FRENCH, "test french " + i);
            val.setTranslation(FxLanguage.ITALIAN, "test italian " + i);
            new FxPhrase(testMandator, "fx.test3." + i, val, "tag " + i).save();
        }
        final List<FxPhrase> loadedTest = pe.loadPhrases("fx.test3.", testMandator, ownMandator);
        Assert.assertEquals(loadedTest.size(), 20);
        Assert.assertEquals(loadedTest.get(12).getMandator(), testMandator);

        final List<FxPhrase> loadedOwn = pe.loadPhrases("fx.test3.", ownMandator, testMandator);
        Assert.assertEquals(loadedOwn.size(), 20);
        Assert.assertEquals(loadedOwn.get(12).getMandator(), ownMandator);

        Assert.assertEquals(pe.removeMandatorPhrases(ownMandator), 20);
        Assert.assertEquals(pe.removeMandatorPhrases(testMandator), 11); /* 10-20 */
        Assert.assertEquals(pe.loadPhrases("fx.", ownMandator, testMandator).size(), 0);
    }

    @Test
    public void phraseTreeNodeTest() throws FxApplicationException {
        final long ownMandator = FxContext.get().getTicket().getMandatorId();
        PhraseEngine pe = EJBLookup.getPhraseEngine();
        pe.clearTree(testMandator);
        pe.removeMandatorPhrases(testMandator);
        pe.clearTree(ownMandator);
        pe.removeMandatorPhrases(ownMandator);
        new FxPhrase(ownMandator, "fx.node.root", new FxString(true, FxLanguage.ENGLISH, "Root Node"), "root").save();
        new FxPhrase(ownMandator, "fx.node.child.1", new FxString(true, FxLanguage.ENGLISH, "Child Node 1"), "child 1").save();
        new FxPhrase(ownMandator, "fx.node.child.2", new FxString(true, FxLanguage.ENGLISH, "Child Node 2"), "child 2").save();
        FxPhrase child3 = new FxPhrase(ownMandator, "fx.node.child.3", new FxString(true, FxLanguage.ENGLISH, "Child Node 3"), "child 3").save();
        FxPhrase child4 = new FxPhrase(ownMandator, "fx.node.child.4", new FxString(true, FxLanguage.ENGLISH, "Child Node 4"), "child 4").save();
        FxPhraseTreeNode nodeRoot = FxPhraseTreeNode.createRootNode("fx.node.root").save();
        FxPhraseTreeNode.createChildNode(nodeRoot, "fx.node.child.1").save();
        FxPhraseTreeNode.createChildNode(nodeRoot, "fx.node.child.2", ownMandator).save();
        FxPhraseTreeNode.createChildNode(nodeRoot, child3).save();
        FxPhraseTreeNode.createChildNode(nodeRoot, ownMandator, child4).save();

        List<FxPhraseTreeNode> tree = pe.loadPhraseTree(false, ownMandator);
        Assert.assertEquals(tree.size(), 1, "Exactly one root node should exist!");
        nodeRoot = tree.get(0);
        Assert.assertTrue(nodeRoot.hasChildren());
        Assert.assertEquals(nodeRoot.getChildren().size(), 4);
        Assert.assertEquals(nodeRoot.getChildren().get(0).getPhrase().getTag(), "child 1");
        Assert.assertEquals(nodeRoot.getChildren().get(1).getPhrase().getTag(), "child 2");
        Assert.assertEquals(nodeRoot.getChildren().get(2).getPhrase().getTag(), "child 3");
        Assert.assertEquals(nodeRoot.getChildren().get(3).getPhrase().getTag(), "child 4");

        //move tests
        FxPhraseTreeNode childNode2 = nodeRoot.getChildren().get(1);
        pe.moveTreeNode(childNode2.getId(), childNode2.getMandatorId(), -1);
        nodeRoot = pe.loadPhraseTreeNode(nodeRoot.getId(), nodeRoot.getMandatorId(), false, ownMandator);
        Assert.assertEquals(nodeRoot.getChildren().size(), 4);
        Assert.assertEquals(nodeRoot.getChildren().get(0).getPhrase().getTag(), "child 2");
        Assert.assertEquals(nodeRoot.getChildren().get(1).getPhrase().getTag(), "child 1");
        Assert.assertEquals(nodeRoot.getChildren().get(2).getPhrase().getTag(), "child 3");
        Assert.assertEquals(nodeRoot.getChildren().get(3).getPhrase().getTag(), "child 4");
        FxPhraseTreeNode childNode1 = nodeRoot.getChildren().get(1);
        pe.moveTreeNode(childNode1.getId(), childNode1.getMandatorId(), 2);
        nodeRoot = pe.loadPhraseTreeNode(nodeRoot.getId(), nodeRoot.getMandatorId(), false, ownMandator);
        Assert.assertEquals(nodeRoot.getChildren().size(), 4);
        Assert.assertEquals(nodeRoot.getChildren().get(0).getPhrase().getTag(), "child 2");
        Assert.assertEquals(nodeRoot.getChildren().get(1).getPhrase().getTag(), "child 3");
        Assert.assertEquals(nodeRoot.getChildren().get(2).getPhrase().getTag(), "child 4");
        Assert.assertEquals(nodeRoot.getChildren().get(3).getPhrase().getTag(), "child 1");
        pe.moveTreeNode(childNode1.getId(), childNode1.getMandatorId(), -3);
        nodeRoot = pe.loadPhraseTreeNode(nodeRoot.getId(), nodeRoot.getMandatorId(), false, ownMandator);
        Assert.assertEquals(nodeRoot.getChildren().size(), 4);
        Assert.assertEquals(nodeRoot.getChildren().get(0).getPhrase().getTag(), "child 1");
        Assert.assertEquals(nodeRoot.getChildren().get(1).getPhrase().getTag(), "child 2");
        Assert.assertEquals(nodeRoot.getChildren().get(2).getPhrase().getTag(), "child 3");
        Assert.assertEquals(nodeRoot.getChildren().get(3).getPhrase().getTag(), "child 4");

        pe.removeTreeNode(nodeRoot.getId(), nodeRoot.getMandatorId());
        tree = pe.loadPhraseTree(false, ownMandator);
        Assert.assertEquals(tree.size(), 4, "All 4 child nodes should be moved up and changed to become root nodes");
        Assert.assertEquals(tree.get(2).getPhrase().getTag(), "child 3"); //verify position of node 3

        //move test in root nodes
        pe.moveTreeNode(childNode1.getId(), childNode1.getMandatorId(), 2);
        tree = pe.loadPhraseTree(false, ownMandator);
        Assert.assertEquals(tree.size(), 4);
        Assert.assertEquals(tree.get(0).getPhrase().getTag(), "child 2");
        Assert.assertEquals(tree.get(1).getPhrase().getTag(), "child 3");
        Assert.assertEquals(tree.get(2).getPhrase().getTag(), "child 1");
        Assert.assertEquals(tree.get(3).getPhrase().getTag(), "child 4");

        pe.clearTree(ownMandator);
        pe.removeMandatorPhrases(ownMandator);
    }

    private boolean checkNodeResult(FxPhraseQueryResult result, int resultIndex, String phrase, long phraseMandator) {
        if (result.getResult().size() < (resultIndex + 1))
            return false;
        FxPhrase p = result.getResult().get(resultIndex);
        return p.getTag().equals("Tag " + phrase) && p.getMandator() == phraseMandator;
    }

    private void assignPhrase(PhraseEngine pe, String key, long phraseMandator, FxPhraseTreeNode node, long assignmentMandator, int pos) throws FxNotFoundException, FxNoAccessException {
        FxPhrase phrase = pe.loadPhrase(key, phraseMandator);
        pe.assignPhrase(assignmentMandator, node.getId(), node.getMandatorId(), phrase.getId(), phrase.getMandator(), pos, false);
    }

    private long createPhrase(PhraseEngine pe, String key, String name, long mandator, FxPhraseTreeNode node, long assignmentMandator, long pos) throws FxNoAccessException, FxNotFoundException {
        FxString val = new FxString(true, FxLanguage.ENGLISH, name);
        val.setTranslation(FxLanguage.GERMAN, name + " german");
        val.setTranslation(FxLanguage.FRENCH, name + " french");
        val.setTranslation(FxLanguage.ITALIAN, name + " italian");
        long phraseId = pe.savePhrase(key, val, "Tag " + name, mandator);
        pe.assignPhrase(assignmentMandator, node.getId(), node.getMandatorId(), phraseId, mandator, pos, false);
        return phraseId;
    }

    private FxPhraseTreeNode createNode(PhraseEngine pe, String phraseKey, String name, long phraseMandator, long nodeMandator, FxPhraseTreeNode parentNode) throws FxNotFoundException, FxNoAccessException {
        try {
            FxContext.startRunningAsSystem();
            FxString val = new FxString(true, FxLanguage.ENGLISH, name);
            val.setTranslation(FxLanguage.GERMAN, name + " german");
            val.setTranslation(FxLanguage.FRENCH, name + " french");
            val.setTranslation(FxLanguage.ITALIAN, name + " italian");
            pe.savePhrase(phraseKey, val, "Tag " + name, phraseMandator);
            if (parentNode == null)
                return FxPhraseTreeNode.createRootNode(nodeMandator, phraseKey, phraseMandator).save();
            else
                return FxPhraseTreeNode.createChildNode(parentNode, nodeMandator, phraseKey, phraseMandator).save();
        } finally {
            FxContext.stopRunningAsSystem();
        }
    }

    @DataProvider(name = "fallbackProvider")
    public Boolean[][] fallbackProvider() {
        return new Boolean[][]{{Boolean.FALSE}, {Boolean.TRUE}};
    }

    @Test(dataProvider = "fallbackProvider")
    public void phraseTreeSearchTest(Boolean languageFallback) throws FxApplicationException {
        final long ownMandator = FxContext.get().getTicket().getMandatorId();
        PhraseEngine pe = EJBLookup.getPhraseEngine();
        pe.clearTree(testMandator);
        pe.removeMandatorPhrases(testMandator);
        pe.clearTree(ownMandator);
        pe.removeMandatorPhrases(ownMandator);
        for (int i = 0; i < 10; i++) {
            FxString val = new FxString(true, FxLanguage.ENGLISH, "test english #" + (i + 1) + "#");
            val.setTranslation(FxLanguage.GERMAN, "test german #" + (i + 1) + "#");
            val.setTranslation(FxLanguage.FRENCH, "test french #" + (i + 1) + "#");
            val.setTranslation(FxLanguage.ITALIAN, "test italian #" + (i + 1) + "#");
            pe.savePhrase("fx.phrase.search.test.own." + (i + 1), val, "Test search tag own #" + (i + 1) + "#", ownMandator);
        }
        try {
            FxContext.startRunningAsSystem();
            for (int i = 0; i < 10; i++) {
                FxString val = new FxString(true, FxLanguage.ENGLISH, "test english #" + (i + 1) + "#");
                val.setTranslation(FxLanguage.GERMAN, "test german #" + (i + 1) + "#");
                val.setTranslation(FxLanguage.FRENCH, "test french #" + (i + 1) + "#");
                val.setTranslation(FxLanguage.ITALIAN, "test italian #" + (i + 1) + "#");
                pe.savePhrase("fx.phrase.search.test.other." + (i + 1), val, "Test search tag other #" + (i + 1) + "#", testMandator);
            }
        } finally {
            FxContext.stopRunningAsSystem();
        }

        FxPhraseQuery query = new FxPhraseQuery();
        query.setLanguageFallback(languageFallback);
        query.setSearchLanguage(FxLanguage.GERMAN);
        query.setResultLanguage(FxLanguage.GERMAN);
        query.setKeyQuery("fx.phrase.search.");
        query.setKeyMatchMode(FxPhraseQuery.MatchMode.STARTS_WITH);
        query.setSortMode(FxPhraseQuery.SortMode.VALUE_ASC);
        FxPhraseQueryResult result = pe.search(query, 1, 10);
        Assert.assertEquals(result.getCurrentPage(), 1);
        Assert.assertEquals(result.getPageSize(), 10);
        Assert.assertEquals(result.getTotalResults(), 20);
        Assert.assertEquals(result.getTotalPages(), 2);

        query.setPhraseMandators(ownMandator);
        query.setFetchFullPhraseInfo(true);
        result = pe.search(query, 1, 10);
        Assert.assertEquals(result.getCurrentPage(), 1);
        Assert.assertEquals(result.getPageSize(), 10);
        Assert.assertEquals(result.getTotalResults(), 10);
        Assert.assertEquals(result.getTotalPages(), 1);
        for (int i = 0; i < result.getResultCount(); i++) {
            Assert.assertEquals(result.getResult().get(i).getValue().getTranslatedLanguages().length, 4);
            Assert.assertEquals(result.getResult().get(i).getMandator(), ownMandator);
        }

        query.setPhraseMandators(testMandator);
        query.setFetchFullPhraseInfo(false);
        query.setResultLanguage(FxLanguage.ITALIAN);
        result = pe.search(query, 1, 10);
        Assert.assertEquals(result.getCurrentPage(), 1);
        Assert.assertEquals(result.getPageSize(), 10);
        Assert.assertEquals(result.getTotalResults(), 10);
        Assert.assertEquals(result.getTotalPages(), 1);
        for (int i = 0; i < result.getResultCount(); i++) {
            Assert.assertEquals(result.getResult().get(i).getValue().getTranslatedLanguages().length, 1);
            Assert.assertTrue(result.getResult().get(i).getValue().translationExists(FxLanguage.ITALIAN));
            Assert.assertEquals(result.getResult().get(i).getMandator(), testMandator);
        }

        query.reset();
        query.setLanguageFallback(languageFallback);
        query.setSearchLanguage(FxLanguage.GERMAN);
        query.setResultLanguage(FxLanguage.ITALIAN);
        query.setFetchFullPhraseInfo(false);
        query.setPhraseMandators(ownMandator);
        query.setValueMatchMode(FxPhraseQuery.MatchMode.EXACT);
        query.setValueQuery("test german #5#");
        result = pe.search(query, 1, 10);
        Assert.assertEquals(result.getCurrentPage(), 1);
        Assert.assertEquals(result.getPageSize(), 10);
        Assert.assertEquals(result.getTotalResults(), 1);
        Assert.assertEquals(result.getTotalPages(), 1);
        Assert.assertEquals(result.getResult().get(0).getSingleValue(), "test italian #5#");

        //build a simple search tree: (own > test in this view)
        // A(own) - A1 (own) - P1 (own)
        //                   - P2 (own)
        //                   - P1 (test)
        //                   - P2 (test)
        //                   - A1.2 (test) - P3 (own)
        //                                 - P1 (test)
        //                                 - P2 (test)
        //                      
        //        - A2 (test) - P3 (own)
        //                      P1 (test)
        //                      P3 (test)
        //                
        // B(test) - P4 (own)
        //         - P5 (test)
        //         - P6 (test)
        FxPhraseTreeNode nodeA = createNode(pe, "fx.phrase.test.node.A", "A", ownMandator, ownMandator, null);
        FxPhraseTreeNode nodeA1 = createNode(pe, "fx.phrase.test.node.A1", "A1", ownMandator, ownMandator, nodeA);
        FxPhraseTreeNode nodeA12 = createNode(pe, "fx.phrase.test.node.A1.2", "A1.2", testMandator, testMandator, nodeA1);
        FxPhraseTreeNode nodeA2 = createNode(pe, "fx.phrase.test.node.A2", "A2", testMandator, testMandator, nodeA);
        FxPhraseTreeNode nodeB = createNode(pe, "fx.phrase.test.node.B", "B", testMandator, testMandator, null);
        long p1_own = createPhrase(pe, "fx.phrase.own.P1", "P1", ownMandator, nodeA1, ownMandator, 1);
        long p2_own = createPhrase(pe, "fx.phrase.own.P2", "P2", ownMandator, nodeA1, ownMandator, 2);
        createPhrase(pe, "fx.phrase.test.P1", "P1", testMandator, nodeA1, testMandator, 1);
        createPhrase(pe, "fx.phrase.test.P2", "P2", testMandator, nodeA1, testMandator, 2);
        assignPhrase(pe, "fx.phrase.test.P1", testMandator, nodeA12, testMandator, 1);
        assignPhrase(pe, "fx.phrase.test.P2", testMandator, nodeA12, testMandator, 2);
        createPhrase(pe, "fx.phrase.own.P3", "P3", ownMandator, nodeA12, ownMandator, 1);
        assignPhrase(pe, "fx.phrase.test.P1", testMandator, nodeA2, testMandator, 1);
        createPhrase(pe, "fx.phrase.test.P3", "P3", testMandator, nodeA2, testMandator, 2);
        assignPhrase(pe, "fx.phrase.own.P3", ownMandator, nodeA2, ownMandator, 1);
        createPhrase(pe, "fx.phrase.test.P5", "P5", testMandator, nodeB, testMandator, 1);
        createPhrase(pe, "fx.phrase.test.P6", "P6", testMandator, nodeB, testMandator, 2);
        createPhrase(pe, "fx.phrase.own.P4", "P4", ownMandator, nodeB, ownMandator, 1);
        List<FxPhraseTreeNode> tree = pe.loadPhraseTree(false, ownMandator, testMandator);
        Assert.assertEquals(tree.size(), 2);
        Assert.assertEquals(tree.get(0).getPhrase().getSingleValue(), "A");
        Assert.assertEquals(tree.get(0).getChildren().size(), 2);
        Assert.assertEquals(tree.get(0).getChildren().get(0).getPhrase().getSingleValue(), "A1");
        Assert.assertEquals(tree.get(0).getChildren().get(0).getChildren().size(), 1);
        Assert.assertEquals(tree.get(0).getChildren().get(0).getChildren().get(0).getPhrase().getSingleValue(), "A1.2");
        Assert.assertEquals(tree.get(0).getChildren().get(1).getPhrase().getSingleValue(), "A2");
        Assert.assertEquals(tree.get(1).getPhrase().getSingleValue(), "B");

        query.reset();
        query.setLanguageFallback(languageFallback);
        query.setResultLanguage(FxLanguage.ENGLISH);
        query.setSearchLanguage(FxLanguage.ENGLISH);
        query.setTreeNode(nodeA1.getId());
        query.setTreeNodeMandator(nodeA1.getMandatorId());
        query.setIncludeChildNodes(false);
        query.setPhraseMandators(ownMandator, testMandator);
        query.setTreeNodeMappingOwner(ownMandator, testMandator);
        query.setOwnMandatorTop(true);
        query.setSortMode(FxPhraseQuery.SortMode.POS_ASC);
        query.setMixMandators(false);
        result = pe.search(query, 1, 10);
        Assert.assertEquals(result.getTotalResults(), 4);
        Assert.assertTrue(checkNodeResult(result, 0, "P1", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 1, "P2", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 2, "P1", testMandator));
        Assert.assertTrue(checkNodeResult(result, 3, "P2", testMandator));

        result.getQuery().setIncludeChildNodes(true);
        result.getQuery().setMixMandators(true);
        result.refresh();
        //sort mode should have switched since child nodes are now included
        Assert.assertEquals(result.getQuery().getSortMode(), FxPhraseQuery.SortMode.VALUE_ASC);
        Assert.assertEquals(result.getTotalResults(), 5);
        Assert.assertTrue(checkNodeResult(result, 0, "P1", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 1, "P1", testMandator));
        Assert.assertTrue(checkNodeResult(result, 2, "P2", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 3, "P2", testMandator));
        Assert.assertTrue(checkNodeResult(result, 4, "P3", ownMandator));

        result.getQuery().setOwnMandatorTop(false);
        result.refresh();
        Assert.assertEquals(result.getTotalResults(), 5);
        Assert.assertTrue(checkNodeResult(result, 0, "P1", testMandator));
        Assert.assertTrue(checkNodeResult(result, 1, "P1", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 2, "P2", testMandator));
        Assert.assertTrue(checkNodeResult(result, 3, "P2", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 4, "P3", ownMandator));

        result.getQuery().setOwnMandatorTop(true);
        result.getQuery().setTreeNode(nodeA.getId());
        result.getQuery().setTreeNodeMandator(nodeA.getMandatorId());
        result.getQuery().setIncludeChildNodes(true);
        result.refresh();
        Assert.assertEquals(result.getTotalResults(), 6);
        Assert.assertTrue(checkNodeResult(result, 0, "P1", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 1, "P1", testMandator));
        Assert.assertTrue(checkNodeResult(result, 2, "P2", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 3, "P2", testMandator));
        Assert.assertTrue(checkNodeResult(result, 4, "P3", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 5, "P3", testMandator));

        result.getQuery().setSortMode(FxPhraseQuery.SortMode.VALUE_DESC);
        result.refresh();
        Assert.assertEquals(result.getTotalResults(), 6);
        Assert.assertTrue(checkNodeResult(result, 0, "P3", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 1, "P3", testMandator));
        Assert.assertTrue(checkNodeResult(result, 2, "P2", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 3, "P2", testMandator));
        Assert.assertTrue(checkNodeResult(result, 4, "P1", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 5, "P1", testMandator));

        //change position of P1 and P2 of own mandator
        pe.moveTreeNodeAssignment(ownMandator, nodeA1.getId(), nodeA1.getMandatorId(), p1_own, ownMandator, 1);
        result.getQuery().setSortMode(FxPhraseQuery.SortMode.POS_ASC);
        result.getQuery().setIncludeChildNodes(false);
        result.getQuery().setTreeNode(nodeA1.getId());
        result.getQuery().setTreeNodeMandator(nodeA1.getMandatorId());
        result.getQuery().setMixMandators(false);
        result.refresh();
        Assert.assertEquals(result.getTotalResults(), 4);
        Assert.assertTrue(checkNodeResult(result, 0, "P2", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 1, "P1", ownMandator));
        Assert.assertTrue(checkNodeResult(result, 2, "P1", testMandator));
        Assert.assertTrue(checkNodeResult(result, 3, "P2", testMandator));

        //find all unassigned phrases with full phrase info
        query.reset();
        query.setLanguageFallback(languageFallback);
        query.setOwnMandatorTop(true);
        query.setSortMode(FxPhraseQuery.SortMode.VALUE_ASC);
        query.setFetchFullPhraseInfo(true);
        query.setPhraseMandators(ownMandator, testMandator);
        query.setSearchLanguage(FxLanguage.GERMAN);
        query.setOnlyUnassignedPhrases(true);
        query.setTreeNodeMappingOwner(ownMandator, testMandator);
        result = query.execute(1, 10);
        Assert.assertEquals(result.getTotalResults(), 25);  //5 node names and 20 fx.phrase.search.test.xxx phrases should be unassigned
        result.getQuery().setKeyQuery("fx.phrase.search.");
        result.getQuery().setKeyMatchMode(FxPhraseQuery.MatchMode.STARTS_WITH);
        result.refresh();
        Assert.assertEquals(result.getTotalResults(), 20);  //20 fx.phrase.search.test.xxx phrases should be unassigned

        //find assignments for phrases
        List<FxPhraseTreeNodePosition> nodes = pe.getAssignedNodes("fx.phrase.test.P1", ownMandator, testMandator);
        Assert.assertTrue(nodesContain(nodes, "A1"));
        Assert.assertTrue(nodesContain(nodes, "A1.2"));
        Assert.assertTrue(nodesContain(nodes, "A2"));
    }

    private boolean nodesContain(List<FxPhraseTreeNodePosition> nodes, String postFix) {
        for(FxPhraseTreeNodePosition node: nodes)
            if(node.getNode().getPhrase().getKey().endsWith(postFix))
                return true;
        return false;
    }
}