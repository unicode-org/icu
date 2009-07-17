/**
 *******************************************************************************
 * Copyright (C) 2001-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Holiday;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceTypeMismatchException;


public final class ICUResourceBundleTest extends TestFmwk {
    private static final ClassLoader testLoader = ICUResourceBundleTest.class.getClassLoader();

    public static void main(String args[]) throws Exception {
        ICUResourceBundleTest test = new ICUResourceBundleTest();
        test.run(args);

    }
    public void TestGetResources(){
        try{
            // It does not work well in eclipse plug-in test because of class loader configuration??
            // For now, specify resource path explicitly in this test case
            //Enumeration en = testLoader.getResources("META-INF");
            Enumeration en = testLoader.getResources("com.ibm.icu.dev.data");
            for(;en.hasMoreElements();) {
                URL url = (URL)en.nextElement();
                if (url == null) {
                    warnln("could not load resource data");
                    return;
                }
                URLConnection c = url.openConnection();

                if (c instanceof JarURLConnection) {
                    JarURLConnection jc = (JarURLConnection)c;
                    JarEntry je = jc.getJarEntry();
                    logln("jar entry: " + je.toString()); 
                } else {
                    InputStream is = c.getInputStream();
                    logln("input stream:");
                    InputStreamReader r = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(r);
                    String line = null;
                    int n = 0;
                    while ((line = br.readLine()) != null) {
                        logln("  " + ++n + ": " + line);
                    }
                }
            }
        }catch(SecurityException ex) {
            warnln("could not load resource data: " + ex);
            ex.printStackTrace();
    }catch(NullPointerException ex) {
        // thrown by ibm 1.4.2 windows jvm security manager
        warnln("could not load resource data: " + ex);
        }catch(Exception ex){
        ex.printStackTrace();
            errln("Unexpected exception: "+ ex);
        }
    }
    public void TestResourceBundleWrapper(){
        UResourceBundle bundle = UResourceBundle.getBundleInstance("com.ibm.icu.impl.data.HolidayBundle", "da_DK");
        Object o = bundle.getObject("holidays");
        if(o instanceof Holiday[] ){
            logln("wrapper mechanism works for Weekend data");
        }else{
            errln("Did not get the expected output for Weekend data");
        }

        bundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "bogus");
        if(bundle instanceof UResourceBundle && bundle.getULocale().equals("en_US")){
            logln("wrapper mechanism works for bogus locale");
        }else{
            errln("wrapper mechanism failed for bogus locale.");
        }

        try{
            bundle = UResourceBundle.getBundleInstance("bogus", "bogus");
            if(bundle!=null){
              errln("Did not get the expected exception");
            }
        }catch(MissingResourceException ex){
            logln("got the expected exception");
        }


    }
    public void TestJB3879(){
        // this tests tests loading of root bundle when a resource bundle
        // for the default locale is requested
        try {
            UResourceBundle bundle = (UResourceBundle) UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata", ULocale.getDefault().toString(), testLoader);
            if(bundle==null){
                errln("could not create the resource bundle");
            }
        }
        catch (MissingResourceException ex) {
            warnln("could not load test data: " + ex.getMessage());
        }
    }
    public void TestOpen(){
        UResourceBundle bundle = (UResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "en_US_POSIX");

        if(bundle==null){
            errln("could not create the resource bundle");
        }

        UResourceBundle obj =  bundle.get("NumberPatterns");

        int size = obj.getSize();
        int type = obj.getType();
        if(type == UResourceBundle.ARRAY){
            UResourceBundle sub;
            for(int i=0; i<size; i++){
                sub = obj.get(i);
                String temp =sub.getString();
                if(temp.length()==0){
                    errln("Failed to get the items from NumberPatterns array in bundle: "+
                            bundle.getULocale().getBaseName());
                }
                //System.out.println("\""+prettify(temp)+"\"");
            }

        }
        String[] strings = bundle.getStringArray("NumberPatterns");
        if(size!=strings.length){
            errln("Failed to get the items from NumberPatterns array in bundle: "+
                    bundle.getULocale().getBaseName());
        }
        {
            obj =  bundle.get("NumberElements");

            size = obj.getSize();
            type = obj.getType();
            if(type == UResourceBundle.ARRAY){
                UResourceBundle sub;
                for(int i=0; i<size; i++){
                    sub = obj.get(i);
                    String temp =sub.getString();
                    if(temp.length()==0){
                        errln("Failed to get the items from NumberPatterns array in bundle: "+
                                bundle.getULocale().getBaseName());
                    }
                   // System.out.println("\""+prettify(temp)+"\"");
                }

            }
        }
        if(bundle==null){
            errln("could not create the resource bundle");
        }

        bundle = (UResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zzz_ZZ_very_very_very_long_bogus_bundle");
        if(!bundle.getULocale().equals(ULocale.getDefault())){
            errln("UResourceBundle did not load the default bundle when bundle was not found. Default: " + ULocale.getDefault() + 
                        ", Bundle locale: " + bundle.getULocale());
        }
    }

    public void TestBasicTypes(){
        UResourceBundle bundle = null;
        try {
            bundle = (UResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata", "testtypes", testLoader);
        }
        catch (MissingResourceException e) {
            warnln("could not load test data: " + e.getMessage());
            return;
        }
        {
            String expected = "abc\u0000def";
            UResourceBundle sub = bundle.get("zerotest");
            if(!expected.equals(sub.getString())){
                errln("Did not get the expected string for key zerotest in bundle testtypes");
            }
            sub = bundle.get("emptyexplicitstring");
            expected ="";
            if(!expected.equals(sub.getString())){
                errln("Did not get the expected string for key emptyexplicitstring in bundle testtypes");
            }
            sub = bundle.get("emptystring");
            expected ="";
            if(!expected.equals(sub.getString())){
                errln("Did not get the expected string for key emptystring in bundle testtypes");
            }
        }
        {
            int expected = 123;
            UResourceBundle sub = bundle.get("onehundredtwentythree");
            if(expected!=sub.getInt()){
                errln("Did not get the expected int value for key onehundredtwentythree in bundle testtypes");
            }
            sub = bundle.get("emptyint");
            expected=0;
            if(expected!=sub.getInt()){
                errln("Did not get the expected int value for key emptyint in bundle testtypes");
            }
        }
        {
            int expected = 1;
            UResourceBundle sub = bundle.get("one");
            if(expected!=sub.getInt()){
                errln("Did not get the expected int value for key one in bundle testtypes");
            }
        }
        {
            int expected = -1;
            UResourceBundle sub = bundle.get("minusone");
            int got = sub.getInt();
            if(expected!=got){
                errln("Did not get the expected int value for key minusone in bundle testtypes");
            }
            expected = 0xFFFFFFF;
            got = sub.getUInt();
            if(expected!=got){
                errln("Did not get the expected int value for key minusone in bundle testtypes");
            }
        }
        {
            int expected = 1;
            UResourceBundle sub = bundle.get("plusone");
            if(expected!=sub.getInt()){
                errln("Did not get the expected int value for key minusone in bundle testtypes");
            }

        }
        {
            int[] expected = new int[]{ 1, 2, 3, -3, 4, 5, 6, 7 }   ;
            UResourceBundle sub = bundle.get("integerarray");
            if(!Utility.arrayEquals(expected,sub.getIntVector())){
                errln("Did not get the expected int vector value for key integerarray in bundle testtypes");
            }
            sub = bundle.get("emptyintv");
            expected = new int[0];
            if(!Utility.arrayEquals(expected,sub.getIntVector())){
                errln("Did not get the expected int vector value for key emptyintv in bundle testtypes");
            }

        }
        {
            UResourceBundle sub = bundle.get("binarytest");
            ByteBuffer got = sub.getBinary();
            if(got.remaining()!=15){
                errln("Did not get the expected length for the binary ByteBuffer");
            }
            for(int i=0; i< got.remaining(); i++){
                byte b = got.get();
                if(b!=i){
                    errln("Did not get the expected value for binary buffer at index: "+i);
                }
            }
            sub = bundle.get("emptybin");
            got = sub.getBinary();
            if(got.remaining()!=0){
                errln("Did not get the expected length for the emptybin ByteBuffer");
            }

        }
        {
            UResourceBundle sub = bundle.get("emptyarray");
            String key = sub.getKey();
            if(!key.equals("emptyarray")){
                errln("Did not get the expected key for emptytable item");
            }
            if(sub.getSize()!=0){
                errln("Did not get the expected length for emptytable item");
            }
        }
        {
            UResourceBundle sub = bundle.get("menu");
            String key = sub.getKey();
            if(!key.equals("menu")){
                errln("Did not get the expected key for menu item");
            }
            UResourceBundle sub1 = sub.get("file");
            key = sub1.getKey();
            if(!key.equals("file")){
                errln("Did not get the expected key for file item");
            }
            UResourceBundle sub2 = sub1.get("open");
            key = sub2.getKey();
            if(!key.equals("open")){
                errln("Did not get the expected key for file item");
            }
            String value = sub2.getString();
            if(!value.equals("Open")){
                errln("Did not get the expected value for key for oen item");
            }

            sub = bundle.get("emptytable");
            key = sub.getKey();
            if(!key.equals("emptytable")){
                errln("Did not get the expected key for emptytable item");
            }
            if(sub.getSize()!=0){
                errln("Did not get the expected length for emptytable item");
            }
            sub = bundle.get("menu").get("file");
            int size = sub.getSize();
            String expected;
            for(int i=0; i<size; i++){
                sub1 = sub.get(i);

                switch(i){
                    case 0:
                        expected = "exit";
                        break;
                    case 1:
                        expected = "open";
                        break;
                    case 2:
                        expected = "save";
                        break;
                    default:
                        expected ="";
                }
                String got = sub1.getKey();
                if(!expected.equals(got)){
                    errln("Did not get the expected key at index"+i+". Expected: "+expected+" Got: "+got);
                }else{
                    logln("Got the expected key at index: "+i);
                }
            }
        }

    }
    private static final class TestCase{
        String key;
        int value;
        TestCase(String key, int value){
            this.key = key;
            this.value = value;
        }
    }
    public void TestTable32(){
        TestCase[] arr = new TestCase[]{
          new TestCase  ( "ooooooooooooooooo", 0 ),
          new TestCase  ( "oooooooooooooooo1", 1 ),
          new TestCase  ( "ooooooooooooooo1o", 2 ),
          new TestCase  ( "oo11ooo1ooo11111o", 25150 ),
          new TestCase  ( "oo11ooo1ooo111111", 25151 ),
          new TestCase  ( "o1111111111111111", 65535 ),
          new TestCase  ( "1oooooooooooooooo", 65536 ),
          new TestCase  ( "1ooooooo11o11ooo1", 65969 ),
          new TestCase  ( "1ooooooo11o11oo1o", 65970 ),
          new TestCase  ( "1ooooooo111oo1111", 65999 )
        };
        UResourceBundle bundle = null;
        try {
            bundle = (UResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testtable32", testLoader);
        }
        catch (MissingResourceException ex) {
            warnln("could not load resource data: " + ex.getMessage());
            return;
        }

        if(bundle.getType()!= UResourceBundle.TABLE){
            errln("Could not get the correct type for bundle testtable32");
        }
        int size =bundle.getSize();
        if(size!=66000){
            errln("Could not get the correct size for bundle testtable32");
        }
        for(int i =0; i<size; i++){
            UResourceBundle item = bundle.get(i);
            String key = item.getKey();
            int parsedNumber = parseTable32Key(key);
            int number=-1;
            switch(item.getType()){
                case UResourceBundle.STRING:
                    String value = item.getString();
                    number = UTF16.charAt(value,0);
                    break;
                case UResourceBundle.INT:
                    number = item.getInt();
                    break;
                default:
                    errln("Got unexpected resource type in testtable32");

            }
            if(number!=parsedNumber){
                errln("Did not get expected value in testtypes32 for key"+
                      key+". Expected: "+parsedNumber+" Got:"+number);
            }

        }
        for(int i=0;i<arr.length; i++){
            String expected = arr[i].key;
            UResourceBundle item = bundle.get(expected);
            int number=0;
            String key = item.getKey();
            int parsedNumber = parseTable32Key(key);
            if(!key.equals(expected)){
                errln("Did not get the expected key. Expected: "+expected+" Got:"+key);
            }
            switch(item.getType()){
                case UResourceBundle.STRING:
                    String value = item.getString();
                    number = UTF16.charAt(value,0);
                    break;
                 case UResourceBundle.INT:
                    number = item.getInt();
                    break;
                default:
                    errln("Got unexpected resource type in testtable32");
            }

            if(number!=parsedNumber){
                errln("Did not get expected value in testtypes32 for key"+
                      key+". Expected: "+parsedNumber+" Got:"+number);
            }
        }
    }
    private static int  parseTable32Key(String key) {
        int number;
        char c;

        number=0;
        for(int i=0; i<key.length(); i++){
            c = key.charAt(i);
            number<<=1;
            if(c=='1') {
                number|=1;
            }
        }
        return number;
    }

    public void TestAliases(){
/*
       String simpleAlias   = "Open";

       UResourceBundle rb = (UResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases", testLoader);
       if (rb == null) {
           warnln("could not load testaliases data");
           return;
       }
        UResourceBundle sub = rb.get("simplealias");
        String s1 = sub.getString("simplealias");
        if(s1.equals(simpleAlias)){
            logln("Alias mechanism works for simplealias");
        }else{
            errln("Did not get the expected output for simplealias");
        }
        {
            try{
                rb = (UResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases",testLoader);
                sub = rb.get("nonexisting");
                errln("Did not get the expected exception for nonexisting");
            }catch(MissingResourceException ex){
                logln("Alias mechanism works for nonexisting alias");
            }
        }
        {
            rb = (UResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases",testLoader);
            sub = rb.get("referencingalias");
            s1 = sub.getString();
            if(s1.equals("Hani")){
                logln("Alias mechanism works for referencingalias");
            }else{
                errln("Did not get the expected output for referencingalias");
            }
        }
        {
            rb = (UResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases",testLoader);
            sub = rb.get("boundaries");
            String word = sub.getString("word");

            if(word.equals("word_ja.brk")){
                logln("Got the expected output for boundaries/word");
            }else{
                errln("Did not get the expected type for boundaries/word");
            }

        }
        {
            UResourceBundle rb1 = (UResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases",testLoader);
            if(rb1!=rb){
                errln("Caching of the resource bundle failed");
            }else{
                logln("Caching of resource bundle passed");
            }
            sub = rb1.get("testGetStringByKeyAliasing" );

            s1 = sub.get("KeyAlias0PST").getString();
            if(s1.equals("America/Los_Angeles")){
                logln("Alias mechanism works for KeyAlias0PST");
            }else{
                errln("Did not get the expected output for KeyAlias0PST");
            }

            s1 = sub.getString("KeyAlias1PacificStandardTime");
            if(s1.equals("Pacific Standard Time")){
                logln("Alias mechanism works for KeyAlias1PacificStandardTime");
            }else{
                errln("Did not get the expected output for KeyAlias1PacificStandardTime");
            }
            s1 = sub.getString("KeyAlias2PDT");
            if(s1.equals("PDT")){
                logln("Alias mechanism works for KeyAlias2PDT");
            }else{
                errln("Did not get the expected output for KeyAlias2PDT");
            }

            s1 = sub.getString("KeyAlias3LosAngeles");
            if(s1.equals("Los Angeles")){
                logln("Alias mechanism works for KeyAlias3LosAngeles. Got: "+s1);
            }else{
                errln("Did not get the expected output for KeyAlias3LosAngeles. Got: "+s1);
            }
        }
        {
            sub = rb.get("testGetStringByIndexAliasing" );
            s1 = sub.getString(0);
            if(s1.equals("America/Los_Angeles")){
                logln("Alias mechanism works for testGetStringByIndexAliasing/0. Got: "+s1);
            }else{
                errln("Did not get the expected output for testGetStringByIndexAliasing/0. Got: "+s1);
            }
            s1 = sub.getString(1);
            if(s1.equals("Pacific Standard Time")){
                logln("Alias mechanism works for testGetStringByIndexAliasing/1");
            }else{
                errln("Did not get the expected output for testGetStringByIndexAliasing/1");
            }
            s1 = sub.getString(2);
            if(s1.equals("PDT")){
                logln("Alias mechanism works for testGetStringByIndexAliasing/2");
            }else{
                errln("Did not get the expected output for testGetStringByIndexAliasing/2");
            }

            s1 = sub.getString(3);
            if(s1.equals("Los Angeles")){
                logln("Alias mechanism works for testGetStringByIndexAliasing/3. Got: "+s1);
            }else{
                errln("Did not get the expected output for testGetStringByIndexAliasing/3. Got: "+s1);
            }
        }
        {
            sub = rb.get("testAliasToTree" );
            
            ByteBuffer buf = sub.get("standard").get("%%CollationBin").getBinary();
            if(buf==null){
                errln("Did not get the expected output for %%CollationBin");
            }
        }
        // should not get an exception
        rb = (UResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_RBNF_BASE_NAME,"fr_BE");
        String str = rb.getString("SpelloutRules");
        if(str !=null && str.length()>0){
            logln("Alias mechanism works");
        }else{
            errln("Alias mechanism failed for fr_BE SpelloutRules");
        }
        rb = (UResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_COLLATION_BASE_NAME,"zh_TW");
        UResourceBundle b = (UResourceBundle) rb.getObject("collations");
        if(b != null){
            if(b.get(0).getKey().equals( "default")){
                logln("Alias mechanism works");
            }else{
                errln("Alias mechanism failed for zh_TW collations");
            }
        }else{
            errln("Did not get the expected object for collations");
        }
*/
    }
    public void TestAlias(){
        logln("Testing %%ALIAS");
        UResourceBundle rb = (UResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME,"iw_IL");
        UResourceBundle b = rb.get("NumberPatterns");
        if(b != null){
            if(b.getSize()>0){
                logln("%%ALIAS mechanism works");
            }else{
                errln("%%ALIAS mechanism failed for iw_IL NumberPatterns");
            }
        }else{
            errln("%%ALIAS mechanism failed for iw_IL");
        }
    }
    public void TestXPathAlias(){
        UResourceBundle rb = (UResourceBundle) UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","te_IN",testLoader);
        UResourceBundle b = rb.get("aliasClient");
        String result = b.getString();
        String expResult= "correct"; 

        if(!result.equals(expResult)){
            errln("Did not get the expected result for XPath style alias");
        }
        try{
            UResourceBundle c = rb.get("rootAliasClient");
            result = c.getString();
            expResult = "correct"; 
            if(!result.equals(expResult)){
                errln("Did not get the expected result for XPath style alias for rootAliasClient");
            }
        }catch( MissingResourceException ex){
            errln("Could not get rootAliasClient");
        }
    }
    public void TestCircularAliases(){
// Aliases no longer supported
//        try{
//            UResourceBundle rb = (UResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases",testLoader);
//            UResourceBundle sub = rb.get("aaa");
//            String s1 = sub.getString();
//            if(s1!=null){
//                errln("Did not get the expected exception");
//            }
//        }catch(IllegalArgumentException ex){
//            logln("got expected exception for circular references");
//        }
//        catch (MissingResourceException ex) {
//            warnln("could not load resource data: " + ex.getMessage());
//        }
    }

    public void TestGetWithFallback(){
        /*
        UResourceBundle bundle =(UResourceBundle) UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","te_IN");
        String key = bundle.getStringWithFallback("Keys/collation");
        if(!key.equals("COLLATION")){
            errln("Did not get the expected result from getStringWithFallback method.");
        }
        String type = bundle.getStringWithFallback("Types/collation/direct");
        if(!type.equals("DIRECT")){
            errln("Did not get the expected result form getStringWithFallback method.");
        }
        */
        ICUResourceBundle bundle = null;
        String key = null;

        bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME,"fr_FR");
        ICUResourceBundle b1 = bundle.getWithFallback("calendar");
        String defaultCal = b1.getStringWithFallback("default");
        if(!defaultCal.equals("gregorian")){
            errln("Did not get the expected default calendar string: Expected: gregorian, Got: "+defaultCal);
        }
        ICUResourceBundle b2 = b1.getWithFallback(defaultCal);
        ICUResourceBundle b3 = b2.getWithFallback("monthNames");
        String defaultContext = b3.getStringWithFallback("default");
        ICUResourceBundle b4 = b3.getWithFallback(defaultContext);
        String defaultWidth  = b4.getStringWithFallback("default");
        ICUResourceBundle b5 = b4.getWithFallback(defaultWidth);
        if(b5.getSize()!=12){
            errln("Did not get the expected size for the default monthNames");
        }
    }

    private static final String DEFAULT_NAME = "default";
    private static final String STANDARD_NAME = "standard";
    private static final String CALENDAR_RESNAME = "calendar";
    private static final String CALENDAR_KEYWORD = "calendar";

    public void TestLocaleDisplayNames() {
        ULocale[] locales = ULocale.getAvailableLocales();
        for (int i = 0; i < locales.length; ++i) {
            if (!hasLocalizedCountryFor(ULocale.ENGLISH, locales[i])){
                 errln("Could not get localized country for "+ locales[i]);
            }
            if(!hasLocalizedLanguageFor(ULocale.ENGLISH, locales[i])){
                errln("Could not get localized language for "+ locales[i]);
            }
            if(!hasLocalizedCountryFor(locales[i], locales[i])){
                errln("Could not get localized country for "+ locales[i]);
                hasLocalizedCountryFor(locales[i], locales[i]);
            }
            if(!hasLocalizedLanguageFor(locales[i], locales[i])){
                errln("Could not get localized language for "+ locales[i]);
            }

            logln(locales[i] + "\t" + locales[i].getDisplayName(ULocale.ENGLISH) + "\t" + locales[i].getDisplayName(locales[i]));
        }
    }

    private static boolean hasLocalizedLanguageFor(ULocale locale, ULocale otherLocale) {
        String lang = otherLocale.getLanguage();
        String localizedVersion = otherLocale.getDisplayLanguage(locale);
        return !lang.equals(localizedVersion);
    }

    private static boolean hasLocalizedCountryFor(ULocale locale, ULocale otherLocale) {
        String country = otherLocale.getCountry();
        if (country.equals("")) return true;
        String localizedVersion = otherLocale.getDisplayCountry(locale);
        return !country.equals(localizedVersion);
    }

    public void TestFunctionalEquivalent(){
       String[] calCases = {
       //  avail    locale                              equiv
           "t",     "en_US_POSIX",                      "en_US@calendar=gregorian",
           "f",     "ja_JP_TOKYO",                      "ja_JP@calendar=gregorian",
           "f",     "ja_JP_TOKYO@calendar=japanese",    "ja@calendar=japanese",
           "t",     "sr@calendar=gregorian",            "sr@calendar=gregorian",
           "t",     "en",                               "en@calendar=gregorian",
           "t",     "th_TH",                            "th@calendar=buddhist",
           "t",     "th_TH@calendar=gregorian",         "th_TH@calendar=gregorian",
           "f",     "th_TH_Bangkok",                    "th@calendar=buddhist",
       };

       logln("Testing functional equivalents for calendar...");
       getFunctionalEquivalentTestCases(ICUResourceBundle.ICU_BASE_NAME,
                                        Calendar.class.getClassLoader(),
               CALENDAR_RESNAME, CALENDAR_KEYWORD, false, calCases);

       logln("Testing error conditions:");
       try {
           ClassLoader cl = BreakIterator.class.getClassLoader();
           ICUResourceBundle.getFunctionalEquivalent(ICUResourceBundle.ICU_BRKITR_BASE_NAME, cl, "calendar",
              "calendar", new ULocale("ar_EG@calendar=islamic"), new boolean[1], true);
           errln("Err: expected MissingResourceException");
       } catch ( MissingResourceException t ) {
           logln("expected MissingResourceException caught (PASS): " + t.toString());
       }
    }

    private void getFunctionalEquivalentTestCases(String path, ClassLoader cl, String resName, String keyword,
            boolean truncate, String[] testCases) {
        //String F_STR = "f";
        String T_STR = "t";
        boolean isAvail[] = new boolean[1];

        logln("Testing functional equivalents...");
        for(int i = 0; i < testCases.length ;i+=3) {
            boolean expectAvail = T_STR.equals(testCases[i+0]);
            ULocale inLocale = new ULocale(testCases[i+1]);
            ULocale expectLocale = new ULocale(testCases[i+2]);

            logln(new Integer(i/3).toString() + ": " + new Boolean(expectAvail).toString() + "\t\t" +
                    inLocale.toString() + "\t\t" + expectLocale.toString());

            ULocale equivLocale = ICUResourceBundle.getFunctionalEquivalent(path, cl, resName, keyword, inLocale, isAvail, truncate);
            boolean gotAvail = isAvail[0];

            if((gotAvail != expectAvail) || !equivLocale.equals(expectLocale)) {
                errln(new Integer(i/3).toString() + ":  Error, expected  Equiv=" + new Boolean(expectAvail).toString() + "\t\t" +
                        inLocale.toString() + "\t\t--> " + expectLocale.toString() + ",  but got " + new Boolean(gotAvail).toString() + " " +
                        equivLocale.toString());
            }
        }
    }

    public void TestNorwegian(){
        try{
            UResourceBundle rb = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "no_NO_NY");
            UResourceBundle sub = rb.get("Countries");
            String s1 = sub.getString("NO");
            if(s1.equals("Noreg")){
                logln("got expected output ");
            }else{
                errln("did not get the expected result");
            }
        }catch(IllegalArgumentException ex){
            errln("Caught an unexpected expected");
        }
    }
    public void TestJB4102(){
        try {
            ICUResourceBundle root =(ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "root");
            ICUResourceBundle t = null;    
// AmPmMarkers now exist in root/islamic calendar, so this test is rendered useless.
//          try{
//              t = root.getWithFallback("calendar/islamic-civil/AmPmMarkers");
//              errln("Second resource does not exist. How did it get here?\n");
//          }catch(MissingResourceException ex){
//              logln("Got the expected exception");
//          }
            try{
                t = root.getWithFallback("calendar/islamic-civil/eras/abbreviated/0/mikimaus/pera");
                errln("Second resource does not exist. How did it get here?\n");
            }catch(MissingResourceException ex){
                logln("Got the expected exception");
            }
            if(t!=null){
                errln("t is not null!");
            }
        } catch (MissingResourceException e) {
           warnln("Could not load the locale data: " + e.getMessage());
        }
    }

    public void TestCLDRStyleAliases() {
        String result = null;
        String expected = null;
        String[]expects = new String[] { "", "a41", "a12", "a03", "ar4" };

        logln("Testing CLDR style aliases......\n");

        UResourceBundle rb = UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata", "te_IN_REVISED",testLoader);
        ICUResourceBundle alias = (ICUResourceBundle)rb.get("a");

        for(int i = 1; i < 5 ; i++) {
          String resource="a"+i;
          UResourceBundle a = (alias).getWithFallback(resource);
          result = a.getString();
          if(result.equals(expected)) {
              errln("CLDR style aliases failed resource with name "+resource+"resource, exp "+expects[i] +" , got " + result); 
          }
        }

    }
    private String getLSString(int status){
        switch(status){
            case ICUResourceBundle.FROM_FALLBACK:
                return "FROM_FALLBACK";
            case ICUResourceBundle.FROM_DEFAULT:
                return "FROM_DEFAULT";
            case ICUResourceBundle.FROM_ROOT: 
                return "FROM_ROOT";
            case ICUResourceBundle.FROM_LOCALE: 
                return "FROM_LOCALE";
            default:
                return "UNKNOWN";
        }
    }
    public void TestLoadingStatus(){
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "yi_IL");
        int status = bundle.getLoadingStatus();
        if(status != ICUResourceBundle.FROM_DEFAULT){
            errln("Did not get the expected value for loading status. Expected "+ getLSString(ICUResourceBundle.FROM_DEFAULT) 
                    + " Got: " + getLSString(status));
        }        
        bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "eo_DE");
        status = bundle.getLoadingStatus();
        if(status != ICUResourceBundle.FROM_FALLBACK){
            errln("Did not get the expected value for loading status. Expected "+ getLSString(ICUResourceBundle.FROM_FALLBACK) 
                    + " Got: " + getLSString(status));
        }        
        
        logln("Test to verify loading status of get(String)");
        bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "te_IN");
        UResourceBundle countries = bundle.get("Countries");
        status = ((ICUResourceBundle)countries).getLoadingStatus();
        if(status != ICUResourceBundle.FROM_FALLBACK){
            errln("Did not get the expected value for loading status. Expected "+ getLSString(ICUResourceBundle.FROM_FALLBACK) 
                    + " Got: " + getLSString(status));
        }
        /*
        UResourceBundle auxExemplar = bundle.get("AuxExemplarCharacters");
        status = auxExemplar.getLoadingStatus();
        if(status != UResourceBundle.FROM_ROOT){
            errln("Did not get the expected value for loading status. Expected "+ getLSString(UResourceBundle.FROM_ROOT) 
                    + " Got: " + getLSString(status));
        } 
        */
        logln("Test to verify loading status of get(int)");
        ICUResourceBundle ms = (ICUResourceBundle)bundle.get("MeasurementSystem");
        status = ms.getLoadingStatus();
        if(status != ICUResourceBundle.FROM_ROOT){
            errln("Did not get the expected value for loading status. Expected "+ getLSString(ICUResourceBundle.FROM_ROOT) 
                    + " Got: " + getLSString(status));
        }
                
        logln("Test to verify loading status of getwithFallback");
        bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata", "sh_YU",testLoader);
        ICUResourceBundle temp = (ICUResourceBundle)bundle.getWithFallback("a/a2");
        status = temp.getLoadingStatus();
        if(status != ICUResourceBundle.FROM_LOCALE){
            errln("Did not get the expected value for loading status. Expected "+ getLSString(ICUResourceBundle.FROM_LOCALE) 
                    + " Got: " + getLSString(status));
        }
        temp = bundle.getWithFallback("a/a1");
        status = temp.getLoadingStatus();
        if(status != ICUResourceBundle.FROM_FALLBACK){
            errln("Did not get the expected value for loading status. Expected "+ getLSString(ICUResourceBundle.FROM_FALLBACK) 
                    + " Got: " + getLSString(status));
        }
        temp = bundle.getWithFallback("a/a4");
        status = temp.getLoadingStatus();
        if(status != ICUResourceBundle.FROM_ROOT){
            errln("Did not get the expected value for loading status. Expected "+ getLSString(ICUResourceBundle.FROM_ROOT) 
                    + " Got: " + getLSString(status));
        }
    }
    public void TestCoverage(){
        UResourceBundle bundle;
        bundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME);
        if (bundle == null){
            errln("UResourceBundle.getBundleInstance(String baseName) failed");
        }
        bundle = null;
        bundle = UResourceBundle.getBundleInstance(ULocale.getDefault());
        if (bundle == null){
            errln("UResourceBundle.getBundleInstance(ULocale) failed");
            return;
        } 
        if (new UResourceTypeMismatchException("coverage") == null){
            errln("Create UResourceTypeMismatchException error");
        }
        class Stub extends UResourceBundle{
            public ULocale getULocale() {return ULocale.ROOT;}
            protected String getLocaleID() {return null;}
            protected String getBaseName() {return null;}
            protected UResourceBundle getParent() {return null;}
            protected void setLoadingStatus(int newStatus) {}
            public Enumeration getKeys() {return null;}
            protected Object handleGetObject(String aKey) {return null;}
        }
        Stub stub = new Stub();
        
        if (!stub.getLocale().equals(ULocale.ROOT.toLocale())){
            errln("UResourceBundle.getLoclae(Locale) should delegate to (ULocale)");
        }
    }
    public void TestJavaULocaleBundleLoading(){
        String baseName="com.ibm.icu.dev.data.resources.TestDataElements";
        String locName = "en_Latn_US";
        UResourceBundle bundle = UResourceBundle.getBundleInstance(baseName, locName, testLoader);
        String fromRoot = bundle.getString("from_root");
        if(!fromRoot.equals("This data comes from root")){
            errln("Did not get the expected string for from_root");
        }
        String fromEn = bundle.getString("from_en");
        if(!fromEn.equals("This data comes from en")){
            errln("Did not get the expected string for from_en");
        }
        String fromEnLatn = bundle.getString("from_en_Latn");
        if(!fromEnLatn.equals("This data comes from en_Latn")){
            errln("Did not get the expected string for from_en_Latn");
        }
        String fromEnLatnUs = bundle.getString("from_en_Latn_US");
        if(!fromEnLatnUs.equals("This data comes from en_Latn_US")){
            errln("Did not get the expected string for from_en_Latn_US");
        }
        UResourceBundle bundle1 = UResourceBundle.getBundleInstance(baseName, new ULocale(locName), testLoader);
        if(!bundle1.equals(bundle)){
            errln("Did not get the expected bundle for "+baseName +"."+locName);
        }
        if(bundle1!=bundle){
            errln("Did not load the bundle from cache");
        }
        
        UResourceBundle bundle2 = UResourceBundle.getBundleInstance(baseName, "en_IN", testLoader);
        if(!bundle2.getLocale().toString().equals("en")){
            errln("Did not get the expected fallback locale. Expected: en Got: "+bundle2.getLocale().toString());    
        }
        UResourceBundle bundle3 = UResourceBundle.getBundleInstance(baseName, "te_IN", testLoader);
        if(!bundle3.getLocale().toString().equals("te")){
            errln("Did not get the expected fallback locale. Expected: te Got: "+bundle2.getLocale().toString());    
        }
        // non-existent bundle .. should return default
        UResourceBundle defaultBundle = UResourceBundle.getBundleInstance(baseName, "hi_IN", testLoader);
        ULocale defaultLocale = ULocale.getDefault();
        if(!defaultBundle.getULocale().equals(defaultLocale)){
            errln("Did not get the default bundle for non-existent bundle");
        }
        // non-existent bundle, non-existent default locale
        // so return the root bundle.
        ULocale.setDefault(ULocale.CANADA_FRENCH);
        UResourceBundle root = UResourceBundle.getBundleInstance(baseName, "hi_IN", testLoader);
        if(!root.getULocale().toString().equals("")){
            errln("Did not get the root bundle for non-existent default bundle for non-existent bundle");
        }        
        //reset the default
        ULocale.setDefault(defaultLocale);
        Enumeration keys = bundle.getKeys();
        int i=0;
        while(keys.hasMoreElements()){
            logln("key: "+ keys.nextElement());
            i++;
        }
        if(i!=4){
            errln("Did not get the expected number of keys: got " + i + ", expected 4");
        }
        UResourceBundle bundle4 = UResourceBundle.getBundleInstance(baseName,"fr_Latn_FR", testLoader);
        if(bundle4==null){
            errln("Could not load bundle fr_Latn_FR");
        }
    }
    public void TestAliasFallback(){
        try{
            ULocale loc = new ULocale("en_US");
            ICUResourceBundle b = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, loc);
            ICUResourceBundle b1 = (ICUResourceBundle)b.getWithFallback("calendar/hebrew/monthNames/format/abbreviated");
            if(b1!=null){
                logln("loaded data for abbreviated month names: "+ b1.getKey()); 
            }
        }catch(MissingResourceException ex){
            warnln("Failed to load data for abbreviated month names");
        }
    }
    private Set<String> setFromEnumeration(Enumeration<String> e) {
        TreeSet<String> set = new TreeSet<String>();
        while (e.hasMoreElements()) {
            set.add(e.nextElement());
        }
        return set;
    }
    /**
     * Test ICUResourceBundle.getKeys() for a whole bundle (top-level resource).
     * JDK JavaDoc for ResourceBundle.getKeys() says that it returns
     * "an Enumeration of the keys contained in this ResourceBundle and its parent bundles."
     */
    public void TestICUGetKeysAtTopLevel() {
        String baseName="com/ibm/icu/dev/data/testdata";
        UResourceBundle te_IN = UResourceBundle.getBundleInstance(baseName, "te_IN", testLoader);
        UResourceBundle te = UResourceBundle.getBundleInstance(baseName, "te", testLoader);
        Set<String> te_set = setFromEnumeration(te.getKeys());
        Set<String> te_IN_set = setFromEnumeration(te_IN.getKeys());
        assertTrue("te.getKeys().contains(string_only_in_Root)", te_set.contains("string_only_in_Root"));
        assertTrue("te.getKeys().contains(string_only_in_te)", te_set.contains("string_only_in_te"));
        assertFalse("te.getKeys().contains(string_only_in_te_IN)", te_set.contains("string_only_in_te_IN"));
        assertTrue("te_IN.getKeys().contains(string_only_in_Root)", te_IN_set.contains("string_only_in_Root"));
        assertTrue("te_IN.getKeys().contains(string_only_in_te)", te_IN_set.contains("string_only_in_te"));
        assertTrue("te_IN.getKeys().contains(string_only_in_te_IN)", te_IN_set.contains("string_only_in_te_IN"));
        // TODO: Check for keys of alias resource items
    }
    /**
     * Test ICUResourceBundle.getKeys() for a resource item (not a whole bundle/top-level resource).
     * This does not take parent bundles into account.
     */
    public void TestICUGetKeysForResourceItem() {
        String baseName="com/ibm/icu/dev/data/testdata";
        UResourceBundle te = UResourceBundle.getBundleInstance(baseName, "te", testLoader);
        UResourceBundle tagged_array_in_Root_te = te.get("tagged_array_in_Root_te");
        Set<String> keys = setFromEnumeration(tagged_array_in_Root_te.getKeys());
        assertTrue("tagged_array_in_Root_te.getKeys().contains(tag0)", keys.contains("tag0"));
        assertTrue("tagged_array_in_Root_te.getKeys().contains(tag1)", keys.contains("tag1"));
        assertFalse("tagged_array_in_Root_te.getKeys().contains(tag7)", keys.contains("tag7"));
        assertFalse("tagged_array_in_Root_te.getKeys().contains(tag12)", keys.contains("tag12"));
        UResourceBundle array_in_Root_te = te.get("array_in_Root_te");
        assertFalse("array_in_Root_te.getKeys().hasMoreElements()", array_in_Root_te.getKeys().hasMoreElements());
        UResourceBundle string_in_Root_te = te.get("string_in_Root_te");
        assertFalse("string_in_Root_te.getKeys().hasMoreElements()", string_in_Root_te.getKeys().hasMoreElements());
    }
}
