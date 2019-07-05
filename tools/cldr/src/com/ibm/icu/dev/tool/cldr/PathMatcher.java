package com.ibm.icu.dev.tool.cldr;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrPath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.unicode.cldr.api.AttributeKey.keyOf;

/**
 * An immutable matcher for {@link CldrPath} instances, which includes the capability to capture
 * matched arguments. A matcher uses a "path specification" which is similar to, but simpler than
 * a regular expression. A path specification looks like {@code "foo/bar[@x="y"][@z=*]"}, where in
 * this case the value of the {@code 'z'} attribute would be made available after matching.
 *
 * <p>Matching is achieved by providing a callback (typically a lambda) to process any captured
 * arguments if matching occurs. In normal use it will look something like:
 * <pre>{@code
 * PathMatcher m = PathMatcher.of("foo[@x=*]/bar[@y=*]");
 * m.onMatch(path, args -> processMatch(args.get(0), args.get(1));
 * }</pre>
 *
 * <p>Note that the path fragment represented by the specification do not include either leading or
 * trailing {@code '/'}. This is because matching can occur at any point in a {@code CdlrPath}. The
 * choice of where to match in the path is governed by the match method used (e.g.
 * {@link PathMatcher#matchesSuffixOf(CldrPath) onSuffixMatch()}.
 */
abstract class PathMatcher {
    /** Parses the path specification into a matcher. */
    public static PathMatcher of(String pathSpec) {
        // Supported so far: "a", "a/b", "a/b[@x=*]"
        return new BasicMatcher(parse(pathSpec));
    }

    /**
     * Combines the given matchers into a single composite matcher which tests all the given
     * matchers in order.
     */
    public static PathMatcher inOrder(PathMatcher... matchers) {
        checkArgument(matchers.length > 0, "must supply at least one matcher");
        if (matchers.length == 1) {
            return checkNotNull(matchers[0]);
        }
        return new CompositeMatcher(ImmutableList.copyOf(matchers));
    }

    /** Attempts a full match against a given path, invoking the given handler on success. */
    public abstract boolean matches(CldrPath path);

    /** Attempts a suffix match against a given path, invoking the given handler on success. */
    public abstract boolean matchesSuffixOf(CldrPath path);

    /** Attempts a prefix match against a given path, invoking the given handler on success. */
    public abstract boolean matchesPrefixOf(CldrPath path);

    /** API for extracting captured arguments during match handling. */
    public interface Arguments {
        /** Gets the Nth (zero-indexed) argument captured during a successful match. */
        String get(int n);
    }

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
            return elementMatchers.size() == path.getLength() && reverseMatch(path);
        }

        @Override
        public boolean matchesSuffixOf(CldrPath path) {
            return elementMatchers.size() <= path.getLength() && reverseMatch(path);
        }

        @Override
        public boolean matchesPrefixOf(CldrPath path) {
            int toTrim = path.getLength() - elementMatchers.size();
            if (toTrim < 0) {
                return false;
            }
            while (toTrim-- > 0) {
                path = path.getParent();
            }
            return reverseMatch(path);
        }

        private boolean reverseMatch(CldrPath path) {
            int matchCount = elementMatchers.size();
            for (int n = matchCount - 1; n >= 0; n--) {
                if (!elementMatchers.get(n).test(path)) {
                    return false;
                }
                path = path.getParent();
            }
            return true;
        }
    }

    // Make a new, non-interned, unique instance here which we can test by reference to
    // determine if the argument is to be captured (needed as ImmutableMap prohibits null).
    private static final String WILDCARD = new String("*");

    private static final Pattern ELEMENT_START_REGEX =
        Pattern.compile("(\\*|[-:\\w]+)(?:/|\\[|$)");
    private static final Pattern ATTRIBUTE_REGEX =
        Pattern.compile("\\[@([-:\\w]+)=(?:\\*|\"([^\"]*)\")\\]");

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
        checkState(m.lookingAt(), "invalid path specification (index=%s): %s", pos, pathSpec);
        String name = m.group(1);
        Map<String, String> attributes = ImmutableMap.of();
        pos = m.end(1);
        if (pos < pathSpec.length() && pathSpec.charAt(pos) == '[') {
            // We have attributes to add.
            attributes = new LinkedHashMap<>();
            do {
                m = ATTRIBUTE_REGEX.matcher(pathSpec).region(pos, pathSpec.length());
                checkState(m.lookingAt(),
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
                if (expected != WILDCARD && !expected.equals(actual)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class WildcardElementMatcher  {
        private final ImmutableMap<String, String> attributes;

        private WildcardElementMatcher(Map<String, String> attributes) {
            this.attributes = ImmutableMap.copyOf(attributes);
        }

        private boolean match(CldrPath path) {
            // The wildcard matcher never fails due to the element name but must create new key
            // instances every time matching occurs (because the key name is dynamic). Since this
            // is rare, it's worth making into a seperate case.
            for (Entry<String, String> attribute : attributes.entrySet()) {
                String actual = path.get(keyOf(path.getName(), attribute.getKey()));
                if (actual == null) {
                    return false;
                }
                String expected = attribute.getValue();
                if (expected != WILDCARD && !expected.equals(actual)) {
                    return false;
                }
            }
            return true;
        }
    }
}
