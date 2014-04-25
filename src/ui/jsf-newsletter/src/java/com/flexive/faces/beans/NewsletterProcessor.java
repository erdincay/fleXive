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

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxMailUtils;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxData;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.value.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Newsletter business logic
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 1295 $
 */
@SuppressWarnings("UnusedDeclaration")
public class NewsletterProcessor implements Serializable {

    private static final Log LOG = LogFactory.getLog(NewsletterProcessor.class);

    public static final String TYPE_SUBSCRIBER = "NEWSLETTER_SUBSCRIBER";

    public static List<NewsletterInfo> getNewsletters() throws FxApplicationException {
        FxResultSet rs = EJBLookup.getSearchEngine().search("SELECT @pk, #Newsletter/Name, #Newsletter/Description FILTER VERSION=MAX, type='Newsletter'");
        List<NewsletterInfo> result = new ArrayList<NewsletterInfo>(rs.getRowCount());
        for (Object[] col : rs.getRows())
            result.add(new NewsletterInfo((FxPK) col[0], (FxString) col[1], (FxString) col[2]));
        return result;
    }

    /**
     * Return the PK for the newsletter with the given name.
     *
     * @param uniqueName the newsletter name
     * @return the PK for the newsletter with the given name
     * @throws com.flexive.shared.exceptions.FxNotFoundException
     *          if no newsletter with the given name was found
     */
    public static FxPK getNewsletterPK(String uniqueName) throws FxNotFoundException {
        final FxResultSet result;
        try {
            result = new SqlQueryBuilder()
                    .select("@pk")
                    .type("newsletter")
                    .condition("#newsletter/uniquename", PropertyValueComparator.EQ, uniqueName)
                    .getResult();
            if (result.getRowCount() == 0) {
                throw new FxNotFoundException("ex.newsletter.name", uniqueName);
            }
            return result.getResultRow(0).getPk(1);
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    public static synchronized FxPK subscribe(String salutation, String name, String surname, String email,
                                 boolean sendPlain, boolean sendHTML, FxPK newsletterPK) throws FxApplicationException {
        if (StringUtils.isEmpty(surname))
            throw new FxApplicationException("ex.newsletter.subscriber.missing.surname");
        if (StringUtils.isEmpty(email))
            throw new FxApplicationException("ex.newsletter.subscriber.missing.email");
        email = email.trim().toLowerCase();
        if (!FxMailUtils.isValidEmail(email))
            throw new FxApplicationException("ex.newsletter.email.invalid", email);
        FxContext.get().runAsSystem();
        FxContent newsletter = EJBLookup.getContentEngine().load(newsletterPK);
        try {
            FxResultSet rs = new SqlQueryBuilder()
                    .select("@pk")
                    .condition('#' + TYPE_SUBSCRIBER + "/EMAIL", PropertyValueComparator.EQ, email)
                    .getResult();
            if (rs.getRowCount() > 0) {
                FxPK foundPK = (FxPK) rs.getObject(1, 1);
                System.out.println("found subscriber with pk " + foundPK);
                FxContent co = EJBLookup.getContentEngine().load(foundPK);
                boolean found = false;
                for (FxValue nl : co.getValues("/NEWSLETTER")) {
                    if (!(nl instanceof FxReference))
                        continue; //should not happen
                    if (((FxReference) nl).getBestTranslation().getId() == newsletterPK.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    sendActivationMail(((FxString) newsletter.getValue("/SMTPSERVER")).getDefaultTranslation(), ((FxString) newsletter.getValue("/ACTURL")).getDefaultTranslation(), foundPK, newsletterPK, salutation, name, surname, email, sendPlain, sendHTML);
                }
                return foundPK;
            } else {
                FxContent co = EJBLookup.getContentEngine().initialize(TYPE_SUBSCRIBER);
                if (!StringUtils.isEmpty(salutation))
                    co.setValue("/SALUTATION", new FxString(false, salutation));
                if (!StringUtils.isEmpty(name))
                    co.setValue("/NAME", new FxString(false, name));
                co.setValue("/SURNAME", new FxString(true, surname));
                co.setValue("/EMAIL", new FxString(false, email));
                co.setValue("/SENDPLAIN", new FxBoolean(false, sendPlain));
                co.setValue("/SENDHTML", new FxBoolean(false, sendHTML));
                FxPK subsPK = EJBLookup.getContentEngine().save(co);
                sendActivationMail(((FxString) newsletter.getValue("/SMTPSERVER")).getDefaultTranslation(), ((FxString) newsletter.getValue("/ACTURL")).getDefaultTranslation(), subsPK, newsletterPK, salutation, name, surname, email, sendPlain, sendHTML);
                return subsPK;
            }
        } finally {
            FxContext.get().stopRunAsSystem();
        }
    }

    public static List<NewsletterSubscriber> getSubscribers(FxPK newsletterPK) throws FxApplicationException {
        FxResultSet rs = EJBLookup.getSearchEngine().search("SELECT @pk, #Newsletter_Subscriber/Salutation, #Newsletter_Subscriber/Name, #Newsletter_Subscriber/Surname, #Newsletter_Subscriber/EMail, #Newsletter_Subscriber/SENDPLAIN, #Newsletter_Subscriber/SENDHTML, #Newsletter_Subscriber/CODE\n" +
                "FILTER VERSION=MAX, type='Newsletter_Subscriber', MAX_RESULTROWS=" + Integer.MAX_VALUE + "\n" +
                "where #Newsletter_Subscriber/NEWSLETTER=" + newsletterPK.getId(),
                0, Integer.MAX_VALUE, null);
        List<NewsletterSubscriber> result = new ArrayList<NewsletterSubscriber>(rs.getRowCount());
        for (Object[] col : rs.getRows())
            result.add(new NewsletterSubscriber((FxPK) col[0], (FxString) col[1], (FxString) col[2], (FxString) col[3],
                    (FxString) col[4], (FxBoolean) col[5], (FxBoolean) col[6], (FxString) col[7]));
        return result;
    }

    public static boolean sendMessage(FxPK messagePK) throws FxApplicationException {
        final ContentEngine ce = EJBLookup.getContentEngine();
        final FxContent message = ce.load(messagePK);
        final FxContent newsletter = ce.load(((FxReference) message.getValue("/NEWSLETTER")).getBestTranslation());
        final List<NewsletterSubscriber> subscribers = getSubscribers(newsletter.getPk());
        if (subscribers.size() == 0) {
            LOG.info("No subscribers for Message " + message.getCaption() + " (pk:" + messagePK + "), Newsletter: " +
                    newsletter.getCaption() + " (pk:" + newsletter.getPk() + ")");
            return false;
        }
        //build the message to send
        String[] attachments = new String[message.getValues("/ATTACHMENT").size()];
        int ai = 0;
        for (FxValue att : message.getValues("/ATTACHMENT")) {
            if (!(att instanceof FxBinary)) {
                LOG.warn("Attachment " + att + " is no FxBinary as expected!");
                continue;
            }
            FxBinary ba = (FxBinary) att;
            File ftmp = null;
            FileOutputStream fout;
            final BinaryDescriptor bdesc = ba.getBestTranslation();
            try {
                ftmp = File.createTempFile("NLAttachment", "bin");
                fout = new FileOutputStream(ftmp);
                bdesc.download(fout);
                fout.close();
                attachments[ai++] = FxMailUtils.encodeAttachment(bdesc.getMimeType(), bdesc.getName(), ftmp);
            } catch (IOException e) {
                LOG.error("Failed to download and encode attachment " + bdesc.getName());
                return false;
            } finally {
                if (ftmp != null)
                    if (!ftmp.delete())
                        ftmp.deleteOnExit();
            }
        }

        final String plainContent = String.valueOf(message.getValue("/CONTENT_PLAIN").getBestTranslation());
        final String htmlContent = String.valueOf(message.getValue("/CONTENT_HTML").getBestTranslation());
        final String subject = String.valueOf(message.getValue("/TITLE").getBestTranslation());
        final String from = String.valueOf(message.getValue("/FROM").getBestTranslation());
        final String replyTo;
        if (message.containsValue("/REPLY_TO") && !message.getValue("/REPLY_TO").isEmpty())
            replyTo = String.valueOf(message.getValue("/REPLY_TO").getBestTranslation());
        else
            replyTo = "";
        FxContent protocol = ce.initialize("NEWSLETTER_PROTOCOL");
        protocol.setValue("/SENT", new FxDateTime(false, new Date(System.currentTimeMillis())));
        protocol.setValue("/NEWSLETTER", new FxReference(false, newsletter.getPk()));
        protocol.setValue("/MESSAGE", new FxReference(false, messagePK));
        final String cancellationURL = ((FxString) newsletter.getValue("/CANCELURL")).getDefaultTranslation();
        final String smtpServer = ((FxString) newsletter.getValue("/SMTPSERVER")).getDefaultTranslation();
        for (NewsletterSubscriber subs : subscribers) {
            boolean sendPlain = !subs.getPlainText().isEmpty() && subs.getPlainText().getBestTranslation();
            boolean sendHTML = !subs.getHtmlText().isEmpty() && subs.getHtmlText().getBestTranslation();
            final String email = subs.getEmail().getBestTranslation();
            if (!sendPlain && !sendHTML) {
                sendPlain = true;
                LOG.warn("Subscriber " + email + " wants neither plain nor html messages! Forcing plain!");
            }
            String plain = sendPlain ? parseContent(newsletter.getPk(), subs, plainContent, cancellationURL) : "";
            String html = sendHTML ? parseContent(newsletter.getPk(), subs, htmlContent, cancellationURL) : "";

            try {
                FxMailUtils.sendMail(smtpServer, subject, plain, html, email, "", "", from, replyTo, attachments);
                protocol.setValue("/SUCCESS", new FxReference(false, subs.getPk()));
                LOG.info("Sent message to " + email);
            } catch (FxApplicationException e) {
                LOG.warn("Failed to send message to " + email);
                protocol.setValue("/FAILURE", new FxReference(false, subs.getPk()));
            }
        }
        ce.save(protocol);
        return true;
    }

    private static String parseContent(FxPK newsletterPK, NewsletterSubscriber subscriber, String content, String cancellationURL) {
        final String name = subscriber.getName().isEmpty() ? "" : subscriber.getName().getBestTranslation();
        final boolean hasName = !StringUtils.isEmpty(name);
        final String salutation = subscriber.getSalutation().isEmpty() ? "" : subscriber.getSalutation().getBestTranslation();
        final boolean hasSalutation = !StringUtils.isEmpty(salutation);
        if (hasName) {
            content = replace(content, "{{NAME}}", name);
            content = replace(content, "{{HAS_NAME_START}}", "");
            content = replace(content, "{{HAS_NAME_END}}", "");
        } else {
            content = content.replaceAll("\\{\\{HAS_NAME_START\\}\\}.*\\{\\{HAS_NAME_END\\}\\}", "");
            content = replace(content, "{{NAME}}", "");
        }
        content = replace(content, "{{SURNAME}}", subscriber.getSurname().getBestTranslation());
        if (hasSalutation) {
            content = replace(content, "{{SALUTATION}}", salutation);
            content = replace(content, "{{HAS_SALUTATION_START}}", "");
            content = replace(content, "{{HAS_SALUTATION_END}}", "");
        } else {
            content = content.replaceAll("\\{\\{HAS_SALUTATION_START\\}\\}.*\\{\\{HAS_SALUTATION_END\\}\\}", "");
            content = replace(content, "{{SALUTATION}}", "");
        }
        content = replace(content, "{{CANCELLATION_URL}}", cancellationURL + subscriber.getCancelCode(newsletterPK));
        return content;
    }

    private static void sendActivationMail(String smtpServer, String activationURL, FxPK subscriberPK, FxPK newsletterPK, String salutation, String name, String surname, String email, boolean sendPlain, boolean sendHTML) throws FxApplicationException {
        final ContentEngine ce = EJBLookup.getContentEngine();
        final FxContent newsletter = ce.load(newsletterPK);
        final FxContent subscriber = ce.load(subscriberPK);
        final String subject = String.valueOf(newsletter.getValue("/ACTIVATIONSUBJECT").getBestTranslation());
        final String plainContent = String.valueOf(newsletter.getValue("/ACTIVATIONCONTENTPLAIN").getBestTranslation());
        final String htmlContent = String.valueOf(newsletter.getValue("/ACTIVATIONCONTENTHTML").getBestTranslation());
        final String from = String.valueOf(newsletter.getValue("/ACT_FROM").getBestTranslation());
        final String replyTo;
        if (newsletter.containsValue("/ACT_REPLY_TO") && !newsletter.getValue("/ACT_REPLY_TO").isEmpty())
            replyTo = String.valueOf(newsletter.getValue("/ACT_REPLY_TO").getBestTranslation());
        else
            replyTo = "";
        if (!sendPlain && !sendHTML)
            sendPlain = true;
        String code = String.valueOf(subscriber.getValue("/CODE").getBestTranslation());
        String _activationURL = activationURL + "A" + code + String.valueOf(subscriber.getPk().getId()) + "_" + String.valueOf(newsletter.getPk().getId());
        String plain = sendPlain ? parseActivationContent(newsletter.getPk(), salutation, name, surname, plainContent, _activationURL) : "";
        String html = sendHTML ? parseActivationContent(newsletter.getPk(), salutation, name, surname, htmlContent, _activationURL) : "";
        FxMailUtils.sendMail(smtpServer, subject, plain, html, email, "", "", from, replyTo);
    }

    private static String parseActivationContent(FxPK newsletterPK, String salutation, String name, String surname, String content, String activationURL) {
        final boolean hasName = !StringUtils.isEmpty(name);
        final boolean hasSalutation = !StringUtils.isEmpty(salutation);
        if (hasName) {
            content = replace(content, "{{NAME}}", name);
            content = replace(content, "{{HAS_NAME_START}}", "");
            content = replace(content, "{{HAS_NAME_END}}", "");
        } else {
            content = content.replaceAll("\\{\\{HAS_NAME_START\\}\\}.*\\{\\{HAS_NAME_END\\}\\}", "");
            content = replace(content, "{{NAME}}", "");
        }
        content = replace(content, "{{SURNAME}}", surname);
        if (hasSalutation) {
            content = replace(content, "{{SALUTATION}}", salutation);
            content = replace(content, "{{HAS_SALUTATION_START}}", "");
            content = replace(content, "{{HAS_SALUTATION_END}}", "");
        } else {
            content = content.replaceAll("\\{\\{HAS_SALUTATION_START\\}\\}.*\\{\\{HAS_SALUTATION_END\\}\\}", "");
            content = replace(content, "{{SALUTATION}}", "");
        }
        content = replace(content, "{{ACTIVATION_URL}}", activationURL);
        return content;
    }

    private static void sendWelcomeMail(FxPK subscriberPK, FxPK newsletterPK, NewsletterSubscriber subscriber) throws FxApplicationException {
        final ContentEngine ce = EJBLookup.getContentEngine();
        final FxContent newsletter = ce.load(newsletterPK);
        if (newsletter.containsValue("/SENDWELCOME") && ((FxBoolean) newsletter.getValue("/SENDWELCOME")).getDefaultTranslation()) {
            LOG.info("Sending welcome email to " + subscriber.getEmail().getBestTranslation());
        } else {
            LOG.info("Not sending welcome email to " + subscriber.getEmail().getBestTranslation());
            return;
        }
//        final FxContent subscriber = ce.load(subscriberPK);
        final String subject = String.valueOf(newsletter.getValue("/WELCOMESUBJECT").getBestTranslation());
        final String plainContent = String.valueOf(newsletter.getValue("/WELCOMECONTENTPLAIN").getBestTranslation());
        final String htmlContent = String.valueOf(newsletter.getValue("/WELCOMECONTENTHTML").getBestTranslation());
        final String from = String.valueOf(newsletter.getValue("/WELCOME_FROM").getBestTranslation());
        final String replyTo;
        if (newsletter.containsValue("/WELCOME_REPLY_TO") && !newsletter.getValue("/WELCOME_REPLY_TO").isEmpty())
            replyTo = String.valueOf(newsletter.getValue("/WELCOME_REPLY_TO").getBestTranslation());
        else
            replyTo = "";
        boolean sendPlain = subscriber.getPlainText().getDefaultTranslation();
        final boolean sendHTML = subscriber.getHtmlText().getDefaultTranslation();
        if (!sendPlain && !sendHTML)
            sendPlain = true;
        final String smtpServer = ((FxString) newsletter.getValue("/SMTPSERVER")).getDefaultTranslation();
        final String cancellationURL = ((FxString) newsletter.getValue("/CANCELURL")).getDefaultTranslation();
        String plain = sendPlain ? parseContent(newsletter.getPk(), subscriber, plainContent, cancellationURL) : "";
        String html = sendHTML ? parseContent(newsletter.getPk(), subscriber, htmlContent, cancellationURL) : "";
        FxMailUtils.sendMail(smtpServer, subject, plain, html, subscriber.getEmail().getDefaultTranslation(), "", "", from, replyTo);
    }

    public static boolean processNewsletterRequest(String code) throws FxApplicationException {
        if (StringUtils.isEmpty(code) || code.length() < 24) {
            LOG.warn("Invalid code: " + code);
            return false;
        }
        char action = code.charAt(0);
        String subsCode = code.substring(1, 21);
        FxPK pkSubs = new FxPK(Long.parseLong(code.substring(21, code.indexOf("_"))));
        FxPK pkNL = new FxPK(Long.parseLong(code.substring(code.indexOf("_") + 1)));
        //load subscriber, newsletter and validate
        final ContentEngine ce = EJBLookup.getContentEngine();
        FxContext.get().runAsSystem();
        try {
            final FxContent subscriber = ce.load(pkSubs);
            if (!subsCode.equals(subscriber.getValue("/CODE").getBestTranslation())) {
                LOG.error("Attempted to supply an invalid subscriber code!");
                return false;
            }
            boolean isSubscribed = false;
            for (FxValue nl : subscriber.getValues("/NEWSLETTER")) {
                if (!(nl instanceof FxReference))
                    continue; //should not happen
                if (((FxReference) nl).getBestTranslation().getId() == pkNL.getId()) {
                    isSubscribed = true;
                    break;
                }
            }
            final FxReference nlRef = new FxReference(false, pkNL);
            switch (action) {
                case 'A': //activate
                    if (isSubscribed) {
                        LOG.info("Already subscribed " + pkSubs + " to " + pkNL);
                    } else {
                        subscriber.setValue("/NEWSLETTER", nlRef);
                        ce.save(subscriber);
                        LOG.info("Subscribed " + pkSubs + " to " + pkNL);
                        sendWelcomeMail(pkSubs, pkNL, new NewsletterSubscriber(subscriber));
                    }
                    break;
                case 'C': //cancel
                    if (!isSubscribed) {
                        LOG.info("Already cancelled " + pkSubs + " from " + pkNL);
                    } else {
                        FxPropertyData nlData = null;
                        for (FxData d : subscriber.getData("/NEWSLETTER")) {
                            if (!d.isProperty())
                                continue; //can not happen actually
                            FxPropertyData p = (FxPropertyData) d;
                            if (((FxReference) p.getValue()).getBestTranslation().getId() == pkNL.getId()) {
                                nlData = p;
                                break;
                            }
                        }
                        if (nlData == null) {
                            LOG.error("Could not find subscription to newsletter " + pkNL + " for subscriber " + pkSubs);
                        } else {
                            subscriber.getRootGroup().removeChild(nlData);
                            ce.save(subscriber);
                            LOG.info("Cancelled " + pkSubs + " from " + pkNL);
                        }
                    }
                    break;

            }
        } finally {
            FxContext.get().stopRunAsSystem();
        }
        return true;
    }

    /**
     * Replaces all instances of oldString with newString in line ignoring the case.
     *
     * @param line      the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     * @return a String will all instances of oldString replaced by newString
     */
    public static String replace(String line, String oldString, String newString) {
        if (line == null)
            return "";
        String lcLine = line.toLowerCase();
        String lcOldString = oldString.toLowerCase();
        int i = 0;
        if ((i = lcLine.indexOf(lcOldString, i)) >= 0) {
            char[] line2 = line.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuilder buf = new StringBuilder(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = lcLine.indexOf(lcOldString, i)) > 0) {
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            return buf.toString();
        }
        return line;
    }
}
