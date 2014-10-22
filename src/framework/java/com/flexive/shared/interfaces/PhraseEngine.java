/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2012
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
package com.flexive.shared.interfaces;

import com.flexive.shared.*;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxEntryInUseException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.value.FxString;

import javax.ejb.Remote;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

/**
 * Phrase engine
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @since 3.2.0
 */
@SuppressWarnings("UnusedDeclaration")
@Remote
public interface PhraseEngine {

    //phrase handling

    /**
     * Save or create a phrase (tag will be removed existing phrases!)
     *
     * @param phraseKey phrase key
     * @param value     phrase value
     * @param mandator  phrase mandator
     * @return phrase id (for the mandator)
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public long savePhrase(String phraseKey, FxString value, long mandator) throws FxNoAccessException;

    /**
     * Save or create a phrase (tag will be removed existing phrases!)
     *
     * @param category  phrase category
     * @param phraseKey phrase key
     * @param value     phrase value
     * @param mandator  phrase mandator
     * @return phrase id (for the mandator)
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public long savePhrase(int category, String phraseKey, FxString value, long mandator) throws FxNoAccessException;

    /**
     * Save or create a phrase (tag will be removed existing phrases!)
     *
     * @param phraseKey phrase key
     * @param value     phrase value
     * @param converter converter for value used for search operations
     * @param mandator  phrase mandator
     * @return phrase id (for the mandator)
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public long savePhrase(String phraseKey, FxString value, FxPhraseSearchValueConverter converter, long mandator) throws FxNoAccessException;

    /**
     * Save or create a phrase
     *
     * @param phraseKey phrase key
     * @param value     phrase value
     * @param tag       String if single language or FxString if multilanguage
     * @param mandator  phrase mandator
     * @return phrase id (for the mandator)
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public long savePhrase(String phraseKey, FxString value, Object tag, long mandator) throws FxNoAccessException;

    /**
     * Save or create a phrase
     *
     * @param category  phrase category
     * @param phraseKey phrase key
     * @param value     phrase value
     * @param tag       String if single language or FxString if multilanguage
     * @param mandator  phrase mandator
     * @return phrase id (for the mandator)
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public long savePhrase(int category, String phraseKey, FxString value, Object tag, long mandator) throws FxNoAccessException;

    /**
     * Save or create a phrase
     *
     * @param phraseKey phrase key
     * @param value     phrase value
     * @param converter converter for value used for search operations
     * @param tag       String if single language or FxString if multilanguage
     * @param mandator  phrase mandator
     * @return phrase id (for the mandator)
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public long savePhrase(String phraseKey, FxString value, FxPhraseSearchValueConverter converter, Object tag, long mandator) throws FxNoAccessException;

    /**
     * Save or create a phrase
     *
     * @param category  phrase category
     * @param phraseKey phrase key
     * @param value     phrase value
     * @param converter converter for value used for search operations
     * @param tag       String if single language or FxString if multilanguage
     * @param mandator  phrase mandator
     * @return phrase id (for the mandator)
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public long savePhrase(int category, String phraseKey, FxString value, FxPhraseSearchValueConverter converter, Object tag, long mandator) throws FxNoAccessException;

    /**
     * Set a phrases hidden flag
     *
     * @param phraseKey phrase key
     * @param mandator  phrase mandator
     * @param hidden    hidden flag
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public void setPhraseHidden(String phraseKey, long mandator, boolean hidden) throws FxNoAccessException;

    /**
     * Set a phrases hidden flag
     *
     * @param category  phrase category
     * @param phraseKey phrase key
     * @param mandator  phrase mandator
     * @param hidden    hidden flag
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public void setPhraseHidden(int category, String phraseKey, long mandator, boolean hidden) throws FxNoAccessException;

    /**
     * Remove the phrase identified by its key and mandator and all its mappings
     *
     * @param phraseKey phrase key
     * @param mandator  phrase mandator
     * @return if the phrase to be removed was found and successfully removed
     * @throws FxNoAccessException   if the requested mandator is not the callers mandator
     * @throws FxEntryInUseException if the phrase is in use by the tree
     */
    public boolean removePhrase(String phraseKey, long mandator) throws FxNoAccessException, FxEntryInUseException;

    /**
     * Remove the phrase identified by its key and mandator and all its mappings
     *
     * @param category  phrase category
     * @param phraseKey phrase key
     * @param mandator  phrase mandator
     * @return if the phrase to be removed was found and successfully removed
     * @throws FxNoAccessException   if the requested mandator is not the callers mandator
     * @throws FxEntryInUseException if the phrase is in use by the tree
     */
    public boolean removePhrase(int category, String phraseKey, long mandator) throws FxNoAccessException, FxEntryInUseException;

    /**
     * Remove all phrases with a key starting with phraseKeyPrefix and mandator
     *
     * @param phraseKeyPrefix phrase key prefix
     * @param mandator        phrase mandator
     * @return count of removed phrases
     * @throws FxNoAccessException   if the requested mandator is not the callers mandator
     * @throws FxEntryInUseException if the phrase is in use by the tree
     */
    public int removePhrases(String phraseKeyPrefix, long mandator) throws FxNoAccessException, FxEntryInUseException;

    /**
     * Remove all phrases with a key starting with phraseKeyPrefix and mandator
     *
     * @param category        phrase category
     * @param phraseKeyPrefix phrase key prefix
     * @param mandator        phrase mandator
     * @return count of removed phrases
     * @throws FxNoAccessException   if the requested mandator is not the callers mandator
     * @throws FxEntryInUseException if the phrase is in use by the tree
     */
    public int removePhrases(int category, String phraseKeyPrefix, long mandator) throws FxNoAccessException, FxEntryInUseException;

    /**
     * Removes all phrases of the requested mandator and all its mappings (but not if used in tree nodes!)
     *
     * @param mandator requested mandator
     * @return count of removed phrases
     * @throws FxNoAccessException   if the requested mandator is not the callers mandator
     * @throws FxEntryInUseException if phrases are in use by the tree
     */
    public int removeMandatorPhrases(long mandator) throws FxNoAccessException, FxEntryInUseException;

    /**
     * Removes all phrases of the requested mandator and all its mappings (but not if used in tree nodes!)
     *
     * @param categories phrase categories
     * @param mandator   requested mandator
     * @return count of removed phrases
     * @throws FxNoAccessException   if the requested mandator is not the callers mandator
     * @throws FxEntryInUseException if phrases are in use by the tree
     */
    public int removeMandatorPhrases(FxPhraseCategorySelection categories, long mandator) throws FxNoAccessException, FxEntryInUseException;

    /**
     * Load a phrase value in the requested language
     *
     * @param language  language of the value
     * @param phraseKey phrase key
     * @param mandators list of mandators to try loading the key from (in the requested order)
     * @return value
     * @throws FxNotFoundException if no value for the requested language was found or the phrase key does not exist
     */
    public String loadPhraseValue(long language, String phraseKey, long... mandators) throws FxNotFoundException;

    /**
     * Load all available translation of a phrase
     *
     * @param phraseKey phrase key
     * @param mandators list of mandators to try loading the key from (in the requested order)
     * @return multilingual value
     * @throws FxNotFoundException if the phrase key does not exist
     */
    public FxString loadPhraseValue(String phraseKey, long... mandators) throws FxNotFoundException;

    /**
     * Load first available FxPhrase instances of a phrase
     *
     * @param phraseKey phrase key
     * @param mandators list of mandators to try loading the key from (in the requested order)
     * @return FxPhrase
     * @throws FxNotFoundException if the phrase key does not exist
     */
    public FxPhrase loadPhrase(String phraseKey, long... mandators) throws FxNotFoundException;

    /**
     * Load first available FxPhrase instances of a phrase
     *
     * @param category  phrase category
     * @param phraseKey phrase key
     * @param mandators list of mandators to try loading the key from (in the requested order)
     * @return FxPhrase
     * @throws FxNotFoundException if the phrase key does not exist
     */
    public FxPhrase loadPhrase(int category, String phraseKey, long... mandators) throws FxNotFoundException;

    /**
     * Load all FxPhrase instances of a phrase with the requested key prefix
     *
     * @param phraseKeyPrefix phrase key prefix
     * @param mandators       list of mandators to try loading the key from (in the requested order)
     * @return found FxPhrases ordered by phraseKey
     */
    public List<FxPhrase> loadPhrases(String phraseKeyPrefix, long... mandators);

    /**
     * Load all FxPhrase instances of a phrase with the requested key prefix
     *
     * @param category        phrase category
     * @param phraseKeyPrefix phrase key prefix
     * @param mandators       list of mandators to try loading the key from (in the requested order)
     * @return found FxPhrases ordered by phraseKey
     */
    public List<FxPhrase> loadPhrases(int category, String phraseKeyPrefix, long... mandators);

    /**
     * Remove all phrases that belong to the requested mandator
     *
     * @param mandatorId requested mandator
     * @throws FxNoAccessException   if the requested mandator is not the callers mandator
     * @throws FxEntryInUseException if phrases are in use by the phrase tree
     */
    public void clearPhrases(long mandatorId) throws FxNoAccessException, FxEntryInUseException;

    /**
     * Remove all phrases that belong to the requested mandator
     *
     * @param categories categories to remove
     * @param mandatorId requested mandator
     * @throws FxNoAccessException   if the requested mandator is not the callers mandator
     * @throws FxEntryInUseException if phrases are in use by the phrase tree
     */
    public void clearPhrases(FxPhraseCategorySelection categories, long mandatorId) throws FxNoAccessException, FxEntryInUseException;

    /**
     * Synchronize the phrase sequencer to match the current max id + 1
     *
     * @param mandatorId mandator
     */
    public void syncPhraseSequencer(long mandatorId);

    //tree handling

    /**
     * Save or create a tree node.
     * Children are not updated using this method!
     *
     * @param node the node the create or update
     * @return node
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     * @throws FxNotFoundException if either the parent node or the phrase dont exist
     */
    public FxPhraseTreeNode saveTreeNode(FxPhraseTreeNode node) throws FxNoAccessException, FxNotFoundException;

    /**
     * Set the parent of a phrase tree node.
     * Parent node category must match the nodes!
     *
     * @param nodeId         node id
     * @param nodeMandator   node mandator
     * @param parentId       parent node id
     * @param parentMandator parent node mandator
     * @throws FxNoAccessException if the node mandator is not the calling users mandator
     * @throws FxNotFoundException if either of the nodes are not found
     */
    public void setPhraseTreeNodeParent(long nodeId, long nodeMandator, long parentId, long parentMandator) throws FxNoAccessException, FxNotFoundException;

    /**
     * Synchronize the phrase node sequencer to match the current max id + 1
     *
     * @param mandatorId mandator
     */
    public void syncPhraseNodeSequencer(long mandatorId);

    /**
     * Synchronize the phrase node sequencer to match the current max id + 1
     *
     * @param category   phrase category
     * @param mandatorId mandator
     */
    public void syncPhraseNodeSequencer(int category, long mandatorId);

    /**
     * Load all root nodes and child nodes for up to 2 mandators
     *
     * @param mandator2top insert nodes of mandator 2 at the top or bottom?
     * @param mandators    mandators
     * @return phrase tree
     */
    public List<FxPhraseTreeNode> loadPhraseTree(boolean mandator2top, long... mandators);

    /**
     * Load all root nodes and child nodes for up to 2 mandators
     *
     * @param category     phrase category
     * @param mandator2top insert nodes of mandator 2 at the top or bottom?
     * @param mandators    mandators
     * @return phrase tree
     */
    public List<FxPhraseTreeNode> loadPhraseTree(int category, boolean mandator2top, long... mandators);

    /**
     * Load a tree nodes and its child nodes for up to 2 mandators
     *
     * @param nodeId       node id
     * @param mandatorId   mandator id
     * @param mandator2top insert nodes of mandator 2 at the top or bottom?
     * @param mandators    mandators
     * @return phrase node and its children
     * @throws FxNotFoundException if no node with the requested id/mandator exists
     */
    public FxPhraseTreeNode loadPhraseTreeNode(long nodeId, long mandatorId, boolean mandator2top, long... mandators) throws FxNotFoundException;

    /**
     * Load a tree nodes and its child nodes for up to 2 mandators
     *
     * @param category     phrase category
     * @param nodeId       node id
     * @param mandatorId   mandator id
     * @param mandator2top insert nodes of mandator 2 at the top or bottom?
     * @param mandators    mandators
     * @return phrase node and its children
     * @throws FxNotFoundException if no node with the requested id/mandator exists
     */
    public FxPhraseTreeNode loadPhraseTreeNode(int category, long nodeId, long mandatorId, boolean mandator2top, long... mandators) throws FxNotFoundException;

    /**
     * Load a tree nodes without its children
     *
     * @param category     phrase category
     * @param nodeId       node id
     * @param mandatorId   mandator id
     * @return phrase node
     * @throws FxNotFoundException if no node with the requested id/mandator exists
     * @since 3.2.1
     */
    public FxPhraseTreeNode loadPhraseTreeNodeOnly(int category, long nodeId, long mandatorId) throws FxNotFoundException;


    /**
     * Move a tree node delta positions (within its own mandator)
     *
     * @param nodeId     node id
     * @param mandatorId mandator id of the node
     * @param delta      delat to move
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     * @throws FxNotFoundException if no node with the requested id/mandator exists
     */
    public void moveTreeNode(long nodeId, long mandatorId, int delta) throws FxNoAccessException, FxNotFoundException;

    /**
     * Move a tree node delta positions (within its own mandator)
     *
     * @param category   phrase category
     * @param nodeId     node id
     * @param mandatorId mandator id of the node
     * @param delta      delat to move
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     * @throws FxNotFoundException if no node with the requested id/mandator exists
     */
    public void moveTreeNode(int category, long nodeId, long mandatorId, int delta) throws FxNoAccessException, FxNotFoundException;

    /**
     * Remove a tree node, moving its children up one level and removing all assignments to this node
     *
     * @param nodeId     node id
     * @param mandatorId mandator of the node
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     * @throws FxNotFoundException if no node with the requested id/mandator exists
     */
    public void removeTreeNode(long nodeId, long mandatorId) throws FxNoAccessException, FxNotFoundException;

    /**
     * Remove a tree node, moving its children up one level and removing all assignments to this node
     *
     * @param category   phrase category
     * @param nodeId     node id
     * @param mandatorId mandator of the node
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     * @throws FxNotFoundException if no node with the requested id/mandator exists
     */
    public void removeTreeNode(int category, long nodeId, long mandatorId) throws FxNoAccessException, FxNotFoundException;

    /**
     * Remove all tree nodes that belong to the requested mandator
     *
     * @param mandatorId requested mandator
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public void clearTree(long mandatorId) throws FxNoAccessException;

    /**
     * Remove all tree nodes that belong to the requested mandator and match selected categories
     *
     * @param categories categories to remove
     * @param mandatorId requested mandator
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public void clearTree(FxPhraseCategorySelection categories, long mandatorId) throws FxNoAccessException;


    //phrase->tree mapping

    /**
     * Assign a phrase to a node.
     * If the assignment already exists, the position is updated if needed
     *
     * @param category         category
     * @param assignmentOwner  mandator which is the "owner" of this assignment
     * @param nodeId           node id
     * @param nodeMandator     node mandator
     * @param phraseId         phrase id
     * @param phraseMandator   phrase mandator
     * @param pos              position
     * @param checkPositioning insert at the given position moving other nodes or just write the position without any moving around
     * @throws FxNoAccessException if the requested assignmentOwner is not the callers mandator
     * @throws FxNotFoundException if either the node or phrase were not found
     */
    public void assignPhrase(int category, long assignmentOwner, long nodeId, long nodeMandator, long phraseId, long phraseMandator, long pos, boolean checkPositioning) throws FxNotFoundException, FxNoAccessException;

    /**
     * Assign a phrase to a node.
     * If the assignment already exists, the position is updated if needed
     *
     * @param assignmentOwner  mandator which is the "owner" of this assignment
     * @param nodeId           node id
     * @param nodeMandator     node mandator
     * @param phraseId         phrase id
     * @param phraseMandator   phrase mandator
     * @param pos              position
     * @param checkPositioning insert at the given position moving other nodes or just write the position without any moving around
     * @throws FxNoAccessException if the requested assignmentOwner is not the callers mandator
     * @throws FxNotFoundException if either the node or phrase were not found
     * @deprecated
     */
    public void assignPhrase(long assignmentOwner, long nodeId, long nodeMandator, long phraseId, long phraseMandator, long pos, boolean checkPositioning) throws FxNotFoundException, FxNoAccessException;

    /**
     * Assign a phrase to a node.
     * If the assignment already exists, the position is updated if needed
     *
     * @param category         category
     * @param assignmentOwner  mandator which is the "owner" of this assignment
     * @param nodeId           node id
     * @param nodeMandator     node mandator
     * @param phraseKey        phrase key
     * @param phraseMandator   phrase mandator
     * @param pos              position
     * @param checkPositioning insert at the given position moving other nodes or just write the position without any moving around
     * @throws FxNoAccessException if the requested assignmentOwner is not the callers mandator
     * @throws FxNotFoundException if either the node or phrase were not found
     */
    public void assignPhrase(int category, long assignmentOwner, long nodeId, long nodeMandator, String phraseKey, long phraseMandator, long pos, boolean checkPositioning) throws FxNotFoundException, FxNoAccessException;

    /**
     * Assign a phrase to a node for the default category.
     * If the assignment already exists, the position is updated if needed
     *
     * @param assignmentOwner  mandator which is the "owner" of this assignment
     * @param nodeId           node id
     * @param nodeMandator     node mandator
     * @param phraseKey        phrase key
     * @param phraseMandator   phrase mandator
     * @param pos              position
     * @param checkPositioning insert at the given position moving other nodes or just write the position without any moving around
     * @throws FxNoAccessException if the requested assignmentOwner is not the callers mandator
     * @throws FxNotFoundException if either the node or phrase were not found
     * @deprecated
     */
    public void assignPhrase(long assignmentOwner, long nodeId, long nodeMandator, String phraseKey, long phraseMandator, long pos, boolean checkPositioning) throws FxNotFoundException, FxNoAccessException;

    /**
     * Assign phrases at a given position to a node
     *
     * @param category        category
     * @param position        requested position
     * @param assignmentOwner mandator which is the "owner" of this assignment
     * @param nodeId          node id
     * @param nodeMandator    node mandator
     * @param phrases         phrases to assign
     * @throws FxApplicationException on errors
     */
    public void assignPhrases(int category, long position, long assignmentOwner, long nodeId, long nodeMandator, FxPhrase[] phrases) throws FxApplicationException;

    /**
     * Assign phrases at a given position to a node for the default category
     *
     * @param position        requested position
     * @param assignmentOwner mandator which is the "owner" of this assignment
     * @param nodeId          node id
     * @param nodeMandator    node mandator
     * @param phrases         phrases to assign
     * @throws FxApplicationException on errors
     * @deprecated
     */
    public void assignPhrases(long position, long assignmentOwner, long nodeId, long nodeMandator, FxPhrase[] phrases) throws FxApplicationException;


    /**
     * Build the indirect child mappings for a category and mandator
     *
     * @param mandator mandator
     * @param category category
     */
    public void buildPhraseChildMapping(long mandator, int category);

    /**
     * "Move" the position of an assignment for the default category
     *
     * @param assignmentOwner mandator which is the "owner" of this assignment
     * @param nodeId          node id
     * @param nodeMandatorId  node mandator
     * @param phraseId        phrase id
     * @param phraseMandator  phrase mandator
     * @param delta           delta to move
     * @throws FxNoAccessException if the requested assignmentOwner is not the callers mandator
     * @throws FxNotFoundException if either the node or phrase were not found
     * @deprecated
     */
    void moveTreeNodeAssignment(long assignmentOwner, long nodeId, long nodeMandatorId, long phraseId, long phraseMandator, int delta) throws FxNotFoundException, FxNoAccessException;

    /**
     * "Move" the position of an assignment
     *
     * @param category        phrase category
     * @param assignmentOwner mandator which is the "owner" of this assignment
     * @param nodeId          node id
     * @param nodeMandatorId  node mandator
     * @param phraseId        phrase id
     * @param phraseMandator  phrase mandator
     * @param delta           delta to move
     * @throws FxNoAccessException if the requested assignmentOwner is not the callers mandator
     * @throws FxNotFoundException if either the node or phrase were not found
     */
    void moveTreeNodeAssignment(int category, long assignmentOwner, long nodeId, long nodeMandatorId, long phraseId, long phraseMandator, int delta) throws FxNotFoundException, FxNoAccessException;

    /**
     * Remove an assign of a phrase from a node for the default category.
     *
     * @param assignmentOwner mandator which is the "owner" of this assignment
     * @param nodeId          node id
     * @param nodeMandator    node mandator
     * @param phraseId        phrase id
     * @param phraseMandator  phrase mandator
     * @return if an assignment was found that could be removed
     * @throws FxNoAccessException if the requested assignmentOwner is not the callers mandator
     * @throws FxNotFoundException if either the node or phrase were not found
     * @deprecated
     */
    public boolean removePhraseAssignment(long assignmentOwner, long nodeId, long nodeMandator, long phraseId, long phraseMandator) throws FxNotFoundException, FxNoAccessException;

    /**
     * Remove an assign of a phrase from a node.
     *
     * @param category        phrase category
     * @param assignmentOwner mandator which is the "owner" of this assignment
     * @param nodeId          node id
     * @param nodeMandator    node mandator
     * @param phraseId        phrase id
     * @param phraseMandator  phrase mandator
     * @return if an assignment was found that could be removed
     * @throws FxNoAccessException if the requested assignmentOwner is not the callers mandator
     * @throws FxNotFoundException if either the node or phrase were not found
     */
    public boolean removePhraseAssignment(int category, long assignmentOwner, long nodeId, long nodeMandator, long phraseId, long phraseMandator) throws FxNotFoundException, FxNoAccessException;

    /**
     * Remove all phrase assignments from a node for the default category.
     * If the calling user is the owner of the node, all assignments will be removed, if not only the phrases assigned by the calling users mandator
     *
     * @param nodeId       node id
     * @param nodeMandator node mandator id
     * @deprecated use #removeAssignmentsFromNode(int,long,long)
     */
    public void removeAssignmentsFromNode(long nodeId, long nodeMandator);

    /**
     * Remove all phrase assignments from a node.
     * If the calling user is the owner of the node, all assignments will be removed, if not only the phrases assigned by the calling users mandator
     *
     * @param category     phrase category
     * @param nodeId       node id
     * @param nodeMandator node mandator id
     */
    public void removeAssignmentsFromNode(int category, long nodeId, long nodeMandator);

    /**
     * Get all assignments for a phrase.
     * Returned FxPhraseTreeNode's will not be populated with children!
     *
     * @param phraseKey phrase key
     * @param mandator  assignment owner mandators
     * @return assigned node (without children!) and the position of the phrase
     */
    public List<FxPhraseTreeNodePosition> getAssignedNodes(String phraseKey, long... mandator);

    /**
     * Get all assignments for a phrase.
     * Returned FxPhraseTreeNode's will not be populated with children!
     *
     * @param category  phrase category
     * @param phraseKey phrase key
     * @param mandator  assignment owner mandators
     * @return assigned node (without children!) and the position of the phrase
     */
    public List<FxPhraseTreeNodePosition> getAssignedNodes(int category, String phraseKey, long... mandator);

    /**
     * Get all assignments for a phrase.
     * Returned FxPhraseTreeNode's will not be populated with children!
     *
     * @param phraseId       phrase Id
     * @param phraseMandator phrase mandator id
     * @param mandator       assignment owner mandators
     * @return assigned node (without children!) and the position of the phrase
     */
    public List<FxPhraseTreeNodePosition> getAssignedNodes(long phraseId, long phraseMandator, long... mandator);

    //query

    /**
     * Execute the requested query, returning all results in one page
     *
     * @param query query
     * @return FxPhraseQueryResult
     */
    public FxPhraseQueryResult search(FxPhraseQuery query);

    /**
     * Execute the requested query with paging
     *
     * @param query    query
     * @param page     the page to return
     * @param pageSize number of entries per page
     * @return FxPhraseQueryResult
     */
    public FxPhraseQueryResult search(FxPhraseQuery query, int page, int pageSize);

    /**
     * Clear all phrases for the targetMandator and copy the division resources as phrases (using the targetMandator as owner)
     *
     * @param targetMandator mandator to use for the phrases
     * @param converter      converter for search values
     * @throws FxApplicationException on errors
     * @throws FxNoAccessException    if not run as global supervisor or member of the mandator
     */
    public void syncDivisionResources(long targetMandator, FxPhraseSearchValueConverter converter) throws FxApplicationException;
}
