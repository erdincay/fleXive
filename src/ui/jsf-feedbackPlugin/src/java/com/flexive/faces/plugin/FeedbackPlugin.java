/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.faces.plugin;
import net.java.dev.weblets.FacesWebletUtils;
import javax.faces.context.FacesContext;

/**
 * Plugin for sumbmitting feedback.
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class FeedbackPlugin implements Plugin<ToolbarPluginExecutor>{


    public void apply(ToolbarPluginExecutor executor) {
        executor.addToolbarSeparatorButton();
        executor.addToolbarButton("*", getGoodFeedbackButton());
        executor.addToolbarButton("*", getModerateFeedbackButton());
        executor.addToolbarButton("*", getBadFeedbackButton());
    }

    private ToolbarPluginExecutor.Button getGoodFeedbackButton() {
        ToolbarPluginExecutor.Button b = new ToolbarPluginExecutor.Button("goodFeedbackButton");
        b.setBean("feedbackPluginBean");
        b.setAction("sendGoodFeedback");
        b.setIconUrl(FacesWebletUtils.getURL(FacesContext.getCurrentInstance(), "feedback_plugin.weblet", "/images/feedback_good.png"));
        b.setLabelKey("FeedbackPlugin.button.tooltip.goodFeedback");
        return b;
    }

    private ToolbarPluginExecutor.Button getModerateFeedbackButton() {
        ToolbarPluginExecutor.Button b = new ToolbarPluginExecutor.Button("moderateFeedbackButton");
        b.setBean("feedbackPluginBean");
        b.setAction("sendModerateFeedback");
        b.setIconUrl(FacesWebletUtils.getURL(FacesContext.getCurrentInstance(), "feedback_plugin.weblet", "/images/feedback_moderate.png"));
        b.setLabelKey("FeedbackPlugin.button.tooltip.moderateFeedback");
        return b;
    }

    private ToolbarPluginExecutor.Button getBadFeedbackButton() {
        ToolbarPluginExecutor.Button b = new ToolbarPluginExecutor.Button("badFeedbackButton");
        b.setBean("feedbackPluginBean");
        b.setAction("sendBadFeedback");
        b.setIconUrl(FacesWebletUtils.getURL(FacesContext.getCurrentInstance(), "feedback_plugin.weblet", "/images/feedback_bad.png"));
        b.setLabelKey("FeedbackPlugin.button.tooltip.badFeedback");
        return b;
    }
}
