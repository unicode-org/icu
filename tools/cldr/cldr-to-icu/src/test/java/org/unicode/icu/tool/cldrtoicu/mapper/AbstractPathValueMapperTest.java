// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.truth.Truth.assertThat;
import static org.unicode.icu.tool.cldrtoicu.testing.AssertUtils.assertThrows;
import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbValue;
import org.unicode.icu.tool.cldrtoicu.testing.FakeResult;
import org.unicode.icu.tool.cldrtoicu.testing.FakeTransformer;

@RunWith(JUnit4.class)
public class AbstractPathValueMapperTest {
    @Test
    public void testUngroupedConcatenation() {
        FakeMapper mapper = new FakeMapper();
        mapper.addUngroupedResult("foo/bar", "one", "two");
        mapper.addUngroupedResult("foo/baz", "other", "path");
        mapper.addUngroupedResult("foo/bar", "three", "four");
        IcuData icuData = mapper.addIcuData("foo");

        assertThat(icuData).getPaths().hasSize(2);
        assertThat(icuData).hasValuesFor("foo/bar", singletonValues("one", "two", "three", "four"));
        assertThat(icuData).hasValuesFor("foo/baz", singletonValues("other", "path"));
    }

    @Test
    public void testGrouping() {
        FakeMapper mapper = new FakeMapper();
        mapper.addGroupedResult("foo/bar", "one", "two");
        mapper.addGroupedResult("foo/baz", "other", "path");
        mapper.addGroupedResult("foo/bar", "three", "four");
        IcuData icuData = mapper.addIcuData("foo");

        assertThat(icuData).getPaths().hasSize(2);
        assertThat(icuData)
            .hasValuesFor("foo/bar", RbValue.of("one", "two"), RbValue.of("three", "four"));
        assertThat(icuData)
            .hasValuesFor("foo/baz", RbValue.of("other", "path"));
    }

    @Test
    public void testFallbackResults() {
        // The indices are important in matching up the results and their respective fallbacks.
        Result explicit1 = FakeResult.of("foo/bar", 1, false, "one");
        Result explicit2 = FakeResult.of("foo/bar", 2, false, "two");
        Result explicit3 = FakeResult.of("foo/bar", 3, false, "three");

        Result fallback1 = FakeResult.fallback("foo/bar", 1, "<ONE>");
        Result fallback2 = FakeResult.fallback("foo/bar", 2, "<TWO>");
        Result fallback3 = FakeResult.fallback("foo/bar", 3, "<THREE>");

        FakeTransformer transformer = new FakeTransformer();
        transformer.addFallbacks("foo/bar", fallback1, fallback2, fallback3);

        // When all results are explicitly present, no fallbacks are used.
        IcuData noFallback = new FakeMapper(transformer)
            .addResult(explicit1)
            .addResult(explicit2)
            .addResult(explicit3)
            .addIcuData("foo");
        assertThat(noFallback).hasValuesFor("foo/bar", singletonValues("one", "two", "three"));

        // Missing explicit results trigger fallbacks.
        IcuData firstFallback = new FakeMapper(transformer)
            .addResult(explicit2)
            .addResult(explicit3)
            .addIcuData("foo");
        assertThat(firstFallback).hasValuesFor("foo/bar", singletonValues("<ONE>", "two", "three"));

        // Fallbacks can appear in any part of the result sequence.
        IcuData lastFallbacks = new FakeMapper(transformer)
            .addResult(explicit1)
            .addIcuData("foo");
        assertThat(lastFallbacks)
            .hasValuesFor("foo/bar", singletonValues("one", "<TWO>", "<THREE>"));

        // Without a single result to "seed" the fallback group, nothing is emitted.
        IcuData allFallbacks = new FakeMapper(transformer).addIcuData("foo");
        assertThat(allFallbacks).getPaths().isEmpty();
    }

    @Test
    public void testAliases_ungrouped() {
        FakeMapper mapper = new FakeMapper();
        mapper.addUngroupedResult("foo/default", "start", "/alias/target", "end");
        mapper.addUngroupedResult("foo/alias-0", "start", "/alias/target[0]", "end");
        mapper.addUngroupedResult("foo/alias-1", "start", "/alias/target[1]", "end");
        mapper.addUngroupedResult("foo/alias-2", "start", "/alias/target[2]", "end");
        mapper.addUngroupedResult("alias/target", "first", "second", "third");
        IcuData icuData = mapper.addIcuData("foo");

        assertThat(icuData).getPaths().hasSize(5);
        assertThat(icuData)
            .hasValuesFor("foo/default", singletonValues("start", "first", "end"));
        assertThat(icuData)
            .hasValuesFor("foo/alias-0", singletonValues("start", "first", "end"));
        assertThat(icuData)
            .hasValuesFor("foo/alias-1", singletonValues("start", "second", "end"));
        assertThat(icuData)
            .hasValuesFor("foo/alias-2", singletonValues("start", "third", "end"));
        assertThat(icuData)
            .hasValuesFor("alias/target", singletonValues("first", "second", "third"));
    }

    // Grouping ignores aliases.
    @Test
    public void testAliases_grouped() {
        FakeMapper mapper = new FakeMapper();
        mapper.addGroupedResult("foo/bar", "grouped", "/alias/target");
        mapper.addGroupedResult("foo/bar", "/alias/target[1]");
        mapper.addUngroupedResult("alias/target", "first", "second");

        IcuData icuData = mapper.addIcuData("foo");
        assertThat(icuData).getPaths().hasSize(2);
        assertThat(icuData)
            .hasValuesFor("foo/bar",
                RbValue.of("grouped", "/alias/target"),
                RbValue.of("/alias/target[1]"));
        assertThat(icuData).hasValuesFor("alias/target", singletonValues("first", "second"));
    }

    @Test
    public void testAliases_explicit() {
        FakeMapper mapper = new FakeMapper();
        mapper.addUngroupedResult("foo/bar:alias", "/alias/target");
        mapper.addUngroupedResult("foo/bar", "/alias/target");
        mapper.addUngroupedResult("alias/target", "alias-value");
        IcuData icuData = mapper.addIcuData("foo");

        assertThat(icuData).getPaths().hasSize(3);
        assertThat(icuData).hasValuesFor("foo/bar:alias", singletonValues("/alias/target"));
        assertThat(icuData).hasValuesFor("foo/bar", singletonValues("alias-value"));
        assertThat(icuData).hasValuesFor("alias/target", singletonValues("alias-value"));
    }

    @Test
    public void testAliases_ordering() {
        // It doesn't matter where an alias is in the order of results.
        FakeMapper mapper = new FakeMapper();
        mapper.addUngroupedResult("first/alias", "hello");
        mapper.addUngroupedResult("foo/bar", "/first/alias", "/last/alias");
        mapper.addUngroupedResult("last/alias", "world");
        IcuData icuData = mapper.addIcuData("foo");

        assertThat(icuData).hasValuesFor("foo/bar", singletonValues("hello", "world"));
    }

    @Test
    public void testAliases_concatenation() {
        // It doesn't matter where an alias is in the order of results.
        FakeMapper mapper = new FakeMapper();
        mapper.addUngroupedResult("alias/target", "hello");
        mapper.addUngroupedResult("foo/bar", "/alias/target[0]", "/alias/target[1]");
        mapper.addUngroupedResult("alias/target", "world");
        IcuData icuData = mapper.addIcuData("foo");

        assertThat(icuData).hasValuesFor("foo/bar", singletonValues("hello", "world"));
    }

    @Test
    public void testAliases_missing() {
        FakeMapper mapper = new FakeMapper();
        mapper.addUngroupedResult("alias/target", "value");
        mapper.addUngroupedResult("foo/bar", "/no-such-alias/target");
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> mapper.addIcuData("foo"));
        assertThat(e).hasMessageThat().contains("no such alias value");
        assertThat(e).hasMessageThat().contains("/no-such-alias/target");
    }

    @Test
    public void testAliases_badIndex() {
        FakeMapper mapper = new FakeMapper();
        mapper.addUngroupedResult("alias/target", "value");
        mapper.addUngroupedResult("foo/bar", "/alias/target[1]");
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> mapper.addIcuData("foo"));
        assertThat(e).hasMessageThat().contains("out of bounds");
        assertThat(e).hasMessageThat().contains("/alias/target[1]");
    }

    @Test
    public void testAliases_noRecursion() {
        FakeMapper mapper = new FakeMapper();
        mapper.addUngroupedResult("alias/target", "/other/alias");
        mapper.addUngroupedResult("other/alias", "/other/alias");
        mapper.addUngroupedResult("foo/bar", "/alias/target");
        IllegalStateException e =
            assertThrows(IllegalStateException.class, () -> mapper.addIcuData("foo"));
        assertThat(e).hasMessageThat().contains("recursive alias resolution is not supported");
    }

    @Test
    public void testAliases_explicitAliasesAreSingletonOnly() {
        FakeMapper mapper = new FakeMapper();
        mapper.addUngroupedResult("foo/bar:alias", "first", "second");
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> mapper.addIcuData("foo"));
        assertThat(e).hasMessageThat().contains("explicit aliases must be singleton values");
        assertThat(e).hasMessageThat().contains("foo/bar:alias");
    }

    private static final class FakeMapper extends AbstractPathValueMapper {
        private final static CldrData EXPLODING_DATA =
            new CldrData() {
                @Override public void accept(PathOrder pathOrder, ValueVisitor valueVisitor) {
                    throw new UnsupportedOperationException("should not be called by test");
                }

                @Override public void accept(PathOrder pathOrder, PrefixVisitor prefixVisitor) {
                    throw new UnsupportedOperationException("should not be called by test");
                }

                @Override public CldrValue get(CldrPath cldrPath) {
                    throw new UnsupportedOperationException("should not be called by test");
                }
            };

        // This preserves insertion order in a well defined way (good for testing alias order).
        private final List<Result> fakeResults = new ArrayList<>();

        FakeMapper() {
            this(new FakeTransformer());
        }

        FakeMapper(FakeTransformer transformer) {
            super(EXPLODING_DATA, transformer);
        }

        // Helper method to neaten up the tests a bit.
        IcuData addIcuData(String localeId) {
            IcuData icuData = new IcuData(localeId, true);
            addIcuData(icuData);
            return icuData;
        }

        FakeMapper addUngroupedResult(String path, String... values) {
            int index = fakeResults.size() + 1;
            return addResult(FakeResult.of(path, index, false, values));
        }

        FakeMapper addGroupedResult(String path, String... values) {
            int index = fakeResults.size() + 1;
            return addResult(FakeResult.of(path, index, true, values));
        }

        FakeMapper addResult(Result r) {
            fakeResults.add(r);
            return this;
        }

        @Override void addResults() {
            fakeResults.forEach(result -> addResult(result.getKey(), result));
        }
    }

    private static RbValue[] singletonValues(String... values) {
        return Arrays.stream(values).map(RbValue::of).toArray(RbValue[]::new);
    }
}