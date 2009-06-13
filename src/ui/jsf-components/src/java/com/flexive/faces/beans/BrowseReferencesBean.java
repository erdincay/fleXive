/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.faces.beans;

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.XPathElement;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.AdminResultLocations;
import com.flexive.shared.search.ResultViewType;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

/**
 * Provides a simple reference browser, currently without pagination.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class BrowseReferencesBean implements ActionBean, Serializable {
    private static final long serialVersionUID = -28763368417197965L;
    private static final Log LOG = LogFactory.getLog(BrowseReferencesBean.class);
    private String xPath;
    private String inputName;
    private String formName;
    private String query;
    private FxResultSet result;

    /**
     * {@inheritDoc}
     */
    public String getParseRequestParameters() {
        setXPath(StringUtils.defaultIfEmpty(FxJsfUtils.getParameter("xPath"), xPath));
        setInputName(StringUtils.defaultIfEmpty(FxJsfUtils.getParameter("inputName"), inputName));
        setFormName(StringUtils.defaultIfEmpty(FxJsfUtils.getParameter("formName"), formName));
        return null;
    }

    public FxResultSet getResult() throws FxApplicationException {
        if (result == null) {
            result = getQueryBuilder(getReferencedType()).getResult();
        }
        return result;
    }

    public FxType getReferencedType() {
        if (StringUtils.isBlank(xPath)) {
            return null;
        }
        if (!XPathElement.isValidXPath(xPath)) {
            //might be a property
            if (CacheAdmin.getEnvironment().propertyExists(xPath)) {
                return CacheAdmin.getEnvironment().getProperty(xPath).getReferencedType();
            } else
                throw new FxInvalidParameterException("xPath", LOG, "ex.browseReferences.xpath.invalid", xPath).asRuntimeException();
        }
        final FxAssignment fxAssignment = CacheAdmin.getEnvironment().getAssignment(xPath);
        if (!(fxAssignment instanceof FxPropertyAssignment)) {
            throw new FxInvalidParameterException("xPath", LOG, "ex.browseReferences.assignment.type",
                    xPath, fxAssignment).asRuntimeException();
        }
        return ((FxPropertyAssignment) fxAssignment).getProperty().getReferencedType();
    }

    private SqlQueryBuilder getQueryBuilder(FxType referencedType) {
        final SqlQueryBuilder builder = new SqlQueryBuilder(AdminResultLocations.BROWSE_REFERENCES, ResultViewType.LIST)
                .select("@pk", "@*");
        if (referencedType != null) {
            builder.type(referencedType.getId());
        }
        if (StringUtils.isNotBlank(query)) {
            builder.condition("*", PropertyValueComparator.EQ, query.trim());
        }
        return builder;
    }

    public String getXPath() {
        return xPath;
    }

    public void setXPath(String xPath) {
        if(!StringUtils.isEmpty(xPath))
            this.xPath = xPath.trim().toUpperCase();
        else
            this.xPath = xPath; //just accept it
    }

    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
