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
package com.flexive.war.beans.admin.content;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.TreeEngine;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.tree.FxTreeNodeEdit;
import org.apache.commons.lang.StringUtils;

import java.util.Formatter;

/**
 * Test beans to generate randomized contents for testing.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ContentGeneratorBean {
    private long type = -1;
    private int count = 100;
    private int maxMultiplicity = 2;
    private String treeFolder = "/Test Data";

    public String create() {
        try {
            final ContentEngine contentEngine = EJBLookup.getContentEngine();
            final TreeEngine treeEngine = EJBLookup.getTreeEngine();
            final FxTreeNode folder;
            if (StringUtils.isNotBlank(treeFolder)) {
                treeFolder = (treeFolder.startsWith("/") ? treeFolder : "/" + treeFolder).trim();
                final long[] nodes = treeEngine.createNodes(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, 0, treeFolder);
                folder = treeEngine.getNode(FxTreeMode.Edit, nodes[nodes.length - 1]);
            } else {
                folder = null;
            }
            final long startTime = System.currentTimeMillis();
            long initializeTime = 0;
            long randomizeTime = 0;
            long saveTime = 0;
            long treeTime = 0;
            for (int i = 0; i < count; i++) {
                long start = System.currentTimeMillis();
                final FxContent co = contentEngine.initialize(type);
                initializeTime += System.currentTimeMillis() - start;
                start = System.currentTimeMillis();
                co.randomize(maxMultiplicity);
                randomizeTime += System.currentTimeMillis() - start;
                start = System.currentTimeMillis();
                final FxPK pk = contentEngine.save(co);
                saveTime += System.currentTimeMillis() - start;
                if (folder != null) {
                    start = System.currentTimeMillis();
                    treeEngine.save(FxTreeNodeEdit.createNewChildNode(folder).setReference(pk));
                    treeTime += System.currentTimeMillis() - start;
                }
            }
            final long totalTime = System.currentTimeMillis() - startTime;
            new FxFacesMsgInfo("Content.nfo.testData.created", count,
                    totalTime,
                    initializeTime, 100 * initializeTime / totalTime,
                    randomizeTime, 100 * randomizeTime / totalTime,
                    saveTime, 100 * saveTime / totalTime,
                    new Formatter().format("%.2f", count / (double) totalTime * 1000),
                    treeTime, 100 * treeTime / totalTime).addToContext();
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return "contentTestData";
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public int getMaxMultiplicity() {
        return maxMultiplicity;
    }

    public void setMaxMultiplicity(int maxMultiplicity) {
        this.maxMultiplicity = maxMultiplicity;
    }

    public String getTreeFolder() {
        return treeFolder;
    }

    public void setTreeFolder(String treeFolder) {
        this.treeFolder = treeFolder;
    }
}
