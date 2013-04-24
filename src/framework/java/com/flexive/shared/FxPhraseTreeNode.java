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
package com.flexive.shared;

import com.flexive.shared.exceptions.FxEntryExistsException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.exceptions.FxNotFoundException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A phrase tree node
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @since 3.1.7
 */
@SuppressWarnings("UnusedDeclaration")
public class FxPhraseTreeNode implements Serializable {
    public final static long NOT_SET = -1L;
    private long id;
    private long mandatorId;
    private int category;
    private long parentNodeId;
    private long parentNodeMandatorId;
    private FxPhrase phrase;
    private List<FxPhraseTreeNode> children;
    private long pos = NOT_SET;

    public FxPhraseTreeNode(long id, long mandatorId, long parentNodeId, long parentNodeMandatorId,
                            FxPhrase phrase, List<FxPhraseTreeNode> children) {
        this.id = id;
        this.mandatorId = mandatorId;
        this.category = FxPhraseCategorySelection.CATEGORY_DEFAULT;
        this.parentNodeId = parentNodeId;
        this.parentNodeMandatorId = parentNodeMandatorId;
        this.phrase = phrase;
        this.children = children;
        checkChildrenCategory();
    }

    public FxPhraseTreeNode(long id, long mandatorId, int category, long parentNodeId, long parentNodeMandatorId,
                            FxPhrase phrase, List<FxPhraseTreeNode> children) {
        this.id = id;
        this.mandatorId = mandatorId;
        this.category = category;
        this.parentNodeId = parentNodeId;
        this.parentNodeMandatorId = parentNodeMandatorId;
        this.phrase = phrase;
        this.children = children;
        checkChildrenCategory();
    }

    /**
     * Check if the category of the children matches this nodes category
     *
     * @throws IllegalArgumentException on category mismatch
     */
    private void checkChildrenCategory() throws IllegalArgumentException {
        if (this.children == null || this.children.size() == 0)
            return;
        for (FxPhraseTreeNode child : this.children) {
            if (child.getCategory() != this.getCategory())
                throw new IllegalArgumentException("Invalid category: " + child.getCategory() + " for child node " + child + ", expected: " + this.getCategory());
            if (child.hasChildren())
                child.checkChildrenCategory();
        }
    }

    public FxPhraseTreeNode(long id, long mandatorId, long parentNodeId, long parentNodeMandatorId,
                            String phraseKey, long phraseMandator, List<FxPhraseTreeNode> children) throws FxNotFoundException {
        this.id = id;
        this.mandatorId = mandatorId;
        this.category = FxPhraseCategorySelection.CATEGORY_DEFAULT;
        this.parentNodeId = parentNodeId;
        this.parentNodeMandatorId = parentNodeMandatorId;
        this.phrase = EJBLookup.getPhraseEngine().loadPhrase(this.category, phraseKey, phraseMandator);
        this.children = children;
        checkChildrenCategory();
    }

    public FxPhraseTreeNode(long id, long mandatorId, int category, long parentNodeId, long parentNodeMandatorId,
                            String phraseKey, long phraseMandator, List<FxPhraseTreeNode> children) throws FxNotFoundException {
        this.id = id;
        this.mandatorId = mandatorId;
        this.category = category;
        this.parentNodeId = parentNodeId;
        this.parentNodeMandatorId = parentNodeMandatorId;
        this.phrase = EJBLookup.getPhraseEngine().loadPhrase(category, phraseKey, phraseMandator);
        this.children = children;
        checkChildrenCategory();
    }

    /**
     * Create a root node for the callers mandator (not persisted!)
     *
     * @param phraseKey phrase key (referencing the callers mandator)
     * @return FxPhraseTreeNode
     * @throws FxNotFoundException if the phrase does not exist
     */
    public static FxPhraseTreeNode createRootNode(String phraseKey) throws FxNotFoundException {
        final long mandator = FxContext.getUserTicket().getMandatorId();
        return createRootNode(mandator, phraseKey, mandator);
    }

    /**
     * Create a root node for the callers mandator (not persisted!)
     *
     * @param category  phrase category
     * @param phraseKey phrase key (referencing the callers mandator)
     * @return FxPhraseTreeNode
     * @throws FxNotFoundException if the phrase does not exist
     */
    public static FxPhraseTreeNode createRootNode(int category, String phraseKey) throws FxNotFoundException {
        final long mandator = FxContext.getUserTicket().getMandatorId();
        return createRootNode(mandator, phraseKey, mandator);
    }

    /**
     * Create a new root node (not persisted!)
     *
     * @param nodeMandator   node mandator
     * @param phraseKey      phrase key
     * @param phraseMandator mandator of the phrase
     * @return FxPhraseTreeNode
     * @throws FxNotFoundException if the phrase does not exist
     */
    public static FxPhraseTreeNode createRootNode(long nodeMandator, String phraseKey, long phraseMandator) throws FxNotFoundException {
        return new FxPhraseTreeNode(NOT_SET, nodeMandator, FxPhraseCategorySelection.CATEGORY_DEFAULT, NOT_SET, NOT_SET, phraseKey, phraseMandator, null);
    }

    /**
     * Create a new root node (not persisted!)
     *
     * @param nodeMandator   node mandator
     * @param category       phrase category
     * @param phraseKey      phrase key
     * @param phraseMandator mandator of the phrase
     * @return FxPhraseTreeNode
     * @throws FxNotFoundException if the phrase does not exist
     */
    public static FxPhraseTreeNode createRootNode(long nodeMandator, int category, String phraseKey, long phraseMandator) throws FxNotFoundException {
        return new FxPhraseTreeNode(NOT_SET, nodeMandator, category, NOT_SET, NOT_SET, phraseKey, phraseMandator, null);
    }

    /**
     * Create a child node for the callers mandator (not persisted!)
     *
     * @param parent    parent node
     * @param phraseKey phrase key (referencing the callers mandator)
     * @return FxPhraseTreeNode
     * @throws FxNotFoundException if the phrase does not exist
     */
    public static FxPhraseTreeNode createChildNode(FxPhraseTreeNode parent, String phraseKey) throws FxNotFoundException {
        final long mandator = FxContext.getUserTicket().getMandatorId();
        return createChildNode(parent, mandator, phraseKey, mandator);
    }

    public static FxPhraseTreeNode createChildNode(FxPhraseTreeNode parent, String phraseKey, long phraseMandator) throws FxNotFoundException {
        final long mandator = FxContext.getUserTicket().getMandatorId();
        return createChildNode(parent, mandator, phraseKey, phraseMandator);
    }

    public static FxPhraseTreeNode createChildNode(FxPhraseTreeNode parent, FxPhrase phrase) {
        final long mandator = FxContext.getUserTicket().getMandatorId();
        return createChildNode(parent, mandator, phrase);
    }

    public static FxPhraseTreeNode createChildNode(FxPhraseTreeNode parent, long nodeMandator, FxPhrase phrase) {
        return new FxPhraseTreeNode(NOT_SET, nodeMandator, parent.getCategory(), parent.id, parent.mandatorId, phrase, null);
    }

    /**
     * Create a child node
     *
     * @param parent         parent node
     * @param nodeMandator   mandator of the new node
     * @param phraseKey      phrase key to use
     * @param phraseMandator mandator of the phrase
     * @return FxPhraseTreeNode
     * @throws FxNotFoundException if the phrase does not exist
     */
    public static FxPhraseTreeNode createChildNode(FxPhraseTreeNode parent, long nodeMandator, String phraseKey, long phraseMandator) throws FxNotFoundException {
        return new FxPhraseTreeNode(NOT_SET, nodeMandator, parent.getCategory(), parent.id, parent.mandatorId, phraseKey, phraseMandator, null);
    }


    /**
     * Is this a new tree node?
     *
     * @return new tree node
     */
    public boolean isNew() {
        return id == NOT_SET;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return id
     */
    public long getId() {
        return id;
    }

    /**
     * @return mandator id
     */
    public long getMandatorId() {
        return mandatorId;
    }

    /**
     * @return category id
     */
    public int getCategory() {
        return category;
    }

    /**
     * Set the phrase key (does not change the phrase mandator!)
     *
     * @param phraseKey phrase key
     * @return FxPhraseTreeNode
     * @throws FxNotFoundException if the phrase does not exist
     */
    public FxPhraseTreeNode setPhraseKey(String phraseKey) throws FxNotFoundException {
        if (this.getPhrase().getKey().equals(phraseKey))
            return this;
        this.phrase = EJBLookup.getPhraseEngine().loadPhrase(phraseKey, phrase.getMandator());
        return this;
    }

    /**
     * Set the phrase key and mandator
     *
     * @param phraseKey      phrase key
     * @param phraseMandator phrase mandator
     * @return FxPhraseTreeNode
     * @throws FxNotFoundException if the phrase does not exist
     */
    public FxPhraseTreeNode setPhrase(String phraseKey, long phraseMandator) throws FxNotFoundException {
        if (this.getPhrase().getKey().equals(phraseKey) && this.getPhrase().getMandator() == phraseMandator)
            return this;
        this.phrase = EJBLookup.getPhraseEngine().loadPhrase(phraseKey, phraseMandator);
        return this;
    }

    /**
     * Set the phrase
     *
     * @param phrase phrase to set
     * @return FxPhraseTreeNode
     */
    public FxPhraseTreeNode setPhrase(FxPhrase phrase) {
        if (this.phrase.equals(phrase))
            return this;
        this.phrase = phrase;
        return this;
    }

    /**
     * @return used phrase
     */
    public FxPhrase getPhrase() {
        return phrase;
    }

    /**
     * @return parent node id unless its a root node
     */
    public long getParentNodeId() {
        return parentNodeId;
    }

    /**
     * @return is this a root node?
     */
    public boolean isRootNode() {
        return parentNodeId == NOT_SET;
    }

    /**
     * @return does a parent node exist?
     */
    public boolean hasParent() {
        return !isRootNode();
    }

    /**
     * @return mandator of the parent node
     */
    public long getParentNodeMandatorId() {
        return parentNodeMandatorId;
    }

    /**
     * @return child nodes of this node (only available if loaded)
     */
    public List<FxPhraseTreeNode> getChildren() {
        if (this.children == null)
            this.children = new ArrayList<FxPhraseTreeNode>(0);
        return children;
    }

    public void setChildren(List<FxPhraseTreeNode> children) {
        if (this.children != null)
            this.children.addAll(children);
        else
            this.children = children;
    }

    /**
     * @return do child nodes exist?
     */
    public boolean hasChildren() {
        return this.children != null && this.children.size() > 0;
    }

    public long getPos() {
        return pos;
    }

    public FxPhraseTreeNode setPos(long pos) {
        this.pos = pos;
        return this;
    }

    public boolean hasPos() {
        return this.pos != NOT_SET;
    }

    /**
     * Save or create this node
     *
     * @return FxPhraseTreeNode
     * @throws FxNoAccessException if trying to save for other mandators
     * @throws FxNotFoundException if a phrase key was not found
     */
    public FxPhraseTreeNode save() throws FxNoAccessException, FxNotFoundException {
        FxPhraseTreeNode node = EJBLookup.getPhraseEngine().saveTreeNode(this);
        this.id = node.id;
        this.parentNodeId = node.parentNodeId;
        this.parentNodeMandatorId = node.parentNodeMandatorId;
        this.phrase = node.getPhrase();
        this.children = node.children;
        return this;
    }

    /**
     * Assign a phrase of the calling users mandator to this node.
     * If this node is new it will be created
     *
     * @param phraseKey phrase key
     * @throws FxNotFoundException    if the phrase was not found
     * @throws FxEntryExistsException if the same phrase is already assigned
     * @throws FxNoAccessException    if this a new node and the calling user may not save it
     */
    public void assignPhrase(String phraseKey) throws FxNotFoundException, FxEntryExistsException, FxNoAccessException {
        assignPhrase(phraseKey, FxContext.getUserTicket().getMandatorId());
    }

    /**
     * Assign a phrase to this node.
     * If this node is new it will be created
     *
     * @param phraseKey      phrase key
     * @param phraseMandator mandator of the phrase
     * @throws FxNotFoundException    if the phrase was not found
     * @throws FxEntryExistsException if the same phrase is already assigned
     * @throws FxNoAccessException    if this a new node and the calling user may not save it
     */
    public void assignPhrase(String phraseKey, long phraseMandator) throws FxNotFoundException, FxEntryExistsException, FxNoAccessException {
        if (this.isNew())
            this.save();
        EJBLookup.getPhraseEngine().assignPhrase(this.mandatorId, this.id, this.mandatorId, phraseKey, phraseMandator, 0, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FxPhraseTreeNode))
            return false;
        FxPhraseTreeNode other = (FxPhraseTreeNode) obj;
        if (other.getId() != this.getId())
            return false;
        if (other.hasParent() == this.hasParent()) {
            if (other.getParentNodeId() != this.getParentNodeId() || other.getParentNodeMandatorId() != this.getParentNodeMandatorId())
                return false;
        } else
            return false;
        return other.getMandatorId() == this.getMandatorId() && other.isRootNode() == this.isRootNode() && other.getPhrase().equals(this.getPhrase());
    }

    @Override
    public String toString() {
        return "{Node " + getPhrase() + " (id:" + getId() + ",mandator:" + getMandatorId() + ",category:" + getCategory() + ",pid:" + getParentNodeId() + ",pman:" + getParentNodeMandatorId() + ")}";
    }
}
