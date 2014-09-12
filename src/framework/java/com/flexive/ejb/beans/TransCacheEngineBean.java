package com.flexive.ejb.beans;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.interfaces.TransCacheEngine;
import com.flexive.shared.interfaces.TransCacheEngineLocal;

import javax.ejb.*;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Stateless(name = "TransCacheEngine", mappedName="TransCacheEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
public class TransCacheEngineBean implements TransCacheEngine, TransCacheEngineLocal{

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void putNoTx(String path, Object key, Object value) throws FxCacheException {
        CacheAdmin.getInstance().put(path, key, value);
    }
}
