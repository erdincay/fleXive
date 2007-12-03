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
package org.apache.myfaces.webapp.filter;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxBinary;
import com.flexive.war.FxRequest;
import com.flexive.war.beans.admin.content.CeIdGenerator;
import com.flexive.war.beans.admin.content.ContentEditorBean;
import com.flexive.war.beans.admin.content.InlineContentEditorBean;
import org.apache.commons.fileupload.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * This class is a modified version of the myfaces MultipartRequestWrapper, which adds
 * support for the fleXive Binary Data Class.
 * <p/>
 * Deprecation warnings are suppressed, since there are a lot of deprecated functions in use.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@SuppressWarnings("deprecation")
public class MultipartRequestWrapper extends HttpServletRequestWrapper {
    private static Log log = LogFactory.getLog(MultipartRequestWrapper.class);
    public static final String UPLOADED_FILES_ATTRIBUTE = "org.apache.myfaces.uploadedFiles";
    public static final String WWW_FORM_URLENCODED_TYPE = "application/x-www-form-urlencoded";

    FxRequest request = null;
    HashMap<String, String[]> parametersMap = null;
    DiskFileUpload fileUpload = null;
    HashMap<String, FileItem> fileItems = null;
    int maxSize;
    int thresholdSize;
    String repositoryPath;
    ContentEditorBean ceb;

    public MultipartRequestWrapper(HttpServletRequest request, int maxSize, int thresholdSize, String repositoryPath) {
        super(request);
        this.request = (FxRequest) request;
        this.maxSize = maxSize;
        this.thresholdSize = thresholdSize;
        this.repositoryPath = repositoryPath;
        this.ceb = null;
    }

    private void parseRequest() {
        fileUpload = new DiskFileUpload();
        fileUpload.setFileItemFactory(new DefaultFileItemFactory());
        fileUpload.setSizeMax(maxSize);

        fileUpload.setSizeThreshold(thresholdSize);

        if (repositoryPath != null && repositoryPath.trim().length() > 0)
            fileUpload.setRepositoryPath(repositoryPath);

        String charset = request.getCharacterEncoding();
        fileUpload.setHeaderEncoding(charset);


        List requestParameters;
        try {
            requestParameters = fileUpload.parseRequest(request.getRequest());
        } catch (FileUploadBase.SizeLimitExceededException e) {
            // TODO: find a way to notify the user about the fact that the uploaded file exceeded size limit
            if (log.isInfoEnabled())
                log.info("user tried to upload a file that exceeded file-size limitations.", e);
            requestParameters = Collections.EMPTY_LIST;

        } catch (FileUploadException fue) {
            log.error("Exception while uploading file.", fue);
            requestParameters = Collections.EMPTY_LIST;
        }

        parametersMap = new HashMap<String, String[]>(requestParameters.size());
        fileItems = new HashMap<String, FileItem>();
        final FxEnvironment environment = CacheAdmin.getEnvironment();

        Iterator iter = requestParameters.iterator();
        if (iter.hasNext()) {
            do {
                FileItem fileItem = (FileItem) iter.next();
                if (fileItem.isFormField()) {
                    String name = fileItem.getFieldName();
                    // The following code avoids commons-fileupload charset problem.
                    // After fixing commons-fileupload, this code should be
                    // >>	String value = fileItem.getString();  <<
                    String value;
                    if (charset == null) {
                        value = fileItem.getString();
                    } else {
                        try {
                            value = new String(fileItem.get(), charset);
                        } catch (UnsupportedEncodingException e) {
                            value = fileItem.getString();
                        }
                    }

                    addTextParameter(name, value);
                } else {
                    if (fileItem.getName() != null && fileItem.getName().length() > 0) {
                        if (request.isContentEditor() || request.isInlineContentEditor()) {
                            String xpath = fileItem.getFieldName().split(":")[1];
                            xpath = CeIdGenerator.decodeToXPath(xpath).substring(10);
                            InputStream uploadedStream = null;
                            try {
                                ceb = request.isContentEditor() ?
                                        ContentEditorBean.getSingleton().getInstance() :
                                        InlineContentEditorBean.getSingleton().getInstance();
                                uploadedStream = fileItem.getInputStream();
                                BinaryDescriptor binary = new BinaryDescriptor(fileItem.getName(), fileItem.getSize(), uploadedStream);
                                ceb.setBinary(xpath,
                                        new FxBinary(((FxPropertyAssignment) ceb.getEnvironment().getAssignment(environment.getType(ceb.getType()).getName() + xpath)).isMultiLang(), binary));
                            } catch (Throwable t) {
                                System.err.println(t.getMessage());
                                t.printStackTrace();
                            } finally {
                                if (uploadedStream != null) {
                                    try {
                                        uploadedStream.close();
                                    } catch (Throwable t) {/*ignore*/}
                                }
                            }
                        } else {

                            fileItems.put(fileItem.getFieldName(), fileItem);
                        }
                    }
                }
            } while (iter.hasNext());
        }

        if (ceb != null) {
            try {
                // Call prepare save once at the end of all file uploads
                ceb.prepareSave();
            } catch (Throwable t) {
                System.err.println(t.getMessage());
                t.printStackTrace();
            }
        }

        //Add the query string paramters
        Iterator it = request.getParameterMap().entrySet().iterator();
        if (it.hasNext()) {
            do {
                Map.Entry entry = (Map.Entry) it.next();
                Object value = entry.getValue();
                if (value instanceof String[]) {
                    String[] valuesArray = (String[]) entry.getValue();
                    int i = 0;
                    while (i < valuesArray.length) {
                        addTextParameter((String) entry.getKey(), valuesArray[i]);
                        i++;
                    }
                } else if (value instanceof String) {
                    String strValue = (String) entry.getValue();
                    addTextParameter((String) entry.getKey(), strValue);
                } else if (value != null)
                    throw new IllegalStateException("value of type : " + value.getClass() + " for key : " +
                            entry.getKey() + " cannot be handled.");
            } while (it.hasNext());
        }
    }

    private void addTextParameter(String name, String value) {
        if (!parametersMap.containsKey(name)) {
            String[] valuesArray = {value};
            parametersMap.put(name, valuesArray);
        } else {
            String[] storedValues = parametersMap.get(name);
            int lengthSrc = storedValues.length;
            String[] valuesArray = new String[lengthSrc + 1];
            System.arraycopy(storedValues, 0, valuesArray, 0, lengthSrc);
            valuesArray[lengthSrc] = value;
            parametersMap.put(name, valuesArray);
        }
    }

    public Enumeration getParameterNames() {
        if (parametersMap == null) parseRequest();
        return Collections.enumeration(parametersMap.keySet());
    }

    public String getParameter(String name) {
        if (parametersMap == null) parseRequest();
        String[] values = parametersMap.get(name);
        if (values == null)
            return null;
        return values[0];
    }

    public String[] getParameterValues(String name) {
        if (parametersMap == null) parseRequest();
        return parametersMap.get(name);
    }

    public Map getParameterMap() {
        if (parametersMap == null) parseRequest();
        return parametersMap;
    }

    // Hook for the x:inputFileUpload tag.
    public FileItem getFileItem(String fieldName) {
        if (fileItems == null) parseRequest();
        return fileItems.get(fieldName);
    }

    /**
     * Not used internally by MyFaces, but provides a way to handle the uploaded files
     * out of MyFaces.
     *
     * @return the map
     */
    public Map getFileItems() {
        if (fileItems == null) parseRequest();
        return fileItems;
    }


    public Object getAttribute(String string) {
        if (string.equals(UPLOADED_FILES_ATTRIBUTE)) {
            return getFileItems();
        }
        return super.getAttribute(string);
    }

    public String getContentType() {
        return WWW_FORM_URLENCODED_TYPE;
    }
}