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
package com.flexive.faces.beans;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * JSF Bean for subscribing to a newsletter.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@SuppressWarnings("UnusedDeclaration")
public class NewsletterBean implements Serializable {
    private static final long serialVersionUID = 5935801772176506911L;

    private String salutation;
    private String name;
    private String surname;
    private String email;
    private boolean sendPlain = true;
    private boolean sendHTML = true;
    private String smtpServer;
    private String activationURL;
    private FxPK newsletterPK;
    private String newsletterName;

    /**
     * Subscribe the user using the given parameters. At least the following parameters have to be set:
     * <ul>
     * <li>surname</li>
     * <li>email</li>
     * <li>smtpServer</li>
     * <li>activationURL</li>
     * <li>newsletterPK or newsletterName</li>
     * </ul>
     *
     * @return  "success" or "error"
     */
    public String subscribe() {
        try {
            if (StringUtils.isNotBlank(newsletterName)) {
                newsletterPK = NewsletterProcessor.getNewsletterPK(newsletterName);
            }

            NewsletterProcessor.subscribe(salutation, name, surname, email, sendPlain, sendHTML, newsletterPK);

            new FxFacesMsgInfo("Newsletter.msg.inf.subscribed", email).addToContext();
            return "success";
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
            return "error";
        }
    }

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isSendPlain() {
        return sendPlain;
    }

    public void setSendPlain(boolean sendPlain) {
        this.sendPlain = sendPlain;
    }

    public boolean isSendHTML() {
        return sendHTML;
    }

    public void setSendHTML(boolean sendHTML) {
        this.sendHTML = sendHTML;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public String getActivationURL() {
        return activationURL;
    }

    public void setActivationURL(String activationURL) {
        this.activationURL = activationURL;
    }

    public FxPK getNewsletterPK() {
        return newsletterPK;
    }

    public void setNewsletterPK(FxPK newsletterPK) {
        this.newsletterPK = newsletterPK;
    }

    public String getNewsletterName() {
        return newsletterName;
    }

    public void setNewsletterName(String newsletterName) {
        this.newsletterName = newsletterName;
    }
}
