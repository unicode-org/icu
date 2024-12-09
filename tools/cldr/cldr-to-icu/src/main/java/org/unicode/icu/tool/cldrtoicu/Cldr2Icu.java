// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import org.unicode.icu.tool.cldrtoicu.ant.CleanOutputDirectoryTask;
import org.unicode.icu.tool.cldrtoicu.ant.ConvertIcuDataTask;
import org.unicode.icu.tool.cldrtoicu.ant.GenerateCodeTask;

public class Cldr2Icu {
    private final Cldr2IcuCliOptions options = new Cldr2IcuCliOptions();

    private void convert() {
        ConvertIcuDataTask convert = ConvertIcuDataTask.fromXml(options.xmlConfig);

        convert.setCldrDir(options.cldrDataDir);
        convert.setOutputDir(options.outDir);
        convert.setSpecialsDir(options.specialsDir);
        convert.setOutputTypes(options.outputTypes);
        convert.setIcuVersion(options.icuVersion);
        convert.setIcuDataVersion(options.icuDataVersion);
        convert.setCldrVersion(options.cldrVersion);
        convert.setMinimalDraftStatus(options.minDraftStatus);
        convert.setLocaleIdFilter(options.localeIdFilter);
        convert.setIncludePseudoLocales(options.includePseudoLocales);
        convert.setEmitReport(options.emitReport);
        convert.setParallel(options.parallel);

        convert.init();
        convert.execute();
    }

    private void generateCode(String action) {
        GenerateCodeTask generateCode = new GenerateCodeTask();

        generateCode.setCldrDir(options.cldrDataDir);
        generateCode.setCOutDir(options.genCCodeDir);
        generateCode.setJavaOutDir(options.genJavaCodeDir);
        generateCode.setAction(action);

        generateCode.init();
        generateCode.execute();
    }

    private void outputDirectories() {
        CleanOutputDirectoryTask clean = CleanOutputDirectoryTask.fromXml(options.xmlConfig);

        clean.setRoot(options.outDir);
        clean.setForceDelete(options.forceDelete);

        clean.init();
        clean.execute();
    }

    private void clean() {
        outputDirectories();
        generateCode("clean");
    }

    private void generate() {
        convert();
        if (!options.dontGenCode) {
            generateCode(null);
        }
    }

    public static void main(String[] args) {
        Cldr2Icu self = new Cldr2Icu();
        self.options.processArgs(args);
        self.clean();
        self.generate();
    }
}
