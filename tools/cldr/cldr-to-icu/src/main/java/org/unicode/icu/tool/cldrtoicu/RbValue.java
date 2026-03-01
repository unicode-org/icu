// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.Objects;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

/**
 * A resource bundle value containing a sequence of elements. This is a very thin wrapper over an
 * immutable list, with a few additional constraints (e.g. cannot be empty).
 *
 * <p>Immutable and thread safe.
 */
public final class RbValue {
    private final ImmutableList<String> elements;
    private final int elementsPerLine;

    /** Returns a resource bundle value of the given elements. */
    public static RbValue of(String... elements) {
        return new RbValue(ImmutableList.copyOf(elements), 1);
    }

    /** Returns a resource bundle value of the given elements. */
    public static RbValue of(Iterable<String> elements) {
        return new RbValue(ImmutableList.copyOf(elements), 1);
    }

    /** Returns a resource bundle value of the given elements by consuming the given stream. */
    public static RbValue of(Stream<String> elements) {
        return new RbValue(elements.collect(toImmutableList()), 1);
    }

    private RbValue(ImmutableList<String> elements, int elementsPerLine) {
        checkArgument(!elements.isEmpty(), "Resource bundle values cannot be empty");
        checkArgument(elementsPerLine > 0, "invalid elements per line: %s", elementsPerLine);
        this.elements = elements;
        this.elementsPerLine = elementsPerLine;
    }

    public RbValue elementsPerLine(int n) {
        return new RbValue(elements, n);
    }

    /** Returns the non-empty list of value elements. */
    public ImmutableList<String> getElements() {
        return elements;
    }

    /**
     * Returns whether this is a single element value. Singleton values are treated different when
     * writing out ICU data files.
     */
    boolean isSingleton() {
        return elements.size() == 1;
    }

    int getElementsPerLine() {
        return elementsPerLine;
    }

    @Override public int hashCode() {
        return Objects.hashCode(elements);
    }

    @Override public boolean equals(Object obj) {
        return obj instanceof RbValue && elements.equals(((RbValue) obj).elements);
    }

    @Override public String toString() {
        return elements.toString();
    }
}
