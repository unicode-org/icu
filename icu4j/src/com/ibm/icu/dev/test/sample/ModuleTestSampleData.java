package com.ibm.icu.dev.test.sample;

import java.util.ListResourceBundle;

/**
 * This is sample data for ModuleTestSample, which is an illustration
 * of a subclass of ModuleTest.  This data is in a format which
 * is understood by ResourceModule, which for simplicity expects
 * all data, including numeric and boolean data, to be represented
 * by Strings.
 */
public class ModuleTestSampleData extends ListResourceBundle {
    public Object[][] getContents() {
	return contents;
    }

    Object[][] contents = {
	{ "Info", new Object[][] {
	    { "Description", "This is a sample test module that illustrates ModuleTest " +
	      "and uses data formatted for ResourceModule." },
	    { "Headers", new String[] {
		"aStringArray", "anIntArray", "aBooleanArray"
	    }},
	}},
	

	{ "Tests", new Object[][] {
	    { "Test01", new Object[][] {
		{ "Info", new Object[][] {
		    { "Description", "A typical test using both settings and cases." },
		    { "Long_Description", "It does not defined its own headers, but instead " +
		      "uses the default headers defined for the module.  " +
		      "There are two sets of settings and three cases." },
		}},
		{ "Settings", new Object[][][] {
		    {{ "aString", "this is a string" },
		     { "anInt", "43" },
		     { "aBoolean", "false" }},
		    {{ "aString", "this is another string" },
		     { "aBoolean", "true" }}
		}},
		{ "Cases", new Object[][] {
		    { new String[] { "one", "two", "three" },
		      new String[] { "24", "48", "72" },
		      new String[] { "true", "false", "true" }
		    },
		    { new String[] { "four", "five", "six" },
		      new String[] { "-1", "-5", "-10" },
		      new String[] { "true", "false", "false" }
		    },
		    { new String[] { "bagel", "peanuts", "carrot" },
		      new String[] { "0", "00001", "10101" },
		      new String[] { "false", "false", "False" }
		    },
		}}
	    }},

	    { "Test02", new Object[][] {
		{ "Info", new Object[][] {
		    { "Description", "A typical test that uses cases but not settings." },
		    { "Long_Description", "It defines its own headers." },
		    { "Headers", new String[] {
			"aString", "anInt", "aBoolean"
		    }},
		}},
		{ "Cases", new Object[][] {
		    { "Superstring", "42", "true" },
		    { "Underdog", "12", "false" },
		    { "ScoobyDoo", "7", "TrUe" }
		}}
	    }},

	    { "Test03", new Object[][] {
		{ "Info", new Object[][] {
		    { "Description", "A typical test that uses just the info, no cases or settings." },
		    { "Extra", "This is some extra information." }
		}},
	    }},

	    // no Test04 data
	    // Test04 should cause an exception to be thrown since ModuleTestSample does not
	    // specify that it is ok for it to have no data.

	    // no Test05 data
	    // Test05 should just log this fact, since ModuleTestSample indicates that it is
	    // ok for Test05 to have no data in its override of validateMethod.

	    { "Test06", new Object[][] {
		{ "Info", new Object[][] {
		    { "Description", "A test that has bad data." },
		    { "Long_Description", "This illustrates how a data error will automatically " +
		      "terminate the settings and cases loop." },
		    { "Headers", new String[] {
			"IsGood", "Data",
		    }},
		}},
		{ "Cases", new Object[][] {
		    { "Good", "23" },
		    { "Good", "-123" },
		    { "Bad", "Whoops" },
		    { "Not Executed", "35" },
		}},
	    }},

	    { "Test07", new Object[][] {
		{ "Info", new Object[][] {
		    { "Description", "A test that fails with a certain combination of settings and case." },
		    { "Long_Description", "This illustrates how a test error will automatically " +
		      "terminate the settings and cases loop.  Settings data is values, the case " +
		      "data is factors.  The third factor is not a factor of the second value.  " +
		      "The test will log an error, which will automatically stop the loop." },
		    { "Headers", new String[] {
			"Factor",
		    }},
		}},
		{ "Settings" , new Object[][][] {
		    {{ "Value", "210" }},
		    {{ "Value", "420" }},
		    {{ "Value", "42" }},
		    {{ "Value", "Not reached." }}
		}},
		{ "Cases", new Object[][] {
		    { "2" },
		    { "3" },
		    { "5" },
		    { "7" },
		}},
	    }},

	    { "Test08", new Object[][] {
		{ "Info", new Object[][] {
		    { "Description", "A test with data missing from a test case." },
		    { "Headers", new String[] {
			"One", "Two", "Three"
		    }},
		}},
		{ "Cases", new Object[][] {
		    { "1", "2", "3" },
		    { "4", "5" }, // too short
		    { "6", "7", "8" },
		}},
	    }},
	}},
    };
}
