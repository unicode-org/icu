package com.ibm.icu.dev.test.impl;

import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.impl.units.UnitsRouter;
import com.ibm.icu.util.Measure;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class UnitRouterTest {
    @Test
    public void testUnitPreferencesFromUnitTests() throws IOException {
        class TestCase {

            final ArrayList<Pair<String, MeasureUnitImpl>> outputUnitInOrder = new ArrayList<>();
            final ArrayList<BigDecimal> expectedInOrder = new ArrayList<>();
            /**
             * Test Case Data
             */
            String category;
            String usage;
            String region;
            Pair<String, MeasureUnitImpl> inputUnit;
            BigDecimal input;

            TestCase(String line) {
                String[] fields = line
                        .replaceAll(" ", "") // Remove all the spaces.
                        .replaceAll(",", "") // Remove all the commas.
                        .split(";");

                String category = fields[0];
                String usage = fields[1];
                String region = fields[2];
                String inputValue = fields[4];
                String inputUnit = fields[5];
                ArrayList<Pair<String, String>> outputs = new ArrayList<>();

                for (int i = 6; i < fields.length - 2; i += 2) {
                    if (i == fields.length - 3) { // last field
                        outputs.add(Pair.of(fields[i + 2], fields[i + 1]));
                    } else {
                        outputs.add(Pair.of(fields[i + 1], fields[i]));
                    }
                }

                this.insertData(category, usage, region, inputUnit, inputValue, outputs);
            }

            private void insertData(String category, String usage, String region, String inputUnitString, String inputValue, ArrayList<Pair<String, String>> outputs /* Unit Identifier, expected value */) {
                this.category = category;
                this.usage = usage;
                this.region = region;
                this.inputUnit = Pair.of(inputUnitString, MeasureUnitImpl.UnitsParser.parseForIdentifier(inputUnitString));
                this.input = new BigDecimal(inputValue);
                for (Pair<String, String> output :
                        outputs) {
                    outputUnitInOrder.add(Pair.of(output.first, MeasureUnitImpl.UnitsParser.parseForIdentifier(output.first)));
                    expectedInOrder.add(new BigDecimal(output.second));
                }
            }
        }

        // Read Test data from the unitPreferencesTest
        String codePage = "UTF-8";
        BufferedReader f = TestUtil.getDataReader("units/unitPreferencesTest.txt", codePage);
        ArrayList<TestCase> tests = new ArrayList<>();
        while (true) {
            String line = f.readLine();
            if (line == null) break;
            if (line.isEmpty() || line.startsWith("#")) continue;
            tests.add(new TestCase(line));
        }

        for (TestCase testCase :
                tests) {
            UnitsRouter router = new UnitsRouter(testCase.inputUnit.second, testCase.region, testCase.usage);
            List<Measure> outputs = router.route(testCase.input).measures;

            Assert.assrt(outputs.size() == testCase.expectedInOrder.size()
                    && outputs.size() == testCase.outputUnitInOrder.size());

            for (int i = 0; i < outputs.size(); i++) {
                if (!UnitConverterTest
                        .compareTwoBigDecimal(testCase.expectedInOrder.get(i),
                                BigDecimal.valueOf(outputs.get(i).getNumber().doubleValue()),
                                BigDecimal.valueOf(0.0001))) {
                    Assert.fail(testCase.toString() + outputs.toString());
                }
            }
        }
    }
}
