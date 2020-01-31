// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.regex;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static org.unicode.cldr.api.CldrPath.parseDistinguishingPath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.DynamicVars;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * A specification for building a result from the arguments in a matched xpath. Results always
 * hold a reference to their originating specification to allow them to be ordered in the same
 * order as the corresponding specifications in the configuration file.
 */
final class ResultSpec {
    // Subtle ordering for results to ensure "config file order" for things in the same
    // resource bundle while being "friendly" towards a global ordering. This is NOT consistent
    // with equals if duplicate results exist.
    //
    // This is ESSENTIAL for correct grouping and ordering within resource bundles.
    //
    // In normal use this is expected only to be used to reorder results within a resource
    // bundle (i.e. those sharing the same resource bundle path "key"). Resource bundles
    // themselves can just be managed in "visitation order" or similar.
    //
    // Ordering priority is:
    // 1: Result key (resource bundle):     Groups results by resource bundle.
    // 2: Result specification line number: Orders resource bundle contents by "file order".
    // 3: Result distinguishing xpath:      Tie breaking if duplicates are not yet removed.
    //
    // Note that the currently uses the String representation of the resource bundle path (key)
    // as the primary order to match legacy behaviour. However it would be better to use the
    // natural lexicographical RbPath order (the difference relates to having '/' as the
    // separator in the string representation of the path). The string form of a path is a bad
    // choice because some paths can contain a literal '/', which makes ordering problematic in
    // rare case. However changing this will have the effect of reodering path elements, which
    // while it should be safe, must be done with caution.
    // TODO: Fix this to use RbPath ordering and NOT the String representation
    private static final Comparator<AbstractResult> RESULT_ORDERING =
        Comparator.<AbstractResult, String>comparing(r -> r.getKey().toString())
            .thenComparing(r -> r.getSpec().lineNumber)
            .thenComparing(nullsLast(comparing(r -> r.getPath().orElse(null))));

    // Splitter for any values (either in CLDR data or results specifications). The only time
    // values are split differently is when quoting exists in the "values" instruction.
    private static final Splitter VALUE_SPLITTER = Splitter.on(whitespace()).omitEmptyStrings();

    // Matcher for "&foo_bar(a,b,c)" which captures function name and complete argument list.
    private static final Pattern FUNCTION = Pattern.compile("&(\\w++)\\(([^)]++)\\)");

    // Resource bundle path specification with placeholders (e.g. "/foo/$1/bar") exactly as it
    // appears in the configuration file.
    private final String rbPathSpec;

    // Declared instructions with which to generate result values (see Instruction).
    private final ImmutableMap<Instruction, VarString> instructions;

    // This index of the xpath argument whose value should be split to create multiple results.
    // This mechanism is used when an xpath attribute is a space separated list of values and
    // one result should be created for each value (e.g. [@territories="AA BB CC"] but you want
    // a resource bundle for each region code (e.g. "foo/XX/bar", "foo/YY/bar", "foo/ZZ/bar").
    // At most one argument is ever split (corresponding to the first unquoted placeholder in
    // the resource bundle path specification).
    private final int splitArgIndex;

    // The line number of the result specification in the file which defines the ordering of
    // results within a resource bundle. This needn't be a line number, but must be unique for
    // each specification.
    private final int lineNumber;

    // The named functions available to the parser. Ideally the rules and result specifications
    // would be an inner class of some kind of context/environment and just share this.
    private final ImmutableMap<String, NamedFunction> icuFunctions;

    // The map of dynamic variables (looked up from CldrPaths when a rule is resolved.
    private final Function<Character, CldrPath> dynamicVarFn;

    ResultSpec(
        String rbPathSpec,
        Map<Instruction, VarString> instructions,
        int lineNumber,
        Map<String, NamedFunction> icuFunctions,
        Function<Character, CldrPath> dynamicVarFn) {
        this.rbPathSpec = checkNotNull(rbPathSpec);
        this.instructions = ImmutableMap.copyOf(instructions);
        this.splitArgIndex = getSplitArgIndex(rbPathSpec);
        this.lineNumber = lineNumber;
        this.icuFunctions = ImmutableMap.copyOf(icuFunctions);
        this.dynamicVarFn = checkNotNull(dynamicVarFn);
    }

    /**
     * Transforms a path/value into a sequence of results. The given matcher has successfully
     * matched the path and contains the captured arguments corresponding to $1..$N in the
     * various result specification strings.
     */
    Stream<Result> transform(
        CldrValue value, Matcher m, DynamicVars varLookupFn) {
        // Discard group(0) since that's always the full xpath that was matched, and we don't
        // need that any more (so "$N" is args.get(N - 1)).
        List<String> args = new ArrayList<>();
        for (int i = 1; i <= m.groupCount(); i++) {
            // Important since we turn this into an ImmutableList (which is null-hostile).
            args.add(checkNotNull(m.group(i),
                "captured regex arguments must always be present\n"
                    + "(use an non-capturing groups for optional arguments): %s", m.pattern()));
        }

        // The first unquoted argument in any resource bundle path declaration, is defined as
        // being "splittable". Typically this happens if the value of the captured xpath
        // argument is expected to be a list of items.
        //
        // In this case, we generate one result for each individual argument, replacing the
        // appropriate captured list with each split value in turn. Thus with original
        // arguments:
        //   ["foo", "bar baz", "quux"]
        // where splitArgIndex == 1, we get two results using the argument lists:
        //   ["foo", "bar", "quux"]
        //   ["foo", "baz", "quux"]
        //
        // Note also that since the splittability of the arguments is technically defined
        // by the resource bundle path specification (not the xpath regular expression) it
        // could differ per ResultSpec instance (but currently never does).
        if (splitArgIndex != -1) {
            List<String> splitArgs = VALUE_SPLITTER.splitToList(args.get(splitArgIndex));
            // Only bother if there was more than one argument there anyway.
            if (splitArgs.size() > 1) {
                return splitArgs.stream().map(a -> {
                    args.set(splitArgIndex, a);
                    return matchedResult(value, args, varLookupFn);
                });
            }
        }
        // No splittable argument, or a splittable argument with only one value.
        return Stream.of(matchedResult(value, args, varLookupFn));
    }

    // Simple helper to make results.
    private Result matchedResult(
        CldrValue value, List<String> args, DynamicVars varLookupFn) {
        return new MatchedResult(
            getRbPath(args),
            getValues(value.getValue(), args),
            getResultPath(value.getPath(), args, varLookupFn));
    }

    // Resource bundle paths are a bit special (unsurprisingly). The captured arguments can
    // contain '/' and will extend the path structure. Thus "foo/$1/bar" might end up as
    // "foo/x/y/bar" after argument substitution.
    //
    // However (a hack for timezone "metazone" paths) if the argument placeholder is quoted
    // (e.g. "foo/"$1"/bar") then '/' in arguments is replaced by ':' and quotes are retained
    // (e.g. "foo/"x:y"/bar).
    // TODO: Replace hard coded hack here with an explicit function in the config file.
    private RbPath getRbPath(List<String> args) {
        // Without more careful parsing, it's hard to figure out it quotes in a resource bundle
        // path specification are around a placeholder or not. Since quotes are only used in a
        // small number of cases currently, and only for this purpose, we just assume that any
        // quotes in the path specification should trigger this behaviour.
        if (rbPathSpec.contains("\"")) {
            // Use a lazy transforming list to avoid char replacement in arguments that don't
            // appear in the resource bundle path.
            args = Lists.transform(args, s -> s.replace('/', ':'));
        }
        String path = substituteArgs(rbPathSpec, args);
        return RbPath.parse(path);
    }

    // Create an array of output values according to the CLDR value (if present) and the
    // "values" instruction in the result specification (if present). Any functions present in
    // the "values" instruction are invoked here.
    private ImmutableList<String> getValues(String value, List<String> args) {
        VarString valuesSpec = instructions.get(Instruction.VALUES);
        if (valuesSpec == null) {
            // No "values" instruction, so just use the _unsplit_ CLDR value. To split a CLDR
            // value use "values={value}" in the result specification.
            return ImmutableList.of(value);
        }
        // The "value" instruction is not expected to have any dynamic %N variables in it,
        // since those only represent CLDR path mappings, which should not be directly present
        // in the ICU data. Hence the valueSpec should have been fully resolved by the static
        // variables applied earlier and we should just need to resolve() it into a String.
        String resolved = valuesSpec.get();

        // First substitute the $N arguments in since they need to be passed to the
        // functions.
        //
        // WARNING: This doesn't strictly work, since an argument or function result could
        // (in theory) contain the string "{value}" which would then be substituted in an
        // unexpected way. The better way to do this is with a single pass which handles
        // arguments, function calling and the special "{value}" token together. This comes
        // down to the fact that the mapping file syntax doesn't have a well defined concept
        // of escaping or invocation order.
        // TODO: Fix this, possibly by rewriting the whole transformer "language" to be consistent.
        resolved = substituteArgs(resolved, args);

        Matcher m = FUNCTION.matcher(resolved);
        if (m.find()) {
            StringBuilder buffer = new StringBuilder();
            int index = 0;
            do {
                // Append up to the start of the function call.
                buffer.append(resolved, index, m.start());

                // Replace '{value}' here so functions can be called with the CLDR value as well
                // as captured path arguments. We also have to replace it below, which is all a bit
                // dodgy if a function every returned '{value}'.
                NamedFunction fn = icuFunctions.get(m.group(1));
                checkArgument(fn != null, "no such function: %s", m.group(1));
                buffer.append(fn.call(m.group(2).replace("{value}", value)));
                index = m.end();
            } while (m.find());
            resolved = buffer.append(resolved.substring(index)).toString();
        }
        // Having done function invocation, we handle the special "{value}" token and split
        // the value (taking quoting into account).
        return splitValues(resolved.replace("{value}", value));
    }

    // IMPORTANT: The path of a result is either:
    // * The original distinguishing path
    // * The specified "base_xpath" (which must also be a distinguishing xpath).
    // and this is used as part of the equality semantics (which are very subtle).
    //
    // The existence of "base_xpath" is a hack to get around the fact the xpaths can only be
    // matched in full, rather than by a prefix. For some cases this means that the "same"
    // result will be created many times by potentially different distinguishing xpaths,
    // perhaps even via different result specifications. "base_xpath" exists as a hack to give
    // these duplicate results the same "fake" xpath, so deduplication can occur.
    private CldrPath getResultPath(CldrPath path, List<String> args, DynamicVars varLookupFn) {
        VarString basePath = instructions.get(Instruction.BASE_XPATH);
        if (basePath == null) {
            return path;
        }
        String resolvedBasePath = basePath.apply(dynamicVarFn.andThen(varLookupFn)).get();
        return parseDistinguishingPath(substituteArgs(resolvedBasePath, args));
    }

    /**
     * Returns a fallback function if this specification has the "fallback=" instruction.
     * The function takes a resolved resource bundle path and returns the possible fallback
     * values for it. Note that currently fallback values do not support either quoting or
     * grouping (but they easily could).
     */
    Optional<BiFunction<RbPath, DynamicVars, Optional<Result>>> getFallbackFunction() {
        VarString fallbackSpec = instructions.get(Instruction.FALLBACK);
        if (fallbackSpec == null) {
            return Optional.empty();
        }
        // This is the only place where any hacking of regular expressions occurs. The fallback
        // function must only return a value if the given resolved resource bundle path could
        // have been a match for the path specification.
        //
        // In order to avoid ambiguity for paths such as "foo/$1/$2/bar" and "foo/$1/bar" which
        // should not both be matched, we explicitly disallow '/' in argument values. In theory
        // this is problematic, since '/' should be an allowed character, but the issues caused
        // by ambiguous matching are worse.
        // TODO: Fix/replace all of this fallback mess with something cleaner.
        Pattern rbPathMatcher = getRbPathMatcher(rbPathSpec);

        // Another, frankly terrifying, bit of hackery to support fallback specifications with
        // $N argument substitution (this currently only happens once, but must be supported).
        // Just another reason to want to replace the current fallback mechanism.
        fallbackSpec = maybeRewriteFallbackSpec(fallbackSpec);

        // Just copying here to make it effectively final.
        VarString finalFallbackSpec = fallbackSpec;
        return Optional.of(
            (p, varFn) -> getFallbackResult(p, varFn, rbPathMatcher, finalFallbackSpec));
    }

    private Optional<Result> getFallbackResult(
        RbPath rbPath, DynamicVars varFn, Pattern rbPathMatcher, VarString fallbackSpec) {
        // Check is the given rbPath could be associated with this fallback (most are not).
        Matcher matcher = rbPathMatcher.matcher(rbPath.toString());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        // Expect that once any dynamic variables are provided to the fallback specification,
        // we can get the resolved fallback specification (potentially with $N placeholders to
        // be filled in from the resource bundle path).
        String specStr = fallbackSpec.apply(dynamicVarFn.andThen(varFn)).get();
        if (matcher.groupCount() > 0) {
            specStr = substituteArgs(specStr, n -> matcher.group(n + 1), matcher.groupCount());
        }

        // Split the fallback value _without_ considering quoting. This matches the original
        // behaviour but could cause all sorts of subtle issues if values contained quotes.
        // TODO: Rework transformation rules to make quoting behaviour deterministic.
        Iterable<String> values =
            VALUE_SPLITTER.splitToList(specStr).stream()
                // Fallback values that "look like" CLDR paths are auto-magically resolved.
                .map(v -> v.startsWith("//") ? varFn.apply(parseDistinguishingPath(v)) : v)
                .collect(toImmutableList());
        return Optional.of(new FallbackResult(rbPath, values));
    }

    // WARNING: Another very hacky behaviour (used exactly once) is that "$N" argument
    // substitutions are allowed in fallback values. This is highly problematic because
    // since the fallback value must be synthesized only from the resource bundle path,
    // there's no way for this substitution to handle:
    // 1: multi-valued list arguments
    // 2: arguments that didn't appear in the resource bundle path
    // 3: dynamic path variables (e.g. %D=//some/path)
    //
    // An example would be something like a resource bundle specification of:
    //   /Baz/$2/$1
    // and a fallback value of:
    //   Foo$1/Bar$2
    //
    // Here the order of substitution is not maintained and the original path specification
    // has values that are not naturally ordered (or possibly even duplicated). The pattern
    // we calculate from the resource bundle path specification will match/capture groups in
    // "natural order" (i.e. "/Baz/(...)/(...)") so we have to rewrite the order of the
    // placeholders in the fallback specification to match (e.g. "Foo$2/Bar$1").
    // TODO: Figure out a way to remove all of this extreme complexity.
    private VarString maybeRewriteFallbackSpec(
        VarString fallbackSpec) {
        Optional<String> fallback = fallbackSpec.resolve();
        // If the fallback string is not present, it's because the VarString still has
        // unresolved "dynamic" variables for late binding. This is okay, but should not
        // be mixed with argument substitution.
        if (!fallback.isPresent() || !fallback.get().contains("$")) {
            return fallbackSpec;
        }
        // After the quick rejection check for '$', do a proper search for $N variables (since
        // '$' is permitted as a literal if not followed by a digit).
        Matcher fallbackMatcher = ARG_PLACEHOLDER.matcher(fallback.get());
        if (!fallbackMatcher.find()) {
            return fallbackSpec;
        }

        // Fallback spec has $N in it, triggering super hacky behaviour.
        Matcher pathMatcher = ARG_PLACEHOLDER.matcher(rbPathSpec);
        checkState(pathMatcher.find(),
            "$N arguments in fallback must be present in the resource bundle path: %s",
            rbPathSpec);
        // Explicit group characters ("1"..."9") in the order they appear in the
        // resource bundle path. There can be duplicates (e.g. "/Foo/$1/Bar$1").
        List<Character> groupIds = new ArrayList<>();
        do {
            groupIds.add(pathMatcher.group().charAt(1));
        } while (pathMatcher.find());

        // Special check to avoid a horrible bug if we every had more than 9 distinct
        // placeholders (essentially impossible with current data). If it did happen,
        // the returned index below would be >= 9 and we would get "$X", where 'X' was
        // not a numeric value.
        checkState(groupIds.size() < 10,
            "too many placeholders in resource bundle path: %s", rbPathSpec);

        // Now find each placeholder in the fallback specification string and map it to
        // the equivalent index for the path matcher we just created.
        StringBuilder rewrittenFallbackSpec = new StringBuilder(fallback.get());
        do {
            int placeholderPos = fallbackMatcher.start() + 1;
            // The new ID is the index of the corresponding placeholder offset by '1'.
            char placeholderDigit = rewrittenFallbackSpec.charAt(placeholderPos);
            int newPlaceholderIndex = groupIds.indexOf(placeholderDigit);
            checkState(newPlaceholderIndex != -1,
                "fallback values may only contain arguments from the resource bundle path: %s",
                fallback.get());
            rewrittenFallbackSpec.setCharAt(placeholderPos, (char)('1' + newPlaceholderIndex));
        } while (fallbackMatcher.find());
        return VarString.of(rewrittenFallbackSpec.toString());
    }

    /** Base class of either a matched or a fallback result. */
    private abstract class AbstractResult extends Result {
        // Split and resolved values for this result (see also "isGrouped()").
        private final ImmutableList<String> values;

        // The "source" CLDR path of a matched result (omitted if this is a fallback result).
        // Note that this is the resolved "base_xpath" if it was specified in the instructions.
        private final Optional<CldrPath> basePath;

        // Calculated eagerly since we always expect results to need to be deduplicated.
        private final int hashCode;

        AbstractResult(RbPath key, Iterable<String> values, Optional<CldrPath> path) {
            super(key);
            this.values = ImmutableList.copyOf(values);
            this.basePath = checkNotNull(path);
            // Same attributes in the same order as tested for in equals().
            this.hashCode = Objects.hash(getKey(), getPath(), isGrouped(), getValues());
        }

        // Returns the specification from which this result was obtained. This is essential for
        // correct ordering and determining fallback values, but is not directly used for
        // determining result equality (since duplicate results can be generated by different
        // specifications).
        final ResultSpec getSpec() {
            return ResultSpec.this;
        }

        final Optional<CldrPath> getPath() {
            return basePath;
        }

        final boolean wasMatched() {
            // We could also do this via a boolean field.
            return this instanceof MatchedResult;
        }

        @Override
        public final ImmutableList<String> getValues() {
            return values;
        }

        @Override
        public final int compareTo(Result other) {
            checkArgument(other instanceof AbstractResult,
                "unknown result type: %s", other.getClass());
            return RESULT_ORDERING.compare(this, (AbstractResult) other);
        }

        @Override
        public final int hashCode() {
            return hashCode;
        }

        // Equality semantics of results is ESSENTIAL for correct behaviour, especially the
        // deduplication of results. See also "getSpec()", "getPath()", and RESULT_ORDERING.
        @Override
        public final boolean equals(Object obj) {
            // Different subclasses are never equal, so test class directly (not instanceof).
            if (obj == null || !getClass().equals(obj.getClass())) {
                return false;
            }
            AbstractResult other = (AbstractResult) obj;
            // DO NOT test the result specifier here. Equal results can be generated from
            // different result specifications (if "base_xpath" was used).
            return getKey().equals(other.getKey())
                && getPath().equals(other.getPath())
                && isGrouped() == other.isGrouped()
                // Alternatively assert that values are equal if everything else is.
                && getValues().equals(other.getValues());
        }
    }

    // Result created for an explicit path match using captured arguments.
    private final class MatchedResult extends AbstractResult {
        MatchedResult(RbPath key, Iterable<String> values, CldrPath path) {
            super(key, values, Optional.of(path));
        }

        @Override
        public boolean isGrouped() {
            // We don't need to use the "group" value at all and it can be removed from the
            // configuration file at some point.
            return instructions.containsKey(Instruction.GROUP);
        }

        @Override
        public boolean isFallbackFor(Result r) {
            // Matched results are never a fallback for anything.
            return false;
        }
    }

    // Result created to hold possible fallback values for a specified resource bundle path.
    private final class FallbackResult extends AbstractResult {
        FallbackResult(RbPath rbPath, Iterable<String> values) {
            super(rbPath, values, Optional.empty());
        }

        // Delete this method and move the other one into AbstractResult if we decide to allow
        // grouping for fallback values (it's not clear if it's a good idea).
        @Override
        public boolean isGrouped() {
            return false;
        }

        @Override
        public boolean isFallbackFor(Result r) {
            // We are a fallback if we came from the same specification as a matched result.
            // To prevent duplication of fallback results, we also return true if the result we
            // are "equal()" to the given result (equivalent fallback results can come from
            // different input paths).
            checkArgument(r instanceof AbstractResult, "unsupported result type: %s", r);
            AbstractResult result = (AbstractResult) r;
            return result.wasMatched() ? getSpec().equals(result.getSpec()) : equals(result);
        }
    }

    // ==== Static helper functions ====

    // Matches any "$N" placeholder without capturing.
    private static final Pattern ARG_PLACEHOLDER = Pattern.compile("\\$[1-9]");

    // Turn "$N" into a capturing groups.
    //
    // Note that this code currently assumes that each "$N" placeholder matches a single path
    // segment (i.e. the captured values cannot contain '/'). This is an artificial restriction
    // since resource bundle paths can have quoting in, so we could detect quoted placeholders
    // and allow any characters. However at the moment this isn't an issue, and none of the
    // "$N" placeholders in the paths expects to match anything with '/' in.
    //
    // TODO: Fix this to handle quoted placeholders (e.g. "$N" or <$N>) properly.
    private static Pattern getRbPathMatcher(String rbPathSpec) {
        // An RbPath instance's toString() does not have a leading '/' on it, so well have to
        // account for that here (or we could just remove the leading '/' from paths in the
        // config file...
        if (rbPathSpec.startsWith("/")) {
            rbPathSpec = rbPathSpec.substring(1);
        }
        // Protect potential regex meta-characters in the original resource bundle path. Using
        // '\Q' and '\E' to mark quotation boundaries is the safest way to do this, but that
        // means we also need to handle '\E' in the original string (incredibly unlikely but it
        // would be super hard to debug if it ever happened).
        // TODO: If resource paths cannot contain literal '\' or '$', add checks and simplify.
        String regex = "\\Q" + rbPathSpec.replace("\\E", "\\E\\E\\Q") + "\\E";

        // Remember that you could get "$1$2" here and the regex groups that replace them will
        // abut. Use reluctant matching (i.e. "+?") to avoid any backtracking in this case.
        // We assume that the substituted arguments contained at least one character, and so we
        // capture at least one character per group here.
        regex = ARG_PLACEHOLDER.matcher(regex).replaceAll("\\\\E([^/]+?)\\\\Q");
        return Pattern.compile(regex);
    }

    private static String substituteArgs(String spec, List<String> args) {
        return substituteArgs(spec, args::get, args.size());
    }

    // Substitutes "$N" (N = 1...9) placeholders for values obtained from a zero-indexed
    // function (i.e. "$N" --> args(N - 1)).
    private static String substituteArgs(String spec, Function<Integer, String> args, int size) {
        return RegexTransformer.substitute(
            spec, '$', c -> args.apply(checkElementIndex(c - '1', size, "argument index")));
    }

    // Matches arguments with or without enclosing quotes.
    private static final Pattern ARGUMENT = Pattern.compile("[<\"]?\\$(\\d)[\">]?");

    // Logic mostly copied from original RegexManager class. Finds first unquoted $N (N=1..9)
    // and returns N-1 (or -1 if no match). We do not permit $0 to appear even though it is
    // captured by the regex because it's just the entire path.
    private static int getSplitArgIndex(String rbPath) {
        // Captures a $N placeholder, but might catch surrounding quoting as well.
        Matcher matcher = ARGUMENT.matcher(rbPath);
        while (matcher.find()) {
            char startChar = rbPath.charAt(matcher.start());
            char endChar = rbPath.charAt(matcher.end() - 1);
            // Splitting occurs for the first unquoted placeholder, so ignore <$1> and "$N".
            // Q: Why two different "quoting" schemes?
            // A: It's complex and relates the something called "hidden labels".
            boolean shouldSplit = !((startChar == '"' && endChar == '"') ||
                (startChar == '<' && endChar == '>'));
            if (shouldSplit) {
                // Allowed "$N" argument placeholders go from $1 to $9 ($0 is disallowed) and
                // arguments are zero-indexed, so we expect an index from 0 to 8.
                int groupNumber = Integer.parseInt(matcher.group(1));
                checkArgument(groupNumber >= 1 && groupNumber <= 9,
                    "invalid split argument: %s", groupNumber);
                return groupNumber - 1;
            }
        }
        return -1;
    }

    // Splits a possibly quoted string, where we need to handle \". This is a bit dubious
    // though as we don't detect or unescape \\. Thus it's impossible to represent a single '\'
    // at the end of a quoted string (e.g. "$1" where the expansion of $1 has a trailing '\'.
    // It's also impossible to have a value that should be split but which contains '"'.
    //
    // This mimics the original RegexManager behaviour where spaces in and quotes in
    // substituted values are _not_ escaped.
    private static ImmutableList<String> splitValues(String value) {
        int qstart = nextBareQuoteIndex(value,  0);
        if (qstart == -1) {
            return ImmutableList.copyOf(VALUE_SPLITTER.split(value));
        }
        ImmutableList.Builder<String> values = ImmutableList.builder();
        int rawStart = 0;
        do {
            values.addAll(VALUE_SPLITTER.split(value.substring(rawStart, qstart)));
            int qend = nextBareQuoteIndex(value,  qstart + 1);
            checkArgument(qend != -1, "mismatched quotes in splittable value: %s", value);
            // Remember to unescape any '"' found in the quoted regions.
            values.add(value.substring(qstart + 1, qend).replace("\\\"", "\""));
            rawStart = qend + 1;
            qstart = nextBareQuoteIndex(value,  qend + 1);
        } while (qstart != -1);
        values.addAll(VALUE_SPLITTER.split(value.substring(rawStart)));
        return values.build();
    }

    // Returns the index of the next '"' character that's not preceded by a '\'.
    private static int nextBareQuoteIndex(String s, int i) {
        i = s.indexOf('"', i);
        // If i == 0, then '"' is the first char and must be "bare".
        if (i > 0) {
            do {
                if (s.charAt(i - 1) != '\\') {
                    break;
                }
                i = s.indexOf('\\', i + 1);
            } while (i >= 0);
        }
        return i;
    }
}
