/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/lang/UCharacterTest.java,v $ 
* $Date: 2002/07/08 23:52:13 $ 
* $Revision: 1.38 $
*
*******************************************************************************
*/

package com.ibm.icu.dev.test.lang;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterDirection;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.RangeValueIterator;
import com.ibm.icu.util.ValueIterator;
import com.ibm.icu.util.VersionInfo;

import java.io.BufferedReader;
/**
* Testing class for UCharacter
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since nov 04 2000
*/
public final class UCharacterTest extends TestFmwk
{ 
  	// private variables =============================================
  
  	/**
  	* ICU4J data version number
  	*/
  	private final VersionInfo VERSION_ = VersionInfo.getInstance("3.2.0.0");
  
  	// constructor ===================================================
  
  	/**
  	* Constructor
  	*/
  	public UCharacterTest()
  	{
  	}
  
  	// public methods ================================================
  
 	public static void main(String[] arg)
  	{
    	try
    	{
      		UCharacterTest test = new UCharacterTest();
      		test.run(arg);
      		//test.TestGetAge();
      		//test.TestAdditionalProperties();
    	}
    	catch (Exception e)
    	{
      	e.printStackTrace();
    	}
  	}
  
  	/**
  	* Testing the letter and number determination in UCharacter
  	*/
  	public void TestLetterNumber()
  	{
    	for (int i = 0x0041; i < 0x005B; i ++) 
      	if (!UCharacter.isLetter(i))
        	errln("FAIL \\u" + hex(i) + " expected to be a letter");
        
    	for (int i = 0x0660; i < 0x066A; i ++) 
      	if (UCharacter.isLetter(i))
        	errln("FAIL \\u" + hex(i) + " expected not to be a letter");
    
    	for (int i = 0x0660; i < 0x066A; i ++) 
      	if (!UCharacter.isDigit(i))
        	errln("FAIL \\u" + hex(i) + " expected to be a digit");
    
    	for (int i = 0x0041; i < 0x005B; i ++) 
      		if (!UCharacter.isLetterOrDigit(i))
        		errln("FAIL \\u" + hex(i) + " expected not to be a digit");
        
    	for (int i = 0x0660; i < 0x066A; i ++) 
      		if (!UCharacter.isLetterOrDigit(i))
        		errln("FAIL \\u" + hex(i) + 
              		"expected to be either a letter or a digit");
	}

  	/**
  	* Tests for space determination in UCharacter
  	*/
  	public void TestSpaces()
  	{
    	int spaces[] = {0x0020, 0x0000a0, 0x002000, 0x002001, 0x002005};
    	int nonspaces[] = {0x61, 0x0062, 0x0063, 0x0064, 0x0074};
    	int whitespaces[] = {0x2008, 0x002009, 0x00200a, 0x00001c, 0x00000c};
    	int nonwhitespaces[] = {0x61, 0x0062, 0x003c, 0x0028, 0x003f};
                       
    	int size = spaces.length;
    	for (int i = 0; i < size; i ++)
    	{
      		if (!UCharacter.isSpaceChar(spaces[i]))
      		{
        		errln("FAIL \\u" + hex(spaces[i]) + 
              		" expected to be a space character");
        		break;
      		}
      
      		if (UCharacter.isSpaceChar(nonspaces[i]))
      		{
        		errln("FAIL \\u" + hex(nonspaces[i]) + 
          	    " expected not to be space character");
        		break;
      		}
 
     	 	if (!UCharacter.isWhitespace(whitespaces[i]))
      		{
        		errln("FAIL \\u" + hex(whitespaces[i]) + 
              			" expected to be a white space character");
        		break;
      		}
      		if (UCharacter.isWhitespace(nonwhitespaces[i]))
      		{
       	 		errln("FAIL \\u" + hex(nonwhitespaces[i]) + 
              				" expected not to be a space character");
        		break;
      		}
      		logln("Ok    \\u" + hex(spaces[i]) + " and \\u" + 
      		      hex(nonspaces[i]) + " and \\u" + hex(whitespaces[i]) + 
      		      " and \\u" + hex(nonwhitespaces[i]));
    	}
  	}
  
  	/**
  	* Tests for defined and undefined characters
  	*/
  	public void TestDefined()
  	{
    	int undefined[] = {0xfff1, 0xfff7, 0xfa6b};
    	int defined[] = {0x523E, 0x004f88, 0x00fffd};
    
    	int size = undefined.length;
    	for (int i = 0; i < size; i ++) 
    	{
      		if (UCharacter.isDefined(undefined[i]))
      		{
        		errln("FAIL \\u" + hex(undefined[i]) + 
          	    			" expected not to be defined");
        		break;
      		}
      		if (!UCharacter.isDefined(defined[i]))
      		{
        		errln("FAIL \\u" + hex(defined[i]) + " expected defined");
        		break;
      		}
    	}
  	}
  
  	/**
  	* Tests for base characters and their cellwidth
  	*/
  	public void TestBase()
  	{
    	int base[] = {0x0061, 0x000031, 0x0003d2};
    	int nonbase[] = {0x002B, 0x000020, 0x00203B};    
    	int size = base.length;
    	for (int i = 0; i < size; i ++) 
    	{
      		if (UCharacter.isBaseForm(nonbase[i]))
      		{
       		 	errln("FAIL \\u" + hex(nonbase[i]) + 
              				" expected not to be a base character");
        		break;
      		}
      		if (!UCharacter.isBaseForm(base[i]))
      		{
        		errln("FAIL \\u" + hex(base[i]) + 
        		      " expected to be a base character");
        		break;
      		}
    	}
  	}
    
  	/**
  	* Tests for digit characters 
  	*/
  	public void TestDigits()
  	{
    	int digits[] = {0x0030, 0x000662, 0x000F23, 0x000ED5, 0x002160};
    
    	//special characters not in the properties table
     	int digits2[] = {0x3007, 0x004e00, 0x004e8c, 0x004e09, 0x0056d8, 
     		             0x004e94, 0x00516d, 0x4e03, 0x00516b, 0x004e5d}; 
    	int nondigits[] = {0x0010, 0x000041, 0x000122, 0x0068FE};
    
    	int digitvalues[] = {0, 2, 3, 5, 1};
    	int digitvalues2[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};     
    
    	int size  = digits.length;
    	for (int i = 0; i < size; i ++) 
      		if (UCharacter.isDigit(digits[i]) && 
          		UCharacter.digit(digits[i]) != digitvalues[i]) 
      		{
       		 	errln("FAIL \\u" + hex(digits[i]) + 
              			" expected digit with value " + digitvalues[i]);
        		break;
      		}
    
    	size = nondigits.length;
    	for (int i = 0; i < size; i ++)
      		if (UCharacter.isDigit(nondigits[i]))
      		{
        		errln("FAIL \\u" + hex(nondigits[i]) + " expected nondigit");
        		break;
      		}
      
    	size = digits2.length;
    	for (int i = 0; i < 10; i ++) 
      		if (UCharacter.isDigit(digits2[i]) &&
          		UCharacter.digit(digits2[i]) != digitvalues2[i]) 
      		{
        		errln("FAIL \\u" + hex(digits2[i]) + 
              		" expected digit with value " + digitvalues2[i]);
        		break;
      		}
  	}
  
  	/**
   	*  Tests for numeric characters
   	*/
  	public void TestNumeric()
  	{
		for (int i = '0'; i < '9'; i ++) {
			int n1 = UCharacter.getNumericValue(i);
			int n2 = UCharacter.getUnicodeNumericValue(i);
			if (n1 != n2 ||  n1 != (i - '0')) {
				errln("Numeric value of " + (char)i + " expected to be " +
				      (i - '0'));
			}
		}
		for (int i = 'A'; i < 'F'; i ++) {
			int n1 = UCharacter.getNumericValue(i);
			int n2 = UCharacter.getUnicodeNumericValue(i);
			if (n2 != -1 ||  n1 != (i - 'A' + 10)) {
				errln("Numeric value of " + (char)i + " expected to be " +
				      (i - 'A' + 10));
			}
		}
		for (int i = 0xFF21; i < 0xFF26; i ++) {
			// testing full wideth latin characters A-F
			int n1 = UCharacter.getNumericValue(i);
			int n2 = UCharacter.getUnicodeNumericValue(i);
			if (n2 != -1 ||  n1 != (i - 0xFF21 + 10)) {
				errln("Numeric value of " + (char)i + " expected to be " +
				      (i - 0xFF21 + 10));
			}
		}
		// testing han numbers
		int han[] = {0x96f6, 0, 0x58f9, 1, 0x8cb3, 2, 0x53c3, 3, 
			         0x8086, 4, 0x4f0d, 5, 0x9678, 6, 0x67d2, 7, 
			         0x634c, 8, 0x7396, 9, 0x5341, 10, 0x62fe, 10,
			         0x767e, 100, 0x4f70, 100, 0x5343, 1000, 0x4edf, 1000,
			         0x824c, 10000, 0x5104, 100000000};
		for (int i = 0; i < han.length; i += 2) {
			if (UCharacter.getHanNumericValue(han[i]) != han[i + 1]) {
				errln("Numeric value of \\u" + 
				      Integer.toHexString(han[i]) +	" expected to be " + 
				      han[i + 1]);
			}
		}
  	}

  	/**
  	* Tests for version 
  	*/
  	public void TestVersion()
  	{
    	if (!UCharacter.getUnicodeVersion().equals(VERSION_))
      		errln("FAIL expected " + VERSION_);
  	}
  
  	/**
  	* Tests for control characters
  	*/
  	public void TestISOControl()
  	{
    	int control[] = {0x001b, 0x000097, 0x000082};
    	int noncontrol[] = {0x61, 0x000031, 0x0000e2};
    
    	int size = control.length;
    	for (int i = 0; i < size; i ++) 
    	{
      		if (!UCharacter.isISOControl(control[i]))
      		{
        		errln("FAIL 0x" + Integer.toHexString(control[i]) + 
         	     		" expected to be a control character");
        		break;
      		}  
      		if (UCharacter.isISOControl(noncontrol[i]))
      		{
        		errln("FAIL 0x" + Integer.toHexString(noncontrol[i]) + 
              			" expected to be not a control character");
        		break;
      		}
      
      		logln("Ok    0x" + Integer.toHexString(control[i]) + " and 0x" +
            		Integer.toHexString(noncontrol[i]));
    	}
  	}
  	
  	/**
  	 * Test Supplementary
  	 */
  	public void TestSupplementary()
  	{
  		for (int i = 0; i < 0x10000; i ++) {
  			if (UCharacter.isSupplementary(i)) {
  				errln("Codepoint \\u" + Integer.toHexString(i) + 
  				      " is not supplementary");
  			}
  		}
  		for (int i = 0x10000; i < 0x10FFFF; i ++) {
  			if (!UCharacter.isSupplementary(i)) {
  				errln("Codepoint \\u" + Integer.toHexString(i) + 
  				      " is supplementary");
  			}
  		}
  	}
  	
  	/**
  	 * Test mirroring
  	 */
  	public void TestMirror()
  	{
  		if (!(UCharacter.isMirrored(0x28) && UCharacter.isMirrored(0xbb) && 
  		      UCharacter.isMirrored(0x2045) && UCharacter.isMirrored(0x232a)
  		      && !UCharacter.isMirrored(0x27) && 
  		      !UCharacter.isMirrored(0x61) && !UCharacter.isMirrored(0x284) 
  		      && !UCharacter.isMirrored(0x3400))) {
        	errln("isMirrored() does not work correctly");
    	}

    	if (!(UCharacter.getMirror(0x3c) == 0x3e && 
    	      UCharacter.getMirror(0x5d) == 0x5b && 
              UCharacter.getMirror(0x208d) == 0x208e && 
              UCharacter.getMirror(0x3017) == 0x3016 &&
         	  UCharacter.getMirror(0x2e) == 0x2e && 
         	  UCharacter.getMirror(0x6f3) == 0x6f3 && 
         	  UCharacter.getMirror(0x301c) == 0x301c && 
         	  UCharacter.getMirror(0xa4ab) == 0xa4ab)) {
        	errln("getMirror() does not work correctly");
    	}
	}
  
  	/**
  	* Tests for printable characters
  	*/
  	public void TestPrint()
  	{
    	int printable[] = {0x0042, 0x00005f, 0x002014};
    	int nonprintable[] = {0x200c, 0x00009f, 0x00001b};
    
    	int size = printable.length;
    	for (int i = 0; i < size; i ++)
    	{
      		if (!UCharacter.isPrintable(printable[i]))
      		{
       	 		errln("FAIL \\u" + hex(printable[i]) + 
              		" expected to be a printable character");
        		break;
      		}
      		if (UCharacter.isPrintable(nonprintable[i]))
      		{
        		errln("FAIL \\u" + hex(nonprintable[i]) +
              			" expected not to be a printable character");
        		break;
      		}
      		logln("Ok    \\u" + hex(printable[i]) + " and \\u" + 
            		hex(nonprintable[i]));
    	}
    
    	// test all ISO 8 controls
    	for (int ch = 0; ch <= 0x9f; ++ ch) {
      		if (ch == 0x20) {
      			// skip ASCII graphic characters and continue with DEL
        		ch = 0x7f;
      		}
      		if (UCharacter.isPrintable(ch)) {
        		errln("Fail \\u" + hex(ch) + 
              		" is a ISO 8 control character hence not printable\n");
      		}
    	}

    	/* test all Latin-1 graphic characters */
    	for (int ch = 0x20; ch <= 0xff; ++ ch) {
      		if (ch == 0x7f) {
        		ch = 0xa0;
      		}
      		if (!UCharacter.isPrintable(ch)) {
        		errln("Fail \\u" + hex(ch) + 
        		      " is a Latin-1 graphic character\n");
        	}
    	}
  	}
  
  	/** 
  	* Testing for identifier characters
  	*/
  	public void TestIdentifier()
  	{
    	int unicodeidstart[] = {0x0250, 0x0000e2, 0x000061};
    	int nonunicodeidstart[] = {0x2000, 0x00000a, 0x002019};
    	int unicodeidpart[] = {0x005f, 0x000032, 0x000045};
    	int nonunicodeidpart[] = {0x2030, 0x0000a3, 0x000020};
    	int idignore[] = {0x0006, 0x0010, 0x206b};
    	int nonidignore[] = {0x0075, 0x0000a3, 0x000061};
    
    	int size = unicodeidstart.length;
    	for (int i = 0; i < size; i ++) 
    	{
      		if (!UCharacter.isUnicodeIdentifierStart(unicodeidstart[i]))
      		{
        		errln("FAIL \\u" + hex(unicodeidstart[i]) + 
              		" expected to be a unicode identifier start character");
        		break;
      		}
      		if (UCharacter.isUnicodeIdentifierStart(nonunicodeidstart[i]))
      		{
        		errln("FAIL \\u" + hex(nonunicodeidstart[i]) + 
              			" expected not to be a unicode identifier start " + 
              			"character");
        		break;
      		}
      		if (!UCharacter.isUnicodeIdentifierPart(unicodeidpart[i]))
      		{
        		errln("FAIL \\u" + hex(unicodeidpart[i]) + 
              		" expected to be a unicode identifier part character");
        		break;
      		}
      		if (UCharacter.isUnicodeIdentifierPart(nonunicodeidpart[i]))
      		{
        		errln("FAIL \\u" + hex(nonunicodeidpart[i]) + 
              			" expected not to be a unicode identifier part " +
              			"character");
        		break;
      		}
            if (!UCharacter.isIdentifierIgnorable(idignore[i]))
      		{
        		errln("FAIL \\u" + hex(idignore[i]) + 
              			" expected to be a ignorable unicode character");
        		break;
      		}
      		if (UCharacter.isIdentifierIgnorable(nonidignore[i]))
      		{
        		errln("FAIL \\u" + hex(nonidignore[i]) + 
              		" expected not to be a ignorable unicode character");
        		break;
      		}
      		logln("Ok    \\u" + hex(unicodeidstart[i]) + " and \\u" +
            		hex(nonunicodeidstart[i]) + " and \\u" + 
            		hex(unicodeidpart[i]) + " and \\u" + 
            		hex(nonunicodeidpart[i]) + " and \\u" +
            		hex(idignore[i]) + " and \\u" + hex(nonidignore[i]));
    	}
  	}
  
  	/**
  	* Tests for the character types, direction.<br>
  	* This method reads in UnicodeData.txt file for testing purposes. A 
  	* default path is provided relative to the src path, however the user 
  	* could set a system property to change the directory path.<br>
  	* e.g. java -DUnicodeData="data_directory_path" 
  	* com.ibm.icu.dev.test.lang.UCharacterTest
  	*/
  	public void TestUnicodeData()
  	{
    	// this is the 2 char category types used in the UnicodeData file
    	final String TYPE = 
      		"LuLlLtLmLoMnMeMcNdNlNoZsZlZpCcCfCoCsPdPsPePcPoSmScSkSoPiPf";
    
    	// directory types used in the UnicodeData file
    	// padded by spaces to make each type size 4
    	final String DIR = 
      		"L   R   EN  ES  ET  AN  CS  B   S   WS  ON  LRE LRO AL  RLE RLO PDF NSM BN  ";
      
    	final int LASTUNICODECHAR = 0xFFFD;
    	int ch = 0,
        	index = 0,
        	type = 0,
        	dir = 0;
	
		try
		{
	  		BufferedReader input = TestUtil.getDataReader(
	  		                                    "unicode/UnicodeData.txt");
	    
      		while (ch != LASTUNICODECHAR)
      		{
       	 		String s = input.readLine();
        		// geting the unicode character, its type and its direction
        		ch = Integer.parseInt(s.substring(0, 4), 16);
        		index = s.indexOf(';', 5);
        		String t = s.substring(index + 1, index + 3);
        		index += 4;
        		int cc = Integer.parseInt(s.substring(index, s.indexOf(';', 
        		                                      index)));
        		index = s.indexOf(';', index);
        		String d = s.substring(index + 1, s.indexOf(';', index + 1));
        
        		// testing the category
        		// we override the general category of some control 
        		// characters
        		type = TYPE.indexOf(t);
        		if (type < 0)
            		type = 0;
        		else 
            		type = (type >> 1) + 1;  
        		if (UCharacter.getType(ch) != type)
        		{
          			errln("FAIL \\u" + hex(ch) + " expected type " + 
                			type);
          			break;
        		}
        
        		// testing combining class
        		if (UCharacter.getCombiningClass(ch) != cc)
        		{
          			errln("FAIL \\u" + hex(ch) + " expected combining " +
                			"class " + cc);
          			break;
        		}
        
        		// testing the direction
        		if (d.length() == 1)
          			d = d + "   ";  
          
        		dir = DIR.indexOf(d) >> 2;
        		if (UCharacter.getDirection(ch) != dir) 
        		{
          			errln("FAIL \\u" + hex(ch) + 
                		" expected wrong direction " + dir);
          			break;
        		}
      		}
      		input.close();
    	}
    	catch (Exception e)
    	{
      		e.printStackTrace();
    	}
    
    	if (UCharacter.getDirection(0x10001) != 
                                  UCharacterDirection.BOUNDARY_NEUTRAL) {
  			errln("FAIL 0x10001 expected direction " + 
          UCharacterDirection.toString(UCharacterDirection.BOUNDARY_NEUTRAL));
    	}
  	}
  
  
  	/**
  	* Test for the character names
  	*/
  	public void TestNames()
  	{
    	int c[] = {0x0061, 0x000284, 0x003401, 0x007fed, 0x00ac00, 0x00d7a3, 
    		       0x00d800, 0x00dc00, 0xff08, 0x00ffe5, 0x00ffff, 
    		       0x0023456};
    	String name[] = {"LATIN SMALL LETTER A", 
                     "LATIN SMALL LETTER DOTLESS J WITH STROKE AND HOOK", 
                     "CJK UNIFIED IDEOGRAPH-3401", 
                     "CJK UNIFIED IDEOGRAPH-7FED", "HANGUL SYLLABLE GA", 
                     "HANGUL SYLLABLE HIH", "", "",
                     "FULLWIDTH LEFT PARENTHESIS",
                     "FULLWIDTH YEN SIGN", "", "CJK UNIFIED IDEOGRAPH-23456"};
    	String oldname[] = {"", "LATIN SMALL LETTER DOTLESS J BAR HOOK", "", 
    		            "",
                        "", "", "", "", "FULLWIDTH OPENING PARENTHESIS", "", 
                        "", ""};
    	String extendedname[] = {"LATIN SMALL LETTER A", 
                             "LATIN SMALL LETTER DOTLESS J WITH STROKE AND HOOK",
                             "CJK UNIFIED IDEOGRAPH-3401",
                             "CJK UNIFIED IDEOGRAPH-7FED",
                             "HANGUL SYLLABLE GA",
                             "HANGUL SYLLABLE HIH",
                             "<lead surrogate-D800>",
                             "<trail surrogate-DC00>",
                             "FULLWIDTH LEFT PARENTHESIS",
                             "FULLWIDTH YEN SIGN",
                             "<noncharacter-FFFF>",
                             "CJK UNIFIED IDEOGRAPH-23456"};
                             
    	int size = c.length;
    	String str;
    	int uc;
    
    	for (int i = 0; i < size; i ++) 
    	{
      		// modern Unicode character name
      		str = UCharacter.getName(c[i]);
      		if ((str == null && name[i].length() > 0) || 
          		(str != null && !str.equals(name[i])))
      		{
        		errln("FAIL \\u" + hex(c[i]) + " expected name " +
              			name[i]);
        		break;
      		}
     
      		// 1.0 Unicode character name
      		str = UCharacter.getName1_0(c[i]);
      		if ((str == null && oldname[i].length() > 0) || 
          		(str != null && !str.equals(oldname[i])))
      		{
        		errln("FAIL \\u" + hex(c[i]) + " expected 1.0 name " +
              			oldname[i]);
        		break;
      		}
      
      		// extended character name
      		str = UCharacter.getExtendedName(c[i]);
      		if (str == null || !str.equals(extendedname[i]))
      		{
        		errln("FAIL \\u" + hex(c[i]) + " expected extended name " +
              			extendedname[i]);
        		break;
      		}
      
      		// retrieving unicode character from modern name
      		uc = UCharacter.getCharFromName(name[i]);
      		if (uc != c[i] && name[i].length() != 0)
      		{
        		errln("FAIL " + name[i] + " expected character \\u" + 
        		      hex(c[i]));
        		break;
      		}
      
      		//retrieving unicode character from 1.0 name
      		uc = UCharacter.getCharFromName1_0(oldname[i]);
      		if (uc != c[i] && oldname[i].length() != 0)
      		{
        		errln("FAIL " + oldname[i] + " expected 1.0 character \\u" + 
        		      hex(c[i]));
        		break;
      		}
      
      		//retrieving unicode character from 1.0 name
      		uc = UCharacter.getCharFromExtendedName(extendedname[i]);
      		if (uc != c[i] && i != 0 && (i == 1 || i == 6))
      		{
        		errln("FAIL " + extendedname[i] + 
        		      " expected extended character \\u" + hex(c[i]));
        		break;
      		}
    	}
    
    	// test getName works with mixed-case names (new in 2.0)
    	if (0x61 != UCharacter.getCharFromName("LATin smALl letTER A")) { 
        	errln("FAIL: 'LATin smALl letTER A' should result in character "
        	      + "U+0061"); 
    	} 
	    
    	// extra testing different from icu
    	for (int i = UCharacter.MIN_VALUE; i < UCharacter.MAX_VALUE; i ++)
    	{
      		str = UCharacter.getName(i);
      		if (str != null && UCharacter.getCharFromName(str) != i)
      		{	
        		errln("FAIL \\u" + hex(i) + " " + str  + 
              						" retrieval of name and vice versa" );
        		break;
      		}
    	}
  	}
  
  	/**
   	* Testing name iteration
   	*/
  	public void TestNameIteration()
  	{
  		ValueIterator iterator = UCharacter.getExtendedNameIterator();
  		ValueIterator.Element element = new ValueIterator.Element();
   	 	ValueIterator.Element old     = new ValueIterator.Element();
    	// testing subrange
    	iterator.setRange(-10, -5);
    	if (iterator.next(element)) {
    		errln("Fail, expected iterator to return false when range is set outside the meaningful range");
    	}
    	iterator.setRange(0x110000, 0x111111);
    	if (iterator.next(element)) {
    		errln("Fail, expected iterator to return false when range is set outside the meaningful range");
    	}
    	try {
    		iterator.setRange(50, 10);
    		errln("Fail, expected exception when encountered invalid range");
    	} catch (Exception e) {
    	}
    		
    	iterator.setRange(-10, 10);
    	if (!iterator.next(element) || element.integer != 0) {
    		errln("Fail, expected iterator to return 0 when range start limit is set outside the meaningful range");
    	}
    	
    	iterator.setRange(0x10FFFE, 0x200000);
    	int last = 0;
    	while (iterator.next(element)) {
    		last = element.integer;
    	}
    	if (last != 0x10FFFF) {
    		errln("Fail, expected iterator to return 0x10FFFF when range end limit is set outside the meaningful range");
    	}
    	
    	iterator = UCharacter.getNameIterator();	
 		iterator.setRange(0xF, 0x45);
 		while (iterator.next(element)) {
    		if (element.integer <= old.integer) {
         		errln("FAIL next returned a less codepoint \\u" + 
         	      	Integer.toHexString(element.integer) + " than \\u" + 
         	      	Integer.toHexString(old.integer));
         		break;
        	}
        	if (!UCharacter.getName(element.integer).equals(element.value)) 
        	{
         		errln("FAIL next codepoint \\u" + 
         	      	Integer.toHexString(element.integer) + 
         	      	" does not have the expected name " + 
         	      	UCharacter.getName(element.integer) + 
         	      	" instead have the name " + (String)element.value);
         		break;
        	}
        	old.integer = element.integer; 
    	}
    
    	iterator.reset();
    	iterator.next(element);
    	if (element.integer != 0x20) {
    		errln("FAIL reset in iterator");
    	}
 
    	iterator.setRange(0, 0x110000);
    	old.integer = 0; 
    	while (iterator.next(element)) {
    		if (element.integer != 0 && element.integer <= old.integer) {
         		errln("FAIL next returned a less codepoint \\u" + 
         	      	Integer.toHexString(element.integer) + " than \\u" + 
         	      	Integer.toHexString(old.integer));
         		break;
        	}
        	if (!UCharacter.getName(element.integer).equals(element.value)) 
        	{
         		errln("FAIL next codepoint \\u" + 
         	      		Integer.toHexString(element.integer) + 
         	      		" does not have the expected name " + 
         	      		UCharacter.getName(element.integer) + 
         	      		" instead have the name " + (String)element.value);
         		break;
        	}
        	for (int i = old.integer + 1; i < element.integer; i ++) {
        		if (UCharacter.getName(i) != null) {
         			errln("FAIL between codepoints are not null \\u" + 
         	      			Integer.toHexString(old.integer) + " and " + 
         	      			Integer.toHexString(element.integer) + " has " + 
         	      			Integer.toHexString(i) + " with a name " + 
         	      			UCharacter.getName(i));
         			break;
        		}
        	}
        	old.integer = element.integer; 
    	}
    
    	iterator = UCharacter.getExtendedNameIterator();
    	old.integer = 0;
    	while (iterator.next(element)) {
    		if (element.integer != 0 && element.integer != old.integer) {
         		errln("FAIL next returned a codepoint \\u" + 
         	      		Integer.toHexString(element.integer) + 
         	      		" different from \\u" + 
         	      		Integer.toHexString(old.integer));
         		break;
        	}
        	if (!UCharacter.getExtendedName(element.integer).equals(
                                                          element.value)) {
         		errln("FAIL next codepoint \\u" + 
         	      	Integer.toHexString(element.integer) + 
         	      	" name should be "
         	      	+ UCharacter.getExtendedName(element.integer) + 
         	      	" instead of " + (String)element.value);
         		break;
        	}
        	old.integer++; 
    	}
		iterator = UCharacter.getName1_0Iterator();
 	   	old.integer = 0;
    	while (iterator.next(element)) {
    		logln(Integer.toHexString(element.integer) + " " +
	      											(String)element.value);
    		if (element.integer != 0 && element.integer <= old.integer) {
         		errln("FAIL next returned a less codepoint \\u" + 
         	      	Integer.toHexString(element.integer) + " than \\u" + 
         	      	Integer.toHexString(old.integer));
         		break;
        	}
        	if (!element.value.equals(UCharacter.getName1_0(
        	                                            element.integer))) {
         		errln("FAIL next codepoint \\u" + 
         	      		Integer.toHexString(element.integer) + 
         	      		" name cannot be null");
         		break;
        	}
        	for (int i = old.integer + 1; i < element.integer; i ++) {
        		if (UCharacter.getName1_0(i) != null) {
         			errln("FAIL between codepoints are not null \\u" + 
         	      		Integer.toHexString(old.integer) + " and " + 
         	      		Integer.toHexString(element.integer) + " has " + 
         	      		Integer.toHexString(i) + " with a name " + 
         	      		UCharacter.getName1_0(i));
         			break;
        		}
        	}
        	old.integer = element.integer; 
    	}
  	}
  
  	/**
  	* Testing the for illegal characters
  	*/
  	public void TestIsLegal() 
  	{
    	int illegal[] = {0xFFFE, 0x00FFFF, 0x005FFFE, 0x005FFFF, 0x0010FFFE,
    		             0x0010FFFF, 0x110000, 0x00FDD0, 0x00FDDF, 0x00FDE0,
    		             0x00FDEF, 0xD800, 0xDC00, -1};
    	int legal[] = {0x61, 0x00FFFD, 0x0010000, 0x005FFFD, 0x0060000, 
    		           0x0010FFFD, 0xFDCF, 0x00FDF0};
    	for (int count = 0; count < illegal.length; count ++) {
        	if (UCharacter.isLegal(illegal[count])) {
            	errln("FAIL \\u" + hex(illegal[count]) + 
                    	" is not a legal character");
        	}
    	}
    
    	for (int count = 0; count < legal.length; count ++) {
        	if (!UCharacter.isLegal(legal[count])) {
            	errln("FAIL \\u" + hex(legal[count]) + 
            	                                   " is a legal character");
        	}
    	}
    	
    	String illegalStr = "This is an illegal string ";
    	String legalStr = "This is a legal string ";
    	
    	for (int count = 0; count < illegal.length; count ++) {
    		StringBuffer str = new StringBuffer(illegalStr);
    		if (illegal[count] < 0x10000) {
    			str.append((char)illegal[count]);
    		}
    		else {
    			char lead = UTF16.getLeadSurrogate(illegal[count]);
    			char trail = UTF16.getTrailSurrogate(illegal[count]);
    			str.append(lead);
    			str.append(trail);
    		}
        	if (UCharacter.isLegal(str.toString())) {
            	errln("FAIL " + hex(str.toString()) + 
            	      " is not a legal string");
        	}
    	}
    
    	for (int count = 0; count < legal.length; count ++) {
    		StringBuffer str = new StringBuffer(legalStr);
    		if (legal[count] < 0x10000) {
    			str.append((char)legal[count]);
    		}
    		else {
    			char lead = UTF16.getLeadSurrogate(legal[count]);
    			char trail = UTF16.getTrailSurrogate(legal[count]);
    			str.append(lead);
    			str.append(trail);
    		}
        	if (!UCharacter.isLegal(str.toString())) {
            	errln("FAIL " + hex(str.toString()) + " is a legal string");
        	}
    	}
  	}
  
  	/**
  	 * Test getCodePoint
  	 */
  	public void TestCodePoint()
  	{
  		int ch = 0x10000;
  		for (char i = 0xD800; i < 0xDC00; i ++) {
  			for (char j = 0xDC00; j <= 0xDFFF; j ++) {
  				if (UCharacter.getCodePoint(i, j) != ch) {
  					errln("Error getting codepoint for surrogate " +
  					      "characters \\u"
  					      + Integer.toHexString(i) + " \\u" + 
  					      Integer.toHexString(j));	      
  				}
  				ch ++;
  			}
  		}
  		try 
  		{
  			UCharacter.getCodePoint((char)0xD7ff, (char)0xDC00);
  			errln("Invalid surrogate characters should not form a " +
  			      "supplementary");
  		} catch(Exception e) {
  		}
  		for (char i = 0; i < 0xFFFF; i++) {
  			if (i == 0xFFFE || 
  			    (i >= 0xD800 && i <= 0xDFFF) || 
  			    (i >= 0xFDD0 && i <= 0xFDEF)) {
  			    // not a character
  				try {
  					UCharacter.getCodePoint(i);
  					errln("Not a character is not a valid codepoint");
  				} catch (Exception e) {
  				}
  			}
  			else {
  				if (UCharacter.getCodePoint(i) != i) {
  					errln("A valid codepoint should return itself");
  				}
  			}
  		}
  	}
  	
  	/**
  	* This method is alittle different from the type test in icu4c.
  	* But combined with testUnicodeData, they basically do the same thing.
  	*/
  	public void TestIteration() 
  	{
      	int limit     = 0;
      	int prevtype  = -1;
      	RangeValueIterator iterator = UCharacter.getTypeIterator();
      	RangeValueIterator.Element result = new RangeValueIterator.Element();
      	while (iterator.next(result)) {
          	if (result.start != limit) {
              	errln("UCharacterIteration failed: Ranges not continuous " + 
                    	"0x" + Integer.toHexString(result.start));
          	}
          
          	limit = result.limit;
          	if (result.value == prevtype) {
              	errln("Type of the next set of enumeration should be different");
          	}
          	prevtype = result.value;
          
		  	for (int i = result.start; i < limit; i ++) {
              	int temptype = UCharacter.getType(i);
              	if (temptype != result.value) {
                  	errln("UCharacterIteration failed: Codepoint \\u" + 
                        	Integer.toHexString(i) + " should be of type " +
                        	temptype + " not " + result.value);
              	}
          	}
      	}
      
      	iterator.reset();
      	if (iterator.next(result) == false || result.start != 0) {
          	System.out.println("result " + result.start);
          	errln("UCharacterIteration reset() failed");
      	}
  	}
  
  	/**
  	 * Testing getAge
   	 */
  	public void TestGetAge() 
  	{
  		int ages[] = {0x41,    1, 1, 0, 0,
					  0xffff,  1, 1, 0, 0,
                      0x20ab,  2, 0, 0, 0,
                      0x2fffe, 2, 0, 0, 0,
                      0x20ac,  2, 1, 0, 0,
        			  0xfb1d,  3, 0, 0, 0,
        			  0x3f4,   3, 1, 0, 0,
        			  0x10300, 3, 1, 0, 0,
        			  0x220,   3, 2, 0, 0,
        			  0xff60,  3, 2, 0, 0};
  		for (int i = 0; i < ages.length; i += 5) {
  			VersionInfo age = UCharacter.getAge(ages[i]);
        	if (age != VersionInfo.getInstance(ages[i + 1], ages[i + 2],
        	                                   ages[i + 3], ages[i + 4])) {
            	errln("error: getAge(\\u" + Integer.toHexString(ages[i]) +
            	      ") == " + age.toString() + " instead of " + 
					  ages[i + 1] + "." + ages[i + 2] + "." + ages[i + 3] + 
					  "." + ages[i + 4]);
        	}
        }
	}
  
  	/**
  	 * Test binary non core properties
  	 */
  	public void TestAdditionalProperties() 
  	{
    	// test data for hasBinaryProperty()
    	int props[][] = { // code point, property
	        { 0x0627, UProperty.ALPHABETIC },
	        { 0x1034a, UProperty.ALPHABETIC },
	        { 0x2028, UProperty.ALPHABETIC },
	
	        { 0x0066, UProperty.ASCII_HEX_DIGIT },
	        { 0x0067, UProperty.ASCII_HEX_DIGIT },
	
	        { 0x202c, UProperty.BIDI_CONTROL },
	        { 0x202f, UProperty.BIDI_CONTROL },
	
	        { 0x003c, UProperty.BIDI_MIRRORED },
	        { 0x003d, UProperty.BIDI_MIRRORED },
	
	        { 0x058a, UProperty.DASH },
	        { 0x007e, UProperty.DASH },
	
	        { 0x0c4d, UProperty.DIACRITIC },
	        { 0x3000, UProperty.DIACRITIC },
	
	        { 0x0e46, UProperty.EXTENDER },
	        { 0x0020, UProperty.EXTENDER },
	
	        { 0xfb1d, UProperty.FULL_COMPOSITION_EXCLUSION },
	        { 0x1d15f, UProperty.FULL_COMPOSITION_EXCLUSION },
	        { 0xfb1e, UProperty.FULL_COMPOSITION_EXCLUSION },
	
	        { 0x0044, UProperty.HEX_DIGIT },
	        { 0xff46, UProperty.HEX_DIGIT },
	        { 0x0047, UProperty.HEX_DIGIT },
	
	        { 0x30fb, UProperty.HYPHEN },
	        { 0xfe58, UProperty.HYPHEN },
	
	        { 0x2172, UProperty.ID_CONTINUE },
	        { 0x0307, UProperty.ID_CONTINUE },
	        { 0x005c, UProperty.ID_CONTINUE },
	
	        { 0x2172, UProperty.ID_START },
	        { 0x007a, UProperty.ID_START },
	        { 0x0039, UProperty.ID_START },
	
	        { 0x4db5, UProperty.IDEOGRAPHIC },
	        { 0x2f999, UProperty.IDEOGRAPHIC },
	        { 0x2f99, UProperty.IDEOGRAPHIC },
	
	        { 0x200c, UProperty.JOIN_CONTROL },
	        { 0x2029, UProperty.JOIN_CONTROL },
	
	        { 0x1d7bc, UProperty.LOWERCASE },
	        { 0x0345, UProperty.LOWERCASE },
	        { 0x0030, UProperty.LOWERCASE },
	
	        { 0x1d7a9, UProperty.MATH },
	        { 0x2135, UProperty.MATH },
	        { 0x0062, UProperty.MATH },
	
	        { 0xfde1, UProperty.NONCHARACTER_CODE_POINT },
	        { 0x10ffff, UProperty.NONCHARACTER_CODE_POINT },
	        { 0x10fffd, UProperty.NONCHARACTER_CODE_POINT },
	
	        { 0x0022, UProperty.QUOTATION_MARK },
	        { 0xff62, UProperty.QUOTATION_MARK },
	        { 0xd840, UProperty.QUOTATION_MARK },
	
	        { 0x061f, UProperty.TERMINAL_PUNCTUATION },
	        { 0xe003f, UProperty.TERMINAL_PUNCTUATION },
	
	        { 0x1d44a, UProperty.UPPERCASE },
	        { 0x2162, UProperty.UPPERCASE },
	        { 0x0345, UProperty.UPPERCASE },
	
	        { 0x0020, UProperty.WHITE_SPACE },
	        { 0x202f, UProperty.WHITE_SPACE },
	        { 0x3001, UProperty.WHITE_SPACE },
	
	        { 0x0711, UProperty.XID_CONTINUE },
	        { 0x1d1aa, UProperty.XID_CONTINUE },
	        { 0x007c, UProperty.XID_CONTINUE },
	
	        { 0x16ee, UProperty.XID_START },
	        { 0x23456, UProperty.XID_START },
	        { 0x1d1aa, UProperty.XID_START },
	
	        // Version break:
	        // The following properties are only supported starting with the
	        // UProperty.Unicode version indicated in the second field.
	         
	        { -1, 0x32, 0 },
	
	        { 0x180c, UProperty.DEFAULT_IGNORABLE_CODE_POINT },
	        { 0xfe02, UProperty.DEFAULT_IGNORABLE_CODE_POINT },
	        { 0x1801, UProperty.DEFAULT_IGNORABLE_CODE_POINT },
	
	        { 0x0341, UProperty.DEPRECATED },
	        { 0xe0041, UProperty.DEPRECATED },
	
	        { 0x00a0, UProperty.GRAPHEME_BASE },
	        { 0x0a4d, UProperty.GRAPHEME_BASE },
	        { 0xff9f, UProperty.GRAPHEME_BASE },
	
	        { 0x0300, UProperty.GRAPHEME_EXTEND },
	        { 0xff9f, UProperty.GRAPHEME_EXTEND },
	        { 0x0a4d, UProperty.GRAPHEME_EXTEND },
	
	        { 0x0a4d, UProperty.GRAPHEME_LINK },
	        { 0xff9f, UProperty.GRAPHEME_LINK },
	
	        { 0x2ff7, UProperty.IDS_BINARY_OPERATOR },
	        { 0x2ff3, UProperty.IDS_BINARY_OPERATOR },
	
	        { 0x2ff3, UProperty.IDS_TRINARY_OPERATOR },
	        { 0x2f03, UProperty.IDS_TRINARY_OPERATOR },
	
	        { 0x0ec1, UProperty.LOGICAL_ORDER_EXCEPTION },
	        { 0xdcba, UProperty.LOGICAL_ORDER_EXCEPTION },
	
	        { 0x2e9b, UProperty.RADICAL },
	        { 0x4e00, UProperty.RADICAL },
	
	        { 0x012f, UProperty.SOFT_DOTTED },
	        { 0x0049, UProperty.SOFT_DOTTED },
	
	        { 0xfa11, UProperty.UNIFIED_IDEOGRAPH },
	        { 0xfa12, UProperty.UNIFIED_IDEOGRAPH }
	    };
	    
	    boolean expected[] = { true, true, false, true, false, 
	    	                   true, false, true, false, true, 
	    	                   false, true, false, true, false, 
	    	                   true, true, false, true, true, 
	    	                   false, true, false, true, true, 
	    	                   false, true, true, false, true, 
	    	                   true, false, true, false, true, 
	    	                   true, false, true, true, false, 
	    	                   true, true, false, true, true, 
	    	                   false, true, false, true, true, 
	    	                   false, true, true, false, true, 
	    	                   true, false, true, true, false,
	    	                   false, true, true, false, true, 
	    	                   false, true, false, false, true, 
	    	                   true, false, true, false, true, 
	    	                   false, true, false, true, false, 
	    	                   true, false, true, false, true, 
	    	                   false};

	    VersionInfo version = UCharacter.getUnicodeVersion();
	    
	    // test hasBinaryProperty() 
	    for (int i = 0; i < props.length; ++ i) {
	    	if (props[i][0] < 0) {
				if (version.compareTo(VersionInfo.getInstance(props[i][1] >> 4, 
	    	                                              props[i][1] & 0xF, 
	    	                                              0, 0)) < 0) {
	    	    	break;
	    	    }
	    	    continue;
	        }
            if (UCharacter.hasBinaryProperty(props[i][0], props[i][1])
	            != expected[i]) {
	            errln("error: UCharacter.hasBinaryProperty(\\u" + 
	                  Integer.toHexString(props[i][0]) + ", " + 
	                  Integer.toHexString(props[i][1]) + ") has an error expected " +
	                  expected[i]);
	        }
	
	        // test separate functions, too 
	        switch (props[i][1]) {
	        case UProperty.ALPHABETIC:
	            if (UCharacter.isUAlphabetic(props[i][0]) != expected[i]) {
	                errln("error: UCharacter.isUAlphabetic(\\u" + 
	                      Integer.toHexString(props[i][0]) + 
	                      ") is wrong expected " + expected[i]);
	            }
	            break;
	        case UProperty.LOWERCASE:
	            if (UCharacter.isULowercase(props[i][0]) != expected[i]) {
	                errln("error: UCharacter.isULowercase(\\u" + 
	                      Integer.toHexString(props[i][0]) + 
	                      ") is wrong expected " + expected[i]);
	            }
	            break;
	        case UProperty.UPPERCASE:
	            if (UCharacter.isUUppercase(props[i][0]) != expected[i]) {
	                errln("error: UCharacter.isUUppercase(\\u" + 
	                      Integer.toHexString(props[i][0]) + 
	                      ") is wrong expected " + expected[i]);
	            }
	            break;
	        case UProperty.WHITE_SPACE:
	            if (UCharacter.isUWhiteSpace(props[i][0]) != expected[i]) {
	                errln("error: UCharacter.isUWhiteSpace(\\u" + 
	                      Integer.toHexString(props[i][0]) + 
	                      ") is wrong expected " + expected[i]);
	            }
	            break;
	        default:
	            break;
	        }
	    }
    }
}

