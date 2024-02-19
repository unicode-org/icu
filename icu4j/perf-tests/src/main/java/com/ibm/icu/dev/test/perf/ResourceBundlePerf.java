// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**********************************************************************
* Copyright (c) 2006-2008, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/

package com.ibm.icu.dev.test.perf;

import java.nio.ByteBuffer;
import java.util.ResourceBundle;

import com.ibm.icu.util.UResourceBundle;

public class ResourceBundlePerf extends PerfTest {
    
    private UResourceBundle icuRes  = null;
    private ResourceBundle javaRes = null;
    
    public static void main(String[] org_args) throws Exception {
      new ResourceBundlePerf().run(org_args);
    }
    
    protected void setup(String[] args) {
        icuRes = UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata", "testtypes");
        javaRes = ResourceBundle.getBundle("com.ibm.icu.dev.data.TestDataElements_testtypes");
    }    

    PerfTest.Function TestResourceBundleConstructionJava() {
        return new PerfTest.Function() {
            public void call() {
                javaRes = ResourceBundle.getBundle("com.ibm.icu.dev.data.TestDataElements_testtypes");
            }
        };
    }
    PerfTest.Function TestResourceBundleConstructionICU() {
        return new PerfTest.Function() {
            public void call() {
                UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata", "testtypes");
            }       
        };
    }
    
    PerfTest.Function TestEmptyArrayJava() {
        return new PerfTest.Function(){
            public void call(){
                String[] s = javaRes.getStringArray("emptyarray");
                if (s.length != 0) throw new Error ("not zero");
            }
        };
    }

    PerfTest.Function TestEmptyArrayICU() {
        return new PerfTest.Function(){
            public void call(){
                String[] s = icuRes.getStringArray("emptyarray");
                if (s.length != 0) throw new Error ("not zero");
            }
        };
    }
    
    class GetStringJava extends PerfTest.Function {
        String key;
        String expected;
        GetStringJava(String key, String expected) {
            this.key = key;
            this.expected = expected;
        }
        public void call() {
            String s = javaRes.getString(key);
            if (!s.equals(expected)) throw new Error("not equal");
        }
    }
    
    class GetStringIcu extends PerfTest.Function {
        String key;
        String expected;
        GetStringIcu(String key, String expected) {
            this.key = key;
            this.expected = expected;
        }
        public void call() {
            String s = icuRes.getString(key);
            if (!s.equals(expected)) throw new Error("not equal");
        }
    }
    
    PerfTest.Function TestZeroTestJava(){
        return new GetStringJava("zerotest", "abc\u0000def");
    }

    PerfTest.Function TestZeroTestICU(){
        return new GetStringIcu("zerotest", "abc\u0000def");
    }

    PerfTest.Function TestEmptyExplicitStringJava(){
        return new GetStringJava("emptyexplicitstring", "");
    }
    
    PerfTest.Function TestEmptyExplicitStringICU(){
        return new GetStringIcu("emptyexplicitstring", "");
    }

    PerfTest.Function TestEmptyStringJava(){
        return new GetStringJava("emptystring", "");
    }
    
    PerfTest.Function TestEmptyStringICU(){
        return new GetStringIcu("emptystring", "");
    }

    class GetIntJava extends PerfTest.Function {
        String key;
        int expected;
        GetIntJava(String key, int expected) {
            this.key = key;
            this.expected = expected;
        }
        public void call() {
            Integer t = (Integer) javaRes.getObject(key);
            if (t.intValue() != expected) throw new Error("not equal");
        }
    }
    
    class GetIntIcu extends PerfTest.Function {
        String key;
        int expected;
        GetIntIcu(String key, int expected) {
            this.key = key;
            this.expected = expected;
        }
        public void call() {
            UResourceBundle temp = icuRes.get(key);
            int t = temp.getInt();
            if (t != expected) throw new Error("not equal");
        }
    }

    PerfTest.Function TestGet123Java(){
        return new GetIntJava("onehundredtwentythree", 123);
    }
    
    PerfTest.Function TestGet123ICU(){
        return new GetIntIcu("onehundredtwentythree", 123);
    }

    PerfTest.Function TestGetEmptyIntJava(){
        return new GetIntJava("emptyint", 0);
    }
    
    PerfTest.Function TestGetEmptyIntICU(){
        return new GetIntIcu("emptyint", 0);
    }

    PerfTest.Function TestGetOneJava(){
        return new GetIntJava("one", 1);
    }
    
    PerfTest.Function TestGetOneICU(){
        return new GetIntIcu("one", 1);
    }
    
    PerfTest.Function TestGetMinusOneJava(){
        return new GetIntJava("minusone", -1);
    }
    
    PerfTest.Function TestGetMinusOneICU(){
        return new GetIntIcu("minusone", -1);
    }

    PerfTest.Function TestGetPlusOneJava(){
        return new GetIntJava("plusone", 1);
    }
    
    PerfTest.Function TestGetPlusOneICU(){
        return new GetIntIcu("plusone", 1);
    }

    PerfTest.Function TestGetMinusOneUintJava(){ // TODO: no equivalence?
        return new PerfTest.Function(){
            public void call(){
                Integer t = (Integer) javaRes.getObject("minusone");
                if (t.intValue() != -1 ) throw new Error("not equal");
            }
        };
    }
    
    PerfTest.Function TestGetMinusOneUintICU(){
        return new PerfTest.Function(){
            public void call(){
                UResourceBundle sub = icuRes.get("minusone");
                int t = sub.getUInt();
                if (t != 0xFFFFFFF) throw new Error("not equal");
            }
        };
    }
    

    class GetIvJava  extends PerfTest.Function {
        String key;
        int[] expected;
        GetIvJava(String key, int[] expected) {
            this.key = key;
            this.expected = expected;
        }
        public void call() {
            Integer[] iv = (Integer[]) javaRes.getObject(key);
            for (int i = 0; i < iv.length; i++){
                if (expected[i] != iv[i].intValue()) throw new Error("not equal"); 
            }
        }
    }
    
    class GetIvIcu  extends PerfTest.Function {
        String key;
        int[] expected;
        GetIvIcu(String key, int[] expected) {
            this.key = key;
            this.expected = expected;
        }
        public void call() {
            UResourceBundle temp = icuRes.get(key);
            int[] iv = temp.getIntVector();
            for (int i = 0; i < iv.length; i++){
                if (expected[i] != iv[i]) throw new Error("not equal"); 
            }
        }
    }

    PerfTest.Function TestGetIntegerArrayJava(){
        return new GetIvJava("integerarray", new int[]{1,2,3,-3,4,5,6,7});
    }
    
    PerfTest.Function TestGetIntegerArrayICU(){
        return new GetIvIcu("integerarray", new int[]{1,2,3,-3,4,5,6,7});
    }

    PerfTest.Function TestGetEmptyIntegerArrayJava(){
        return new GetIvJava("emptyintv", new int[0]);
    }
    
    PerfTest.Function TestGetEmptyIntegerArrayICU(){
        return new GetIvIcu("emptyintv", new int[0]);
    }

    
    class GetBinaryIcu extends PerfTest.Function {
        String key;
        int expected_len;
        GetBinaryIcu(String key, int expected_len) {
            this.key = key;
            this.expected_len = expected_len;
        }
        public void call() {
            UResourceBundle temp = icuRes.get(key);
            ByteBuffer got = temp.getBinary();
            if(got.remaining() != expected_len) throw new Error("not the expected len");
            for(int i=0; i< got.remaining(); i++){
              byte b = got.get();
              if (i != b) throw new Error("not equal");
            }
        }
    }

    class GetBinaryJava extends PerfTest.Function {
        String key;
        int expected_len;
        GetBinaryJava(String key, int expected_len) {
            this.key = key;
            this.expected_len = expected_len;
        }
        public void call() {
            ByteBuffer got = ByteBuffer.wrap((byte[])javaRes.getObject(key));
            if(got.remaining() != expected_len) throw new Error("not the expected len");
            for(int i=0; i< got.remaining(); i++){
              byte b = got.get();
              if (i != b) throw new Error("not equal");
            }
        }
    }

    PerfTest.Function TestGetBinaryTestICU(){
        return new GetBinaryIcu("binarytest", 15);
    }
    
    PerfTest.Function TestGetBinaryTestJava(){
        return new GetBinaryJava("binarytest", 15);
    }
    
    PerfTest.Function TestGetEmptyBinaryICU(){
        return new GetBinaryIcu("emptybin", 0);
    }
    
    PerfTest.Function TestGetEmptyBinaryJava(){
        return new GetBinaryJava("emptybin", 0);
    }

    class GetMenuJava extends PerfTest.Function {
        String key;
        String[] expected;
        GetMenuJava(String key, String[] expected) {
            this.key = key;
            this.expected = expected;
        }
        public void call() {
            int p = 0;
            Object[][] menus = (Object[][]) javaRes.getObject(key);
            int sizei = menus.length;
            for (int i=0; i<sizei; i++){
                String menu_name = (String) menus[i][0];
                Object[][] menu_items = (Object[][]) menus[i][1];
                if (!expected[p++].equals(menu_name)) throw new Error("not equal");
                
                int sizej = menu_items.length;
                for (int j=0; j< sizej; j++){
                    String itemKey = (String) menu_items[j][0];
                    String value = (String) menu_items[j][1];
                    if(!expected[p++].equals(itemKey)) throw new Error("not equal");
                    if(!expected[p++].equals(value)) throw new Error("not equal");
                }
            }
            
        }
    }
    
    class GetMenuIcu extends PerfTest.Function {
        String key;
        String[] expected;
        GetMenuIcu(String key, String[] expected) {
            this.key = key;
            this.expected = expected;
        }
        public void call() {
            int p = 0;
            UResourceBundle menus = icuRes.get(key);
            int sizei = menus.getSize();
            for (int i=0; i<sizei; i++){
                UResourceBundle menu = menus.get(i);
                String menu_name = menu.getKey();
                if (!expected[p++].equals(menu_name)) throw new Error("not equal");
                
                int sizej = menu.getSize();
                for (int j=0; j< sizej; j++){
                    UResourceBundle menu_item = menu.get(j);
                    String itemKey = menu_item.getKey();
                    String value = menu_item.getString();
                    if(!expected[p++].equals(itemKey)) throw new Error("not equal");
                    if(!expected[p++].equals(value)) throw new Error("not equal");
                }
            }
            
        }
    }

    PerfTest.Function TestGetMenuJava(){
        String[] expected = new String[]{"file", "exit", "Exit", "open", "Open", "save", "Save"};
        return new GetMenuJava("menu", expected);
    }
    
    PerfTest.Function TestGetMenuICU(){
        String[] expected = new String[]{"file", "exit", "Exit", "open", "Open", "save", "Save"};
        return new GetMenuIcu("menu", expected);
    }

    PerfTest.Function TestGetEmptyMenuJava(){
        return new GetMenuJava("emptytable", new String[]{});
    }
    
    PerfTest.Function TestGetEmptyMenuICU(){
        return new GetMenuIcu("emptytable", new String[]{});
    }
}
