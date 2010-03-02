package com.flexive.faces.components.menu.yui;

import com.flexive.faces.javascript.menu.MenuItemContainer;
import com.flexive.faces.javascript.yui.menu.YahooMenuWriter;
import com.flexive.faces.javascript.yui.menu.YahooMenuItemData;
import com.flexive.faces.javascript.FxJavascriptUtils;
import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.faces.JsfRelativeUriMapper;
import static com.flexive.faces.FxJsfComponentUtils.getStringValue;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.component.UIOutput;
import javax.faces.component.NamingContainer;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class YahooMenu extends UIOutput implements MenuItemContainer<YahooMenuItemData>, NamingContainer {
    public static final String COMPONENT_TYPE = "flexive.YahooMenu";

    public static final String ICON_PATH = "adm/images/menu";

    private final List<YahooMenuItemData> menuItems = new ArrayList<YahooMenuItemData>();
    private String name;
    private String trigger;
    private String clickHandler;
    private String beforeShow;

    @Override
    public boolean getRendersChildren() {
        return false;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getClientId(FacesContext facesContext) {
        if (getName() != null) {
            setId(getName());    // use the js component name as our id
        }
        return super.getClientId(facesContext);
    }

    @Override
    public void encodeEnd(FacesContext facesContext) throws IOException {
        final ResponseWriter writer = facesContext.getResponseWriter();
        final String containerId = getClientId(facesContext) + "_menu";

        // render menu container
        writer.write("<div id=\"" + containerId + "\"> </div>");

        FxJavascriptUtils.beginJavascript(writer);
        final JsfRelativeUriMapper uriMapper = new JsfRelativeUriMapper();
        YahooMenuWriter.writeMenu(writer, getName(), this, uriMapper, getTrigger());
        // write to container
        FxJavascriptUtils.onYahooLoaded(writer, "function() { \n"
                + getName() + ".render('" + containerId + "');\n"
                + (isNotBlank(getClickHandler()) ? getName() + ".clickEvent.subscribe(" + getClickHandler() + ");\n" : "")
                + (isNotBlank(getBeforeShow()) ? getName() + ".subscribe('beforeShow', " + getBeforeShow() + ");\n" : "")
                + "}\n");
        FxJavascriptUtils.endJavascript(writer);

        // add icons
        FxJavascriptUtils.beginStyleSheet(writer);
        // traverse menu item tree
        for (YahooMenuItemData item : new YahooMenuItemData(menuItems)) {
            if (isNotBlank(item.getIcon())) {
                final String imageUrl = uriMapper.getAbsoluteUri(ICON_PATH + "/" + item.getIcon() + ".png");
                writer.write("li#" + item.getId() + " .yuimenuitemlabel { background: url(" + imageUrl + ") no-repeat }\n");
            }
        }
        FxJavascriptUtils.endStyleSheet(writer);
    }

    public void addMenuItem(YahooMenuItemData menuItem) {
        menuItems.add(menuItem);
    }

    public List<YahooMenuItemData> getMenuItems() {
        return menuItems;
    }

    public String getName() {
        if (name == null) {
            name = getStringValue(this, "name");
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTrigger() {
        if (trigger == null) {
            trigger = getStringValue(this, "trigger");
        }
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getClickHandler() {
        if (clickHandler == null) {
            clickHandler = getStringValue(this, "clickHandler");
        }
        return clickHandler;
    }

    public void setClickHandler(String clickHandler) {
        this.clickHandler = clickHandler;
    }

    public String getBeforeShow() {
        if (beforeShow == null) {
            beforeShow = getStringValue(this, "beforeShow");
        }
        return beforeShow;
    }

    public void setBeforeShow(String beforeShow) {
        this.beforeShow = beforeShow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[4];
        state[0] = super.saveState(facesContext);
        state[1] = getName();
        state[2] = getTrigger();
        state[3] = getClickHandler();
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState(FacesContext facesContext, Object o) {
        Object[] state = (Object[]) o;
        super.restoreState(facesContext, state[0]);
        setName((String) state[1]);
        setTrigger((String) state[2]);
        setClickHandler((String) state[3]);
    }

}
