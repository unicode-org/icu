package com.ibm.icu.dev.tool.bnd;

import java.io.File;
import java.util.Formatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Type;
import aQute.libg.reporter.ReporterAdapter;

/**
 * A custom implementation of the bnd baseline command inspired by the BaselineMojo.
 * <p>
 * The default {@link aQute.bnd.main.BaselineCommands} does not allow to fail the build when there is a mismatch.
 */
public class BaselineCheck {

    public static void main(String... args) throws Exception {
        boolean argFailPackageMismatch = false;
        boolean argFailBundleMismatch = false;
        boolean argPackageReport = false;
        boolean argBundleReport = false;
        String argDiffIgnore = "";
        String argDiffPackages = "*";

        if (args.length < 2) {
            System.err.println("Insufficient arguments. At least 2 expected: <newJar> <oldJar>");
            System.exit(1);
        }

        if (args.length > 2) {
            for (int i = 0; i < args.length - 2; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--failPackageMismatch":
                        argFailPackageMismatch = true;
                        break;
                    case "--failBundleMismatch":
                        argFailBundleMismatch = true;
                        break;
                    case "--packageReport":
                        argPackageReport = true;
                        break;
                    case "--bundleReport":
                        argBundleReport = true;
                        break;
                    case "--diffIgnore":
                        if (++i < args.length) {
                            argDiffIgnore = args[i];
                        } else {
                            System.err.println("Missing argument value for --diffIgnore");
                        }
                        break;
                    case "--diffPackages":
                        if (++i < args.length) {
                            argDiffPackages = args[i];
                        } else {
                            System.err.println("Missing argument value for --diffPackages");
                        }
                        break;
                    default:
                        System.out.println("Ignore argument: " + arg);
                        break;
                }
            }
        }

        BaselineCheck baselineCheck = new BaselineCheck(argFailPackageMismatch, argFailBundleMismatch, argPackageReport, argBundleReport, argDiffIgnore, argDiffPackages);
        File newJar = new File(args[args.length - 2]);
        File oldJar = new File(args[args.length - 1]);

        System.out.println("Baseline");
        if (!baselineCheck.execute(newJar, oldJar)) {
            System.exit(10);
        }
    }

    private final boolean argFailPackageMismatch;
    private final boolean argFailBundleMismatch;
    private final boolean failOnError;
    private final boolean packageReport;
    private final boolean bundleReport;
    private final String diffIgnores;
    private final String diffPackages;

    private BaselineCheck(boolean argFailPackageMismatch, boolean argFailBundleMismatch, boolean packageReport, boolean bundleReport, String diffIgnores,
        String diffPackages) {
        this.argFailPackageMismatch = argFailPackageMismatch;
        this.argFailBundleMismatch = argFailBundleMismatch;
        this.failOnError = argFailPackageMismatch || argFailBundleMismatch;
        this.packageReport = packageReport;
        this.bundleReport = bundleReport;
        this.diffIgnores = diffIgnores;
        this.diffPackages = diffPackages;
    }

    private boolean execute(File newJar, File oldJar) throws Exception {
        try (Processor processor = new Processor()) {
            if ((oldJar == null || !oldJar.exists())) {
                System.out.printf("baseline not found: %s.%n", oldJar);
                return !failOnError;
            }

            ReporterAdapter reporter;
            if (packageReport || bundleReport) {
                reporter = new ReporterAdapter(System.out);
                reporter.setTrace(true);
            } else {
                reporter = new ReporterAdapter();
            }

            DiffPluginImpl differ = new DiffPluginImpl();
            differ.setIgnore(new Parameters(diffIgnores, processor));
            Baseline baseline = new Baseline(reporter, differ);
            Instructions instructions = new Instructions(new Parameters(diffPackages, processor));

            if (checkFailures(newJar, oldJar, baseline, instructions)) {
                System.out.printf("The baselining check failed when checking %s against %s.%n", newJar, oldJar);
                return !failOnError;
            } else {
                System.out.printf("Baselining check succeeded checking %s against %s.%n", newJar, oldJar);
                return true;
            }
        }
    }

    private boolean checkFailures(File newJar, File oldJar, Baseline baseline, Instructions diffpackages) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Formatter f = new Formatter(sb, Locale.US); Jar newer = new Jar(newJar); Jar older = new Jar(oldJar)) {
            boolean failed = false;

            for (Baseline.Info info : baseline.baseline(newer, older, diffpackages)) {
                if (info.mismatch) {
                    failed = argFailPackageMismatch;
                    sb.setLength(0);
                    f.format(
                        "Baseline mismatch for package %s, %s change. Current is %s, repo is %s, suggest %s or %s",
                        info.packageName, info.packageDiff.getDelta(), info.newerVersion, info.olderVersion,
                        info.suggestedVersion, info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders);
                    if (packageReport) {
                        f.format("%n%#S", info.packageDiff);
                    }
                    System.out.println(f);
                }
            }

            if (failed) {
                // early exit when failed to not duplicate the output with the bundle info
                return true;
            }

            Baseline.BundleInfo binfo = baseline.getBundleInfo();

            if (binfo.mismatch) {
                failed = argFailBundleMismatch;
                sb.setLength(0);
                f.format("The bundle version change (%s to %s) is too low, the new version must be at least %s",
                    binfo.olderVersion, binfo.newerVersion, binfo.suggestedVersion);
                if (bundleReport) {
                    f.format("%n%#S", baseline.getDiff());
                }
                System.out.println(f);
            }

            return failed;
        }
    }
}
