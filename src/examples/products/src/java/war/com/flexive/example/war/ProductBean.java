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
