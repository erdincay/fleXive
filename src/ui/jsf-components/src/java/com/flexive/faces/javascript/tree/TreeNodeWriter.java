/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.faces.javascript.tree;

import com.flexive.faces.javascript.RelativeUriMapper;
import com.flexive.war.JsonWriter;

import java.io.IOException;
import java.io.Writer;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A basic writer for tree nodes for the Dojo tree (V3).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class TreeNodeWriter {
    private final static Map<String, Object> EMPTY_MAP = new HashMap<String, Object>();

    /**
     * A single tree node.
     */
    public static class Node {
        /**
         * Default class for nodes without children (leaves)
         */
        public static final String TITLE_CLASS_LEAF = "treeNodeV3Child";
        /**
         * Default class for nodes with children
         */
        public static final String TITLE_CLASS_NODE = "treeNodeV3Node";

        private String id;
        private String title;
        private String titleClass;
        private String icon;
        private String docType;
        private String link;
        private Map<String, ?> properties;

        /**
         * Create a node without style infos. Formatting is based on the CSS class(es)
         * corresponding to docType.
         *
         * @param title      the node title
         * @param docType    the document type
         * @param properties optional properties
         */
        public Node(String id, String title, String docType, Map<String, ?> properties) {
            this.id = id;
            this.title = title;
            this.docType = docType;
            this.properties = properties;
        }

        public Node(String id, String title, String titleClass, String icon, Map<String, ?> properties) {
            this.id = id;
            this.title = title;
            this.titleClass = titleClass;
            this.icon = icon;
            this.properties = properties != null ? properties : EMPTY_MAP;
        }

        public Node(String id, String title, String titleClass, String icon) {
            this(id, title, titleClass != null ? titleClass : TITLE_CLASS_LEAF, icon, EMPTY_MAP);
        }

        public Node(String id, String title, String titleClass, String icon, String link) {
            this(id, title, titleClass != null ? titleClass : TITLE_CLASS_LEAF, icon, EMPTY_MAP);
            this.link = link;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getTitleClass() {
            return titleClass;
        }

        public String getIcon() {
            return icon;
        }

        public String getDocType() {
            return docType;
        }

        public String getLink() {
            return link;
        }
    }

    /**
     * Formats the label of a given tree node, e.g. adds
     * HTML markup for formatting.
     */
    public static interface NodeFormatter extends Serializable {
        String format(Node node, RelativeUriMapper uriMapper);
    }

    /**
     * Plain node formatter. Icons may be rendered using the
     * TreeDocIconExtension and specifying matching CSS classes.
     * Preferred to FORMAT_ADMINTREE if inline node editing is required.
     */
    public static final NodeFormatter FORMAT_PLAIN = new NodeFormatter() {
        private static final long serialVersionUID = -6205709223249640378L;

        /** {@inheritDoc} */
        public String format(Node node, RelativeUriMapper uriMapper) {
            return node.title;
        }
    };

    /**
     * Format the node using the default style for the admin backend trees.
     */
    public static final NodeFormatter FORMAT_ADMINTREE = new NodeFormatter() {
        private static final long serialVersionUID = 7271762591207000704L;
        private static final String ICON_CLASS = "treeNodeV3Icon";
        private static final String ICON_PATH = "adm/images/tree/";

        /** {@inheritDoc} */
        public String format(Node node, RelativeUriMapper uriMapper) {
            final String title = "<div class=\"" + node.titleClass + "\">" + node.title + "</div>";
            if (node.icon == null) {
                return title;
            } else {
                final String icon = ICON_PATH + node.icon + ".png";
                return "<img src=\"" + uriMapper.getAbsoluteUri(icon) + "\" class=\"" + ICON_CLASS + "\">" + title;
            }
        }
    };

    public static final NodeFormatter FORMAT_CONTENTTREE = new NodeFormatter() {
        private static final long serialVersionUID = -7586232861308540856L;

        /** {@inheritDoc} */
        public String format(Node node, RelativeUriMapper uriMapper) {
            final StringBuilder style = new StringBuilder();
            if ((Boolean) node.properties.get("isDirty")) {
                style.append("dirty ");
            }
            if (!(Boolean) node.properties.get("mayEdit")) {
                style.append("readonly ");
            }
            if (style.length() > 0) {
                // add dirty node style
                return "<span class=\"" + style + "\">" + node.title + "</span>";
            } else {
                return node.title;
            }
        }
    };


    private final JsonWriter out;
    private final NodeFormatter formatter;
    private final RelativeUriMapper uriMapper;

    /**
     * Create a new TreeNodeWriter using an existing JsonWriter.
     *
     * @param out       the JsonWriter instance to be used
     * @param formatter the default formatter to be used
     * @throws IOException if the output could not be written
     */
    public TreeNodeWriter(JsonWriter out, RelativeUriMapper uriMapper, NodeFormatter formatter) throws IOException {
        this.out = out;
        this.formatter = formatter;
        this.uriMapper = uriMapper;
        out.startArray();
    }

    /**
     * Create a new TreeNodeWriter for the given output writer.
     *
     * @param out       an output writer.
     * @param formatter the default node formatter to be used
     * @throws IOException if the output could not be written
     */
    public TreeNodeWriter(Writer out, RelativeUriMapper uriMapper, NodeFormatter formatter) throws IOException {
        this(new JsonWriter(out), uriMapper, formatter);
    }

    /**
     * Closes the tree structure. No nodes can be appended to the generated tree
     * afterwards.
     *
     * @throws IOException if the output could not be written
     */
    public void finishResponse() throws IOException {
        out.closeArray();
    }

    /**
     * Start a new node without closing it. Additional parameters
     * and nested child nodes may be appended in subsequent calls. The node must be
     * closed with a call to {@link TreeNodeWriter#closeNode()}.
     *
     * @param node the node to be written
     * @throws IOException if the output could not be written
     */
    public void startNode(Node node) throws IOException {
        out.startMap();
        out.writeAttribute("title", formatter.format(node, uriMapper));
        if (node.docType != null) {
            out.writeAttribute("nodeDocType", node.docType);
        }
        if (node.link != null) {
            out.writeAttribute("link", node.link);
        }
        for (Entry<String, ?> entry : node.properties.entrySet()) {
            out.writeAttribute(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Close a node previously opened with
     * {@link TreeNodeWriter#startNode(TreeNodeWriter.Node)}.
     *
     * @throws IOException if the output could not be written
     */
    public void closeNode() throws IOException {
        out.closeMap();
    }

    /**
     * Render a leaf node.
     *
     * @param node the node to be rendered
     * @throws IOException if the output could not be written
     */
    public void writeNode(Node node) throws IOException {
        startNode(node);
        closeNode();
    }

    /**
     * Prepare the current node (created with
     * {@link TreeNodeWriter#startNode(TreeNodeWriter.Node)})
     * for appending of nested child nodes.
     *
     * @throws IOException if the output could not be written
     */
    public void startChildren() throws IOException {
        out.startAttribute("children");
        out.startArray();
    }


    /**
     * Close the child nodes array previously created with
     * {@link TreeNodeWriter#startChildren()}.
     *
     * @throws IOException if the output could not be written
     */
	public void closeChildren() throws IOException {
		out.closeArray();
	}
	
}
