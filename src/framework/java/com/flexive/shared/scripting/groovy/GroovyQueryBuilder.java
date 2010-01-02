/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxInvalidStateException;
import com.flexive.shared.search.query.*;
import com.flexive.shared.structure.FxEnvironment;
import groovy.lang.Closure;
import groovy.util.BuilderSupport;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An (experimental) groovy builder for search queries.<br/>
 * Example:<br/>
 * <pre>
    new GroovyQueryBuilder().select(["@pk", "caption", "*"]) {
      eq("caption", "bla")
      not_empty("filename")
      or {
         gt("id", 0)
         lt("id", 100)
      }
      lt("created_at", new Date())
    }.sqlQuery
 * </pre>
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class GroovyQueryBuilder extends BuilderSupport {
    private final FxEnvironment environment = CacheAdmin.getEnvironment();
    private QueryRootNode root = new QueryRootNode(QueryRootNode.Type.CONTENTSEARCH);

    public GroovyQueryBuilder() {
    }

    /**
     * Use an external query builder as the target builder.
     *
     * @param builder   a sql query builder to be used for creating the query
     */
    public GroovyQueryBuilder(SqlQueryBuilder builder) {
        root.setQueryBuilder(builder);
    }

    public Object builder(Closure closure) {
        System.out.println(closure);
        return root;
    }
    
    /** {@inheritDoc} */
    @Override
    protected Object doInvokeMethod(String methodName, Object name, Object args) {
        List list = InvokerHelper.asList(args);
        if (list.size() >= 2) {
            Object object1 = list.get(0);
            Object object2 = list.get(1);
            if (!(object1 instanceof Closure || object1 instanceof Map)
                    && !(object2 instanceof Closure || object2 instanceof Map)) {
                // object1=property, object2=value
                Map<String, Object> attributes = new HashMap<String, Object>();
                attributes.put("property", object1);
                if (list.size() > 2) {
                    // pass parameters as list
                    object2 = list.subList(1, list.size());
                }
                attributes.put("value", object2);
                return super.doInvokeMethod(methodName, name, attributes);
            }
        }
        return super.doInvokeMethod(methodName, name, args);
    }

    /** {@inheritDoc} */
    @Override
    protected void setParent(Object parent, Object child) {
        if (parent != child) {
            root.addChild((QueryNode) parent, (QueryNode) child);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Object createNode(Object name) {
        if ("and".equals(name) || "or".equals(name)) {
            QueryOperatorNode node;
            if (root == null) {
                root = new QueryRootNode(QueryRootNode.Type.CONTENTSEARCH);
                node = root;
            } else {
                node = new QueryOperatorNode(root.getNewId());
            }
            node.setOperator("and".equals(name) ? QueryOperatorNode.Operator.AND : QueryOperatorNode.Operator.OR);
            return node;
        } else {
            final PropertyValueComparator comparator;
            try {
                comparator = PropertyValueComparator.valueOf(((String) name).toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new FxInvalidParameterException("COMPARATOR", "ex.fxQueryBuilder.comparator.unknown",
                        name, Arrays.asList(PropertyValueComparator.values())).asRuntimeException();
            }
            PropertyValueNode node = new PropertyValueNode(root.getNewId(), -1);
            node.setComparator(comparator);
            return node;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Object createNode(Object name, Object value) {

        if ("select".equals(name)) {
            if (!(value instanceof List)) {
                throw new FxInvalidParameterException("VALUE", "ex.fxQueryBuilder.select.column").asRuntimeException();
            }
            List columns = (List) value;
            if (!columns.isEmpty()) {
                if (root.getQueryBuilder() == null) {
                    root.setQueryBuilder(new SqlQueryBuilder());
                }
                root.getQueryBuilder().setIncludeBasicSelects(false);
                // add column as specified in the columns attribute
                for (Object column: columns) {
                    root.getQueryBuilder().select(column.toString());
                }
            }
            return root;
        } else if ("filterBriefcase".equals(name)) {
            if (!(value instanceof Number)) {
                throw new FxInvalidParameterException("VALUE", "ex.fxQueryBuilder.filterBriefcase.number").asRuntimeException();
            }
            root.getQueryBuilder().filterBriefcase(((Number) value).longValue());
            return root;
        } else {
            final QueryNode node = (QueryNode) createNode(name);
            if (!(node instanceof PropertyValueNode)) {
                return root;
            }
            // name = comparator, value = [property name, value]
            final PropertyValueNode propertyNode = (PropertyValueNode) node;
            /*if (value instanceof List) {
                List args = (List) value;
                propertyNode.setProperty(environment.getProperty((String) args.get(0)));
                if (propertyNode.getComparator().isNeedsInput()) {
                    if (args.size() < 2) {
                        throw new FxInvalidStateException("ex.fxQueryBuilder.node.value", propertyNode.getComparator()).asRuntimeException();
                    }
                    propertyNode.setValue(getFxValue(args.get(1)));
                }
            } else */if (propertyNode.getComparator().isNeedsInput()) {
                // operator needs input, but only property name supplied
                throw new FxInvalidParameterException("VALUE", "ex.fxQueryBuilder.node.value",
                        propertyNode.getComparator()).asRuntimeException();
            } else {
                // comparator doesn't need input, property name supplied as scalar
                propertyNode.setPropertyId(environment.getProperty((String) value).getId());
            }
            return node;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Object createNode(Object name, Map attributes) {
        final Object node = createNode(name);
        if (node instanceof PropertyValueNode) {
            // name = comparator, attributes = {column, value}
            final PropertyValueNode propertyNode = (PropertyValueNode) node;
            if (attributes.get("property") == null) {
                throw new FxInvalidStateException("ex.fxQueryBuilder.node.column", propertyNode.getComparator()).asRuntimeException();
            }
            if (propertyNode.getComparator().isNeedsInput() && attributes.get("value") == null) {
                throw new FxInvalidStateException("ex.fxQueryBuilder.node.value", propertyNode.getComparator()).asRuntimeException();
            }
            String propertyName = (String) attributes.get("property");
            propertyNode.setPropertyId(environment.getProperty(propertyName).getId());
            propertyNode.setValue(propertyNode.getProperty().getEmptyValue());
            Object value = attributes.get("value");
            //noinspection unchecked
            propertyNode.getValue().setDefaultTranslation(value);
            return propertyNode;
        } else {
            throw new UnsupportedOperationException("createNode(Object, Map) not implemented for " + name);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Object createNode(Object name, Map attributes, Object value) {
        throw new UnsupportedOperationException("createNode(Object, Map, Object) not implemented yet");
    }
}
