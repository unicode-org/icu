// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.truth.Truth.assertThat;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;

import java.util.Arrays;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class IcuDataTest {
    @Test
    public void testSimple() {
        IcuData icuData = new IcuData("icu-name", true);
        assertThat(icuData.getName()).isEqualTo("icu-name");
        assertThat(icuData.hasFallback()).isTrue();

        IcuData otherData = new IcuData("other-name", false);
        assertThat(otherData.getName()).isEqualTo("other-name");
        assertThat(otherData.hasFallback()).isFalse();
    }

    @Test
    public void testFileComment() {
        IcuData icuData = new IcuData("icu-name", false);
        assertThat(icuData.getFileComment()).isEmpty();

        icuData.setFileComment("Hello", "World");
        assertThat(icuData.getFileComment()).containsExactly("Hello", "World").inOrder();

        icuData.setFileComment(Arrays.asList("Foo", "Bar"));
        assertThat(icuData.getFileComment()).containsExactly("Foo", "Bar").inOrder();

        icuData.setFileComment(ImmutableList.of());
        assertThat(icuData.getFileComment()).isEmpty();
    }

    @Test
    public void testSetVersion() {
        IcuData icuData = new IcuData("icu-name", false);
        icuData.setVersion("VERSION");

        RbPath rbPath = RbPath.of("Version");
        assertThat(icuData.getPaths()).containsExactly(rbPath);
        assertThat(icuData.get(rbPath)).isEqualTo(ImmutableList.of(RbValue.of("VERSION")));
    }

    @Test
    public void testGetPaths() {
        IcuData icuData = new IcuData("icu-name", false);
        // getPaths() is a live view on the data, not a snapshot.
        Set<RbPath> paths = icuData.getPaths();
        assertThat(paths).isEmpty();

        RbPath fooBar = RbPath.of("foo", "bar");
        icuData.add(fooBar, "value1");
        assertThat(icuData.get(fooBar)).contains(RbValue.of("value1"));
        assertThat(paths).containsExactly(fooBar);
        assertThat(paths).hasSize(1);

        RbPath fooBaz = RbPath.of("foo", "baz");
        icuData.add(fooBaz, "value2");
        assertThat(icuData.get(fooBaz)).contains(RbValue.of("value2"));
        assertThat(paths).containsExactly(fooBar, fooBaz).inOrder();
        assertThat(paths).hasSize(2);

        // Paths is not modifiable.
        assertThrows(UnsupportedOperationException.class, () -> paths.add(RbPath.of("nope")));
        assertThrows(UnsupportedOperationException.class, () -> paths.remove(fooBar));
        assertThrows(UnsupportedOperationException.class, paths::clear);
    }

    @Test
    public void addMultiple() {
        IcuData icuData = new IcuData("icu-name", false);
        RbPath fooBar = RbPath.of("foo", "bar");

        RbValue value1 = RbValue.of("the", "first", "value");
        RbValue value2 = RbValue.of("another-value");

        icuData.add(fooBar, value1);
        assertThat(icuData.get(fooBar)).containsExactly(value1);

        icuData.add(fooBar, "another-value");
        assertThat(icuData.get(fooBar)).containsExactly(value1, value2).inOrder();

        // It's just a list, with no ordering and no deduplication.
        icuData.add(fooBar, Arrays.asList(value2, value1));
        assertThat(icuData.get(fooBar)).containsExactly(value1, value2, value2, value1).inOrder();
    }

    @Test
    public void replace() {
        IcuData icuData = new IcuData("icu-name", false);
        RbPath fooBar = RbPath.of("foo", "bar");

        RbValue value1 = RbValue.of("the", "first", "value");
        RbValue value2 = RbValue.of("another-value");

        icuData.replace(fooBar, value1);
        assertThat(icuData.get(fooBar)).containsExactly(value1);

        icuData.replace(fooBar, "another-value");
        assertThat(icuData.get(fooBar)).containsExactly(value2);
    }
}
