/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.cmis.spi;

import static com.flexive.shared.EJBLookup.getBriefcaseEngine;
import static com.flexive.shared.EJBLookup.getContentEngine;
import com.flexive.shared.*;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.exceptions.FxLockException;
import com.flexive.shared.search.Briefcase;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.value.FxString;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.core.LifeCycleInfoImpl;
import com.google.common.collect.Lists;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple implementation for handling checkout/checkin of documents (= Private Working Copies/PWC) via CMIS.
 * This implementation keeps the information about the checked out objects in a protected briefcase
 * and simply locks all version of the "checked out" instance for everybody else until the checkout is canceled or
 * the changes have been checked in.
 * <p>
 * The implementation is synchronized on the request division, i.e. only one operation at a time for a given
 * division. 
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class PrivateWorkingCopyManager {
    private static final String CHECKED_OUT_BRIEFCASE = "$flexive_cmis_checkedout$";
    private static final String PRIVATE_ACL = "CMIS_CHECKED_OUT_PRIVATE";

    private static final String METADATA_ACLS = "instanceACLs";
    private static final String METADATA_CHECKEDOUT_BY = "checkedOutBy";
    private static final String METADATA_CHECKEDOUT_PK = "checkedOutPK";
    private static final String METADATA_INSTANCE_OWNER = "instanceOwner";

    // update locks per division
    private static final ConcurrentMap<Integer, Object> UPDATE_LOCKS = new ConcurrentHashMap<Integer, Object>();

    private long checkedOutBriefcaseId = -1;
    private long privateAclId = -1;

    /**
     * Return true if the given PK is currently checked out.
     *
     * @param pk    the document PK
     * @return      true if the given PK is currently checked out.
     */
    public boolean isCheckedOut(FxPK pk) {
        return ArrayUtils.contains(getCheckedOutIds(), pk.getId());
    }

    /**
     * Checkout the given content, return the PK of the Private Working Copy. Until the PWC is
     * "checked in" or the checkout is canceled, no other user can checkout this content, or make
     * changes to existing instances.
     *
     * @param content   the content to be checked out
     * @return          the PK of the Private Working Copy
     */
    public FxPK checkout(FxContent content) {
        if (isCheckedOut(content.getPk())) {
            throw new IllegalArgumentException("Document " + content.getPk() + " is already checked out.");
        }
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        try {
            // record checkout information for this content in the briefcase item metadata
            final FxReferenceMetaData<FxPK> meta = initMetaData(content);

            // create cloned instance, which can only be seen by the user
            final FxContent copy = content.copyAsNewInstance();
            copy.setAclId(getPrivateAclId());
            fixWorkflowStep(environment, copy);
            changeOwner(copy, FxContext.getUserTicket().getUserId());

            synchronized(getDivisionLock()) {
                return createPWC(content.getPk(), meta, copy).getPk();
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    /**
     * Cancel a checkout, remove the PWC.
     *
     * @param pk    the content PK whose PWC should be destroyed
     */
    public void cancelCheckout(FxPK pk) {
        if (!isCheckedOut(pk)) {
            throw new IllegalArgumentException("Document " + pk + " is not checked out.");
        }
        try {
            synchronized(getDivisionLock()) {
                final FxReferenceMetaData<FxPK> metadata = getMetaData(pk);
                unlockAllVersions(pk);
                removePWC(pk, FxPK.fromString(metadata.getString(METADATA_CHECKEDOUT_PK, null)));
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    /**
     * Checkin the changes made in a working copy, destroy the PWC.
     *
     * @param pwc   the Private Working Copy
     * @return      the PK of the new content version, containing the changes of the PWC
     */
    public FxPK checkin(FxContent pwc) {
        synchronized(getDivisionLock()) {
            FxReferenceMetaData<FxPK> meta = getMetaDataForPWC(pwc.getPk());

            // restore "real" object owner
            changeOwner(pwc, meta.getLong(METADATA_INSTANCE_OWNER, -1));

            // restore ACLs
            final String[] aclValues = StringUtils.split(meta.getString(METADATA_ACLS, ""), ',');
            final List<Long> aclIds = Lists.newArrayListWithCapacity(aclValues.length);
            for (String value : aclValues) {
                aclIds.add(Long.valueOf(value));
            }
            pwc.setAclIds(aclIds);

            try {
                // move changes to actual content
                final FxContent content = getContentEngine().load(new FxPK(meta.getReference().getId(), FxPK.MAX));
                for (FxPropertyData data : pwc.getPropertyData(-1, true)) {
                    if (!data.getPropertyAssignment().isSystemInternal()) {
                        content.setValue(data.getXPath(), data.getValue());
                    }
                }

                // save content
                final FxPK newPK = content.saveNewVersion().getPk();

                // remove locks and PWC
                unlockAllVersions(meta.getReference());
                removePWC(meta.getReference(), pwc.getPk());

                return newPK;
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }
    }

    private FxReferenceMetaData<FxPK> getMetaDataForPWC(FxPK pwcPK) {
        final List<FxReferenceMetaData<FxPK>> allMeta;
        FxContext.startRunningAsSystem();
        try {
            allMeta = getBriefcaseEngine().getMetaData(getBriefcaseId());
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        } finally {
            FxContext.stopRunningAsSystem();
        }
        for (FxReferenceMetaData<FxPK> data : allMeta) {
            if (data.getString(METADATA_CHECKEDOUT_PK, "").equals(pwcPK.toString())) {
                return data;
            }
        }
        throw new IllegalArgumentException("Document " + pwcPK + " is not a known private working copy.");
    }

    private FxReferenceMetaData<FxPK> getMetaData(FxPK pk) {
        FxContext.startRunningAsSystem();
        final FxReferenceMetaData<FxPK> metadata;
        try {
            metadata = getBriefcaseEngine().getMetaData(getBriefcaseId(), pk);
            if (metadata == null) {
                throw new IllegalArgumentException("Document " + pk + " is checked out, but no metadata entry exists (internal error).");
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        } finally {
            FxContext.stopRunningAsSystem();
        }
        return metadata;
    }


    private FxContent createPWC(FxPK checkedOutPK, FxReferenceMetaData<FxPK> meta, FxContent pwc) throws FxApplicationException {
        lockAllVersions(checkedOutPK);

        // save the PWC. We need to run this with system privileges, because the 'create' permission of a
        // owner group assignment is ignored (otherwise any instance would become creatable)
        final FxContent clone;
        FxContext.startRunningAsSystem();
        try {
            clone = pwc.save();
        } finally {
            FxContext.stopRunningAsSystem();
        }

        // store ID of checked-out instance
        meta.put(METADATA_CHECKEDOUT_PK, clone.getPk().toString());
        addToBriefcase(meta);

        return clone;
    }

    private void removePWC(FxPK pk, FxPK pwc) throws FxApplicationException {
        // remove PWC instance with permission checks - only the owner may delete it
        getContentEngine().remove(pwc);
        removePWCFromBriefcase(pk);
    }

    private void removePWCFromBriefcase(FxPK pk) throws FxApplicationException {
        // remove from briefcase
        FxContext.startRunningAsSystem();
        try {
            getBriefcaseEngine().removeItems(getBriefcaseId(), Arrays.asList(pk));
        } finally {
            FxContext.stopRunningAsSystem();
        }
    }

    private FxReferenceMetaData<FxPK> initMetaData(FxContent content) {
        final FxReferenceMetaData<FxPK> meta = new FxReferenceMetaData<FxPK>(content.getPk());
        meta.put(METADATA_CHECKEDOUT_BY, FxContext.getUserTicket().getUserId());
        meta.put(METADATA_ACLS, StringUtils.join(content.getAclIds(), ','));
        meta.put(METADATA_INSTANCE_OWNER, content.getLifeCycleInfo().getCreatorId());
        return meta;
    }

    private void addToBriefcase(FxReferenceMetaData<FxPK> meta) throws FxApplicationException {
        // update system briefcase
        FxContext.startRunningAsSystem();
        try {
            getBriefcaseEngine().addItems(getBriefcaseId(), Arrays.asList(meta.getReference()));
            getBriefcaseEngine().setMetaData(getBriefcaseId(), Arrays.asList(meta));
        } finally {
            FxContext.stopRunningAsSystem();
        }
    }

    private void lockAllVersions(FxPK pk) throws FxLockException {
        final List<Integer> lockedVersions = Lists.newArrayList();
        try {
            for (Integer version : getContentEngine().getContentVersionInfo(pk).getVersions()) {
                getContentEngine().lock(FxLockType.Permanent, new FxPK(pk.getId(), version));
                lockedVersions.add(version);
            }
        } catch (FxApplicationException e) {
            // revert locks
            for (Integer version : lockedVersions) {
                getContentEngine().unlock(new FxPK(pk.getId(), version));
            }
            throw e.asRuntimeException();
        }
    }

    private void unlockAllVersions(FxPK pk) throws FxLockException {
        try {
            for (Integer version : getContentEngine().getContentVersionInfo(pk).getVersions()) {
                getContentEngine().unlock(new FxPK(pk.getId(), version));
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    private void changeOwner(FxContent copy, long userId) {
        if (userId == -1) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }
        // TODO: HORRIBLE hack! The content engine currently doesn't distinguish between creator and owner
        // This also corrupts the original content, because the lifecycle info isn't cloned.
        ((LifeCycleInfoImpl) copy.getLifeCycleInfo()).setCreatorId(userId);
    }

    private void fixWorkflowStep(FxEnvironment environment, FxContent copy) {
        final Step step = environment.getStep(copy.getStepId());
        final StepDefinition stepdef = environment.getStepDefinition(step.getStepDefinitionId());
        if (stepdef.isUnique()) {
            // move new version to non-unique target, e.g. Live -> Edit
            copy.setStepId(environment.getStepByDefinition(step.getWorkflowId(), stepdef.getUniqueTargetId()).getId());
        }
    }

    private long[] getCheckedOutIds() {
        FxContext.startRunningAsSystem();
        try {
            return getBriefcaseEngine().getItems(getBriefcaseId());
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        } finally {
            FxContext.stopRunningAsSystem();
        }
    }

    private long getBriefcaseId() {
        synchronized(getDivisionLock()) {
            if (checkedOutBriefcaseId == -1) {
                FxContext.startRunningAsSystem();
                try {
                    final List<Briefcase> briefcases = getBriefcaseEngine().loadAll(true);
                    for (Briefcase briefcase : briefcases) {
                        if (CHECKED_OUT_BRIEFCASE.equals(briefcase.getName())) {
                            checkedOutBriefcaseId = briefcase.getId();
                            break;
                        }
                    }

                    if (checkedOutBriefcaseId == -1) {
                        // not found - create
                        checkedOutBriefcaseId = getBriefcaseEngine().create(
                                CHECKED_OUT_BRIEFCASE,
                                "Collection of contents that have been checked out with CMIS.",
                                ACL.NULL_ACL_ID
                        );
                    }
                } catch (FxApplicationException e) {
                    throw e.asRuntimeException();
                } finally {
                    FxContext.stopRunningAsSystem();
                }
            }
            return checkedOutBriefcaseId;
        }
    }

    private long getPrivateAclId() {
        synchronized(getDivisionLock()) {
            if (privateAclId == -1) {
                try {
                    privateAclId = CacheAdmin.getEnvironment().getACL(PRIVATE_ACL).getId();
                } catch (FxRuntimeException e) {
                    // create ACL
                    FxContext.startRunningAsSystem();
                    try {
                        privateAclId = EJBLookup.getAclEngine().create(
                            PRIVATE_ACL,
                                new FxString(true, "CMIS Private Working Copy"),
                                Mandator.MANDATOR_FLEXIVE,
                                "#000000",
                                "A document checked out with CMIS.",
                                ACLCategory.INSTANCE);

                        // the owner can do anything, everybody else won't see it
                        EJBLookup.getAclEngine().assign(
                                privateAclId,
                                UserGroup.GROUP_OWNER,
                                true, true, true, true, true, true
                        );
                    } catch (FxApplicationException e1) {
                        throw e1.asRuntimeException();
                    } finally {
                        FxContext.stopRunningAsSystem();
                    }
                }
            }
            return privateAclId;
        }
    }

    private Object getDivisionLock() {
        final Integer divisionId = FxContext.get().getDivisionId();
        if (!UPDATE_LOCKS.containsKey(divisionId)) {
            UPDATE_LOCKS.putIfAbsent(divisionId, new Object());
        }
        return UPDATE_LOCKS.get(divisionId);
    }
}
