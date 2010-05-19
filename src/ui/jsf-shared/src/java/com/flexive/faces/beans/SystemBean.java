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
package com.flexive.faces.beans;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.faces.messages.FxFacesMessage;
import com.flexive.faces.messages.FxFacesMessages;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.*;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.AdminResultLocations;
import com.flexive.shared.search.ResultLocation;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.war.FxRequest;
import com.flexive.war.filter.FxResponseWrapper;
import com.flexive.war.filter.FxRequestUtils;
import com.google.common.collect.Lists;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import static javax.faces.context.FacesContext.getCurrentInstance;
import javax.faces.model.DataModel;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.*;


/**
 * Generic [fleXive] System informations and access to various functions
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SystemBean implements Serializable {
    private static final long serialVersionUID = 6592229045042549537L;
    private static final FacesMessageMap messageMap = new FacesMessageMap();

    private long updateLanguageId = -1;
    private Map<Object, Integer> counters;
    private Map<Object, Integer> counterMap;
    private Map<Object, Integer> counterValueMap;
    private Map<String, Boolean> messageExistsMap;
    private Map<Object, FxContent> contentMap;
    private Map<Object, FxContent> explodedContentMap;
    private Map<String, DataModel> queries;
    private Map<Long, Long> instanceCountMap;
    private String nodeName;

    /**
     * Resets all counters between phases.
     */
    public void reset() {
        counters = null;
        counterMap = null;
        counterValueMap = null;
    }

    public Map<String, List<FacesMessage>> getComponentMessages() {
        return messageMap;
    }

    /**
     * Returns all faces messages.
     *
     * @return all messages
     */
    public List<FacesMessage> getMessages() {
        return FxJsfUtils.getMessages(null);
    }

    /**
     * Returns all faces messages grouped by their summary.
     *
     * @return all messages
     */
    public List<FxFacesMessages> getGroupedFxMessages() {
        return FxJsfUtils.getGroupedFxMessages();
    }

    /**
     * Returns true if grouped FxMessages are available.
     *
     * @return true if grouped FxMessages are available
     */
    public boolean getHasGroupedFxMessages() {
        List l = getGroupedFxMessages();
        return (l != null && l.size() > 0);
    }

    /**
     * Returns all faces messages.
     *
     * @return all messages
     */
    public List<FxFacesMessage> getFxMessages() {
        return FxJsfUtils.getFxMessages();
    }

    /**
     * Returns true if the current request's error messages contain the
     * given ID.
     *
     * @return a map that returns true if the requested key is contained in the current request's error message IDs
     */
    public Map<String, Boolean> getContainsErrorId() {
        return new HashMap<String, Boolean>() {
            public Boolean get(Object key) {
                if (key == null) {
                    return false;
                }
                final String keyString = key.toString();
                for (FxFacesMessage message : getFxMessages()) {
                    if (keyString.equals(message.getId())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Returns the next page to display based on the request parameter page.
     *
     * @return the next page to display based on the request parameter page
     */
    public String gotoPage() {
        return FxJsfUtils.getParameter("page");
    }


    /**
     * Updates the current session language supplied in the "updateLanguageId" property
     * and re-renders the current page.
     */
    public void updateLanguage() {
        try {
            getUserTicket().setLanguage(EJBLookup.getLanguageEngine().load(updateLanguageId));
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    public long getUpdateLanguageId() {
        if (updateLanguageId == -1) {
            updateLanguageId = getUserTicket().getLanguage().getId();
        }
        return updateLanguageId;
    }

    public void setUpdateLanguageId(long updateLanguageId) {
        this.updateLanguageId = updateLanguageId;
    }

    /**
     * Returns the ticket of the calling user.
     *
     * @return the ticket of the calling user
     */
    public UserTicket getUserTicket() {
        return FxJsfUtils.getRequest().getUserTicket();
    }

    /**
     * Return the {@link FxContext} instance of the current request.
     *
     * @return the {@link FxContext} instance of the current request.
     */
    public FxContext getContext() {
        return FxContext.get();
    }


    /**
     * Get a handle to the response
     *
     * @return response
     */
    public FxResponseWrapper getResponse() {
        return FxJsfUtils.getResponse();
    }

    /**
     * Get a handle to the request
     *
     * @return request
     */
    public FxRequest getRequest() {
        return FxJsfUtils.getRequest();
    }

    /**
     * Get the current context path
     *
     * @return current context path
     */
    public String getContextPath() {
        return FxJsfUtils.getRequest().getContextPath();
    }

    /**
     * Get the context path for the (optional!) backend application
     *
     * @return context path for the (optional!) backend application
     */
    public String getFlexiveContextPath() {
        return "/flexive";
    }

    /**
     * Get the protocol, servername and (optional if non-standard) port
     *
     * @return server base
     */
    public String getServerBase() {
        HttpServletRequest request = FxJsfUtils.getRequest().getRequest();
        return "http" + (request.isSecure() ? "s://" : "://") + FxRequestUtils.getExternalServerName(request);
    }

    /**
     * Get the document base (server base and context path)
     *
     * @return document base (server base and context path)
     */
    public String getDocumentBase() {
        return getServerBase() + getContextPath() + "/";
    }

    private static class FacesMessageMap extends HashMap<String, List<FacesMessage>> {
        private static final long serialVersionUID = -9157142909355611529L;

        /**
         * {@inheritDoc}
         */
        @Override
        public List<FacesMessage> get(Object key) {
            return FxJsfUtils.getMessages("" + key);
        }
    }

    /**
     * Returns the browser that the client is using
     *
     * @return the browser the client is using
     */
    public FxRequest.Browser getBrowser() {
        return FxJsfUtils.getRequest().getBrowser();
    }

    /**
     * Check if the backend supports the current browser
     *
     * @return backend support the current browser
     */
    public boolean getIsSupportedBackendBrowserVersion() {
        final FxRequest req = FxJsfUtils.getRequest();
        switch( req.getBrowser() ) {
            case FIREFOX:
                return req.getBrowserVersion() >= 2.0d;
            case IE:
                return req.getBrowserVersion() >= 7.0d;
            case GECKO:
                return req.getBrowserVersion() >= 1.8;   // firefox 2.0 upwards
            case SAFARI:
                return req.getBrowserVersion() >= 530;  // safari 4.0+ - http://en.wikipedia.org/wiki/Safari_version_history
            default:
                return false;
        }
    }

    /**
     * Returns the browser version that the client is using
     *
     * @return the browser version the client is using
     * @since 3.1
     */
    public double getBrowserVersion() {
        return FxJsfUtils.getRequest().getBrowserVersion();
    }

    /**
     * Returns the browser that the client is using
     *
     * @return the browser the client is using
     */
    public String getBrowserAsString() {
        return String.valueOf(FxJsfUtils.getRequest().getBrowser());
    }

    /**
     * Get verbose information about the flexive build
     *
     * @return verbose information about the flexive build
     */
    public String getBuildInfoVerbose() {
        return FxSharedUtils.getFlexiveEditionFull() + " " + FxSharedUtils.getFlexiveVersion() + "/build #" + FxSharedUtils.getBuildNumber() + " - " + FxSharedUtils.getBuildDate();
    }

    /**
     * Get the flexive edition
     *
     * @return flexive edition
     */
    public String getEdition() {
        return FxSharedUtils.getFlexiveEdition();
    }

    /**
     * Get the flexive edition with product
     *
     * @return flexive edition with product
     */
    public String getEditionFull() {
        return FxSharedUtils.getFlexiveEditionFull();
    }

    /**
     * Get information about the flexive build
     *
     * @return information about the flexive build
     */
    public String getBuildInfo() {
        return FxSharedUtils.getFlexiveEdition() + " " + FxSharedUtils.getFlexiveVersion();
    }

    /**
     * Get a list of all installed and deployed drop names.
     *
     * @return list of all installed and deployed drop names.
     */
    public List<String> getDrops() {
        return FxSharedUtils.getDrops();
    }

    /**
     * Get a list of all installed and deployed drops.
     *
     * @return  a list of all installed and deployed drops.
     */
    public List<FxDropApplication> getDropApplications() {
        return FxSharedUtils.getDropApplications();
    }

    /**
     * Get a list of all installed and deployed drops that have a web application context, excluding
     * the flexive backend application.
     *
     * @return  a list of all installed and deployed drops that have a web application context.
     * @since 3.1
     */
    public List<FxDropApplication> getDropApplicationsWithContext() {
        final ArrayList<FxDropApplication> apps = Lists.newArrayList(FxSharedUtils.getDropApplications());
        final Iterator<FxDropApplication> iter = apps.iterator();
        while (iter.hasNext()) {
            final FxDropApplication application = iter.next();
            if (!application.isWebContextAvailable()
                    // drop flexive backend application form list
                    || "flexive".equals(application.getName())
                    || "flexive-backend-war".equals(application.getName())) {
                iter.remove();
            }
        }
        return apps;
    }

    /**
     * Get the flexive header string
     *
     * @return header string
     */
    public String getHeader() {
        String header = FxSharedUtils.getHeader();
        if (header.indexOf("$user$") > 0)
            header = header.replaceAll("\\$user\\$", getRequest().getUserTicket().getUserName());
        return header;
    }

    public ResultLocation getAdminResultLocation() {
        return AdminResultLocations.ADMIN;
    }

    /**
     * <p>Provides parametric access to components.</p>
     * <p/>
     * For example:
     * <pre>#{fxSystemBean.component['frm:myInputComponent']}</pre>
     * </p>
     *
     * @return a mapped function returning components for clientIds
     */
    public Map<String, UIComponent> getComponent() {
        return FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, UIComponent>() {
            public UIComponent get(Object key) {
                return getCurrentInstance().getViewRoot().findComponent((String) key);
            }
        });
    }

    /**
     * Provides parametric access to UIInput form values. Returns the submitted value during
     * decoding, if available, otherwise the value property is returned.
     *
     * @return the UIInput value property
     */
    public Map<String, Object> getComponentValue() {
        return FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, Object>() {
            public Object get(Object key) {
                final UIInput component = (UIInput) getCurrentInstance().getViewRoot().findComponent((String) key);
                return component.getSubmittedValue() != null ? component.getSubmittedValue() : component.getValue();
            }
        });
    }

    /**
     * Check via map if the calling user is in the given role
     *
     * @return if the calling user is in the given role
     */
    public Map<String, Boolean> getIsInRole() {
        return FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, Boolean>() {
            public Boolean get(Object key) {
                return getUserTicket().isInRole(Role.valueOf((String) key));
            }
        });
    }

    /**
     * Get the instance count of the given ID
     * 
     * @return the instance count
     */
    public Map<Long, Long> getInstanceCount() {
        if (instanceCountMap == null) {
            FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Long, Long>() {
                public Long get(Object key) {
                    return EJBLookup.getTypeEngine().getInstanceCount(Long.valueOf(key.toString()));
                }
            }, true);
        }
        return instanceCountMap;
    }

    /**
     * Escape a String for usage in java script (remove characters like ')
     *
     * @return escaped String
     */
    public Map<String, String> getEscapeForJavaScript() {
        return FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, String>() {
            public String get(Object key) {
                return FxFormatUtils.escapeForJavaScript(String.valueOf(key), false, true);
            }
        });
    }

    /**
     * Returns the lower-case name of the given JSF message severity.
     *
     * @return the lower-case name of the given JSF message severity.
     */
    public Map<FacesMessage.Severity, String> getSeverityName() {
        return FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<FacesMessage.Severity, String>() {
            public String get(Object key) {
                final FacesMessage.Severity severity = (FacesMessage.Severity) key;
                if (FacesMessage.SEVERITY_ERROR.equals(severity)) {
                    return "error";
                } else if (FacesMessage.SEVERITY_FATAL.equals(severity)) {
                    return "fatal";
                } else if (FacesMessage.SEVERITY_INFO.equals(severity)) {
                    return "info";
                } else if (FacesMessage.SEVERITY_WARN.equals(severity)) {
                    return "warn";
                } else {
                    return "";
                }
            }
        });
    }

    /**
     * Returns a new unique ID, obtained from the view root.
     *
     * @return a new unique ID, obtained from the view root.
     */
    public String getUniqueId() {
        return getCurrentInstance().getViewRoot().createUniqueId();
    }

    /**
     * Provides a key-based counter service for JSF pages. Supply your (unique) key
     * and get an auto-incremented counter value, starting with 0.
     *
     * @return a key-based counter service for JSF pages
     */
    public Map<Object, Integer> getCounter() {
        if (counterMap == null) {
            counterMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Object, Integer>() {
                public Integer get(Object key) {
                    if (counters == null) {
                        counters = new HashMap<Object, Integer>();
                    }
                    final int ctr = FxSharedUtils.get(counters, key, -1) + 1;
                    counters.put(key, ctr);
                    return ctr;
                }
            }, true);
        }
        return counterMap;
    }

    /**
     * Like {@link #getCounter()}, but only returns the current counter value without incrementing it.
     *
     * @return a key-based counter "reader" service for JSF pages
     */
    public Map<Object, Integer> getCounterValue() {
        if (counterValueMap == null) {
            counterValueMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Object, Integer>() {
                public Integer get(Object key) {
                    if (counters == null) {
                        counters = new HashMap<Object, Integer>();
                    }
                    return FxSharedUtils.get(counters, key, 0);
                }
            }, true);
        }
        return counterValueMap;
    }

    /**
     * Returns the unique page ID generated for flexive responses.
     *
     * @return the unique page ID generated for flexive responses.
     */
    public long getPageId() {
        final HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        return response instanceof FxResponseWrapper ? ((FxResponseWrapper) response).getId() : -1;
    }

    /**
     * Returns the total render time at the moment of this method call.
     *
     * @return the total render time at the moment of this method call.
     */
    public long getPageRenderTime() {
        final HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        return response instanceof FxResponseWrapper ? System.currentTimeMillis() - ((FxResponseWrapper) response).getCreatedAt() : 0;
    }

    /**
     * Return true if the content tree was modified in the current request.
     *
     * @return true if the content tree was modified in the current request.
     */
    public boolean isTreeModified() {
        return FxContext.get().getTreeWasModified();
    }

    /**
     * Return if this is a new flexive installation that needs initialization still
     *
     * @return if this is a new flexive installation that needs initialization still
     */
    public boolean isNewInstallation() {
        return CacheAdmin.isNewInstallation();
    }

    /**
     * A utility lookup function that returns true when a message key can be resolved in
     * {@link MessageBean}, false otherwise.
     *
     * @return true when a message key can be resolved in {@link MessageBean}, false otherwise.
     */
    public Map<String, Boolean> getMessageExists() {
        if (messageExistsMap == null) {
            messageExistsMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, Boolean>() {
                public Boolean get(Object key) {
                    try {
                        MessageBean.getInstance().getResource(String.valueOf(key));
                        return true;    // resource exists
                    } catch (MissingResourceException e) {
                        return false;   // no message for the given key available
                    }
                }
            });
        }
        return messageExistsMap;
    }

    /**
     * Returns mapped access to FxContent instances indexed by primary key.
     * The key parameter can be of type {@link Long}, {@link String}, {@link com.flexive.shared.content.FxPK FxPK},
     * or {@link com.flexive.shared.value.FxReference FxReference}, as described in
     * {@link com.flexive.shared.content.FxPK#fromObject(Object)}.
     *
     * @return the content instance for the given primary key
     */
    public Map<Object, FxContent> getContent() {
        if (contentMap == null) {
            contentMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Object, FxContent>() {
                public FxContent get(Object key) {
                    try {
                        return EJBLookup.getContentEngine().load(FxPK.fromObject(key));
                    } catch (FxApplicationException e) {
                        throw e.asRuntimeException();
                    }
                }
            }, true);
        }
        return contentMap;
    }

    /**
     * <p>
     * Returns mapped access to FxContent instances indexed by primary key.
     * The key parameter can be of type {@link Long}, {@link String}, {@link com.flexive.shared.content.FxPK FxPK},
     * or {@link com.flexive.shared.value.FxReference FxReference}, as described in
     * {@link com.flexive.shared.content.FxPK#fromObject(Object)}.
     * </p>
     * <p>
     * The only difference to {@link #getContent()} is that the returned content instance is "exploded", i.e.
     * that empty top-level properties and groups are properly initialized and not returned as null. This is
     * useful for editor pages that use contents provided by this bean.
     * </p>
     *
     * @return the content instance for the given primary key
     */
    public Map<Object, FxContent> getExplodedContent() {
        if (explodedContentMap == null) {
            explodedContentMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Object, FxContent>() {
                public FxContent get(Object key) {
                    try {
                        final FxContent content = EJBLookup.getContentEngine().load(FxPK.fromObject(key));
                        content.getRootGroup().explode(true);
                        return content;
                    } catch (FxApplicationException e) {
                        throw e.asRuntimeException();
                    }
                }
            }, true);
        }
        return explodedContentMap;
    }

    /**
     * Perform a FxSQL query
     * key=query string
     *
     * @return DataModel
     */
    public Map<String, DataModel> getQuery() {
        if (queries == null) {
            queries = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, DataModel>() {
                public DataModel get(Object key) {
                    try {
                        final FxResultSet result = EJBLookup.getSearchEngine().search((String) key, 0, -1, null);
                        return new FxResultSetDataModel(result);
                    } catch (FxApplicationException e) {
                        throw e.asRuntimeException();
                    }
                }
            }, true);
        }
        return queries;
    }

    /**
     * Returns the current system timestamp.
     *
     * @return the current system timestamp.
     */
    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Get the number of failed login attempts
     *
     * @return number of failed login attempts
     */
    public long getFailedLoginAttempts() {
        return FxContext.getUserTicket().getFailedLoginAttempts();
    }

    /**
     * Return the structure envirnoment (filtered for the current user).
     *
     * @return the structure envirnoment (filtered for the current user).
     */
    public FxEnvironment getEnvironment() {
        return CacheAdmin.getFilteredEnvironment();
    }

    /**
     * Return true if the current request has been submitted via Ajax4JSF.
     *
     * @return true if the current request has been submitted via Ajax4JSF.
     */
    public boolean isAjaxRequest() {
        return FxJsfUtils.isAjaxRequest();
    }

    /**
     * Return the name of the current network node.
     *
     * @return  the name of the current network node.
     * @since 3.1
     */
    public String getNodeName() {
        if (nodeName == null) {
            nodeName = EJBLookup.getNodeConfigurationEngine().getNodeName();
        }
        return nodeName;
    }

    /**
     * Get the host name
     *
     * @return host name
     */
    public String getHostName() {
        return FxSharedUtils.getHostName();
    }

    /**
     * Return the language to be used in GUI translations of flexive components. Returns the user's locale
     * if the locale is translated in our property files, otherwise "en" is returned.
     *
     * @return  the ISO code of the language to be used in GUI translations of flexive components
     * @since   3.1.3
     */
    public String getGuiTranslationLanguageIso() {
        final FxLanguage lang = getUserTicket().getLanguage();
        return FxSharedUtils.isTranslatedLocale(lang.getIso2digit())
                ? lang.getIso2digit().toLowerCase()
                : "en";
    }


    /**
     * String concat function (fx:concat).
     *
     * @param s1    the first string
     * @param s2    the second string
     * @return s1 + s2
     */
    public static String concat(Object s1, Object s2) {
        return String.valueOf(s1) + String.valueOf(s2);
    }
}
