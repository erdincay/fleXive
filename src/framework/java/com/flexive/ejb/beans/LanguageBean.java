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
package com.flexive.ejb.beans;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidLanguageException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.interfaces.LanguageEngine;
import com.flexive.shared.interfaces.LanguageEngineLocal;
import com.flexive.shared.mbeans.FxCacheMBean;
import com.flexive.shared.value.FxString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [fleXive] language engine interface.
 * This engine should not be used to load languages as they are available from the environment!
 * Its purpose is to enable/disable, initially load and manage (position, etc.) languages.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */

@Stateless(name = "LanguageEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class LanguageBean implements LanguageEngine, LanguageEngineLocal {

    private static transient Log LOG = LogFactory.getLog(LanguageBean.class);

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxLanguage load(long languageId) throws FxApplicationException {
        try {
            FxLanguage lang = (FxLanguage) CacheAdmin.getInstance().get(CacheAdmin.LANGUAGES_ID, languageId);
            if (lang == null) {
                loadAll(true, true);
                lang = (FxLanguage) CacheAdmin.getInstance().get(CacheAdmin.LANGUAGES_ID, languageId);
                if (lang == null)
                    throw new FxInvalidLanguageException("ex.language.invalid", languageId);
            }
            return lang;
        } catch (FxCacheException e) {
            throw new FxLoadException(LOG, e);
        }
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxLanguage load(String languageIsoCode) throws FxApplicationException {
        try {
            FxLanguage lang = (FxLanguage) CacheAdmin.getInstance().get(CacheAdmin.LANGUAGES_ISO, languageIsoCode);
            if (lang == null) {
                loadAll(true, true);
                lang = (FxLanguage) CacheAdmin.getInstance().get(CacheAdmin.LANGUAGES_ISO, languageIsoCode);
                if (lang == null)
                    throw new FxInvalidLanguageException("ex.language.invalid", languageIsoCode);
            }
            return lang;
        } catch (FxCacheException e) {
            throw new FxLoadException(LOG, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxLanguage[] loadAvailable() throws FxApplicationException {
        try {
            FxLanguage[] available = (FxLanguage[]) CacheAdmin.getInstance().get(CacheAdmin.LANGUAGES_ALL, "id");
            if (available == null) {
                loadAll(true, true);
                available = (FxLanguage[]) CacheAdmin.getInstance().get(CacheAdmin.LANGUAGES_ALL, "id");
                if (available == null)
                    throw new FxInvalidLanguageException("ex.language.loadFailed");
            }
            return available;
        } catch (FxCacheException e) {
            throw new FxLoadException(LOG, e);
        }
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<FxLanguage> loadDisabled() throws FxApplicationException {
        return loadAll(false, false);
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<FxLanguage> loadAvailable(boolean excludeSystemLanguage) throws FxApplicationException {
        FxLanguage[] tmp = loadAvailable();
        ArrayList<FxLanguage> result = new ArrayList<FxLanguage>();
        for (FxLanguage lang : tmp) {
            if (excludeSystemLanguage && lang.getId() == 0) continue;
            result.add(lang);
        }
        return result;
    }


    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean isValid(long languageId) {
        // Does the language exist at all? Check via constru
        try {
            load(languageId);
        } catch (FxApplicationException exc) {
            return false;
        }
        return true;
    }

    /**
     * Initial load function.
     *
     * @param used        load used or unused languages?
     * @param add2cache put loaded languages into cache?
     * @return list containing requested languages
     */
    private synchronized List<FxLanguage> loadAll(boolean used, boolean add2cache) {
        String sql = "SELECT l.LANG_CODE, l.ISO_CODE, t.LANG, t.DESCRIPTION FROM " + DatabaseConst.TBL_LANG + " l, " +
                DatabaseConst.TBL_LANG + DatabaseConst.ML + " t " +
                "WHERE t.LANG_CODE=l.LANG_CODE AND l.INUSE=" + used + " ORDER BY l.DISPPOS ASC, l.LANG_CODE ASC";
        Connection con = null;
        Statement stmt = null;
        List<FxLanguage> alLang = new ArrayList<FxLanguage>(140);
        try {
            con = Database.getDbConnection();
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            Map<Long, String> hmMl = new HashMap<Long, String>(5);
            int lang_code = -1;
            String iso_code = null;
            FxCacheMBean cache = CacheAdmin.getInstance();
            while (rs != null && rs.next()) {
                if (lang_code != rs.getInt(1)) {
                    if (lang_code != -1 && lang_code != FxLanguage.SYSTEM_ID) {
                        //add
                        FxLanguage lang = new FxLanguage(lang_code, iso_code, new FxString(FxLanguage.DEFAULT_ID, hmMl), true);
                        if (add2cache) {
                            cache.put(CacheAdmin.LANGUAGES_ID, lang.getId(), lang);
                            cache.put(CacheAdmin.LANGUAGES_ISO, lang.getIso2digit(), lang);
                        }
                        alLang.add(lang);
                    }
                    lang_code = rs.getInt(1);
                    iso_code = rs.getString(2);
                    hmMl.clear();
                }
                hmMl.put(rs.getLong(3), rs.getString(4));
            }
            if (lang_code != -1 && lang_code != FxLanguage.SYSTEM_ID) {
                //add
                FxLanguage lang = new FxLanguage(lang_code, iso_code, new FxString(FxLanguage.DEFAULT_ID, hmMl), true);
                if (add2cache) {
                    cache.put(CacheAdmin.LANGUAGES_ID, lang.getId(), lang);
                    cache.put(CacheAdmin.LANGUAGES_ISO, lang.getIso2digit(), lang);
                }
                alLang.add(lang);
            }
            if (add2cache)
                cache.put(CacheAdmin.LANGUAGES_ALL, "id", alLang.toArray(new FxLanguage[alLang.size()]));
        } catch (SQLException e) {
            LOG.error(e, e);
        } catch (FxCacheException e) {
            LOG.error(e, e);
        } finally {
            if (con != null)
                Database.closeObjects(LanguageBean.class, con, stmt);
        }
        return alLang;
    }
}

