package com.flexive.shared.interfaces;

import javax.ejb.Remote;

/**
 * Application configuration engine. Allows the storage of parameters in application scope. The
 * application ID is determined using {@link com.flexive.shared.FxContext#getApplicationId()}.
 * For web applications, this is equal to the servlet context path (e.g. "flexive").
 * <p>
 * As with the division configuration, only global supervisors may update entries in the application
 * configuration.
 * </p>
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 * @since 3.1
 */
@Remote
public interface ApplicationConfigurationEngine extends CustomDomainConfigurationEngine<String> {
}
