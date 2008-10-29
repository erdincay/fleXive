package com.flexive.faces.components.tree;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.faces.javascript.FxJavascriptUtils;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import org.apache.commons.lang.StringUtils;

/**
 * <p>The flexive tree navigation component. Renders part of the topic tree as
 * nested {@code <ul>} elements. These can be styled via CSS to resemble
 * menus or even pop-up menus, or enhanced with JavaScript using YahooUI components.</p>
 * <p/>
 * <p>
 * The part of the topic tree to be rendered can be controlled through the
 * {@code nodeId}, {@code path} and {@code depth} attributes. The tree mode is set through
 * the {@code mode} attribute ("edit" or "live" (default)).
 * </p>
 * <p/>
 * <p>
 * The {@code output} attribute modifies the visual style of the navigation:
 * <dl>
 * <dt><strong>default</strong></dt>
 * <dd>Only marker style classes are added to the elements, for example to indicate an active menu item.</dd>
 * <dt><strong>csspopup</strong></dt>
 * <dd>Renders a menu that uses popups for nested elements (only works in current browsers, like Firefox 2+ or IE7+)</dd>
 * <dt><strong>yui</strong></dt>
 * <dd>Adds style classes for the YahooUI menu component. If JavaScript is enabled, YUI creates a
 * Menu component from the markup. If JavaScript is disabled, the basic markup version is shown.</dd>
 * </dl>
 * </p>
 * <p/>
 * <h3>Facets</h3>
 * <dl>
 * <dt><strong>item</strong></dt>
 * <dd>
 * The markup of an individual item body. The tree node is exposed under the variable set
 * in the {@code var} attribute and is of type {@link com.flexive.shared.tree.FxTreeNode}.
 * For example:
 * <pre> {@code <f:facet name="item">
 *     <a href="/content/#{node.id}">#{node.label}</a>
 * </f:facet>}</pre>
 * </dd>
 * </dl>
 * <p/>
 * <h3>Examples</h3>
 * <h4>Rendering the topic tree as a menu bar</h4>
 * <pre> {@code <fx:navigation var="node" output="yui" path="/" depth="5">
 *    <f:facet name="item">
 *        <a href="javascript:alert('You clicked on node with ID=#{node.id} and path=#{node.path}')">#{node.label}</a>
 *    </f:facet>
 * </fx:navigation>}</pre>
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class TreeNavigation extends UIOutput {
    private static enum OutputType {
        DEFAULT, CSSPOPUP, YUI
    }

    /**
     * CSS class to be set for selected items (i.e. in current node path)
     */
    private static final String CSS_SELECTED = "selectedItem";
    /**
     * Container style class for the enclosing UL element of a tree
     */
    private static final String CSS_TREE = "flexiveNavigation";
    /**
     * Container for the CSS popup menu version
     */
    private static final String CSS_POPUP_TREE = "flexiveMenuBar";

    private String var;
    private String mode;
    private String output;
    private Long nodeId;
    private String path;
    private Integer depth;
    private String menuOptions;

    public TreeNavigation() {
        setRendererType(null);
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        super.encodeBegin(context);
        if (getFacet("item") == null) {
            throw new IllegalArgumentException("Required facet \"item\" not specified.");
        }
        if (getVar() == null) {
            throw new IllegalArgumentException("Required attribute \"var\" not specified.");
        }
        final FxTreeMode mode;
        try {
            mode = FxTreeMode.valueOf(StringUtils.capitalize(getMode().toLowerCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value for \"mode\" attribute: " + getMode(), e);
        }
        try {
            final long nodeId;
            if (isNotBlank(getPath())) {
                nodeId = EJBLookup.getTreeEngine().getIdByPath(mode, getPath());
            } else if (getNodeId() != -1) {
                nodeId = getNodeId();
            } else {
                nodeId = FxTreeNode.ROOT_NODE;
            }
            createRenderer(context).renderTree(
                    EJBLookup.getTreeEngine().getTree(mode, nodeId, getDepth())
            );
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    private Renderer createRenderer(FacesContext context) {
        switch (getOutputType()) {
            case YUI:
                return new YUIRenderer(context);
            case CSSPOPUP:
                return new CssPopupRenderer(context);
            case DEFAULT:
                return new Renderer(context);
            default:
                throw new IllegalArgumentException("Invalid output type: " + getOutputType());
        }
    }

    /**
     * Submenu list renderer
     */
    private class Renderer {
        protected final ResponseWriter out;
        protected final FacesContext context;

        public Renderer(FacesContext context) {
            this.context = context;
            this.out = context.getResponseWriter();
        }

        public void renderTree(FxTreeNode node) throws IOException {
            renderSubtree(node);
        }

        protected void renderSubtree(FxTreeNode node) throws IOException {
            out.startElement("ul", null);
            out.writeAttribute("class", CSS_TREE, null);
            for (FxTreeNode child : node.getChildren()) {
                out.startElement("li", null);
                out.writeAttribute("class", getElementClass(child), null);

                renderMenuItem(child);

                // render nested items
                if (!child.getChildren().isEmpty()) {
                    renderSubtree(child);
                }
                out.endElement("li");
            }
            out.endElement("ul");
        }

        protected void renderMenuItem(FxTreeNode node) throws IOException {
            // provide "var" variable in request
            final Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
            final Object oldValue = requestMap.put(getVar(), node);

            // encode item facet
            getFacet("item").encodeAll(context);

            // remove "var" variable
            if (oldValue != null) {
                requestMap.put(getVar(), oldValue);
            } else {
                requestMap.remove(getVar());
            }
        }

        /**
         * Return the style class for the given tree node.
         *
         * @param node the tree node
         * @return the style class for the given tree node.
         */
        protected String getElementClass(FxTreeNode node) {
            return isSelected(node) ? CSS_SELECTED : "";
        }
    }

    private class CssPopupRenderer extends Renderer {
        private CssPopupRenderer(FacesContext context) {
            super(context);
        }

        @Override
        public void renderTree(FxTreeNode node) throws IOException {
            out.startElement("div", null);
            out.writeAttribute("class", CSS_POPUP_TREE, null);

            super.renderTree(node);

            out.endElement("div");
        }
    }

    /**
     * Submenu list renderer for YUI output
     */
    private class YUIRenderer extends Renderer {
        private int subTreeCounter;

        private YUIRenderer(FacesContext context) {
            super(context);
        }

        @Override
        public void renderTree(FxTreeNode node) throws IOException {
            // write container
            out.startElement("div", null);
            final String id = getClientId(FacesContext.getCurrentInstance());
            out.writeAttribute("id", id, null);
            out.writeAttribute("class", "yuimenubar yuimenubarnav", null);

            super.renderTree(node);

            out.endElement("div");

            // init YUI menu
            FxJavascriptUtils.beginJavascript(out);
            FxJavascriptUtils.writeYahooRequires(out, "menu");
            FxJavascriptUtils.onYahooLoaded(out,
                    "function() {\n"
                            + "var oMenu = new YAHOO.widget.MenuBar(\"" + id + "\", { " + getMenuOptions() + "});\n"
                            + "oMenu.render();\n"
                            + "}\n"
            );
            FxJavascriptUtils.endJavascript(out);
        }

        @Override
        protected void renderSubtree(FxTreeNode node) throws IOException {
            final int cnt = subTreeCounter++;
            if (cnt > 0) {
                // write container except for top level
                out.startElement("div", null);
                out.writeAttribute("class", "yuimenu", null);
            }

            out.startElement("div", null);
            out.writeAttribute("class", "bd", null);

            // render menu
            super.renderSubtree(node);

            if (cnt > 0) {
                out.endElement("div");
            }
            out.endElement("div");
        }

        @Override
        protected String getElementClass(FxTreeNode node) {
            return super.getElementClass(node) + " yuimenuitem";
        }
    }

    private boolean isSelected(FxTreeNode node) {
        return false;
    }

    public String getMode() {
        if (mode == null) {
            return FxJsfComponentUtils.getStringValue(this, "mode", "live");
        }
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public long getNodeId() {
        if (nodeId == null) {
            return FxJsfComponentUtils.getLongValue(this, "nodeId", -1);
        }
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getPath() {
        if (path == null) {
            return FxJsfComponentUtils.getStringValue(this, "path");
        }
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getDepth() {
        if (depth == null) {
            return FxJsfComponentUtils.getIntegerValue(this, "depth", 1);
        }
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public String getOutput() {
        if (output == null) {
            return FxJsfComponentUtils.getStringValue(this, "output", "default");
        }
        return output;
    }

    private OutputType getOutputType() {
        try {
            return OutputType.valueOf(getOutput().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid output mode: " + getOutput(), e);
        }
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getMenuOptions() {
        if (menuOptions == null) {
            return FxJsfComponentUtils.getStringValue(this, "menuOptions",
                    "autosubmenudisplay: true, hidedelay: 750, lazyload: true");
        }
        return menuOptions;
    }

    public void setMenuOptions(String menuOptions) {
        this.menuOptions = menuOptions;
    }

    public String getVar() {
        if (var == null) {
            return FxJsfComponentUtils.getStringValue(this, "var");
        }
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }

    @Override
    public Object saveState(FacesContext context) {
        final Object[] state = new Object[8];
        state[0] = super.saveState(context);
        state[1] = mode;
        state[2] = nodeId;
        state[3] = path;
        state[4] = depth;
        state[5] = output;
        state[6] = menuOptions;
        state[7] = var;
        return state;
    }

    @Override
    public void restoreState(FacesContext context, Object oState) {
        final Object[] state = (Object[]) oState;
        super.restoreState(context, state[0]);
        this.mode = (String) state[1];
        this.nodeId = (Long) state[2];
        this.path = (String) state[3];
        this.depth = (Integer) state[4];
        this.output = (String) state[5];
        this.menuOptions = (String) state[6];
        this.var = (String) state[7];
    }
}
