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

import org.testng.annotations.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;

import com.thoughtworks.selenium.SeleniumException;
import com.flexive.tests.browser.exceptions.*;

/**
 * Base class for backend browser tests.
 * Context menu support needs a patched server as described at http://forums.openqa.org/thread.jspa?messageID=20582.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class AbstractBackendBrowserTest extends AbstractSeleniumTest {
    protected static final String WND = "selenium.browserbot.getCurrentWindow()";
    private static final String CONTEXTPATH = "/flexive";

    private static final Log LOG = LogFactory.getLog(AbstractBackendBrowserTest.class);
    protected final static String MANDATOR_OVERVIEW_PAGE = "/adm/main/mandator/overview.jsf";

    private final static boolean FORCE_ALL = true;
//    private final static boolean FORCE_ALL = false;
    protected final static String ACL_OVERVIEW_PAGE = "/adm/main/acl/aclOverview.jsf";
    protected final static String USER_OVERVIEW_PAGE = "/adm/main/userGroup/overview.jsf";
    private static int numSkip;

    protected abstract boolean doSkip();

    private final static ArrayList<String> skipList = new ArrayList<String>();
    private static String curTestName = "";
    private static int lastCount = 0;
    private final static Hashtable<String, String> REP_LUT = new Hashtable<String, String>();

    private static long start = 0;
    private static Boolean mandatorsCreated = false;

    private static Boolean grupsCreated = false;

    protected FlexiveSelenium selenium = AbstractSeleniumTest.getSeleniumInstance();

//    private String lastNotFound = "";
//    private File lastNotFoundF = null;

    private static LinkedList<TestMethod> methods = new LinkedList<TestMethod>();

    private static String lastMName;
    private static long lastStartTime;

    /**
     * Searches the result table on the page
     *
     * @param url the url to load first (if <code>null</code> it is ignored)
     * @return the result table extracted from the page
     * @throws ResultTableNotFoundException if the table is not found
     */
    protected String getResultTable(String url) throws ResultTableNotFoundException {
        int end;

        int i;

        if (url != null) {
            loadContentPage(url);
        }
        String str = "";

        int trys = 20;
        while (trys-- > 0) {
            try {
                selectFrame(Frame.Content);
                str = selenium.getHTMLSource("<table ", "</table>", "/images/messages/");
                String [] tmp = str.split("<table ");
                switch (tmp.length) {
                    case 1: // not found
                        break;
                    case 2: // 1 table found --> this will be it
                        str = tmp[1];
                        end = str.indexOf("</table>");
                        return "<table " + str.substring(0, end);
                    default:
                        for (i = 1; i < tmp.length; i++) {
                            if (tmp[i].indexOf("/images/messages/") < 0) {
                                str = tmp[i];
                                end = str.indexOf("</table>");
                                return "<table " + str.substring(0, end);
                            }

                        }
                }
            } catch (Throwable t) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // ignoring
                }
            }
        }
        throw new ResultTableNotFoundException(writeHTMLtoHD("wrongResultTable", LOG, str));
    }

    /**
     * creates a mandator
     * @param name name of the mandator
     * @param setActive set the mandator active or not
     * @return <code>true</code> if the mandator was created
     * @see com.flexive.tests.browser.AbstractBackendBrowserTest#createMandator_(String, boolean)
     */
    protected boolean createMandator(String name, boolean setActive) {
        int trys = 20;
        while (trys-->0){
            try {
                return createMandator_(name,setActive);
            } catch (Throwable t) {
                try {
                    Thread.sleep (200);
                } catch (InterruptedException e) {
                    // ignoring
                }
            }
        }
        return createMandator_(name,setActive);
    }

    /**
     * create the needed mandators
     */
    protected void createMandators() {
        synchronized (mandatorsCreated) {
            if (mandatorsCreated) {
                LOG.info("mandators already created...");
            } else {
                LOG.info("creating mandators...");
                new AdmMandatorTest().createMandators_();
                mandatorsCreated = true;
            }
        }
    }

    /**
     * create the needed groups
     */
    protected void createGroup() {
        synchronized (grupsCreated) {
            createMandators();
            if (grupsCreated) {
                LOG.info("groups already created...");
            } else {
                LOG.info("creating groups...");
                new AdmUserGroupTest().createGroup_();
                grupsCreated = true;
            }
        }
    }

    public static Boolean isGrupsCreated() {
        return grupsCreated;
    }

    public static boolean isMandatorsCreated() {
        return mandatorsCreated;
    }

    /**
     * creates a mandator
     * @param name name of the mandator
     * @param setActive set the mandator active or not
     * @return <code>true</code> if the mandator was created
     */
    private boolean createMandator_(String name, boolean setActive) {
        loadContentPage(MANDATOR_OVERVIEW_PAGE);
        selectFrame(Frame.Content);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // ignoring
        }
        selenium.click("link=Create");
        selenium.waitForPageToLoad("3000");
        selenium.type("frm:name", name);
        if (setActive) {
            selenium.click("frm:active");
        }
        selenium.click("link=Create");
        selenium.waitForPageToLoad("3000");

        return !selenium.isTextPresent("Mandator " + name + " already exists!");
    }

    /**
     * checks if the given text is present on the current content page
     * @param text the text to check
     * @param expectedResult should it be present (<code>null</code> == don't care)
     * @param LOG the Log where to log errors
     * @return true if the result is as expected
     */
    protected boolean checkText(String text, Boolean expectedResult, Log LOG) {
        return checkText(text, expectedResult, LOG, Frame.Content);
    }

    /**
     * checks if the given text is present on the current content page
     * @param text the text to check
     * @param expectedResult should it be present (<code>null</code> == don't care)
     * @param LOG the Log where to log errors
     * @param frameToLook the Frame to search the text
     * @return true if the result is as expected
     */
    protected boolean checkText(String text, Boolean expectedResult, Log LOG, Frame frameToLook) {
        int trys = 10;
        boolean succesfull = false;

        while (trys-->0) {
            try {
                selectFrame(frameToLook);
                succesfull = selenium.isTextPresent(text);
                break;
            } catch (SeleniumException se) {
                sleep(400);
            }
        }

        try {
            succesfull = succesfull ^ (!expectedResult);
        } catch (NullPointerException npe) {
            return succesfull;
        }

        if (!succesfull) {
            LOG.error("can't find text \"" + text + "\"");
            LOG.error(writeHTMLtoHD("checkText", LOG));
            printFXMessages(LOG);
        }

        return succesfull;
    }

    /**
     * prints the current FXMessages to the log
     * @param LOG the Log to print to
     */
    private void printFXMessages(Log LOG) {
        String tmpHTML = selenium.getHTMLSource("<div ", "<tbody>", "</tbody>", "<tr>", "<td>", "</td>");

        int begin = tmpHTML.indexOf("<div id=\"fxMessages\"");
        if (begin > 0) {
            int end;
            begin = tmpHTML.indexOf("<tbody>", begin);
            end = tmpHTML.indexOf("</tbody>", begin);
            if (begin * end > 1) {
                String tmpTbody = tmpHTML.substring(begin, end);
                String [] trS = tmpTbody.split("<tr>");
                String [] tdS;
                String tmpMSG;
                for (String tr : trS) {
                    tdS = tr.split("<td>");
                    if (tdS.length == 3) {
                        tmpMSG = tdS[2].substring(0,tdS[2].lastIndexOf("</td>"));
                        if (tdS[1].indexOf("error") > 0) {
                            LOG.error(tmpMSG);
                            throw new RuntimeException(tmpMSG);
                        } else {
                            LOG.warn(tmpMSG);
                        }
                    }
                }
            }
        }
    }

    /**
     * A small function to set a value of a checkbox
     * @param locator the "name" / locator of the checkbox
     * @param value the value to set
     * @throws com.thoughtworks.selenium.SeleniumException if something went wrong (= wrong name)
     */
    protected void setCheckboxState(String locator, boolean value) throws SeleniumException {
        SeleniumException lastEx = null;
        for (int trys = 10; trys-->0;) {
            try {
            // if the current value of the locator != the value to set
            if (selenium.isChecked(locator) ^ value) {
                selenium.click(locator);
            }
                return;
            } catch (SeleniumException se) {
                lastEx = se;
                sleep(200); // maybe we need to wait...
            }
        }
        throw lastEx;
    }

    /**
     * tests if the given text is present on the navigation tab
     * @param text text to lookup
     * @param notFounds a list where the result should be stored
     * @return <code>true</code> if found
     */
    protected boolean testIfNavigationTabTextPresent(String text, ArrayList<String> notFounds) {
        if (selenium.isTextPresent(text)) {
            notFounds.add("+\tText \"" + text + "\" is present");
        } else {
            String htmlSource = selenium.getHTMLSource("<div ", "</div>");
            if (htmlSource.indexOf("{\"title\":'ACL',") > 0 || htmlSource.indexOf("<div class=\"treeNodeV3Node\">" + text + "</div>") > 0) {
                notFounds.add("+\tText \"" + text + "\" is present");
            } else {
                notFounds.add("-\tText \"" + text + "\" is NOT present");
                return false;
            }
        }
        return true;
    }

    public static boolean isForceAll() {
        return FORCE_ALL;
    }

    /**
     * add the given ACL permissions
     *
     * @param name name of the ACL to edit / mod. permissions
     * @param groupName name of the usergroup for the new premission (have to be something like "Mandator: Group")
     * @param read read-premission
     * @param edit edit-premission
     * @param relate relate-presmission
     * @param create create-presmission
     * @param delete delete-presmission
     * @param export export-presmission
     * @param expectedResult true : it has to work (the save) OR false: it must not work (double group name...) OR null: just don't care
     *      WARNING: if <code>true</code> or <code>false</code> givven, it will fail otherwise
     * @param LOG the Log to log...
     * @return according to expectedResult, so if result == expectedResult --> true if expectedResult == null then just the result
     */
    protected boolean addACLPermission(String name, String groupName, boolean read, boolean edit, boolean relate, boolean create, boolean delete, boolean export, Boolean expectedResult, Log LOG) {
        loadContentPage(ACL_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        boolean [] flags = {read, edit, relate, create,  delete, export};
        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),name,":editButton_"));

        String src = "";
        int trys;
        trys = 20;
        while (trys-- > 0) {
            try {
                selenium.click("link=Add row");
                break;
            } catch (SeleniumException t){
                sleep(200);
            }
        }
        sleep(500);

        trys = 20;
        while (trys-- > 0) {
            try {
                src = selenium.getHTMLSource("<legend>", "<table", "</table>", "<tr", "</tr>", "<td>");
                break;
            } catch (SeleniumException t){
                sleep(200);
            }
        }

        int begin = src.indexOf("<legend>");
        if (begin < 0) return false;
        begin = src.indexOf("<table", begin);
        int ende = src.indexOf("</table>", begin);
        src = src.substring(begin, ende);
        String [] trS = src.split("<tr");

        String tr = trS[trS.length-1];
        ende = tr.indexOf("</tr>");
        tr = tr.substring(0,ende);
        String [] tdS = tr.split("<td>");

        selenium.select(getName(tdS[1]), groupName);

        int i = 2;
        for (boolean value : flags) {
            setCheckboxState("name=" + getName(tdS[i++]), value);
        }

        clickAndWait("link=Save");

        return checkText("Access control list '" + name + "' was saved successfully.", expectedResult, LOG);
    }

    /**
     * get the name of a tag
     *
     * @param td the td in wich to search
     * @return the name
     * @throws SubstringNotFoundException if the name is not found
     */
    private String getName(String td) {
        int begin, end;
        begin = td.indexOf("name=\"") + 6;
        end = td.indexOf("\"", begin);
        try {
        return td.substring(begin, end);
        } catch (StringIndexOutOfBoundsException sout) {
            throw new SubstringNotFoundException("String \"name=\\\" in \"" + td + "\" not found!");
        }
    }

    /**
     * Mark all the given values in the select list
     *
     * @param selectName the name of the selectlist
     * @param values if not <code>null</code> the groups to select if <code>""</code> then all groups will be removed (from user)
     * @throws com.thoughtworks.selenium.SeleniumException if a value is not found
     */
    protected void markInSelectList(String selectName, String ... values){
        try {
             if (values.length == 1 && values[0].length() == 0) {// only a "" given
                 try {
                 for (String role: selenium.getSelectedLabels(selectName)) {
                     selenium.removeSelection(selectName, role);
                 }
                 } catch (SeleniumException se) {
                     // when there is nothing selected --> nothing to do...
                 }
             } else {
                 for (String role: values) {
                     selenium.select(selectName, role);
                 }
             }
         } catch (NullPointerException npe) {
             // ignore it
         }
    }

    /**
     * edit the roles of a usergroup
     *
     * @param groupName the name of the group to edit
     * @param expectedResult what is expected to happen
     * @param LOG the Log to log
     * @param roles the roles to set
     * @return <code>true</code> if expected
     */
    protected boolean editUserGroup(String groupName, Boolean expectedResult, Log LOG, String... roles) {

        loadContentPage(USER_OVERVIEW_PAGE);

        selectFrame(Frame.Content);
        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),groupName,":editButton_"), 30000);
        markInSelectList("frm:roles", roles);
        sleep(200);
        clickAndWait("link=Save", 30000);
//        selenium.waitForPageToLoad("30000");

        return checkText("Group " + groupName + " was successfully updated.", expectedResult, LOG);
    }

    /**
     * @see com.flexive.tests.browser.AbstractBackendBrowserTest#buildHashtableFromMenu(String, String, int)
     */
    protected Hashtable<String, String> buildHashtableFromMenu(String src, String prefix) {
        return buildHashtableFromMenu(src, prefix, 0);
    }

    /**
     * builds a Hashtable from the menu (context menu)
     * it adds all the key, value pairs into the hashtable
     *
     * @param src the html-source to search in
     * @param prefix the prefix to find
     * @param begin the beginindex
     * @return the resulting hashtable
     */
    protected Hashtable<String, String> buildHashtableFromMenu(String src, String prefix, int begin) {
        int index = src.indexOf(prefix, begin);
        int ende = Math.min(src.indexOf("[", index), src.indexOf("}", index));
        if (index <= 0) throw new SelectItemsNotFoundException();
//        index = src.indexOf("typeId", index) + 7;
        String[] pars = (src.substring(index, ende) + "''").split(",");
        Hashtable<String, String> params = new Hashtable<String, String>();
        for (String tmp : pars) {
            try {
                params.put(tmp.split("\"")[1], tmp.split("'")[1]);
            } catch (Throwable t) {
                // if we can't read an arg, just ignore it...
            }
        }

        return params;
    }

    /**
     * write the current html source to a file
     *
     * @param prefix the prefix of the filename
     * @param LOG the Log to log errors
     * @return the name of the created file
     * @see com.flexive.tests.browser.AbstractBackendBrowserTest#writeHTMLtoHD(String, org.apache.commons.logging.Log, String)
     */
    protected String writeHTMLtoHD(String prefix, Log LOG) {
        return writeHTMLtoHD(prefix, LOG, selenium.getHTMLSource());
    }

    /**
     * write the given String to a file
     * @param prefix the prefix of the file
     * @param LOG the Log to log errors
     * @param html the String to write
     * @return the name of the file created
     */
    protected String writeHTMLtoHD(String prefix, Log LOG, String html) {
//        http://localhost:8080/flexive/adm/structure/propertyEditor.jsf?action=createProperty&id=10&nodeType=Type

        File f = new File(prefix + "@" + System.currentTimeMillis() + ".html");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(html.getBytes());
            fos.flush();
            fos.close();
            LOG.info(f.getAbsolutePath()+ "\t" + new Throwable().getStackTrace()[2].toString().replaceAll("com.flexive.tests.browser.", ""));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f.getAbsolutePath();
    }


    /**
     * generates a group name (have to be "mandator: group"
     * @param mandator name of the mandator
     * @param group name of the group
     * @return "mandator: group"
     */
    protected static String generateGroupName(String mandator, String group) {
        return mandator + ": " + group;
    }

    /**
     * All IFrames available in the backend administration.
     */
    protected enum Frame {
        Content("contentFrame"),
        Top("relative=top"),
        NavContent("treeNavFrame_0"),
        NavStructures("treeNavFrame_1"),
        NavSearch("treeNavFrame_2"),
        NavAdministration("treeNavFrame_3");

        private String name;

        private Frame(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    /**
     * The available navigation tabs of the main navigation frame.
     */
    protected enum NavigationTab {
        // the order of the navigation tabs must match the actual layout
        // since the position determines the ID

        Content(Frame.NavContent),
        Structure(Frame.NavStructures),
        Search(Frame.NavSearch),
        Administration(Frame.NavAdministration);

        private Frame frame;

        NavigationTab(Frame frame) {
            this.frame = frame;
        }

    }

    public AbstractBackendBrowserTest() {
        super(null, "browser.properties");
    }

    @BeforeSuite
    public void beforeSuite(){
        numSkip= 0;
        super.beforeClass();
    }

    @AfterSuite
    public void afterSuite(){
        selenium.stop();
        LOG.info("skipped : " + numSkip + " test classes...");
        if (skipList.size() > 0) {
            LOG.warn("skipped following " + skipList.size() + " tests : ");
            for (String s : skipList) {
                LOG.warn(s);
            }
        }
    }

    private static int concurentClasses = 0;

    @Override
    @BeforeClass
    public void beforeClass() {
        curTestName = this.getClass().getCanonicalName() + " ::.. \t[" + (++concurentClasses) + "]";
        if (doSkip()) {
            LOG.warn("\t\t..:: Skiping test : " + curTestName);
            numSkip++;
        } else {
            LOG.info("\t\t..:: Running test : " + curTestName);

//            super.beforeClass();
        }
    }

    @Override
    @AfterClass
    public void afterClass() {
//        if (!doSkip()) {
//            super.afterClass();
//        }
        LOG.info("\t\t..:: after test : " + this.getClass().getCanonicalName() + " ::.. \t[" + (concurentClasses--) + "]");
    }

    @BeforeMethod
    public void beforeMethod(){
        lastCount = skipList.size();
        start = System.currentTimeMillis();
        selenium = AbstractSeleniumTest.getSeleniumInstance();
    }

    @AfterMethod
    public void afterMethod() {
        long ende = System.currentTimeMillis();
        if (lastCount == skipList.size() && (ende - start) < 1000) { // bad skip
            skipList.add("\t???\tMethod in [" + curTestName + "]")  ;
        }
    }

    /**
     * a method every test should call if it decides to skip it self
     */
    protected void skipMe() {
        Throwable t = new Throwable();
        String s = t.getStackTrace()[1].toString();
        skipList.add(s);
    }

    /**
     * Login and if flexive initializes, then it waits, and click on continue
     * @param userName username
     * @param password password
     * @return <code>true</code> if the login was succesfull
     */
    protected boolean login(String userName, String password) {
//        selenium.open(CONTEXTPATH + "/adm/main.jsf");
        try {
            selenium.open(CONTEXTPATH + "/pub/login.jsf");
        } catch (SeleniumException se) {
            // just ignore if it time outs...
        }
        selenium.waitForPageToLoad("300000");     // 5 min
        selenium.type("fxLoginForm:username", userName);
        selenium.type("fxLoginForm:password", password);
        selenium.click("fxLoginForm:takeOver");
        clickAndWait("link=Login");
        // wait for the loading screen to disappear
        try {
            sleep(100);
            if (selenium.isTextPresent("Login failed (invalid user or password)"))
                return false;
        } catch (SeleniumException se) {
            // no login error..
        }
        try {
                selenium.waitForCondition(WND + ".document.getElementById('loading') != null " +
                "&& " + WND + ".document.getElementById('loading').style.display == 'none'", "10000");
        } catch (SeleniumException se) {
            // ignore the timeout...
        }
        if (selenium.isTextPresent("Login failed (invalid user or password)")) return false;
        if (selenium.isTextPresent("RunOnce script execution status:")) {
            LOG.info("waiting for run script");
            int maxSec = 900;
            while (maxSec-->0) {
                try {
                    selenium.waitForCondition(WND + ".document.getElementById('continue') != null " +
                    "&& " + WND + ".document.getElementById('continue').style.display != 'none'", "50");
                    LOG.info("select info from table...");
                    selenium.click("link=Continue");
                    maxSec = 900;
                    while (maxSec-->0) {
                        try {
                            if (selenium.getTitle().toLowerCase().startsWith("user: " + userName.toLowerCase())) {
                                sleep(2000);
                                return true;
                            }
                        } catch (SeleniumException se) {
                            //
                        }
                        sleep(1000);
                    }
                } catch (SeleniumException se) {
                    // not ready
                    sleep(1000);
                }
            }
            return false;
        }
        try {
                selenium.waitForCondition(WND + ".document.getElementById('loading') != null " +
                "&& " + WND + ".document.getElementById('loading').style.display == 'none'", "120000");
        } catch (SeleniumException se) {
            // ignore the timeout...
        }
        if (selenium.isTextPresent("Login failed (invalid user or password)")) return false;
        return true;
    }

    protected void loginSupervisor() {
        login("supervisor", "supervisor");
    }

    protected void logout() {
        selectFrame(Frame.Top);
        selenium.click("searchForm:logoutImage");
        final int MAX_TRYS = 10;
        int trys = MAX_TRYS;
        while (trys-->0) {
            try {
                selectFrame(Frame.Content);
                selenium.click("frm:logoutButton");
                break;
            } catch (Throwable t) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // ignoring
                }
            }
        }
        if (trys == 0) {
            selectFrame(Frame.Content);
            selenium.waitForPageToLoad("30000");
            selenium.click("frm:logoutButton");
        }
//        if (trys + 1 < MAX_TRYS) {
//            LOG.info("trys : " + trys);
//        }
        selenium.waitForPageToLoad("30000");
    }

    protected void navigateTo(NavigationTab tab) {
        selectFrame(Frame.Top);
        final String result = selenium.getEval(WND + ".gotoNavMenu(" + tab.ordinal() + ")");
        selectFrame(tab.frame);
        if ("false".equals(result)) {
            // frame not loaded yet, wait for loading to complete
            selenium.waitForPageToLoad("30000");
        }
    }

    protected void selectFrame(Frame frame) {
        if (frame != Frame.Top) {
            selectFrame(Frame.Top); // select top frame first
        }
        selenium.selectFrame(frame.getName());
    }

    protected void loadContentPage(String url) {
        selectFrame(Frame.Content);
        selenium.open(CONTEXTPATH + (url.startsWith("/") ? url : "/" + url));
        selenium.waitForPageToLoad("30000");
    }

    /**
     *
     * @param src the html source of the table in which search
     * @param name the name to search
     * @return the tr-id of the row which contains the given name
     * @see com.flexive.tests.browser.AbstractBackendBrowserTest#getTRContainingName(String, String, int)
     */
    protected String getTRContainingName(String src, String name) {
        return getTRContainingName(src, name, 1);
    }

    /**
     * correct a html source
     * @param src the html source
     * @param reps the replacements to make
     * @return the corrected html source
     */
    public static String correctHTML(final String src, String ... reps) {
        String result = src;
        for (String rep : reps) {
            result = result.replaceAll(buildRegex(rep), rep);
        }

        return result;
    }

    /**
     * build a regex for a given string to find it case-in-sensitive
     *
     * it first looksup in the <code>REP_LUT</code> Hashtable if the string is already "known"
     *
     * @param rep the String to generate the regex from
     * @return the regex
     */
    private static String buildRegex(String rep) {
        String result = REP_LUT.get(rep);
        if (result != null) {
            return result;
        }

        StringBuilder regex = new StringBuilder(50);
        rep = rep.toLowerCase();
        for (char c : rep.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                regex.append("[").append((char)(c & 0xDF)).append(c).append("]");
            } else {
                regex.append(c);
            }

        }
        result = regex.toString();
        REP_LUT.put(rep, result);
        return result;
    }

    /**
     * get the whole tr containing a name
     * @param src the html source of a table to serach in
     * @param name the name to find
     * @param index the index in wich td it is (starts at 1)
     * @return the html source of the tr
     */
    protected String getTRContainingName(final String src, String name, int index) {
        String corSrc = correctHTML(src, "<tr ", "<td ", "id=", "</div></td>");
        String [] trS = corSrc.split("<tr ");
        String [] tdS;
        int beginTD, beginID;
        for (String s : trS) {
            beginTD = s.indexOf("<td ");
            beginID = s.indexOf("id=") + 4;
            if (beginTD > beginID && beginID > 4) {
                tdS = s.split("<td ");
                if (tdS[index].indexOf(">" + name + "</div></td>") > 0) {
//                    end = s.indexOf("\"", beginID);
                    return s;
                }
            }
        }

        writeHTMLtoHD("TR", LOG, src);
        return null;
    }

    /**
     * somehow the argument don't seems to matter
     * @return a contextMenu
     */
    protected FlexiveSelenium.ContextMenu getSearchResultContextMenu() {
//        return selenium.getContextMenu("frm:searchResults_resultMenu");
        return selenium.getContextMenu("frm:resultsMenu_menu");
    }

//    public String getButtonFromTRContainingName__(String src, String name, String buttonName) {
//        String [] trS = src.split("<tr ");
//        String [] tdS;
//        String [] aS;
//        int beginTD, beginID, end;
//        for (String s : trS) {
//            beginTD = s.indexOf("<td ");
//            beginID = s.indexOf("id=") + 4;
//            if (beginTD > beginID && beginID > 4) {
//                tdS = s.split("<td ");
//                if (tdS[1].indexOf(">" + name + "</div></td>") > 0) {
//                    aS = s.split("<a id=\"");
//                    for (String a : aS){
//                        end = a.indexOf(buttonName + "\" ");
//                        if (end > 0) {
//                            return a.substring(0,end + buttonName.length());
//                        }
//                    }
//                }
//            }
//        }
//
//        writeHTMLtoHD("TR_Button", LOG, src);
//        return null;
//    }


    public String getButtonFromTRContainingName_(String src, String name, String buttonName){
        return getButtonFromTRContainingName_(src, name, buttonName, 1);
    }

    /**
     * @param src the src of the table in which to search
     * @param name name of the entry to search (first column)
     * @param buttonName name of the button to find in the row OR null to get the whole row
     * @param tdPos the td to search in (starts by 1)
     * @return the name of the Button or the whole table row (no leading &lt;tr&gt;)
     */
    public String getButtonFromTRContainingName_(String src, String name, String buttonName, int tdPos){
        String corSrc = correctHTML(src, "<tr ", "<td ", "<td>", "</td", "</div></td>", "id=\""); 
        String[] trS = corSrc.split("<tr ");
        String[] tdS;
        String[] aS;
        String curTD;
        int end;
        int index;
        boolean startWith;
        final String prefix = "<!--name : \"" + name + "\"\nbuttonName : \"" + buttonName + "\"-->\n\n";
        for (String s : trS) {
            s = s.replaceAll("<!--(.*?)-->", "");
            tdS = s.split("<td[ >]");
            try {
                curTD = tdS[tdPos].replaceAll("[\\s\\n]*<","<");
                curTD = curTD.replaceAll("[\\n\\t]*", "");
                index = 0;
                index +=  curTD.indexOf(">" + name + "</td>") +1 ;
//                index +=  curTD.indexOf(">" + name + "\n") +1 ;
                index +=  curTD.indexOf(">" + name + "</div></td>") +1;
                startWith =curTD.startsWith(name + "</");
                if (index > 0 || startWith) {
                    if (buttonName == null) {
                        return s;
                    }
                    aS = s.split("<a ");
                    for (String a : aS) {
                        end = a.indexOf(buttonName);
                        if (end > 0) {
                            return getID(a);
                        }
                    }
                    writeHTMLtoHD("ButtonNotFound", LOG, prefix + corSrc);
                    throw new ButtonNotFoundException("Row with \"" + name + "\" in the " + tdPos + ". column found, but it has no Button containing \"" + buttonName + "\"!");
                }
            } catch (ArrayIndexOutOfBoundsException aout) {
//                LOG.info(aout.getMessage(), aout);
                // not found
            }
        }
        writeHTMLtoHD("ButtonNotFound", LOG, prefix + corSrc);
        throw new RowNotFoundException("No row with \"" + name + "\" in the " + tdPos + ". column found!");
    }

    protected String getID(String text) {
        return getID(text, "id=", 0);
    }

    protected String getID(String text, int begin) {
        return getID(text, "id=", begin);
    }

    protected String getID(String text, String search, int begin) {
        int index;

        index = text.indexOf(search, begin) + search.length();

        return text.substring(index, text.indexOf(" ", index)).replaceAll("\"", "");

    }

    private static class TestMethod {
        private final String name;
        private final long duration;

        private TestMethod(long duration, String name) {
            this.duration = duration;
            this.name = name;
        }
    }
}
