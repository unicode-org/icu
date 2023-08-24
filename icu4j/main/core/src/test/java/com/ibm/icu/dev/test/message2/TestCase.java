// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.Map;
import java.util.StringJoiner;

/** Utility class encapsulating what we need for a simple test. */
class TestCase {
        final String message;
        final Locale locale;
        final Map<String, Object> arguments;
        final String expected;
        final boolean ignore;
        final String ignoreReason;
        final List<String> errors;

        @Override
        public String toString() {
            StringJoiner result = new StringJoiner(",\n  ", "TestCase {\n  ", "\n}");
            result.add("message: " + message + "'");
            result.add("locale: '" + locale.toLanguageTag() + "'");
            result.add("arguments: " + arguments);
            result.add("expected: '" + expected + "'");
            result.add("ignore: " + ignore);
            result.add("ignoreReason: '" + ignoreReason + "'");
            result.add("errors: " + errors);
            return result.toString();
        }

        private TestCase(TestCase.Builder builder) {
            this.ignore = builder.ignore;
            this.message = builder.pattern == null ? "" : builder.pattern;
            this.locale = (builder.localeId == null)
                    ? Locale.getDefault(Category.FORMAT)
                    : Locale.forLanguageTag(builder.localeId);
            this.arguments = builder.arguments == null ? Args.NONE : builder.arguments;
            this.expected = builder.expected == null ? "" : builder.expected;
            this.errors = builder.errors == null ? new ArrayList<String>() : builder.errors;
            this.ignoreReason = builder.ignoreReason == null ? "" : builder.ignoreReason;
        }

        static class Builder {
            private String pattern;
            private String localeId;
            private Map<String, Object> arguments;
            private String expected;
            private boolean ignore = false;
            private String ignoreReason;
            private List<String> errors;

            public TestCase build() {
                return new TestCase(this);
            }

            public TestCase.Builder pattern(String pattern) {
                this.pattern = pattern;
                return this;
            }

            public TestCase.Builder patternJs(String patternJs) {
                // Ignore the JavaScript stuff
                return this;
            }

            public TestCase.Builder arguments(Map<String, Object> arguments) {
                this.arguments = arguments;
                return this;
            }

            public TestCase.Builder expected(String expected) {
                this.expected = expected;
                return this;
            }

            public TestCase.Builder errors(String ... errors) {
                this.errors = new ArrayList<>();
                this.errors.addAll(Arrays.asList(errors));
                return this;
            }

            public TestCase.Builder locale(String localeId) {
                this.localeId = localeId;
                return this;
            }

            public TestCase.Builder ignore() {
                this.ignore = true;
                this.ignoreReason = "";
                return this;
            }

            public TestCase.Builder ignore(String reason) {
                this.ignore = true;
                this.ignoreReason = reason;
                return this;
            }
        }
    }