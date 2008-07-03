package com.flexive.faces.javascript.menu;

import com.flexive.war.JsonWriter;
import com.flexive.faces.javascript.RelativeUriMapper;

import java.util.Map;
import java.util.List;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class DojoMenuItemData extends AbstractMenuItemData<DojoMenuItemData> {
    public DojoMenuItemData(String title) {
        super(title);
    }

    public DojoMenuItemData(String id, String title, String icon, String onClick, Map<String, Object> properties, List<DojoMenuItemData> menuItems) {
        super(id, title, icon, onClick, properties, menuItems);
    }

    @Override
    public void renderItemAttributes(JsonWriter out, RelativeUriMapper uriMapper, Map<String, String> subscriptions,
                                     String widgetId) throws IOException {
        final String caption = "<span id=\"" + widgetId + "\">" + title + "</span>";    // add an unique ID for browser tests
        out.writeAttribute("widgetId", widgetId);
        out.writeAttribute("caption", caption);
        if (StringUtils.isNotBlank(icon)) {
            out.writeAttribute("iconSrc", uriMapper.getAbsoluteUri(DojoMenuWriter.ICON_PATH + "/" + icon + ".png"));
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            out.writeAttribute(entry.getKey(), entry.getValue());
        }
        if (StringUtils.isNotBlank(onClick)) {
            // event subscriptions will be written after the menu was rendered
            subscriptions.put(widgetId, onClick);
        }
    }


}
