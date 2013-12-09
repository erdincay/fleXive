/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ANT task to build an index file (line seperated) for a script directory to be used by
 * the ScriptingEngine
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ScriptIndexBuilderTask extends Task {

    private String scriptDir = null;
    private String indexFile = null;
    private boolean copyFiles = false;
    private boolean recursive = false;

    public void setScriptDir(String scriptDir) {
        this.scriptDir = scriptDir;
    }

    public void setIndexFile(String indexFile) {
        this.indexFile = indexFile;
    }

    public void setCopyFiles(boolean copyFiles) {
        this.copyFiles = copyFiles;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public String getTaskName() {
        return "scriptIndexBuilder";
    }

    /**
     * Rather primitive "write String to file" helper, returns <code>false</code> if failed
     * Code copied from FxSharedUtils!
     *
     * @param contents the String to store
     * @param file     the file, if existing it will be overwritten
     * @return if successful
     */
    public static boolean storeFile(String contents, File file) {
        /*if (file.exists()) {
            System.err.println("Warning: " + file.getName() + " already exists! Overwriting!");
        }*/
        if (file.exists()) {
            // check if the contents are equal
            try {
                final byte[] newBytes = contents.getBytes("UTF-8");
                final byte[] oldBytes = new byte[newBytes.length + 10];
                final int read = Math.max(new FileInputStream(file).read(oldBytes), 0);
                if (read == newBytes.length && new String(oldBytes, 0, read).equals(contents)) {
                    // identical bytes, same length, don't rewrite file
                    //System.out.println("No changes for " + file.getPath());
                    return true;
                }
            } catch (IOException e) {
                System.err.println("Failed to read old file contents (ignored): " + e.getMessage());
            }
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(contents.getBytes("UTF-8"));
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to store " + file.getAbsolutePath() + ": " + e.getMessage());
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

    public void execute() throws BuildException {
        if (scriptDir == null || indexFile == null)
            usage();
        File index = new File(indexFile);
        if (index.exists() && !index.isFile())
            usage();
        File idxDir = new File(index.getAbsolutePath().substring(0, index.getAbsolutePath().lastIndexOf(File.separatorChar)));
        if (!idxDir.exists()) {
            System.out.println("Creating [" + idxDir.getAbsolutePath() + "]");
            idxDir.mkdirs();
        }
        File scripts = new File(scriptDir);
        if (!scripts.exists() || !scripts.isDirectory())
            usage();

        if (copyFiles && recursive) {
            throw new BuildException(getTaskName() + ": cannot combine copyFiles and recursive attributes");
        }

        List<String> scriptlist = new ArrayList<String>();
        addDirectory(scriptlist, index, idxDir, scripts, "");

        Collections.sort(scriptlist);
        StringBuilder sb = new StringBuilder(5000);
        for (String s : scriptlist) {
            sb.append(s).append("\n");
        }
        storeFile(sb.toString(), index);
//        System.out.println("Processed [" + scripts.getAbsolutePath() + "] --> [" + index.getAbsolutePath() + "]");
    }

    private void addDirectory(List<String> scriptList, File index, File idxDir, File scripts, String prefix) {
        File[] files = scripts.listFiles();
        if (files == null) {
            return;
        }
        final String filePrefix = prefix == null || prefix.length() == 0 ? "" : prefix + File.separator;
        for (File f : files) {
            if (".".equals(f.getName()) || "..".equals(f.getName()) || index.getName().equals(f.getName()))
                continue;
            if (f.isDirectory()) {
                if (recursive) {
                    addDirectory(scriptList, index, idxDir, f, filePrefix + f.getName());
                }
                continue;
            }
            scriptList.add(filePrefix + f.getName() + "|" + f.length());
            if (copyFiles) {
                try {
                    final File dest = new File(idxDir.getAbsolutePath() + File.separator + f.getName());
                    if (dest.exists() && dest.length() == f.length())
                        continue;
                    System.out.println("Copying " + f.getName() + " -> " + dest);
                    FileChannel ic = new FileInputStream(f).getChannel();
                    FileChannel oc = new FileOutputStream(dest).getChannel();
                    ic.transferTo(0, ic.size(), oc);
                    ic.close();
                    oc.close();
                } catch (IOException e) {
                    throw new BuildException("Failed to copy file!", e);
                }
            }
        }
    }

    private void usage() {
        throw new BuildException(getTaskName() + ": need valid scriptDir and indexFile attributes!");
    }

}
