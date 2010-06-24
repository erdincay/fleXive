package com.flexive.faces.components.tree;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.faces.javascript.FxJavascriptUtils;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import org.apache.commons.lang.StringUtils;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.NamingContainer;
import javax.faces.context.FacesContext;
import static javax.faces.context.FacesContext.getCurrentInstance;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;
import java.io.IOException;
import java.util.*;

/**
 * <p>The flexive tree navigation component. Renders part of the topic tree as
 * nested {@code <ul>} elements. These can be styled via CSS to resemble
 * menus or even pop-up menus, or enhanced with JavaScript using YahooUI components.</p>
 * <p>
 * The part of the topic tree to be rendered can be controlled through the
 * {@code nodeId}, {@code path} and {@code depth} attributes. The tree mode is set through
 * the {@code mode} attribute ("edit" or "live" (default)).
 * </p>
 * <p>
 * When either {@code selectedNodeId} or {@code selectedPath} is specified, the nodes leading from the
 * root node to the given node are marked with the CSS style class "{@code selected}".
 * </p>
 * <p>
 * The {@code output} attribute modifies the visual style of the navigation:
 * <dl>
 * <dt><strong>default</strong></dt>
 * <dd>Only marker style classes are added to the elements, for example to indicate an active menu item.</dd>
 * <dt><strong>csspopup</strong></dt>
 * <dd>Renders a menu that uses popups for nested elements (only works in current browsers, like Firefox 2+ or IE7+)</dd>
 * <dt><strong>yui</strong></dt>
 * <dd>Adds style classes and javascript for the YahooUI menu component. If JavaScript is enabled, YUI creates a
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
 * Note that even if it contains only HTML markup, it must be embedded in a JSF component
 * (e.g. {@code ui:fragment}) for proper processing.
 *
 * For example:
 * <pre> {@code <f:facet name="item">
 *   <ui:fragment>
 *     <a href="/content/#{node.id}">#{node.label}</a>
 *   </ui:fragment>
 * </f:facet>}</pre>
 * </dd>
 * </dl>
 * <p/>
 * <h3>Examples</h3>
 * <h4>Rendering the topic tree as a menu bar</h4>
 * <pre> {@code <fx:navigation var="node" output="yui" path="/" depth="5">
 *    <f:facet name="item">
 *      <ui:fragment>
 *        <a href="javascript:alert('You clicked on node with ID=#{node.id} and path=#{node.path}')">#{node.label}</a>
 *      </ui:fragment>
 *    </f:facet>
 * </fx:navigation>}</pre>
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 * @since 3.1
 */
public class TreeNavigation extends UIOutput implements NamingContainer {
    public static final String COMPONENT_TYPE = "flexive.TreeNavigation";

    private static final Log LOG = LogFactory.getLog(TreeNavigation.class);

    private static enum OutputType {
        DEFAULT, CSSPOPUP, YUI
    }

    /**
     * CSS class to be set for selected items (i.e. in current node path)
     */
    private static final String CSS_SELECTED = "selected";
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
    private Long selectedNodeId;
    private String selectedPath;
    private Integer depth;
    private String menuOptions;
    private String treeOptions;
    private Long baseTypeId;
    private Boolean includeRoot;
    private Boolean includeFolders;

    private transient String selectedPathCache;
    private transient FxTreeNode treeCache;
    private transient long treeCacheBaseTypeId;

    public TreeNavigation() {
        setRendererType(null);
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public String getClientId(FacesContext context) {
        return super.getClientId(context) + NamingContainer.SEPARATOR_CHAR +
                (getVarValue(context) != null ? getVarValue(context).getId() : 0);
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        final FxTreeNode tree = getTree();
        if (tree != null) {
            createRenderer(context).renderTree(tree);
        }
    }

    @Override
    public void processDecodes(FacesContext context) {
        final FxTreeNode tree = getTree();
        if (tree != null) {
            try {
                new ItemPhaseRenderer(context, PhaseId.APPLY_REQUEST_VALUES).renderTree(tree);
            } catch (IOException e) {
                LOG.warn(e.getMessage(), e);    // shouldn't happen
            }
        }
    }

    @Override
    public void processUpdates(FacesContext context) {
        final FxTreeNode tree = getTree();
        if (tree != null) {
            try {
                new ItemPhaseRenderer(context, PhaseId.UPDATE_MODEL_VALUES).renderTree(tree);
            } catch (IOException e) {
                LOG.warn(e.getMessage(), e);    // shouldn't happen
            }
        }
    }

    @Override
    public void processValidators(FacesContext context) {
        final FxTreeNode tree = getTree();
        if (tree != null) {
            try {
                new ItemPhaseRenderer(context, PhaseId.PROCESS_VALIDATIONS).renderTree(tree);
            } catch (IOException e) {
                LOG.warn(e.getMessage(), e);    // shouldn't happen
            }
        }
    }

    @Override
    public void queueEvent(FacesEvent event) {
        super.queueEvent(new NodeEvent(this, event, getVarValue(getCurrentInstance())));
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        if (event instanceof NodeEvent) {
            final NodeEvent nodeEvent = (NodeEvent) event;
            final FacesContext ctx = getCurrentInstance();
            final Object oldValue = provideVar(ctx,nodeEvent.getNode());
            try {
                performBroadcast(ctx, nodeEvent.getTarget());
            } finally {
                removeVar(ctx, oldValue);
            }
        } else {
            super.broadcast(event);
        }
    }

    protected void performBroadcast(FacesContext ctx, FacesEvent event) {
        event.getComponent().broadcast(event);
    }


    /**
     * Return the entire tree that should be rendered.
     *
     * @return  the entire tree that should be rendered.
     */
    private FxTreeNode getTree() {
        final FxTreeMode mode = getTreeMode();
        try {
            final long nodeId = getNodeId(mode);
            if (nodeId == -1) {
                return null;
            }
            if (treeCache != null && treeCache.getId() == nodeId && treeCacheBaseTypeId == getBaseTypeId()
                    && !FxContext.get().getTreeWasModified() && treeCache.getMode() == mode) {
                return treeCache;
            }
            treeCache = EJBLookup.getTreeEngine().getTree(mode, nodeId, getDepth());
            treeCacheBaseTypeId = getBaseTypeId();

            // remove all filtered type IDs when a filter was specified
            if (getBaseTypeId() != -1) {
                final FxEnvironment environment = CacheAdmin.getEnvironment();
                final Set<Long> validTypeIds = new HashSet<Long>();
                validTypeIds.addAll(
                        FxSharedUtils.getSelectableObjectIdList(
                            environment.getType(getBaseTypeId()).getDerivedTypes(true, true)
                        )
                );
                if (isIncludeFolders()) {
                    validTypeIds.addAll(
                            FxSharedUtils.getSelectableObjectIdList(
                                    environment.getType(FxType.FOLDER).getDerivedTypes(true, true)
                            )
                    );
                }
                removeInvalidChildren(treeCache, validTypeIds);
            }

            return treeCache;
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    protected long getNodeId(FxTreeMode mode) throws FxApplicationException {
        if (isNotBlank(getPath())) {
            return EJBLookup.getTreeEngine().getIdByPath(mode, getPath());
        } else if (getNodeId() != -1) {
            return getNodeId();
        } else {
            return FxTreeNode.ROOT_NODE;
        }
    }

    /**
     * Remove all child nodes of {@code node} whose type is not in {@code validTypeIds}.
     *
     * @param node          the parent node
     * @param validTypeIds  the valid type IDs
     */
    private void removeInvalidChildren(FxTreeNode node, Set<Long> validTypeIds) {
        final Iterator<FxTreeNode> iterator = node.getChildren().iterator();
        while (iterator.hasNext()) {
            final FxTreeNode child = iterator.next();
            if (!validTypeIds.contains(child.getReferenceTypeId())) {
                iterator.remove();
            }
            if (child.getDirectChildCount() > 0) {
                removeInvalidChildren(child, validTypeIds);
            }
        }
    }

    /**
     * Return the selected tree mode (edit or live).
     *
     * @return  the selected tree mode (edit or live).
     */
    private FxTreeMode getTreeMode() {
        final FxTreeMode mode;
        try {
            mode = FxTreeMode.valueOf(StringUtils.capitalize(getMode().toLowerCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value for \"mode\" attribute: " + getMode(), e);
        }
        return mode;
    }

    /**
     * Create a renderer for the selected output type (plain, YUI, CSS).
     *
     * @param context   the faces context
     * @return  a navigation renderer for the selected output type (plain, YUI, CSS).
     */
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
     * Store the given tree node in the request variable specified by the "var" attribute.
     *
     * @param context   the faces context
     * @param node      the tree node to be stored
     * @return          the old value of the request variable (may be null)
     */
    protected Object provideVar(FacesContext context, FxTreeNode node) {
        return context.getExternalContext().getRequestMap().put(getVar(), node);
    }

    /**
     * Restore the previous value of the request variable specified by "var".
     *
     * @param context   the faces context
     * @param oldValue  the value to be restored
     */
    protected void removeVar(FacesContext context, Object oldValue) {
        if (oldValue != null) {
            context.getExternalContext().getRequestMap().put(getVar(), oldValue);
        } else {
            context.getExternalContext().getRequestMap().remove(getVar());
        }
    }

    /**
     * Return the current value of the request variable specified by the "var" attribute.
     *
     * @param context   the faces context
     * @return  the current value of the request variable specified by the "var" attribute.
     */
    protected FxTreeNode getVarValue(FacesContext context) {
        return (FxTreeNode) context.getExternalContext().getRequestMap().get(getVar());
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
            if (getFacet("item") == null) {
                throw new IllegalArgumentException("Required facet \"item\" not specified.");
            }
            if (getVar() == null) {
                throw new IllegalArgumentException("Required attribute \"var\" not specified.");
            }
        }

        public void renderTree(FxTreeNode node) throws IOException {
            renderSubtree(
                    isIncludeRoot() ? Arrays.asList(node) : node.getChildren()
            );
        }

        protected void renderSubtree(List<FxTreeNode> nodes) throws IOException {
            out.startElement("ul", null);
            out.writeAttribute("class", CSS_TREE, null);

            for (FxTreeNode child : nodes) {
                out.startElement("li", null);
                out.writeAttribute("class", getElementClass(child), null);

                renderMenuItem(child);

                // render nested items
                if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                    renderSubtree(child.getChildren());
                }
                out.endElement("li");
            }
            
            out.endElement("ul");
        }

        protected void renderMenuItem(FxTreeNode node) throws IOException {
            final Object oldValue = provideVar(context, node);
            
            // encode item facet
            final UIComponent item = getFacet("item");
            FxJsfComponentUtils.clearCachedClientIds(item);
            item.encodeAll(context);

            removeVar(context, oldValue);
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

    /**
     * Helper class to encode all the items in JSF phases other than render response
     */
    private class ItemPhaseRenderer extends Renderer {
        private final PhaseId phase;

        private ItemPhaseRenderer(FacesContext context, PhaseId phase) {
            super(context);
            this.phase = phase;
        }

        @Override
        protected void renderSubtree(List<FxTreeNode> nodes) throws IOException {
            for (FxTreeNode child: nodes) {
                final Object oldValue = provideVar(context, child);
                final UIComponent item = getFacet("item");
                FxJsfComponentUtils.clearCachedClientIds(item);
                if (PhaseId.APPLY_REQUEST_VALUES.equals(phase)) {
                    item.processDecodes(context);
                } else if (PhaseId.UPDATE_MODEL_VALUES.equals(phase)) {
                    item.processUpdates(context);
                } else if (PhaseId.PROCESS_VALIDATIONS.equals(phase)) {
                    item.processValidators(context);
                } else {
                    throw new IllegalArgumentException("Invalid phase: " + phase);
                }
                if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                    renderSubtree(child.getChildren());
                }
                removeVar(context, oldValue);
            }
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
            final String id = getClientId(getCurrentInstance());
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
        protected void renderSubtree(List<FxTreeNode> nodes) throws IOException {
            final int cnt = subTreeCounter++;
            if (cnt > 0) {
                // write container except for top level
                out.startElement("div", null);
                out.writeAttribute("class", "yuimenu", null);
            }

            out.startElement("div", null);
            out.writeAttribute("class", "bd", null);

            // render menu
            super.renderSubtree(nodes);

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
        return !StringUtils.isBlank(getSelectedPath())
                && (getSelectedPath().equals(node.getPath())
                || getSelectedPath().startsWith(node.getPath() + "/"));
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

    public String getTreeOptions() {
        if (treeOptions == null) {
            return FxJsfComponentUtils.getStringValue(this, "treeOptions", "");
        }
        return treeOptions;
    }

    public void setTreeOptions(String treeOptions) {
        this.treeOptions = treeOptions;
    }

    public long getBaseTypeId() {
        if (baseTypeId == null) {
            // check baseTypeId attribute
            long id = FxJsfComponentUtils.getLongValue(this, "baseTypeId", -1);
            if (id != -1) {
                return id;
            }
            
            // check baseTypeName attribute
            String name = FxJsfComponentUtils.getStringValue(this, "baseTypeName", null);
            if (name != null) {
                return CacheAdmin.getEnvironment().getType(name).getId();
            }
            return -1;
        }
        return baseTypeId;
    }

    public void setBaseTypeId(long baseTypeId) {
        this.baseTypeId = baseTypeId;
    }

    public void setBaseTypeName(String baseTypeName) {
        this.baseTypeId = CacheAdmin.getEnvironment().getType(baseTypeName).getId();
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

    public Long getSelectedNodeId() {
        if (selectedNodeId == null) {
            return FxJsfComponentUtils.getLongValue(this, "selectedNodeId", -1);
        }
        return selectedNodeId;
    }

    public void setSelectedNodeId(Long selectedNodeId) {
        this.selectedNodeId = selectedNodeId;
    }

    public String getSelectedPath() {
        if (selectedPath == null) {
            if (FxJsfComponentUtils.getStringValue(this, "selectedPath") != null) {
                return FxJsfComponentUtils.getStringValue(this, "selectedPath");
            }
            if (selectedPathCache != null) {
                return selectedPathCache;
            }
            if (getSelectedNodeId() != -1) {
                // resolve path
                try {
                    selectedPathCache = EJBLookup.getTreeEngine().getPathById(getTreeMode(), getSelectedNodeId());
                    return selectedPathCache;
                } catch (FxApplicationException e) {
                    throw e.asRuntimeException();
                }
            }
        }
        return selectedPath;
    }

    public void setSelectedPath(String selectedPath) {
        this.selectedPath = selectedPath;
    }

    public Boolean isIncludeRoot() {
        if (includeRoot == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "includeRoot", Boolean.FALSE);
        }
        return includeRoot;
    }

    public void setIncludeRoot(Boolean includeRoot) {
        this.includeRoot = includeRoot;
    }

    public Boolean isIncludeFolders() {
        if (includeFolders == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "includeFolders", Boolean.FALSE);
        }
        return includeFolders;
    }

    public void setIncludeFolders(Boolean includeFolders) {
        this.includeFolders = includeFolders;
    }

    protected static class NodeEvent extends FacesEvent {
        private static final long serialVersionUID = 5274895541939738723L;
        private final FacesEvent target;
        private final FxTreeNode node;

        public NodeEvent(TreeNavigation owner, FacesEvent target, FxTreeNode node) {
            super(owner);
            this.target = target;
            this.node = node;
        }

        @Override
        public PhaseId getPhaseId() {
            return (this.target.getPhaseId());
        }

        @Override
        public void setPhaseId(PhaseId phaseId) {
            this.target.setPhaseId(phaseId);
        }

        @Override
        public boolean isAppropriateListener(FacesListener listener) {
            return this.target.isAppropriateListener(listener);
        }

        @Override
        public void processListener(FacesListener listener) {
            ((TreeNavigation) getComponent()).provideVar(getCurrentInstance(), node);
            this.target.processListener(listener);
            ((TreeNavigation) getComponent()).removeVar(getCurrentInstance(), node);
        }

        public FacesEvent getTarget() {
            return target;
        }

        public FxTreeNode getNode() {
            return node;
        }
    }

    @Override
    public Object saveState(FacesContext context) {
        final Object[] state = new Object[14];
        state[0] = super.saveState(context);
        state[1] = mode;
        state[2] = nodeId;
        state[3] = path;
        state[4] = depth;
        state[5] = output;
        state[6] = menuOptions;
        state[7] = var;
        state[8] = treeOptions;
        state[9] = selectedNodeId;
        state[10] = selectedPath;
        state[11] = baseTypeId;
        state[12] = includeRoot;
        state[13] = includeFolders;
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
        this.treeOptions = (String) state[8];
        this.selectedNodeId = (Long) state[9];
        this.selectedPath = (String) state[10];
        this.baseTypeId = (Long) state[11];
        this.includeRoot = (Boolean) state[12];
        this.includeFolders = (Boolean) state[13];
    }
}
