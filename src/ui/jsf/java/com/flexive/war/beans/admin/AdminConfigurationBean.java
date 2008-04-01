package com.flexive.war.beans.admin;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.faces.messages.FxFacesMsgErr;

/**
 * Provides access to miscellaneous backend configuration settings.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class AdminConfigurationBean {
    private Boolean treeLiveEnabled;
    private String exportURLprefix;

    /**
     * Updates the system configuration. Since all getters should use the actual configuration
     * as fallback, only parameters that are actually set will be updated.
     */
    public void updateConfiguration() {
        try {
            EJBLookup.getConfigurationEngine().putInSource(SystemParameters.TREE_LIVE_ENABLED, isTreeLiveEnabled());
            EJBLookup.getDivisionConfigurationEngine().put(SystemParameters.EXPORT_DOWNLOAD_URL, getExportURLprefix());
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    /**
     * Returns true if the live tree is available.
     *
     * @return true if the live tree is available.
     * @throws FxApplicationException on system errors
     * @see SystemParameters#TREE_LIVE_ENABLED
     */
    public boolean isTreeLiveEnabled() throws FxApplicationException {
        if (treeLiveEnabled == null) {
            return EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_LIVE_ENABLED);
        }
        return treeLiveEnabled;
    }

    public void setTreeLiveEnabled(boolean treeLiveEnabled) {
        this.treeLiveEnabled = treeLiveEnabled;
    }

    public String getExportURLprefix() throws FxApplicationException {
        if( exportURLprefix == null )
            return EJBLookup.getDivisionConfigurationEngine().get(SystemParameters.EXPORT_DOWNLOAD_URL);
        return exportURLprefix;
    }

    public void setExportURLprefix(String exportURLprefix) {
        this.exportURLprefix = exportURLprefix;
    }
}
