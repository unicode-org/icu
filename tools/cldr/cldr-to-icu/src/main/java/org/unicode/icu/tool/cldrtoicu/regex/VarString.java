// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.regex;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * An immutable representation of a String with placeholders for variable substitution. A
 * VarString can be "resolved" or "partially resolved" by providing a mapping from placeholder
 * characters to strings, and any remaining unresolved variables are tracked. This is a very
 * private bit of implementation detail with a far from ideal API, so it's probably best not to
 * use it elsewhere without careful thought.
 */
final class VarString {
    private static final CharMatcher VAR_CHAR = CharMatcher.inRange('A', 'Z');

    static VarString of(String varString) {
        ImmutableSet.Builder<Character> requiredChars = ImmutableSet.builder();
        // Variable placeholders are any % followed by upper-case ASCII letter (A-Z).
        // Other '%' chars are ignored.
        for (int i = 0; i < varString.length() - 1; i++) {
            if (varString.charAt(i) == '%') {
                char c = varString.charAt(i + 1);
                if (VAR_CHAR.matches(c)) {
                    requiredChars.add(c);
                }
            }
        }
        return new VarString(varString, requiredChars.build(), ImmutableMap.of());
    }

    static VarString of(String s, Function<Character, String> varFn) {
        return of(s).apply(varFn);
    }

    private final String varString;
    private final ImmutableSet<Character> requiredChars;
    private final ImmutableMap<Character, String> varMap;

    private VarString(
        String varString,
        ImmutableSet<Character> requiredChars,
        ImmutableMap<Character, String> varMap) {
        this.varString = checkNotNull(varString);
        this.requiredChars = checkNotNull(requiredChars);
        this.varMap = checkNotNull(varMap);
    }

    /** Applies a variable function to produce a new, potentially resolved, VarString. */
    VarString apply(Function<Character, String> varFn) {
        ImmutableMap.Builder<Character, String> newVarMap = ImmutableMap.builder();
        newVarMap.putAll(this.varMap);
        for (Character c : requiredChars) {
            if (!varMap.containsKey(c)) {
                // Allowed to return null if the function cannot resolve a variable.
                String v = varFn.apply(c);
                if (v != null) {
                    newVarMap.put(c, v);
                }
            }
        }
        return new VarString(varString, requiredChars, newVarMap.build());
    }

    /** Returns a resolved value if all variables are available for substitution. */
    Optional<String> resolve() {
        return varMap.keySet().equals(requiredChars)
            ? Optional.of(
                RegexTransformer.substitute(varString, '%', c -> varMap.getOrDefault(c, "%" + c)))
            : Optional.empty();
    }

    /** Returns the resolved value or fails if not all variables are available. */
    String get() {
        checkState(varMap.keySet().equals(requiredChars), "unresolved variable string: %s", this);
        return RegexTransformer.substitute(varString, '%', c -> varMap.getOrDefault(c, "%" + c));
    }

    @Override public String toString() {
        return varString + ": " + varMap;
    }
}
