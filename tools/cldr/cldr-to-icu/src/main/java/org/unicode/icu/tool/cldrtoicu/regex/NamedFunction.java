// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.regex;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.function.Function;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

/**
 * Function used by {@code RegexTransformer} to convert CLDR values in special ways. See also
 * {@code IcuFunctions}.
 */
public final class NamedFunction implements Function<List<String>, String> {
    private static final CharMatcher NAME_CHARS =
        CharMatcher.inRange('a', 'z').or(CharMatcher.is('_'));
    private static final Splitter ARG_SPLITTER = Splitter.on(',').trimResults(whitespace());

    public static NamedFunction create(
        String name, int argCount, Function<List<String>, String> fn) {
        return new NamedFunction(name, argCount, fn);
    }

    private final String name;
    private final int maxArgs;
    private final Function<List<String>, String> fn;

    private NamedFunction(String name, int argCount, Function<List<String>, String> fn) {
        checkArgument(!name.isEmpty() && NAME_CHARS.matchesAllOf(name),
            "invalid function name (must be lower_case_underscore): %s", name);
        checkArgument(argCount >= 0, "invalid argument count: %s", argCount);
        this.name = name;
        this.maxArgs = argCount;
        this.fn = checkNotNull(fn);
    }

    public String call(String argList) {
        List<String> args = ARG_SPLITTER.splitToList(argList);
        checkArgument(args.size() <= maxArgs,
            "too many arguments for function '%s' (max=%s)", name, maxArgs);
        return checkNotNull(apply(args),
            "named functions must never return null: function=%s", name);
    }

    public String getName() {
        return name;
    }

    @Override
    public String apply(List<String> args) {
        return fn.apply(args);
    }
}
