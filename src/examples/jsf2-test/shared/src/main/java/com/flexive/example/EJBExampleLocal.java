package com.flexive.example;

import javax.ejb.Local;

/**
 * <p>The local interface of the example EJB.</p>
 * <p>
 * You should only use this interface when you're sure it exists in the current context
 * (e.g. for EJB calls inside EJBs).
 * </p>
 */
@Local
public interface EJBExampleLocal extends EJBExample {
}
