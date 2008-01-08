package com.flexive.shared.search.query;

/**
 * <p>
 * Version filters available in the FxSQL search engine.
 * </p>
 * <p>
 * For example:
 * <code>
 * FILTER co.version=max
 * </code>
 * to select the highest version of contents, or
 * <code>
 * FILTER co.version=all
 * </code>
 * to return all versions of found content instances.
 * </p>
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public enum VersionFilter {
    MAX,
    LIVE,
    ALL,
    AUTO
}
