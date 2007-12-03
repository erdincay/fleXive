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
 * Retrieve the current checked out subversion version
 */
class svnVersion extends Task {
    String property

    public void execute() {
        File f = new File(".svn/entries")
        if (f.exists()) {
            try {
                StringBuffer sbInput = new StringBuffer(1000)
                FileReader fr = new FileReader(f)
                while (fr.ready()) sbInput.append((char) fr.read())
                fr.close()

                StringTokenizer tok = new StringTokenizer(sbInput.toString(), "\n", false)
                String currLine = null
                boolean isV4 = false
                int line = 0
                while (tok.hasMoreTokens()) {
                    line++
                    if (currLine == null) {
                        currLine = tok.nextToken().trim()
                        isV4 = !currLine.startsWith("<?xml")
                    } else
                        currLine = tok.nextToken().trim()
                    if (!isV4 && currLine.startsWith("revision")) {
                        String rev = currLine.substring(currLine.indexOf('"') + 1, currLine.lastIndexOf('"'))
                        project.setProperty(property, rev)
                        return
                    } else if (isV4 && line == 3) {
                        project.setProperty(property, currLine.trim())
                        return
                    }
                }
            } catch (IOException e) {
                e.printStackTrace()
            }
        } else
            project.setProperty(property, "-")
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
    project.addTaskDefinition('svnVersion', svnVersion)
    project.addTaskDefinition('batchExec', batchExec)
}