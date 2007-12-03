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
package com.flexive.tools;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Retrieve the current checked out subversion version for flexive
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class subversionVersion {

    public static void main(String[] args) {
        System.out.println(getVersion());
    }

    public static String getVersion() {
        File f = new File(".svn/entries");
        if (f.exists()) {
            try {
                StringBuffer sbInput = new StringBuffer(1000);
                FileReader fr = new FileReader(f);
                while (fr.ready()) sbInput.append((char) fr.read());
                fr.close();

                StringTokenizer tok = new StringTokenizer(sbInput.toString(), "\n", false);
                String currLine = null;
                boolean isV4 = false;
                int line=0;
                while (tok.hasMoreTokens()) {
                    line++;
                    if( currLine == null ) {
                        currLine = tok.nextToken().trim();
                        isV4 = !currLine.startsWith("<?xml");
                    } else
                        currLine = tok.nextToken().trim();
                    if( !isV4 && currLine.startsWith("revision")) {
                        return currLine.substring(currLine.indexOf('"')+1, currLine.lastIndexOf('"'));
                    } else if (isV4 && line == 3) {
                        return currLine.trim();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "unknown";
    }

}
