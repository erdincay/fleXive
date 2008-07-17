package com.flexive.shared.value.mapper;

import com.flexive.shared.value.*;
import com.flexive.shared.search.query.ValueComparator;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxPK;

import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * An abstract input mapper between discrete values (e.g. IDs) and unique String representations.
 * For example, this can be used to map account IDs to login names.
 * This is also used for most auto-complete inputs for flexive structure propreties.
 *
 * @param <T>   the actual value type of the FxValue implementation (e.g. Long)
 * @param <BaseType>    the FxValue class to be used as input (e.g. FxLargeNumber or FxReference)
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public abstract class NumberQueryInputMapper<T, BaseType extends FxValue<T, ?>> extends InputMapper<BaseType, FxString> {
    /**
     * Maps account IDs to login names. The autocomplete options are provided by
     * com.flexive.faces.javascript.AutoCompleteProvider#userQuery().
     */
    public static class AccountQueryInputMapper extends NumberQueryInputMapper<Long, FxLargeNumber> {
        public AccountQueryInputMapper() {
            buildAutocompleteHandler("AutoCompleteProvider.userQuery");
        }

        @Override
        public String encodeId(Long id) {
            if (id == null) {
                return "";
            }
            try {
                return EJBLookup.getAccountEngine().load(id).getLoginName();
            } catch (FxApplicationException e) {
                return "";  // account does not exist or cannot be loaded - ignore and return an empty value
            }
        }

        @Override
        protected FxLargeNumber doDecode(FxString value) {
            final String query = value.getDefaultTranslation();
            Long result = null;
            if (StringUtils.isNotBlank(query)) {
                try {
                    result = EJBLookup.getAccountEngine().load(query).getId();
                } catch (FxApplicationException e) {
                    // fail silently - reset input
                }
            }
            return result != null ? new FxLargeNumber(false, result) : new FxLargeNumber(false, FxLargeNumber.EMPTY).setEmpty();
        }
    }

    public static class ReferenceQueryInputMapper extends NumberQueryInputMapper<ReferencedContent, FxReference> {
        /** Separator between primary key and caption */
        private static final String SEP_PK_CAPTION = " - ";

        public ReferenceQueryInputMapper(FxProperty property) {
            buildAutocompleteHandler("AutoCompleteProvider.pkQuery", property.getReferencedType() != null
                    ? String.valueOf(property.getReferencedType().getId()): "-1");
        }

        public static ReferencedContent getReferencedContent(String query) {
            final int sepIndex = query.indexOf(SEP_PK_CAPTION);
            if (sepIndex == -1) {
                // not properly formatted, return a temporary reference that uses the entire query in the caption
                return new ReferencedContent(new FxPK(-1), query, null, null);
            }
            final FxPK pk = FxPK.fromString(query.substring(0, sepIndex));
            return new ReferencedContent(pk, query.substring(sepIndex + SEP_PK_CAPTION.length()), null, null);
        }

        @Override
        public String encodeId(ReferencedContent rc) {
            if (rc == null) {
                return "";
            }
            return rc.getId() + "." + rc.getVersion() + SEP_PK_CAPTION + rc.getCaption();
        }

        @Override
        protected FxReference doDecode(FxString value) {
            final String query = value.getDefaultTranslation();
            if (StringUtils.isNotBlank(query)) {
                return new FxReference(false, getReferencedContent(query));
            }
            return new FxReference(false, new ReferencedContent()).setEmpty();
        }
    }

    public abstract String encodeId(T id);

    @Override
    protected FxString doEncode(BaseType value) {
        if (value.isMultiLanguage()) {
            throw new FxInvalidParameterException("VALUE", "ex.content.value.mapper.numberQuery.singleLanguage").asRuntimeException();
        }
        return new FxString(value.isMultiLanguage(), encodeId(value.isEmpty() ? null : value.getDefaultTranslation()));
    }

    @Override
    public List<? extends ValueComparator> getAvailableValueComparators() {
        return PropertyValueComparator.getAvailable(FxDataType.SelectOne);
    }
}
