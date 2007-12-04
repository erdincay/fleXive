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
package com.flexive.core.storage.genericSQL;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.*;
import com.flexive.core.LifeCycleInfoImpl;
import com.flexive.core.storage.EnvironmentLoader;
import com.flexive.core.structure.FxEnvironmentImpl;
import com.flexive.core.structure.FxPreloadGroupAssignment;
import com.flexive.core.structure.FxPreloadType;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptMapping;
import com.flexive.shared.scripting.FxScriptMappingEntry;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.shared.workflow.Route;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.workflow.Workflow;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * generic sql environment loader implementation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class GenericEnvironmentLoader implements EnvironmentLoader {

    protected static final transient Log LOG = LogFactory.getLog(GenericEnvironmentLoader.class);

    /**
     * {@inheritDoc}
     */
    public List<ACL> loadACLs(Connection con) throws FxLoadException {
        Statement stmt = null;
        String curSql;
        ArrayList<ACL> result = new ArrayList<ACL>(250);
        try {
            //                            1      2          3             4                5          6                 7
            curSql = "SELECT DISTINCT acl.ID, acl.NAME, acl.CAT_TYPE, acl.DESCRIPTION, acl.COLOR, acl.MANDATOR, mand.NAME from \n" +
                    TBL_ACLS + " acl, " + TBL_MANDATORS + " mand WHERE mand.ID=acl.MANDATOR";
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(curSql);
            while (rs != null && rs.next()) {
                try {
                    result.add(new ACL(rs.getInt(1), rs.getString(2),
                            Database.loadFxString(con, TBL_ACLS, "LABEL", "ID=" + rs.getInt(1)),
                            rs.getInt(6), rs.getString(7), rs.getString(4), rs.getString(5), rs.getInt(3)));
                } catch (FxInvalidParameterException e) {
                    throw new FxLoadException(LOG, e);
                }
            }
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "Failed to load all ACLs: " + exc.getMessage(), exc);
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Mandator[] loadMandators(Connection con) throws FxLoadException {

        PreparedStatement ps = null;
        try {
            // Load all mandators within the system
            ps = con.prepareStatement("SELECT ID,NAME,METADATA,IS_ACTIVE,CREATED_BY,CREATED_AT,MODIFIED_BY,MODIFIED_AT FROM " + TBL_MANDATORS + " order by upper(NAME)");
            ResultSet rs = ps.executeQuery();
            ArrayList<Mandator> result = new ArrayList<Mandator>(20);
            while (rs != null && rs.next()) {
                result.add(new Mandator(rs.getInt(1), rs.getString(2), rs.getInt(3),
                        rs.getBoolean(4), LifeCycleInfoImpl.load(rs, 5, 6, 7, 8)));
            }
            // return the result
            return result.toArray(new Mandator[result.size()]);
        } catch (SQLException se) {
            FxLoadException le = new FxLoadException(se.getMessage(), se);
            LOG.error(le);
            throw le;
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<FxDataType> loadDataTypes(Connection con) throws FxLoadException {
        Statement stmt = null;
        ArrayList<FxDataType> alRet = new ArrayList<FxDataType>(20);
        try {
            String sql = "SELECT d.TYPECODE, d.NAME, t.LANG, t.DESCRIPTION FROM " + TBL_STRUCT_DATATYPES + " d, " +
                    TBL_STRUCT_DATATYPES + ML + " t WHERE t.ID=d.TYPECODE ORDER BY d.TYPECODE, t.LANG ASC";
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            FxDataType dtCurr = null;
            HashMap<Integer, String> hmDesc = new HashMap<Integer, String>(5);
            String name = null;
            while (rs != null && rs.next()) {
                if (dtCurr != null && rs.getLong(1) != dtCurr.getId()) {
                    dtCurr.initialize(name, new FxString(FxLanguage.DEFAULT_ID, hmDesc));
                    alRet.add(dtCurr);
                    hmDesc.clear();
                    dtCurr = null;
                }
                if (dtCurr == null)
                    for (FxDataType dt : FxDataType.values()) {
                        if (dt.getId() == rs.getInt(1)) {
                            dtCurr = dt;
                            break;
                        }
                    }
                if (dtCurr == null)
                    throw new FxLoadException(LOG, "ex.structure.dataType.unknownId", rs.getInt(1));
                hmDesc.put(rs.getInt(3), rs.getString(4));
                name = rs.getString(2);
            }
            if (dtCurr != null) {
                dtCurr.initialize(name, new FxString(FxLanguage.DEFAULT_ID, hmDesc));
                alRet.add(dtCurr);
            }
            return alRet;
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<FxGroup> loadGroups(Connection con) throws FxLoadException {
        Statement stmt = null;
        ArrayList<FxGroup> alRet = new ArrayList<FxGroup>(50);
        try {
            final Map<Long, List<FxStructureOption>> groupOptions = loadAllGroupOptions(con);
            //final List<FxStructureOption> emptyGroupOptions = new ArrayList<FxStructureOption>(0);
            //                     1     2       3             4             5                  6       7              8
            final String sql = "SELECT g.ID, g.NAME, g.DEFMINMULT, g.DEFMAXMULT, g.MAYOVERRIDEMULT, t.LANG, t.DESCRIPTION, t.HINT FROM " +
                    TBL_STRUCT_GROUPS + " g, " + TBL_STRUCT_GROUPS + ML + " t WHERE t.ID=g.ID ORDER BY g.ID, t.LANG ASC";
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            HashMap<Integer, String> hmDesc = new HashMap<Integer, String>(5);
            HashMap<Integer, String> hmHint = new HashMap<Integer, String>(5);
            String name = null;
            long id = -1;
            int minMult = -1;
            int maxMult = -1;
            boolean mayOverride = false;
            while (rs != null && rs.next()) {
                if (name != null && rs.getLong(1) != id) {
                    alRet.add(new FxGroup(id, name, new FxString(FxLanguage.DEFAULT_ID, hmDesc),
                            new FxString(FxLanguage.DEFAULT_ID, hmHint), mayOverride,
                            new FxMultiplicity(minMult, maxMult), FxSharedUtils.get(groupOptions, id, new ArrayList<FxStructureOption>(0))));
                    hmDesc.clear();
                }

                if (hmDesc.size() == 0) {
                    id = rs.getLong(1);
                    name = rs.getString(2);
                    minMult = rs.getInt(3);
                    maxMult = rs.getInt(4);
                    mayOverride = rs.getBoolean(5);
                }
                hmDesc.put(rs.getInt(6), rs.getString(7));
                hmHint.put(rs.getInt(6), rs.getString(8));
            }
            if (hmDesc.size() > 0) {
                alRet.add(new FxGroup(id, name, new FxString(FxLanguage.DEFAULT_ID, hmDesc),
                        new FxString(FxLanguage.DEFAULT_ID, hmHint), mayOverride,
                        new FxMultiplicity(minMult, maxMult), FxSharedUtils.get(groupOptions, id, new ArrayList<FxStructureOption>(0))));
            }
            return alRet;
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<FxProperty> loadProperties(Connection con, FxEnvironment environment) throws FxLoadException, FxNotFoundException {
        Statement stmt = null;
        ArrayList<FxProperty> alRet = new ArrayList<FxProperty>(50);
        try {
            final Map<Long, List<FxStructureOption>> propertyOptions = loadAllPropertyOptions(con);
            //final List<FxStructureOption> emptyPropertyOptions = new ArrayList<FxStructureOption>(0);

            //                     1     2       3             4             5                  6       7
            String sql = "SELECT p.ID, p.NAME, p.DEFMINMULT, p.DEFMAXMULT, p.MAYOVERRIDEMULT, t.LANG, t.DESCRIPTION, " +
                    // 8      9                 10          11         12              13
                    "p.ACL, p.MAYOVERRIDEACL, p.DATATYPE, p.REFTYPE, p.ISSEARCHABLE, p.MAYOVERRIDESEARCH, " +
                    // 14              15                     16               17
                    "p.ISINOVERVIEW, p.MAYOVERRIDEOVERVIEW, p.USEHTMLEDITOR, p.MAYOVERRIDEHTMLEDITOR, " +
                    // 18                   19                      20             21               22     23
                    "p.ISFULLTEXTINDEXED, p.MAYOVERRIDEMULTILANG, p.ISMULTILANG, t.DEFAULT_VALUE, t.HINT, p.SYSINTERNAL, " +
                    //24          25
                    "p.REFLIST, p.UNIQUEMODE FROM " +
                    TBL_STRUCT_PROPERTIES + " p, " + TBL_STRUCT_PROPERTIES + ML + " t WHERE t.ID=p.ID ORDER BY p.ID, t.LANG ASC";
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            HashMap<Integer, String> hmDesc = new HashMap<Integer, String>(5);
            HashMap<Integer, String> hmHint = new HashMap<Integer, String>(5);
            HashMap<Integer, String> hmDefault = new HashMap<Integer, String>(5);
            String name = null;
            long id = -1;
            int minMult = -1;
            int maxMult = -1;
            boolean mayOverrideMult = false;
            boolean mayOverrideACL = false;
            boolean mayOverrideMultiLang = false;
            boolean mayOverrideSearchable = false;
            boolean mayOverrideOverview = false;
            boolean mayOverrideHTMLEditor = false;
            ACL acl = null;
            FxDataType dataType = null;
            boolean multiLang = false;
            boolean searchable = false;
            boolean overview = false;
            boolean useHTMLEditor = false;
            boolean fulltextIndexed = false;
            long refTypeId = -1;
            long refListId = -1;
            boolean systemInternal = false;
            UniqueMode uniqueMode = UniqueMode.None;

            while (rs != null && rs.next()) {
                if (name != null && rs.getLong(1) != id) {
//                    if( !name.startsWith("TEST"))
//                        System.out.println("=======> Loaded: "+name);
                    alRet.add(new FxProperty(id, name, new FxString(FxLanguage.DEFAULT_ID, hmDesc),
                            new FxString(FxLanguage.DEFAULT_ID, hmHint), systemInternal, mayOverrideMult,
                            new FxMultiplicity(minMult, maxMult), mayOverrideACL, acl, dataType,
                            new FxString(FxLanguage.DEFAULT_ID, hmDefault),
                            fulltextIndexed, (refTypeId == -1 ? null : environment.getType(refTypeId)),
                            (refListId == -1 ? null : environment.getSelectList(refListId)), uniqueMode,
                            FxSharedUtils.get(propertyOptions, id, new ArrayList<FxStructureOption>(0))));
                    hmDesc.clear();
                }

                if (hmDesc.size() == 0) {
                    id = rs.getLong(1);
                    name = rs.getString(2);
                    minMult = rs.getInt(3);
                    maxMult = rs.getInt(4);
                    mayOverrideMult = rs.getBoolean(5);
                    acl = environment.getACL(rs.getInt(8));
                    mayOverrideACL = rs.getBoolean(9);
                    dataType = environment.getDataType(rs.getLong(10));
                    refTypeId = rs.getLong(11);
                    if (rs.wasNull())
                        refTypeId = -1;
                    refListId = rs.getLong(24);
                    if (rs.wasNull())
                        refListId = -1;
                    searchable = rs.getBoolean(12);
                    mayOverrideSearchable = rs.getBoolean(13);
                    overview = rs.getBoolean(14);
                    mayOverrideOverview = rs.getBoolean(15);
                    useHTMLEditor = rs.getBoolean(16);
                    mayOverrideHTMLEditor = rs.getBoolean(17);
                    fulltextIndexed = rs.getBoolean(18);
                    mayOverrideMultiLang = rs.getBoolean(19);
                    multiLang = rs.getBoolean(20);
                    systemInternal = rs.getBoolean(23);
                    uniqueMode = UniqueMode.getById(rs.getInt(25));
                }
                hmDesc.put(rs.getInt(6), rs.getString(7));
                hmDefault.put(rs.getInt(6), rs.getString(21));
                hmHint.put(rs.getInt(6), rs.getString(22));
            }
            if (hmDesc.size() > 0) {
//                if( !name.startsWith("TEST"))
//                        System.out.println("=======> Loaded: "+name);
                alRet.add(new FxProperty(id, name, new FxString(FxLanguage.DEFAULT_ID, hmDesc),
                        new FxString(FxLanguage.DEFAULT_ID, hmHint), systemInternal, mayOverrideMult,
                        new FxMultiplicity(minMult, maxMult), mayOverrideACL, acl, dataType,
                        new FxString(FxLanguage.DEFAULT_ID, hmDefault),
                        fulltextIndexed, (refTypeId == -1 ? null : environment.getType(refTypeId)),
                        (refListId == -1 ? null : environment.getSelectList(refListId)), uniqueMode,
                        FxSharedUtils.get(propertyOptions, id, new ArrayList<FxStructureOption>(0))));
            }
            return alRet;
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
    }

    /**
     * Load options for a group
     *
     * @param con an open and valid connection
     * @param id  id of the group
     * @return options
     */
    private List<FxStructureOption> loadGroupOptions(Connection con, long id) {
        //TODO: codeme!!
        return FxStructureOption.getEmptyOptionList(5);
    }

    /**
     * Load options for a group assignment
     *
     * @param con an open and valid connection
     * @param id  id of the group assignment
     * @return options
     */
    private List<FxStructureOption> loadGroupAssignmentOptions(Connection con, long id) {
        //TODO: codeme!
        return FxStructureOption.getEmptyOptionList(5);
    }

    /**
     * Load all options for group assignments
     *
     * @param con an open and valid connection
     * @return options
     * @throws SQLException on errors
     */
    private Map<Long, List<FxStructureOption>> loadAllGroupAssignmentOptions(Connection con) throws SQLException {
        return loadAllOptions(con, "ASSID", "ASSID IS NOT NULL", TBL_GROUP_OPTIONS);
    }

    /**
     * Load all options for groups
     *
     * @param con an open and valid connection
     * @return options
     * @throws SQLException on errors
     */
    private Map<Long, List<FxStructureOption>> loadAllGroupOptions(Connection con) throws SQLException {
        return loadAllOptions(con, "ID", "ASSID IS NULL", TBL_GROUP_OPTIONS);
    }

    private Map<Long, List<FxStructureOption>> loadAllPropertyAssignmentOptions(Connection con) throws SQLException {
        return loadAllOptions(con, "ASSID", "ASSID IS NOT NULL", TBL_PROPERTY_OPTIONS);
    }

    private Map<Long, List<FxStructureOption>> loadAllPropertyOptions(Connection con) throws SQLException {
        return loadAllOptions(con, "ID", "ASSID IS NULL", TBL_PROPERTY_OPTIONS);
    }

    private Map<Long, List<FxStructureOption>> loadAllOptions(Connection con, String idColumn, String whereClause, String table) throws SQLException {
        Statement stmt = null;
        Map<Long, List<FxStructureOption>> result = new HashMap<Long, List<FxStructureOption>>();
        try {
            stmt = con.createStatement();
            final ResultSet rs = stmt.executeQuery("SELECT " + idColumn + ",OPTKEY,MAYOVERRIDE,OPTVALUE FROM "
                    + table + " WHERE " + whereClause);
            while (rs.next()) {
                final long id = rs.getLong(1);
                if (!result.containsKey(id)) {
                    result.put(id, new ArrayList<FxStructureOption>());
                }
                FxStructureOption.setOption(result.get(id), rs.getString(2), rs.getBoolean(3), rs.getString(4));
            }
            return result;
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<FxType> loadTypes(Connection con, FxEnvironment environment) throws FxLoadException {
        Statement stmt = null;
        PreparedStatement ps = null, ps2 = null;
        String curSql;
        ArrayList<FxType> result = new ArrayList<FxType>(20);
        try {
            ps = con.prepareStatement("select mandatorid from " + TBL_STRUCT_TYPES2MANDATORS + " where typeid=?");
            //                                 1         2       3        4
            ps2 = con.prepareStatement("select typesrc, typedst, maxsrc, maxdst from " + TBL_STRUCT_TYPERELATIONS + " where typedef=?");
            //               1   2         3     4       5             6         7          8
            curSql = "select id, mandator, name, parent, storage_mode, category, type_mode, validity_checks, " +
                    //9         10          11             12            13           14
                    "lang_mode, type_state, security_mode, trackhistory, history_age, max_versions," +
                    //15                 16                17          18          19           20           21   22
                    "rel_total_maxsrc, rel_total_maxdst, created_by, created_at, modified_by, modified_at, acl, workflow" +
                    " from " + TBL_STRUCT_TYPES + " order by name";

            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(curSql);
            ResultSet rsMand;
            ResultSet rsRelations;
            while (rs != null && rs.next()) {
                try {
                    ps.setLong(1, rs.getLong(1));
                    ps2.setLong(1, rs.getLong(1));
                    rsMand = ps.executeQuery();
                    ArrayList<Mandator> alMand = new ArrayList<Mandator>(10);
                    while (rsMand != null && rsMand.next()) {
                        alMand.add(environment.getMandator(rsMand.getInt(1)));
                    }
                    ArrayList<FxTypeRelation> alRelations = new ArrayList<FxTypeRelation>(10);
                    rsRelations = ps2.executeQuery();
                    while (rsRelations != null && rsRelations.next())
                        alRelations.add(new FxTypeRelation(new FxPreloadType(rsRelations.getLong(1)), new FxPreloadType(rsRelations.getLong(2)),
                                rsRelations.getInt(3), rsRelations.getInt(4)));
                    long parentId = rs.getLong(4);
                    FxType parentType = rs.wasNull() ? null : new FxPreloadType(parentId);
                    result.add(new FxType(rs.getLong(1), environment.getACL(rs.getInt(21)),
                            environment.getWorkflow(rs.getInt(22)), environment.getMandator(rs.getInt(2)), alMand, rs.getString(3),
                            Database.loadFxString(con, TBL_STRUCT_TYPES, "description", "id=" + rs.getLong(1)),
                            parentType, TypeStorageMode.getById(rs.getInt(5)),
                            TypeCategory.getById(rs.getInt(6)), TypeMode.getById(rs.getInt(7)), rs.getBoolean(8),
                            LanguageMode.getById(rs.getInt(9)), TypeState.getById(rs.getInt(10)), rs.getByte(11),
                            rs.getBoolean(12), rs.getLong(13), rs.getLong(14), rs.getInt(15), rs.getInt(16),
                            LifeCycleInfoImpl.load(rs, 17, 18, 19, 20), new ArrayList<FxType>(5), alRelations));
                } catch (FxNotFoundException e) {
                    throw new FxLoadException(LOG, e);
                }
            }
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "Failed to load all FxAssignments: " + exc.getMessage(), exc);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                //ignore
            }
            try {
                if (ps2 != null)
                    ps2.close();
            } catch (SQLException e) {
                //ignore
            }
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public List<FxAssignment> loadAssignments(Connection con, FxEnvironment environment) throws FxLoadException {
        Statement stmt = null;
        String curSql;
        ArrayList<FxAssignment> result = new ArrayList<FxAssignment>(250);
        try {
            final Map<Long, FxString[]> translations = Database.loadFxStrings(con, TBL_STRUCT_ASSIGNMENTS, new String[]{"DESCRIPTION", "HINT", "DEFAULT_VALUE"});
            final FxString[] emptyTranslation = new FxString[]{new FxString(""), new FxString(""), new FxString("")};
            final Map<Long, List<FxStructureOption>> propertyAssignmentOptions = loadAllPropertyAssignmentOptions(con);
            final Map<Long, List<FxStructureOption>> groupAssignmentOptions = loadAllGroupAssignmentOptions(con);
            //final List<FxStructureOption> emptyOptions = new ArrayList<FxStructureOption>(0);

            //               1   2      3        4        5        6        7    8      9
            curSql = "select id, atype, enabled, typedef, MINMULT, MAXMULT, pos, xpath, xalias, " +
                    //10          11      12         13   14          15           16            17             18    19       20           21         22
                    "parentgroup, agroup, aproperty, acl, inoverview, ismultilang, issearchable, usehtmleditor, base, deflang, SYSINTERNAL, GROUPMODE, DEFMULT from " +
                    TBL_STRUCT_ASSIGNMENTS + " order by typedef, parentgroup, pos";//atype, enabled";

            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(curSql);
            while (rs != null && rs.next()) {
                switch (rs.getInt(2)) {   //ATYPE
                    case FxAssignment.TYPE_GROUP:
                        if (rs.getLong(1) == FxAssignment.NO_PARENT)
                            break;
                        final FxString[] desc_hint = FxSharedUtils.get(translations, rs.getLong(1), emptyTranslation);//Database.loadFxString(con, TBL_STRUCT_ASSIGNMENTS, new String[]{"DESCRIPTION", "HINT"}, "ID=" + rs.getLong(1));
                        FxGroupAssignment ga = new FxGroupAssignment(rs.getLong(1), rs.getBoolean(3), environment.getType(rs.getLong(4)),
                                rs.getString(9), rs.getString(8), rs.getInt(7),
                                new FxMultiplicity(rs.getInt(5), rs.getInt(6)), rs.getInt(22),
                                new FxPreloadGroupAssignment(rs.getLong(10)),
                                rs.getLong(18), desc_hint[0], desc_hint[1],
                                environment.getGroup(rs.getLong(11)), GroupMode.getById(rs.getInt(21)),
                                FxSharedUtils.get(groupAssignmentOptions, rs.getLong(1), new ArrayList<FxStructureOption>(0)));
                        if (rs.getBoolean(20))
                            ga._setSystemInternal();
                        result.add(ga);
                        break;
                    case FxAssignment.TYPE_PROPERTY:
                        final FxString[] desc_hint_def = FxSharedUtils.get(translations, rs.getLong(1), emptyTranslation);//Database.loadFxString(con, TBL_STRUCT_ASSIGNMENTS, new String[]{"DESCRIPTION", "HINT", "DEFAULT_VALUE"}, "ID=" + rs.getLong(1));
//                        System.out.println("===> Loading assignment #"+rs.getLong(1)+": "+desc_hint_def[0].getDefaultTranslation() );
                        FxPropertyAssignment pa = new FxPropertyAssignment(rs.getLong(1), rs.getBoolean(3), environment.getType(rs.getLong(4)),
                                rs.getString(9), rs.getString(8), rs.getInt(7),
                                new FxMultiplicity(rs.getInt(5), rs.getInt(6)), rs.getInt(22),
                                new FxPreloadGroupAssignment(rs.getLong(10)),
                                rs.getLong(18),
                                desc_hint_def[0], desc_hint_def[1], desc_hint_def[2],
                                environment.getProperty(rs.getLong(12)),
                                environment.getACL(rs.getInt(13)), rs.getInt(19),
                                FxSharedUtils.get(propertyAssignmentOptions, rs.getLong(1), new ArrayList<FxStructureOption>(0)));
                        if (rs.getBoolean(20))
                            pa._setSystemInternal();
                        result.add(pa);
                        break;
                    default:
                        LOG.error("Invalid assignment type " + rs.getInt(2) + " for assignment #" + rs.getLong(1));
                }
            }
            for (FxAssignment as : result)
                as.resolvePreloadDependencies(result);
            for (FxAssignment as : result)
                as.resolveParentDependencies(result);
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "Failed to load all FxAssignments: " + exc.getMessage(), exc);
        } catch (FxNotFoundException e) {
            throw new FxLoadException(e);
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public List<Workflow> loadWorkflows(Connection con, FxEnvironment environment) throws FxLoadException {
        Statement stmt = null;
        final String sql = "SELECT ID, NAME, DESCRIPTION FROM " + TBL_WORKFLOW;
        try {
            // Create the new workflow instance
            stmt = con.createStatement();
            // Read all defined workflows
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            ArrayList<Workflow> tmp = new ArrayList<Workflow>(10);
            while (rs != null && rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                String description = rs.getString(3);
                ArrayList<Step> wfSteps = new ArrayList<Step>(5);
                for (Step step : environment.getSteps()) {
                    if (step.getWorkflowId() == id) {
                        wfSteps.add(step);
                    }
                }
                tmp.add(new Workflow(id, name, description, wfSteps, loadRoutes(con, id)));
            }
            return tmp;
        } catch (SQLException exc) {
            String sErr = "Unable to retrieve workflows";
            LOG.error(sErr + ", sql=" + sql);
            throw new FxLoadException(sErr, exc);
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<StepDefinition> loadStepDefinitions(Connection con) throws FxLoadException {
        Statement stmt = null;
        try {
            // Read all stepDefinitions from the database
            stmt = con.createStatement();
            //                                             1  2           3
            final ResultSet rs = stmt.executeQuery("SELECT ID,DESCRIPTION,UNIQUE_TARGET FROM " + TBL_STEPDEFINITION);
            ArrayList<StepDefinition> tmp = new ArrayList<StepDefinition>(10);

            // Build the result array set
            while (rs != null && rs.next()) {
                int id = rs.getInt(1);
                String description = rs.getString(2);
                int uniqueTargetId = rs.getInt(3);
                if (rs.wasNull()) {
                    uniqueTargetId = -1;
                }
                FxString name = Database.loadFxString(con, TBL_STEPDEFINITION, "name", "id=" + id);
                StepDefinition aStepDef = new StepDefinition(id, name, description, uniqueTargetId);
                tmp.add(aStepDef);
            }
            return tmp;
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "Unable to read steps definitions", exc);
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Step> loadSteps(Connection con) throws FxLoadException {
        Statement stmt = null;
        //                                      1      2               3                4
        final String sql = "SELECT DISTINCT stp.ID, stp.WORKFLOW, stp.STEPDEF,stp.ACL " +
                "FROM " + TBL_STEP + " stp";
        try {
            // Load all steps in the database
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            ArrayList<Step> steps = new ArrayList<Step>(30);
            while (rs != null && rs.next())
                steps.add(new Step(rs.getLong(1), rs.getLong(3), rs.getLong(2), rs.getLong(4)));

            return steps;
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "Unable to read the steps from the database. " +
                    "Error=" + exc.getMessage() + ", sql=" + sql, exc);
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Route> loadRoutes(Connection con, int workflowId) throws FxLoadException {
        //                             1     2               3             4
        final String sql = "SELECT ro.ID,ro.FROM_STEP,ro.TO_STEP,ro.USERGROUP " +
                "FROM " + TBL_ROUTES + " ro, " + TBL_STEP + " stp " +
                "WHERE ro.TO_STEP=stp.ID AND stp.WORKFLOW=" + workflowId + " " +
                "ORDER BY ro.USERGROUP ASC";

        if (LOG.isDebugEnabled()) LOG.debug("getRoute(" + workflowId + ")=" + sql);

        Statement stmt = null;
        try {
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            ArrayList<Route> routes = new ArrayList<Route>(50);

            // Process result set
            while (rs != null && rs.next()) {
                long routeId = rs.getLong(1);
                long fromId = rs.getLong(2);
                long toId = rs.getLong(3);
                long groupId = rs.getLong(4);
                Route route = new Route(routeId, groupId, fromId, toId);
                routes.add(route);
            }

            return routes;
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "Unable to load routes for workflow [" + workflowId + "], msg=" +
                    exc.getMessage() + ", sql=" + sql, exc);
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<FxScriptInfo> loadScripts(Connection con) throws FxLoadException, FxNotFoundException, FxInvalidParameterException {
        PreparedStatement ps = null;
        String sql;
        List<FxScriptInfo> scripts = new ArrayList<FxScriptInfo>(10);
        try {
            //            1  2     3     4     5
            sql = "SELECT ID,SNAME,SDESC,SDATA,STYPE FROM " + TBL_SCRIPTS + " ORDER BY ID";
            ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs != null && rs.next())
                scripts.add(new FxScriptInfo(rs.getLong(1), FxScriptEvent.getById(rs.getInt(5)), rs.getString(2),
                        rs.getString(3), rs.getString(4)));
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, exc, "ex.scripting.load.failed", -1, exc.getMessage());
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, ps);
        }
        return scripts;
    }

    /**
     * {@inheritDoc}
     */
    public List<FxScriptMapping> loadScriptMapping(Connection con, FxEnvironmentImpl environment) throws FxLoadException {
        List<FxScriptMapping> mapping = new ArrayList<FxScriptMapping>(20);
        List<FxScriptMappingEntry> e_ass;
        List<FxScriptMappingEntry> e_types;
        PreparedStatement ps_a = null, ps_t = null;
        String sql;
        try {
            //            1          2             3      4
            sql = "SELECT ASSIGNMENT,DERIVED_USAGE,ACTIVE,STYPE FROM " + TBL_SCRIPT_MAPPING_ASSIGN + " WHERE SCRIPT=?";
            ps_a = con.prepareStatement(sql);
            sql = "SELECT TYPEDEF,DERIVED_USAGE,ACTIVE,STYPE FROM " + TBL_SCRIPT_MAPPING_TYPES + " WHERE SCRIPT=?";
            ps_t = con.prepareStatement(sql);
            ResultSet rs;
            for (FxScriptInfo si : environment.getScripts()) {
                ps_a.setLong(1, si.getId());
                ps_t.setLong(1, si.getId());
                rs = ps_a.executeQuery();
                e_ass = new ArrayList<FxScriptMappingEntry>(20);
                e_types = new ArrayList<FxScriptMappingEntry>(20);
                while (rs != null && rs.next()) {
                    long[] derived;
                    if (!rs.getBoolean(2))
                        derived = new long[0];
                    else {
                        List<FxAssignment> ass = environment.getDerivedAssignments(rs.getLong(1));
                        derived = new long[ass.size()];
                        for (int i = 0; i < ass.size(); i++)
                            derived[i] = ass.get(i).getId();
                    }
                    e_ass.add(new FxScriptMappingEntry(FxScriptEvent.getById(rs.getInt(4)), si.getId(), rs.getBoolean(3), rs.getBoolean(2), rs.getLong(1), derived));
                }
                rs = ps_t.executeQuery();
                while (rs != null && rs.next()) {
                    long[] derived;
                    if (!rs.getBoolean(2))
                        derived = new long[0];
                    else {
                        List<FxType> types = environment.getType(rs.getLong(1)).getDerivedTypes();
                        derived = new long[types.size()];
                        for (int i = 0; i < types.size(); i++)
                            derived[i] = types.get(i).getId();
                    }
                    e_types.add(new FxScriptMappingEntry(FxScriptEvent.getById(rs.getInt(4)), si.getId(), rs.getBoolean(3), rs.getBoolean(2), rs.getLong(1), derived));
                }
                mapping.add(new FxScriptMapping(si.getId(), e_types, e_ass));
            }

        } catch (SQLException exc) {
            throw new FxLoadException(LOG, exc, "ex.scripting.mapping.load.failed", exc.getMessage());
        } catch (FxNotFoundException e) {
            throw new FxLoadException(e);
        } finally {
            try {
                if (ps_t != null)
                    ps_t.close();
            } catch (SQLException e) {
                //ignore
            }
            Database.closeObjects(GenericEnvironmentLoader.class, null, ps_a);
        }
        return mapping;
    }

    /**
     * {@inheritDoc}
     */
    public List<FxSelectList> loadSelectLists(Connection con, FxEnvironmentImpl environment) throws FxLoadException {
        PreparedStatement ps = null;
        String sql;
        List<FxSelectList> lists = new ArrayList<FxSelectList>(10);
        try {
            final Map<Long, FxString[]> translations = Database.loadFxStrings(con, TBL_SELECTLIST, new String[]{"LABEL", "DESCRIPTION"});
            final FxString[] emptyTranslation = new FxString[]{new FxString(""), new FxString("")};
            final Map<Long, FxString[]> itemTranslations = Database.loadFxStrings(con, TBL_SELECTLIST_ITEM, new String[]{"LABEL"});
            final FxString[] emptyItemTranslation = new FxString[]{new FxString(""), new FxString(""), new FxString("")};

            //            1  2        3    4                 5               6            7
            sql = "SELECT ID,PARENTID,NAME,ALLOW_ITEM_CREATE,ACL_CREATE_ITEM,ACL_ITEM_NEW,DEFAULT_ITEM FROM " +
                    TBL_SELECTLIST + " ORDER BY NAME";
            ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs != null && rs.next()) {
                FxString[] strings = FxSharedUtils.get(translations, rs.getLong(1), emptyTranslation);
                long parent = rs.getLong(2);
                if (rs.wasNull())
                    parent = -1;
                lists.add(new FxSelectList(rs.getLong(1), parent, rs.getString(3), strings[0], strings[1],
                        rs.getBoolean(5), environment.getACL(rs.getLong(5)), environment.getACL(rs.getLong(6)),
                        rs.getLong(7)));
            }
            ps.close();
            //            1  2   3        4    5     6          7          8           9           10      11       12
            sql = "SELECT ID,ACL,PARENTID,DATA,COLOR,CREATED_BY,CREATED_AT,MODIFIED_BY,MODIFIED_AT,DBIN_ID,DBIN_VER,DBIN_QUALITY FROM " +
                    TBL_SELECTLIST_ITEM + " WHERE LISTID=? ORDER BY ID";
            ps = con.prepareStatement(sql);
            for (FxSelectList list : lists) {
                ps.setLong(1, list.getId());
                rs = ps.executeQuery();
                while (rs != null && rs.next()) {
                    long parent = rs.getLong(3);
                    if (rs.wasNull())
                        parent = -1;
                    new FxSelectListItem(rs.getLong(1), environment.getACL(rs.getLong(2)), list, parent,
                            FxSharedUtils.get(itemTranslations, rs.getLong(1), emptyItemTranslation)[0],
                            rs.getString(4), rs.getString(5), rs.getLong(10), rs.getInt(11), rs.getInt(12),
                            LifeCycleInfoImpl.load(rs, 6, 7, 8, 9));
                }
            }
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, exc, "ex.structure.list.load.failed", exc.getMessage());
        } finally {
            Database.closeObjects(GenericEnvironmentLoader.class, null, ps);
        }
        return lists;
    }
}
