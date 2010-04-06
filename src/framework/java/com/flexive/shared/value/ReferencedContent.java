/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.shared.value;

import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.workflow.Step;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A referenced content - value class for FxReference
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ReferencedContent extends FxPK implements Serializable {
    private static final Log LOG = LogFactory.getLog(ReferencedContent.class);
    private static final long serialVersionUID = 4530337199230606480L;
    private String caption;
    private final Step step;
    private final List<ACL> acls = newArrayListWithCapacity(5);

    private FxContent content;
    private boolean accessGranted;
    private boolean resolved;


    /**
     * Ctor
     *
     * @param pk      referenced primary key
     * @param caption caption of the referenced content, only available if loaded
     * @param step    the step
     * @param acls    the ACL(s)
     * @since         3.1
     */
    public ReferencedContent(FxPK pk, String caption, Step step, List<ACL> acls) {
        super(pk.getId(), pk.getVersion());
        this.caption = caption;
        this.step = step;
        if (acls != null) {
            this.acls.addAll(acls);
        }
        this.content = null;
        this.accessGranted = false;
    }

    /**
     * Ctor
     *
     * @param pk      referenced primary key
     * @param caption caption of the referenced content, only available if loaded
     * @param step    the step
     * @param acl     the acl
     * @deprecated    use {@link #ReferencedContent(com.flexive.shared.content.FxPK, String, com.flexive.shared.workflow.Step, java.util.List)}
     */
    @Deprecated
    public ReferencedContent(FxPK pk, String caption, Step step, ACL acl) {
        this(pk, caption, step, acl == null ? null : Arrays.asList(acl));
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
        this.content = null;
        this.accessGranted = false;
    }

    /**
     * Copy constructor.
     *
     * @param other the instance to be copied
     */
    public ReferencedContent(ReferencedContent other) {
        this.caption = other.caption;
        this.step = other.step;
        this.acls.addAll(other.acls);
        this.content = other.hasContent() ? other.getContent().copy() : null;
        this.accessGranted = other.accessGranted;
        this.resolved = other.isResolved();
    }

    /**
     * Returns a copy of this ReferencedContent instance.
     *
     * @return  a copy of this ReferencedContent instance.
     */
    public ReferencedContent copy() {
        return new ReferencedContent(this);
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
        return !acls.isEmpty();
    }

    /**
     * Get the step for this reference
     *
     * @return step of this reference
     */
    public Step getStep() {
        if (!hasStep())
            throw new FxApplicationException("ex.content.reference.step.missing").asRuntimeException();
        return step;
    }

    /**
     * Get the first ACL for this reference
     *
     * @return ACL for this reference
     * @deprecated  use {@link #getAcls()}
     */
    @Deprecated
    public ACL getAcl() {
        if (!hasACL())
            throw new FxApplicationException("ex.content.reference.ACL.missing").asRuntimeException();
        return acls.get(0);
    }

    /**
     * Return the ACL(s) assigned to the reference.
     *
     * @return  the ACL(s) assigned to the reference
     * @since 3.1
     */
    public List<ACL> getAcls() {
        return Collections.unmodifiableList(acls);
    }

    /**
     * Is a loaded FxContent assigned for this reference?
     *
     * @return if a loaded FxContent is assigned for this reference
     */
    public synchronized boolean hasContent() {
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
     * Returns true if the content instance has already been resolved.
     *
     * @return  true if the content instance has already been resolved.
     */
    public synchronized boolean isResolved() {
        return resolved;
    }

    /**
     * Set the assigned FxContent for this reference
     *
     * @param content the assigned FxContent for this reference
     */
    public synchronized void setContent(FxContent content) {
        if(!resolved) {
            this.content = content;
            try {
                if( this.content.hasCaption() )
                    this.caption = this.content.getCaption().getBestTranslation();
            } catch (FxRuntimeException e) {
                LOG.warn(e);
            }
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
     * @param obj the other object to compare
     * @return <code>true</code> if obj is a ReferencedContent and the id and caption are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ReferencedContent))
            return false;
        final ReferencedContent other = (ReferencedContent) obj;
        return id == other.id;
//        return StringUtils.equals(caption, other.caption);
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
