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
package com.flexive.faces.javascript.tree;

import com.flexive.faces.javascript.RelativeUriMapper;
import com.flexive.war.JsonWriter;
import net.java.dev.weblets.FacesWebletUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.context.FacesContext;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
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
    private static final Log LOG = LogFactory.getLog(TreeNodeWriter.class);
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

        /**
         * Icons starting with WEBLET_ICON will be rendered using the given resource from the weblet
         */
        private static final String WEBLET_ICON = "weblet:";

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
         * @param id         the node id
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

        /**
         * Format a weblet icon resource
         *
         * @param weblet   name of the weblet
         * @param iconPath path of the icon within the plugins weblet directory
         * @return formatted weblet icon resource
         */
        public static String formatWebletIcon(String weblet, String iconPath) {
            return TreeNodeWriter.Node.WEBLET_ICON + weblet + ":" + iconPath;
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
            return node.getTitle();
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
            final String title = "<div class=\"" + node.getTitleClass() + "\">" + node.getTitle() + "</div>";
            if (node.getIcon() == null) {
                return title;
            } else {
                if (node.getIcon().startsWith(Node.WEBLET_ICON)) {
                    //render the icon from a weblet resource
                    String[] data = node.getIcon().split(":");
                    if (data.length == 3) {
                        final String uri = FacesWebletUtils.getURL(FacesContext.getCurrentInstance(), data[1], data[2]);
                        return "<img src=\"" + uri + "\" class=\"" + ICON_CLASS + "\">" + title;
                    } else {
                        LOG.error("Invalid weblet icon descriptor [" + node.getIcon() + "]! Expected format: " + Node.WEBLET_ICON + ":weblet:path_to_icon");
                        return title;
                    }
                } else {
                    final String icon = ICON_PATH + node.getIcon() + ".png";
                    return "<img src=\"" + uriMapper.getAbsoluteUri(icon) + "\" class=\"" + ICON_CLASS + "\">" + title;
                }
            }
        }
    };

    public static final NodeFormatter FORMAT_CONTENTTREE = new NodeFormatter() {
        private static final long serialVersionUID = -7586232861308540856L;

        /** {@inheritDoc} */
        public String format(Node node, RelativeUriMapper uriMapper) {
            final StringBuilder style = new StringBuilder();
            if (node.properties.containsKey("isDirty") && (Boolean) node.properties.get("isDirty")) {
                style.append("dirty ");
            }
            if (node.properties.containsKey("mayEdit") && !(Boolean) node.properties.get("mayEdit")) {
                style.append("readonly ");
            }
            if (style.length() > 0) {
                // add dirty node style
                return "<span class=\"" + style + "\">" + node.getTitle() + "</span>";
            } else {
                return node.getTitle();
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
     * @param uriMapper URI Mapper to use
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
     * @param uriMapper URI Mapper to use
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
        if (node.getDocType() != null) {
            out.writeAttribute("nodeDocType", node.getDocType());
        }
        if (node.getLink() != null) {
            out.writeAttribute("link", node.getLink());
        }
        for (Entry<String, ?> entry : node.properties.entrySet()) {
            out.writeAttribute(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Close a node previously opened with
     * {@link TreeNodeWriter#startNode(com.flexive.faces.javascript.tree.TreeNodeWriter.Node)}.
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
     * {@link TreeNodeWriter#startNode(com.flexive.faces.javascript.tree.TreeNodeWriter.Node)}
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
