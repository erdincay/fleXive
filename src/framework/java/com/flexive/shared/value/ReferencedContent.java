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
package com.flexive.shared.value;

import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.workflow.Step;

import java.io.Serializable;

/**
 * A referenced content - value class for FxReference
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ReferencedContent extends FxPK implements Serializable {
    private static final long serialVersionUID = 4530337199230606480L;
    private String caption;
    private Step step;
    private ACL acl;
    private FxContent content;
    private boolean accessGranted;
    private boolean resolved;


    /**
     * Ctor
     *
     * @param pk      referenced primary key
     * @param caption caption of the referenced content, only available if loaded
     * @param step    the step
     * @param acl     the acl
     */
    public ReferencedContent(FxPK pk, String caption, Step step, ACL acl) {
        super(pk.getId(), pk.getVersion());
        this.caption = caption;
        this.step = step;
        this.acl = acl;
        this.content = null;
        this.accessGranted = false;
    }

    /**
     * Ctor
     *
     * @param pk referenced primary key
     */
    public ReferencedContent(FxPK pk) {
        super(pk.getId(), pk.getVersion());
        this.caption = "";
        this.step = null;
        this.acl = null;
        this.content = null;
        this.accessGranted = false;
    }

    /**
     * Ctor
     *
     * @param id      id
     * @param version version
     */
    public ReferencedContent(long id, int version) {
        super(id, version);
        this.caption = "";
        this.step = null;
        this.acl = null;
        this.content = null;
        this.accessGranted = false;
    }

    /**
     * Ctor
     *
     * @param id id
     */
    public ReferencedContent(long id) {
        super(id);
        this.caption = "";
        this.step = null;
        this.acl = null;
        this.content = null;
        this.accessGranted = false;
    }

    /**
     * Ctor
     */
    public ReferencedContent() {
        super();
        this.caption = "";
        this.step = null;
        this.acl = null;
        this.content = null;
        this.accessGranted = false;
    }

    /**
     * Get the caption of the referenced content, only available if loaded
     *
     * @return caption of the referenced content, only available if loaded
     */
    public String getCaption() {
        return caption;
    }

    /**
     * Is a step known for this reference?
     *
     * @return if a step is known for this reference
     */
    public boolean hasStep() {
        return step != null;
    }

    /**
     * Is an ACL known for this reference?
     *
     * @return if an ACL is known for this reference
     */
    public boolean hasACL() {
        return acl != null;
    }

    /**
     * Get the step for this reference
     *
     * @return step of this reference
     */
    public Step getStep() {
        if (!hasStep())
            throw new FxApplicationException("ex.content.reference.content.missing").asRuntimeException();
        return step;
    }

    /**
     * Get the ACL for this reference
     *
     * @return ACL for this reference
     */
    public ACL getAcl() {
        if (!hasACL())
            throw new FxApplicationException("ex.content.reference.content.missing").asRuntimeException();
        return acl;
    }

    /**
     * Is a loaded FxContent assigned for this reference?
     *
     * @return if a loaded FxContent is assigned for this reference
     */
    public boolean hasContent() {
        return content != null;
    }

    /**
     * Get the assigned FxContent for this reference
     *
     * @return the assigned FxContent for this reference
     */
    public synchronized FxContent getContent() {
        if (!hasContent())
            throw new FxApplicationException("ex.content.reference.content.missing").asRuntimeException();
        return content;
    }

    /**
     * Set the assigned FxContent for this reference
     *
     * @param content the assigned FxContent for this reference
     */
    public synchronized void setContent(FxContent content) {
        if(!resolved) {
            this.content = content;
            accessGranted = content != null;
        } else {
            resolved = true;
        }
    }

    /**
     * Is access to the referenced FxContent granted?
     *
     * @return if access to the referenced FxContent is granted
     */
    public boolean isAccessGranted() {
        return accessGranted;
    }

    /**
     * Set if (read) access to the referenced instance is granted.
     * This flag is usually set during creation of a ReferencedContent.
     * Setting it to <code>true</code> will <b>not</b> enable you to read the referenced content ;-)
     *
     * @param accessGranted if access to the referenced content is granted
     */
    public void setAccessGranted(boolean accessGranted) {
        this.accessGranted = accessGranted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() * 31 + caption.hashCode();
    }

    /**
     * Evaluates the given string value to an object of type ReferencedContent.
     *
     * @param value string value to be evaluated
     * @return the value interpreted as ReferencedContent
     */
    public static ReferencedContent fromString(String value) {
        return new ReferencedContent(FxPK.fromString(value));
    }

    public String toStringExtended() {
        return "ReferencedContent{pk="+super.toString()+", caption=["+caption+"]}";
    }
}
