package com.flexive.faces.components.menu.yui;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.javascript.menu.MenuItemContainer;
import com.flexive.faces.javascript.yui.menu.YahooMenuItemData;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class YahooMenuItem extends UIOutput implements MenuItemContainer<YahooMenuItemData> {
    public static final String COMPONENT_TYPE = "flexive.YahooMenuItem";

    private final List<YahooMenuItemData> menuItems = new ArrayList<YahooMenuItemData>();
    private String labelKey;
    private String label;
    private String icon;
    private String clickHandler;
    private String obj;    // arguments for clickHandler
    private String url;
    private Map<String, Object> itemProperties;

    public YahooMenuItem() {
        setRendererType(null);
    }

    @Override
    public boolean getRendersChildren() {
        return false;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        final MenuItemContainer container = FxJsfUtils.findAncestor(this, MenuItemContainer.class);
        final String itemLabel = getLabel() != null ? getLabel() : FxJsfUtils.getLocalizedMessage(getLabelKey());
        final YahooMenuItemData menuItem = new YahooMenuItemData(
                getId() != null ? getId() : getClientId(facesContext), itemLabel
                , getIcon(), getClickHandler(), getObj(), getUrl(), getItemProperties(), getMenuItems());
        container.addMenuItem(menuItem);
    }

    @Override
    public void addMenuItem(YahooMenuItemData menuItem) {
        menuItems.add(menuItem);
    }

    @Override
    public List<YahooMenuItemData> getMenuItems() {
        return menuItems;
    }

    public String getLabelKey() {
        if (labelKey == null) {
            labelKey = FxJsfComponentUtils.getStringValue(this, "labelKey");
        }
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getIcon() {
        if (icon == null) {
            icon = FxJsfComponentUtils.getStringValue(this, "icon");
        }
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getClickHandler() {
        if (clickHandler == null) {
            clickHandler = FxJsfComponentUtils.getStringValue(this, "clickHandler");
        }
        return clickHandler;
    }

    public void setClickHandler(String clickHandler) {
        this.clickHandler = clickHandler;
    }

    public String getLabel() {
        if (label == null) {
            label = FxJsfComponentUtils.getStringValue(this, "label");
        }
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUrl() {
        if (url == null) {
            url = FxJsfComponentUtils.getStringValue(this, "url");
        }
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getObj() {
        if (obj == null) {
            obj = FxJsfComponentUtils.getStringValue(this, "obj");
        }
        return obj;
    }

    public void setObj(String obj) {
        this.obj = obj;
    }

    public Map<String, Object> getItemProperties() {
        return itemProperties;
    }

    public void setItemProperties(Map<String, Object> itemProperties) {
        this.itemProperties = itemProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[7];
        state[0] = super.saveState(facesContext);
        state[1] = clickHandler;
        state[2] = icon;
        state[3] = label;
        state[4] = labelKey;
        state[5] = url;
        state[6] = obj;
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState(FacesContext facesContext, Object o) {
        Object[] state = (Object[]) o;
        super.restoreState(facesContext, state[0]);
        clickHandler = (String) state[1];
        icon = (String) state[2];
        label = (String) state[3];
        labelKey = (String) state[4];
        url = (String) state[5];
        obj = (String) state[6];
    }

}
