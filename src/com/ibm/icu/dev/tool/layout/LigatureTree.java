/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.impl.Utility;

public class LigatureTree
{
    static class Lignode
    {
        int target;
        int ligature = -1;
        Lignode[] subnodes = null;

        Lignode()
        {
            target = -1;
        }

        Lignode(int target)
        {
            this.target = target;
        }

        boolean isMatch()
        {
            return ligature != -1;
        }

        int getLigature()
        {
            return ligature;
        }

        Lignode subnode(int c)
        {
            if (subnodes != null) {
                int len = subnodes.length;

                if (c <= subnodes[len - 1].target) {
                    for (int i = 0; i < len; i+= 1) {
                        int t = subnodes[i].target;

                        if (t > c) {
                            return null;
                        }

                        if (t == c) {
                            return subnodes[i];
                        }
                    }
                }
            }

            return null;
        }

        String ligatureString(int[] chars)
        {
            StringBuffer result = new StringBuffer();
            int len = chars.length - 1;
            
            for (int i = 0; i < len; i += 1) {
                if (i > 0) {
                    result.append(" + ");
                }
                
                result.append(Utility.hex(chars[i], 6));
           }
            
            result.append(" => " + Utility.hex(chars[len], 6));
            
            return result.toString();
        }
        
        void insert(int[] chars, int index)
        {
            int c = chars[index];
            int len = chars.length;

            if (len == index + 1) {
                if (ligature != -1) {
                    System.out.println("ignoring ligature " + ligatureString(chars) +
                                       ": already have " + Utility.hex(ligature, 6));
                } else {
                    ligature = c;
                }

                return;
            }

            if (subnodes == null) {
                subnodes = new Lignode[1];
                subnodes[0] = new Lignode(c);
                subnodes[0].insert(chars, index + 1);
            } else {
                int i;

                for (i = 0; i < subnodes.length; i += 1)
                {
                    int t = subnodes[i].target;

                    if (t == c) {
                        subnodes[i].insert(chars, index + 1);
                        return;
                    } else if (t > c) {
                        break;
                    }
                }

                Lignode[] nnodes = new Lignode[subnodes.length + 1];

                if (i > 0) {
                    System.arraycopy(subnodes, 0, nnodes, 0, i);
                }

                nnodes[i] = new Lignode(c);

                if (i < subnodes.length) {
                    System.arraycopy(subnodes, i, nnodes, i + 1, subnodes.length - i);
                }

                subnodes = nnodes;

                subnodes[i].insert(chars, index + 1);
            }
        }
        
        public void walk(TreeWalker walker)
        {
            if (target != -1) {
                walker.down(target);
            }
            
            if (subnodes != null) {
                for (int i = 0; i < subnodes.length; i += 1)
                {
                    subnodes[i].walk(walker);
                }
            }
            
            if (ligature != -1) {
                walker.ligature(ligature);
            }
                
            walker.up();
        }

        static final String ind = "                                      ";

        /*
         * Write debugging information to w, starting at the provided indentation level.
         */
        public void dump(Writer w, int indent)
        {
            String tab = ind.substring(0, Math.min(indent, ind.length()));

            try {
                w.write(tab);
                if (target != -1) {
                    w.write(Utility.hex(target, 6));
                }
                
                if (ligature != -1)
                {
                    w.write(" --> ");
                    w.write(Utility.hex(ligature, 6));
                }
                
                w.write("\n");
                
                if (subnodes != null) {
                    w.write(tab);
                    w.write("{\n");
                    indent += 4;
                    
                    for (int i = 0; i < subnodes.length; i += 1) {
                        subnodes[i].dump(w, indent);
                    }
                    
                    w.write(tab);
                    w.write("}\n");
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }

    }

    private Lignode root = new Lignode();
    
    public LigatureTree()
    {
        // anything?
    }

    private int[] toIntArray(String s)
    {
        int count = UTF16.countCodePoint(s);
        int[] result = new int[count];
        
        for (int i = 0; i < count; i += 1) {
            result[i] = UTF16.charAt(s, i);
        }
        
        return result;
    }

    public void insert(String string)
    {
        root.insert(toIntArray(string), 0);
    }
    
    public void insert(int[] chars)
    {
        root.insert(chars, 0);
    }
    
    public void walk(TreeWalker walker)
    {
        root.walk(walker);
        walker.done();
    }
    
    public void dump()
    {
        PrintWriter pw = new PrintWriter(System.out);
        
        root.dump(pw, 0);
        pw.flush();
    }
}
