// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

/**
 * Helper tool to dump the resource bundle paths and values from an IcuData instance in a stable
 * ordering, to allow easy comparison in cases where ICU ordering changes. This could easily be
 * extended to be a more fully featured "diff" tool or a proper ICU data file parser.
 *
 * <p>This is a temporary debugging tool and should not be relied upon during any part of the data
 * generation process.
 */
final class IcuDataDumper {
    private static final Joiner LIST_JOINER = Joiner.on(',');
    private static final RbPath VERSION = RbPath.of("Version");

    public static void main(String... args) throws IOException {
        Path fileOrDir;
        Optional<Pattern> name = Optional.empty();
        switch (args.length) {
        case 2:
            name = Optional.of(Pattern.compile(args[1]));
        case 1:
            fileOrDir = Paths.get(args[0]);
            break;
        default:
            throw new IllegalArgumentException("Usage: <file-or-dir> [<name-pattern>]");
        }

        if (Files.isDirectory(fileOrDir)) {
            walkDirectory(fileOrDir, name);
        } else {
            checkArgument(!name.isPresent(),
                "cannot specificy a name pattern for a non-directory file: %s", fileOrDir);
            IcuDataParser parser = new IcuDataParser(fileOrDir);
            parser.parse();
            dump(parser.icuData);
        }
    }

    private static void walkDirectory(Path fileOrDir, Optional<Pattern> name) throws IOException {
        Predicate<Path> matchesName =
            f -> name.map(n -> n.matcher(f.getFileName().toString()).matches()).orElse(true);
        List<IcuDataParser> icuParsers;
        try (Stream<Path> files = Files.walk(fileOrDir)) {
            icuParsers = files
                .filter(Files::isRegularFile)
                .filter(matchesName)
                .map(IcuDataParser::new)
                .collect(toImmutableList());
        }
        ListMultimap<RbPath, RbValue> allPaths = ArrayListMultimap.create();
        for (IcuDataParser p : icuParsers) {
            p.parse();
            for (RbPath k : p.icuData.keySet()) {
                List<RbValue> values = p.icuData.get(k);
                if (!allPaths.containsKey(k)) {
                    allPaths.putAll(k, values);
                } else if (!VERSION.equals(k)) {
                    checkState(allPaths.get(k).equals(values), "inconsistent data for path: ", k);
                }
            }
        }
        dump(allPaths);
    }

    private static void dump(ListMultimap<RbPath, RbValue> allPaths) {
        allPaths.keySet().stream()
            .sorted()
            .forEach(k -> System.out.println(k + " :: " + LIST_JOINER.join(allPaths.get(k))));
    }

    private static final class IcuDataParser {
        // Path of file being parsed.
        private final Path path;

        // Comments in header (before data starts), without comment characters.
        private final List<String> headerComment = new ArrayList<>();
        // ICU data name (the name of the root element).
        private String name = null;
        // ICU data values.
        private final ListMultimap<RbPath, RbValue> icuData = ArrayListMultimap.create();

        // Current line number (1-indexed).
        private int lineNumber = 0;
        // The type of the previous line that was processed.
        private LineType lastType = LineType.COMMENT;
        // True when inside /* .. */ comments in the header.
        private boolean inBlockComment = false;
        // True when in the final top-level group at the end of parsing.
        private boolean inFinalGroup = false;
        // True when a partial (line wrapped) value has been read.
        private boolean isLineContinuation = false;
        // Current path while parsing (NOT including the root element).
        private Deque<String> pathStack = new ArrayDeque<>();
        // Current sequence of values for the path (as defined in the current path stack).
        private List<String> currentValue = new ArrayList<>();
        // Current partially read value of a multi-line value.
        private String wrappedValue = "";
        // Map of indices used to auto-generate names for anonymous path segments.
        // TODO: Check if this is even needed and remove if not.
        private Multiset<Integer> indices = HashMultiset.create();

        IcuDataParser(Path path) {
            this.path = checkNotNull(path);
        }

        public boolean parse() throws IOException {
            List<String> lines = Files.readAllLines(path);
            // Best approximation to a magic number be have (BOM plus inline comment). This stops
            // use trying to parse the transliteration files, which are a different type.
            if (!lines.get(0).startsWith("\uFEFF//")) {
                return false;
            }
            lines.stream().map(whitespace()::trimFrom).forEach(this::processLineWithCheck);

            // Sanity check for expected final state. Just checking the "lastType" should be enough
            // to catch everything else (due to transition rules and how the code tidies up) but it
            // seems prudent to sanity check everything just in case.
            checkState(lastType == LineType.GROUP_END);
            checkState(!inBlockComment);
            checkState(name != null);
            checkState(pathStack.isEmpty() && inFinalGroup);
            checkState(wrappedValue.isEmpty() && currentValue.isEmpty());
            return true;
        }

        void processLineWithCheck(String line) {
            lineNumber++;
            if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            try {
                processLine(line);
            } catch (RuntimeException e) {
                throw new RuntimeException(
                    String.format("[%s:%s] %s (%s)", path, lineNumber, e.getMessage(), line),
                    e);
            }
        }

        void processLine(String line) {
            line = maybeTrimEndOfLineComment(line);
            if (line.isEmpty()) {
                return;
            }
            LineMatch match = LineType.match(line, inBlockComment);
            checkState(match.getType().isValidTransitionFrom(lastType),
                "invalid state transition: %s --//-> %s", lastType, match.getType());
            boolean isEndOfWrappedValue = false;
            switch (match.getType()) {
            case COMMENT:
                if (name != null) {
                    // Comments in data are ignored since they cannot be properly associated with
                    // paths or values in an IcuData instance (only legacy tooling emits these).
                    break;
                }
                if (line.startsWith("/*")) {
                    inBlockComment = true;
                }
                headerComment.add(match.get(0));
                if (inBlockComment && line.contains("*/")) {
                    checkState(line.indexOf("*/") == line.length() - 2,
                        "unexpected end of comment block");
                    inBlockComment = false;
                }
                break;

            case INLINE_VALUE:
                icuData.put(
                    getPathFromStack().extendBy(getSegment(match.get(0))),
                    RbValue.of(unquote(match.get(1))));
                break;

            case GROUP_START:
                checkState(currentValue.isEmpty());
                if (name == null) {
                    name = match.get(0);
                    checkState(name != null, "cannot have anonymous top-level group");
                } else {
                    pathStack.push(getSegment(match.get(0)));
                }
                wrappedValue = "";
                isLineContinuation = false;
                break;

            case QUOTED_VALUE:
                wrappedValue += unquote(match.get(0));
                isLineContinuation = !line.endsWith(",");
                if (!isLineContinuation) {
                    currentValue.add(wrappedValue);
                    wrappedValue = "";
                }
                break;

            case VALUE:
                checkState(!isLineContinuation, "unexpected unquoted value");
                currentValue.add(match.get(0));
                break;

            case GROUP_END:
                // Account for quoted values without trailing ',' just before group end.
                if (isLineContinuation) {
                    currentValue.add(wrappedValue);
                    isLineContinuation = false;
                }
                // Emit the collection sequence of values for the current path as an RbValue.
                if (!currentValue.isEmpty()) {
                    icuData.put(getPathFromStack(), RbValue.of(currentValue));
                    currentValue.clear();
                }
                // Annoyingly the name is outside the stack so the stack will empty before the last
                // end group.
                if (!pathStack.isEmpty()) {
                    pathStack.pop();
                    indices.setCount(pathStack.size(), 0);
                } else {
                    checkState(!inFinalGroup, "unexpected group end");
                    inFinalGroup = true;
                }
                break;

            case UNKNOWN:
                throw new IllegalStateException("cannot parse line: " + match.get(0));
            }
            lastType = match.getType();
        }

        private RbPath getPathFromStack() {
            if (pathStack.isEmpty()) {
                return RbPath.of();
            }
            List<String> segments = new ArrayList<>();
            Iterables.addAll(segments, pathStack);
            if (segments.get(0).matches("<[0-9]{4}>")) {
                segments.remove(0);
            }
            return RbPath.of(Lists.reverse(segments));
        }

        private String getSegment(String segmentOrNull) {
            if (segmentOrNull != null) {
                return segmentOrNull;
            }
            int depth = pathStack.size();
            int index = indices.count(depth);
            indices.add(depth, 1);
            return String.format("<%04d>", index);
        }

        private String maybeTrimEndOfLineComment(String line) {
            // Once the name is set, we are past the header and into the data.
            if (name != null) {
                // Index to search for '//' from - must skip quoted values.
                int startIdx = line.startsWith("\"") ? line.indexOf('"', 1) + 1 : 0;
                int commentIdx = line.indexOf("//", startIdx);
                if (commentIdx != -1) {
                    line = whitespace().trimTrailingFrom(line.substring(0, commentIdx));
                }
            }
            return line;
        }

        private static String unquote(String s) {
            if (s.startsWith("\"") && s.endsWith("\"")) {
                return s.substring(1, s.length() - 1).replaceAll("\\\\([\"\\\\])", "$1");
            }
            checkState(!s.contains("\""), "invalid unquoted value: %s", s);
            return s;
        }

        private static final class LineMatch {
            private final LineType type;
            private final Function<Integer, String> args;

            LineMatch(LineType type, Function<Integer, String> args) {
                this.type = checkNotNull(type);
                this.args = checkNotNull(args);
            }

            String get(int n) {
                return args.apply(n);
            }

            LineType getType() {
                return type;
            }
        }

        private enum LineType {
            // Comment _start_ with any comment value captured.
            COMMENT("(?://|/\\*)\\s*(.*)"),
            // A combination of GROUP_START, VALUE and GROUP_END with whitespace.
            INLINE_VALUE("(?:(.*\\S)\\s*)?\\{\\s*((?:\".*\")|(?:[^\"{}]*\\S))\\s*\\}"),
            // Allows for empty segment names (anonymous arrays) which match 'null'.
            GROUP_START("(?:(.*\\S)\\s*)?\\{"),
            GROUP_END("\\}"),
            QUOTED_VALUE("(\".*\"),?"),
            VALUE("([^\"{}]+),?"),
            UNKNOWN(".*");

            // Table of allowed transitions expected during parsing.
            // key=current state, values=set of permitted previous states
            private static ImmutableSetMultimap<LineType, LineType> TRANSITIONS =
                ImmutableSetMultimap.<LineType, LineType>builder()
                    .putAll(COMMENT, COMMENT)
                    .putAll(INLINE_VALUE, COMMENT, INLINE_VALUE, GROUP_START, GROUP_END)
                    .putAll(GROUP_START, COMMENT, GROUP_START, GROUP_END, INLINE_VALUE)
                    .putAll(VALUE, GROUP_START, VALUE, QUOTED_VALUE)
                    .putAll(QUOTED_VALUE, GROUP_START, VALUE, QUOTED_VALUE)
                    .putAll(GROUP_END, GROUP_END, INLINE_VALUE, VALUE, QUOTED_VALUE)
                    .build();

            private final Pattern pattern;

            LineType(String regex) {
                this.pattern = Pattern.compile(regex);
            }

            boolean isValidTransitionFrom(LineType lastType) {
                return TRANSITIONS.get(this).contains(lastType);
            }

            static LineMatch match(String line, boolean inBlockComment) {
                // Block comments kinda suck and it'd be great if the ICU data only used '//' style
                // comments (if would definitely simplify any parsers out there). Once the
                // transition to the new transformation tools is complete, they can be changed to
                // only emit '//' style comments.
                if (inBlockComment) {
                    if (line.startsWith("*")) {
                        line = whitespace().trimLeadingFrom(line.substring(1));
                    }
                    return new LineMatch(COMMENT, ImmutableList.of(line)::get);
                }
                for (LineType type : TRANSITIONS.keySet()) {
                    // Regex groups start at 1, but we want the getter function to be zero-indexed.
                    Matcher m = type.pattern.matcher(line);
                    if (m.matches()) {
                        return new LineMatch(type, n -> {
                            checkElementIndex(n, m.groupCount());
                            return m.group(n + 1);
                        });
                    }
                }
                return new LineMatch(UNKNOWN, ImmutableList.of(line)::get);
            }
        }
    }
}
