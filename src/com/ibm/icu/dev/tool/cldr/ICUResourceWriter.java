/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package com.ibm.icu.dev.tool.cldr;

import java.io.OutputStream;

import com.ibm.icu.text.UTF16;

/**
 * @author ram
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ICUResourceWriter {
    private static final String CHARSET         = "UTF-8";
    private static final String OPENBRACE       = "{";
    private static final String CLOSEBRACE      = "}";
    private static final String COLON           = ":";
    private static final String COMMA           = ",";
    private static final String QUOTE           = "\"";
    private static final String COMMENTSTART    = "/**";
    private static final String COMMENTEND      = " */";
    private static final String COMMENTMIDDLE   = " * ";
    private static final String SPACE           = " ";
    private static final String INDENT          = "    ";
    private static final String EMPTY           = "";
    private static final String STRINGS         = "string";
    private static final String BIN             = "bin";
    private static final String INTS            = "int";
    private static final String TABLE           = "table";
    private static final String IMPORT          = "import";
    private static final String INCLUDE         = "include";
    private static final String ALIAS           = "alias";
    private static final String INTVECTOR       = "intvector";
    //private static final String ARRAYS          = "array";
    private static final String LINESEP         = System.getProperty("line.separator");
    
    public static class Resource{
        String[] note = new String[20];
        int noteLen = 0;
        String translate;
        String comment;
        String name;
        Resource next;
        public StringBuffer escapeSyntaxChars(String val){
            // escape the embedded quotes
            if(val==null) {
                System.err.println("Resource.escapeSyntaxChars: warning, resource '" + name + "': string value is NULL - assuming 'empty'");
                return new StringBuffer("");
            }
            char[] str = val.toCharArray();
            StringBuffer result = new StringBuffer();
            for(int i=0; i<str.length; i++){
                switch (str[i]){
                    case '\u0022':
                        result.append('\\'); //append backslash
                    default:
                        result.append(str[i]);
                }      
            }
            return result;
        }
        public void write(OutputStream writer, int numIndent, boolean bare){
            while(next!=null){
                next.write(writer, numIndent+1, false);
            }
        }
        public void writeIndent(OutputStream writer, int numIndent){
            for(int i=0; i< numIndent; i++){
                write(writer,INDENT);
            }
        }
    
        public void write(OutputStream writer, String value){
            try {
                byte[] bytes = value.getBytes(CHARSET);
                writer.write(bytes, 0, bytes.length);
                
            } catch(Exception e) {
                System.err.println(e);
                System.exit(1);
            }
        }
        public void writeComments(OutputStream writer, int numIndent){
            if(comment!=null || translate != null || noteLen > 0){
                // print the start of the comment
                writeIndent(writer, numIndent);
                write(writer, COMMENTSTART+LINESEP);
                
                // print comment if any
                if(comment!=null){
                    writeIndent(writer, numIndent);
                    write(writer, COMMENTMIDDLE);
                    write(writer, comment);
                    write(writer, LINESEP);
                }
                  
                // terminate the comment
                writeIndent(writer, numIndent);
                write(writer, COMMENTEND+LINESEP);
            }          
        }
        public void sort(){
            //System.out.println("In sort");
            return;
        }
        public void swap(){
            return;
        }
    }
    
    public static class  ResourceAlias extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+ALIAS+ OPENBRACE+QUOTE+escapeSyntaxChars(val)+QUOTE+CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name!");
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }
    }
    public static class  ResourceInclude extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+INCLUDE+ OPENBRACE+QUOTE+escapeSyntaxChars(val)+QUOTE+CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name!");
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }
    }
    public static class  ResourceImport extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+IMPORT+ OPENBRACE+QUOTE+escapeSyntaxChars(val)+QUOTE+CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name!");
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }
    }
    public static class  ResourceArray extends Resource{
        Resource first;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            if(name!=null){
                write(writer, name+OPENBRACE+LINESEP);
            }else{
                write(writer, OPENBRACE+LINESEP);
            }
            numIndent++;
            Resource current = first;
            while(current != null){
                current.write(writer, numIndent, true);
                if(current instanceof ResourceTable ||
                   current instanceof ResourceArray){
                    
                }else{
                    write(writer, COMMA+LINESEP);
                }
                current = current.next;
            }
            numIndent--;
            writeIndent(writer, numIndent);
            write(writer, CLOSEBRACE+LINESEP);
        }
        public void sort(){
            Resource current = first;
            while(current!=null){
                current.sort();
                current = current.next;
            }
        }
    }
    
    public static class ResourceBinary extends Resource{
        String internal;
        String external;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            if(internal==null){
                String line = ((name==null) ? EMPTY : name)+COLON+IMPORT+ OPENBRACE+QUOTE+external+QUOTE+CLOSEBRACE + ((bare==true) ?  EMPTY : LINESEP);
                write(writer, line);
            }else{
                String line = ((name==null) ? EMPTY : name)+COLON+BIN+ OPENBRACE+internal+CLOSEBRACE+ ((bare==true) ?  EMPTY : LINESEP);
                write(writer,line);
            }
            
        }
    }
    
    public static class ResourceInt extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            String line =  ((name==null)? EMPTY: name)+COLON+INTS+ OPENBRACE + val +CLOSEBRACE;
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name!");
                }
                write(writer,line); 
            }else{
                write(writer, line+LINESEP);
            }
        }
    }
    
    public static class ResourceIntVector extends Resource{
        ResourceInt first;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            write(writer, name+COLON+INTVECTOR+OPENBRACE+LINESEP);
            numIndent++;
            ResourceInt current = (ResourceInt) first;
            while(current != null){
                //current.write(writer, numIndent, true);
                writeIndent(writer, numIndent);
                write(writer, current.val);
                write(writer, COMMA+LINESEP);
                current = (ResourceInt) current.next;
            }
            numIndent--;
            writeIndent(writer, numIndent);
            write(writer, CLOSEBRACE+LINESEP);
        }
    }
    
    public static class ResourceString extends Resource{
        String val;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            if(bare==true){
                if(name!=null){
                    throw new RuntimeException("Bare option is set to true but the resource has a name!");
                }
                
                write(writer,QUOTE+escapeSyntaxChars(val)+QUOTE); 
            }else{
                StringBuffer str = escapeSyntaxChars(val);
                
                int colLen = 80-(numIndent*4);
                int strLen = str.length();
                if(strLen>colLen){
                    int startIndex = 0;
                    int endIndex = 0;
                    write(writer, name + OPENBRACE +LINESEP);
                    numIndent++;
                    boolean isRules = name.equals("Sequence"); 
                    // Find a safe point where we can insert a line break!
                    while(endIndex < strLen){
                        startIndex = endIndex;
                        endIndex = startIndex+colLen;
                        if(endIndex>strLen){
                            endIndex = strLen;
                        }
                        if(isRules){
                            // look for the reset tag only if we are writing 
                            // collation rules!
                            int firstIndex = str.indexOf("&", startIndex);
                            
                            if(firstIndex >-1 ){
                                if(startIndex!= (firstIndex -1)&& startIndex!=firstIndex && firstIndex < endIndex ){
                                    if(str.charAt(firstIndex-1)!=0x27){
                                        endIndex = firstIndex;
                                    }
                                }
                                int nextIndex  = 0;
                                while((nextIndex = str.indexOf( "&", firstIndex+1))!=-1 && nextIndex < endIndex){
                                    
                                    if(nextIndex>-1 && firstIndex!=nextIndex){
                                        if(str.charAt(nextIndex-1)!=0x27){
                                            endIndex = nextIndex;
                                            break;
                                        }else {
                                            firstIndex = nextIndex;
                                        }
                                    }
                                }
                            }
                        }
                        int indexOfEsc = 0;
                        if((indexOfEsc =str.lastIndexOf("\\u",endIndex))>-1 && (endIndex-indexOfEsc)<6 ||
                           (indexOfEsc = str.lastIndexOf("\\U",endIndex))>-1 && (endIndex-indexOfEsc)<10 ||
                           (indexOfEsc = str.lastIndexOf("'\'",endIndex))>-1 && (endIndex-indexOfEsc)<3){
                            
                            endIndex = indexOfEsc;
                        }
                        if(indexOfEsc > -1 && str.charAt(indexOfEsc-1)==0x0027){
                            endIndex = indexOfEsc-1;
                        }
                        if(endIndex<strLen && UTF16.isLeadSurrogate(str.charAt(endIndex-1))){
                            endIndex--;
                        }
                        
                        writeIndent(writer, numIndent);
                        write(writer, QUOTE);
                        write(writer, str.substring(startIndex,endIndex));
                        write(writer, QUOTE+LINESEP);
                    }
                    numIndent--;
                    writeIndent(writer, numIndent);
                    write(writer,CLOSEBRACE + LINESEP);
                    
                }else{
                    write(writer,name + OPENBRACE + QUOTE + str.toString() + QUOTE + CLOSEBRACE + LINESEP);
                }
                
            }
        }
    }
    
    public static class ResourceTable extends Resource{
        Resource first;
        public void write(OutputStream writer, int numIndent, boolean bare){
            writeComments(writer, numIndent);
            writeIndent(writer, numIndent);
            write(writer, name+OPENBRACE+LINESEP);
            numIndent++;
            Resource current = first;
            while(current != null){
                current.write(writer, numIndent, false);
                current = current.next;
            }
            numIndent--;
            writeIndent(writer, numIndent);
            write(writer, CLOSEBRACE+LINESEP);
        }
        // insertion sort of the linked list
        // from Algorithms in C++ Sedgewick
        public void sort(){
           // System.out.println("Entering sort of table");
            Resource b =new Resource();
            Resource a = first;
            Resource t,u,x;
            for(t = a; t!=null; t=u ){
                u=t.next;
                for(x=b;x.next!=null; x=x.next){
                    if(x.next.name.compareTo(t.name)>0){
                        break;
                    }
                }
                t.next = x.next;
                x.next = t;
            }
           // System.out.println("Exiting sort of table");
            if(b.next!=null){
                first = b.next;   
            }
            
            Resource current = first;
            while(current!=null){
                current.sort();
                current = current.next;
            }
            
        } // end sort()
    }
}
