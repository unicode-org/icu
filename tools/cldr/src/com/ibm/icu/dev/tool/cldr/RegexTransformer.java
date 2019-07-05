package com.ibm.icu.dev.tool.cldr;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.escape.CharEscaperBuilder;
import com.google.common.escape.Escaper;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.function.Function.identity;

/**
 * Path/value transformer configured by {@code ldml2icu_xxx.txt} mapping and configuration files.
 * See {@code ldml2icu_readme.txt} for details on the configuration file format and
 * {@link PathValueTransformer} for the public API description and usage.
 *
 * <p>This class is thread safe.
 */
// TODO(dbeaumont): Rewrite the readme to match current behaviour and describe edge cases properly.
// TODO(dbeaumont): Consider removing PathValueTransformer when the dust settles.
final class RegexTransformer extends PathValueTransformer {
    private static final Pattern SORT_INDEX = Pattern.compile("(/\\w+)#[0-9]+");

    // Note that the full path we get here contains the "sort index" suffix for ORDERED
    // elements. This means that some element names are "foo#N" where N is the sort index.
    // Since the regex transformer works around "ordered elements" in a completely different
    // way, we can just remove the sort indices.
    private static String getFullXPathWithoutSortIndices(CldrValue v) {
        return SORT_INDEX.matcher(v.getFullPath()).replaceAll("$1");
    }

    /**
     * Returns a new transformer based on transformation rules defined in the given configuration
     * file and using the specified functions for resolving ICU values.
     *
     * @throws IOException if the configuration file could not be read.
     */
    public static PathValueTransformer fromConfig(Path configFile, NamedFunction... functions)
        throws IOException {
        return fromConfig(Files.readAllLines(configFile, StandardCharsets.UTF_8), functions);
    }

    /**
     * Returns a new transformer based on transformation rules defined in the given configuration,
     * and using the specified functions for resolving ICU values.
     */
    public static PathValueTransformer fromConfig(List<String> lines, NamedFunction... functions) {
        return new RegexTransformer(RuleParser.parseConfig(lines, Arrays.asList(functions)));
    }

    @Override
    public ImmutableList<Result> transform(CldrValue v) {
        String fullXPath = getFullXPathWithoutSortIndices(v);
        Map<String, Result> results = new LinkedHashMap<>();
        for (Rule rule : rules) {
            rule.match(v.getPath(), v, fullXPath).forEach(r -> results.putIfAbsent(r.getKey(), r));
        }
        return ImmutableList.copyOf(results.values());
    }

    @Override
    public ImmutableList<Result> getFallbackResultsFor(String key) {
        return fallbackFunctions.stream()
            .map(f -> f.apply(key))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toImmutableList());
    }

    // Transformation rules loading from the configuration file.
    private final ImmutableList<Rule> rules;
    // Functions which can generate a fallback value from a given resource bundle path.
    private final ImmutableList<Function<String, Optional<Result>>> fallbackFunctions;

    private RegexTransformer(List<Rule> rules) {
        this.rules = ImmutableList.copyOf(rules);
        this.fallbackFunctions =
            rules.stream().flatMap(Rule::getFallbackFunctions).collect(toImmutableList());
    }

    /*
     * Each rule corresponds to a single target xpath specification in the configuration file
     * (lines starting //) but may have more than one result specification. For example:
     *
     * //supplementalData/languageData/language[@type="(%W)"][@scripts="(%W)"][@territories="(%W)"]
     *      ; /languageData/$1/primary/scripts ; values=$2
     *      ; /languageData/$1/primary/territories; values=$3
     *
     * is represented by a single rule with two result specifications.
     */
    private static final class Rule {
        // The processed xpath specification yielding an xpath matching regular expression. This is
        // only suitable for matching incoming xpaths and cannot be processed in any other way.
        private final Pattern xpathPattern;
        private final ImmutableList<ResultSpec> resultSpecs;

        Rule(String xpathRegex, List<ResultSpec> resultSpecs) {
            this.xpathPattern = Pattern.compile(xpathRegex);
            this.resultSpecs = ImmutableList.copyOf(resultSpecs);
        }

        /**
         * Attempts to match the incoming xpath and (if successful) use captured arguments to
         * generate one result for each result specification.
         */
        Stream<Result> match(CldrPath p, CldrValue v, String fullXPath) {
            Matcher m = xpathPattern.matcher(fullXPath);
            return m.matches()
                ? resultSpecs.stream().flatMap(r -> r.transform(p, v, m)) : Stream.of();
        }

        /**
         * Returns any fallback functions defined in results specifications. These are used to
         * determine the set of possible fallback values for a given resource bundle path.
         */
        Stream<Function<String, Optional<Result>>> getFallbackFunctions() {
            return resultSpecs.stream()
                .map(ResultSpec::getFallbackFunction)
                .filter(Optional::isPresent)
                .map(Optional::get);
        }
    }

    /** Instructions in result specifications (e.g. "values=..." or "fallback=..."). */
    private enum Instruction {
        /** Defines processing and transformation of CLDR values. */
        VALUES,
        /** Defines fallback values to be used if no result was matched in a resource bundle. */
        FALLBACK,
        /** Defines an xpath used to hack result equality to make deduplication work. */
        BASE_XPATH,
        /**
         * Defines whether result values should be appended one at a time to a resource bundle
         * (default) or grouped into a separate array. The "group=" value is currently ignored.
         */
        GROUP;

        /** Returns the instruction enum for its ID as it appears in the configuration file. */
        static Instruction forId(String id) {
            return Instruction.valueOf(Ascii.toUpperCase(id));
        }
    }

    /**
     * A specification for building a result from the arguments in a matched xpath. Results always
     * hold a reference to their originating specification to allow them to be ordered in the same
     * order as the corresponding specifications in the configuration file.
     */
    private static final class ResultSpec {
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
        // Ordering is:
        // 1: Result key (resource bundle):     Groups results by resource bundle.
        // 2: Result specification line number: Orders resource bundle contents by "file order".
        // 3: Result distinguishing xpath:      Tie breaking if duplicates are not yet removed.
        private static final Comparator<ResultSpec.AbstractResult> RESULT_ORDERING =
            comparing(ResultSpec.AbstractResult::getKey)
                .thenComparing(r -> r.getSpec().lineNumber)
                .thenComparing(nullsLast(comparing(r -> r.getPath().orElse(null))));

        // Splitter for any values (either in CLDR data or results specifications). The only time
        // values are split differently is when quoting exists in the "values" instruction.
        private static final Splitter VALUE_SPLITTER = Splitter.on(whitespace()).omitEmptyStrings();

        // Matcher for "&foo(a,b,c)" which captures function name and complete argument list.
        private static final Pattern FUNCTION = Pattern.compile("\\&(\\w++)\\(([^\\)]++)\\)");

        // Resource bundle path specification with placeholders (e.g. "/foo/$1/bar") exactly as it
        // appears in the configuration file.
        private final String rbPathSpec;

        // Declared instructions with which to generate result values (see Instruction).
        private final ImmutableMap<Instruction, String> instructions;

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

        // A copy of the named functions available to the parser. Ideally the rules and result
        // specifications would be an inner class of some kind of context/environment instance and
        // just share stuff like this that way, but since this is currently the only example of
        // this sort of shared resource, and all result specifications just share the same
        // instance, it seems less trouble to just have it as a field.
        private final ImmutableMap<String, NamedFunction> icuFunctions;

        ResultSpec(
            String rbPathSpec, Map<Instruction, String> instructions,
            int lineNumber, ImmutableMap<String, NamedFunction> icuFunctions) {
            this.rbPathSpec = checkNotNull(rbPathSpec);
            this.instructions = ImmutableMap.copyOf(instructions);
            this.splitArgIndex = getSplitArgIndex(rbPathSpec);
            this.lineNumber = lineNumber;
            this.icuFunctions = checkNotNull(icuFunctions);
        }

        /**
         * Transforms a path/value into a sequence of results. The given matcher has successfully
         * matched the xpath and contains the captured arguments corresponding to $1..$N in the
         * various result specification strings.
         */
        Stream<Result> transform(CldrPath path, CldrValue value, Matcher m) {
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
            // by the resource bundle path specification, not the xpath regular expression, it
            // could differ per ResultSpec instance (but currently never does).
            if (splitArgIndex != -1) {
                List<String> splitArgs = VALUE_SPLITTER.splitToList(args.get(splitArgIndex));
                // Only bother if there was more than one argument there anyway.
                if (splitArgs.size() > 1) {
                    return splitArgs.stream().map(a -> {
                        args.set(splitArgIndex, a);
                        return matchedResult(path, value.getValue(), args);
                    });
                }
            }
            // No splittable argument, or a splittable argument with only one value.
            return Stream.of(matchedResult(path, value.getValue(), args));
        }

        // Simple helper to make results.
        private Result matchedResult(CldrPath path, String value, List<String> args) {
            return new MatchedResult(getRbPath(args), getValues(value, args), getXPath(path, args));
        }

        // Resource bundle paths are a bit special (unsurprisingly). The captured arguments can
        // contain '/' and will extend the path structure. Thus "foo/"$1"/bar" might end up as
        // "foo/x/y/bar" after argument substitution. However if the argument placeholder is quoted
        // (e.g. "foo/"$1"/bar") then / in arguments is replaced by ':' and quotes are retained,
        // (e.g. "foo/"x:y"/bar).
        private String getRbPath(List<String> args) {
            // Without more careful parsing, it's hard to figure out it quotes in a resource bundle
            // path specification are around a placeholder or not. Since quotes are only used in a
            // small number of cases currently, and only for this purpose, we just assume that any
            // quotes in the path specification should trigger this behaviour.
            if (rbPathSpec.contains("\"")) {
                // Use a lazy transforming list to avoid char replacement in arguments that don't
                // appear in the resource bundle path.
                args = Lists.transform(args, s -> s.replace('/', ':'));
            }
            return substituteArgs(rbPathSpec, args);
        }

        // Create an array of output values according to the CLDR value (if present) and the
        // "values" instruction in the result specification (if present). Any functions present in
        // the "values" instruction are invoked here.
        private ImmutableList<String> getValues(String value, List<String> args) {
            String valuesSpec = instructions.get(Instruction.VALUES);
            if (valuesSpec == null) {
                // No "values" instruction, so just use the _unsplit_ CLDR value. To split a CLDR
                // value use "values={value}" in the result specification.
                return ImmutableList.of(value);
            }
            // First substitute the $N arguments in since they need to be passed to the
            // functions.
            //
            // WARNING: This doesn't strictly work, since an argument or function result could
            // (in theory) contain the string "{value}" which would then be substituted in an
            // unexpected way. The better way to do this is with a single pass which handles
            // arguments, function calling and the special "{value}" token together. This comes
            // down to the fact that the mapping file syntax doesn't have a well defined concept
            // of escaping.
            String resolved = substituteArgs(valuesSpec, args);
            Matcher m = FUNCTION.matcher(resolved);
            if (m.find()) {
                StringBuilder buffer = new StringBuilder();
                int index = 0;
                do {
                    buffer.append(resolved, index, m.start());
                    buffer.append(icuFunctions.get(m.group(1)).call(m.group(2)));
                    index = m.end();
                } while (m.find());
                resolved = buffer.append(resolved.substring(index)).toString();
            }
            // Having done function invocation, we handle the special "{value}" token and split
            // the value (taking quoting into account).
            return splitValues(resolved.replace("{value}", value));
        }

        // IMPORTANT: The xpath of a result is either:
        // * The original distinguishing xpath
        // * The specified "base_xpath" (which must also be a distinguishing xpath).
        // and this is used as part of the equality semantics (which are very subtle).
        //
        // The existence of "base_xpath" is a hack to get around the fact the xpaths can only be
        // matched in full, rather than by a prefix. For some cases this means that the "same"
        // result will be created many times by potentially different distinguishing xpaths,
        // perhaps even via different result specifications. "base_xpath" exists as a hack to give
        // these duplicate results the same "fake" xpath, so deduplication can occur.
        private CldrPath getXPath(CldrPath path, List<String> args) {
            String basePath = instructions.get(Instruction.BASE_XPATH);
            if (basePath == null) {
                return path;
            }
            return CldrPath.parseDistinguishingPath(substituteArgs(basePath, args));
        }

        /**
         * Returns a fallback function if this specification has the "fallback=" instruction.
         * The function takes a resolved resource bundle path and returns the possible fallback
         * values for it. Note that currently fallback values do not support either quoting or
         * grouping (but they easily could).
         */
        Optional<Function<String, Optional<Result>>> getFallbackFunction() {
            String fallbackValues = instructions.get(Instruction.FALLBACK);
            if (fallbackValues == null) {
                return Optional.empty();
            }
            // This is the only place where any hacking of regular expressions occurs. The fallback
            // function must only return a value if the given resolved resource bundle path could
            // have been a match for the path specification. Note that this is definitely a
            // heuristic, since "foo/$1/$2/bar" and "foo/$1/bar" will both produce matchers that
            // match "foo/x/y/bar", because '/' is explicitly permitted in argument values.
            Pattern rbPathMatcher = getRbPathMatcher(rbPathSpec);
            // Split value without considering quoting (call "splitvalues()" to handle quoting).
            ImmutableList<String> values =
                ImmutableList.copyOf(VALUE_SPLITTER.split(fallbackValues));
            return Optional.of(
                p -> rbPathMatcher.matcher(p).matches()
                    ? Optional.of(new FallbackResult(p, values)) : Optional.empty());
        }

        // Base class of either a matched or a fallback result.
        private abstract class AbstractResult extends Result {
            // Split and resolved values for this result (see also "isGrouped()").
            private final ImmutableList<String> values;

            // The source xpath of a matched result (omitted if this is a fallback result). Note
            // that this is the resolved "base_xpath" if one was specified in the instructions.
            private final Optional<CldrPath> sourceXPath;

            // Calculated eagerly since we always expect results to need to be deduplicated.
            private final int hashCode;

            AbstractResult(String key, Iterable<String> values, Optional<CldrPath> path) {
                super(key);
                this.values = ImmutableList.copyOf(values);
                this.sourceXPath = checkNotNull(path);
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
                return sourceXPath;
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
            // deduplication of results. See also "getSpec()", "getXPath()", and RESULT_ORDERING.
            @Override
            public final boolean equals(Object obj) {
                // Different subclasses are never equal, so test class directly (not instanceof).
                if (obj == null || !getClass().equals(obj.getClass())) {
                    return false;
                }
                AbstractResult other = (AbstractResult) obj;
                // DO NOT test the result specifier here. Equal results can be generated from
                // different result specifications (though in those cases we expect a base xpath
                // to have been used).
                return getKey().equals(other.getKey())
                    && getPath().equals(getPath())
                    && isGrouped() == other.isGrouped()
                    // Alternatively assert that values are equal if everything else is.
                    && getValues().equals(other.getValues());
            }
        }

        // Result created for an explicit match of an xpath using captured arguments.
        private final class MatchedResult extends AbstractResult {
            MatchedResult(String key, Iterable<String> values, CldrPath path) {
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

        // Result created to hold possible fallback values of a specified resource bundle path.
        // This result will be used only if it is NOT a fallback for any existing matched result.
        private final class FallbackResult extends AbstractResult {
            FallbackResult(String key, Iterable<String> values) {
                super(key, values, Optional.empty());
            }

            // Delete this method and move the other one into AbstractResult if we decide to allow
            // grouping for fallback values.
            @Override
            public boolean isGrouped() {
                return false;
            }

            @Override
            public boolean isFallbackFor(Result r) {
                // We are a fallback if we came from the same specification as a matched result.
                // We also match the same fallback if it was already added to prevent multiple
                // additions of the same fallback.
                return
                    (r instanceof MatchedResult) && getSpec().equals(((MatchedResult) r).getSpec())
                        || equals(r);
            }
        }

        // ==== Static helper functions ====

        // Matches any "$N" placeholder.
        private static final Pattern ARG_PLACEHOLDER = Pattern.compile("\\$[1-9]");

        // Turn "$N" into element capture. Note that since arguments can contain '/' we can get a
        // situation where "foo/$1/bar" expands to "foo/x/y/bar", and we must create a matcher that
        // recognizes this case.
        private static Pattern getRbPathMatcher(String rbPathSpec) {
            // Protect potential regex meta-characters in the original resource bundle path. Note
            // that because '$' is also a meta-character we cannot use Pattern.quote() to quote the
            // string since it could produce unwanted "\$". Using \Q and \E to mark quotation
            // boundaries is the safest way to do this, but that means we also need to handle \E in
            // the original string (incredibly unlikely but it would be super hard to debug if it
            // ever happened).
            String regex = "\\Q" + rbPathSpec.replace("\\E", "\\E\\E\\Q") + "\\E";

            // Remember that you could get "$1$2" here and the regex groups that replace them will
            // abut. Use reluctant matching (i.e. ".+?") to avoid any backtracking in this case.
            // We assume that the substituted arguments contained at least one character, and so we
            // capture at least one character per group here.
            regex = ARG_PLACEHOLDER.matcher(regex).replaceAll("\\\\E(?:.+?)\\\\Q");
            return Pattern.compile(regex);
        }

        private static String substituteArgs(String spec, List<String> args) {
            return substitute(spec, '$',
                c -> args.get(checkElementIndex(c - '1', args.size(), "argument index")));
        }

        // Logic mostly copied from original RegexManager class. Finds first unquoted $N (N=1..9)
        // and returns N-1 (or -1 if no match). We do not permit $0 to appear even though it is
        // captured by the regex because it's just the entire xpath.
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

    private static final class RuleParser {
        // Preprocessing replaces %X variables defined in the configuration file. This helps to
        // keep the xpath specification a bit easier to read.
        private static final Pattern VAR = Pattern.compile("^%([A-Z])=(.*)$");
        private static final CharMatcher VAR_CHAR = CharMatcher.inRange('A', 'Z');

        // Multi-line rules must start with "   ; " for some optional amount of leading/trailing
        // whitespace.
        private static final Pattern RULE_PARTS_SEPERATOR = Pattern.compile("\\s*+;\\s*+");

        // Splitter for the resource bundle / value declarations.
        private static final Splitter RULE_PARTS_SPLITTER =
            Splitter.on(RULE_PARTS_SEPERATOR).trimResults(whitespace()).omitEmptyStrings();

        // Splitter for instruction name/contents.
        private static final Splitter INSTRUCTION_SPLITTER =
            Splitter.on('=').trimResults(whitespace()).limit(2);

        // Note that of _all_ regex meta-characters, only [,] are literals in the xpath spec.
        private static final Escaper SPECIAL_CHARS_ESCAPER =
            new CharEscaperBuilder().addEscape('[', "\\[").addEscape(']', "\\]").toEscaper();

        /** Parses a configuration file to create a sequence of transformation rules. */
        static ImmutableList<Rule> parseConfig(
            List<String> configLines, List<NamedFunction> functions) {
            // Extract %X variable declarations in the first pass.
            ImmutableMap<Character, String> varMap = configLines.stream()
                .filter(s -> s.startsWith("%"))
                .map(VAR::matcher)
                .peek(m -> checkArgument(m.matches(), "invalid argument declaration: %s", m))
                .collect(ImmutableMap.toImmutableMap(m -> m.group(1).charAt(0), m -> m.group(2)));
            return new RuleParser(varMap, functions).parseLines(configLines);
        }

        private final ImmutableMap<Character, String> varMap;
        private final ImmutableMap<String, NamedFunction> fnMap;

        private RuleParser(ImmutableMap<Character, String> varMap, List<NamedFunction> functions) {
            this.varMap = checkNotNull(varMap);
            this.fnMap =
                functions.stream().collect(toImmutableMap(NamedFunction::getName, identity()));
        }

        private ImmutableList<Rule> parseLines(List<String> configLines) {
            List<Rule> rules = new ArrayList<>();
            for (int lineIndex = 0; lineIndex < configLines.size(); lineIndex++) {
                String line = configLines.get(lineIndex);
                try {
                    if (line.startsWith("//")) {
                        // Either it's "//xpath ; resource-bundle-path ; values"
                        // Or "//xpath" with " ; resource-bundle-path ; values" on subsequent lines.
                        int xpathEnd = line.indexOf(";");
                        String xpath;
                        List<ResultSpec> specs = new ArrayList<>();
                        if (xpathEnd != -1) {
                            // Single line rule, extract result specification from trailing part.
                            xpath = whitespace().trimFrom(line.substring(0, xpathEnd));
                            // Keep leading " ; " in the transformation string since it matches the
                            // multi-rule case and is handled the same.
                            specs.add(parseResultSpec(line.substring(xpathEnd), lineIndex + 1));
                        } else {
                            xpath = line;
                            while (++lineIndex < configLines.size()
                                && RULE_PARTS_SEPERATOR
                                .matcher(configLines.get(lineIndex)).lookingAt()) {
                                specs.add(parseResultSpec(configLines.get(lineIndex), lineIndex + 1));
                            }
                            // The loop above moved us past the last line of the rule, so readjust.
                            lineIndex--;
                        }
                        rules.add(parseRule(xpath, specs));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(
                        String.format("Parse error at line %d: %s", lineIndex + 1, line), e);
                }
            }
            return ImmutableList.copyOf(rules);
        }

        private ResultSpec parseResultSpec(String spec, int lineNumber) {
            // The result specifier still has leading separator (e.g. "  ; /foo/bar/$1 ; value=$2"),
            // but that's okay because the splitter ignores empty results.
            List<String> rbPathAndInstructions = RULE_PARTS_SPLITTER.splitToList(expandVars(spec));
            String rbPathSpec = expandVars(rbPathAndInstructions.get(0));

            ImmutableMap<Instruction, String> instructions =
                rbPathAndInstructions.stream()
                    .skip(1)
                    .map(INSTRUCTION_SPLITTER::splitToList)
                    .collect(toImmutableMap(
                        p -> Instruction.forId(p.get(0)),
                        p -> expandVars(p.get(1))));
            return new ResultSpec(rbPathSpec, instructions, lineNumber, fnMap);
        }

        private Rule parseRule(String xpathSpec, List<ResultSpec> resultSpecs) {
            return new Rule(xpathSpecToRegex(xpathSpec), resultSpecs);
        }

        private String xpathSpecToRegex(String s) {
            // Escape a subset of the possible metacharacters, since variables will contain things
            // like [,]. Things like (,),? are not escaped however since they never appear in the
            // xpath patterns as literals.
            return expandVars(SPECIAL_CHARS_ESCAPER.escape(s));
        }

        private String expandVars(String s) {
            // We must treat % as "bare" if it appears and is not followed by a letter (e.g. "%$").
            return substitute(s, '%', c -> VAR_CHAR.matches(c) ? varMap.get(c) : "%" + c);
        }
    }

    // Matches arguments with or without enclosing quotes.
    private static final Pattern ARGUMENT = Pattern.compile("[<\"]?\\$(\\d)[\">]?");

    // Generic helper for substituting single-character place-holders like $N or %X.
    private static String substitute(String s, char token, Function<Character, String> replaceFn) {
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
