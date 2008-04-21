/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
import org.apache.tools.ant.taskdefs.Concat;
import org.apache.tools.ant.types.FileSet;

import java.io.File;

/**
 * Builds message property files.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class MakeMessagesTask extends Task {
    private String srcdir;
    private String dest;
    private String srcprefix = "";
    private String languages = "en,de";

    @Override
    public void execute() throws BuildException {
        final String[] langs = languages.split(",");
        // create language-specific property files
        for (String language: langs) {
            createMessages(language, language);
        }
        // create default property files
        createMessages(null, null);     // without suffix
        createMessages(null, langs[0]); // with default language suffix, used if the JVM locale
                                        // is not equal to the default language
    }

    private void createMessages(String language, String destSuffix) {
        final File dest = new File(this.dest + (destSuffix != null ? "_" + destSuffix : "") + ".properties");
        final Concat concat = (Concat) getProject().createTask("concat");
        concat.setDestfile(dest);
        final FileSet fileSet = new FileSet();
        fileSet.setProject(getProject());
        fileSet.setDir(new File(srcdir));
        if (language == null) {
            fileSet.setIncludes((srcprefix != null ? srcprefix + "*" : "*") + ".properties");
            fileSet.setExcludes(srcprefix + "*_??.properties");
        } else {
            fileSet.setIncludes(srcprefix + "*_" + language + ".properties");
        }
        concat.addFileset(fileSet);
        concat.setFixLastLine(true);
        concat.setForce(false);
        concat.execute();
    }

    /**
     * Sets the source directory containing all .properties files.
     *
     * @param srcdir    the source directory containing all .properties files.
     */
    public void setSrcdir(String srcdir) {
        this.srcdir = srcdir;
    }

    /**
     * Sets the target name of the output property file (e.g. "ApplicationResources").
     * @param dest  the base name of the output property file (e.g. "ApplicationResources")
     */
    public void setDest(String dest) {
        this.dest = dest;
    }

    /**
     * Sets the common prefix of the files to be included (may be null).
     *
     * @param srcprefix    the common prefix of the files to be included (e.g. "Ex", may be null). Should
     * not contain wildcards.
     */
    public void setSrcprefix(String srcprefix) {
        this.srcprefix = srcprefix;
    }

    /**
     * Sets the language(s) to be processed (comma separated). The first language
     * is used as the default language, i.e. will be used to created the fallback
     * properties file without a language suffix.
     *
     * @param languages the language(s) to be processed (comma separated).
     */
    public void setLanguages(String languages) {
        this.languages = languages;
    }
}
