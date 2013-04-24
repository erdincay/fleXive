package com.flexive.shared;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Category selection for phrases
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxPhraseCategorySelection implements Serializable {

    /**
     * The default category
     */
    public final static int CATEGORY_DEFAULT = 0;

    /**
     * Only the default category is returned
     */
    public final static FxPhraseCategorySelection SELECTION_DEFAULT = new FxPhraseCategorySelection(CATEGORY_DEFAULT);

    /**
     * Any category is returned
     */
    public final static FxPhraseCategorySelection SELECTION_ANY = new FxPhraseCategorySelection();

    private int[] categories = null;

    private FxPhraseCategorySelection() {
        this.categories = new int[0];
    }

    public FxPhraseCategorySelection(int... categories) {
        this.categories = categories != null ? categories : new int[0];
        Arrays.sort(this.categories);
    }

    public int[] getCategories() {
        return categories;
    }

    public boolean isAny() {
        return categories.length == 0;
    }

    public boolean isSingleCategory() {
        return categories.length == 1;
    }

    public int getSingleCategory() {
        if (categories.length == 1)
            return categories[0];
        throw new IllegalArgumentException("Not a single category selection!");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FxPhraseCategorySelection))
            return false;
        FxPhraseCategorySelection o = (FxPhraseCategorySelection) obj;
        if (o.categories.length != this.categories.length)
            return false;
        for (int i = 0; i < this.categories.length; i++) {
            if (this.categories[i] != o.categories[i])
                return false;
        }
        return true;
    }
}
