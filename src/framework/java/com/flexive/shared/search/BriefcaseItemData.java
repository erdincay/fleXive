package com.flexive.shared.search;

import com.flexive.shared.AbstractSelectableObjectWithName;

import java.io.Serializable;

/**
 * Item meta data for a briefcase entry
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class BriefcaseItemData extends AbstractSelectableObjectWithName implements Serializable {
    private static final long serialVersionUID = -5177473531623077055L;

    public static enum SortField {
        POS,
        METADATA,
        INTFLAG1,
        INTFLAG2,
        INTFLAG3,
        LONGFLAG1,
        LONGFLAG2
    }

    public static enum SortOrder {
        ASC,
        DESC
    }

    private long briefcaseId;
    private long itemId;
    private int pos = -1;
    private String metaData;
    private Integer intFlag1 = null, intFlag2 = null, intFlag3 = null;
    private Long longFlag1 = null, longFlag2 = null;

    private BriefcaseItemData(long briefcaseId, long itemId, String metaData) {
        this.briefcaseId = briefcaseId;
        this.itemId = itemId;
        this.metaData = metaData == null ? "" : metaData;
    }

    public static BriefcaseItemData createBriefCaseItemData(long briefcaseId, long itemId, String metaData) {
        return new BriefcaseItemData(briefcaseId, itemId, metaData);
    }

    public String getName() {
        return metaData;
    }

    public long getId() {
        return itemId;
    }

    public long getBriefcaseId() {
        return briefcaseId;
    }

    public long getItemId() {
        return itemId;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public boolean isPosSet() {
        return pos > -1;
    }

    public String getMetaData() {
        return metaData;
    }

    public BriefcaseItemData setMetaData(String metaData) {
        this.metaData = metaData != null ? metaData : "";
        return this;
    }

    public Integer getIntFlag1() {
        return intFlag1;
    }

    public boolean isIntFlagSet(int index) {
        switch (index) {
            case 1:
                return intFlag1 != null;
            case 2:
                return intFlag2 != null;
            case 3:
                return intFlag3 != null;
            default:
                return false;
        }
    }

    public int getIntFlag(int index, int notSetValue) {
        switch (index) {
            case 1:
                return intFlag1 != null ? intFlag1 : notSetValue;
            case 2:
                return intFlag2 != null ? intFlag2 : notSetValue;
            case 3:
                return intFlag3 != null ? intFlag3 : notSetValue;
            default:
                return notSetValue;
        }
    }


    public boolean isLongFlagSet(int index) {
        switch (index) {
            case 1:
                return longFlag1 != null;
            case 2:
                return longFlag2 != null;
            default:
                return false;
        }
    }

    public Long getLongFlag(int index, Long notSetValue) {
        switch (index) {
            case 1:
                return longFlag1 != null ? longFlag1 : notSetValue;
            case 2:
                return longFlag2 != null ? longFlag2 : notSetValue;
            default:
                return notSetValue;
        }
    }

    public BriefcaseItemData setIntFlag1(Integer intFlag1) {
        this.intFlag1 = intFlag1;
        return this;
    }

    public Integer getIntFlag2() {
        return intFlag2;
    }

    public BriefcaseItemData setIntFlag2(Integer intFlag2) {
        this.intFlag2 = intFlag2;
        return this;
    }

    public Integer getIntFlag3() {
        return intFlag3;
    }

    public BriefcaseItemData setIntFlag3(Integer intFlag3) {
        this.intFlag3 = intFlag3;
        return this;
    }

    public Long getLongFlag1() {
        return longFlag1;
    }

    public BriefcaseItemData setLongFlag1(Long longFlag1) {
        this.longFlag1 = longFlag1;
        return this;
    }

    public Long getLongFlag2() {
        return longFlag2;
    }

    public BriefcaseItemData setLongFlag2(Long longFlag2) {
        this.longFlag2 = longFlag2;
        return this;
    }
}
