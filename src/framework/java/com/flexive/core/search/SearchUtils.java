package com.flexive.core.search;

import com.flexive.core.DatabaseConst;
import com.flexive.core.storage.DBStorage;
import com.flexive.core.storage.StorageManager;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.ACLPermission;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxFlatStorageMapping;
import com.flexive.shared.FxLanguage;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.flexive.shared.structure.FxType;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.Workflow;
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

    /**
     * Return the security filter for instance-, type-, and workflow-based permissions.
     *
     * @param tableAlias        the table alias where the content columns are present
     * @param referencedTypes   all types that may occur in the result table
     * @param contentTableAvailable whether content table columns are available in tableAlias.
     * The static security filter needs at least these FX_CONTENT columns selected:
     * <ul>
     * <li>mandator</li>
     * <li>tdef</li>
     * <li>created_by</li>
     * <li>acl</li>
     * </ul>
     * If these columns are not present and this flag is set to false, the more generic
     * (and slower) stored procedure mayReadInstance2 is used.
     * @return  the security filter for instance-, type-, and workflow-based permissions.
     */
    public static String getSecurityFilter(String tableAlias, List<FxType> referencedTypes, boolean contentTableAvailable) {
        final UserTicket ticket = FxContext.getUserTicket();

        if (ticket.isGlobalSupervisor()) {
            return "1=1";
        }

        if (!contentTableAvailable) {
            // not selected from content table, main table properties for optimized security filter are not available
            final DBStorage storage = StorageManager.getStorageImpl();
            return "mayReadInstance2(" + tableAlias + ".id," + tableAlias + ".ver," + ticket.getUserId() + ","
                    + ticket.getMandatorId() + "," + storage.getBooleanExpression(ticket.isMandatorSupervisor()) +
                    "," + storage.getBooleanExpression(ticket.isGlobalSupervisor()) + ")="+storage.getBooleanTrueExpression();
        }

        // create security filters per type - the conditions will be joined with "OR"
        // property permissions are not handled by this filter, this is the responsibility of the
        // methods that return data from selected properties
        final List<String> conditions = new ArrayList<String>();
        if (ticket.isMandatorSupervisor()) {
            conditions.add(tableAlias + ".mandator=" + ticket.getMandatorId());
        }

        for (FxType type : referencedTypes) {
            final String tdef = tableAlias + ".tdef=" + type.getId();
            if (!type.isUsePermissions()) {
                // no filtering at all needed for this type
                conditions.add(tdef);
                continue;
            }
            final List<String> typeFilter = new ArrayList<String>();
            typeFilter.add(tdef);
            if (type.isUseTypePermissions() && !ticket.mayReadACL(type.getACL().getId(), -1)) {
                if (!ticket.mayReadACL(type.getACL().getId(), ticket.getUserId())) {
                    // neither general nor private read permissions - skip all instances of this type
                    continue;
                }
                // ACL cannot be read, but private permissions set - select only own instances
                typeFilter.add(tableAlias + ".created_by=" + ticket.getUserId());
            }
            if (type.isUseInstancePermissions()) {
                // collect all readable instance ACLs
                final List<Long> readable = new ArrayList<Long>(
                        Arrays.asList(ticket.getACLsId(-1, ACLCategory.INSTANCE, ACLPermission.READ))
                );
                // collect all ACLs that can be read if the calling user is the owner
                final List<Long> privateReadable = new ArrayList<Long>(
                        Arrays.asList(ticket.getACLsId(ticket.getUserId(), ACLCategory.INSTANCE, ACLPermission.READ))
                );
                // remove all ACLs that are readable regardless of the owner
                privateReadable.removeAll(readable);
                // add ACL filter
                typeFilter.add(
                        contentFilterWithPrivate(ticket, tableAlias,
                                contentAclFilter(tableAlias, readable),
                                contentAclFilter(tableAlias, privateReadable)
                        )
                );
            }
            if (type.isUseStepPermissions()) {
                // collect all readable workflow steps
                final List<Long> readable = new ArrayList<Long>();
                final List<Long> privateReadable = new ArrayList<Long>();
                for (Workflow workflow : CacheAdmin.getEnvironment().getWorkflows()) {
                    for (Step step : workflow.getSteps()) {
                        if (ticket.mayReadACL(step.getAclId(), -1)) {
                            // readable ACL
                            readable.add(step.getId());
                        } else if (ticket.mayReadACL(step.getAclId(), ticket.getUserId())) {
                            // ACL readable only when the calling user is the owner
                            privateReadable.add(step.getAclId());
                        }
                    }
                }
                // add step filter
                typeFilter.add(
                        contentFilterWithPrivate(ticket, tableAlias, readable, privateReadable, "step")
                );
            }
            conditions.add("(" + StringUtils.join(typeFilter, " AND ") + ")");
        }
        return conditions.isEmpty() ? "1=0" : "(" + StringUtils.join(conditions, " OR ") + ")";
    }

    private static String contentAclFilter(String contentTableAlias, List<Long> acls) {
        return acls.isEmpty() ? null :
                // first check for contents that have the desired ACL in the main table column
                "(" + contentTableAlias + ".acl IN (" + StringUtils.join(acls, ',') + ") " +
                // then check for the ACL in TBL_CONTENT_ACLS
                "OR EXISTS(" +
                contentAclFilter(contentTableAlias, DatabaseConst.TBL_CONTENT_ACLS, acls)
                + "))";
    }

    private static String contentAclFilter(String contentTableAlias, String table, List<Long> acls) {
        return "SELECT c.acl FROM " + table + " c WHERE c.id=" + contentTableAlias + ".id AND c.ver=" + contentTableAlias + ".ver " +
                " AND c.acl IN (" + StringUtils.join(acls, ',') + ")";
    }

    private static String contentFilterWithPrivate(UserTicket ticket, String tableAlias, List<Long> readableIds, List<Long> privateReadableIds, String column) {
        return contentFilterWithPrivate(
                ticket,
                tableAlias,
                tableAlias + "." + column + " IN (" + StringUtils.join(readableIds, ',') + ")",
                privateReadableIds.isEmpty() ? null :
                        tableAlias + "." + column + " IN (" + StringUtils.join(privateReadableIds, ',') + ")"
        );
    }

    private static String contentFilterWithPrivate(UserTicket ticket, String contentTableAlias, String readableFilter, String privateReadableFilter) {
        final String result;
        if (isBlank(privateReadableFilter)) {
            // no private permissions have to be checked
            result = readableFilter;
        } else {
            // must match readable column OR owner and private readable column
            result = "(" + readableFilter + " OR ("
                    + contentTableAlias + ".created_by=" + ticket.getUserId() + " AND "
                    + privateReadableFilter
                    + "))";
        }
        return result;
    }

    private static String flatStorageFilterCondition(String flatTableAlias, FxPropertyAssignment assignment) {
        final String alias = StringUtils.isNotBlank(flatTableAlias) ? flatTableAlias + "." : "";
        return "(" + alias + "typeid=" + assignment.getAssignedType().getId()
                + " AND " + alias + "lvl=" + assignment.getFlatStorageMapping().getLevel()
                + (assignment.isMultiLang() ? "" : " AND " + alias + "lang=" + FxLanguage.SYSTEM_ID)
                + ")";
    }
}
