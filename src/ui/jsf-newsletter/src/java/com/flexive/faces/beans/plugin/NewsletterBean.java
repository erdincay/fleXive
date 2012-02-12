/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2012
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
package com.flexive.faces.beans.plugin;

import com.flexive.faces.components.content.FxWrappedContent;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Newsletter bean
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 1295 $
 */
@SuppressWarnings("UnusedDeclaration")
public class NewsletterBean implements Serializable {

    private FxPK newsletterPK;
    private String testParam;
    private FxWrappedContent nlContent;
//    private String typeName = null;
    private static Long [] typeID = {11L, 12L};
    private final static String [] SQL_STRINGS = {"SELECT @pk, #Newsletter/Name, #Newsletter/Description FILTER VERSION=MAX, type='Newsletter'",
                                                  "SELECT @pk, #NEWSLETTER_MESSAGE/TITLE, #NEWSLETTER_MESSAGE/CONTENT_PLAIN FILTER VERSION=MAX, type='NEWSLETTER_MESSAGE'"};
    private int curType = 0;

    private final static int TYPE_NL  = 0;
    private final static int TYPE_MSG = 1;
//    private final static byte TYPE_MSG = 2;

    private int nlListPageNumber = 1;
    private int nlListRowNumber = 10;

    private boolean showNew = false;
    private boolean showEdit = false;

    private boolean firstRun = true;

    public boolean isShowEdit() {
        mT();
        System.out.println("showEdit : " + showEdit);
        return showEdit;
    }

    public void setShowEdit(boolean showEdit) {
        mT();
        this.showEdit = showEdit;
        System.out.println("showEdit : " + showEdit);
    }

    public boolean isShowNew() {
        mT();
        System.out.println("showNew : " + showNew);
        return showNew;
    }

    public void setShowNew(boolean showNew) {
        mT();
        this.showNew = showNew;
        System.out.println("showNew : " + showNew);
    }

    public String getSqlString() {
        mT("ct:" + curType);
        return SQL_STRINGS[curType];
    }

    public NewsletterBean() {
        mT();
    }

    private void mT(String ... args) {
        String where = new Throwable().getStackTrace()[1].toString();
        where = where.substring(where.lastIndexOf(".", where.indexOf("("))+1, where.indexOf(")"));
        where = where.replaceAll("\\(NewsletterBean.java", "").replaceAll("set", "\t-->\tset");
        if (where.indexOf("NlContent") > 0)
            where = "-->>" + where + "<<--";
        else if (where.indexOf("get") > 0)
            where = where.substring(where.indexOf(":"));
        where = "\t\t_\t" + where;
        if (args != null && args.length > 0)
            where += "\t" +  Arrays.toString(args);
        System.out.println(where);
    }

    public Long getTypeID() {
        mT("ct:" + curType, "tID : " + typeID[curType]);
        return typeID[curType];
    }

    public FxWrappedContent getNlContent() {
        mT();
        System.out.println (nlContent == null);
        return nlContent;
    }

    public void setNlContent(FxWrappedContent nlContent) {
        mT();
        System.out.println("setting content: " + nlContent);
        this.nlContent = nlContent;
    }

    public String getDeletePk() {
        mT();
        return "";
    }

    public void setDeletePk(String deletePk) {
        mT();
        System.out.println("delete: " + deletePk);
        try {
            FxPK curPK = FxPK.fromString(deletePk);
            EJBLookup.getContentEngine().remove(curPK);
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    public String saveNL() {
        mT();
        System.out.println("saving content " + nlContent);
        typeID[curType] =   nlContent.getContent().getTypeId();
        try {
            nlContent.getContent().save();
            System.out.println("TypeID : " + typeID[TYPE_NL] + "\tTypeID_MSG : " + typeID[TYPE_MSG]);
            new FxFacesMsgInfo("Newsletter.msg.info.saved");
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return null;
    }

    public String getCurId() {
        mT();
        return "" + this.curType;
    }


    public void setCurId(String curType) {
        mT("curType:" + curType);
        this.curType = Integer.parseInt(curType.trim());
    }

    public Integer getTypeMsg() {
        mT();
        return TYPE_MSG;
    }

    public Integer getTypeNl() {
        mT();
        return TYPE_NL;
    }

    public void cancelEdit() {
        mT();
        newsletterPK = null;
    }

    public int getNlListPageNumber() {
        mT();
        return nlListPageNumber;
    }

    public void setNlListPageNumber(int nlListPageNumber) {
        mT();
        this.nlListPageNumber = nlListPageNumber;
    }

    public int getNlListRowNumber() {
        mT();
        return nlListRowNumber;
    }

    public void setNlListRowNumber(int nlListRowNumber) {
        mT();
        this.nlListRowNumber = nlListRowNumber;
    }

    public FxPK getNewsletterPK() {
        mT();
        return newsletterPK;
    }

    public String getNewsletterPK_S() {
        mT();
        String tmpPK;
        try {
            tmpPK = newsletterPK.getId() + "";
        } catch (NullPointerException npe) {
            tmpPK = "22";
        }
        return tmpPK;
    }

    public void setNewsletterPK(FxPK newsletterPK) {
        mT();
        this.newsletterPK = newsletterPK;
    }

    public String doEdit() {
        mT();
        return "";
    }

    public void editNewsletter(Object evt) {
        mT();
    }

    public String doEdit(FxPK pk) {
        mT();
        return "";
    }

    public String getTestParam() {
        mT();
        return testParam;
    }

    public void setTestParam(String testParam) {
        mT();
        this.testParam = testParam;
    }

    public String getNlEditPK() {
        mT();
        if (newsletterPK == null)
            return null;
        return newsletterPK.toString();
    }

    public void setNlEditPK(String nlEditPK) {
        mT();
        this.newsletterPK = FxPK.fromString(nlEditPK);
    }

    public boolean isFirstRun() {
        mT();
        boolean rc = firstRun;
        firstRun = false;
        return rc;
    }
}
