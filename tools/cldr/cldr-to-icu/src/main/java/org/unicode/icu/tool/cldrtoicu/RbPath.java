// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * A resource bundle path, used to identify entries in ICU data.
 *
 * <p>Immutable and thread safe.
 */
public final class RbPath implements Comparable<RbPath> {
    private static final Splitter PATH_SPLITTER = Splitter.on('/').trimResults();

    // This defines ordering of paths in IcuData instances and thus the order in ICU data files.
    // If there's ever a reason to have a different "natural" order for paths, this Comparator
    // should be moved into the ICU file writer class(es).
    private static final Comparator<RbPath> ORDERING =
        Comparator.comparing(
            p -> p.segments,
            Comparators.lexicographical(Comparator.<String>naturalOrder()));

    // Matches the definition of invariant characters in "uinvchar.cpp". We can make this all much
    // faster if needed with a custom matcher (it's just a 128 way bit lookup via 2 longs).
    private static final CharMatcher INVARIANT_CHARS =
        CharMatcher.ascii().and(CharMatcher.anyOf("!#$@[\\]^`{|}~").negate());

    // Note that we must also prohibit double-quote from appearing anywhere other than surrounding
    // segment values. This is because some segment values can contain special ICU data characters
    // (e.g. ':') but must be treated as literals. There is not proper "escaping" mechanism in ICU
    // data for key values (since '\' is not an invariant, things like \\uxxxx are not possible).
    //
    // Ideally quoting would be done when the file is written, but that would require additional
    // complexity in RbPath, since suffixes like ":intvector" must not be quoted and must somehow
    // be distinguished from timezone "metazone" names which also contain ':'.
    private static final CharMatcher QUOTED_SEGMENT_CHARS =
        INVARIANT_CHARS
            .and(CharMatcher.javaIsoControl().negate())
            .and(CharMatcher.isNot('"'));
    private static final CharMatcher UNQUOTED_SEGMENT_CHARS =
        QUOTED_SEGMENT_CHARS.and(whitespace().negate());

    /**
     * Returns a path with the specified segments in (possibly empty). Note that unlike
     * {@link #parse(String)}, {@code '/'} is not treated specially and can be present in a path
     * element constructed by this method.
     */
    public static RbPath of(String... segments) {
        return of(Arrays.asList(segments));
    }

    /**
     * Returns a path with the specified segments in (possibly empty). Note that unlike
     * {@link #parse(String)}, {@code '/'} is not treated specially and can be present in a path
     * element constructed by this method.
     */
    public static RbPath of(Iterable<String> segments) {
        return new RbPath(segments);
    }

    /** Parses the given path string, assuming {@code '/'} as a path separator. */
    public static RbPath parse(String path) {
        checkArgument(!path.isEmpty(), "cannot parse an empty path string");
        // Allow leading '/', but don't allow empty segments anywhere else.
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return new RbPath(PATH_SPLITTER.split(path));
    }

    /** Returns the common prefix length of two paths (useful when thinking of path hierarchies). */
    public static int getCommonPrefixLength(RbPath lhs, RbPath rhs) {
        int maxLength = Math.min(lhs.length(), rhs.length());
        int n = 0;
        while (n < maxLength && lhs.getSegment(n).equals(rhs.getSegment(n))) {
            n++;
        }
        return n;
    }

    private final ImmutableList<String> segments;
    private final int hashCode;

    private RbPath(Iterable<String> segments) {
        this.segments = ImmutableList.copyOf(segments);
        // Use "this.segments" since the incoming list can have a different hash!
        this.hashCode = Objects.hash(this.segments);
        for (String segment : this.segments) {
            checkArgument(!segment.isEmpty(), "path segments must not be empty: %s", this.segments);
            // Either the label is quoted (e.g. "foo") or it is bar (e.g. foo) but it can only
            // contain double quotes at either end, or not at all. If the string is quoted, only
            // validate the content, and not the quotes themselves.
            switch (segment.charAt(0)) {
            case '<':
                // Allow anything in hidden labels, since they will be removed later and never
                // appear in the final ICU data.
                checkArgument(segment.endsWith(">"),
                    "mismatched quoting for hidden label: %s", segment);
                continue;

            case '"':
                checkArgument(segment.endsWith("\""),
                    "mismatched quoting for segment: %s", segment);
                checkArgument(
                    QUOTED_SEGMENT_CHARS.matchesAllOf(segment.substring(1, segment.length() - 1)),
                    "invalid character in unquoted resource bundle path segment: %s", segment);
                break;

            default:
                checkArgument(
                    UNQUOTED_SEGMENT_CHARS.matchesAllOf(segment),
                    "invalid character in unquoted resource bundle path segment: %s", segment);
                break;
            }
        }
    }

    /** Returns the number of segments in this path. */
    public int length() {
        return segments.size();
    }

    /** Returns the Nth segments in this path. */
    public String getSegment(int n) {
        return segments.get(n);
    }

    /** Returns a new path extended at the end by the specified segments. */
    public RbPath extendBy(String... parts) {
        return new RbPath(Iterables.concat(segments, Arrays.asList(parts)));
    }

    /** Returns whether this path starts with the specified prefix. */
    public boolean startsWith(RbPath prefix) {
        return prefix.length() <= length() && matchesSublist(prefix, 0);
    }

    /** Returns whether this path ends with the specified suffix. */
    public boolean endsWith(RbPath suffix) {
        return suffix.length() <= length() && matchesSublist(suffix, length() - suffix.length());
    }

    /** Returns whether this path contains the specified path. */
    public boolean contains(RbPath path) {
        int maxOffset = length() - path.length();
        for (int i = 0; i <= maxOffset; i++) {
            if (matchesSublist(path, i)) {
                return true;
            }
        }
        return false;
    }

    // Assume length check has been done.
    private boolean matchesSublist(RbPath path, int offset) {
        for (int i = 0; i < path.length(); i++) {
            if (!path.getSegment(i).equals(getSegment(i + offset))) {
                return false;
            }
        }
        return true;
    }

    // TODO: Remove this and isAnonymous() since they are only called once each, in the same place.
    public RbPath getParent() {
        checkState(length() > 0, "cannot get parent of the empty path");
        return new RbPath(segments.subList(0, length() - 1));
    }

    public boolean isAnonymous() {
        return length() > 0 && segments.get(length() - 1).charAt(0) == '<';
    }

    // TODO: Remove this special case code (called exactly once).
    public RbPath mapSegments(Function<? super String, String> fn) {
        return new RbPath(segments.stream().map(fn).collect(toImmutableList()));
    }

    // TODO: Remove this and isAlias() in favour of having properly typed paths.
    public boolean isIntPath() {
        return typeSuffixIsAnyOf(":int", ":intvector");
    }

    public boolean isBinPath() {
        return typeSuffixIsAnyOf(":bin");
    }

    public boolean isAlias() {
        return typeSuffixIsAnyOf(":alias");
    }

    private boolean typeSuffixIsAnyOf(String... types) {
        String lastElement = getSegment(length() - 1);
        for (String type : types) {
            if (lastElement.endsWith(type)) {
                return true;
            }
        }
        return false;
    }

    @Override public int compareTo(RbPath other) {
        return ORDERING.compare(this, other);
    }

    @Override public boolean equals(Object other) {
        return (other instanceof RbPath) && segments.equals(((RbPath) other).segments);
    }

    @Override public int hashCode() {
        return hashCode;
    }

    @Override public String toString() {
        return String.join("/", segments);
    }
}
