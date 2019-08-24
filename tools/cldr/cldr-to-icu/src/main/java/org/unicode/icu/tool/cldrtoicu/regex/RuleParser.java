// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.regex;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.transformValues;
import static java.util.function.Function.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.escape.CharEscaperBuilder;
import com.google.common.escape.Escaper;

/** Parser for rule specifications in the regex transformer configuration files. */
final class RuleParser {
    // Pattern to capture first two path elements (for the dtd type and path prefix).
    private static final Pattern PATH_SPEC_PREFIX = Pattern.compile("//([^/]+)/([^/]+)/");

    // Preprocessing replaces %X variables defined in the configuration file. This helps to
    // keep the path specification a bit easier to read.
    private static final Pattern VAR = Pattern.compile("^%([A-Z])=(.*)$");

    // Multi-line rules start with " ; " for some optional amount of whitespace.
    private static final Pattern RULE_PARTS_SEPERATOR = Pattern.compile("\\s*+;\\s*+");

    // Splitter for the resource bundle / value declarations.
    private static final Splitter RULE_PARTS_SPLITTER =
        Splitter.on(RULE_PARTS_SEPERATOR).trimResults(whitespace()).omitEmptyStrings();

    // Splitter for instruction name/expressions.
    private static final Splitter INSTRUCTION_SPLITTER =
        Splitter.on('=').trimResults(whitespace()).limit(2);

    // Only '[',']' need escaping in path specifications (so we can write "foo{@bar="baz"]").
    private static final Escaper SPECIAL_CHARS_ESCAPER =
        new CharEscaperBuilder().addEscape('[', "\\[").addEscape(']', "\\]").toEscaper();

    /** Parses a configuration file to create a sequence of transformation rules. */
    static ImmutableList<Rule> parseConfig(
        List<String> configLines, List<NamedFunction> functions) {
        // Extract '%X' variable declarations in the first pass.
        ImmutableMap<Character, String> varMap = configLines.stream()
            .filter(s -> s.startsWith("%"))
            .map(VAR::matcher)
            .peek(m -> checkArgument(m.matches(), "invalid argument declaration: %s", m))
            .collect(ImmutableMap.toImmutableMap(m -> m.group(1).charAt(0), m -> m.group(2)));
        return new RuleParser(varMap, functions).parseLines(configLines);
    }

    private final ImmutableMap<Character, String> staticVarMap;
    private final ImmutableMap<Character, CldrPath> dynamicVarMap;
    private final ImmutableMap<String, NamedFunction> fnMap;

    private RuleParser(ImmutableMap<Character, String> varMap, List<NamedFunction> functions) {
        this.staticVarMap = ImmutableMap.copyOf(filterValues(varMap, s -> !s.startsWith("//")));
        this.dynamicVarMap = ImmutableMap.copyOf(
            transformValues(
                filterValues(varMap, s -> s.startsWith("//")),
                CldrPath::parseDistinguishingPath));
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
                    int ruleLineNumber = lineIndex + 1;
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
                            && RULE_PARTS_SEPERATOR.matcher(configLines.get(lineIndex)).lookingAt()) {
                            specs.add(parseResultSpec(configLines.get(lineIndex), lineIndex + 1));
                        }
                        // The loop above moved us past the last line of the rule, so readjust.
                        lineIndex--;
                    }
                    rules.add(parseRule(xpath, specs, ruleLineNumber));
                }
            } catch (Exception e) {
                throw new RuntimeException(
                    String.format("parse error at line %d: %s", lineIndex + 1, line), e);
            }
        }
        return ImmutableList.copyOf(rules);
    }

    private ResultSpec parseResultSpec(String spec, int lineNumber) {
        // The result specifier still has leading separator (e.g. " ; /foo/bar/$1 ; value=$2"),
        // but that's okay because the splitter ignores empty results.
        List<String> rbPathAndInstructions = RULE_PARTS_SPLITTER.splitToList(spec);
        String rbPathSpec = rbPathAndInstructions.get(0);

        ImmutableMap<Instruction, VarString> instructions =
            rbPathAndInstructions.stream()
                .skip(1)
                .map(INSTRUCTION_SPLITTER::splitToList)
                .collect(toImmutableMap(
                    p -> Instruction.forId(p.get(0)),
                    p -> VarString.of(p.size() > 1 ? p.get(1) : "", staticVarMap::get)));
        return new ResultSpec(rbPathSpec, instructions, lineNumber, fnMap, dynamicVarMap::get);
    }

    private Rule parseRule(String xpathSpec, List<ResultSpec> resultSpecs, int lineNumber) {
        // The escaped path is nearly a regular expression, but still contains '%X' variables.
        String escapedPathSpec = SPECIAL_CHARS_ESCAPER.escape(xpathSpec);
        Matcher m = PATH_SPEC_PREFIX.matcher(escapedPathSpec);
        checkArgument(m.lookingAt(), "unexpected path spec: %s", escapedPathSpec);

        // Extract type a path prefix for rule grouping and fast rejection during matching.
        CldrDataType dtdType = CldrDataType.forXmlName(m.group(1));
        String pathPrefix = m.group(2);

        // If the variable string contains a "dynamic" argument, is cannot be resolved yet and
        // must result in a "dynamic" rule being created here (this is very rare though).
        VarString varString = VarString.of(escapedPathSpec, staticVarMap::get);
        Optional<String> resolved = varString.resolve();
        // Don't turn this into a "map().orElse()" chain (despite what your IDE might suggest)
        // because we don't want to create lots of unused dynamic rules!
        return resolved.isPresent()
            ? Rule.staticRule(
                dtdType, pathPrefix, resultSpecs, resolved.get(), xpathSpec, lineNumber)
            : Rule.dynamicRule(
                dtdType, pathPrefix, resultSpecs, varString, dynamicVarMap::get, xpathSpec, lineNumber);
    }
}
