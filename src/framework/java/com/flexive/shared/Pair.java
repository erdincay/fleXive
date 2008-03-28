package com.flexive.shared;

/**
 * A generic pair.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class Pair<X, Y> {
    private final X first;
    private final Y second;

    public Pair(X first, Y second) {
        this.first = first;
        this.second = second;
    }

    public X getFirst() {
        return first;
    }

    public Y getSecond() {
        return second;
    }
}
