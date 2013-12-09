/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.shared.search;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tree-paths returned by queries
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxPaths implements Serializable {
    private static final long serialVersionUID = -810354430866830163L;
    private final List<Path> paths;

    /**
     * This object represents a single path, eg '/Images/myFolder/myFile.txt'
     */
    public static class Path implements Serializable {
        private static final long serialVersionUID = -4533408073950150665L;
        private List<Item> items;
        private String caption = "";

        /**
         * Constructor.
         *
         * @param encoded the encoded path
         */
        public Path(String encoded) {
            String split[] = encoded.substring(1).split("/");
            items = new ArrayList<Item>(split.length);
            for (String item : split) {
                if (StringUtils.isBlank(item)) {
                    continue;
                }
                final Item aItem = new Item(item);
                items.add(aItem);
                caption += "/" + aItem.getCaption();
            }
        }

        /**
         * Returns all items of the path in the correct order.
         * <p />
         * The path '/Images/myFolder/myFile.txt' will return the items 
         * Images, myFolder and myFile.txt
         *
         * @return all items of the path 
         */
        public List<Item> getItems() {
            return items;
        }

        /**
         * Getter for the path caption, eg '/Images/myFolder/myFile.txt'.
         * 
         * @return the path caption
         */
        public String getCaption() {
            return caption;
        }
    }

    /**
     * This object represebts a item within a path, eg 'myFolder' in the path
     * '/Images/myFolder/myFile.txt'.
     */
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1498578471619916869L;
        private String caption;
        private long nodeId;
        private long referenceId;
        private long contentTypeId;

        /**
         * Constructor.
         *
         * @param encodedPath the encoded path, item informations are separated by a ':'
         */
        private Item(String encodedPath) {
            String split[] = encodedPath.split(":");
            caption = split[0];
            nodeId = Long.parseLong(split[1]);
            referenceId = Long.parseLong(split[2]);
            contentTypeId = Long.parseLong(split[3]);
        }

        /**
         * Getter for the caption of the item.
         *
         * @return the caption
         */
        public String getCaption() {
            return caption;
        }

        /**
         * Gerrer for the node id of the item.
         *
         * @return the node id
         */
        public long getNodeId() {
            return nodeId;
        }

        /**
         * Getter for the reference id of the item.
         *
         * @return the refrence id
         */
        public long getReferenceId() {
            return referenceId;
        }

        /**
         * Returns the referenced content type id of the item, or -1 if the reference is not set.
         *
         * @return the referenced content type id, or -1
         */
        public long getContentTypeId() {
            return contentTypeId;
        }
    }


    /**
     * Decodes the given DB string into a FxPaths object.
     * <p/>
     * The encoded paths are separated by a newline ('\n').<br>
     * The items within a path are separated by a '/' character.<br>
     * The informations within the a path item are separated by a ':' character
     *
     * @param encoded the encoded string
     */
    public FxPaths(String encoded) {
        if (StringUtils.isBlank(encoded)) {
            paths = new ArrayList<Path>(0);
        } else {
            final String[] pathArray = encoded.trim().split("\n");
            this.paths = new ArrayList<Path>(pathArray.length);
            for (String path : pathArray) {
                this.paths.add(new Path(path));
            }
        }
    }

    /**
     * Returns all paths.
     *
     * @return the paths
     */
    public List<Path> getPaths() {
        return paths;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public String toString() {
        if (paths == null) return "";
        final StringBuilder result = new StringBuilder();
        for (Path path : paths) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(path.getCaption());
        }
        return result.toString();
    }
}
