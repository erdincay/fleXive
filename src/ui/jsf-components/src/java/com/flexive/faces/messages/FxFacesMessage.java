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
package com.flexive.faces.messages;

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxLocalizedException;
import com.flexive.shared.security.UserTicket;
import org.apache.commons.lang.StringUtils;

import javax.faces.application.FacesMessage;
import static javax.faces.context.FacesContext.getCurrentInstance;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

/**
 * A Flexive Faces Message.
 * <p/>
 * This class is extends the FacesMessages by adding access to the form and client id that the message belongs to.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxFacesMessage extends FacesMessage implements Serializable {
    private static final long serialVersionUID = 8756831033361611047L;

    private String form;
    private String id;

    /**
     * Constructor.
     *
     * @param msg       the original faces message
     * @param component the client id, or null for a global message. The client id may conain the form (JSF style), eg
     *                  'myForm:myComponentId'
     */
    public FxFacesMessage(FacesMessage msg, String component) {
        super(msg.getSeverity(), msg.getSummary(), msg.getDetail());
        if (component != null) {
            String split[] = component.split(":");
            this.form = split[0];
            this.id = split[1];
        } else {
            this.form = "";
            this.id = "";
        }
    }

    /**
     * Constructor.
     *
     * @param severity      the severity
     * @param summaryKey    the summary key
     * @param summaryParams the summary parameters
     */
    public FxFacesMessage(Severity severity, String summaryKey, Object... summaryParams) {
        super(severity, summaryKey, "");
        setLocalizedSummary(summaryKey, summaryParams);
    }


    /**
     * Returns the detail message for use in javascripts.
     *
     * @return the detail message for use in javascripts
     */
    public String getDetailForJavascript() {
        String detail = super.getDetail();
        detail = detail.replaceAll("'", "&prime;");
        detail = detail.replaceAll("\"", "&Prime;");
        detail = detail.replaceAll("\n", "<br>");
        detail = detail.replaceAll("\r", "");
        return detail;
    }

    /**
     * Constructor.
     *
     * @param exc           the Exception
     * @param severity      the severity
     * @param summaryKey    the summary key
     * @param summaryParams the summary parameters
     */
    public FxFacesMessage(Throwable exc, Severity severity, String summaryKey, Object... summaryParams) {
        super(severity, summaryKey, summaryKey);
        setLocalizedSummary(summaryKey, summaryParams);
        processException(exc, true, false);
    }

    /**
     * Constructor.
     *
     * @param exc      the Exception
     * @param severity the severity
     */
    public FxFacesMessage(Throwable exc, Severity severity) {
        super(severity, "", "");
        processException(exc, false, true);
    }

    /**
     * Extracts the message of the exception, and set the this.id property in
     * case of a found FxInvalidParameterException.
     *
     * @param exc        the Exception the exception to process
     * @param setDetail  set the detail to the message of the exception
     * @param setSummary set the summaray to the message of the exception
     */
    private void processException(Throwable exc, boolean setDetail, boolean setSummary) {

        // Try to find a FX__ type within the exception stack
        Throwable tmp = exc;
        while (!isFxThrowable(tmp) && tmp.getCause() != null) {
            tmp = tmp.getCause();
        }

        // If we found a FX__ type we use it
        if (isFxThrowable(tmp)) {
            exc = tmp;
        }

        // Obtain id of the invalid parameter
        if (exc instanceof FxInvalidParameterException) {
            this.id = ((FxInvalidParameterException) exc).getParameter();
        }

        String msg;
        if (exc instanceof FxLocalizedException) {
            UserTicket ticket = FxContext.get().getTicket();
            FxLocalizedException le = ((FxLocalizedException) exc);
            msg = le.getMessage(ticket);
        } else {
            msg = (exc.getMessage() == null) ? String.valueOf(exc.getClass()) : exc.getMessage();
        }

        if (setSummary) {
            setSummary(msg);
        }

        if (setDetail) {
            String sDetail = "";
            if (exc.getCause() != null) {
                String sCause;
                sCause = exc.getCause().getMessage();
                if (sCause == null || sCause.trim().length() == 0 || sCause.equalsIgnoreCase("null")) {
                    sCause = exc.getCause().getClass().getName();
                }
                sDetail += "Cause message: " + sCause + "\n\t";
            }
            sDetail += getStackTrace(exc);
            setDetail(sDetail);
        }
    }

    /**
     * Gets the stacktrace as a string from a throwable.
     *
     * @param th the throwable
     * @return the stacktrace as string
     */
    private String getStackTrace(Throwable th) {
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            return sw.toString();
        } catch (Throwable t) {
            return "n/a";
        } finally {
            try {
                if (sw != null) sw.close();
            } catch (Throwable t) {/*ignore*/}
            try {
                if (pw != null) pw.close();
            } catch (Throwable t) {/*ignore*/}
        }
    }

    private boolean isFxThrowable(Throwable exc) {
        return exc != null && (exc instanceof FxLocalizedException);
    }

    /**
     * The form the message belongs to, or a empty string for a global message.
     *
     * @return The form the message belong to, or a empty string for a global message
     */
    public String getForm() {
        return form;
    }

    /**
     * The client id that the message belongs (without the jsf form prefix) to, or a empty string for a global message.
     *
     * @return The client id that the message belongs to, or a empty string for a global message
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the client id (may be a empty string).
     *
     * @return the client id
     */
    public String getClientId() {
        return (id == null || id.length() == 0) ? "" : (form + ":" + id);
    }

    /**
     * Set the form name
     *
     * @param form name of the form
     */
    public void setForm(String form) {
        this.form = form;
    }

    /**
     * Sets the client id that the message belongs to.
     *
     * @param id the client id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the summary
     *
     * @param key    the message key
     * @param params the message parameters
     */
    public final void setLocalizedSummary(String key, Object... params) {
        super.setSummary(FxJsfUtils.getLocalizedMessage(key, params));
    }

    /**
     * Sets the message detail.
     *
     * @param key    The detail text key
     * @param params the message parameters
     */
    public void setLocalizedDetail(String key, Object... params) {
        super.setDetail(FxJsfUtils.getLocalizedMessage(key, params));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof FxFacesMessage)) return false;
        //noinspection ConstantConditions
        FxFacesMessage comp = (FxFacesMessage) obj;
        // Compare properties
        return (StringUtils.equals(this.getSummary(), comp.getSummary()) &&
                StringUtils.equals(this.getDetail(), comp.getDetail()) &&
                this.getSeverity() == comp.getSeverity() &&
                StringUtils.equals(this.getForm(), comp.getForm()) &&
                StringUtils.equals(this.getClientId(), comp.getClientId())
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        if (getDetail() != null) {
            hashCode = getDetail().hashCode();
        }
        if (getSummary() != null) {
            hashCode = hashCode * 31 + getSummary().hashCode();
        }
        if (getForm() != null) {
            hashCode = hashCode * 31 + getForm().hashCode();
        }
        if (getClientId() != null) {
            hashCode = hashCode * 31 + getClientId().hashCode();
        }
        return hashCode;
    }

    /**
     * Adds the message to the context.
     * <p/>
     * A message will only be available to the client (UI) when it was added to the context
     */
    public void addToContext() {
        getCurrentInstance().addMessage(null, this);
    }

    /**
     * Adds the message to the context.
     * <p/>
     * A message will only be available to the client (UI) when it was added to the context
     *
     * @param clientId the client id of the message
     */
    public void addToContext(String clientId) {
        getCurrentInstance().addMessage(clientId, this);
    }

    /**
     * Returns true if the message has a detail information.
     *
     * @return true if the message has a detail information
     */
    public boolean getHasDetail() {
        return this.getDetail() != null && this.getDetail().trim().length() > 0;
    }
}
