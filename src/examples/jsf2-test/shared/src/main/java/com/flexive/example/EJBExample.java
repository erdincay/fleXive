package com.flexive.example;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxType;

import javax.ejb.Remote;
import java.util.Map;

/**
 * <p>A demo interface for an EJB bean.</p>
 * <p>
 * This EJB engine provides a method that returns the instance counts by type for the
 * current installation.
 * </p>
 * <p>
 * The common interface should always be marked as a Remote interface, since this is more
 * portable across application servers for classes that are not using EJB dependency injection.
 * </p>
 * <p>
 * The easiest way to do this is to extend the remote interface and tag it with the @Local annotation.
 * Flexive's {@link com.flexive.shared.EJBLookup EJBLookup} automatically uses the local interface
 * if it's available.
 * </p>
 */
@Remote
public interface EJBExample {

    /**
     * Return the instance counts for all registered {@link FxType FxTypes}.
     *
     * @return the instance counts for all registered {@link FxType FxTypes}.
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          if the instance counts could not be determined
     */
    Map<FxType, Integer> getInstanceCounts() throws FxApplicationException;
}
