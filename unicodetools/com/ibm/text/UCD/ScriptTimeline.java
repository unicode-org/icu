package com.ibm.text.UCD;

import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

import java.util.List;

public class ScriptTimeline {
  public static void main(String[] args) {
    String[] versions = { "2.0.0", "2.1.2", "3.0.0", "3.1.0", "3.2.0", "4.0.0", "4.1.0", "5.0.0" };
    for (int s = 0; s < UScript.CODE_LIMIT; ++s) {
      String scriptName = UScript.getName(s);
      UnicodeSet chars = new UnicodeSet().applyPropertyAlias("script", scriptName);
      if (chars.size() == 0) continue;
      System.out.print(scriptName);
      for (int v = 0; v < versions.length; ++v) {
        UnicodeSet age = new UnicodeSet();
        age.applyPropertyAlias("age", versions[v]);
        System.out.print("\t" + new UnicodeSet(chars).retainAll(age).size());
      }
      System.out.println();
    }
  }
}