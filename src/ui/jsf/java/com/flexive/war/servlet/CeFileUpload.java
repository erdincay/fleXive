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
package com.flexive.war.servlet;

import com.flexive.shared.content.FxContent;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxBinary;
import com.flexive.war.beans.admin.content.ContentEditorBean;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

public class CeFileUpload implements Servlet {

    private ServletConfig servletConfig = null;

    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
    }

    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        String renderContent = null;
        try {

            final HttpServletRequest request = (HttpServletRequest) servletRequest;
            final ContentEditorBean ceb = ContentEditorBean.getSingleton().getInstance(request);

            if (ceb == null) {
                renderContent = "No Content Editor Bean is active";
            } else {

                // Create a factory for disk-based file items
                FileItemFactory factory = new DiskFileItemFactory();

                // Create a new file upload handler
                ServletFileUpload upload = new ServletFileUpload(factory);

                // Parse the request
                List /* FileItem */ items = upload.parseRequest(request);

                BinaryDescriptor binary = null;

                String xpath = null;
                for (Object item1 : items) {
                    FileItem item = (FileItem) item1;
                    if (item.isFormField()) {
                        if (item.getFieldName().equalsIgnoreCase("result")) {
                            renderContent = item.getString().replaceAll("\\\\n", "\\\n");
                        } else if (item.getFieldName().equalsIgnoreCase("xpath")) {
                            xpath = item.getString();
                        }
                    } else {
                        InputStream uploadedStream = null;
                        try {
                            uploadedStream = item.getInputStream();
                            binary = new BinaryDescriptor(item.getName(), item.getSize(), uploadedStream);
                        } finally {
                            if (uploadedStream != null) uploadedStream.close();
                        }
                    }
                    System.out.println("Item: " + item.getName());
                }


                FxContent co = ceb.getContent();
                FxBinary binProperty = new FxBinary(binary);
                co.setValue(xpath, binProperty);
                ceb.getContentEngine().prepareSave(co);
            }
        } catch (Throwable t) {
            System.err.println(t.getMessage());
            t.printStackTrace();
            renderContent = t.getMessage();
        }

        // Render the result
        PrintWriter w = servletResponse.getWriter();
        if (renderContent == null) {
            renderContent = "No content";
        }
        w.print(renderContent);
        w.close();
        servletResponse.setContentType("text/html");
        servletResponse.setContentLength(renderContent.length());
        ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_OK);
    }

    public String getServletInfo() {
        return this.getClass().getName();
    }

    public void destroy() {
        // nothing to do
    }
}
