package com.ibm.text.UCD;

import java.io.IOException;
import java.io.PrintWriter;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.PrettyPrinter;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.text.utility.Utility;

public class IDNTester {
	static StringBuffer inbuffer = new StringBuffer();
	static StringBuffer intermediate, outbuffer;
	static final int OK = 0, DELETED = 1, ILLEGAL = 2, REMAPPED = 3, IDNA_TYPE_LIMIT = 4;
	static UnicodeSet IDNInputOnly = new UnicodeSet();
	static UnicodeSet IDNOutput = new UnicodeSet();
	static boolean initialized = false;
	static UnicodeSet IDInputOnly32 = new UnicodeSet();
	static UnicodeSet IDOutput32 = new UnicodeSet();
	static UnicodeSet IDInputOnly50 = new UnicodeSet();
	static UnicodeSet IDOutput50 = new UnicodeSet();
	static PrettyPrinter pp = new PrettyPrinter();
	static PrintWriter pw;
	
	public static void main(String[] args) throws IOException {
		initialize();
		pw = BagFormatter.openUTF8Writer(Utility.GEN_DIR, "idnCount.html");
		pw.println("<html><body>");
		showSet("IDN InputOnly: ", IDNInputOnly);
		showSet("IDN Output: ", IDNOutput);
		showSet("ID InputOnly, U3.2: ", IDInputOnly32);
		showSet("ID Output, U3.2: ", IDOutput32);
		
		showSet("IDN Output - ID Output, U3.2: ", new UnicodeSet(IDNOutput).removeAll(IDOutput32));
		showSet("IDN Output & ID Output, U3.2: ", new UnicodeSet(IDNOutput).retainAll(IDOutput32));
		showSet("ID Output - IDN Output, U3.2: ", new UnicodeSet(IDOutput32).removeAll(IDNOutput));
		
		showSet("ID InputOnly, U5.0: ", IDInputOnly50);
		showSet("ID Output, U5.0: ", IDOutput50);
		showSet("ID Output, U5.0 - U3.2: ", new UnicodeSet(IDOutput50).removeAll(IDOutput32));
		
		pw.println("</body></html>");

		pw.close();
	}
	
	public static void showSet(String title, UnicodeSet set) {
		pw.println("<h2>" + title + set.size() + "</h2>" + "<p>" + pp.toPattern(set) + "</p>");
		pw.println();
	}
	
	static UnicodeSet getIDNInput() {
		if (!initialized) initialize();
		return IDNInputOnly;
	}

	static UnicodeSet getIDNOutput() {
		if (!initialized) initialize();
		return IDNInputOnly;
	}

	private static void initialize() {
		UnicodeSet oddballs = new UnicodeSet("[\u034F \u180B-\u180D \uFE00-\uFE0F _]");
		UCD U32 = UCD.make("3.2.0");
		Normalizer nfkc32 = new Normalizer(Normalizer.NFKC, "3.2.0");
		UCDProperty xid32 = DerivedProperty.make(UCD.Mod_ID_Continue_NO_Cf,U32);
		UnicodeSet IDInput32 = xid32.getSet();
		IDInput32.add('-').removeAll(oddballs);
		
		UCD U50 = UCD.make("5.0.0");
		Normalizer nfkc50 = new Normalizer(Normalizer.NFKC, "5.0.0");
		UCDProperty xid50 = DerivedProperty.make(UCD.Mod_ID_Continue_NO_Cf,U50);
		UnicodeSet IDInput50 = xid50.getSet();
		IDInput50.add('-').removeAll(oddballs);
		
		for (int i = 0; i < 0x10FFFF; ++i) {
			if ((i & 0xFFF) == 0) {
				System.out.println(i);
				System.out.flush();
			}
			int type = getIDNAType(i);
			if (type == OK) {
				IDNOutput.add(i);
			} else if (type != ILLEGAL) {
				IDNInputOnly.add(i);
			}
			if (IDInput32.contains(i)) {
				splitSet(IDInputOnly32, IDOutput32, U32, nfkc32, i);
			}
			if (IDInput50.contains(i)) {
				splitSet(IDInputOnly50, IDOutput50, U50, nfkc50, i);
			}
		}
		initialized = true;
	}

	private static void splitSet(UnicodeSet inputOnlySet, UnicodeSet outputSet, UCD ucd, Normalizer nfkc, int i) {
		if (i < 0x7F) {
			outputSet.add(i);
			return;
		}
		String v = UTF16.valueOf(i);
		String s = ucd.getCase(i, UCD.FULL, UCD.FOLD);
		if (s.equals(v)) {
			s = nfkc.normalize(s);
			if (s.equals(v)) {
				s = ucd.getCase(s, UCD.FULL, UCD.FOLD);
				if (s.equals(v)) {
					outputSet.add(i);
					return;
				}
			}
		}
		inputOnlySet.add(i);
	}

	static public int getIDNAType(int cp) {
		if (cp == '-') return OK;
		inbuffer.setLength(0);
		UTF16.append(inbuffer, cp);
		try {
			intermediate = IDNA.convertToASCII(inbuffer,
					IDNA.DEFAULT); // USE_STD3_RULES
			if (intermediate.length() == 0)
				return DELETED;
			outbuffer = IDNA.convertToUnicode(intermediate,
					IDNA.USE_STD3_RULES);
		} catch (StringPrepParseException e) {
			return ILLEGAL;
		} catch (Exception e) {
			System.out.println("Failure at: " + Utility.hex(cp));
			return ILLEGAL;
		}
		if (!TestData.equals(inbuffer, outbuffer))
			return REMAPPED;
		return OK;
	}

}