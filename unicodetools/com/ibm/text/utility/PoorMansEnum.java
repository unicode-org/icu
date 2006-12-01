/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/PoorMansEnum.java,v $
* $Date: 2002/10/05 01:28:57 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

/* Goal for enum is:
 * Easy to use
 * ID <-> int
 * ID <-> string name
 */
package com.ibm.text.utility;

import java.util.*;

public class PoorMansEnum {
    protected int value;
    protected String name;
    protected PoorMansEnum next;

    public int toInt() {
        return value;
    }

    public String toString() {
        return name;
    }

    // for subclassers

    protected PoorMansEnum() {
    }

    /** Utility for subclasses
    */
    protected static class EnumStore {
        private List int2Id = new ArrayList();
        private Map string2Id = new HashMap();
        private PoorMansEnum last = null;

        public PoorMansEnum add(PoorMansEnum id, String name) {
            // both string and id must be new!
            if (int2Id.indexOf(id) >= 0) {
                throw new IllegalArgumentException("ID already stored for \"" + name + '"');
            } else if (string2Id.containsKey(name)) {
                throw new IllegalArgumentException('"' + name + "\" already stored for ID ");
            }
            id.value = int2Id.size();
            id.name = name;
            if (last != null) {
                last.next = id;
            }
            int2Id.add(id);
            string2Id.put(name, id);
            last = id;
            return id;
        }

        public PoorMansEnum addAlias(PoorMansEnum id, String name) {
            // id must be old, string must be new
            if (int2Id.indexOf(id) < 0) {
                throw new IllegalArgumentException("ID must already be stored for \"" + name + '"');
            } else if (string2Id.containsKey(name)) {
                throw new IllegalArgumentException('"' + name + "\" already stored for ID ");
            }
            string2Id.put(name, id);
            return id;
        }
        
        public Collection getAliases(PoorMansEnum id, Collection output) {
            Iterator it = string2Id.keySet().iterator();
            while (it.hasNext()) {
                Object s = it.next();
                if (s == id.name) continue;
                if (id == string2Id.get(s)) output.add(s);
            }
            return output;
        }
        
        public int getMax() {
            return int2Id.size();
        }

        public PoorMansEnum get(int value) {
            return (PoorMansEnum) int2Id.get(value);
        }

        public PoorMansEnum get(String name) {
            return (PoorMansEnum) string2Id.get(name);
        }
    }
}