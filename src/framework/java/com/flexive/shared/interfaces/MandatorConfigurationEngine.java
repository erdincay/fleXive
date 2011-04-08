package com.flexive.shared.interfaces;

import javax.ejb.Remote;

/**
 * Configuration engine for mandator-specific parameters.
 *
 * <p>Only mandator supervisors may update parameters in the mandator configuration.
 * </p>
 *
 * <p>
 * Use this for direct manipulation of parameters in the mandator configuration,
 * use the {@link com.flexive.shared.interfaces.ConfigurationEngine} for everything else.
 * </p>
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 * @since   3.1.6
 */
@Remote
public interface MandatorConfigurationEngine extends CustomDomainConfigurationEngine<String> {

}
