// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(JUnit4.class)
public class Bcp47MapperTest {
    private static final ImmutableMap<RbPath, RbValue> EXPECTED_ALIAS_MAP = ImmutableMap.of(
        RbPath.of("bcpTypeAlias", "tz:alias"),
        RbValue.of("/ICUDATA/timezoneTypes/bcpTypeAlias/tz"),
        RbPath.of("typeAlias", "timezone:alias"),
        RbValue.of("/ICUDATA/timezoneTypes/typeAlias/timezone"),
        RbPath.of("typeMap", "timezone:alias"),
        RbValue.of("/ICUDATA/timezoneTypes/typeMap/timezone"));

    @Test
    public void testSimple() {
        CldrData cldrData = cldrData(
            simpleType("foo", "one"),
            simpleType("foo", "two"),
            simpleType("foo", "three"),
            simpleType("bar", "four"),
            simpleType("bar", "five"));

        ImmutableList<IcuData> icuData = Bcp47Mapper.process(cldrData);

        IcuData bcp47Data = icuData.get(0);
        assertThat(bcp47Data).hasName("keyTypeData");
        assertThat(bcp47Data).hasFallback(false);

        // Check the number of paths and verify the special injected values.
        assertThat(bcp47Data).getPaths().hasSize(7 + EXPECTED_ALIAS_MAP.size());
        EXPECTED_ALIAS_MAP.forEach((p, v) -> assertThat(bcp47Data).hasValuesFor(p, v));

        assertThat(bcp47Data).hasEmptyValue("/keyMap/foo");
        assertThat(bcp47Data).hasEmptyValue("/keyMap/bar");

        assertThat(bcp47Data).hasEmptyValue("/typeMap/foo/one");
        assertThat(bcp47Data).hasEmptyValue("/typeMap/foo/two");
        assertThat(bcp47Data).hasEmptyValue("/typeMap/foo/three");
        assertThat(bcp47Data).hasEmptyValue("/typeMap/bar/four");
        assertThat(bcp47Data).hasEmptyValue("/typeMap/bar/five");

        IcuData tzData = icuData.get(1);
        assertThat(tzData).hasName("timezoneTypes");
        assertThat(tzData).hasFallback(false);
        assertThat(tzData).getPaths().isEmpty();
    }

    @Test
    public void testSimpleTimezone() {
        CldrData cldrData = cldrData(
            simpleType("tz", "one"),
            simpleType("tz", "two"),
            simpleType("tz", "three"),
            simpleType("bar", "four"),
            simpleType("bar", "five"));

        ImmutableList<IcuData> icuData = Bcp47Mapper.process(cldrData);

        IcuData bcp47Data = icuData.get(0);
        assertThat(bcp47Data).hasName("keyTypeData");
        assertThat(bcp47Data).hasFallback(false);

        // Check the number of paths and verify the special injected values.
        assertThat(bcp47Data).getPaths().hasSize(4 + EXPECTED_ALIAS_MAP.size());
        EXPECTED_ALIAS_MAP.forEach((p, v) -> assertThat(bcp47Data).hasValuesFor(p, v));

        // The key-map is only ever in the main bcp47 data and contains the timezone key.
        assertThat(bcp47Data).hasEmptyValue("/keyMap/tz");
        assertThat(bcp47Data).hasEmptyValue("/keyMap/bar");
        assertThat(bcp47Data).hasEmptyValue("/typeMap/bar/four");
        assertThat(bcp47Data).hasEmptyValue("/typeMap/bar/five");

        IcuData tzData = icuData.get(1);
        assertThat(tzData).hasName("timezoneTypes");
        assertThat(tzData).hasFallback(false);

        // Only the type-map paths/values are split into the timezone data.
        assertThat(tzData).getPaths().hasSize(3);
        assertThat(tzData).hasEmptyValue("/typeMap/tz/one");
        assertThat(tzData).hasEmptyValue("/typeMap/tz/two");
        assertThat(tzData).hasEmptyValue("/typeMap/tz/three");
    }

    @Test
    public void testKeyAliases() {
        CldrData cldrData = cldrData(
            alias("key", "ALIAS", "type"));

        ImmutableList<IcuData> icuData = Bcp47Mapper.process(cldrData);
        IcuData bcp47Data = icuData.get(0);

        // Key aliases are lower-cased (though it's not entirely obvious as to why).
        assertThat(bcp47Data).hasValuesFor("/keyMap/alias", "key");
        assertThat(bcp47Data).hasEmptyValue("/typeMap/alias/type");
    }

    @Test
    public void testTypeAliases_single() {
        CldrData cldrData = cldrData(
            alias("key", null, "type", "main"));

        ImmutableList<IcuData> icuData = Bcp47Mapper.process(cldrData);
        IcuData bcp47Data = icuData.get(0);

        assertThat(bcp47Data).hasEmptyValue("/keyMap/key");
        assertThat(bcp47Data).hasValuesFor("/typeMap/key/main", "type");
    }

    @Test
    public void testTypeAliases_multiple() {
        CldrData cldrData = cldrData(
            alias("key", null, "type", "main", "alias1", "alias2", "alias3"));

        ImmutableList<IcuData> icuData = Bcp47Mapper.process(cldrData);
        IcuData bcp47Data = icuData.get(0);

        assertThat(bcp47Data).hasEmptyValue("/keyMap/key");
        assertThat(bcp47Data).hasValuesFor("/typeMap/key/main", "type");

        // Only aliases after the first (main) one go in the typeAlias set.
        assertThat(bcp47Data).getPaths().doesNotContain(RbPath.parse("typeAlias/key/main"));
        assertThat(bcp47Data).hasValuesFor("/typeAlias/key/alias1", "main");
        assertThat(bcp47Data).hasValuesFor("/typeAlias/key/alias2", "main");
        assertThat(bcp47Data).hasValuesFor("/typeAlias/key/alias3", "main");
    }

    @Test
    public void testKeyAndTypeAliases() {
        CldrData cldrData = cldrData(
            alias("key", "key-alias", "type", "main", "type-alias"));

        ImmutableList<IcuData> icuData = Bcp47Mapper.process(cldrData);
        IcuData bcp47Data = icuData.get(0);

        assertThat(bcp47Data).hasValuesFor("/keyMap/key-alias", "key");
        assertThat(bcp47Data).hasValuesFor("/typeMap/key-alias/main", "type");
        assertThat(bcp47Data).hasValuesFor("/typeAlias/key-alias/type-alias", "main");
    }

    @Test
    public void testPreferredTypeName() {
        CldrData cldrData = cldrData(
            deprecated("deprecated-key", true, "type", false, "/preferred/path1"),
            deprecated("key", false, "deprecated-type", true, "/preferred/path2"));

        ImmutableList<IcuData> icuData = Bcp47Mapper.process(cldrData);
        IcuData bcp47Data = icuData.get(0);

        assertThat(bcp47Data).hasValuesFor("/bcpTypeAlias/deprecated-key/type", "/preferred/path1");
        assertThat(bcp47Data).hasValuesFor("/bcpTypeAlias/key/deprecated-type", "/preferred/path2");
    }

    @Test
    public void testInfoAttributes() {
        CldrData cldrData = cldrData(
            // Deprecated without a replacement.
            deprecated("deprecated-key", true, "type", false, null),
            deprecated("key", false, "deprecated-type", true, null),
            valueType("info-key", "info-type", "value-type"));

        ImmutableList<IcuData> icuData = Bcp47Mapper.process(cldrData);
        IcuData bcp47Data = icuData.get(0);

        assertThat(bcp47Data).hasEmptyValue("/keyMap/deprecated-key");
        assertThat(bcp47Data).hasEmptyValue("/typeMap/deprecated-key/type");
        assertThat(bcp47Data).hasValuesFor("/keyInfo/deprecated/deprecated-key", "true");

        assertThat(bcp47Data).hasEmptyValue("/keyMap/key");
        assertThat(bcp47Data).hasEmptyValue("/typeMap/key/deprecated-type");
        assertThat(bcp47Data).hasValuesFor("/typeInfo/deprecated/key/deprecated-type", "true");

        assertThat(bcp47Data).hasEmptyValue("/keyMap/info-key");
        assertThat(bcp47Data).hasEmptyValue("/typeMap/info-key/info-type");
        assertThat(bcp47Data).hasValuesFor("/keyInfo/valueType/info-key", "value-type");
    }

    // This will hopefully one day be the responsibility of the IcuTextWriter.
    @Test
    public void testTimezonePathQuotingForAliases() {
        CldrData cldrData = cldrData(
            alias("tz", null, "escaped", "foo/bar", "hello/world"),
            alias("tz", null, "unescaped", "foo_bar", "hello_world"));

        ImmutableList<IcuData> icuData = Bcp47Mapper.process(cldrData);
        IcuData tzData = icuData.get(1);

        // Only the type-map paths/values are split into the timezone data.
        assertThat(tzData).getPaths().hasSize(4);
        assertThat(tzData).hasValuesFor("/typeMap/tz/foo_bar", "unescaped");
        assertThat(tzData).hasValuesFor("/typeAlias/tz/hello_world", "foo_bar");

        // TODO: Raise bug - having alias target "foo/bar" not match the key "foo:bar" is a bug!
        assertThat(tzData).hasValuesFor("/typeMap/tz/\"foo:bar\"", "escaped");
        assertThat(tzData).hasValuesFor("/typeAlias/tz/\"hello:world\"", "foo/bar");
    }

    private static CldrData cldrData(CldrValue... values) {
        return CldrDataSupplier.forValues(Arrays.asList(values));
    }

    private static CldrValue simpleType(String keyName, String typeName) {
        StringBuilder cldrPath = new StringBuilder("//ldmlBCP47/keyword");
        cldrPath.append("/key");
        appendAttribute(cldrPath, "name", keyName);
        cldrPath.append("/type");
        appendAttribute(cldrPath, "name", typeName);
        return CldrValue.parseValue(cldrPath.toString(), "");
    }

    private static CldrValue alias(
        String keyName, String keyAlias, String typeName, String... typeAliases) {

        StringBuilder cldrPath = new StringBuilder("//ldmlBCP47/keyword");
        cldrPath.append("/key");
        appendAttribute(cldrPath, "name", keyName);
        if (keyAlias != null) {
            appendAttribute(cldrPath, "alias", keyAlias);
        }
        cldrPath.append("/type");
        appendAttribute(cldrPath, "name", typeName);
        if (typeAliases.length > 0) {
            appendAttribute(cldrPath, "alias", Joiner.on(" ").join(typeAliases));
        }
        return CldrValue.parseValue(cldrPath.toString(), "");
    }

    private static CldrValue deprecated(
        String keyName,
        boolean keyDeprecated,
        String typeName,
        boolean typeDeprecated,
        String preferred) {

        StringBuilder cldrPath = new StringBuilder("//ldmlBCP47/keyword");
        cldrPath.append("/key");
        appendAttribute(cldrPath, "name", keyName);
        if (keyDeprecated) {
            appendAttribute(cldrPath, "deprecated", keyDeprecated);
        }
        cldrPath.append("/type");
        appendAttribute(cldrPath, "name", typeName);
        if (preferred != null) {
            appendAttribute(cldrPath, "preferred", preferred);
        }
        if (typeDeprecated) {
            appendAttribute(cldrPath, "deprecated", typeDeprecated);
        }
        return CldrValue.parseValue(cldrPath.toString(), "");
    }

    private static CldrValue valueType(String keyName, String typeName, String valueType) {
        StringBuilder cldrPath = new StringBuilder("//ldmlBCP47/keyword");
        cldrPath.append("/key");
        appendAttribute(cldrPath, "name", keyName);
        appendAttribute(cldrPath, "valueType", valueType);
        cldrPath.append("/type");
        appendAttribute(cldrPath, "name", typeName);
        return CldrValue.parseValue(cldrPath.toString(), "");
    }

    private static void appendAttribute(StringBuilder out, String k, Object v) {
        out.append(String.format("[@%s=\"%s\"]", k, v));
    }
}