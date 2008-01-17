/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.tools.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public void setScriptDir(String scriptDir) {
        this.scriptDir = scriptDir;
    }

    public void setIndexFile(String indexFile) {
        this.indexFile = indexFile;
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
        if (file.exists()) {
            System.err.println("Warning: " + file.getName() + " already exists! Overwriting!");
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
        if( !idxDir.exists() ) {
            System.out.println("Creating ["+idxDir.getAbsolutePath()+"]");
            idxDir.mkdirs();
        }
        File scripts = new File(scriptDir);
        if (!scripts.exists() || !scripts.isDirectory())
            usage();
        File[] files = scripts.listFiles();
        List<String> scriptlist = new ArrayList<String>(files.length);
        for (File f : files) {
            if (!f.isFile() || ".".equals(f.getName()) || "..".equals(f.getName()) || index.getName().equals(f.getName()))
                continue;
            scriptlist.add(f.getName()+"|"+f.length());
        }
        Collections.sort(scriptlist);
        StringBuilder sb = new StringBuilder(5000);
        for (String s : scriptlist) {
            sb.append(s).append("\n");
        }
        storeFile(sb.toString(), index);
        System.out.println("Processed [" + scripts.getAbsolutePath() + "] --> [" + index.getAbsolutePath() + "]");
    }

    private void usage() {
        throw new BuildException(getTaskName()+": need valid scriptDir and indexFile attributes!");
    }

}
