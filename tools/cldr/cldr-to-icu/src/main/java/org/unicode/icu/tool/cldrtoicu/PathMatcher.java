// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.unicode.cldr.api.AttributeKey.keyOf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrPath;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * An immutable matcher for {@link CldrPath} instances. A path matcher specification looks like
 * {@code "foo/*[@x="z"]/bar[@y=*]"}, where element names and attribute values can be wildcards.
 *
 * <p>Note that the path fragment represented by the specification does not include either leading
 * or trailing {@code '/'}. This is because matching can occur at any point in a {@code CdlrPath}.
 * The choice of where to match in the path is governed by the match method used (e.g.
 * {@link PathMatcher#matchesSuffixOf(CldrPath)}.
 */
public abstract class PathMatcher {
    /** Parses the path specification into a matcher. */
    public static PathMatcher of(String pathSpec) {
        // Supported so far: "a", "a/b", "a/b[@x=*]"
        return new BasicMatcher(parse(pathSpec));
    }

    /**
     * Combines the given matchers into a single composite matcher which tests all the given
     * matchers in order.
     */
    public static PathMatcher anyOf(PathMatcher... matchers) {
        checkArgument(matchers.length > 0, "must supply at least one matcher");
        if (matchers.length == 1) {
            return checkNotNull(matchers[0]);
        }
        return new CompositeMatcher(ImmutableList.copyOf(matchers));
    }

    /** Attempts a full match against a given path. */
    public abstract boolean matches(CldrPath path);

    /** Attempts a suffix match against a given path. */
    public abstract boolean matchesSuffixOf(CldrPath path);

    /** Attempts a prefix match against a given path. */
    public abstract boolean matchesPrefixOf(CldrPath path);

    // A matcher that simply combines a sequences of other matchers in order.
    private static final class CompositeMatcher extends PathMatcher {
        private final ImmutableList<PathMatcher> matchers;

        private CompositeMatcher(ImmutableList<PathMatcher> matchers) {
            checkArgument(matchers.size() > 1);
            this.matchers = checkNotNull(matchers);
        }

        @Override
        public boolean matches(CldrPath path) {
            for (PathMatcher m : matchers) {
                if (m.matches(path)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean matchesSuffixOf(CldrPath path) {
            for (PathMatcher m : matchers) {
                if (m.matchesSuffixOf(path)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean matchesPrefixOf(CldrPath path) {
            for (PathMatcher m : matchers) {
                if (m.matchesPrefixOf(path)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class BasicMatcher extends PathMatcher {
        private final ImmutableList<Predicate<CldrPath>> elementMatchers;

        private BasicMatcher(List<Predicate<CldrPath>> elementMatchers) {
            this.elementMatchers = ImmutableList.copyOf(elementMatchers);
        }

        @Override
        public boolean matches(CldrPath path) {
            return elementMatchers.size() == path.getLength() && matchRegion(path, 0);
        }

        @Override
        public boolean matchesSuffixOf(CldrPath path) {
            int start = path.getLength() - elementMatchers.size();
            return start >= 0 && matchRegion(path, start);
        }

        @Override
        public boolean matchesPrefixOf(CldrPath path) {
            return path.getLength() >= elementMatchers.size() && matchRegion(path, 0);
        }

        private boolean matchRegion(CldrPath path, int offset) {
            // offset is the path element corresponding the the "top most" element matcher, it
            // must be in the range 0 ... (path.length() - elementMatchers.size()).
            checkPositionIndex(offset, path.getLength() - elementMatchers.size());
            // First jump over the path parents until we find the last matcher.
            int matchPathLength = offset + elementMatchers.size();
            while (path.getLength() > matchPathLength) {
                path = path.getParent();
            }
            return matchForward(path, elementMatchers.size() - 1);
        }

        private boolean matchForward(CldrPath path, int matcherIndex) {
            if (matcherIndex < 0) {
                return true;
            }
            return matchForward(path.getParent(), matcherIndex - 1)
                && elementMatchers.get(matcherIndex).test(path);
        }
    }

    // Make a new, non-interned, unique instance here which we can test by reference to
    // determine if the argument is to be captured (needed as ImmutableMap prohibits null).
    // DO NOT change this code to assign "*" as the value directly, it MUST be a new instance.
    @SuppressWarnings("StringOperationCanBeSimplified")
    private static final String WILDCARD = new String("*");

    private static final Pattern ELEMENT_START_REGEX =
        Pattern.compile("(\\*|[-:\\w]+)(?:/|\\[|$)");
    private static final Pattern ATTRIBUTE_REGEX =
        Pattern.compile("\\[@([-:\\w]+)=(?:\\*|\"([^\"]*)\")]");

    // element := foo, foo[@bar="baz"], foo[@bar=*]
    // pathspec := element{/element}*
    private static List<Predicate<CldrPath>> parse(String pathSpec) {
        List<Predicate<CldrPath>> specs = new ArrayList<>();
        int pos = 0;
        do {
            pos = parse(pathSpec, pos, specs);
        } while (pos >= 0);
        return specs;
    }

    // Return next start index or -1.
    private static int parse(String pathSpec, int pos, List<Predicate<CldrPath>> specs) {
        Matcher m = ELEMENT_START_REGEX.matcher(pathSpec).region(pos, pathSpec.length());
        checkArgument(m.lookingAt(), "invalid path specification (index=%s): %s", pos, pathSpec);
        String name = m.group(1);
        Map<String, String> attributes = ImmutableMap.of();
        pos = m.end(1);
        if (pos < pathSpec.length() && pathSpec.charAt(pos) == '[') {
            // We have attributes to add.
            attributes = new LinkedHashMap<>();
            do {
                m = ATTRIBUTE_REGEX.matcher(pathSpec).region(pos, pathSpec.length());
                checkArgument(m.lookingAt(),
                    "invalid path specification (index=%s): %s", pos, pathSpec);
                // Null if we matched the '*' wildcard.
                String value = m.group(2);
                attributes.put(m.group(1), value != null ? value : WILDCARD);
                pos = m.end();
            } while (pos < pathSpec.length() && pathSpec.charAt(pos) == '[');
        }
        // Wildcard matching is less efficient because attribute keys cannot be made in advance, so
        // since it's also very rare, we special case it.
        Predicate<CldrPath> matcher = name.equals(WILDCARD)
            ? new WildcardElementMatcher(attributes)::match
            : new ElementMatcher(name, attributes)::match;
        specs.add(matcher);
        if (pos == pathSpec.length()) {
            return -1;
        }
        checkState(pathSpec.charAt(pos) == '/',
            "invalid path specification (index=%s): %s", pos, pathSpec);
        return pos + 1;
    }

    // Matcher for path elements like "foo[@bar=*]" where the name is known in advance.
    private static final class ElementMatcher  {
        private final String name;
        private final ImmutableMap<AttributeKey, String> attributes;

        private ElementMatcher(String name, Map<String, String> attributes) {
            this.name = checkNotNull(name);
            this.attributes = attributes.entrySet().stream()
                .collect(toImmutableMap(e -> keyOf(name, e.getKey()), Entry::getValue));
        }

        boolean match(CldrPath path) {
            if (!path.getName().equals(name)) {
                return false;
            }
            for (Entry<AttributeKey, String> e : attributes.entrySet()) {
                String actual = path.get(e.getKey());
                if (actual == null) {
                    return false;
                }
                String expected = e.getValue();
                // DO NOT change this to use expected.equals(WILDCARD).
                if (expected != WILDCARD && !expected.equals(actual)) {
                    return false;
                }
            }
            return true;
        }
    }

    // Matcher for path elements like "*[@bar=*]", where the name isn't known until match time.
    private static final class WildcardElementMatcher  {
        private final ImmutableMap<String, String> attributes;

        private WildcardElementMatcher(Map<String, String> attributes) {
            this.attributes = ImmutableMap.copyOf(attributes);
        }

        private boolean match(CldrPath path) {
            // The wildcard matcher never fails due to the element name but must create new key
            // instances every time matching occurs (because the key name is dynamic). Since this
            // is rare, it's worth making into a separate case.
            for (Entry<String, String> attribute : attributes.entrySet()) {
                String actual = path.get(keyOf(path.getName(), attribute.getKey()));
                if (actual == null) {
                    return false;
                }
                String expected = attribute.getValue();
                // DO NOT change this to use expected.equals(WILDCARD).
                if (expected != WILDCARD && !expected.equals(actual)) {
                    return false;
                }
            }
            return true;
        }
    }
}
