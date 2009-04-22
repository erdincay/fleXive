package com.flexive.faces.javascript.yui.menu;

import com.flexive.faces.javascript.menu.AbstractMenuItemData;
import com.flexive.faces.javascript.RelativeUriMapper;
import com.flexive.war.JsonWriter;

import java.util.Map;
import java.util.List;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

/**
 * Yahoo UI menu item wrapper.
 * 
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class YahooMenuItemData extends AbstractMenuItemData<YahooMenuItemData> {
    private String url;
    private String obj;

    public YahooMenuItemData(String title) {
        super(title);
    }

    /**
     * Creates a container menu item (for item groups).
     *
     * @param menuItems the nested menu items
     * @since 3.1
     */
    public YahooMenuItemData(List<YahooMenuItemData> menuItems) {
        super(menuItems);
    }

    public YahooMenuItemData(String id, String title, String icon, String onClick, String obj, String url,
                             Map<String, Object> properties, List<YahooMenuItemData> menuItems) {
        super(id, title, icon, onClick, properties, menuItems, false);
        this.url = url;
        this.obj = obj;
    }

    @Override
    public void renderItemAttributes(JsonWriter out, RelativeUriMapper uriMapper, Map<String, String> subscriptions, String widgetId) throws IOException {
        out.writeAttribute("id", id);
        out.writeAttribute("text", title);
        if (StringUtils.isNotBlank(url)) {
            out.writeAttribute("url", url);
        }
        if (StringUtils.isNotBlank(onClick)) {
            out.startAttribute("onclick").startMap();
            out.writeAttribute("fn", onClick, false);
            if (StringUtils.isNotBlank(obj)) {
                out.writeAttribute("obj", obj, false);
            }
            out.closeMap();
        }
    }
}
