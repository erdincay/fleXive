package com.flexive.shared.interfaces;

import com.flexive.shared.FxPhrase;
import com.flexive.shared.FxPhraseQuery;
import com.flexive.shared.FxPhraseQueryResult;
import com.flexive.shared.FxPhraseTreeNode;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxEntryInUseException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.value.FxString;

import javax.ejb.Remote;
import java.util.List;

/**
 * Phrase engine
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @since 3.1.7
 */
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
     * Removes all phrases of the requested mandator and all its mappings (but not if used in tree nodes!)
     *
     * @param mandator requested mandator
     * @return count of removed phrases
     * @throws FxNoAccessException   if the requested mandator is not the callers mandator
     * @throws FxEntryInUseException if phrases are in use by the tree
     */
    public int removeMandatorPhrases(long mandator) throws FxNoAccessException, FxEntryInUseException;

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
     * Load all FxPhrase instances of a phrase with the requested key prefix
     *
     * @param phraseKeyPrefix phrase key prefix
     * @param mandators       list of mandators to try loading the key from (in the requested order)
     * @return found FxPhrases ordered by phraseKey
     */
    public List<FxPhrase> loadPhrases(String phraseKeyPrefix, long... mandators);

    /**
     * Remove all phrases that belong to the requested mandator
     *
     * @param mandatorId requested mandator
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     * @throws FxEntryInUseException if phrases are in use by the phrase tree
     */
    public void clearPhrases(long mandatorId) throws FxNoAccessException, FxEntryInUseException;

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
     * Load all root nodes and child nodes for up to 2 mandators
     *
     * @param mandator2top insert nodes of mandator 2 at the top or bottom?
     * @param mandators    mandators
     * @return phrase tree
     */
    public List<FxPhraseTreeNode> loadPhraseTree(boolean mandator2top, long... mandators);

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
     * Remove a tree node, moving its children up one level and removing all assignments to this node
     *
     * @param nodeId     node id
     * @param mandatorId mandator of the node
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     * @throws FxNotFoundException if no node with the requested id/mandator exists
     */
    public void removeTreeNode(long nodeId, long mandatorId) throws FxNoAccessException, FxNotFoundException;

    /**
     * Remove all tree nodes that belong to the requested mandator
     *
     * @param mandatorId requested mandator
     * @throws FxNoAccessException if the requested mandator is not the callers mandator
     */
    public void clearTree(long mandatorId) throws FxNoAccessException;

    //phrase->tree mapping

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
     */
    public void assignPhrase(long assignmentOwner, long nodeId, long nodeMandator, long phraseId, long phraseMandator, long pos, boolean checkPositioning) throws FxNotFoundException, FxNoAccessException;

    /**
     * Assign a phrase to a node.
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
     */
    public void assignPhrase(long assignmentOwner, long nodeId, long nodeMandator, String phraseKey, long phraseMandator, long pos, boolean checkPositioning) throws FxNotFoundException, FxNoAccessException;

    /**
     * "Move" the position of an assignment
     *
     * @param assignmentOwner mandator which is the "owner" of this assignment
     * @param nodeId          node id
     * @param nodeMandatorId  node mandator
     * @param phraseId        phrase id
     * @param phraseMandator  phrase mandator
     * @param delta           delta to move
     * @throws FxNoAccessException if the requested assignmentOwner is not the callers mandator
     * @throws FxNotFoundException if either the node or phrase were not found
     */
    void moveTreeNodeAssignment(long assignmentOwner, long nodeId, long nodeMandatorId, long phraseId, long phraseMandator, int delta) throws FxNotFoundException, FxNoAccessException;

    /**
     * Remove an assign of a phrase from a node.
     *
     * @param assignmentOwner mandator which is the "owner" of this assignment
     * @param nodeId          node id
     * @param nodeMandator    node mandator
     * @param phraseId        phrase id
     * @param phraseMandator  phrase mandator
     * @return if an assignment was found that could be removed
     * @throws FxNoAccessException if the requested assignmentOwner is not the callers mandator
     * @throws FxNotFoundException if either the node or phrase were not found
     */
    public boolean removePhraseAssignment(long assignmentOwner, long nodeId, long nodeMandator, long phraseId, long phraseMandator) throws FxNotFoundException, FxNoAccessException;

    /**
     * Remove all phrase assignments from a node.
     * If the calling user is the owner of the node, all assignments will be removed, if not only the phrases assigned by the calling users mandator
     *
     * @param nodeId       node id
     * @param nodeMandator node mandator id
     */
    public void removeAssignmentsFromNode(long nodeId, long nodeMandator);

    /**
     * Get all assignments for a phrase.
     * Returned FxPhraseTreeNode's will not be populated with children!
     *
     * @param phraseKey phrase key
     * @param mandator  assignment owner mandators
     * @return assigned noded (without children!)
     */
    public List<FxPhraseTreeNode> getAssignedNodes(String phraseKey, long... mandator);

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
     * @throws FxApplicationException on errors
     * @throws FxNoAccessException    if not run as global supervisor or member of the mandator
     */
    public void syncDivisionResources(long targetMandator) throws FxApplicationException;
}
