package com.flexive.shared.structure;

/**
 * Options to set for customizing the "flattening" of property assignments via {@link com.flexive.shared.interfaces.AssignmentEngine}.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 * @since 3.2.0
 */
public class FlattenOptions {
    private boolean includeMultiLang = true;

    /**
     * @return  whether multi-language properties should be considered for flat storage
     */
    public boolean isIncludeMultiLang() {
        return includeMultiLang;
    }

    /**
     * Exclude multi-lang assignments from the flattening operation
     *
     * @return  this
     */
    public FlattenOptions skipMultiLang() {
        this.includeMultiLang = false;
        return this;
    }
}
