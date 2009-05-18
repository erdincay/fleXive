package com.flexive.shared.interfaces;

import javax.ejb.Remote;

/**
 * Node configuration. The node can be specified externally through the system attribute
 * {@code flexive.nodename}, otherwise the hostname is used.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Remote
public interface NodeConfigurationEngine extends GenericConfigurationEngine {

    /**
     * Return this node's name.
     *
     * @return  the node name of the current maching
     */
    String getNodeName();
}
