/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
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
package com.flexive.war.beans.admin.main;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.cmis.CmisResultSet;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.FxJsfUtils;
import org.apache.commons.lang.StringUtils;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import java.io.Serializable;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class CmisSearchBean implements Serializable {
    private static final long serialVersionUID = 4232425892696197228L;
    private static final String LAST_QUERY_STORE = CmisSearchBean.class + "_LAST_QUERY";

    private String query;
    private CmisResultSet result;
    private DataModel resultModel;
    private int startRow;
    private int maxRows = 1000;

    public String getQuery() {
        if (query == null) {
            query = (String) FxJsfUtils.getSessionAttribute(LAST_QUERY_STORE);
        }
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
        this.result = null;
        FxJsfUtils.setSessionAttribute(LAST_QUERY_STORE, query);
    }

    public CmisResultSet getResult()  {
        if (result == null && StringUtils.isNotBlank(query)) {
            try {
                result = EJBLookup.getCmisSearchEngine().search(query, true, startRow, maxRows);
                resultModel = null;
            } catch (Exception e) {
                new FxFacesMsgErr(e).addToContext();
            }
        }
        return result;
    }

    public void setResult(CmisResultSet result) {
        this.result = result;
    }

    public DataModel getResultModel() {
        if (resultModel == null && getResult() != null) {
            resultModel = new ListDataModel(result.getRows());
        }
        return resultModel;
    }

    public void setResultModel(DataModel resultModel) {
        this.resultModel = resultModel;
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }
}
