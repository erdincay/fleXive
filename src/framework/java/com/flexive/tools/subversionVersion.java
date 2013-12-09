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
package com.flexive.tools;

import java.io.IOException;
import java.io.InputStream;

/**
 * Retrieve the current checked out subversion version for flexive
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class subversionVersion {
    private static final String REVISION = "Revision: ";

    public static void main(String[] args) {
        System.out.println(getVersion());
    }

    public static String getVersion() {
        final ProcessBuilder pb = new ProcessBuilder();

        pb.redirectErrorStream(true);
        pb.command("svn", "info");

        try {
            final Process process = pb.start();
            final InputStream in = process.getInputStream();

            final byte[] buf = new byte[1024];
            final StringBuilder responseBuilder = new StringBuilder();
            int read;
            while ((read = in.read(buf)) != -1) {
                responseBuilder.append(new String(buf, 0, read));
            }

            final String[] response = responseBuilder.toString().split("\n");
            for (String line : response) {
                if (line.startsWith(REVISION)) {
                    // parse numeric revision number
                    return Integer.valueOf(line.substring(REVISION.length())).toString();
                }
            }

            System.err.println("No revision number found in output of \"svn info\"");
            return "unknown";
        } catch (IOException e) {
            System.err.println("Failed to determine working copy revision: " + e.getMessage());
            return "unknown";
        }
    }

}
