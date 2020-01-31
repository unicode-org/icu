// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.ant;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.partitioningBy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;

// Note: Auto-magical Ant methods are listed as "unused" by IDEs, unless the warning is suppressed.
public final class CleanOutputDirectoryTask extends Task {
    private static final ImmutableSet<String> ALLOWED_DIRECTORIES =
        Stream
            .concat(
                Stream.of("misc", "translit"),
                Arrays.stream(IcuLocaleDir.values()).map(IcuLocaleDir::getOutputDir))
            .sorted()
            .collect(toImmutableSet());

    private static final CharMatcher NOT_WHITESPACE = CharMatcher.whitespace().negate();

    private Path root = null;
    private boolean forceDelete = false;
    private final List<Dir> outputDirs = new ArrayList<>();
    private final ImmutableList<String> headerLines;

    public CleanOutputDirectoryTask() {
        // TODO: Consider passing in header lines via Ant?
        this.headerLines = readLinesFromResource("/ldml2icu_header.txt");
    }

    public static final class Retain extends Task {
        private Path path = null;

        // Don't use "Path" for the argument type because that always makes an absolute path (e.g.
        // relative to the working directory for the Ant task). We want relative paths.
        @SuppressWarnings("unused")
        public void setPath(String path) {
            Path p = Paths.get(path).normalize();
            checkBuild(!p.isAbsolute() && !p.startsWith(".."), "invalid path: %s", path);
            this.path = p;
        }

        @Override
        public void init() throws BuildException {
            checkBuild(path != null, "missing 'path' attribute");
        }
    }

    public static final class Dir extends Task {
        private String name;
        private final Set<Path> retained = new HashSet<>();

        @SuppressWarnings("unused")
        public void setName(String name) {
            checkBuild(ALLOWED_DIRECTORIES.contains(name),
                "unknown directory name '%s'; allowed values: %s", name, ALLOWED_DIRECTORIES);
            this.name = name;
        }

        @SuppressWarnings("unused")
        public void addConfiguredRetain(Retain retain) {
            retained.add(retain.path);
        }

        @Override
        public void init() throws BuildException {
            checkBuild(name != null, "missing 'name' attribute");
        }
    }

    @SuppressWarnings("unused")
    public void setRoot(Path root) {
        this.root = root;
    }

    @SuppressWarnings("unused")
    public void setForceDelete(boolean forceDelete) {
        this.forceDelete = forceDelete;
    }

    @SuppressWarnings("unused")
    public void addConfiguredDir(Dir dir) {
        outputDirs.add(dir);
    }

    @Override
    public void execute() throws BuildException {
        checkBuild(root != null, "missing 'root' attribute");
        checkBuild(!outputDirs.isEmpty(), "missing <dir> elements");

        if (!Files.exists(root)) {
            log("Root directory '" + root + "' does not exist (nothing to clean)");
            return;
        }
        checkBuild(Files.isDirectory(root), "specified root '%s' is not a directory", root);

        Set<Path> autogenFiles = new TreeSet<>();
        Set<Path> unknownFiles = new TreeSet<>();
        for (Dir dirInfo : outputDirs) {
            Path dirPath = root.resolve(dirInfo.name);
            if (!Files.exists(dirPath)) {
                continue;
            }
            checkBuild(Files.isDirectory(dirPath), "'%s' is not a directory", dirPath);

            // Note: For now we just walk the immediate contents of each output directory and don't
            // attempt to recursively process things. Only a couple of output directories have
            // sub-directories anyway, and we never write files into them anyway.
            try (Stream<Path> files = Files.list(dirPath)) {
                Map<Boolean, List<Path>> map = files
                    .filter(p -> couldDelete(p, dirPath, dirInfo))
                    .parallel()
                    .collect(partitioningBy(this::wasAutoGenerated));
                unknownFiles.addAll(map.get(false));
                autogenFiles.addAll(map.get(true));
            } catch (IOException e) {
                throw new BuildException("Error processing directory: " + dirPath, e);
            }
        }

        if (!unknownFiles.isEmpty() && !forceDelete) {
            // If there are NO safe files, then something weird is going on (perhaps a change in
            // the header file).
            if (autogenFiles.isEmpty()) {
                log("Error determining 'safe' files for deletion (no auto-generated files found).");
                log(unknownFiles.size() + " files would be deleted for 'clean' task");
                logPartioned(unknownFiles);
                log("Set '-DforceDelete=true' to delete all files not listed in"
                    + " <outputDirectories>.");
            } else {
                // A mix of safe and unsafe files is weird, but in this case it should be a
                // relatively small number of files (e.g. adding a new manually maintained file or
                // accidental editing of header lines).
                log("Unknown files exist which cannot be determined to be auto-generated");
                log("Files:");
                logPartioned(unknownFiles);
                log(String.format("%d unknown files or directories found", unknownFiles.size()));
                log("Set '-DforceDelete=true' to delete these files, or add them to"
                    + " <outputDirectories>.");
            }
            throw new BuildException("Unsafe files cannot be deleted");
        }
        if (!unknownFiles.isEmpty()) {
            checkState(forceDelete, "unexpected flag state (forceDelete should be true here)");
            List<Path> filesToDelete =
                unknownFiles.stream()
                    .filter(p -> !Files.isDirectory(p))
                    .collect(Collectors.toList());
            log(String.format("Force deleting %,d files...\n", filesToDelete.size()));
            deleteAllFiles(filesToDelete);

            List<Path> unknownDirs =
                unknownFiles.stream()
                    .filter(p -> Files.isDirectory(p))
                    .collect(Collectors.toList());
            if (!unknownDirs.isEmpty()) {
                log("Add the following directories to the <outputDirectories> task:");
                logPartioned(unknownDirs);
                throw new BuildException("Unsafe directories cannot be deleted");
            }
        }
        if (!autogenFiles.isEmpty()) {
            log(String.format("Deleting %,d auto-generated files...\n", autogenFiles.size()));
            deleteAllFiles(autogenFiles);
        }
    }

    private void logPartioned(Iterable<Path> files) {
        Iterables.partition(files, 5)
            .forEach(f -> log(
                f.stream().map(p -> root.relativize(p).toString()).collect(joining(", "))));
    }

    private boolean couldDelete(Path path, Path dir, Dir dirInfo) {
        return !dirInfo.retained.contains(dir.relativize(path));
    }

    private boolean wasAutoGenerated(Path path) {
        if (!Files.isRegularFile(path, NOFOLLOW_LINKS)) {
            // Directories, symbolic links, devices etc.
            return false;
        }
        try (BufferedReader r = Files.newBufferedReader(path)) {
            // A byte-order-mark (BOM) is added to ICU data files, but not JSON deps files, so just
            // treat it as optional everywhere (it's not the important thing we check here).
            r.mark(1);
            int maybeByteOrderMark = r.read();
            if (maybeByteOrderMark != '\uFEFF') {
                // Also reset if the file was empty, but that should be harmless.
                r.reset();
            }
            for (String headerLine : headerLines) {
                String line = r.readLine();
                if (line == null) {
                    return false;
                }
                int headerStart = skipComment(line);
                if (headerStart < 0
                    || !line.regionMatches(headerStart, headerLine, 0, headerLine.length())) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int skipComment(String line) {
        if (line.startsWith("#")) {
            return toCommentStart(line, 1);
        } else if (line.startsWith("//")) {
            return toCommentStart(line, 2);
        }
        return -1;
    }

    // Not just "index-of" since a comment start followed by only whitespace is NOT a failure to
    // find a comment (since the header might have an empty line in it, which should be okay).
    private static int toCommentStart(String line, int offset) {
        int index = NOT_WHITESPACE.indexIn(line, offset);
        return index >= 0 ? index : line.length();
    }

    private static void deleteAllFiles(Iterable<Path> files) {
        for (Path p : files) {
            try {
                // This is a code error, since only files should be passed here.
                checkArgument(!Files.isDirectory(p), "Cannot delete directories: %s", p);
                Files.deleteIfExists(p);
            } catch (IOException e) {
                throw new BuildException("Error deleting file: " + p, e);
            }
        }
    }

    private static void checkBuild(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new BuildException(String.format(message, args));
        }
    }

    private static ImmutableList<String> readLinesFromResource(String name) {
        try (InputStream in = CleanOutputDirectoryTask.class.getResourceAsStream(name)) {
            return ImmutableList.copyOf(CharStreams.readLines(new InputStreamReader(in)));
        } catch (IOException e) {
            throw new RuntimeException("cannot read resource: " + name, e);
        }
    }
}
