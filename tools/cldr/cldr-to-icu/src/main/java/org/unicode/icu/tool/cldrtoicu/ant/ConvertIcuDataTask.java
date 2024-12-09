// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.ant;

import static com.google.common.base.CharMatcher.inRange;
import static com.google.common.base.CharMatcher.is;
import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableTable.toImmutableTable;
import static com.google.common.collect.Tables.immutableCell;
import static java.util.stream.Collectors.joining;
import static org.unicode.cldr.api.CldrPath.parseDistinguishingPath;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.icu.tool.cldrtoicu.AlternateLocaleData;
import org.unicode.icu.tool.cldrtoicu.IcuConverterConfig;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter.OutputType;
import org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir;
import org.unicode.icu.tool.cldrtoicu.PseudoLocales;
import org.unicode.icu.tool.cldrtoicu.SupplementalData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table.Cell;

public final class ConvertIcuDataTask extends Task {
    private static final Splitter LIST_SPLITTER =
            Splitter.on(CharMatcher.anyOf(",\n")).trimResults(whitespace()).omitEmptyStrings();

    private static final CharMatcher DIGIT_OR_UNDERSCORE = inRange('0', '9').or(is('_'));
    private static final CharMatcher UPPER_UNDERSCORE = inRange('A', 'Z').or(DIGIT_OR_UNDERSCORE);
    private static final CharMatcher LOWER_UNDERSCORE = inRange('a', 'z').or(DIGIT_OR_UNDERSCORE);
    private static final CharMatcher VALID_ENUM_CHAR = LOWER_UNDERSCORE.or(UPPER_UNDERSCORE);

    private Path cldrPath;
    private CldrDraftStatus minimumDraftStatus;
    // Set of default locale ID specifiers (wildcard IDs which are expanded).
    private LocaleIds localeIds = null;
    // Per directory overrides (fully specified locale IDs).
    private final SetMultimap<IcuLocaleDir, String> perDirectoryIds = HashMultimap.create();
    private final SetMultimap<IcuLocaleDir, String> inheritLanguageSubtag = HashMultimap.create();
    private final IcuConverterConfig.Builder config = IcuConverterConfig.builder();
    // Don't try and resolve actual paths until inside the execute method.
    private final List<AltPath> altPaths = new ArrayList<>();
    // TODO(CLDR-13381): Move into CLDR API; e.g. withPseudoLocales()
    private boolean includePseudoLocales = false;
    private Predicate<String> idFilter = id -> true;

    public void setOutputDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        config.setOutputDir(Paths.get(path));
    }

    public void setCldrDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        this.cldrPath = checkNotNull(Paths.get(path));
    }

    public void setIcuVersion(String icuVersion) {
        config.setIcuVersion(icuVersion);
    }

    public void setIcuDataVersion(String icuDataVersion) {
        config.setIcuDataVersion(icuDataVersion);
    }

    public void setCldrVersion(String cldrVersion) {
        config.setCldrVersion(cldrVersion);
    }

    public void setMinimalDraftStatus(String status) {
        minimumDraftStatus = resolve(CldrDraftStatus.class, status);
    }

    public void setOutputTypes(String types) {
        ImmutableList<OutputType> typeList =
            LIST_SPLITTER
                .splitToList(types).stream()
                .map(s -> resolve(OutputType.class, s))
                .collect(toImmutableList());
        if (!typeList.isEmpty()) {
            config.setOutputTypes(typeList);
        }
    }

    public void setSpecialsDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        config.setSpecialsDir(Paths.get(path));
    }

    public void setIncludePseudoLocales(boolean includePseudoLocales) {
        this.includePseudoLocales = includePseudoLocales;
    }

    public void setLocaleIdFilter(String idFilterRegex) {
        this.idFilter = Pattern.compile(idFilterRegex).asPredicate();
    }

    public void setEmitReport(boolean emit) {
        config.setEmitReport(emit);
    }

    public void setParallel(boolean parallel) {
        config.setParallel(parallel);
    }

    public static final class LocaleIds extends Task {
        private ImmutableSet<String> ids;

        public void addText(String localeIds) {
            this.ids = parseLocaleIds(localeIds);
        }

        @Override
        public void init() throws BuildException {
            checkBuild(!ids.isEmpty(), "Locale IDs must be specified");
        }
    }

    public static final class Directory extends Task {
        private IcuLocaleDir dir;
        private ImmutableSet<String> inheritLanguageSubtag = ImmutableSet.of();
        private final List<ForcedAlias> forcedAliases = new ArrayList<>();
        private LocaleIds localeIds = null;

        public void setDir(String directory) {
            this.dir = resolve(IcuLocaleDir.class, directory);
        }

        public void setInheritLanguageSubtag(String localeIds) {
            this.inheritLanguageSubtag = parseLocaleIds(localeIds);
        }

        public void addConfiguredForcedAlias(ForcedAlias alias) {
            forcedAliases.add(alias);
        }

        public void addConfiguredLocaleIds(LocaleIds localeIds) {
            checkBuild(this.localeIds == null,
                "Cannot add more that one <localeIds> element for <directory>: %s", dir);
            this.localeIds =  localeIds;
        }

        @Override
        public void init() throws BuildException {
            checkBuild(dir != null, "Directory attribute 'dir' must be specified");
            checkBuild(localeIds != null, "<localeIds> must be specified for <directory>: %s", dir);
        }
    }

    public static final class ForcedAlias extends Task {
        private String source = "";
        private String target = "";

        public void setSource(String source) {
            this.source = whitespace().trimFrom(source);
        }

        public void setTarget(String target) {
            this.target = whitespace().trimFrom(target);
        }

        @Override
        public void init() throws BuildException {
            checkBuild(!source.isEmpty(), "Alias source must not be empty");
            checkBuild(!target.isEmpty(), "Alias target must not be empty");
        }
    }

    public static final class AltPath extends Task {
        private String source = "";
        private String target = "";
        private ImmutableSet<String> localeIds = ImmutableSet.of();

        public void setTarget(String target) {
            this.target = target.replace('\'', '"');
        }

        public void setSource(String source) {
            this.source = source.replace('\'', '"');
        }

        public void setLocales(String localeIds) {
            this.localeIds = parseLocaleIds(localeIds);
        }

        @Override
        public void init() throws BuildException {
            checkBuild(!source.isEmpty(), "Source path not be empty");
            checkBuild(!target.isEmpty(), "Target path not be empty");
        }
    }

    public void addConfiguredLocaleIds(LocaleIds localeIds) {
        checkBuild(this.localeIds == null, "Cannot add more that one <localeIds> element");
        this.localeIds =  localeIds;
    }

    public void addConfiguredDirectory(Directory filter) {
        checkState(!perDirectoryIds.containsKey(filter.dir),
            "directory %s specified twice", filter.dir);
        ImmutableSet<String> ids = filter.localeIds.ids;
        perDirectoryIds.putAll(filter.dir, ids);

        // Check that any locale IDs marked to inherit the base language (instead of root) are
        // listed in the set of generated locales.
        inheritLanguageSubtag.putAll(filter.dir, filter.inheritLanguageSubtag);
        if (!ids.containsAll(filter.inheritLanguageSubtag)) {
            log(String.format(
                "WARNING: Locale IDs listed in 'inheritLanguageSubtag' should also be listed "
                    + "in <localeIds> for that directory (%s): %s",
                filter.dir, String.join(", ", Sets.difference(filter.inheritLanguageSubtag, ids))));
            perDirectoryIds.putAll(filter.dir, filter.inheritLanguageSubtag);
        }

        // Check that locales specified for forced aliases in this directory are also listed in
        // the set of generated locales.
        filter.forcedAliases.forEach(a -> config.addForcedAlias(filter.dir, a.source, a.target));
        Set<String> sourceIds =
            filter.forcedAliases.stream().map(a -> a.source).collect(Collectors.toSet());
        if (!ids.containsAll(sourceIds)) {
            Set<String> missingIds = Sets.difference(sourceIds, ids);
            log(String.format(
                "WARNING: Locale IDs listed as sources of a <forcedAlias> should also be listed "
                    + "in <localeIds> for that directory (%s): %s",
                filter.dir, String.join(", ", missingIds)));
            perDirectoryIds.putAll(filter.dir, missingIds);
        }
        Set<String> targetIds =
            filter.forcedAliases.stream().map(a -> a.target).collect(Collectors.toSet());
        if (!ids.containsAll(targetIds)) {
            Set<String> missingIds = Sets.difference(targetIds, ids);
            log(String.format(
                "WARNING: Locale IDs listed as targets of a <forcedAlias> should also be listed "
                    + "in <localeIds> for that directory (%s): %s",
                filter.dir, String.join(", ", missingIds)));
            perDirectoryIds.putAll(filter.dir, missingIds);
        }
    }

    // Aliases on the outside are applied to all directories.
    public void addConfiguredForcedAlias(ForcedAlias alias) {
        for (IcuLocaleDir dir : IcuLocaleDir.values()) {
            config.addForcedAlias(dir, alias.source, alias.target);
        }
    }

    public void addConfiguredAltPath(AltPath altPath) {
        // Don't convert to CldrPath here (it triggers a bunch of CLDR data loading for the DTDs).
        // Wait until the "execute()" method since in future we expect to use the configured CLDR
        // directory explicitly there.
        altPaths.add(altPath);
    }

    public void execute() throws BuildException {
        // Spin up CLDRConfig outside of other inner loops, to
        // avoid static init problems seen in CLDR-14636
        CLDRConfig.getInstance().getSupplementalDataInfo();

        checkBuild(localeIds != null, "<localeIds> must be specified");

        CldrDataSupplier src = CldrDataSupplier
            .forCldrFilesIn(cldrPath)
            .withDraftStatusAtLeast(minimumDraftStatus);

        // We must do this wrapping of the data supplier _before_ creating the supplemental data
        // instance since adding pseudo locales affects the set of available locales.
        // TODO: Move some/all of this into the base converter and control it via the config.
        if (!altPaths.isEmpty()) {
            src = AlternateLocaleData.transform(src, getGlobalAltPaths(), getLocaleAltPaths());
        }
        if (includePseudoLocales) {
            src = PseudoLocales.addPseudoLocalesTo(src);
        }

        SupplementalData supplementalData = SupplementalData.create(src);
        ImmutableSet<String> defaultTargetIds =
            LocaleIdResolver.expandTargetIds(this.localeIds.ids, supplementalData);
        for (IcuLocaleDir dir : IcuLocaleDir.values()) {
            Iterable<String> ids = perDirectoryIds.asMap().getOrDefault(dir, defaultTargetIds);
            config.addLocaleIds(dir, Iterables.filter(ids, idFilter::test));

            // We should only have locale IDs like "zh_Hant" here (language + script) and only
            // those which would naturally inherit to "root"
            inheritLanguageSubtag.get(dir).forEach(id -> {
                checkArgument(id.matches("[a-z]{2}_[A-Z][a-z]{3}"),
                    "Invalid locale ID for inheritLanguageSubtag (expect '<lang>_<Script>'): ", id);
                checkArgument(supplementalData.getParent(id).equals("root"),
                    "Invalid locale ID for inheritLanguageSubtag (parent must be 'root'): ", id);
                config.addForcedParent(dir, id, id.substring(0, 2));
            });
        }
        config.setMinimumDraftStatus(minimumDraftStatus);
        LdmlConverter.convert(src, supplementalData, config.build());
    }

    private ImmutableMap<CldrPath, CldrPath> getGlobalAltPaths() {
        // This fails if the same key appears more than once.
        return altPaths.stream()
            .filter(a -> a.localeIds.isEmpty())
            .collect(toImmutableMap(
                a -> parseDistinguishingPath(a.target),
                a -> parseDistinguishingPath(a.source)));
    }

    private ImmutableTable<String, CldrPath, CldrPath> getLocaleAltPaths() {
        return altPaths.stream()
            .flatMap(
                a -> a.localeIds.stream().map(
                    id -> immutableCell(
                        id,
                        parseDistinguishingPath(a.target),
                        parseDistinguishingPath(a.source))))
            // Weirdly there's no collector method to just collect cells.
            .collect(toImmutableTable(Cell::getRowKey, Cell::getColumnKey, Cell::getValue));
    }

    private static void checkBuild(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new BuildException(String.format(message, args));
        }
    }

    private static ImmutableSet<String> parseLocaleIds(String localeIds) {
        // Need to filter out '//' style end-of-line comments first (replace with \n to avoid
        // inadvertently joining two elements.
        localeIds = localeIds.replaceAll("//[^\n]*\n", "\n");
        return ImmutableSet.copyOf(LIST_SPLITTER.splitToList(localeIds));
    }

    private static <T extends Enum<T>> T resolve(Class<T> enumClass, String name) {
        checkArgument(!name.isEmpty(), "enumeration name cannot be empty");
        checkArgument(VALID_ENUM_CHAR.matchesAllOf(name),
            "invalid enumeration name '%s'; expected only ASCII letters or '_'", name);
        CaseFormat format;
        if (UPPER_UNDERSCORE.matchesAllOf(name)) {
            format = CaseFormat.UPPER_UNDERSCORE;
        } else if (LOWER_UNDERSCORE.matchesAllOf(name)) {
            format = CaseFormat.LOWER_UNDERSCORE;
        } else {
            // Mixed case with '_' is not permitted.
            checkArgument(!name.contains("_"),
                "invalid enumeration name '%s'; mixed case with underscore not allowed: %s", name);
            format =
                Ascii.isLowerCase(name.charAt(0)) ? CaseFormat.LOWER_CAMEL : CaseFormat.UPPER_CAMEL;
        }
        try {
            return Enum.valueOf(enumClass, format.to(CaseFormat.UPPER_UNDERSCORE, name));
        } catch (IllegalArgumentException e) {
            String validNames =
                Arrays.stream(enumClass.getEnumConstants())
                    .map(Object::toString)
                    .collect(joining(", "));
            throw new IllegalArgumentException(
                "invalid enumeration name " + name + "; expected one of; " + validNames);
        }
    }

    private static AltPath getAltPath(Element elem) {
        if (!"altPath".equals(elem.getTagName())) {
            return null;
        }
        String source = elem.getAttribute("source");
        String target = elem.getAttribute("target");
        String locales = elem.getAttribute("locales");
        AltPath ap = new AltPath();
        ap.setSource(source);
        ap.setTarget(target);
        ap.setLocales(locales);
        ap.init();
        return ap;
    }

    private static ForcedAlias getForcedAlias(Element elem) {
        if (!"forcedAlias".equals(elem.getTagName())) {
            return null;
        }
        String source = elem.getAttribute("source");
        String target = elem.getAttribute("target");
        ForcedAlias fa = new ForcedAlias();
        fa.setSource(source);
        fa.setTarget(target);
        fa.init();
        return fa;
    }

    private static LocaleIds getLocaleIds(Element elem) {
        if (!"localeIds".equals(elem.getTagName())) {
            return null;
        }
        LocaleIds localeIds = new LocaleIds();
        String strLocaleIds = elem.getTextContent();
        localeIds.addText(strLocaleIds);
        localeIds.init();
        return localeIds;
    }

    private static Directory getDirectory(Element element) {
        if (!"directory".equals(element.getTagName())) {
            return null;
        }
        String dir = element.getAttribute("dir");
        String inheritLanguageSubtag = element.getAttribute("inheritLanguageSubtag");
        Directory directory = new Directory();
        directory.setDir(dir);
        directory.setInheritLanguageSubtag(inheritLanguageSubtag);
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                switch (childElement.getTagName()) {
                    case "localeIds":
                        LocaleIds localeIds = getLocaleIds(childElement);
                        directory.addConfiguredLocaleIds(localeIds);
                        break;
                    case "forcedAlias":
                        ForcedAlias fa = getForcedAlias(childElement);
                        directory.addConfiguredForcedAlias(fa);
                        break;
                    default:
                }
            }
            node = node.getNextSibling();
        }
        if (directory.localeIds == null) {
            directory.addConfiguredLocaleIds(new LocaleIds());
        }
        directory.init();
        return directory;
    }

    public static ConvertIcuDataTask fromXml(String fileName) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new File(fileName));
            Element root = doc.getDocumentElement();
            if (!"config".equals(root.getTagName())) {
                System.err.println("The root of the config file should be <config>");
                return null;
            }

            NodeList convertNodes = root.getElementsByTagName("convert");
            if (convertNodes.getLength() != 1) {
                System.err.println("Exactly one <convert> element allowed and required");
                return null;
            }
            ConvertIcuDataTask converter = new ConvertIcuDataTask();
            Node node = convertNodes.item(0).getFirstChild();
            while (node != null) {
                if (node instanceof Element) {
                    Element childElement = (Element) node;
                    String nodeName = childElement.getTagName();
                    switch (nodeName) {
                        case "localeIds":
                            LocaleIds localeIds = getLocaleIds(childElement);
                            converter.addConfiguredLocaleIds(localeIds);
                            break;
                        case "directory":
                            Directory directory = getDirectory(childElement);
                            converter.addConfiguredDirectory(directory);
                            break;
                        case "forcedAlias":
                            ForcedAlias fa = getForcedAlias(childElement);
                            converter.addConfiguredForcedAlias(fa);
                            break;
                        case "altPath":
                            AltPath altPath = getAltPath(childElement);
                            converter.addConfiguredAltPath(altPath);
                            break;
                        default:
                            break;
                    }
                }
                node = node.getNextSibling();
            }
            return converter;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
