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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.tree.FxTemplateInfo;
import com.flexive.shared.tree.FxTemplateMapping;
import com.flexive.shared.tree.FxTreeMode;

import javax.ejb.Remote;
import java.util.ArrayList;
import java.util.List;

@Remote
/**
 * Template Engine interface
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface TemplateEngine {

    public static enum Type {
        MASTER('M'), CONTENT('C'), TAG('T');
        private char dbValue;

        Type(char dbValue) {
            this.dbValue = dbValue;
        }

        public char getDbValue() {
            return dbValue;
        }

        @Override
        public String toString() {
            return String.valueOf(dbValue);
        }

        public static Type fromString(String s) {
            if (s == null) return null;
            if (s.equalsIgnoreCase(MASTER.toString())) {
                return MASTER;
            } else if (s.equalsIgnoreCase(TAG.toString())) {
                return TAG;
            } else {
                return CONTENT;
            }
        }
    }

    /**
     * Returns the last change time of any item matching the given type.
     *
     * @param type the type, may be null for the last 'global' change timestamp
     * @param mode tree mode
     * @return the last change time
     */
    public long getLastChange(Type type, FxTreeMode mode);

    public void activate(long id) throws FxApplicationException;

    public long create(String name, Type type, String contentType, String content) throws FxApplicationException;

    /**
     * Sets the content of the template
     *
     * @param id      the templqates id
     * @param content the content to set
     * @param type    the template type
     * @param mode    tree mode
     * @throws FxApplicationException if the function fails
     */
    public void setContent(long id, String content, String type, FxTreeMode mode) throws FxApplicationException;


    /**
     * Returns true if the template is used by other templates.
     *
     * @param id the id of the templates
     * @return true if the template is used by other templates.
     * @throws FxLoadException if the function failed
     */
    public boolean templateIsReferenced(long id) throws FxLoadException;

    public FxTemplateInfo getInfo(long id, FxTreeMode mode) throws FxApplicationException;

    public FxTemplateInfo getInfo(String name, FxTreeMode mode) throws FxApplicationException;

    /**
     * Renames a existing template.
     *
     * @param id   the id of the template to rename
     * @param name the new name
     * @throws FxApplicationException if the rename failed
     */
    public void setName(long id, String name) throws FxApplicationException;

    /**
     * Loads all defined templates
     *
     * @param type the template type filter, or null
     * @return the found templates
     * @throws FxApplicationException if a error occured
     */
    public ArrayList<FxTemplateInfo> list(Type type) throws FxApplicationException;

    /**
     * Retrives the content from the template.
     * <p/>
     * This function returns the content represented to the users for editing, while the getFinalContent(id)  function
     * returns the text that is use by the application server to generate the pages.
     *
     * @param id   the template id
     * @param mode tree mode
     * @return the content
     * @throws FxApplicationException if the function fails
     */
    public String getContent(long id, FxTreeMode mode) throws FxApplicationException;

    /**
     * Retrives the final content from the template.
     * <p/>
     * The "final" content is the text that is use by the application server to generate the pages.
     *
     * @param id                 the template id
     * @param mode               tree mode
     * @param masterTemplateFile The absolute filesystem name of the master template file
     * @return the final content
     * @throws FxApplicationException if the function fails
     */
    public String getFinalContent(long id, String masterTemplateFile, FxTreeMode mode) throws FxApplicationException;

    /**
     * Retrives the content from the template.
     * <p/>
     * This function returns the content represented to the users for editing, while the getFinalContent(id)  function
     * returns the text that is use by the application server to generate the pages.
     *
     * @param templateName the template name
     * @param mode         tree mode
     * @return the final content
     * @throws FxApplicationException if the function fails
     */
    public String getContent(String templateName, FxTreeMode mode) throws FxApplicationException;

    /**
     * Retrives the final content from the template.
     * <p/>
     * The "final" content is the text that is use by the application server to generate the pages.
     *
     * @param templateName       the template name
     * @param masterTemplateFile The absolute filesystem name of the master template file
     * @return the final content
     * @throws FxApplicationException if the function fails
     */
    public String getFinalContent(String templateName, String masterTemplateFile) throws FxApplicationException;

    public ArrayList<FxTemplateMapping> getTemplateMappings(long treeNodeId, FxTreeMode mode) throws FxApplicationException;

    /**
     * Gets the template for the specified node id.
     *
     * @param treeNodeId the tree node id to get the template for.
     * @param mode       tree mode
     * @return the template infos
     * @throws FxApplicationException if the function fails
     */
    public FxTemplateInfo getTemplate(long treeNodeId, FxTreeMode mode) throws FxApplicationException;

    public void setTemplateMappings(long nodeId, List<FxTemplateMapping> map) throws FxApplicationException;
}
