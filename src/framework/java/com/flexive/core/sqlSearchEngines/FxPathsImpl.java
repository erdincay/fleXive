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
package com.flexive.core.sqlSearchEngines;

import com.flexive.shared.search.FxPaths;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Found tree paths
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxPathsImpl implements Serializable, FxPaths {
    private List<Path> paths;
    private static final long serialVersionUID = -810354430866830163L;

    public static class PathImpl implements Serializable, Path {
        private static final long serialVersionUID = -4533408073950150665L;
        private ArrayList<Item> items;
        private String caption = "";

        /**
         * Constructor.
         *
         * @param encoded the encoded path
         */
        public PathImpl(String encoded) {
            String split[] = encoded.substring(1).split("/");
            items = new ArrayList<Item>(split.length);
            for (String item : split) {
                ItemImpl aItem = new ItemImpl(item);
                items.add(aItem);
                caption += "/" + aItem.getCaption();
            }
        }

        /**
         * {@inheritDoc} *
         */
        public ArrayList<Item> getItems() {
            return items;
        }

        /**
         * {@inheritDoc} *
         */
        public String getCaption() {
            return caption;
        }
    }

    public static class ItemImpl implements Serializable, Item {
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
        private ItemImpl(String encodedPath) {
            String split[] = encodedPath.split(":");
            caption = split[0];
            nodeId = Long.valueOf(split[1]);
            referenceId = Long.valueOf(split[2]);
            contentTypeId = Long.valueOf(split[3]);
        }

        /**
         * {@inheritDoc} *
         */
        public String getCaption() {
            return caption;
        }

        /**
         * {@inheritDoc} *
         */
        public long getNodeId() {
            return nodeId;
        }

        /**
         * {@inheritDoc} *
         */
        public long getReferenceId() {
            return referenceId;
        }

        /**
         * {@inheritDoc} *
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
    public FxPathsImpl(String encoded) {
        if (encoded == null || encoded.trim().length() == 0) {
            paths = new ArrayList<Path>(0);
        } else {
            encoded = encoded.trim();
            String _paths[] = encoded.split("\n");
            paths = new ArrayList<Path>(_paths.length);
            for (String _path : _paths) {
                try {
                    paths.add(new PathImpl(_path));
                } catch (Throwable t) {
                    // skip the element
                    t.printStackTrace();
                }
            }
        }
    }

    /**
     * {@inheritDoc} *
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
        String result = "";
        for (Path path : paths) {
            if (result.length() > 0) result += "\n";
            result += path.getCaption();
        }
        return result;
    }
}
