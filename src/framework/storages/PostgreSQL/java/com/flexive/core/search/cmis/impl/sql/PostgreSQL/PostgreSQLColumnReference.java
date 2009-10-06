/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.core.search.cmis.impl.sql.PostgreSQL;

import com.flexive.core.search.cmis.impl.ResultColumnReference;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.generic.mapper.select.GenericColumnReference;
import com.flexive.core.search.cmis.impl.sql.generic.GenericSqlDialect;
import com.flexive.core.search.cmis.model.ColumnReference;
import com.flexive.core.search.PropertyResolver;
import static com.flexive.core.DatabaseConst.TBL_CONTENT;
import static com.flexive.core.DatabaseConst.TBL_CONTENT_ACLS;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.value.*;
import com.flexive.shared.security.ACL;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class PostgreSQLColumnReference extends GenericColumnReference {
    private static final String GROUPSEP = "|&#@";
    private static final PostgreSQLColumnReference INSTANCE = new PostgreSQLColumnReference();

    @Override
    public boolean isDirectSelectForMultivalued(SqlMapperFactory factory, ResultColumnReference column, FxDataType dataType) {
        return dataType != null
                && column.getPropertyEntry() != null
                // multivalued properties are stored in CONTENT_DATA
                && (column.getPropertyEntry().getTableType() == PropertyResolver.Table.T_CONTENT_DATA
                // ... except for the ACL, which will require extra care
                || isACL(column.getSelectedObject()))
                && (dataType.isPrimitiveValueType()
                || FxDataType.Reference.equals(dataType)
                || FxDataType.SelectOne.equals(dataType));
    }

    @Override
    protected String getMultivaluedConcatFunction(ColumnReference reference, String column) {
        if (reference.getPropertyEntry() != null && reference.getPropertyEntry().getTableType() != PropertyResolver.Table.T_CONTENT_DATA) {
            throw new IllegalArgumentException(
                    "Cannot select multivalued property " + reference.getAlias() + " from table " + reference.getPropertyEntry().getTableName()
            );
        }
        return "GROUP_CONCAT(" + column + " ORDER BY pos SEPARATOR '" + GROUPSEP + "')";
    }

    @Override
    protected String selectUsingTableFilter(String readColumn, ColumnReference column, String tableName) {
        if (isACL(column)) {
            // select ACLs from main table and from links FX_CONTENT_ACLS table
            // TODO: FX-658: this is *very* slow and could be greatly improved by caching the ACL ID list in FX_CONTENT
            return "SELECT GROUP_CONCAT(" + SUBSEL_ALIAS + "." + readColumn + " SEPARATOR '" + GROUPSEP + "') "
                    // select from FX_CONTENT
                    + " FROM (SELECT id, ver, acl FROM " + TBL_CONTENT + " acls_content"
                    + " WHERE acl != " + ACL.NULL_ACL_ID
                    // select from FX_CONTENT_ACLS 
                    + " UNION"
                    + " SELECT id, ver, acl FROM " + TBL_CONTENT_ACLS + " acls"
                    + ") " + SUBSEL_ALIAS
                    + " WHERE " + column.getTableReference().getIdVersionLink(GenericSqlDialect.FILTER_ALIAS, SUBSEL_ALIAS);
        } else {
            return super.selectUsingTableFilter(readColumn, column, tableName);
        }
    }

    @Override
    protected Object decodeMultivaluedRowValue(SqlMapperFactory factory, FxDataType dataType, String value) {
        final boolean usePrimitive = factory.getSqlDialect().isReturnPrimitives();
        switch (dataType) {
            // TODO: refactor mapping of primitive values to FxValues 
            case Number:
                final int nval = Integer.parseInt(value);
                return usePrimitive ? nval : new FxNumber(false, nval);
            case LargeNumber:
                final long largeval = Long.parseLong(value);
                return usePrimitive ? largeval : new FxLargeNumber(false, largeval);
            case String1024:
            case Text:
                return usePrimitive ? value : new FxString(false, value);
            case HTML:
                return usePrimitive ? value : new FxHTML(false, value);
            case Double:
                final double dval = Double.parseDouble(value);
                return usePrimitive ? dval : new FxDouble(false, dval);
            case Float:
                final float fval = Float.parseFloat(value);
                return usePrimitive ? fval : new FxFloat(false, fval);
            case Boolean:
                final boolean bval = Boolean.parseBoolean(value != null ? value.toLowerCase() : null);
                return usePrimitive ? bval : new FxBoolean(false, bval);
            case Reference:
                // TODO: reference needs version and possibly caption
                final ReferencedContent rc = new ReferencedContent(Long.parseLong(value));
                return usePrimitive ? rc : new FxReference(false, rc);
            case SelectOne:
                final FxSelectListItem sli = factory.getSqlDialect().getEnvironment().getSelectListItem(Long.parseLong(value));
                return usePrimitive ? sli : new FxSelectOne(false, sli);
            default:
                throw new IllegalArgumentException("Cannot decode multivalued property of type " + dataType);
        }
    }

    @Override
    protected List<String> splitMultivaluedResult(String result) {
        return result != null
                ? Arrays.asList(StringUtils.splitByWholeSeparator(result, GROUPSEP))
                : new ArrayList<String>(0);
    }

    public static PostgreSQLColumnReference getInstance() {
        return INSTANCE;
    }

    private boolean isACL(ColumnReference columnReference) {
        return "ACL".equalsIgnoreCase(columnReference.getPropertyEntry().getProperty().getName());
    }
}
