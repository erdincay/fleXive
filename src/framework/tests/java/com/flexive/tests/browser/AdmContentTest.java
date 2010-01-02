/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.tests.browser;

import com.flexive.tests.browser.exceptions.ResultTableNotFoundException;
import com.flexive.tests.browser.exceptions.RowNotFoundException;
import com.flexive.tests.browser.exceptions.SelectItemsNotFoundException;
import com.thoughtworks.selenium.SeleniumException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.testng.Assert.fail;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Hashtable;

/**
 * Tests related to Content
 *
 * @author Laszlo Hernadi (laszlo.hernadi@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 462 $
 */
public class AdmContentTest extends AbstractBackendBrowserTest {
    private final static boolean[] SKIP_TEST_S = calcSkips();
    private final static Log LOG = LogFactory.getLog(AdmContentTest.class);
    private final static String CREATE_TYPE_LINK = "adm/structure/typeEditor.jsf?action=createType";
    private final static String PROPERTY_EDITOR_LINK = "adm/structure/propertyEditor.jsf";
    private final static String CREATE_CONTENT_LINK = "adm/content/contentEditor.jsf?action=newInstance&";
    private final static String CREATE_QUERY_LINK = "adm/search/query.jsf?action=typeSearch&typeId=";
    private final static String ADD_PROPERTY_LINK = "adm/search/query.jsf?action=propertySearch&propertyId=";

    private final static Hashtable<String, String> propertyLinkLUT = new Hashtable<String, String>();
    private final static Hashtable<String, Hashtable<String, String>> propertyLUT = new Hashtable<String, Hashtable<String, String>>();

    private final static String[] TYPES = {"Type01"};
    private final static String[] PARAMETERS = {"TestString", "TestNumber"};
    private final static long TS = System.currentTimeMillis() / 60000;
    private final static String[] BRIEF_CASES = {"TestBC"+TS, "BC02" + TS};


    /**
     * build the skip array, an array in which every test-method have an entry which
     * indicates if a method should be skiped
     * @return the skip-array
     */
    private static boolean[] calcSkips() {
        boolean[] skipList = new boolean[3];
        for (int i = 0; i < skipList.length; i++) {
            skipList[i] = !AbstractBackendBrowserTest.isForceAll();
//            skipList[i] = false;
        }
//        skipList[0] = false;
//        skipList[1] = false;
//        skipList[skipList.length - 1] = false;
        return skipList;
    }

    /**
     * only used if selenium browser must be setup for every class
     * @return <code>true</code> if all elements in the skip-array are true
     */
    protected boolean doSkip() {
        for (boolean cur : SKIP_TEST_S) {
            if (!cur) return false;
        }
        return true;
    }

    /**
     * creates a Type
     * creates 2 properties
     * creates a content (a Hahstable is needed)
     */
    @Test
    public void content_AC_1() {
        if (SKIP_TEST_S[0]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            if (AbstractBackendBrowserTest.isForceAll()) {
                Assert.assertTrue (createType(TYPES[0], TYPES[0]));
                Assert.assertTrue (createProperty(TYPES[0], PARAMETERS[0], "String1024"));
                Assert.assertTrue (createProperty(TYPES[0], PARAMETERS[1], "Number"));
            }
            Hashtable<String, Object> param = new Hashtable<String, Object>();
            param.put(PARAMETERS[0], "blaaa");
            param.put(PARAMETERS[1], "123");
            Assert.assertTrue (createContent(TYPES[0], param));
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            fail(t.getMessage(), t);
        } finally {
            logout();
        }
    }

    /**
     * edit a content and change a parameter from blaaa to xxx
     * checks if the results are ok (no results for blaaa but for xxx)
     * change it back
     */
    @Test(dependsOnMethods = {"content_AC_1"})
    public void content_AC_2() {
        if (SKIP_TEST_S[1]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            Hashtable<String, Object> param = new Hashtable<String, Object>();
            param.put(PARAMETERS[0], "blaaa");
            Hashtable<String, Object> rep = new Hashtable<String, Object>();
            rep.put(PARAMETERS[0], "xxx");

            Assert.assertTrue ( editContent(TYPES[0], param, rep));
            Assert.assertTrue( checkResults(PARAMETERS[0], "blaaa", false));
            Assert.assertTrue ( checkResults(PARAMETERS[0], "xxx", true));
            editContent(TYPES[0], rep, param);                   // change back...
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            fail(t.getMessage(), t);
        } finally {
            logout();
        }
    }

    /**
     * searche for the blaaa content and add it to a briefcase
     * exclude it from the briefcase
     * create a new briefcase and add the content to that briefcase
     * move the result from the second briefcase to the first
     * copy the result from the first briefcase to the second
     * delete the content of the first briefcase
     * save the query
     */
    @Test(dependsOnMethods = {"content_AC_2"})
    public void content_AC_3() {
        if (SKIP_TEST_S[2]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            Hashtable<String, Object> param = new Hashtable<String, Object>();
            param.put(PARAMETERS[0], "blaaa");
            Assert.assertTrue  (addToBriefCase(TYPES[0], BRIEF_CASES[0], param));
            Assert.assertTrue  (excludeFromBriefCase(BRIEF_CASES[0], TYPES[0]));
            Assert.assertTrue  (createBriefCase(BRIEF_CASES[1]));
            Assert.assertTrue ( addToBriefCase(TYPES[0], BRIEF_CASES[0], param));
            Assert.assertTrue ( moveTypeFromToBriefCase(BRIEF_CASES[0], BRIEF_CASES[1], TYPES[0]));
            Assert.assertTrue  (copyTypeFromToBriefCase(BRIEF_CASES[1], BRIEF_CASES[0], TYPES[0]));
            Assert.assertTrue  (deleteFromBriefCase(BRIEF_CASES[0], TYPES[0]));
            Assert.assertTrue  (saveCurrentQuery("Q02") || saveCurrentQuery("Q03"));
//            excludeFromBriefCase(BRIEF_CASES[1], TYPES[0]);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            fail(t.getMessage(), t);
        } finally {
            logout();
        }
    }

    /**
     * saves the current query
     * @param queryName the name the saved query should have
     * @return <code>true</code> if the query is successfully saved
     */
    private boolean saveCurrentQuery(String queryName) {
        clickOnQueryTab();
        try {
            sleep(1000);
            selenium.click("link=Save query");
//            clickAndWait("link=Save query");
            sleep(1000);
        } catch (SeleniumException se) {
//            LOG.error(se.getMessage(), se);
            /* ignore it...*/
        }
        fillInPopupForm(queryName);

        sleep(3000);

        return checkText("Saved query", true, LOG);
    }


    /**
     * excludes content by a type-name from a given briefcase
     * @param CaseName the briefcase from wich to exclude
     * @param typeName the name of the type to exclude
     * @return <code>true</code> if the briefcase has no more records
     */
    private boolean excludeFromBriefCase(String CaseName, String typeName) {
        selectBriefCase(CaseName);
        String tr = getTRContainingName(getResultTable(null), typeName, 2);
        if (selectTR(tr)) {
            selenium.click("link=exclude");
            return checkText("No records found.", true, LOG);
        }

        return false;
    }

    /**
     * delete a content from a briefcase by a type name
     * @param briefCase the name of the briefcase
     * @param typeName the name of the type
     * @return <code>true</code> in all cases
     */
    private boolean deleteFromBriefCase(String briefCase, String typeName) {
        selectBriefCase(briefCase);
        selectFrame(Frame.Content);

        String link;
        int begin, end;
        String tr = correctHTML(getTRContainingName(getResultTable(null), typeName, 2), "</a>");

        end = tr.indexOf(">delete</a>");
        if (tr.charAt(end-1) == '\'') {
            end--;
        }
        begin = tr.lastIndexOf("javascript:", end) + 11;
        tr = tr.substring(begin, end);
        link = WND + "." + tr.replaceFirst("\\(", "(" + WND + ".");
        selenium.getEval(link);
        sleep(200);
        selenium.selectFrame("relative=up");
        selenium.click("//button[@type='button']");

        return true; // TODO
    }

    /**
     * copies a content from a briefcase to an other
     *
     * @param fromCase the name of the briefcase from which to copy
     * @param toCase the name of the briefcase to which to copy
     * @param typeName the name of the type which to copy
     * @return <code>true</code> if the copy was successfull
     */
    private boolean copyTypeFromToBriefCase(String fromCase, String toCase, String typeName) {
        return copy_moveBriefCase(fromCase, toCase, typeName,"Copy to briefcase...", "Added 1 objects to \""+toCase+"\".");
    }

    /**
     * moves a content from a briefcase to an other
     *
     * @param fromCase the name of the briefcase from which to move
     * @param toCase the name of the briefcase to which to move
     * @param typeName the name of the type which to move
     * @return <code>true</code> if the copy was successfull
     */
    private boolean moveTypeFromToBriefCase(String fromCase, String toCase, String typeName) {
        return copy_moveBriefCase(fromCase, toCase, typeName,"Move to briefcase...", "Moved 1 objects from \""+fromCase+"\" to \""+toCase+"\".");
    }

    /**
     * copies or moves a content from one briefcase to an other
     * @param fromCase the name of the briefcase from which to copy / move
     * @param toCase the name of the briefcase to which to copy / move
     * @param typeName the name of the type what to copy / move
     * @param firstIndex what to search (copy / move)
     * @param msg message to test
     * @return <code>true</code> if the copy / move was successfull
     */
    private boolean copy_moveBriefCase(String fromCase, String toCase, String typeName, String firstIndex, String msg){
        selectBriefCase(fromCase);
        selectFrame(Frame.Content);

        String link;
        int begin, end;
        String html;
        String tr = getTRContainingName(getResultTable(null), typeName, 2);
        if (clickOnMoreLink(tr))   {
            html = selenium.getHTMLSource("</div", "<li ");
            begin = html.indexOf(firstIndex);

            end = html.indexOf("</div", begin) + 4;
            end = html.indexOf("</div", end);
            html = html.substring(begin, end);
            String [] li_s = html.split("<li ");
            String li;
            for (int i = li_s.length-1; i > 0; i-- ) {
                li = li_s[i];
                if (li.indexOf(toCase + " (") > 0){
//                    begin = li.indexOf("id=\"") + 4;
//                    end = li.indexOf("\"", begin);
                    link = "//li[@id='" + getID(li) + "']/a";

                    selenium.click(link);
                    sleep(300);
                    return checkText(msg, true, LOG, Frame.Top);
                }
            }
        }

        return false;
    }

    /**
     * clicks on a tr
     * @param tr the tr to click
     * @return <code>true</code> if successfull
     */
    private boolean selectTR(String tr) {
        String trID;

        int begin, ende;
        if (tr != null) {
            ende = tr.indexOf(">");
            begin = tr.indexOf("id=") + 3;
            if (begin > 3 && begin < ende) {
                ende = tr.indexOf(" ", begin);
                trID = tr.substring(begin, ende).replaceAll("\"", "");
//                sleep(500);
//                clickAndWait(trID);
                int trys = 10;
                while (trys-->0) {
                    try {
                        selenium.click(trID);
                        break;
                    } catch (SeleniumException se) {
                        selectFrame(Frame.Content);
                        sleep (200);
                    }
                }
                if (trys <=0){
                    selenium.click(trID);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * select a briefcase
     * @param briefName the name of the briefcase
     */
    private void selectBriefCase(String briefName) {
        navigateTo(NavigationTab.Search);

        String html = selenium.getHTMLSource("title", "<a ", "id=");
        String link;
        int begin;
        begin = html.indexOf("title=\"" + briefName + " ");
        begin = html.lastIndexOf("<a ", begin);
//        begin = html.indexOf("id=\"", begin) + 4;
//        ende = html.indexOf("\"", begin);
//        link = html.substring(begin, ende);
        link = getID(html, begin);

        selenium.click(link);
//        selenium.click(link);
        selenium.waitForPageToLoad("10000");

    }

    /**
     * creates a briefcase with the given name
     * @param briefName name of the briefcase
     * @return <code>true</code> if successfuly created
     */
    private boolean createBriefCase(String briefName) {
        navigateTo(NavigationTab.Search);

        String html = selenium.getHTMLSource("javascript:", "href=\"");
        String link;
        int begin;
        begin = html.indexOf("Briefcases");
        if (begin > 0 && html.indexOf("(",begin) < begin + 25) {
//            begin = html.indexOf("href=\"", begin) + 6;
//            ende = html.indexOf("\"", begin);
//            link = html.substring(begin, ende).replaceAll("javascript:", WND + ".");
            link = getID(html, "href=", begin).replaceAll("javascript:", WND + ".");
            link = link.substring(0, link.lastIndexOf(")") + 1);
            
            selenium.getEval(link);
            fillInPopupForm(briefName);
            return true;
        }

        return false; 
    }

    /**
     * fill in the text in a popup-window
     * @param value the text value to fill in
     */
    private void fillInPopupForm(String value) {
        selenium.selectFrame("relative=up");
        sleep(200);
        selenium.type("__fxPromptInput", value);
        selenium.click("//button[@type='button']");
    }

    /**
     * clicks on the more link in a tr
     * @param tr the source of the tr
     * @return <code>true</code> if the tr is found
     */
    private boolean clickOnMoreLink(String tr) {
        if (selectTR(tr)) {

            String tmp = tr.split("<td ")[1];
            int ende = tmp.indexOf("</div></td>");
            int begin = tmp.lastIndexOf(">", ende) + 1;
            tmp = "more_link_" + tmp.substring(begin, ende);
            selenium.click(tmp);
            return true;
        }

        return false;
    }

    /**
     * add a content to a briefcase
     * @param typeName the name of the type to add
     * @param briefName the name of the briefcase to put in (if not exsist, it will be created...)
     * @param param the parameter for the search
     * @return <code>true</code> if successfully added
     */
    private boolean addToBriefCase(String typeName, String briefName, Hashtable<String, Object> param) {
        searchForType(typeName, param);
        selectFrame(Frame.Content);

        String tr = getTRContainingName(getResultTable(null), typeName, 2);
//        writeHTMLtoHD("addToBC");
        String link = null;
        int begin, end;
        if (clickOnMoreLink(tr)) {
            tr = selenium.getHTMLSource("</div", "<li ");
            begin = tr.indexOf("New...");
            end = tr.indexOf("</div", begin)+ 4;
            end = tr.indexOf("</div", end);
            tr = tr.substring(begin, end);
            String [] li_s = tr.split("<li ");
            for (String li : li_s) {
                if (li.indexOf(briefName + " (") > 0){
//                    begin = li.indexOf("id=\"") + 4;
//                    end = li.indexOf("\"", begin);
//                    link = "//li[@id='" + li.substring(begin, end) + "']/a";
                    link = "//li[@id='" + getID(li) + "']/a";

                    selenium.click(link);
//                    sleep(200);
                    return true;
//                    break;
                }
            }
            if (link == null) {
                selenium.click("link=New...");
                fillInPopupForm(briefName);
            }
            int trys = 60;
            long start = System.currentTimeMillis();
            while (trys-->0) {
                if (checkText("Added 1 objects to", true, LOG, Frame.Top)){
                    LOG.info("time : " + (System.currentTimeMillis() - start) + "ms");
                    return true;
                }
                sleep (100);
            }
//            return checkText("Added 1 objects to", true, LOG);
        }

        LOG.error("can't find type \"" + typeName + "\"");

        return false;
    }

    /**
     * checks the results of a search
     * @param pName the name of a parameter to search
     * @param value the value of that parameter
     * @param expectedResult the expected result
     * @return <code>true</code> if result is expected
     */
    private boolean checkResults(String pName, Object value, boolean expectedResult) {
        clickOnQueryTab();
        fillInQuery(pName, value);
        clickAndWait("link=Search");
        sleep(1000);
        return checkText("No records found.", !expectedResult, LOG);
    }

    /**
     * searches for a type
     * @param typeName the name of the type
     * @param param the parameter which to fill in
     */
    private void searchForType(String typeName, Hashtable<String, Object> param) {
        fillLUT(typeName);

        String link = CREATE_QUERY_LINK + propertyLUT.get(typeName).get("typeId");

        loadContentPage(link);
        navigateTo(NavigationTab.Structure);
        selectFrame(Frame.NavStructures);


        if (param != null) {
            String id ;
            boolean isEditor;
            for (String pName : param.keySet()) {
                selectFrame(Frame.NavStructures);
                fillLUT(pName);
                id = propertyLUT.get(pName).get("propertyId");
                try {
                    isEditor = selenium.getEval(WND + ".parent.frames[\"contentFrame\"].isQueryEditor").equals("true");
                } catch (Throwable t) {
                    isEditor = false;
                }
                if (isEditor) {
                    try {
                        selenium.getEval(WND + ".parent.frames[\"contentFrame\"].addPropertyQueryNode(" + id + ")");
                    } catch (SeleniumException se) {
                        LOG.error(se.getMessage());
                    }
                } else {
                    link = ADD_PROPERTY_LINK + id;
                    loadContentPage(link);
                }
                fillInQuery(pName, param.get(pName));
//            writeHTMLtoHD("query", LOG);
//            selenium.type("frm:nodeInput_1_input_", param.get(pName).toString());
            }
        }

        clickAndWait("link=Search");

       // TODO return value ? 
    }

    /**
     * edit a content
     * @param typeName the name of the type
     * @param param the parameter which to find
     * @param replace the parameter which to set
     * @return <code>true</code> if the edit was successfull
     */
    private boolean editContent(String typeName, Hashtable<String, Object> param, Hashtable<String, Object> replace) {

        searchForType(typeName, param);

        selectFrame(Frame.Content);
        String tr = getTRContainingName(getResultTable(null), typeName, 2);

        int begin, ende;
        if (tr != null) {
            for (String td : tr.split("<td ")) {
                ende = td.indexOf(">edit</a>");
                if (ende < 0) continue;
                begin = td.lastIndexOf("javascript:", ende) + 11;
                td = td.substring(begin, ende).replaceAll("'", "");
                td = WND + "." + td.replaceFirst("\\(", "(" + WND + ".");
                selenium.getEval(td);
                selenium.waitForPageToLoad("10000");
                String html = selenium.getHTMLSource("<div ", "</div>");
                final String NAVID = "<div class=\"navigationDisplay\">";
                final String NAVID_ = "<div class=navigationDisplay>";
                begin = Math.max(html.indexOf(NAVID) + NAVID.length(), html.indexOf(NAVID_) + NAVID_.length());
                ende = html.indexOf("</div>", begin);
                html = html.substring(begin, ende).trim();
                fillInContent(replace);
                Assert.assertTrue (html.equals("1 - 1"));
                clickAndWait("link=Save");

                break;
            }
        }

//        selenium.getEval(WND + ".editContent(" + WND + ".flexive.util.parsePk(\"73.1\"));");

//        writeHTMLtoHD("editContent", LOG);

        return checkText("Content was updated successfully", true, LOG);
    }

    /**
     * fill in a current query (must be loaded)
     * @param pName parameter name
     * @param value value
     */
    private void fillInQuery(String pName, Object value) {
        int trys = 4;

        while (trys-- > 0) {
            try {
                selectFrame(Frame.Content);
                String html = correctHTML(selenium.getHtmlSource(), "<table ", "</table>");
                int i1, i1_, i2;
                int begin = html.indexOf("<table ");
                i1 = html.indexOf("id=\"queryEditorRoot\"", begin);
                i1_ = html.indexOf("id=queryEditorRoot", begin);
                i1 = Math.max(i1,i1_);
                i2 = html.indexOf(">", begin);
                if (i1 > i2 || i1 < 0) {
                    throw new ResultTableNotFoundException(writeHTMLtoHD("wrongResultTable", LOG, html));
                }
                Hashtable<String, String> input;
                if (begin > 0) {
                    int ende = html.indexOf("</table>", begin);
                    if (ende < 0) throw new ResultTableNotFoundException(writeHTMLtoHD("wrongResultTable", LOG, html));
                    input = getInput(html.substring(begin, ende), pName, "id", "type");
                    if (input.get("type").equals("text")) {
                        selenium.type(input.get("id"), value.toString());
                    }
                } else {
                    throw new ResultTableNotFoundException(writeHTMLtoHD("wrongResultTable", LOG, html));
                }
                break;
            } catch (RowNotFoundException rnfe) {
                sleep(300);
            }
        }
    }

    /**
     * click on the query tab from the top frame
     */
    private void clickOnQueryTab() {
        int begin, begin_, ende;

        selectFrame(Frame.Top);
        String src = selenium.getHTMLSource("<table", "</table>", "</td>");

        begin = src.indexOf("<table id=\"contentHeaderTabTbl\"");
        begin_ = src.indexOf("<table id=contentHeaderTabTbl");
        begin = Math.max(begin, begin_);
        ende = src.indexOf("</table>", begin);

        String tab = src.substring(begin, ende);

        String[] tds = tab.split("<td ");

        String link;

        for (String td : tds) {
            begin = td.indexOf(">Query</td>");
            if (begin > 0) {
//                begin = td.lastIndexOf("onclick=\"", begin) + 9;
//                ende = td.indexOf("\"", begin);
//                link = WND + "." + td.substring(begin, ende);
                link = WND + "." + getID(td, "onclick=", begin).replaceAll("=", "").replaceAll("\"", "");
                selenium.getEval(link);
                selenium.waitForPageToLoad("10000");
                return;
            }
        }
    }

    /**
     * @param tabSrc       the source of the html table
     * @param label        the casesensitive name of the item lable
     * @param neededFields the fields which should be added to the hashtable
     * @return the fields of the input tag
     */
    private Hashtable<String, String> getInput(String tabSrc, String label, String... neededFields) {
        String[] trs = tabSrc.split("<tr");
        Hashtable<String, String> input = new Hashtable<String, String>();
        String curTr;
        int begin, end;
//        int trys = 4;

//        while (trys-- > 0) {
            for (int i = 1; i < trs.length; i++) {
                curTr = trs[i];
                begin = curTr.indexOf(label + "</label>");
                if (begin > 0) {
                    begin = curTr.indexOf("<input");
                    end = curTr.indexOf(">", begin);
                    curTr = curTr.substring(begin, end);

                    for (String cf : neededFields) {
                        cf = cf.trim();
                        String tmp = cf;
                        if (!tmp.endsWith("=\"")) {
                            if (tmp.endsWith("=")) {
                                tmp += "\"";
                            } else {
                                tmp += "=\"";
                            }
                        }
                        begin = curTr.indexOf(tmp) + tmp.length();
                        end = curTr.indexOf("\"", begin);
                        input.put(cf, curTr.substring(begin, end));
                    }
                    return input;
                }
            }
//            sleep(300);
//        }
        throw new RowNotFoundException("Row containing \"" + label + "</label>\"");
    }

    /**
     * Create a type whit a given name
     * @param name the name of the type to create
     * @param label the label of the type to create
     * @return <code>true</code> if successfull
     */
    private boolean createType(String name, String label) {
        loadContentPage(CREATE_TYPE_LINK);
        selectFrame(Frame.Content);

        int trys = 3;
        while (trys-- > 0) {
            try {
                selenium.type("frm:name", name);
            } catch (SeleniumException se) {
                LOG.error(se.getMessage());
                sleep(1000);
            }
        }
        selenium.type("frm:description_input_1", label);

        clickAndWait("link=Create");
        return checkText("Successfully created type " + name + ".", null, LOG);
    }

    /**
     * create a property of a type
     * @param typeName the name of the type
     * @param name the name of the property
     * @param dataType the datatype of the new property
     * @return always <code>true</code> due FX-727
     */
    private boolean createProperty(String typeName, String name, String dataType) {

        fillLUT(typeName);
        String link = propertyLinkLUT.get(typeName);

        loadContentPage(link);
        selectFrame(Frame.Content);

        selenium.type("frm:name", name);
        selenium.type("frm:label_input_1", name);

        if (dataType != null) selenium.select("frm:dataType", dataType);

        sleep(1000);

        clickAndWait("link=Create this property");
        
//        return checkText("Property successfully created.", true, LOG);
        // TODO fix PropertyEditorBean.createContent so that messages after a successfull create were shown
        return true;
    }

    /**
     * fill the LUT with a type name this is extracted from the menu
     * @param typeName the name of the type
     */
    private void fillLUT(String typeName) {
        if (propertyLUT.get(typeName) != null) return;

        int trys = 4;

        Hashtable<String, String> params = null;
        String html = "";
        while (trys-- > 0) {
            try {
                navigateTo(NavigationTab.Structure);
                selectFrame(Frame.NavStructures);

                html = selenium.getHtmlSource();
                params = buildHashtableFromMenu(html, "{\"title\":'" + typeName + "'");
                break;
            } catch (SelectItemsNotFoundException sinf) {
                sleep(300);
            }
        }
        if (trys <= 0) {
            params = buildHashtableFromMenu(html, "{\"title\":'" + typeName + "'");
        }
        String link = PROPERTY_EDITOR_LINK + "?action=createProperty&id=" + params.get("typeId") + "&nodeType=" + params.get("nodeType");
        propertyLinkLUT.put(typeName, link);
        propertyLUT.put(typeName, params);
    }

    /**
     * creates a content
     * @param typeName the type name of what to create
     * @param contents the parameters to fill in
     * @return <code>true</code> if successfull
     */
    private boolean createContent(String typeName, Hashtable<String, Object> contents) {
// http://localhost:8080/flexive/adm/content/contentEditor.jsf?action=newInstance&typeId=10&nodeId=-1
        fillLUT(typeName);
        Hashtable<String, String> param = propertyLUT.get(typeName);

        String link = CREATE_CONTENT_LINK + "typeId=" + param.get("typeId") + "&nodeId=-1";

        loadContentPage(link);
        selectFrame(Frame.Content);
        sleep(100);
//        writeHTMLtoHD("Content");

        fillInContent(contents);

        clickAndWait("link=Create");

        return checkText("Content was created ", true, LOG);
    }

    /**
     * fill the content in
     * @param contents key-value pairs representing the content
     */
    private void fillInContent(Hashtable<String, Object> contents) {

        String htmlSrc = selenium.getHTMLSource("<div ", "</div>", "<input ");

        final String MARK = "<div class=\"display\" title=\"\" style=\"\">";
        String[] items1 = htmlSrc.split(MARK);
        final String MARK_ = "<div class=display title= style=>";
        String[] items2 = htmlSrc.split(MARK_);
        String [] items = new String[items1.length + items2.length];
        System.arraycopy(items1,0,items,0,items1.length);
        System.arraycopy(items2,0,items,items1.length,items2.length);

        String tmpS;
        String name;
        Hashtable<String, Hashtable<String, String>> inputs = new Hashtable<String, Hashtable<String, String>>();
        Hashtable<String, String> params;
        int begin;
        int b, e;
        for (int i = 1; i < items.length; i++) {
            tmpS = items[i];
            begin = tmpS.indexOf("</div>");
            name = tmpS.substring(0, begin).trim();
            begin = tmpS.indexOf("<input ", begin);
            params = new Hashtable<String, String>();
            if (begin > 0) {
                begin += 7;
                b = tmpS.indexOf("id=\"", begin) + 4;
                if (b > 0) {
                    e = tmpS.indexOf("\"", b);
                } else {
                    b = tmpS.indexOf("id=", begin) + 3;
                    e = tmpS.indexOf(" ", b);
                }
                params.put("id", tmpS.substring(b, e));

                b = tmpS.indexOf("type=\"", begin) + 6;
                if (b > 0) {
                    e = tmpS.indexOf("\"", b);
                } else {
                    b = tmpS.indexOf("type=", begin) + 5;
                    e = tmpS.indexOf(" ", b);
                }
                params.put("type", tmpS.substring(b, e).toLowerCase());

                inputs.put(name, params);
            }
        }

        String type;
        String id;
        Object value;
        for (String curName : contents.keySet()) {
            params = inputs.get(curName);
            if (params != null) {
                type = params.get("type");
                id = params.get("id");
                value = contents.get(curName);
                if (type.equals("text")) {
                    selenium.type(id, value.toString());
                } else if (type.equals("checkbox")) {
                    setCheckboxState(id, (Boolean) value);
                }

            } else {
//  TODO throw not found...               
            }
        }
    }
}
