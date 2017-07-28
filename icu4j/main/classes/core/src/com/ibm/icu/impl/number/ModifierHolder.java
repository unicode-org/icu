// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.util.ArrayDeque;

public class ModifierHolder {
  private ArrayDeque<Modifier> mods = new ArrayDeque<Modifier>();

  // Using five separate fields instead of the ArrayDeque saves about 10ns at the expense of
  // worse code.
  // TODO: Decide which implementation to use.

  //    private Modifier mod1 = null;
  //    private Modifier mod2 = null;
  //    private Modifier mod3 = null;
  //    private Modifier mod4 = null;
  //    private Modifier mod5 = null;

  public ModifierHolder createCopy() {
    ModifierHolder copy = new ModifierHolder();
    copy.mods.addAll(mods);
    return copy;
  }

  public ModifierHolder clear() {
    //      mod1 = null;
    //      mod2 = null;
    //      mod3 = null;
    //      mod4 = null;
    //      mod5 = null;
    mods.clear();
    return this;
  }

  public void add(Modifier modifier) {
    //      if (mod1 == null) {
    //        mod1 = modifier;
    //      } else if (mod2 == null) {
    //        mod2 = modifier;
    //      } else if (mod3 == null) {
    //        mod3 = modifier;
    //      } else if (mod4 == null) {
    //        mod4 = modifier;
    //      } else if (mod5 == null) {
    //        mod5 = modifier;
    //      } else {
    //        throw new IndexOutOfBoundsException();
    //      }
    if (modifier != null) mods.addFirst(modifier);
  }

  public Modifier peekLast() {
    return mods.peekLast();
  }

  public Modifier removeLast() {
    return mods.removeLast();
  }

  public int applyAll(NumberStringBuilder string, int leftIndex, int rightIndex) {
    int addedLength = 0;
    //      if (mod5 != null) {
    //        addedLength += mod5.apply(string, leftIndex, rightIndex + addedLength);
    //        mod5 = null;
    //      }
    //      if (mod4 != null) {
    //        addedLength += mod4.apply(string, leftIndex, rightIndex + addedLength);
    //        mod4 = null;
    //      }
    //      if (mod3 != null) {
    //        addedLength += mod3.apply(string, leftIndex, rightIndex + addedLength);
    //        mod3 = null;
    //      }
    //      if (mod2 != null) {
    //        addedLength += mod2.apply(string, leftIndex, rightIndex + addedLength);
    //        mod2 = null;
    //      }
    //      if (mod1 != null) {
    //        addedLength += mod1.apply(string, leftIndex, rightIndex + addedLength);
    //        mod1 = null;
    //      }
    while (!mods.isEmpty()) {
      Modifier mod = mods.removeFirst();
      addedLength += mod.apply(string, leftIndex, rightIndex + addedLength);
    }
    return addedLength;
  }

  public int applyStrong(NumberStringBuilder string, int leftIndex, int rightIndex) {
    int addedLength = 0;
    while (!mods.isEmpty() && mods.peekFirst().isStrong()) {
      Modifier mod = mods.removeFirst();
      addedLength += mod.apply(string, leftIndex, rightIndex + addedLength);
    }
    return addedLength;
  }
}
