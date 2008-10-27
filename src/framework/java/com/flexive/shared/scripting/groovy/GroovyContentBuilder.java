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
package com.flexive.shared.scripting.groovy;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.value.FxValue;
import groovy.util.BuilderSupport;

import java.util.Map;
import java.util.HashMap;

/**
 * <p>
 * A groovy builder for FxContent instances.
 * </p>
 * <p>Example:
 * <pre>
 * def builder = new GroovyContentBuilder("DOCUMENT")
 * builder {
 *  title("Test article")
 *  Abstract("My abstract text")
 *  teaser {
 *      teaser_title("Teaser title")
 *      teaser_text("Teaser text")
 *  }
 *  box {
 *      box_title(new FxString(false, "Box title 1"))
 *  }
 *  box {
 *      box_title("Box title 2")
 *      box_text("Some box text")
 *  }
 * }
 * </pre>
 * </p>
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class GroovyContentBuilder extends BuilderSupport {
    private class NodeInfo {
        String xpath;
        final Object value;

        public NodeInfo(String xpath, Object value) {
            if (value == null && !xpath.endsWith("]")) {
                // group node
                if (!newGroupIndex.containsKey(xpath)) {
                    newGroupIndex.put(xpath, 1);
                }
                this.xpath = xpath + "[" + newGroupIndex.get(xpath) + "]";
                newGroupIndex.put(xpath, newGroupIndex.get(xpath) + 1);
            } else if (!xpath.endsWith("]")) {
                String path;
                try {
                    final FxPropertyData propertyData = content.getPropertyData(xpath);
                    int newIndex = propertyData.getOccurances() + 1;
                    if (!propertyData.getAssignment().getMultiplicity().isValid(newIndex)) {
                        // increase index, unless we're at the end of the multiplicity range
                        newIndex--;
                    }
                    path = xpath + "[" + newIndex + "]";
                } catch (FxApplicationException e) {
                    path = xpath + "[1]";
                }
                this.xpath = path;
            } else {
                this.xpath = xpath;
            }
            this.value = value;
        }

        public void addParentXPath(String parentXPath) {
            xpath = parentXPath + xpath;
        }

        @SuppressWarnings({"unchecked"})
        public FxValue getValue() {
            if (value instanceof FxValue) {
                return (FxValue) value;
            } else {
                final FxValue fxValue = getPropertyAssignment(xpath).getEmptyValue();
                fxValue.setDefaultTranslation(fxValue.fromString(value.toString()));
                return fxValue;
            }
        }
    }

    private final FxContent content;
    private final FxEnvironment environment = CacheAdmin.getEnvironment();
    private final Map<String, Integer> newGroupIndex = new HashMap<String, Integer>();

    /**
     * Create a new content builder that operates on the given content instance.
     *
     * @param content   the target content
     */
    public GroovyContentBuilder(FxContent content) {
        this.content = content;
    }

    /**
     * Create an empty content builder for the given type.
     *
     * @param typeName  the content type name
     * @throws com.flexive.shared.exceptions.FxApplicationException if the content could not be initialized
     * by the content engine
     */
    public GroovyContentBuilder(String typeName) throws FxApplicationException {
        this.content = EJBLookup.getContentEngine().initialize(typeName);
    }

    /**
     * Create a content builder for the given instance.
     *
     * @param pk    the object id (the content will be loaded through the content engine)
     * @throws com.flexive.shared.exceptions.FxApplicationException if the content could not be loaded
     */
    public GroovyContentBuilder(FxPK pk) throws FxApplicationException {
        this.content = EJBLookup.getContentEngine().load(pk);
    }

    /**
     * Return our content instance.
     *
     * @return  our content instance.
     */
    public FxContent getContent() {
        return content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setParent(Object parent, Object child) {
        try {
            // if parent is a node info, use its xpath as prefix
            final NodeInfo nodeInfo = (NodeInfo) child;
            // add parent xpath to our node
            nodeInfo.addParentXPath(parent instanceof NodeInfo ? ((NodeInfo) parent).xpath : "");
            if (nodeInfo.value != null) {
                content.setValue(nodeInfo.xpath, nodeInfo.getValue());
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createNode(Object name) {
        return "call".equals(name) || "doCall".equals(name) ? name : new NodeInfo(createXPath(name), null);  
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override
    protected Object createNode(Object name, Object value) {
        final String xpath = createXPath(name);
        return new NodeInfo(xpath, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createNode(Object name, Map attributes) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createNode(Object name, Map attributes, Object value) {
        return null;
    }

    private String createXPath(Object name) {
        return (name instanceof String && !((String) name).startsWith("/") ? "/" + name : name.toString()).toUpperCase();
    }

    private FxPropertyAssignment getPropertyAssignment(String xpath) {
        return (FxPropertyAssignment) environment.getAssignment(
                environment.getType(content.getTypeId()).getName() + xpath);
    }
}

