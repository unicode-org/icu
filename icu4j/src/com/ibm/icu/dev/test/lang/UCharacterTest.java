/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/lang/UCharacterTest.java,v $ 
* $Date: 2002/03/08 02:03:16 $ 
* $Revision: 1.31 $
*
*******************************************************************************
*/

package com.ibm.icu.dev.test.lang;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.io.File;
import java.util.Vector;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UCharacterDirection;
import com.ibm.icu.util.RangeValueIterator;
import com.ibm.icu.util.ValueIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.BreakIterator;

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
  private final String VERSION_ = "3.1.1.0";
  
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
      UCharacter.getName1_0(0x1d18b);
      test.TestNameIteration();
      //test.run(arg);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  /**
  * Testing the uppercase and lowercase function of UCharacter
  */
  public void TestUpperLowercharacter()
  {
    // variables to test the uppercase and lowercase characters
    int upper[] = {0x41, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047, 0x00b1, 0x00b2, 
                   0xb3, 0x0048, 0x0049, 0x004a, 0x002e, 0x003f, 0x003a, 0x004b, 0x004c,
                   0x4d, 0x004e, 0x004f, 0x01c4, 0x01c8, 0x000c, 0x0000};
    int lower[] = {0x61, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067, 0x00b1, 0x00b2, 
                   0xb3, 0x0068, 0x0069, 0x006a, 0x002e, 0x003f, 0x003a, 0x006b, 0x006c,
                   0x6d, 0x006e, 0x006f, 0x01c6, 0x01c9, 0x000c, 0x0000};
        
    int size = upper.length;
    
    for (int i = 0; i < size; i ++) 
    {
      if (UCharacter.isLetter(lower[i]) && !UCharacter.isLowerCase(lower[i]))
      {
        errln("FAIL isLowerCase test for \\u" + hex(lower[i]));
        break;
      }
      if (UCharacter.isLetter(upper[i]) && !(UCharacter.isUpperCase(upper[i]) 
                                       || UCharacter.isTitleCase(upper[i])))     
      {
        errln("FAIL isUpperCase test for \\u" + hex(upper[i]));
        break;
      }
      if (lower[i] != UCharacter.toLowerCase(upper[i]) || 
          (upper[i] != UCharacter.toUpperCase(lower[i]) &&
            upper[i] != UCharacter.toTitleCase(lower[i])))
      {
        errln("FAIL case conversion test for \\u" + hex(upper[i]) + " to \\u" 
              + hex(lower[i]));
        break;
      }
      if (lower[i] != UCharacter.toLowerCase(lower[i]))
      {
        errln("FAIL lower case conversion test for \\u" + hex(lower[i]));
        break;
      }
      if (upper[i] != UCharacter.toUpperCase(upper[i]) && 
          upper[i] != UCharacter.toTitleCase(upper[i]))
      {
        errln("FAIL upper case conversion test for \\u" + hex(upper[i]));
        break;
      }
      logln("Ok    \\u" + hex(upper[i]) + " and \\u" + hex(lower[i]));
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
      logln("Ok    \\u" + hex(spaces[i]) + " and \\u" + hex(nonspaces[i]) + 
            " and \\u" + hex(whitespaces[i]) + " and \\u" +
            hex(nonwhitespaces[i]));
    }
  }
  
  /**
  * Tests for defined and undefined characters
  */
  public void TestDefined()
  {
    int undefined[] = {0xfff1, 0x00fff7, 0x00fa30};
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
        errln("FAIL \\u" + hex(base[i]) + " expected to be a base character");
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
    int digits2[] = {0x3007, 0x004e00, 0x004e8c, 0x004e09, 0x0056d8, 0x004e94, 0x00516d, 
                     0x4e03, 0x00516b, 0x004e5d}; 
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
  * Tests for version 
  */
  public void TestVersion()
  {
    String version = UCharacter.getUnicodeVersion();
    if (!version.equals(VERSION_))
      errln("FAIL expected " + VERSION_);
  }
  
  /**
  * Tests for control characters
  */
  /* isControl is deprecated
  public void TestControl()
  {
    int control[] = {0x001b, 0x000097, 0x000082};
    int noncontrol[] = {0x61, 0x000031, 0x0000e2};
    
    int size = control.length;
    for (int i = 0; i < size; i ++) 
    {
      if (!UCharacter.isControl(control[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(control[i]) + 
              " expected to be a control character");
        break;
      }  
      if (UCharacter.isControl(noncontrol[i]))
      {
        errln("FAIL 0x" + Integer.toHexString(noncontrol[i]) + 
              " expected to be not a control character");
        break;
      }
      
      logln("Ok    0x" + Integer.toHexString(control[i]) + " and 0x" +
            Integer.toHexString(noncontrol[i]));
    }
  }
  */
  
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
        errln("Fail \\u" + hex(ch) + " is a Latin-1 graphic character\n");
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
    int idignore[] = {0x070F, 0x00180B, 0x00180C};
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
              " expected not to be a unicode identifier start character");
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
              " expected not to be a unicode identifier part character");
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
            hex(nonunicodeidstart[i]) + " and \\u" + hex(unicodeidpart[i]) + 
            " and \\u" + hex(nonunicodeidpart[i]) + " and \\u" +
            hex(idignore[i]) + " and \\u" + hex(nonidignore[i]));
    }
  }
  
  /**
  * Tests for the character types, direction.<br>
  * This method reads in UnicodeData.txt file for testing purposes. A default 
  * path is provided relative to the src path, however the user could 
  * set a system property to change the directory path.<br>
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
      
    // default unicode data file name
    String UNICODE_DATA_FILE = "/src/com/ibm/icu/dev/data/unicode/UnicodeData.txt";
	UNICODE_DATA_FILE.replace('/', File.pathSeparatorChar);    
    // unicode data file path system name
    final String UNICODE_DATA_SYSTEM_NAME = "UnicodeData";
    String s = System.getProperty(UNICODE_DATA_SYSTEM_NAME);
    if (s == null) {
    // assuming runtime directory is on the same level as the source
      s = System.getProperty("user.dir") + UNICODE_DATA_FILE;
    }
    else {
      StringBuffer tempfilename = new StringBuffer(s);
      if (tempfilename.charAt(tempfilename.length() - 1) != 
          File.pathSeparatorChar) {
        tempfilename.append(File.separatorChar);
      }
      tempfilename.append("UnicodeData.txt");
      s = tempfilename.toString();
    }
    
    final int LASTUNICODECHAR = 0xFFFD;
    int ch = 0,
        index = 0,
        type = 0,
        dir = 0;
	
	try
	{
	    // reading in the UnicodeData file
	  FileReader fr = new FileReader(s);
	  BufferedReader input = new BufferedReader(fr);
	    
      while (ch != LASTUNICODECHAR)
      {
        s= input.readLine();
        
        // geting the unicode character, its type and its direction
        ch = Integer.parseInt(s.substring(0, 4), 16);
        index = s.indexOf(';', 5);
        String t = s.substring(index + 1, index + 3);
        index += 4;
        int cc = Integer.parseInt(s.substring(index, s.indexOf(';', index)));
        index = s.indexOf(';', index);
        String d = s.substring(index + 1, s.indexOf(';', index + 1));
        
        // testing the category
        // we override the general category of some control characters
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
    catch (FileNotFoundException e)
    {
      errln("FAIL UnicodeData.txt not found. File name with path: " + s +
            "\nConfigure the system setting UnicodeData to the right path\n" +
            "e.g. java -DUnicodeData=\"data_dir_path\" " +
            "com.ibm.icu.dev.test.lang.UCharacterTest");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    
    if (UCharacter.getDirection(0x10001) != 
                                         UCharacterDirection.LEFT_TO_RIGHT) 
      errln("FAIL 0x10001 expected direction " + 
      UCharacterDirection.toString(UCharacterDirection.LEFT_TO_RIGHT));
  }
  
  
  /**
  * Test for the character names
  */
  public void TestNames()
  {
    int c[] = {0x0061, 0x000284, 0x003401, 0x007fed, 0x00ac00, 0x00d7a3, 0x00d800, 0x00dc00, 
               0xff08, 0x00ffe5, 0x00ffff, 0x0023456, 0x009};
    String name[] = {"LATIN SMALL LETTER A", 
                     "LATIN SMALL LETTER DOTLESS J WITH STROKE AND HOOK", 
                     "CJK UNIFIED IDEOGRAPH-3401", 
                     "CJK UNIFIED IDEOGRAPH-7FED", "HANGUL SYLLABLE GA", 
                     "HANGUL SYLLABLE HIH", "", "",
                     "FULLWIDTH LEFT PARENTHESIS",
                     "FULLWIDTH YEN SIGN", "", "CJK UNIFIED IDEOGRAPH-23456",
                     ""};
    String oldname[] = {"", "LATIN SMALL LETTER DOTLESS J BAR HOOK", "", "",
                        "", "", "", "", "FULLWIDTH OPENING PARENTHESIS", "", 
                        "", "", "HORIZONTAL TABULATION"};
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
                             "CJK UNIFIED IDEOGRAPH-23456", 
                             "HORIZONTAL TABULATION"};
                             
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
        errln("FAIL " + name[i] + " expected character \\u" + hex(c[i]));
        break;
      }
      
      //retrieving unicode character from 1.0 name
      uc = UCharacter.getCharFromName1_0(oldname[i]);
      if (uc != c[i] && oldname[i].length() != 0)
      {
        errln("FAIL " + oldname[i] + " expected 1.0 character \\u" + hex(c[i]));
        break;
      }
      
      //retrieving unicode character from 1.0 name
      uc = UCharacter.getCharFromExtendedName(extendedname[i]);
      if (uc != c[i] && i != 0 && (i == 1 || i == 6))
      {
        errln("FAIL " + extendedname[i] + " expected extended character \\u" + hex(c[i]));
        break;
      }
    }
    
    // test getName works with mixed-case names (new in 2.0)
    if (0x61 != UCharacter.getCharFromName("LATin smALl letTER A")) { 
        errln(
          "FAIL: 'LATin smALl letTER A' should result in character U+0061"); 
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
  	ValueIterator iterator = UCharacter.getNameIterator();
  	ValueIterator.Element element = new ValueIterator.Element();
    ValueIterator.Element old     = new ValueIterator.Element();
    // testing subrange
 	iterator.setRange(0xF, 0x45);
 	while (iterator.next(element)) {
    	if (element.integer <= old.integer) {
         	errln("FAIL next returned a less codepoint \\u" + 
         	      Integer.toHexString(element.integer) + " than \\u" + 
         	      Integer.toHexString(old.integer));
         	break;
        }
        if (!UCharacter.getName(element.integer).equals(element.value)) {
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
        if (!UCharacter.getName(element.integer).equals(element.value)) {
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
         	      Integer.toHexString(element.integer) + " name should be "
         	      + UCharacter.getExtendedName(element.integer) + 
         	      " instead of " + (String)element.value);
         	break;
        }
        old.integer++; 
    }
	iterator = UCharacter.getName1_0Iterator();
    old.integer = 0;
    while (iterator.next(element)) {
    	System.out.println(Integer.toHexString(element.integer) + " " +
    	                   (String)element.value);
    	if (element.integer != 0 && element.integer <= old.integer) {
         	errln("FAIL next returned a less codepoint \\u" + 
         	      Integer.toHexString(element.integer) + " than \\u" + 
         	      Integer.toHexString(old.integer));
         	break;
        }
        if (!element.value.equals(UCharacter.getName1_0(element.integer))) {
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

    /* ### TODO: test error cases and other interesting things */
  }
  
  /**
  * Testing the for illegal characters
  */
  public void TestIsLegal() 
  {
    int illegal[] = {0xFFFE, 0x00FFFF, 0x005FFFE, 0x005FFFF, 0x0010FFFE, 0x0010FFFF,
                     0x110000, 0x00FDD0, 0x00FDDF, 0x00FDE0, 0x00FDEF};
    int legal[] = {0x61, 0x00FFFD, 0x0010000, 0x005FFFD, 0x0060000, 0x0010FFFD,
                   0xFDCF, 0x00FDF0};
    for (int count = 0; count < illegal.length; count ++) {
        if (UCharacter.isLegal(illegal[count])) {
            errln("FAIL \\u" + hex(illegal[count]) + 
                    " is not a legal character");
        }
    }
    
    for (int count = 0; count < legal.length; count ++) {
        if (!UCharacter.isLegal(legal[count])) {
            errln("FAIL \\u" + hex(legal[count]) + " is a legal character");
        }
    }
  }
  
  public void TestCaseFolding() 
  {
    int simple[] = {
        // input, default, exclude special i
        0x61,   0x61,  0x61,
        0x49,   0x69,  0x69,
        0x131,  0x69,  0x131,
        0xdf,   0xdf,  0xdf,
        0xfb03, 0x00fb03, 0x00fb03,
        0x5ffff,0x5ffff,0x5ffff
    };
    
    boolean isUnicode31 = UCharacter.getUnicodeVersion().compareTo("3.1") >= 0;           
	    
    // test simple case folding
    for (int i = 0; i < simple.length; i += 3) {
        if (UCharacter.foldCase(simple[i], true) != simple[i + 1]) {
            errln("FAIL: foldCase(\\u" + hex(simple[i]) + 
                  ", true) should be \\u" + hex(simple[i + 1]));
        }
        if (isUnicode31 && 
            UCharacter.foldCase(simple[i], false) != simple[i + 2]) {
            errln("FAIL: foldCase(\\u" + hex(simple[i]) + 
                  ", false) should be \\u" + hex(simple[i + 2]));
        }
    }

    // test full string case folding with default option and separate buffers
    String mixed                 = "\u0061\u0042\u0131\u03d0\u00df\ufb03\ud93f\udfff",
           foldedDefault         = "\u0061\u0062\u0069\u03b2\u0073\u0073\u0066\u0066\u0069\ud93f\udfff";
    String foldedExcludeSpecialI = "\u0061\u0062\u0131\u03b2\u0073\u0073\u0066\u0066\u0069\ud93f\udfff";
    
    String foldedstr = UCharacter.foldCase(mixed, true);
    if (!foldedDefault.equals(foldedstr)) {
        errln("FAIL: foldCase(" + hex(mixed) + ", true) should be " + 
              foldedDefault);
    }
    
    if (isUnicode31) {
        if (!UCharacter.foldCase(mixed, false).equals(foldedExcludeSpecialI)) {
        	errln("FAIL: foldCase(" + hex(mixed) + ", true) should be " + 
                  foldedExcludeSpecialI);
        }
    }
    
    String str1 = "A\u00df\u00b5\ufb03\uD801\uDC0C\u0131",
           str2 = "ass\u03bcffi\uD801\uDC34i",
           str3 = "ass\u03bcffi\uD801\uDC34\u0131";
    if (!str2.equals(UCharacter.foldCase(str1, true))) {
       errln("FAIL: foldCase(" + hex(str1) + ") should be " + hex(str2));
    }

    // alternate handling for dotted I/dotless i (U+0130, U+0131)
    if (!str3.equals(UCharacter.foldCase(str1, false))) {
        errln("FAIL: foldCase(" + hex(str1) + " should be " + hex(str3));
    }
  }

  	/**
  	* Testing the strings case mapping methods
  	*/
  	public void TestCaseUpper() 
  	{	
    	String beforeUpper =  "\u0061\u0042\u0069\u03c2\u00df\u03c3\u002f\ufb03\ud93f\udfff",
           	   upperRoot =    "\u0041\u0042\u0049\u03a3\u0053\u0053\u03a3\u002f\u0046\u0046\u0049\ud93f\udfff",
               upperTurkish = "\u0041\u0042\u0130\u03a3\u0053\u0053\u03a3\u002f\u0046\u0046\u0049\ud93f\udfff";

    	// uppercase with root locale and in the same buffer
    	String result = UCharacter.toUpperCase(beforeUpper);
    	if (result == null || !upperRoot.equals(result)) {
      		errln("Fail " + beforeUpper + " after uppercase should be " + 
      		      upperRoot);
    	}
      
    	// uppercase with turkish locale and separate buffers
    	result = UCharacter.toUpperCase(new Locale("tr", "TR"), beforeUpper);
    	if (result == null || !upperTurkish.equals(result)) {
      		errln("Fail " + beforeUpper + " after turkish-sensitive uppercase " 
      		      + "should be " + upperTurkish);
  		}
  	}
  
 	public void TestCaseLower() 
 	{
  	  	String beforeLower =  "\u0061\u0042\u0049\u03a3\u00df\u03a3\u002f\ud93f\udfff",
               lowerRoot =    "\u0061\u0062\u0069\u03c3\u00df\u03c2\u002f\ud93f\udfff",
               lowerTurkish = "\u0061\u0062\u0131\u03c3\u00df\u03c2\u002f\ud93f\udfff";

        String result = UCharacter.toLowerCase(beforeLower);
   		if (result == null || !lowerRoot.equals(result)) {
      		errln("Fail " + beforeLower + " after lowercase should be " + 
      		      lowerRoot);
   		}
   
    	// lowercase with turkish locale
    	result = UCharacter.toLowerCase(new Locale("tr", "TR"), beforeLower);
    	if (result == null || !lowerTurkish.equals(result)) {
     		errln("Fail " + beforeLower + 
     		      " after turkish-sensitive lowercase " +
                  "should be " + lowerRoot);
    	}
  	}
  	
  	public void TestCaseTitle() {
    	String beforeTitle = "\u0061\u0042\u0020\u0069\u03c2\u0020\u00df\u03c3\u002f\ufb03\ud93f\udfff";
    	String titleWord   = "\u0041\u0062\u0020\u0049\u03c2\u0020\u0053\u0073\u03c3\u002f\u0046\u0066\u0069\ud93f\udfff";
    	String titleChar   = "\u0041\u0042\u0020\u0049\u03a3\u0020\u0053\u0073\u03a3\u002f\u0046\u0066\u0069\ud93f\udfff";

    	BreakIterator titleIterChars = BreakIterator.getCharacterInstance();
	    if (titleIterChars == null) {
 	        errln("error: character break iterator not opened");
   		}

    	String result = UCharacter.toTitleCase(beforeTitle, null);
    	if (result == null || result.length() == 0 ||
    	    !result.equals(titleWord)) {
        	errln("error in toTitleCase(standard iterator) unable to return title case of string or title case of string does not match word breaker");
        }

	    // titlecase with UBRK_CHARACTERS and separate buffers
    	result = UCharacter.toTitleCase(beforeTitle, titleIterChars);
    	if (result == null || !result.equals(titleChar)) {
        	errln("error in toTitleCase(character iterator) expected result " +
        	      titleChar);
        }

		BreakIterator titleIterWord = BreakIterator.getWordInstance();
	    if (titleIterWord == null) {
 	        errln("error: word break iterator not opened");
   		}
    	result = UCharacter.toTitleCase(beforeTitle, titleIterWord);
	    if (result == null || !result.equals(titleWord)) {
        	errln("error in toTitleCase(word iterator) expected result " +
        	      titleWord);
        }
	}
  
  /**
  * Tests for case mapping in the file SpecialCasing.txt
  * This method reads in SpecialCasing.txt file for testing purposes. 
  * A default path is provided relative to the src path, however the user 
  * could set a system property to change the directory path.<br>
  * e.g. java -DUnicodeData="data_dir_path" com.ibm.dev.test.lang.UCharacterTest
  */
  public void TestSpecialCasing()
  {
    // default unicode data file name
    String SPECIALCASING_FILE = "/src/com/ibm/icu/dev/data/unicode/SpecialCasing.txt";
    SPECIALCASING_FILE.replace('/',File.pathSeparatorChar);
    // unicode data file path system name
    final String UNICODE_DATA_SYSTEM_NAME = "UnicodeData";
    String s = System.getProperty(UNICODE_DATA_SYSTEM_NAME);
    if (s == null) {
    // assuming runtime directory is on the same level as the source
      s = System.getProperty("user.dir") + SPECIALCASING_FILE;
    }
    else {
      StringBuffer tempfilename = new StringBuffer(s);
      if (tempfilename.charAt(tempfilename.length() - 1) != 
          File.pathSeparatorChar) {
        tempfilename.append(File.separatorChar);
      }
      tempfilename.append("SpecialCasing.txt");
      s = tempfilename.toString();
    }
    
    try
	{
	  // reading in the SpecialCasing file
	  FileReader fr = new FileReader(s);
	  BufferedReader input = new BufferedReader(fr);
	    
      while (true)
      {
        s = input.readLine();
        if (s == null) {
            break;
        }
        if (s.length() == 0 || s.charAt(0) == '#') {
            continue;
        }
        String chstr[] = getUnicodeStrings(s);
        if (chstr.length == 5) {
            StringBuffer strbuffer   = new StringBuffer(chstr[0]);
            StringBuffer lowerbuffer = new StringBuffer(chstr[1]); 
            StringBuffer upperbuffer = new StringBuffer(chstr[3]); 
            
            if (chstr[4].indexOf("AFTER_i NOT_MORE_ABOVE") != -1) {
                strbuffer.insert(0, 'i');
                lowerbuffer.insert(0, strbuffer);
                upperbuffer.insert(0, (char)(0x130));
            } 
            else {
                if (chstr[4].indexOf("MORE_ABOVE") != -1) {
                    strbuffer.append((char)0x300);
                    lowerbuffer.append((char)0x300);
                    upperbuffer.append((char)0x300);
                }
                if (chstr[4].indexOf("AFTER_i") != -1) {
                    strbuffer.insert(0, 'i');
                    lowerbuffer.insert(0, 'i');
                    upperbuffer.insert(0, 'I');
                }
                if (chstr[4].indexOf("FINAL_SIGMA") != -1) {
                    strbuffer.insert(0, 'c');
                    lowerbuffer.insert(0, 'c');
                    upperbuffer.insert(0, 'C');
                }
            }
            if (UCharacter.isLowerCase(chstr[4].charAt(0))) {
                Locale locale = new Locale(chstr[4].substring(0, 2), "");
                if (!UCharacter.toLowerCase(locale, 
                        strbuffer.toString()).equals(lowerbuffer.toString())) {
                    errln(s);
                    errln("Fail: toLowerCase for locale " + locale + 
                        ", character " + Utility.escape(strbuffer.toString()) +
                        ", expected " + Utility.escape(lowerbuffer.toString()) 
                        + " but resulted in " + 
                        Utility.escape(UCharacter.toLowerCase(locale, 
                                                      strbuffer.toString())));
                }
                if (!UCharacter.toUpperCase(locale, 
                       strbuffer.toString()).equals(upperbuffer.toString())) {
                    errln(s);
                    errln("Fail: toUpperCase for locale " + locale + 
                        ", character " + Utility.escape(strbuffer.toString()) 
                        + ", expected "
                        + Utility.escape(upperbuffer.toString()) + 
                        " but resulted in " + 
                        Utility.escape(UCharacter.toUpperCase(locale, 
                                                      strbuffer.toString())));
                }
            }
            else {
                if (!UCharacter.toLowerCase(strbuffer.toString()).equals(
                                                    lowerbuffer.toString())) {
                    errln(s);
                    errln("Fail: toLowerCase for character " + 
                          Utility.escape(strbuffer.toString()) + ", expected " 
                          + Utility.escape(lowerbuffer.toString()) 
                          + " but resulted in " + 
                          Utility.escape(UCharacter.toLowerCase( 
                                                      strbuffer.toString())));
                }
                if (!UCharacter.toUpperCase(strbuffer.toString()).equals(
                                                    upperbuffer.toString())) {
                    errln(s);
                    errln("Fail: toUpperCase for character " + 
                          Utility.escape(strbuffer.toString()) + ", expected "
                          + Utility.escape(upperbuffer.toString()) + 
                          " but resulted in " + 
                          Utility.escape(UCharacter.toUpperCase( 
                                                      strbuffer.toString())));
                }
            }
        }
        else {
            if (!UCharacter.toLowerCase(chstr[0]).equals(chstr[1])) {
                errln(s);
                errln("Fail: toLowerCase for character " + 
                      Utility.escape(chstr[0]) + ", expected "
                      + Utility.escape(chstr[1]) + " but resulted in " + 
                      Utility.escape(UCharacter.toLowerCase(chstr[0])));
            }
            if (!UCharacter.toUpperCase(chstr[0]).equals(chstr[3])) {
                errln(s);
                errln("Fail: toUpperCase for character " + 
                      Utility.escape(chstr[0]) + ", expected "
                      + Utility.escape(chstr[3]) + " but resulted in " + 
                      Utility.escape(UCharacter.toUpperCase(chstr[0])));
            }
        }
      }
      input.close();
    }
    catch (FileNotFoundException e)
    {
      errln("FAIL SpecialCasing.txt not found in \n" + s +
            ". Configure the system setting UnicodeData to the right path\n" +
            "e.g. java -DUnicodeData=\"data_dir_path\" " +
            "com.ibm.icu.dev.test.lang.UCharacterTest");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    
    String special = "ab'cD \uFB00i\u0131I\u0130 \u01C7\u01C8\u01C9 " + 
                     UTF16.valueOf(0x1043C) + UTF16.valueOf(0x10414);
    String specialUpper = "AB'CD FFIII\u0130 \u01C7\u01C7\u01C7 " +
                          UTF16.valueOf(0x10414) + UTF16.valueOf(0x10414);
    String specialTitle = "Ab'cd FFi\u0131I\u0130 \u01C7\u01C8\u01C9 " +
                          UTF16.valueOf(0x10414) + UTF16.valueOf(0x1043C);
    String specialLower = "ab'cd \uFB00i\u0131ii \u01C9\u01C9\u01C9 " + 
                          UTF16.valueOf(0x1043C) + UTF16.valueOf(0x1043C);
    String result = UCharacter.toUpperCase(special);
    if (!result.equals(specialUpper)) {
        errln("Error getting uppercase in special string");
    }
    /*
    result = UCharacter.toTitleCase(special);
    if (!result.equals(specialLower)) {
        errln("Error getting lowercase in special string");
    }
    */
    result = UCharacter.toLowerCase(special);
    if (!result.equals(specialLower)) {
        errln("Error getting lowercase in special string");
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
              errln("UCharacterEnumeration failed: Ranges not continuous " + 
                    "0x" + Integer.toHexString(result.start));
          }
          
          limit = result.limit;
          if (result.value == prevtype) {
              errln("Type of the next set of enumeration should be different");
          }
          prevtype = result.value;
          /*
          System.out.println("start and end " + Integer.toHexString(start) + 
                             " " + Integer.toHexString(end));
                             */
		  for (int i = result.start; i < limit; i ++) {
              int temptype = UCharacter.getType(i);
              if (temptype != result.value) {
                  errln("UCharacterEnumeration failed: Codepoint \\u" + 
                        Integer.toHexString(i) + " should be of type " +
                        UCharacter.getType(i) + " not " + result.value);
              }
          }
      }
      
      iterator.reset();
      if (iterator.next(result) == false || result.start != 0) {
          System.out.println("result " + result.start);
          errln("UCharacterEnumeration reset() failed");
      }
  }
  
  /**
  * Converting the hex numbers represented betwee                             n ';' to Unicode strings
  * @param str string to break up into Unicode strings
  * @return array of Unicode strings ending with a null
  */
  private String[] getUnicodeStrings(String str)
  {
    Vector v = new Vector(10);
    int end = str.indexOf("; ");
    int start = 0;
    while (end != -1) {
        StringBuffer buffer = new StringBuffer(10);
        int tempstart = start;
        int tempend   = str.indexOf(' ', tempstart);
        while (tempend != -1 && tempend < end) {
           buffer.append((char)Integer.parseInt(str.substring(tempstart, 
                                                              tempend), 16));
           tempstart = tempend + 1;
           tempend   = str.indexOf(' ', tempstart);
        }
        String s = str.substring(tempstart, end);
        try {
            if (s.length() != 0) {
                buffer.append((char)Integer.parseInt(s, 16));
            }
        } catch (NumberFormatException e) {
            buffer.append(s);
        }
        start = end + 2;
        end   = str.indexOf("; ", start);
        v.addElement(buffer.toString());
    }
    String s = str.substring(start);
    if (s.charAt(0) != '#') {
        v.addElement(s);
    }
    int size = v.size();
    String result[] = new String[size];
    for (int i = 0; i < size; i ++) {
        result[i] = (String)v.elementAt(i);
    }
    return result;
  }
}

