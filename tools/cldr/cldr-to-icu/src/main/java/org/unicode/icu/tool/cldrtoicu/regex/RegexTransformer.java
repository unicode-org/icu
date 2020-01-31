// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.regex;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;
import static java.util.function.Function.identity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSetMultimap;

/**
 * Path/value transformer configured by {@code ldml2icu_xxx.txt} mapping and configuration files.
 * See {@code ldml2icu_readme.txt} for details on the configuration file format and
 * {@link PathValueTransformer} for the public API description and usage.
 *
 * <p>This class is thread safe.
 */
// TODO: Rewrite the readme to match current behaviour and describe edge cases properly.
public final class RegexTransformer extends PathValueTransformer {
    /**
     * Returns a new transformer based on transformation rules defined in the given configuration
     * file contents, and using the specified functions for resolving ICU values.
     */
    public static PathValueTransformer fromConfigLines(
        List<String> lines, NamedFunction... functions) {
        return new RegexTransformer(RuleParser.parseConfig(lines, Arrays.asList(functions)));
    }

    // Map of path prefixes grouped by DTD type (for early efficient filtering of paths).
    private final ImmutableSetMultimap<CldrDataType, String> prefixMap;
    // Transformation rules loading from the configuration file, grouped by path prefix.
    private final ImmutableListMultimap<String, Rule> rulesMap;
    // Functions which can generate a fallback value from a given resource bundle path.
    private final ImmutableList<BiFunction<RbPath, DynamicVars, Optional<Result>>> fallbackFunctions;
    // Records the total set of rules, removing them as they are matched. Used for reporting any
    // unused rules for debugging purposes.
    private final Set<Rule> unusedRules = new LinkedHashSet<>();

    private RegexTransformer(List<Rule> rules) {
        this.prefixMap =
            rules.stream().collect(toImmutableSetMultimap(Rule::getDataType, Rule::getPathPrefix));
        this.rulesMap =
            rules.stream().collect(toImmutableListMultimap(Rule::getPathPrefix, identity()));
        this.fallbackFunctions =
            rules.stream().flatMap(Rule::getFallbackFunctions).collect(toImmutableList());
        // Add all rules first and remove as they are matched.
        this.unusedRules.addAll(rules);
    }

    @Override
    public ImmutableList<Result> transform(CldrValue value) {
        return transform(value, p -> null);
    }

    @Override
    public ImmutableList<Result> transform(CldrValue value, DynamicVars varLookupFn) {
        // This early rejection of non-matching paths, combined with "bucketing" the rules by path
        // path prefix for easy lookup dramatically reduces the transformation time.
        String pathPrefix = getPathPrefix(value);
        if (!prefixMap.get(value.getDataType()).contains(pathPrefix)) {
            return ImmutableList.of();
        }
        // Even though this is just derived from the value, resolve it here and pass it into each
        // rule to avoid recalculating the same thing every time.
        String fullXPath = getFullXPathWithoutSortIndices(value);
        // Bucketing the rules by the path prefix means that each incoming value is only tested
        // against likely matches. This reduces the number of tests per value by about 10x.
        for (Rule rule : rulesMap.get(pathPrefix)) {
            // We break after the first matching rule, since there is an implicit assumption
            // that no paths will match more than one rule.
            // TODO: Add a debug mode that checks that only one rule matches any given CLDR path.
            ImmutableList<Result> results = rule.transform(value, fullXPath, varLookupFn);
            if (!results.isEmpty()) {
                unusedRules.remove(rule);
                return results;
            }
        }
        return ImmutableList.of();
    }

    // All "leaf" paths must have at least two elements, so we can find the "prefix" which is
    // the first element after the DTD root. This corresponds to the value extracted via
    // PATH_SPEC_PREFIX in the parser.
    private static String getPathPrefix(CldrValue value) {
        CldrPath prefix = value.getPath();
        checkArgument(prefix.getLength() >= 2, "unexpectedly short path: %s", prefix);
        while (prefix.getLength() > 2) {
            prefix = prefix.getParent();
        }
        return prefix.getName();
    }

    // A regex to capture any sort-indices in the full path string (which must be removed).
    private static final Pattern SORT_INDEX = Pattern.compile("(/\\w+)#[0-9]+");

    // Note that the full path we get here contains the "sort index" suffix for ORDERED
    // elements. This means that some element names are "foo#N" where N is the sort index.
    // Since the regex transformer works around "ordered elements" in a completely different
    // way and doesn't have them in the regular expressions, we can just remove them.
    private static String getFullXPathWithoutSortIndices(CldrValue v) {
        String fullPath = v.getFullPath();
        for (CldrPath p = v.getPath(); p != null; p = p.getParent()) {
            if (p.getSortIndex() != -1) {
                // Only do expensive regex stuff if there's an "ordered" element with a sort index.
                return SORT_INDEX.matcher(fullPath).replaceAll("$1");
            }
        }
        // No path parts have a sort index, so the original full path string is safe to return.
        return fullPath;
    }

    @Override
    public ImmutableList<Result> getFallbackResultsFor(RbPath rbPath, DynamicVars varLookupFn) {
        return fallbackFunctions.stream()
            .map(f -> f.apply(rbPath, varLookupFn))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toImmutableList());
    }

    @Override public String toString() {
        StringWriter buf = new StringWriter();
        PrintWriter out = new PrintWriter(buf);
        out.println(getClass().getName() + "{");
        out.println("  Rules: " + rulesMap.size());
        if (!unusedRules.isEmpty()) {
            out.println("  Unused Rules:");
            unusedRules.forEach(
                r -> out.format("    [line=%3d] %s\n", r.getLineNumber(), r.getXpathSpec()));
        }
        out.println('}');
        out.flush();
        return buf.toString();
    }

    // Package use helper for substituting single-character place-holders like '$N' or '%X'.
    static String substitute(String s, char token, Function<Character, String> replaceFn) {
        if (s.indexOf(token) == -1) {
            return s;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        for (int j = s.indexOf(token); j != -1; i = j + 2, j = s.indexOf(token, i)) {
            char varChar = s.charAt(j + 1);
            String replacement =
                checkNotNull(replaceFn.apply(varChar), "no such variable %s%s", token, varChar);
            out.append(s, i, j).append(replacement);
        }
        return out.append(s.substring(i)).toString();
    }
}
