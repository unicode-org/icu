package com.ibm.icu.dev.tool.cldr;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.unicode.cldr.api.CldrDraftStatus.UNCONFIRMED;

final class CldrDataConverter {
    private static final PathMatcher COLLATION =
        PathMatcher.inOrder(
            PathMatcher.of("ldml/collations"),
            PathMatcher.of("ldml/special"));

    public static void main(String[] args) throws IOException {
        Path cldrRoot = PathUtils.getCldrRoot();
        Path icuRoot = PathUtils.getIcuRoot();
        Path outDir = Paths.get(args[0]);

        CldrDataSupplier supplier =
            CldrDataSupplier.forCldrFilesIn(cldrRoot).withDraftStatus(UNCONFIRMED);
        Path specialsDir = icuRoot.resolve("icu4c/source/data/xml");

        Bcp47Visitor bcp47 = Bcp47Visitor.process(supplier);
        IcuTextWriter.writeToFile(bcp47.getBcp47Data(), outDir);
        IcuTextWriter.writeToFile(bcp47.getTimezoneData(), outDir);

        IcuTextWriter.writeToFile(PluralsVisitor.process(supplier), outDir);
        IcuTextWriter.writeToFile(PluralRangesVisitor.process(supplier), outDir);
        IcuTextWriter.writeToFile(DayPeriodsVisitor.process(supplier), outDir);

        Path packageDir = PathUtils.getPackageDirectoryFor(CldrDataConverter.class);
        Path configFile = packageDir.resolve("ldml2icu_supplemental.txt");
        IcuTextWriter.writeToFile(SupplementalVisitor.process(supplier, configFile), outDir);

        Path collationDir = outDir.resolve("collation");
        Files.createDirectories(collationDir);
        for (IcuData icuData : CollationVisitor.process(supplier, specialsDir.resolve("collation"))) {
            IcuTextWriter.writeToFile(icuData, collationDir);
        }

        Path brkItrDir = outDir.resolve("brkitr");
        Files.createDirectories(brkItrDir);
        for (IcuData icuData : BreakIteratorVisitor.process(supplier, specialsDir.resolve("brkitr"))) {
            IcuTextWriter.writeToFile(icuData, brkItrDir);
        }

        Path rbnfDir = outDir.resolve("rbnf");
        Files.createDirectories(rbnfDir);
        for (IcuData icuData : RbnfVisitor.process(supplier, specialsDir.resolve("rbnf"))) {
            IcuTextWriter.writeToFile(icuData, rbnfDir);
        }

        PathMatcher m = PathMatcher.of("supplementalData/languageMatching/languageMatches[@type=*]");
        supplier.getDataForType(CldrDataType.SUPPLEMENTAL).accept(CldrData.PathOrder.ARBITRARY, v -> {
            if (m.matchesPrefixOf(v.getPath())) {
                System.out.println(v);
            }
        });

        TransformsVisitor.process(supplier, p -> null);
    }
}
