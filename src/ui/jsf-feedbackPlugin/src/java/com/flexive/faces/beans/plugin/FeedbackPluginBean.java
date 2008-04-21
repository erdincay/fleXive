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
package com.flexive.faces.beans.plugin;

import net.java.dev.weblets.FacesWebletUtils;

import javax.faces.context.FacesContext;

import com.flexive.faces.beans.MessageBean;
import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.FxSharedUtils;

/**
 * Backing bean for feedback buttons and feedback forms of feedback plugin
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class FeedbackPluginBean {

    private String refPage;
    private String comment="";
    private int feedback=-1;
    private static final int FEEDBACK_POSITIVE=0;
    private static final int FEEDBACK_MODERATE=1;
    private static final int FEEDBACK_NEGATIVE=2;


    public String sendGoodFeedback() {
        refPage=FacesContext.getCurrentInstance().getViewRoot().getViewId();
        feedback=FEEDBACK_POSITIVE;
        return showFeedback();
    }

    public String sendModerateFeedback() {
        refPage=FacesContext.getCurrentInstance().getViewRoot().getViewId();
        feedback=FEEDBACK_MODERATE;
        return showFeedback();
    }

    public String sendBadFeedback() {
        refPage=FacesContext.getCurrentInstance().getViewRoot().getViewId();
        feedback=FEEDBACK_NEGATIVE;
        return showFeedback();
    }

    private String showFeedback() {
        return "feedback";
    }

    public String getRefPage() {
        return refPage;
    }

    public void setRefPage(String refPage) {
        this.refPage = refPage;
    }

    public int getFeedback() {
        return feedback;
    }

    public void setFeedback(int feedback) {
        this.feedback = feedback;
    }

    public String getHeader() {
        if (feedback == FEEDBACK_POSITIVE)
            return FxJsfUtils.getLocalizedMessage("FeedbackPlugin.header.positiveFeedback");
        else if (feedback == FEEDBACK_MODERATE)
            return FxJsfUtils.getLocalizedMessage("FeedbackPlugin.header.moderateFeedback");
        else if (feedback == FEEDBACK_NEGATIVE)
            return FxJsfUtils.getLocalizedMessage("FeedbackPlugin.header.negativeFeedback");

        else
            return FxJsfUtils.getLocalizedMessage("FeedbackPlugin.err.internal");
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getBuildInfo() {
        return FxSharedUtils.getFlexiveVersion() + "/build #" + FxSharedUtils.getBuildNumber();
    }

    public void setBuildInfo(String buildInfo) {

    }
}
