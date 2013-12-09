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
package com.flexive.faces;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>A base implementation of an URI route. This class allows to define static mappers
 * for URI. External URIs can be parsed using {@link #getMatcher(String)} and created
 * with {@link #getMappedUri(java.util.Map)}. This implementation provides simple string-based,
 * named parameters that can be specified in the URI using "${parameter}", e.g.
 * "/myUri/${myParam}.xhtml". Subclasses like the {@link ContentURIRoute} provide more
 * specialized parameters.</p>
 * <p>
 * Although this class provides a complete implementation, it is declared abstract since
 * any usable URI mapper must define at least one static parameter.
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class URIRoute {
    private static final Log LOG = LogFactory.getLog(URIRoute.class);

    protected static final Map<String, String> PARAMETERS = new HashMap<String, String>();
    protected final Pattern pattern;
    protected final String format;
    protected final String target;
    private final Map<String, Integer> positions = new HashMap<String, Integer>();

    protected URIRoute(String target, String format) {
        this.target = target;
        this.format = format;
        this.pattern = Pattern.compile(buildRegexp(format));
    }

    /**
     * Returns the target URI of this mapper.
     *
     * @return the target URI of this mapper.
     */
    public String getTarget() {
        return target;
    }

    /**
     * Return the URI format of this mapper.
     *
     * @return the URI format of this mapper.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Return a matcher for the given URI. The matcher allows to extract parameters
     * set in the URI.
     *
     * @param uri the URI (matching this mapper's URI format) to be parsed
     * @return a matcher for the given URI
     */
    public URIMatcher getMatcher(String uri) {
        final Matcher matcher = pattern.matcher(uri);
        matcher.find();
        return new URIMatcher(this, uri, matcher);
    }

    /**
     * Replaces the given parameters in this mapper's URI format and returns the created URI.
     * Note that the given parameters must include <b>all</b> parameters set in the format,
     * although not all parameters specified have to be included in the format.
     *
     * @param parameters the parameter values to be replaced
     * @return a formatted URI
     */
    public String getMappedUri(Map<String, String> parameters) {
        String uri = format;
        int replacements = 0;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (hasParameter(entry.getKey())) {
                uri = replaceUriParameter(uri, entry.getKey(), entry.getValue());
                replacements++;
            }
        }
        if (replacements != positions.size()) {
            throw new FxInvalidParameterException("parameters", LOG, "ex.uriRoute.parameter.missing", format, uri).asRuntimeException();
        }
        return uri;
    }

    private String buildRegexp(String format) {
        final StringBuilder out = new StringBuilder();
        int pos = 0;
        int nextParameter = format.indexOf("${", pos);
        int parameterPos = 0;
        while (nextParameter != -1) {
            out.append(format, pos, nextParameter);
            final int endIndex = format.indexOf('}', nextParameter + 2);
            if (endIndex == -1) {
                throw new FxInvalidParameterException("format", LOG, "ex.uriRoute.format.closing", format).asRuntimeException();
            }
            final String name = format.substring(nextParameter + 2, endIndex);
            if (StringUtils.isBlank(name)) {
                throw new FxInvalidParameterException("format", LOG, "ex.uriRoute.format.emptyParameter", format).asRuntimeException();
            }
            if (positions.containsKey(name)) {
                throw new FxInvalidParameterException("format", LOG, "ex.uriRoute.format.uniqueParameter", name, format).asRuntimeException();
            }
            positions.put(name, ++parameterPos);
            out.append(getParameterRegexp(name));
            pos = endIndex + 1;
            nextParameter = format.indexOf("${", pos);
        }
        // append characters after last match and return formatted string
        return out.append(format, pos, format.length()).toString();
    }

    /**
     * Replace the given parameter in the URI format string.
     *
     * @param format        the format string
     * @param parameterName the parameter name
     * @param value         the value to be set for the parameter
     * @return the replaced format string
     */
    protected String replaceUriParameter(String format, String parameterName, String value) {
        // TODO create a more efficient version with string buffers
        return StringUtils.replace(format, "${" + parameterName + "}", value);
    }

    /**
     * Return the position of the given parameter name in the format string. If the parameter
     * is not set in the format string, a FxRuntimeException is thrown.
     *
     * @param parameterName the parameter name
     * @return the 1-based position of the parameter in the format string
     */
    int getPosition(String parameterName) {
        if (!positions.containsKey(parameterName)) {
            throw new FxNotFoundException("parameterName", LOG, "ex.uriRoute.parameter.unknown", parameterName).asRuntimeException();
        }
        return positions.get(parameterName);
    }

    /**
     * Returns true if the given parameter is set in this mapper's format string.
     *
     * @param parameterName the parameter name to be checked
     * @return true if the given parameter is set in this mapper's format string.
     */
    boolean hasParameter(String parameterName) {
        return positions.containsKey(parameterName);
    }

    /**
     * Returns the regular expression of the given parameter, or throws a
     * FxRuntimeException if the parameter is not available in this mapper.
     *
     * @param name the parameter name
     * @return the regular expression of the given parameter
     */
    protected String getParameterRegexp(String name) {
        if (!PARAMETERS.containsKey(name)) {
            throw new FxInvalidParameterException("name", LOG, "ex.uriRoute.parameter.unknown", name).asRuntimeException();
        }
        return PARAMETERS.get(name);
    }
}
