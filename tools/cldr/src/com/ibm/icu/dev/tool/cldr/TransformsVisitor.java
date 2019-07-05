package com.ibm.icu.dev.tool.cldr;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrData.ValueVisitor;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.function.Function;

import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;

final class TransformsVisitor {
    // <supplementalData>
    //    <transforms>
    //        <transform source="Latn" target="Ethi" direction="both" draft="contributed" alias="Latin-Ethiopic und-Ethi-t-und-latn" backwardAlias="Ethiopic-Latin und-Latn-t-und-ethi">
    //            <tRule>
    private static final PathMatcher TRULE =
        PathMatcher.of("supplementalData/transforms/transform/tRule");
    private static final AttributeKey TRANSFORM_SOURCE = keyOf("transform", "source");
    private static final AttributeKey TRANSFORM_TARGET = keyOf("transform", "target");
    private static final AttributeKey TRANSFORM_DIRECTION = keyOf("transform", "direction"); // (forward | backward | both)
    private static final AttributeKey TRANSFORM_VARIANT = keyOf("transform", "variant");
    private static final AttributeKey TRANSFORM_ALIAS = keyOf("transform", "alias");
    private static final AttributeKey TRANSFORM_BACKALIAS = keyOf("transform", "backwardAlias");
    private static final AttributeKey TRANSFORM_VISIBILITY = keyOf("transform", "visibility"); // (internal | external)

    private enum Direction { forward, backard, both }

    //        String status = "internal".equals(visibility) ? "internal" : "file";

    /** Processes data from the given supplier to generate day period data. */
    public static IcuData process(CldrDataSupplier supplier, Function<Path, PrintWriter> outFn) {
        // Supplemental data, ordered by DTD.
        // Scan for paths, write file if present and add to IcuData.

        RuleVisitor v = new RuleVisitor();
        supplier.getDataForType(SUPPLEMENTAL).accept(DTD, v);
        return null;
    }

    static class RuleVisitor implements ValueVisitor {
        @Override public void visit(CldrValue value) {
            if (TRULE.matches(value.getPath())) {
//                String source = TRANSFORM_SOURCE.valueFrom(value);
//                String target = TRANSFORM_TARGET.valueFrom(value);
//                Direction dir = TRANSFORM_DIRECTION.valueFrom(value, Direction.class);
//
//                System.out.println(source + "-" + target + ":" + dir);
            }
        }
    }


    // /transforms/*.xml
    // Supplemental Data - CONTRIBUTED
    // match = ((?!.*(Canadian|Ethiopic|ug-Latin).*).*)
    // commentSkip, approvedOnly


    //    <target name="translit" depends="init,setup" description="builds collation files in ICU text format">
    //        <cldr-build toolName="org.unicode.cldr.icu.ConvertTransforms" srcFile=".*xml" destFile=".*txt">
    //            <run>
    //                <args>
    //                    <arg name="-m" value="((?!.*(Canadian|Ethiopic|ug-Latin).*).*)" />
    //                    <arg name="--sourcedir" value="${env.CLDR_DIR}/common/transforms" />
    //                    <arg name="--destdir"   value="${env.ICU4C_DIR}/source/data/translit"/>
    //                    <arg name="--commentSkip"/>
    //                    <arg name="--approvedOnly"/>
    //                </args>
    //                <!-- http://ant.apache.org/faq.html#xml-entity-include -->
    //
    //            </run>
    //        </cldr-build>
    //    </target>

//        Set<String> ids = cldrFactory.getAvailable();
//        PrintWriter index = FileUtilities.openUTF8Writer(outputDirectory, "root.txt");
//        doHeader(index, "//", "root.txt");
//        try {
//            index.println("root {");
//            index.println("    RuleBasedTransliteratorIDs {");
//            // addAlias(index, "Latin", "el", "", "Latin", "Greek", "UNGEGN");
//            // addAlias(index, "el", "Latin", "", "Greek", "Latin", "UNGEGN");
//            // addAlias(index, "Latin", "Jamo", "", "Latin", "ConjoiningJamo", "");
//            addAlias(index, "Tone", "Digit", "", "Pinyin", "NumericPinyin", "");
//            addAlias(index, "Digit", "Tone", "", "NumericPinyin", "Pinyin", "");
//            // addAlias(index, "Simplified", "Traditional", "", "Hans", "Hant", "");
//            // addAlias(index, "Traditional", "Simplified", "", "Hant", "Hans", "");
//            for (String id : ids) {
//                if (id.equals("All")) continue;
//                try {
//                    convertFile(cldrFactory, id, outputDirectory, index);
//                } catch (IOException e) {
//                    System.err.println("Failure in: " + id);
//                    throw e;
//                }
//            }
//            index.println("    }");
//            index.println("    TransliteratorNamePattern {");
//            index.println("        // Format for the display name of a Transliterator.");
//            index.println("        // This is the language-neutral form of this resource.");
//            index.println("        \"{0,choice,0#|1#{1}|2#{1}-{2}}\" // Display name");
//            index.println("    }");
//            index.println("    // Transliterator display names");
//            index.println("    // This is the English form of this resource.");
//            index.println("    \"%Translit%Hex\"         { \"%Translit%Hex\" }");
//            index.println("    \"%Translit%UnicodeName\" { \"%Translit%UnicodeName\" }");
//            index.println("    \"%Translit%UnicodeChar\" { \"%Translit%UnicodeChar\" }");
//            index.println("    TransliterateLATIN{        ");
//            index.println("    \"\",");
//            index.println("    \"\"");
//            index.println("    }");
//            index.println("}");
//        } finally {
//            index.close();
//        }

}
