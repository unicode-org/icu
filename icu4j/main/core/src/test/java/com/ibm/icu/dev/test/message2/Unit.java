// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.List;
import java.util.StringJoiner;

// Class corresponding to the json test files.
// Since this is serialized by Gson, the field names should match the keys in the .json files.
class Unit {
    // For why this is not an ArrayList<String>, see StringToListAdapter.java
    final Sources src;
    final String locale;
    final Param[] params;
    final String exp;
    final String ignoreJava;
    final List<Error> expErrors;

    Unit(
            Sources src,
            String locale,
            Param[] params,
            String exp,
            String ignoreJava,
            List<Error> expErrors) {
        this.src = src;
        this.locale = locale;
        this.params = params;
        this.exp = exp;
        this.ignoreJava = ignoreJava;
        this.expErrors = expErrors;
    }

    class Error {
        final String name;
        final String type;

        Error(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return "Error ["
                    + (name != null ? "name=" + name + ", " : "")
                    + (type != null ? "type=" + type : "")
                    + "]";
        }
    }

    @Override
    public String toString() {
        StringJoiner result = new StringJoiner(", ", "UnitTest {", "}");
        if (src != null) {
            result.add("src=" + src.sources.toString());
        }
        if (params != null) {
            result.add("params=" + params);
        }
        if (exp != null) {
            result.add("exp=" + escapeString(exp));
        }
        return result.toString();
    }

    /**
     * Creates and returns a new Unit created by merging the current unit with the `other` one.
     *
     * <p>Each value in `other`, if not null, will override the corresponding current value.</p>
     *
     * @param other the unit to merge into the current one
     * @return a new unit created by merging `this` unit and `other`
     */
    public Unit merge(Unit other) {
        Sources newSrc = other.src != null ? other.src : this.src;
        String newLocale = other.locale != null ? other.locale : this.locale;
        Param[] newParams = other.params != null ? other.params : this.params;
        String newExp = other.exp != null ? other.exp : this.exp;
        String newIgnore = other.ignoreJava != null ? other.ignoreJava : this.ignoreJava;
        List<Error> newExpErrors = other.expErrors != null ? other.expErrors : this.expErrors;
        return new Unit(newSrc, newLocale, newParams, newExp, newIgnore, newExpErrors);
    }

    private static String escapeString(String str) {
        if (str == null) {
            return "null";
        }

        StringBuilder result = new StringBuilder();
        str.chars().forEach(c -> {
                switch (c) {
                    case '\\': result.append("\\\\"); break;
                    case '\t': result.append("\\t"); break;
                    case '\n': result.append("\\n"); break;
                    case '\r': result.append("\\r"); break;
                    default:
                        if (c < 0x0020 || (c >= 0x3000 && c <= 3020)) {
                            result.append(String.format("\\u%04X", c));
                        } else {
                            result.append((char) c);
                        }
                }
        });
        return "\"" + result.toString() + "\"";
    }
}
