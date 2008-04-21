/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.example.war;

import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.SortDirection;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.search.query.VersionFilter;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.value.FxString;

import javax.faces.model.DataModel;

/**
 * Provides a list of all products and stores the currently shown product in detail view.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ProductBean {
    private DataModel products;
    /** PK of the displayed product (in detail view) */
    private FxPK pk;
    /** The currently selected article number (if any) */
    private FxString articleNumber;

    public DataModel getProducts() throws FxApplicationException {
        if (products == null) {
            final FxResultSet result =
                    new SqlQueryBuilder()
                    .select("@pk", "product/name", "product/price", "product/variant/articlenumber")
                    .filterVersion(VersionFilter.LIVE)
                    .type("product")
                    .orderBy("product/name", SortDirection.ASCENDING)
                    .getResult();
            products = new FxResultSetDataModel(result);
        }
        return products;
    }

    public FxPK getPk() {
        return pk;
    }

    public void setPk(FxPK pk) {
        this.pk = pk;
    }

    public FxString getArticleNumber() {
        return articleNumber;
    }

    public void setArticleNumber(FxString articleNumber) {
        this.articleNumber = articleNumber;
    }
}
