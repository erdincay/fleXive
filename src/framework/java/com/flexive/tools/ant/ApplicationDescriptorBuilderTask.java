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
package com.flexive.tools.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ANT task to build application.xml descriptors on the fly with descriptor entries
 * for drop-in applications
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ApplicationDescriptorBuilderTask extends Task {
    /** Disabled drop directory name */
    private static final String DIR_DISABLED = "disabled";

    private String dropDir = null;
    private String srcFile = null;
    private String destFile = null;

    public void setDropDir(String dropDir) {
        this.dropDir = dropDir;
    }

    public void setSrcFile(String srcFile) {
        this.srcFile = srcFile;
    }

    public void setDestFile(String destFile) {
        this.destFile = destFile;
    }

    public String getTaskName() {
        return "applicationDescriptorBuilder";
    }

    public void execute() throws BuildException {
        if (srcFile == null || !(new File(srcFile).exists()))
            throw new BuildException("argument srcFile (" + srcFile + ") does not exist!");
        storeFile(
                loadFile(new File(srcFile)).
                        replace("<!--DROP_IN_PLACEHOLDER-->", processDrops()),
                new File(destFile));
    }

    /**
     * Load the contents of a file, returning it as a String.
     * This method should only be used when really necessary since no real error handling is performed!!!
     *
     * @param file the File to load
     * @return file contents
     */
    public static String loadFile(File file) {
        try {
            return loadFromInputStream(new FileInputStream(file), (int) file.length());
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Load a String from an InputStream (until end of stream)
     *
     * @param in     InputStream
     * @param length length of the string if &lt; -1 (NOT number of bytes to read!)
     * @return String
     */
    public static String loadFromInputStream(InputStream in, int length) {
        StringBuilder sb = new StringBuilder(length > 0 ? length : 5000);
        try {
            int read;
            byte[] buffer = new byte[1024];
            while ((read = in.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, read));
            }
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return sb.toString();
    }

    /**
     * Rather primitive "write String to file" helper, returns <code>false</code> if failed
     *
     * @param contents the String to store
     * @param file     the file, if existing it will be overwritten
     * @return if successful
     */
    public static boolean storeFile(String contents, File file) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(contents.getBytes("UTF-8"));
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    private String processDrops() {
        StringBuilder sb = new StringBuilder(500).append("\n");
        StringBuilder dropNames = new StringBuilder(100);
        File drops = new File(dropDir);
        List<File> jar = new ArrayList<File>(5);
        List<File> ejb = new ArrayList<File>(5);
        List<File> war = new ArrayList<File>(5);
        processDirectory(drops, jar, ejb, war);
        // Currently, JARs will be copied to the ear/lib directory and do not have to be registered
/*
        for (File f : jar) {
            sb.append("    <module>\n" + "      <java>").
                    append(f.getName()).
                    append("</java>\n" + "    </module>\n\n");
        }
*/
        for (File f : ejb) {
            sb.append("    <module>\n" + "      <ejb>").
                    append(f.getName()).
                    append("</ejb>\n" + "    </module>\n\n");
        }
        for (File f : war) {
            if (dropNames.length() > 0)
                dropNames.append(',');
            dropNames.append(f.getName().substring(0, f.getName().lastIndexOf('.')));
            sb.append("    <module>\n" + "        <web>\n" + "            <web-uri>").
                    append(f.getName()).
                    append("</web-uri>\n" + "            <context-root>/").
                    append(f.getName().substring(0, f.getName().lastIndexOf('.'))).
                    append("</context-root>\n" + "        </web>\n" + "    </module>\n");
        }
        storeFile(dropNames.toString(), new File(drops.getAbsolutePath() + File.separatorChar + "drops.archives"));
        return sb.toString();
    }

    private void processDirectory(File directory, List<File> jars, List<File> ejbs, List<File> wars) {
        if (!directory.exists() || !directory.isDirectory())
            throw new BuildException("argument dropDir (" + directory.getAbsolutePath() + ") is not a valid directory!");
        if (DIR_DISABLED.equals(directory.getName())) {
            return; // ignore
        }
        for (File f : directory.listFiles()) {
            if (f.isDirectory())
                processDirectory(f, jars, ejbs, wars);
            else if (f.getAbsolutePath().toLowerCase().endsWith("-ejb.jar"))
                ejbs.add(f);
            else if (f.getAbsolutePath().toLowerCase().endsWith(".jar"))
                jars.add(f);
            else if (f.getAbsolutePath().toLowerCase().endsWith(".war"))
                wars.add(f);
            else if (!f.getName().startsWith(".") && !f.getName().equals("drops.archives")
                    && !f.getName().equals("format") && !f.getName().equals("entries")
                    && !f.getName().equals("all-wcprops")) //ignore .svn, .cvs etc files
                System.out.println("Warning: Unrecognized drop file: " + f.getName());
        }
    }
}
