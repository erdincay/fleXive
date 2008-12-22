package com.flexive.example.war;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.VersionFilter;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.faces.beans.PageBean;

import javax.faces.model.DataModel;

/**
 * Provides a manufacturer list and stores the currently shown manufacturer PK.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ManufacturerBean {
    /** All products of the current PK */
    private DataModel products;

    public DataModel getProducts() throws FxApplicationException {
        final FxPK pk = PageBean.getInstance().getPagePK();
        if (products == null && !pk.isNew()) {
            final FxResultSet result = new SqlQueryBuilder()
                    .select("@pk", "product/name", "product/price", "product/variant/articlenumber")
                    .filterVersion(VersionFilter.LIVE)
                    .condition("product/manufacturer", PropertyValueComparator.EQ, pk)
                    .getResult();
            products = new FxResultSetDataModel(result);
        }
        return products;
    }

    public void setProducts(DataModel products) {
        this.products = products;
    }
}
