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
package com.flexive.war.webdav;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;

/**
 * FileSystem DAV context implementation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxDavFileSystemContext extends FxDavContext {

    private static FxDavFileSystemContext ddc = new FxDavFileSystemContext();
    private static String BASE = "/home/pandur";

    /**
     * Returns a singleton for the class.
     *
     * @return a singleton for the class.
     */
    public FxDavContext getSingleton() {
        return ddc;
    }

    /**
     * Gets the resource stored under the given name, or null if the resource does not exist.
     * <p/>
     * The returned resource may be a collection (directory) or file.<br>
     * The path starts always from the root entry, but there is no starting '/' character.<br>
     * A empty String denotes the root collection.
     *
     * @param request the current request
     * @param name    the name of the resource, eg 'myFolder1/myFolder2/demo.html'
     * @return the resource
     */
    public FxDavEntry getResource(HttpServletRequest request, String name) {
        if (name.equals("")) {
            return new FxDavCollection("", new Date(), new Date());
        }
        String fname = BASE + File.separatorChar + name;
        File f = new File(fname);
        if (f.exists()) {
            if (f.isDirectory())
                return new FxDavCollection(f.getName(), new Date(f.lastModified()), new Date(f.lastModified()));
            return new FxDavResource(name, new Date(f.lastModified()), new Date(f.lastModified()), f.length());
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Returns all childs of a given resource.
     * <p/>
     * A empty String denotes the root collection.
     *
     * @param request the current request
     * @param name    the name of the resource, eg 'myFolder1/myFolder2', 'myFolder1/File1.txt', 'File.txt'
     * @return all childs of a given resource.
     */
    public FxDavEntry[] getChildren(HttpServletRequest request, String name) {
        ArrayList<FxDavEntry> entries = new ArrayList<FxDavEntry>(20);
        File fBase = new File(BASE + File.separatorChar + name);
        File[] all = fBase.listFiles();
        for (File curr : all) {
            if (curr.isDirectory()) {
                entries.add(new FxDavCollection(curr.getName(), new Date(curr.lastModified()), new Date(curr.lastModified())));
            } else if (curr.isFile()) {
                entries.add(new FxDavResource(curr.getName(), new Date(curr.lastModified()), new Date(curr.lastModified()), curr.length()));
            }
        }
        return entries.toArray(new FxDavEntry[0]);
    }

    /**
     * Serves a resource to the webdav client.
     *
     * @param request  the current request
     * @param response the response
     * @param name     the name of the resource, eg 'myFolder1/File1.txt'
     * @throws java.io.IOException if the resource could not be served
     */
    public void serviceResource(HttpServletRequest request, HttpServletResponse response, String name) throws IOException {
        FxDavEntry entry = getResource(request, name);
        if (entry == null || entry.isCollection()) {
            response.sendError(FxWebDavStatus.SC_NOT_FOUND);
            return;
        }
        FxDavResource resource = (FxDavResource) entry;
        File f = new File(BASE + File.separatorChar + name);
        String ext = f.getAbsolutePath().lastIndexOf('.') > 0 ? f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1) : "unknown";
        System.out.println("sending: " + f.getAbsolutePath() + " ext:" + ext);
        response.setContentLength((int) resource.getContentlength());
        response.setContentType(FxWebDavUtils.getContentTypeMapping(ext) + ";charset=UTF-8");
        FxWebDavUtils.setModifiedAtDate(response, resource.getLastmodified());
        response.setStatus(HttpServletResponse.SC_OK);
        OutputStream out = null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            out = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1)
                out.write(buffer, 0, read);
            out.flush();
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

    }

    /**
     * Create a collection (Mkcol operation).
     * <p/>
     * MKCOL creates a new collection resource at the location specified by the Request-URI.
     * If the resource identified by the Request-URI is non-null then the MKCOL MUST fail.
     * During MKCOL processing, a server MUST make the Request-URI a member of its parent collection, unless the
     * Request-URI is "/". If no such ancestor exists, the method MUST fail.<br>
     * When the MKCOL operation creates a new collection resource, all ancestors MUST already exist, or the method MUST
     * fail with a 409 (Conflict) status code. For example, if a request to create collection /a/b/c/d/ is made, and
     * neither /a/b/ nor /a/b/c/ exists, the request must fail.<br>
     * <br><br>
     * Possible return codes:
     * 201 (Created) - The collection or structured resource was created in its entirety.<br>
     * 403 (Forbidden) - This indicates at least one of two conditions: 1) the server does not allow the creation of
     * collections at the given location in its namespace, or 2) the parent collection of the Request-URI exists but
     * cannot accept members.<br>
     * 405 (Method Not Allowed) - MKCOL can only be executed on a deleted/non-existent resource.<br>
     * 409 (Conflict) - A collection cannot be made at the Request-URI until one or more intermediate collections have
     * been created.<br>
     * 507 (Insufficient Storage) - The resource does not have sufficient space to record the state of the resource
     * after the execution of this method.<br>
     * 423 (Locked) - The resource is locked
     *
     * @param request the current request
     * @param name    the absolute name of the collection, eg 'folder1/folder2/new_folder'
     * @return the status code, WebdavStatus.SC_CREATED if the operation was successfull
     */
    public int createCollection(HttpServletRequest request, String name) {
        System.out.println("creating: " + new File(BASE + File.separator + name).getAbsolutePath());
        return new File(BASE + File.separator + name).mkdirs() ? FxWebDavStatus.SC_CREATED : FxWebDavStatus.SC_INSUFFICIENT_SPACE_ON_RESOURCE;
    }

    /**
     * Creates a resource.
     *
     * @param path    the absolute path of the new resource, eg 'folder1/folder2/file.txt'
     * @param request the request containing the data for the resource
     * @return the status code, WebdavStatus.SC_CREATED if the operation was successfull
     */
    public int createResource(HttpServletRequest request, String path) {
        try {
            File newFile = new File(BASE + File.separator + path);

            if (!newFile.createNewFile())
                return FxWebDavStatus.SC_METHOD_FAILURE;
            ServletInputStream in = null;
            FileOutputStream out = null;
            try {
                in = request.getInputStream();
                out = new FileOutputStream(newFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (out != null)
                        out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return FxWebDavStatus.SC_CREATED;
        } catch (IOException e) {
            return FxWebDavStatus.SC_METHOD_FAILURE;
        }
    }

    /**
     * Moves a resource.
     *
     * @param src     the absolute path of the source resource, eg 'folder1/folder2/file.txt'
     * @param dest    the absolute path of the destination resource, eg 'folder1/folder2/file_new.txt'
     * @param request the request containing the data for the resource
     */
    public void moveResource(HttpServletRequest request, String src, String dest) {
        System.out.println("moving " + src + "->" + dest + " success:" +
                new File(BASE + File.separator + src).renameTo(new File(BASE + File.separator + dest)));
    }

    /**
     * Copies a resource.
     *
     * @param src     the absolute path of the source resource, eg 'folder1/folder2/file.txt'
     * @param dest    the absolute path of the destination resource, eg 'folder1/folder2/file_new.txt'
     * @param request the request containing the data for the resource
     * @return the status code, FxWebDavStatus.SC_OK if the operation was successfull
     */
    public int copyResource(HttpServletRequest request, String src, String dest) {
        return FxWebDavStatus.SC_NOT_IMPLEMENTED;
    }

    /**
     * Attempts to delete a file, with error checking.
     *
     * @param f The file to delete.
     * @return True if sucessful.
     */
    private static boolean delete(File f) {

        if (!f.delete()) {
            if (f.list().length != 0) {
                File fa[] = f.listFiles();
                for (File aFa : fa) {
                    if (!aFa.isDirectory())
                        aFa.delete();
                    else {
                        delete(aFa);
                        aFa.delete();
                    }
                }
                return delete(f);
            }
            return false;
        }
        return true;
    }

    /**
     * Deletes a resource.
     *
     * @param path    the absolute path of the new resource, eg 'folder1/folder2/file.txt'
     * @param request the request containing the data for the resource
     * @return the status code, FxWebDavStatus.SC_OK if the operation was successful
     */
    public int deleteResource(HttpServletRequest request, String path) {
        File f = new File(BASE + File.separatorChar + path);
        if (!f.exists())
            return FxWebDavStatus.SC_NOT_FOUND;
        return delete(f) ? FxWebDavStatus.SC_OK : FxWebDavStatus.SC_CONFLICT;
    }
}
