package com.ibm.icu.dev.tool.cldr;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Function used by {@code RegexTransformer} to convert CLDR values in special ways. See also
 * {@link IcuFunctions}.
 */
final class NamedFunction implements Function<List<String>, String> {
    private static final CharMatcher NAME_CHARS = CharMatcher.inRange('a', 'z');
    private static final Splitter ARG_SPLITTER = Splitter.on(',').trimResults(CharMatcher.whitespace());

    public static NamedFunction create(
        String name, int argCount, Function<List<String>, String> fn) {
        return new NamedFunction(name, argCount, fn);
    }

    private final String name;
    private final int maxArgs;
    private final Function<List<String>, String> fn;

    private NamedFunction(String name, int argCount, Function<List<String>, String> fn) {
        checkArgument(!name.isEmpty() && NAME_CHARS.matchesAllOf(name),
            "invalid function name: %s", name);
        checkArgument(argCount >= 0, "invalid argument count: %s", argCount);
        this.name = name;
        this.maxArgs = argCount;
        this.fn = checkNotNull(fn);
    }

    public String call(String argList) {
        List<String> args = ARG_SPLITTER.splitToList(argList);
        checkArgument(args.size() <= maxArgs,
            "too many arguments for function '%s' (max=%s)", name, maxArgs);
        return apply(args);
    }

    public String getName() {
        return name;
    }

    public int getArgCount() {
        return maxArgs;
    }

    @Override
    public String apply(List<String> args) {
        return fn.apply(args);
    }
}
