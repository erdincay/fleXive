/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.core.search;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.search.FxPaths;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.value.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * This class is repsonsible for reading value from the search result set, and converting it to the
 * correct result Objects
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
class ResultReader {
    private static final transient Log LOG = LogFactory.getLog(ResultReader.class);

    ResultSet rs;
    FxLanguage lang;

    protected ResultReader(final ResultSet rs, FxLanguage lang) {
        this.rs = rs;
        this.lang = lang;
    }

    protected Object getValue(final PropertyResolver.Entry entry) throws FxSqlSearchException {
        Object result;
        final int pos = entry.getPositionInResultSet();
        String xpath = null;

        try {
            // Special type @NODE_POSITION
            if (entry.getType() == PropertyResolver.Entry.Type.NODE_POSITION) {
                result = rs.getLong(pos);
            }
            // Special type @PK
            else if (entry.getType() == PropertyResolver.Entry.Type.PK) {
                long id = rs.getLong(pos);
                int ver = rs.getInt(pos + 1);
                result = new FxPK(id, ver);
            }
            // Special type @PATH
            else if (entry.getType() == PropertyResolver.Entry.Type.PATH) {
                String encoded = rs.getString(pos);
                result = new FxPaths(encoded);
            }
            // 'Normal' property
            else {

                // Get the XPATH if we are reading from the content data table
                if (entry.getTableType() == PropertyResolver.Table.T_CONTENT_DATA) {
                    xpath = rs.getString(pos + entry.getReadColumns().length);
                }

                // Handle by type
                switch (entry.getDataType()) {
                    case DateTime:
                        if( rs.getMetaData().getColumnType(pos) == java.sql.Types.BIGINT) {
                            result = new FxDateTime(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, new Date(rs.getLong(pos)));
                            break;
                        }
                        Timestamp dttstp = rs.getTimestamp(pos);
                        Date _dtdate = new Date(dttstp.getTime());
                        result = new FxDateTime(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, _dtdate);
                        break;
                    case Date:
                        Timestamp tstp = rs.getTimestamp(pos);
                        Date _date = new Date(tstp.getTime());
                        result = new FxDate(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, _date);
                        break;
                    case DateRange:
                        final Date from = new Date(rs.getTimestamp(pos).getTime());     // FDATE1
                        final Date to = new Date(rs.getTimestamp(pos + 4).getTime());   // FDATE2
                        result = new FxDateRange(new DateRange(from, to));
                        break;
                    case DateTimeRange:
                        final Date from2 = new Date(rs.getTimestamp(pos).getTime());     // FDATE1
                        final Date to2 = new Date(rs.getTimestamp(pos + 7).getTime());   // FDATE2
                        result = new FxDateTimeRange(new DateRange(from2, to2));
                        break;
                    case HTML:
                        result = new FxHTML(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, rs.getString(pos));
                        break;
                    case String1024:
                    case Text:
                        result = new FxString(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, rs.getString(pos));
                        break;
                    case LargeNumber:
                        result = new FxLargeNumber(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, rs.getLong(pos));
                        break;
                    case Number:
                        result = new FxNumber(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, rs.getInt(pos));
                        break;
                    case Float:
                        result = new FxFloat(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, rs.getFloat(pos));
                        break;
                    case Boolean:
                        result = new FxBoolean(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, rs.getBoolean(pos));
                        break;
                    case Double:
                        result = new FxDouble(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, rs.getDouble(pos));
                        break;
                    case Reference:
                        result = new FxReference(new ReferencedContent(new FxPK(rs.getLong(pos), FxPK.MAX)));  // TODO!!
                        break;
                    case SelectOne:
                        FxSelectListItem oneItem = CacheAdmin.getEnvironment().getSelectListItem(rs.getLong(pos));
                        result = new FxSelectOne(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, oneItem);
                        break;
                    case SelectMany:
                        FxSelectListItem manyItem = CacheAdmin.getEnvironment().getSelectListItem(rs.getLong(pos));
                        SelectMany valueMany = new SelectMany(manyItem.getList());
                        valueMany.selectFromList(rs.getString(pos + 1));
                        result = new FxSelectMany(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, valueMany);
                        //System.out.println("xp: "+xpath+" => "+valueMany);
                        break;
                    case Binary:
                        result = new FxBinary(entry.isMultilanguage(), FxLanguage.SYSTEM_ID, new BinaryDescriptor());
                        break;
                    default:
                        throw new FxSqlSearchException(LOG, "ex.sqlSearch.reader.UnknownColumnType",
                                String.valueOf(entry.getProperty().getDataType()));
                }
            }

            // Handle xpath and null values
            if (result instanceof FxValue) {
                FxValue fx_res = (FxValue) result;
                fx_res.setXPath(xpath);
                if (rs.wasNull()) fx_res.setEmpty(lang.getId());
            } else {
                if (rs.wasNull()) {
                    result = null;
                }
            }

            // all done ;-)
            return result;

        } catch (SQLException exc) {
            throw new FxSqlSearchException(LOG, exc, "ex.sqlSearch.reader.failedToReadValue", pos,
                    String.valueOf(entry.getProperty().getDataType()));
        }
    }


}
