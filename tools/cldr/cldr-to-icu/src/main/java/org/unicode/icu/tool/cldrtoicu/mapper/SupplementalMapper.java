// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.mapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.unicode.cldr.api.CldrData.PathOrder.NESTED_GROUPING;

import java.util.function.Predicate;

import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.CldrValue;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer;
import org.unicode.icu.tool.cldrtoicu.PathValueTransformer.Result;
import org.unicode.icu.tool.cldrtoicu.RbPath;

/**
 * Generate supplemental {@link IcuData} by transforming {@link CldrDataType#SUPPLEMENTAL
 * SUPPLEMENTAL} data using a {@link PathValueTransformer}.
 *
 * <p>This is currently driven by the {@code ldml2icu_supplemental.txt} configuration file via a
 * {@code RegexTransformer}, but could use any {@link PathValueTransformer} implementation.
 */
public final class SupplementalMapper extends AbstractPathValueMapper {
    private static final RbPath RB_FIFO = RbPath.of("<FIFO>");

    /**
     * Processes a subset of supplemental data from the given supplier.
     *
     * @param src the CLDR data supplier to process.
     * @param transformer the transformer to match and transform each CLDR path/value pair.
     * @param icuName the name for the generated IcuData.
     * @param paths a matcher to select the CLDR paths to be transformed.
     * @return An IcuData instance containing the specified subset of supplemental data with the
     *     given ICU name.
     */
    // TODO: Improve external data splitting and remove need for a PathMatcher here.
    public static IcuData process(
        CldrDataSupplier src,
        PathValueTransformer transformer,
        String icuName,
        Predicate<CldrPath> paths) {

        IcuData icuData = new IcuData(icuName, false);
        new SupplementalMapper(src, transformer, paths).addIcuData(icuData);
        return icuData;
    }

    private final Predicate<CldrPath> paths;
    private int fifoCounter = 0;

    private SupplementalMapper(
        CldrDataSupplier src, PathValueTransformer transformer, Predicate<CldrPath> pathFilter) {

        super(src.getDataForType(CldrDataType.SUPPLEMENTAL), transformer);
        this.paths = checkNotNull(pathFilter);
    }

    @Override
    void addResults() {
        // NESTED_GROUPING and DTD order differ because of how the magic <FIFO> label works (it
        // basically enforces "encounter order" onto things in unlabelled sequences, which matches
        // the old behaviour). If it wouldn't break anything, it might be worth moving to DTD order
        // to remove any lingering implicit dependencies on the CLDR data behaviour.
        getCldrData().accept(NESTED_GROUPING, this::visit);
    }

    private void visit(CldrValue value) {
        if (paths.test(value.getPath())) {
            transformValue(value).forEach(this::collectResult);
            fifoCounter++;
        }
    }

    // <FIFO> hidden labels could be supported in the abstract mapper, but would need a "bulk" add
    // method for results (since the counter is updated once per batch, which corresponds to once
    // per rule). Having the same FIFO counter value for the same group of values is essential
    // since it serves to group them.
    //
    // TODO: Improve this and push this up into the abstract class (so it works with LocaleMapper).
    private void collectResult(Result r) {
        RbPath rbPath = r.getKey();
        if (rbPath.contains(RB_FIFO)) {
            // The fifo counter needs to be formatted with leading zeros for sorting.
            rbPath = rbPath.mapSegments(
                s -> s.equals("<FIFO>") ? String.format("<%04d>", fifoCounter) : s);
        }
        addResult(rbPath, r);
    }
}
