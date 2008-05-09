/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.ejb.beans;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.*;
import com.flexive.core.storage.StorageManager;
import com.flexive.core.storage.genericSQL.GenericTreeStorage;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxUpdateException;
import com.flexive.shared.interfaces.SequencerEngine;
import com.flexive.shared.interfaces.TemplateEngine;
import com.flexive.shared.interfaces.TemplateEngineLocal;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.tree.FxTemplateInfo;
import com.flexive.shared.tree.FxTemplateMapping;
import com.flexive.shared.tree.FxTreeMode;
import static com.flexive.shared.tree.FxTreeMode.Edit;
import static com.flexive.shared.tree.FxTreeMode.Live;
import com.flexive.shared.tree.FxTreeNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless(name = "TemplateEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class TemplateEngineBean implements TemplateEngine, TemplateEngineLocal {

    private static transient Log LOG = LogFactory.getLog(TemplateEngineBean.class);
    @Resource
    javax.ejb.SessionContext ctx;
    @EJB
    private SequencerEngine seq;


    private void registerChange(Type type, FxTreeMode mode) {
        try {
            CacheAdmin.getInstance().put(this.getClass().getName() + ".lastChange",
                    mode + "_" + type.toString(), java.lang.System.currentTimeMillis());
            CacheAdmin.getInstance().put(this.getClass().getName() + ".lastChange",
                    mode + "_" + "global", java.lang.System.currentTimeMillis());
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long getLastChange(Type type, FxTreeMode mode) {    // TODO
        String key = mode + "_" + (type == null ? "global" : type.toString());
        try {
            Long lastChange = (Long) CacheAdmin.getInstance().get(this.getClass().getName() + ".lastChange", key);
            if (lastChange == null) {
                registerChange(type, mode);
                lastChange = (Long) CacheAdmin.getInstance().get(this.getClass().getName() + ".lastChange", key);
            }
            return lastChange;
        } catch (Exception e) {
            LOG.error(e);
            return java.lang.System.currentTimeMillis();
        }
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long create(String name, Type type, String contentType, String content) throws FxApplicationException {

        Connection con = null;
        PreparedStatement ps = null;
        long newId = -1;
        int typeid = 0;
        try {
            newId = seq.getId(SequencerEngine.System.TEMPLATE);
            // Determine the next type id to use, fill holes!
            con = Database.getDbConnection();
            ps = con.prepareStatement("SELECT TYPEID from " + TBL_TEMPLATE + " where TEMPLATE_TYPE='" +
                    type.getDbValue() + "'");
            ResultSet rs = ps.executeQuery();
            while (rs != null && rs.next()) {
                if (rs.getInt(1) > typeid) {
                    break;
                }
                typeid++;
            }
            ps.close();
        } catch (FxApplicationException t) {
            throw t;
        } catch (Exception e) {
            ctx.setRollbackOnly();
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.create", name);
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TreeEngineBean.class, con, ps);
        }

        _create(newId, typeid, Edit, name, type, contentType, content, false);
        return newId;
    }

    /**
     * @param id
     * @param typeid
     * @param mode
     * @param name
     * @param type
     * @param contentType
     * @param content
     * @param inSync
     * @throws FxApplicationException
     */
    private void _create(long id, int typeid, FxTreeMode mode, String name, Type type, String contentType, String content,
                         boolean inSync) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            // Do sanity checks
            checkTagName(type, name);
            FxTemplateInfo master = getMasterTemplate(type, content, mode);
            StringBuffer finalContent = new StringBuffer(content);
            ArrayList<FxTemplateInfo> tags = getTags(finalContent, mode);
            UserTicket ticket = FxContext.get().getTicket();

            // Create the entry
            con = Database.getDbConnection();
            ps = con.prepareStatement("INSERT INTO " + TBL_TEMPLATE +
                    // 1, 2  ,  3         ,  4    ,  5       , 6         , 7         ,  8
                    // 9            ,10
                    "(ID,TYPEID,NAME,CONTENT_TYPE,CONTENT,CREATED_BY ,CREATED_AT,MODIFIED_BY,MODIFIED_AT," +
                    "TEMPLATE_TYPE,MASTER_TEMPLATE,FINAL_CONTENT,ISLIVE,INSYNC) " +
                    "values (?,?,?,?,?,?,?,?,?,?,?,?," + (mode == Live) + "," + inSync + ")");
            final long NOW = System.currentTimeMillis();
            ps.setLong(1, id);
            ps.setInt(2, typeid);
            ps.setString(3, name);
            ps.setString(4, contentType);
            ps.setString(5, content);
            ps.setLong(6, ticket.getUserId());
            ps.setLong(7, NOW);
            ps.setLong(8, ticket.getUserId());
            ps.setLong(9, NOW);
            ps.setString(10, String.valueOf(type.getDbValue()));
            if (master == null) {
                ps.setNull(11, java.sql.Types.INTEGER);
            } else {
                ps.setLong(11, master.getId());
            }
            ps.setString(12, finalContent.toString());
            ps.executeUpdate();
            ps.close();

            // Update inSync in Edit AND Live version (if available)
            ps = con.prepareStatement("UPDATE " + TBL_TEMPLATE + " SET INSYNC=" + inSync + " WHERE ID=" + id);
            ps.executeUpdate();

            registerTagRelations(con, id, mode, tags);
            registerChange(type, mode);
        } catch (FxApplicationException t) {
            throw t;
        } catch (Exception e) {
            ctx.setRollbackOnly();
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.create", name);
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, ps);
        }
    }

    /**
     * Removes the template with the given ID.
     *
     * @param id the template ID
     * @throws FxApplicationException if the template could not be deleted
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(long id) throws FxApplicationException {
        // TODO: security!
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = Database.getDbConnection();
            stmt = con.prepareStatement("DELETE FROM " + TBL_TEMPLATE + " WHERE id=?");
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            ctx.setRollbackOnly();
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.delete", id);
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, stmt);
        }
    }


    /**
     * Activates the template with the given id.
     *
     * @param id the id of the template
     * @throws FxApplicationException if the function fails
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void activate(long id) throws FxApplicationException {
        // Check if the template exists at all
        FxTemplateInfo self = getInfo(id, Edit);
        if (self == null) {
            throw new FxNotFoundException("ex.templateEngine.notFound", id);
        }
        // Activate the template
        Connection con = null;
        Statement stmt = null;
        try {

            // Delete any old LIVE version
            con = Database.getDbConnection();
            stmt = con.createStatement();
            stmt.executeUpdate("DELETE FROM " + TBL_TEMPLATE + " WHERE ID=" + id + " AND ISLIVE=true");

            // Try to create the new LIVE version
            FxTemplateInfo info = getInfo(id, Edit);
            String content = getContent(id, Edit);
            _create(id, (int) info.getTypeId(), Live, info.getName(), info.getTemplateType(), info.getContentType(),
                    content, true);

        } catch (Exception e) {
            ctx.setRollbackOnly();
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.activate", id);
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, stmt);
        }
    }


    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setContent(long id, String content, String type, FxTreeMode mode) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            FxTemplateInfo self = getInfo(id, mode);
            FxTemplateInfo master = getMasterTemplate(self.getTemplateType(), content, mode);
            StringBuffer finalContent = new StringBuffer(content);
            ArrayList<FxTemplateInfo> tags = getTags(finalContent, mode);
            UserTicket ticket = FxContext.get().getTicket();
            con = Database.getDbConnection();
            ps = con.prepareStatement("UPDATE " + TBL_TEMPLATE + " set CONTENT_TYPE=?, CONTENT=?, " +
                    " MODIFIED_AT=?,MODIFIED_BY=?,MASTER_TEMPLATE=?,FINAL_CONTENT=? " +
                    " where id=" + id + " and islive=" + (mode == Live));
            ps.setString(1, type);
            ps.setString(2, content);
            ps.setLong(3, System.currentTimeMillis());
            ps.setLong(4, ticket.getUserId());
            if (master == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setLong(5, master.getId());
            }
            ps.setString(6, finalContent.toString());
            ps.executeUpdate();
            ps.close();

            // Check if the live and edit versions are in sync (=equal)
            ps = con.prepareStatement("select\n" +
                    "((select content from " + TBL_TEMPLATE + " where id=" + id + " and islive=false)=\n" +
                    "(select content from " + TBL_TEMPLATE + " where id=" + id + " and islive=true))");
            ResultSet rs = ps.executeQuery();
            rs.next();
            boolean inSync = rs.getBoolean(1);
            ps.close();

            // Update the inSync flag of both versions
            ps = con.prepareStatement("UPDATE " + TBL_TEMPLATE + " set INSYNC=" + inSync + " where id=" + id);
            ps.executeUpdate();
            ps.close();
            ps = null;

            // Update relations and the last change date
            registerTagRelations(con, id, mode, tags);
            registerChange(self.getTemplateType(), mode);
        } catch (FxApplicationException e) {
            throw e;
        } catch (Exception e) {
            ctx.setRollbackOnly();
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.setContent", id);
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, ps);
        }
    }

    /**
     * Stores the tag relations in the database.
     *
     * @param con  the connection to use
     * @param id   the id of the template
     * @param tags the tags the template is using
     * @param mode tree mode
     * @throws FxUpdateException if a error occurs
     */
    private void registerTagRelations(Connection con, long id, FxTreeMode mode, ArrayList<FxTemplateInfo> tags) throws FxUpdateException {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.executeUpdate("DELETE FROM " + TBL_TAG_RELATIONS + " where TEMPLATE_ID=" + id + " and TEMPLATE_ISLIVE=" + (mode == Live));
            stmt.close();

            for (FxTemplateInfo tag : tags) {
                stmt = con.createStatement();
                stmt.executeUpdate("INSERT INTO " + TBL_TAG_RELATIONS + " (TEMPLATE_ID,TAG_ID,TEMPLATE_ISLIVE) " +
                        " value (" + id + "," + tag.getId() + "," + (mode == Live) + ")");
                stmt.close();
            }
        } catch (Exception e) {
            ctx.setRollbackOnly();
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.setTagRelations", id, e.getMessage());
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, null, stmt);
        }
    }


    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean templateIsReferenced(long id) throws FxLoadException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = Database.getDbConnection();
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("select\n" +
                    "(select count(*) from " + TBL_TAG_RELATIONS + " where tag_id=" + id + ") +\n" +
                    "(select count(*) from " + TBL_TEMPLATE + " where MASTER_TEMPLATE=" + id + ")");
            rs.next();
            long count = rs.getLong(1);
            return count > 0;
        } catch (Exception e) {
            ctx.setRollbackOnly();
            FxLoadException exc = new FxLoadException(e, "ex.templateEngine.templateIsReferenced", id, e.getMessage());
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, stmt);
        }

    }


    /**
     * Returns a list of all tags used in the template, and modifies the content to the
     * final_content.
     *
     * @param content the content
     * @param mode    tree mode
     * @return the tag list and a modified content
     * @throws FxApplicationException if the function fails
     */
    private ArrayList<FxTemplateInfo> getTags(final StringBuffer content, FxTreeMode mode) throws FxApplicationException {
        // Compile the regex.
        Pattern pattern = Pattern.compile("<cms:([^\\>])+>");

        // A list of all used tags
        ArrayList<FxTemplateInfo> tags = new ArrayList<FxTemplateInfo>(25);
        Hashtable<String, Boolean> dups = new Hashtable<String, Boolean>(25);
        Hashtable<String, FxTemplateInfo> replace = new Hashtable<String, FxTemplateInfo>(25);

        // Get a Matcher based on the content string
        Matcher matcher = pattern.matcher(content);

        // Find all the matches.        
        while (matcher.find()) {
            String tagName = matcher.group().split(" ")[0].substring(5);
            Object exists = dups.put(tagName, Boolean.TRUE);
            try {
                FxTemplateInfo nfo = getInfo(tagName, mode);
                if (nfo == null) throw new Exception("notFoundDummy");
                if (exists == null) {
                    tags.add(nfo);
                }
                replace.put(matcher.group(), nfo);
            } catch (Exception e) {
                FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.invalidTag", tagName);
                LOG.debug(exc);
                throw exc;
            }
        }

        // Replace the user-tags with the internal tags
        String result = content.toString();
        for (String userTag : replace.keySet()) {
            while (result.indexOf(userTag) != -1) {
                String suffix = userTag.substring(replace.get(userTag).getName().length() + 5);
                String prefix = "<cms:tag" + replace.get(userTag).getTypeId() + " ";
                result = StringUtils.replace(result, userTag, prefix + suffix);
            }
        }
        content.delete(0, content.length());
        content.append(result);

        return tags;
    }

    /**
     * Returns the master template used by the content, or null if none is set.
     *
     * @param type    the template type
     * @param content the content
     * @param mode    tree mode
     * @return the name of the master template
     * @throws FxApplicationException when the referenced master template is invalid
     */
    private FxTemplateInfo getMasterTemplate(final Type type, final String content, FxTreeMode mode) throws FxApplicationException {
        FxTemplateInfo master = null;
        String sMaster = null;
        if (type == Type.CONTENT) try {
            // Extract the name of the master template
            sMaster = content.split("<ui:composition[\\s]*template[\\s]*=[\\s]*\"")[1].split("\"[\\s]*>")[0];
        } catch (Exception e) {
            /* its okay */
        }
        if (sMaster != null) try {
            // Check if the template exists
            master = getInfo(sMaster, mode);
            // Throw null pointer if the mast is not defined
            master.getName();
        } catch (Exception e) {
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.invalidMasterTemplate", sMaster);
            LOG.debug(exc);
            throw exc;
        }
        return master;
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setName(long id, String name) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            FxTemplateInfo nfo = getInfo(id, Edit); // TODO: Refs! Live Mode!!!
            checkTagName(nfo.getTemplateType(), name);
            UserTicket ticket = FxContext.get().getTicket();
            con = Database.getDbConnection();
            ps = con.prepareStatement("UPDATE " + TBL_TEMPLATE + " set NAME=?,MODIFIED_AT=?," +
                    "MODIFIED_BY=? where id=" + id);
            ps.setString(1, name);
            ps.setLong(2, System.currentTimeMillis());
            ps.setLong(3, ticket.getUserId());
            ps.executeUpdate();
            registerChange(nfo.getTemplateType(), Live);
            registerChange(nfo.getTemplateType(), Edit);
        } catch (FxApplicationException t) {
            throw t;
        } catch (Exception e) {
            ctx.setRollbackOnly();
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.setName", id);
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, ps);
        }
    }

    private void checkTagName(Type type, String name) throws FxUpdateException {
        if (type == Type.TAG) {
            if (name.split("\\s").length > 1) {
                throw new FxUpdateException("ex.templateEngine.invalidTagNameNoWhitespace");
            }
        }
    }


    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxTemplateInfo getInfo(long id, FxTreeMode mode) throws FxApplicationException {
        return _getInfo(id, null, mode);
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxTemplateInfo getInfo(String name, FxTreeMode mode) throws FxApplicationException {
        return _getInfo(null, name, mode);
    }

    private FxTemplateInfo _getInfo(Long id, String name, FxTreeMode mode) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Database.getDbConnection();

            String condition = " ";
            if (id != null) condition += "id=" + id;
            if (id != null && name != null) condition += " and ";
            if (name != null) condition += "name=?";

            String sSql = "SELECT ID,TYPEID,CONTENT_TYPE,MODIFIED_BY,MODIFIED_AT,NAME,TEMPLATE_TYPE,MASTER_TEMPLATE,\n" +
                    // Last modification Timestamp
                    "IF(master_template is null,null,(select sub.modified_at from " +
                    TBL_TEMPLATE + " sub where sub.id=t.master_template)),\n" +
                    // Does this template have an LIVE version?
                    "(select 1 from " + TBL_TEMPLATE + " where " + condition + " and islive=true),\n" +
                    "INSYNC \n" +
                    " FROM " + TBL_TEMPLATE + " t where islive=" + (mode == Live) + " and " + condition;


            ps = con.prepareStatement(sSql);
            if (name != null) {
                ps.setString(1, name);
                ps.setString(2, name);
            }
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                long _id = rs.getLong(1);
                int _typeid = rs.getInt(2);
                String _type = rs.getString(3);
                long _modifiedBy = rs.getLong(4);
                long _modifiedAt = rs.getLong(5);
                String _name = rs.getString(6);
                Type _ttype = Type.fromString(rs.getString(7));
                Long _master = rs.getLong(8);
                if (rs.wasNull()) {
                    _master = null;
                }
                final long tstp = rs.getLong(9);
                long _masterMod = -1;
                if (!rs.wasNull())
                    _masterMod = tstp;
                boolean hasLive = rs.getLong(10) == 1;
                boolean inSync = rs.getBoolean(11);

                return new FxTemplateInfo(_id, _typeid, _name, _type, _modifiedAt, _modifiedBy, -1, _ttype, _master,
                        _masterMod, mode == Live, hasLive, inSync);
            } else {
                throw new FxNotFoundException("ex.templateEngine.notFound.mode", id, mode);
            }
        } catch (Exception e) {
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.load", id);
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, ps);
        }
    }


    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<FxTemplateInfo> list(Type type) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        ArrayList<FxTemplateInfo> result = new ArrayList<FxTemplateInfo>(250);
        try {
            con = Database.getDbConnection();
            stmt = con.createStatement();
            String sSql = "SELECT id,typeid,CONTENT_TYPE,MODIFIED_BY,MODIFIED_AT,NAME,TEMPLATE_TYPE,MASTER_TEMPLATE,\n" +
                    "IF(master_template is null,-1,(select sub.modified_at from " + TBL_TEMPLATE + " sub where sub.id=t.master_template)),\n" +
                    "(select 1 from " + TBL_TEMPLATE + " sub where sub.id=t.id and islive=true),\n " +
                    "INSYNC\n" +
                    " FROM " +
                    TBL_TEMPLATE + " t ";
            if (type != null) {
                sSql += " where template_type='" + type.toString() + "' and islive=false";
            }
            ResultSet rs = stmt.executeQuery(sSql);
            while (rs.next()) {
                long id = rs.getLong(1);
                int typeid = rs.getInt(2);
                String contentType = rs.getString(3);
                long modifiedBy = rs.getLong(4);
                long modifiedAt = rs.getLong(5);
                String name = rs.getString(6);
                Type _ttype = Type.fromString(rs.getString(7));
                Long _master = rs.getLong(8);
                if (rs.wasNull()) {
                    _master = null;
                }
                final long tstp = rs.getLong(9);
                long _masterMod = -1;
                if (!rs.wasNull())
                    _masterMod = tstp;
                boolean hasLive = rs.getInt(10) == 1;
                boolean inSync = rs.getBoolean(11);
                result.add(new FxTemplateInfo(id, typeid, name, contentType, modifiedAt, modifiedBy,
                        -1, _ttype, _master, _masterMod, false, hasLive, inSync));
            }
            return result;
        } catch (Exception e) {
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.list");
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, stmt);
        }
    }


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private String _getContent(long id, boolean getFinal, FxTreeMode mode) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = Database.getDbConnection();
            stmt = con.createStatement();
            String selectCol = getFinal ? "ifnull(FINAL_CONTENT,content)" : "content";
            ResultSet rs = stmt.executeQuery("SELECT " + selectCol + " FROM " + TBL_TEMPLATE + " where id=" + id +
                    " and islive=" + (mode == Live));
            if (rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }
        } catch (Exception e) {
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.loadContent", id);
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getContent(long id, FxTreeMode mode) throws FxApplicationException {
        return _getContent(id, false, mode);
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getFinalContent(long id, String masterTemplateFile, FxTreeMode mode) throws FxApplicationException {
        return _setMasterTemplateFile(_getContent(id, true, mode), masterTemplateFile);
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getContent(String templateName, FxTreeMode mode) throws FxApplicationException {
        return _getContent(templateName, false, mode);
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    // TODO: ???
    public String getFinalContent(String templateName, String masterTemplateFile, FxTreeMode mode) throws FxApplicationException {
        return _setMasterTemplateFile(_getContent(templateName, true, mode), masterTemplateFile);
    }

    /**
     * Helper function, replaces the internal template handle by the provided one.
     *
     * @param content            the content
     * @param masterTemplateFile the final template file
     * @return the processed content
     */
    private String _setMasterTemplateFile(String content, String masterTemplateFile) {
        if (masterTemplateFile == null) {
            return content;
        }
        return content.replaceFirst("<ui:composition[\\s]*template[\\s]*=[\\s]*\"[^\"]*\">",
                "<ui:composition template=\"" + masterTemplateFile + "\">");
    }


    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private String _getContent(String templateName, boolean getFinal, FxTreeMode mode) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Database.getDbConnection();
            String selectCol = getFinal ? "ifnull(FINAL_CONTENT,content)" : "content";
            ps = con.prepareStatement("SELECT " + selectCol + " FROM " + TBL_TEMPLATE + " where name=?"
                    + " and islive=" + (mode == Live));
            ps.setString(1, templateName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }
        } catch (Exception e) {
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.loadContent", templateName);
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setTemplateMappings(long nodeId, List<FxTemplateMapping> map) throws FxApplicationException {
        String encodedTemplate = null;
        if (map != null) {
            encodedTemplate = "";
            for (FxTemplateMapping m : map) {
                String type = m.getContentType() == null ? "*" : m.getContentType().toString();
                encodedTemplate += ((encodedTemplate.length() == 0) ? "" : ";") + type + ":" + m.getTemplateId();
            }
        }
        Connection con = null;
        try {
            con = Database.getDbConnection();
            //TODO: Edit Tree??
            StorageManager.getTreeStorage().setData(con, FxTreeMode.Edit, nodeId, encodedTemplate);
        } catch (Exception e) {
            ctx.setRollbackOnly();
            FxUpdateException exc = new FxUpdateException(e, "ex.tree.setTemplate.failed", nodeId);  // TODO
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, null);
        }
    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public ArrayList<FxTemplateMapping> getTemplateMappings(long treeNodeId, FxTreeMode mode) throws FxApplicationException {
        FxTreeNode node = new TreeEngineBean().getNode(mode, treeNodeId);
        if (node.getData() == null || node.getData().length() == 0) {
            return new ArrayList<FxTemplateMapping>(0);
        }

        ArrayList<FxTemplateMapping> result = new ArrayList<FxTemplateMapping>(25);
        for (String templateMap : node.getData().split(";")) {
            String decode[] = templateMap.split(":");
            FxTemplateMapping mp = new FxTemplateMapping(
                    decode[0].equals("*") ? null : Long.valueOf(decode[0]),
                    Long.valueOf(decode[1]));
            result.add(mp);
        }
        return result;

    }

    /**
     * {@inheritDoc} *
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxTemplateInfo getTemplate(long treeNodeId, FxTreeMode mode) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        long contentType = 0;
        long contentId = -1;
        long templateId = -1;
        long parentNode = -1;
        String encodedTemplates = null;
        try {
            con = Database.getDbConnection();
            stmt = con.createStatement();
            final String VERSION = mode == Live ? "islive_ver=true" : "ismax_ver=true";
            ResultSet rs = stmt.executeQuery(" select c.tdef,t.ref, getTemplates(t.id,false),t.parent from " +
                    TBL_CONTENT + " c," + GenericTreeStorage.getTable(mode) + " t where " +
                    "t.id=" + treeNodeId + " and t.ref=c.id and c." + VERSION);
            if (rs.next()) {
                contentType = rs.getLong(1);
                contentId = rs.getLong(2);
                // Read the encoded template String (eg. "infotypeId1:template1,infotypeId2:template2,....")
                encodedTemplates = rs.getString(3);
                parentNode = rs.getLong(4);
                if (rs.wasNull()) parentNode = -1;
            }
            // No templates defined at all?
            if (encodedTemplates == null) {
                return null;
            }
            // Determine if any template fits
            String sct = contentType + ":";
            long fallbackTemplate = -1;
            for (String encodedTemplate : encodedTemplates.split(",")) {
                for (String templateMap : encodedTemplate.split(";")) {
                    if (templateMap.startsWith("*:")) {
                        if (fallbackTemplate == -1) {
                            fallbackTemplate = Long.valueOf(templateMap.split(":")[1]);
                        }
                    } else if (templateMap.startsWith(sct)) {
                        templateId = Long.valueOf(templateMap.split(":")[1]);
                        break;
                    }
                }
                if (templateId != -1) break;
            }

            // Use fallback?
            if (templateId == -1) {
                templateId = fallbackTemplate;
            }

            // Return template
            FxTemplateInfo result;
            if (templateId == -1) {
                // No template found for the node, still return a info object
                // whith the parent node, content id and content type infos
                result = new FxTemplateInfo(null, mode == Live, mode == Live, false);
            } else {
                // Load the template data
                result = getInfo(templateId, mode);
            }
            result.setParentNode(parentNode);
            result.setContentId(contentId);
            result.setTdef(contentType);
            return result;
        } catch (Exception e) {
            FxUpdateException exc = new FxUpdateException(e, "ex.templateEngine.loadForTreeNode", treeNodeId);
            LOG.error(exc);
            throw exc;
        } finally {
            Database.closeObjects(TemplateEngineBean.class, con, stmt);
        }
    }


}
