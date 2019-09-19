// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.truth.Truth.assertThat;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;
import static org.unicode.icu.tool.cldrtoicu.testing.RbPathSubjectFactory.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RbPathTest {
    @Test
    public void testEmpty() {
        assertThat(RbPath.of()).hasSegments();
        assertThat(RbPath.of()).hasLength(0);
    }

    @Test
    public void testParseVsOf() {
        assertThat(RbPath.of("foo", "bar")).hasSegments("foo", "bar");
        assertThat(RbPath.parse("foo/bar/baz")).hasSegments("foo", "bar", "baz");
        // Allow and ignore leading '/' for legacy reasons.
        assertThat(RbPath.parse("/foo/bar/baz")).hasSegments("foo", "bar", "baz");
        assertThat(RbPath.of("foo/bar", "baz")).hasSegments("foo/bar", "baz");
    }

    @Test
    public void testBadArgs() {
        assertBadPath("", "empty path string");
        assertBadPath("foo//bar", "path segments must not be empty");
        assertBadPath("foo/<bar/baz", "mismatched quoting");
        assertBadPath("foo/\"bar", "mismatched quoting");
        assertBadPath("foo/\"bar\"baz\"", "invalid character");
        assertBadPath("foo/bar baz", "invalid character");
    }

    @Test
    public void testIteration() {
        RbPath path = RbPath.parse("foo/bar/baz");
        assertThat(path.length()).isEqualTo(3);
        assertThat(path.getSegment(0)).isEqualTo("foo");
        assertThat(path.getSegment(1)).isEqualTo("bar");
        assertThat(path.getSegment(2)).isEqualTo("baz");
    }

    @Test
    public void testExtendBy() {
        assertThat(RbPath.of("foo").extendBy("bar")).hasSegments("foo", "bar");
        assertThat(RbPath.of("foo").extendBy("bar/baz")).hasSegments("foo", "bar/baz");
        assertThat(RbPath.of("foo").extendBy("bar/baz")).isNotEqualTo(RbPath.parse("foo/bar/baz"));
    }

    @Test
    public void testStartsWith() {
        RbPath p = RbPath.of("foo", "bar", "baz");
        assertThat(p).startsWith(p).isTrue();
        assertThat(p).startsWith(RbPath.of()).isTrue();

        assertThat(p).startsWith(p.getParent()).isTrue();
        assertThat(p).startsWith(RbPath.of("foo")).isTrue();
        assertThat(p).startsWith(RbPath.of("bar")).isFalse();
        assertThat(p).startsWith(RbPath.of("foo/bar/baz")).isFalse();
    }

    @Test
    public void testEndsWith() {
        RbPath p = RbPath.of("foo", "bar", "baz");
        assertThat(p).endsWith(p).isTrue();
        assertThat(p).endsWith(RbPath.of()).isTrue();

        assertThat(p).endsWith(RbPath.of("bar", "baz")).isTrue();
        assertThat(p).endsWith(RbPath.of("bar")).isFalse();
        assertThat(p).endsWith(RbPath.of("foo/bar/baz")).isFalse();
    }

    @Test
    public void testContains() {
        RbPath p = RbPath.of("foo", "bar", "baz");
        assertThat(p).contains(p).isTrue();
        assertThat(p).contains(RbPath.of()).isTrue();

        assertThat(p).contains(RbPath.of("bar", "baz")).isTrue();
        assertThat(p).contains(RbPath.of("foo", "bar")).isTrue();
        assertThat(p).contains(RbPath.of("foo/bar/baz")).isFalse();
    }

    @Test
    public void testCommonPrefixLength() {
        RbPath p = RbPath.of("foo", "bar", "baz");
        RbPath q = RbPath.of("foo", "bar", "quux");
        assertThat(RbPath.getCommonPrefixLength(p, q)).isEqualTo(2);
        assertThat(RbPath.getCommonPrefixLength(p, p)).isEqualTo(3);
        assertThat(RbPath.getCommonPrefixLength(p, RbPath.of())).isEqualTo(0);
        // Not a prefix even though it's a suffix of the path.
        assertThat(RbPath.getCommonPrefixLength(p, RbPath.of("bar", "baz"))).isEqualTo(0);
    }

    private static void assertBadPath(String path, String errorSnippet) {
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> RbPath.parse(path));
        assertThat(e).hasMessageThat().contains(errorSnippet);
    }
}
