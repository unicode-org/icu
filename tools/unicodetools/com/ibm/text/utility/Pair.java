/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/Pair.java,v $
* $Date: 2001/09/19 23:33:52 $
* $Revision: 1.3 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

public final class Pair implements java.lang.Comparable, Cloneable {

  public Comparable first, second;

  public Pair (Comparable first, Comparable second) {
    this.first = first;
    this.second = second;
  }

  public int hashCode() {
    return first.hashCode() * 37 + second.hashCode();
  }

  public boolean equals(Object other) {
    try {
      Pair that = (Pair)other;
      return first.equals(that.first) && second.equals(that.second);
    } catch (Exception e) {
      return false;
    }
  }

    public int compareTo(Object other) {
        Pair that = (Pair)other;
        int trial = first.compareTo(that.first);
        if (trial != 0) return trial;
        return second.compareTo(that.second);
    }
    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}