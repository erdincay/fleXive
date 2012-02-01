package com.flexive.shared;

import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * Phrase implementation
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxPhrase implements Serializable {
    private long id = -1L;
    private long mandator;
    private String key;
    private String value; 
    private FxString fxValue;
    private String tag;
    private FxString fxTag;

    public FxPhrase(String key, FxString fxValue, String tag) {
        this.mandator = FxContext.getUserTicket().getMandatorId();
        this.key = key;
        this.value = null;
        this.fxValue = fxValue;
        this.tag = tag;
        this.fxTag = null;
    }

    public FxPhrase(String key, FxString fxValue, FxString fxTag) {
        this.mandator = FxContext.getUserTicket().getMandatorId();
        this.key = key;
        this.value = null;
        this.fxValue = fxValue;
        this.tag = null;
        this.fxTag = fxTag;
    }

    public FxPhrase(long mandator, String key, FxString fxValue, String tag) {
        this.mandator = mandator;
        this.key = key;
        this.value = null;
        this.fxValue = fxValue;
        this.tag = tag;
        this.fxTag = null;
    }

    public FxPhrase(long mandator, String key, FxString fxValue, FxString fxTag) {
        this.mandator = mandator;
        this.key = key;
        this.value = null;
        this.fxValue = fxValue;
        this.tag = null;
        this.fxTag = fxTag;
    }

    public FxPhrase(long mandator, String key, String value, String tag) {
        this.mandator = mandator;
        this.key = key;
        this.value = value;
        this.fxValue = null;
        this.tag = tag;
        this.fxTag = null;
    }

    public long getMandator() {
        return mandator;
    }

    public String getKey() {
        return key;
    }

    public FxString getValue() {
        if(fxValue == null)
            return new FxString(false, value);
        return fxValue;
    }
    
    public String getSingleValue() {
        if(value != null)
            return value;
        return fxValue.getBestTranslation();
    }

    public void setValue(FxString value) {
        this.fxValue = value;
    }

    public String getTag() {
        if (tag != null)
            return tag;
        else
            return fxTag.getBestTranslation();
    }

    public FxString getFxTag() {
        if (fxTag != null)
            return fxTag;
        return new FxString(false, tag);
    }

    public void setTag(String tag) {
        this.tag = tag;
        this.fxTag = null;
    }

    public void setFxTag(FxString fxTag) {
        this.fxTag = fxTag;
        this.tag = null;
    }

    public boolean hasTag() {
        return (fxTag != null && !fxTag.isEmpty()) || (tag != null && !StringUtils.isBlank(tag));
    }

    public long getId() {
        return id;
    }

    public FxPhrase setId(long id) {
        this.id = id;
        return this;
    }

    public boolean hasId() {
        return this.id >= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof FxPhrase))
            return false;
        FxPhrase other = (FxPhrase)obj;
        try {
            return !(this.hasId() && other.hasId() && this.getId() != other.getId()) &&
                    other.getKey().equals(this.getKey()) && other.getMandator() == this.getMandator() &&
                    other.hasTag() == this.hasTag() && other.getFxTag().equals(this.getFxTag());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "{" + getSingleValue() + " (id:" + getId() + ",mandator:" + getMandator() + ")" + "}";
    }

    public FxPhrase save() throws FxNoAccessException {
        EJBLookup.getPhraseEngine().savePhrase(this.getKey(), this.getValue(), tag != null ? tag : fxTag != null ? fxTag : null, this.mandator);
        return this;
    }
}
