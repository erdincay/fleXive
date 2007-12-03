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
package com.flexive.shared.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree-paths returned by queries
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface FxPaths {

    /**
     * This object represents a single path, eg '/Images/myFolder/myFile.txt'
     */
    public interface Path {

        /**
         * Returns all items of the path in the correct order.
         * <p />
         * The path '/Images/myFolder/myFile.txt' will return the items 
         * Images, myFolder and myFile.txt
         *
         * @return all items of the path 
         */
        public ArrayList<Item> getItems();

        /**
         * Getter for the path caption, eg '/Images/myFolder/myFile.txt'.
         * 
         * @return the path caption
         */
        public String getCaption();
        
    }

    /**
     * This object represebts a item within a path, eg 'myFolder' in the path
     * '/Images/myFolder/myFile.txt'.
     */
    public interface Item {

        /**
         * Getter for the caption of the item.
         *
         * @return the caption
         */
        public String getCaption();

        /**
         * Gerrer for the node id of the item.
         *
         * @return the node id
         */
        public long getNodeId();

        /**
         * Getter for the reference id of the item.
         *
         * @return the refrence id
         */
        public long getReferenceId();

        /**
         * Returns the referenced content type id of the item, or -1 if the reference is not set.
         *
         * @return the referenced content type id, or -1
         */
        public long getContentTypeId();
    }

    /**
     * Returns all paths.
     *
     * @return the paths
     */
    public List<Path> getPaths();
}
