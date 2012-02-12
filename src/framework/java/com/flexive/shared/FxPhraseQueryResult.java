/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2012
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
package com.flexive.shared;

import java.io.Serializable;
import java.util.List;

/**
 * Phrase query result
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@SuppressWarnings("UnusedDeclaration")
public class FxPhraseQueryResult implements Serializable {
    private FxPhraseQuery query;
    private int totalResults;
    private int startRow;
    private int pageSize;
    private int resultCount;
    private List<FxPhrase> result;

    public FxPhraseQueryResult(FxPhraseQuery query, int totalResults, int startRow, int pageSize, int resultCount,
                               List<FxPhrase> result) {
        this.query = query;
        this.totalResults = totalResults;
        this.startRow = startRow;
        this.pageSize = pageSize;
        this.resultCount = resultCount;
        this.result = result;
    }

    public FxPhraseQuery getQuery() {
        return query;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getResultCount() {
        return resultCount;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        if (totalResults % pageSize == 0)
            return totalResults / pageSize;
        else
            return (totalResults / pageSize) + 1;
    }

    public int getCurrentPage() {
        return (startRow / pageSize) + 1;
    }

    public List<FxPhrase> getResult() {
        return result;
    }

    private void applyValues(FxPhraseQueryResult result) {
        this.query = result.query;
        this.pageSize = result.pageSize;
        this.totalResults = result.totalResults;
        this.resultCount = result.resultCount;
        this.startRow = result.startRow;
        this.result = result.result;
    }

    public void nextPage() {
        if (getCurrentPage() < getTotalPages())
            applyValues(EJBLookup.getPhraseEngine().search(this.query, getCurrentPage() + 1, this.pageSize));
    }

    public void previousPage() {
        if (getCurrentPage() > 1)
            applyValues(EJBLookup.getPhraseEngine().search(this.query, getCurrentPage() - 1, this.pageSize));
    }

    public void firstPage() {
        if (getCurrentPage() != 1)
            applyValues(EJBLookup.getPhraseEngine().search(this.query, 1, this.pageSize));
    }

    public void lastPage() {
        if (getCurrentPage() < getTotalPages())
            applyValues(EJBLookup.getPhraseEngine().search(this.query, getTotalPages(), this.pageSize));
    }

    public void gotoPage(int page) {
        if (page > 0 && page != getCurrentPage() && page <= getTotalPages())
            applyValues(EJBLookup.getPhraseEngine().search(this.query, page, this.pageSize));
    }

    public void refresh() {
        applyValues(EJBLookup.getPhraseEngine().search(this.query, this.getCurrentPage(), this.getPageSize()));
    }

    public void switchResultLanguage(long language) {
        if (!this.query.isResultLanguageRestricted())
            return;
        this.query.setResultLanguage(language);
        applyValues(EJBLookup.getPhraseEngine().search(this.query, this.getCurrentPage(), this.pageSize));
    }
}
