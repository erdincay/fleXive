package com.flexive.core.search;

import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxFlatStorageMapping;
import com.flexive.shared.FxLanguage;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Utility methods for FxSQL and CMIS-SQL query engines.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 * @since 3.1
 */
public class SearchUtils {
    private SearchUtils() {
    }

    public static String getFlatStorageAssignmentFilter(FxEnvironment environment, String flatTableAlias, FxPropertyAssignment assignment) {
        final List<String> conditions = newArrayList();
        conditions.add(flatStorageFilterCondition(flatTableAlias, assignment));
        // also check derived assignments
        for (FxPropertyAssignment derived : assignment.getDerivedAssignments(environment)) {
            if (derived.isFlatStorageEntry()) {
                conditions.add(flatStorageFilterCondition(flatTableAlias, derived));
            }
        }
        return "(" + StringUtils.join(conditions, " OR ") + ")";
    }

    private static String flatStorageFilterCondition(String flatTableAlias, FxPropertyAssignment assignment) {
        final String alias = StringUtils.isNotBlank(flatTableAlias) ? flatTableAlias + "." : "";
        return "(" + alias + "typeid=" + assignment.getAssignedType().getId()
                + " AND " + alias + "lvl=" + assignment.getFlatStorageMapping().getLevel()
                + (assignment.isMultiLang() ? "" : " AND " + alias + "lang=" + FxLanguage.SYSTEM_ID)
                + ")";
    }
}
