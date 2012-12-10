/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2010
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

/**
 * Some useful groovy tasks for ant
 *
 * @author Markus Plesser, Unique Computing Solutions (UCS)
 */

import org.apache.tools.ant.Task
import org.apache.tools.ant.BuildException


/**
* Delete a file and schedule for deletion if not possible
*/
class safeDelete extends Task {
    String file
    public void execute() {
        def f = new File(file)
        if (!f.exists() || !f.isFile())
            return
        if (!f.delete()) {
            println "Scheduling " + file + " for removal..."
            f.deleteOnExit()
        } else
            println "Removed " + file
    }
}

/**
 * Build a windows or unix start file
 */
class buildBatchFile extends Task {
    String platform
    String application
    String path
    String mainClass

    def checkPlatform() {
        switch (platform) {
            case "windows":
            case "unix":
                break;
            default:
                throw new BuildException("Wrong or no platform supplied! Only 'windows' and 'unix' are valid values for 'platform'-attribute!")
        }
    }

    public void execute() {
        checkPlatform()
        if (application == null)
            throw new BuildException("No application specified!")
        if (path == null)
            throw new BuildException("No path specified!")
        if (mainClass == null)
            throw new BuildException("No mainClass specified!")
        String fileName = application
        String cpSep = ""
        switch (platform) {
            case "windows": fileName += ".bat"; cpSep = ";"; break
            case "unix": fileName += ".sh"; cpSep = ":"; break
        }
        File file = new File(path + File.separatorChar + fileName)
        if (file.exists()) return
        println "Building ${platform} batchfile ${file}"
        def lib_prefix = ".." + File.separatorChar + "lib" + File.separatorChar
        def classpath = lib_prefix + application + ".jar"
        new File(project.properties['lib.dir']).eachFileMatch(~/.*\.jar/) {jar ->
            classpath += "${cpSep}${lib_prefix}${jar.name}"
        }
        def cmd = "java -Dlog4j.configuration=file:../config/log4j.xml -classpath ${classpath} ${mainClass}"
        (1..20).each {cmd += " %" + it}
        file.withWriter {out ->
            if (platform == 'windows')
                out << "@echo off\n"
            else if (platform == 'unix')
                out << "#!/bin/sh\n"
            out << cmd << "\n"
        }
    }
}

/**
 * Execute a shell/batch command depending on OS
 */
class batchExec extends Task {
    //path attribute
    String dir
    String scriptBase
    def static ant

    public void execute() {
        boolean isWindows = System.properties['os.name'].toLowerCase().contains('windows')
        def command = """$dir$File.separator$scriptBase${isWindows ? ".bat" : ".sh"}"""
        ant.exec(dir: dir, executable: command)
    }
}

//avoid duplicates
if (!project.getTaskDefinitions().containsKey("safeDelete")) {
    //'inject' ant
    batchExec.ant = ant
    //add the tasks to ant
    project.addTaskDefinition('safeDelete', safeDelete)
    project.addTaskDefinition('buildBatchFile', buildBatchFile)
    project.addTaskDefinition('batchExec', batchExec)

    //set the isJDK6 property
    //(code from FxSharedUtils)
    int major = -1, minor = -1
    try {
        String[] ver = System.getProperty("java.specification.version").split("\\.")
        if (ver.length >= 2) {
            major = Integer.valueOf(ver[0])
            minor = Integer.valueOf(ver[1])
        }
    } catch (Exception e) {
    }
    if (major > 1 || (major == 1 && minor >= 6))
        properties['isJDK6'] = true;
}