/**
*******************************************************************************
* Copyright (C) 2002-2010, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.dev.tool.translit;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
/**
 * @author ram
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.7F
 */
public class WriteIndicCharts {
    
    public static void main(String[] args){
        writeIICharts();
    }
   
    
    static String header =  "<html>\n" +
                            "    <head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">"+
                            "           Inter-Indic Transliteration Comparison chart"+
                            "    </head>\n"+
                            "    <body bgcolor=#FFFFFF>\n"+
                            "         <table border=1 width=100% >\n"+
                            "            <tr>\n"+
                            "            <th width=9%>Inter-Indic</th>\n"+
                            "            <th width=9%>Latin</th>\n"+
                            "            <th width=9%>Devanagari</th>\n"+
                            "            <th width=9%>Bengali</th>\n"+
                            "            <th width=9%>Gurmukhi</th>\n"+
                            "            <th width=9%>Gujarati</th>\n"+
                            "            <th width=9%>Oriya</th>\n"+
                            "            <th width=9%>Tamil</th>\n"+
                            "            <th width=9%>Telugu</th>\n"+
                            "            <th width=9%>Kannada</th>\n"+
                            "            <th width=9%>Malayalam</th>\n"+
                            "            </tr>\n";
    static String footer =  "          </table>\n"+
                            "    </body>\n" +
                            "</html>\n";
                   
    static UnicodeSet deva = new UnicodeSet("[:deva:]"); 
    static UnicodeSet beng = new UnicodeSet("[:beng:]");
    static UnicodeSet gujr = new UnicodeSet("[:gujr:]");
    static UnicodeSet guru = new UnicodeSet("[:guru:]");
    static UnicodeSet orya = new UnicodeSet("[:orya:]");
    static UnicodeSet taml = new UnicodeSet("[:taml:]");
    static UnicodeSet telu = new UnicodeSet("[:telu:]");
    static UnicodeSet knda = new UnicodeSet("[:knda:]"); 
    static UnicodeSet mlym = new UnicodeSet("[:mlym:]");                      
    static UnicodeSet inter= new UnicodeSet("[\uE000-\uE082]");
    
    public static void writeIICharts(){
        try{
            Transliterator t1 = Transliterator.getInstance("InterIndic-Bengali");    
            Transliterator t2 = Transliterator.getInstance("InterIndic-Gurmukhi");
            Transliterator t3 = Transliterator.getInstance("InterIndic-Gujarati");
            Transliterator t4 = Transliterator.getInstance("InterIndic-Oriya");
            Transliterator t5 = Transliterator.getInstance("InterIndic-Tamil");
            Transliterator t6 = Transliterator.getInstance("InterIndic-Telugu");
            Transliterator t7 = Transliterator.getInstance("InterIndic-Kannada");
            Transliterator t8 = Transliterator.getInstance("InterIndic-Malayalam");
            Transliterator t9 = Transliterator.getInstance("InterIndic-Devanagari");
            Transliterator t10 = Transliterator.getInstance("InterIndic-Latin");
            //UnicodeSetIterator sIter = new UnicodeSetIterator(deva);
            
            for(int i=0x00;i<=0x80;i++){
               String[] arr =  new String[10];
               arr[0]=UTF16.valueOf(i+ 0xE000);
               table.put(UTF16.valueOf(i),arr);
            }
            
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream("comparison-chart.html"),"UTF-8");
            
            os.write(header);

            writeIICharts(t9,0x0900,1);
            writeIICharts(t1,0x0980,2);        
            writeIICharts(t2,0x0A00,3); 
            writeIICharts(t3,0x0A80,4); 
            writeIICharts(t4,0x0B00,5); 
            writeIICharts(t5,0x0B80,6); 
            writeIICharts(t6,0x0c00,7); 
            writeIICharts(t7,0x0C80,8); 
            writeIICharts(t8,0x0D00,9); 

            for(int i=0x00;i<=0x80;i++){
                String[] temp = (String[])table.get(UTF16.valueOf(i));
                boolean write = false;
                for(int k=1;k<temp.length && temp[k]!=null;k++){
                    if(UCharacter.getExtendedName(UTF16.charAt(temp[k],0)).indexOf("unassigned")<0 ||
                       temp[k].indexOf(":UNASSIGNED")<0){
                        write = true;
                    }
                }
                if(write){
                    os.write("        <tr>\n");
                    for(int j=0; j<temp.length;j++){
                        if(temp[j]!=null){
                            boolean fallback=false;
                            boolean unassigned=false;
                            boolean unmapped = false;
                            boolean consumed =false;
                            String str = temp[j];
                                                        
                            if(temp[j].indexOf(":FALLBACK")>=0){
                                str = temp[j].substring(0,temp[j].indexOf(":"));
                                fallback=true;
                               // os.write("            <td bgcolor=#FFFF00 align=center title=\""++"\">"+str+"<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                            }
                            if(temp[j].indexOf(":UNASSIGNED")>=0){
                                str = temp[j].substring(0,temp[j].indexOf(":"));
                                unassigned=true;
                            }
                            
                            if(temp[j].indexOf(":UNMAPPED")>=0){
                                str = temp[j].substring(0,temp[j].indexOf(":"));
                                unmapped=true;
                            }
                            if(temp[j].indexOf(":CONSUMED")>=0){
                                str = temp[j].substring(0,temp[j].indexOf(":"));
                                consumed=true;
                            }
                            
                            String name;
                            StringBuffer nameBuf=new StringBuffer(); 
                            for(int f=0; f<str.length();f++){
                                if(f>0){ nameBuf.append("+");}
                                nameBuf.append(UCharacter.getExtendedName(UTF16.charAt(str,f)));
                            }
                            name = nameBuf.toString();   
                            if(fallback){

                                if(UCharacter.getExtendedName(UTF16.charAt(str,0)).indexOf("unassigned")>0){
                                    os.write("            <td  width=9% bgcolor=#BBBBFF align=center title=\""+name+"\">"+"&nbsp<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                                }else{
                                    os.write("            <td width=9% bgcolor=#BBBBFF align=center title=\""+name+"\">"+ str+"<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                                }
                            }else if(unmapped){
                                os.write("            <td bgcolor=#FF9999 align=center title=\""+name+"\">"+"&nbsp<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                            }else if(unassigned){
                                if(UCharacter.getExtendedName(UTF16.charAt(str,0)).indexOf("unassigned")>0){
                                    os.write("            <td width=9% bgcolor=#00FFFF align=center title=\""+name+"\">"+"&nbsp<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                                }else{
                                    os.write("            <td width=9% bgcolor=#00FFFF align=center title=\""+name+"\">"+ str+"<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                                }
                            }else if(consumed){
                                 if(UCharacter.getExtendedName(UTF16.charAt(str,0)).indexOf("unassigned")>0){
                                    os.write("            <td width=9% bgcolor=#FFFF55 align=center title=\""+name+"\">"+"&nbsp<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                                }else{
                                    os.write("            <td width=9% bgcolor=#FFFF55 align=center title=\""+""+"\">"+"&nbsp<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                                }
                            }else if(name.indexOf("private")!=-1){
                                String s = t10.transliterate(str);
                                os.write("            <td width=9% bgcolor=#FFBBBB  align=center title=\""+name+"\">"+"&nbsp<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                                if(!s.equals(str)){
                                    os.write("            <td width=9%  bgcolor=#CCEEDD align=center>"+s +"</td>");
                                }else{
                                    os.write("            <td width=9% bgcolor=#CCEEDD align=center>&nbsp;</td>");
                                }
                            }else{
                               os.write("            <td width=9% align=center title=\""+name+"\">"+ str+"<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                            } 
                        }else{
                             os.write("           <td width=9% >&nbsp</td>\n");
                        }
                    }
                    os.write("        </tr>\n");
                }
            }
            os.write(footer);
            os.close();
        }catch( Exception e){
            e.printStackTrace();
        }
    }
    public static void writeCharts(){
        try{
            Transliterator t1 = Transliterator.getInstance("InterIndic-Bengali");    
            Transliterator t2 = Transliterator.getInstance("InterIndic-Gurmukhi");
            Transliterator t3 = Transliterator.getInstance("InterIndic-Gujarati");
            Transliterator t4 = Transliterator.getInstance("InterIndic-Oriya");
            Transliterator t5 = Transliterator.getInstance("InterIndic-Tamil");
            Transliterator t6 = Transliterator.getInstance("InterIndic-Telugu");
            Transliterator t7 = Transliterator.getInstance("InterIndic-Kannada");
            Transliterator t8 = Transliterator.getInstance("InterIndic-Malayalam");
            Transliterator t9 = Transliterator.getInstance("InterIndic-Devanagari");
            
            //UnicodeSetIterator sIter = new UnicodeSetIterator(deva);
            
            for(int i=0x0900;i<=0x097F;i++){
               String[] arr =  new String[10];
               arr[0]=UTF16.valueOf((i&0xFF) + 0xE000);
               table.put(UTF16.valueOf(i),arr);
            }
            
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream("comparison-chart.html"),"UTF-8");
            
            os.write(header);
            /*
            writeCharts(t1,beng,1);        
            writeCharts(t2,guru,2); 
            writeCharts(t3,gujr,3); 
            writeCharts(t4,orya,4); 
            writeCharts(t5,taml,5); 
            writeCharts(t6,telu,6); 
            writeCharts(t7,knda,7); 
            writeCharts(t8,mlym,8); 
            */
            /*
            writeCharts(t9,0x0900,1);
            writeCharts(t1,0x0980,2);        
            writeCharts(t2,0x0A00,3); 
            writeCharts(t3,0x0A80,4); 
            writeCharts(t4,0x0B00,5); 
            writeCharts(t5,0x0B80,6); 
            writeCharts(t6,0x0c00,7); 
            writeCharts(t7,0x0C80,8); 
            writeCharts(t8,0x0D00,9); 
            */
            writeIICharts(t9,0x0900,1);
            writeIICharts(t1,0x0980,2);        
            writeIICharts(t2,0x0A00,3); 
            writeIICharts(t3,0x0A80,4); 
            writeIICharts(t4,0x0B00,5); 
            writeIICharts(t5,0x0B80,6); 
            writeIICharts(t6,0x0c00,7); 
            writeIICharts(t7,0x0C80,8); 
            writeIICharts(t8,0x0D00,9); 
            for(int i=0x0900;i<=0x097F;i++){
                String[] temp = (String[])table.get(UTF16.valueOf(i));
                boolean write = false;
                for(int k=1;k<temp.length;k++){
                    if(UCharacter.getExtendedName(UTF16.charAt(temp[k],0)).indexOf("unassigned")<0){
                        write = true;
                    }
                }
                if(write){
                    os.write("        <tr>\n");
                    for(int j=0; j<temp.length;j++){
                        if(temp[j]!=null){
                            boolean fallback=false;
                            String str = temp[j];
                            
                            if(temp[j].indexOf(":FALLBACK")>=0){
                                str = temp[j].substring(0,temp[j].indexOf(":"));
                                fallback=true;
                               // os.write("            <td bgcolor=#FFFF00 align=center title=\""++"\">"+str+"<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                            }
                            String name = UCharacter.getExtendedName(UTF16.charAt(str,0));
                            if(fallback){
                                os.write("            <td bgcolor=#BBBBFF align=center title=\""+name+"\">"+ str+"<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                            }else if(name.indexOf("unassigned")!=-1){
                                os.write("            <td bgcolor=#CCCCCC align=center title=\""+name+"\">"+"&nbsp<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                            }else if(name.indexOf("private")!=-1){

                                
                                os.write("            <td bgcolor=#FFBBBB align=center title=\""+name+"\">"+"&nbsp<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");

                            }else{
                               os.write("            <td align=center title=\""+name+"\">"+ str+"<br><tt>"+Utility.hex(str)+"</tt>"+"</td>\n");
                            } 
                        }else{
                             os.write("           <td>&nbsp</td>\n");
                        }
                    }
                    os.write("        </tr>\n");
                }
            }
            os.write(footer);
            os.close();
        }catch( Exception e){
            e.printStackTrace();
        }
    }
    static Hashtable table = new Hashtable();
    static String getKey(int cp){
        int delta = cp & 0xFF;
        delta-= (delta>0x7f)? 0x80 : 0;
        //delta+=0x0900;
        return UTF16.valueOf(delta);
    }
    
    public static void writeCharts(Transliterator trans, int start, int index){
        
        Transliterator inverse = trans.getInverse();
        for(int i=0;i<=0x7f;i++){
            String cp = UTF16.valueOf(start+i);
            String s1 = inverse.transliterate(cp);
            String s2 = trans.transliterate(s1);
            
            String[] arr = (String[])table.get(getKey(start+i));
            if(cp.equals(s2)){
                arr[index] = s1;
            }else{
                arr[index] = s1 + ":FALLBACK";
            }
        }
    }
    
    public static void writeIICharts(Transliterator trans,int start, int index){
        
        Transliterator inverse = trans.getInverse();
        UnicodeSetIterator iter = new UnicodeSetIterator(inter);
        
        while(iter.next()){
            String cp =UTF16.valueOf(iter.codepoint);
            String s1 = trans.transliterate(cp);
            String s2 = inverse.transliterate(s1);
            String[] arr = (String[])table.get(UTF16.valueOf(iter.codepoint&0xFF));
            if(cp.equals(s1)){
                arr[index] = UTF16.valueOf(start+(((byte)iter.codepoint)&0xFF))+":UNASSIGNED";
            }else if(cp.equals(s2)){
                arr[index] = s1;
            }else if(s1.equals(s2)){
                if(s1.equals("")){
                    arr[index] = UTF16.valueOf(start+(((byte)iter.codepoint)&0xFF))+":CONSUMED";
                }else{
                    arr[index] = s1+ ":FALLBACK";
                }
            } else{
                if(s2.equals("")){
                    arr[index] = UTF16.valueOf(start+(((byte)iter.codepoint)&0xFF))+":CONSUMED";
                }else{
                    arr[index] = s1+ ":FALLBACK";
                }
            }
        }
    }
    public static void writeCharts(Transliterator trans, UnicodeSet target, int index){
        UnicodeSetIterator tIter = new UnicodeSetIterator(target);
        Transliterator inverse = trans.getInverse();
        while(tIter.next()){
            String cp = UTF16.valueOf(tIter.codepoint);
            String s1 = inverse.transliterate(cp);
            String s2 = trans.transliterate(s1);
            
            String[] arr = (String[])table.get(getKey(tIter.codepoint));
            if(cp.equals(s2)){
                arr[index] = cp;
            }else{
                arr[index] = cp + ":FALLBACK";
            }
        }
    }
}

