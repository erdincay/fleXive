package com.flexive.faces.javascript;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.value.FxString;
import com.flexive.shared.exceptions.FxApplicationException;

import java.io.Serializable;

/**
 * Provides access to content instances via JSON/RPC.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class ContentProvider implements Serializable {
    private static final long serialVersionUID = 8529840536101737397L;

    /**
     * Returns the caption of the content with the given PK.
     *
     * @param id    the object id
     * @param version   the object version
     * @return      the caption of the content
     * @throws FxApplicationException   if the content could not be loaded
     */
    public String getCaption(long id, int version) throws FxApplicationException {
        final FxString caption = EJBLookup.getContentEngine().load(new FxPK(id, version)).getCaption();
        return caption != null ? caption.getDefaultTranslation() : "";
    }
}
