/**
 *******************************************************************************
 * Copyright (C) 2001-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.nio.ByteBuffer;
import java.util.MissingResourceException;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;


public final class ICUResourceBundleTest extends TestFmwk {
    
    public static void main(String args[]) throws Exception {
        ICUResourceBundleTest test = new ICUResourceBundleTest();
        test.run(args);

    }
    
    public void TestOpen(){
        ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(UResourceBundle.ICU_BASE_NAME, "en_US_POSIX", ICUData.class.getClassLoader());
       
        if(bundle==null){
            errln("could not create the resource bundle");   
        }
        
        ICUResourceBundle obj = (ICUResourceBundle) bundle.getObject("NumberPatterns");
        
        int size = obj.getSize();
        int type = obj.getType();
        if(type == ICUResourceBundle.ARRAY){
            ICUResourceBundle sub;
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
        {
            obj = (ICUResourceBundle) bundle.getObject("NumberElements");
            
            size = obj.getSize();
            type = obj.getType();
            if(type == ICUResourceBundle.ARRAY){
                ICUResourceBundle sub;
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
        bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(UResourceBundle.ICU_COLLATION_BASE_NAME, "en_US_POSIX");
        if(bundle==null){
            errln("could not load the stream");   
        }
        bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(UResourceBundle.ICU_BASE_NAME, "my_very_very_very_long_bogus_bundle");   
        if(!bundle.getULocale().equals(ULocale.getDefault())){
            errln("UResourceBundle did not load the default bundle when bundle was not found");   
        }
        
        
    }
    
    public void TestBasicTypes(){
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata", "testtypes");
        {
            String expected = "abc\u0000def";
            ICUResourceBundle sub = bundle.get("zerotest");
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
            ICUResourceBundle sub = bundle.get("onehundredtwentythree");
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
            ICUResourceBundle sub = bundle.get("one");
            if(expected!=sub.getInt()){
                errln("Did not get the expected int value for key one in bundle testtypes");
            }
        }
        {
            int expected = -1;
            ICUResourceBundle sub = bundle.get("minusone");
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
            ICUResourceBundle sub = bundle.get("plusone");
            if(expected!=sub.getInt()){
                errln("Did not get the expected int value for key minusone in bundle testtypes");
            }
            
        }
        {
            int[] expected = new int[]{ 1, 2, 3, -3, 4, 5, 6, 7 }   ;
            ICUResourceBundle sub = bundle.get("integerarray");
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
            ICUResourceBundle sub = bundle.get("binarytest");
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
            ICUResourceBundle sub = bundle.get("emptyarray");
            String key = sub.getKey();
            if(!key.equals("emptyarray")){
                errln("Did not get the expected key for emptytable item");   
            }
            if(sub.getSize()!=0){
                errln("Did not get the expected length for emptytable item");   
            }
        }
        {
            ICUResourceBundle sub = bundle.get("menu");
            String key = sub.getKey();
            if(!key.equals("menu")){
                errln("Did not get the expected key for menu item");   
            }
            ICUResourceBundle sub1 = sub.get("file");
            key = sub1.getKey();
            if(!key.equals("file")){
                errln("Did not get the expected key for file item");   
            }
            ICUResourceBundle sub2 = sub1.get("open");
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
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testtable32");
        if(bundle.getType()!= ICUResourceBundle.TABLE){
            errln("Could not get the correct type for bundle testtable32");   
        }
        int size =bundle.getSize();
        if(size!=66000){
            errln("Could not get the correct size for bundle testtable32");   
        }
        for(int i =0; i<size; i++){
            ICUResourceBundle item = bundle.get(i);
            String key = item.getKey();
            int parsedNumber = parseTable32Key(key);
            int number=-1;
            switch(item.getType()){
                case ICUResourceBundle.STRING:
                    String value = item.getString();
                    number = UTF16.charAt(value,0);
                    break;
                case ICUResourceBundle.INT:
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
            ICUResourceBundle item = bundle.get(expected);
            int number=0;
            String key = item.getKey();
            int parsedNumber = parseTable32Key(key);
            if(!key.equals(expected)){
                errln("Did not get the expected key. Expected: "+expected+" Got:"+key);
            }
            switch(item.getType()){
                case ICUResourceBundle.STRING:
                    String value = item.getString();
                    number = UTF16.charAt(value,0);
                    break;
                 case ICUResourceBundle.INT:
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
    /*
    public void TestReferences() {
        ResourceBundle rb = UResourceBundle.getBundleInstance("th");
        logln("got rb:" + rb);
    
        byte[] binaryData = null;//(byte[])rb.getObject("%%CollationBin");
        Object colElem = rb.getObject("collations");
        if(colElem instanceof Object[][]){
            Object[][] colElemArr = (Object[][])colElem;
            if(((String)colElemArr[0][0]).equals("%%CollationBin")){   
               binaryData = (byte[]) colElemArr[0][1];
            }
        }else{
            errln("Did not get the expected object");
        }
        logln("got binaryData: " + binaryData + " length: " + (binaryData == null ? 0 : binaryData.length));
        Object[] stringArrayData = (Object[])rb.getObject("collations");
        //String[] collationData = new String[] {
         //   (String)stringArrayData[0],
         //   (String)stringArrayData[0]
        //};
        logln("got stringData: " + stringArrayData + " length: " + stringArrayData.length);
        logln("got stringDataElement: " + stringArrayData[0] + " length: " + stringArrayData.length);
        //System.out.println("got sdee: " + collationData[0]);
        //  System.out.println("char data length: " + stringArrayData.length());
    }
   */
   String simpleAlias   = "Open";

   Object[][] zoneTests =  new Object[][]{
                               {
                                    "zoneAlias",
                                    new String[] { 
                                                    "PST",
                                                    "Pacific Standard Time",
                                                    "PST",
                                                    "Pacific Daylight Time",
                                                    "PDT",
                                                    "Los Angeles",
                                                },
                               },
                               {
                                   "zoneAlias1",
                                   new String[] { 
                                                   "America/Denver",
                                                   "Mountain Standard Time",
                                                   "MST",
                                                   "Mountain Daylight Time",
                                                   "MDT",
                                                   "Denver",
                        
                                               },
                               },
                               {
                                   "zoneAlias2",
                                   "America/Denver",
                               },
                           };
    Object[] zoneStrings = new Object[] {
                                new String[] { 
                                    
                                    "PST",
                                    "Pacific Standard Time",
                                    "PST",
                                    "Pacific Daylight Time",
                                    "PDT",
                                    "Los Angeles",
                                
                                },
                                new String[] { 
                                    "America/Los_Angeles",
                                    "Pacific Standard Time",
                                    "PST",
                                    "Pacific Daylight Time",
                                    "PDT",
                                    "Los Angeles",
                                
                                },
                            };
    Object[] testGetStringByIndexAliasing =  new String[] { 
                                                    "PST",
                                                    "Pacific Standard Time",
                                                    "PDT",
                                                    "Los Angeles",
                                                };
    Object[][] testGetStringByKeyAliasing = new Object[][]{
                                                {
                                                    "KeyAlias0PST",
                                                    "PST",
                                                },
                                                {
                                                    "KeyAlias1PacificStandardTime",
                                                    "Pacific Standard Time",
                                                },
                                                {
                                                    "KeyAlias2PDT",
                                                    "PDT",
                                                },
                                                {
                                                    "KeyAlias3LosAngeles",
                                                    "Los Angeles",
                                                },
                                            };     
    // 
    public void TestAliases(){
      
        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases");
        ICUResourceBundle sub = rb.get("simplealias");
        String s1 = sub.getString();
        if(s1.equals(simpleAlias)){
            logln("Alias mechanism works for simplealias");
        }else{
            errln("Did not get the expected output for simplealias");
        }
        {
            try{
                rb = (ICUResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases");
                sub = rb.get("nonexisting");
                errln("Did not get the expected exception for nonexisting");
            }catch(MissingResourceException ex){
                logln("Alias mechanism works for nonexisting alias");
            }
        }
        {
            rb = (ICUResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases");
            sub = rb.get("referencingalias");
            s1 = sub.getString();
            if(s1.equals("Hani")){
                logln("Alias mechanism works for referencingalias");
            }else{
                errln("Did not get the expected output for referencingalias");
            }
        }
        {
            rb = (ICUResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases");
            sub = rb.get("BreakDictionaryData");
            if(sub.getType()!=ICUResourceBundle.BINARY){
                errln("Did not get the expected type for BreakDictionaryData");   
            }
            if(sub.getBinary().remaining()>0){
                logln("Got the expected output for BreakDictionaryData");
            }else{
                errln("Did not get the expected type for BreakDictionaryData"); 
            }
            
        }
        {
            ICUResourceBundle rb1 = (ICUResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases");
            if(rb1!=rb){
                errln("Caching of the resource bundle failed");   
            }else{
                logln("Caching of resource bundle passed");   
            }
            sub = rb1.get("testGetStringByKeyAliasing" );
            
            s1 = sub.get("KeyAlias0PST").getString();
            if(s1.equals("PST")){
                logln("Alias mechanism works for KeyAlias0PST");
            }else{
                errln("Did not get the expected output for KeyAlias0PST");
            }
            
            s1 = sub.get("KeyAlias1PacificStandardTime").getString();
            if(s1.equals("Pacific Standard Time")){
                logln("Alias mechanism works for KeyAlias1PacificStandardTime");
            }else{
                errln("Did not get the expected output for KeyAlias1PacificStandardTime");
            }
            s1 = sub.get("KeyAlias2PDT").getString();
            if(s1.equals("PDT")){
                logln("Alias mechanism works for KeyAlias2PDT");
            }else{
                errln("Did not get the expected output for KeyAlias2PDT");
            }            
            
            s1 = sub.get("KeyAlias3LosAngeles").getString();
            if(s1.equals("Los Angeles")){
                logln("Alias mechanism works for KeyAlias3LosAngeles. Got: "+s1);
            }else{
                errln("Did not get the expected output for KeyAlias3LosAngeles. Got: "+s1);
            }
        }
        {
            sub = rb.get("testGetStringByIndexAliasing" );
            s1 = sub.getString(0);
            if(s1.equals("PST")){
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
        
        // should not get an exception
        rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(UResourceBundle.ICU_BASE_NAME,"fr_BE");
        ICUResourceBundle b = (ICUResourceBundle) rb.getObject("SpelloutRules");
        String str = b.getString();
        if(str !=null || str.length()>0){
            logln("Alias mechanism works");
        }else{
            errln("Alias mechanism failed for fr_BE SpelloutRules");
        }
        rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(UResourceBundle.ICU_COLLATION_BASE_NAME,"zh_TW");
        b = (ICUResourceBundle) rb.getObject("collations");
        if(b != null){
            if(b.get(0).getKey().equals( "default")){
                logln("Alias mechanism works");
            }else{
                errln("Alias mechanism failed for zh_TW collations");
            }
        }else{
            errln("Did not get the expected object for collations");
        }

    }
    public void TestAlias(){
        logln("Testing %%ALIAS");
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(UResourceBundle.ICU_BASE_NAME,"iw_IL");
        ICUResourceBundle b = (ICUResourceBundle) rb.getObject("NumberPatterns");
        if(b != null){
            if(b.getSize()>0){
                logln("%%ALIAS mechanism works");
            }else{
                errln("%%ALIAS mechanism failed for iw_IL collations");
            }
        }else{
            errln("%%ALIAS mechanism failed for iw_IL");
        }        
    }
    public void TestCircularAliases(){
        try{
            ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","testaliases");
            ICUResourceBundle sub = rb.get("aaa");
            String s1 = sub.getString();
            if(s1!=null){
                errln("Did not get the expected exception");   
            }
        }catch(IllegalArgumentException ex){
            logln("got expected exception for circular references");   
        }
    }    
}

