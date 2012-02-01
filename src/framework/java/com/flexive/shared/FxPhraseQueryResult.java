package com.flexive.shared;


import java.io.Serializable;
import java.util.List;

/**
 * Phrase query result
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
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
