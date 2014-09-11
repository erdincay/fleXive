package com.flexive.shared.interfaces;

import com.flexive.shared.cache.FxCacheException;

import javax.ejb.Remote;

/**
 * Custom cache operations for flexive EJBs.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 *
 * @since 3.2.1
 */
@Remote
public interface TransCacheEngine {

    /**
     * Store a parameter and use a new transaction. This is used to prevent deadlocks due to caching of read-only
     * data (e.g. when initially caching a configuration parameter read from the DB.
     *
     * @param path  path
     * @param key   key
     * @param value value
     * @throws FxCacheException on cache errors
     */
    void putNewTx(String path, Object key, Object value) throws FxCacheException;
}
