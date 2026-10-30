// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.collator;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;

public abstract class CollationTestBase extends TestFmwk {

    void doTest(TestFmwk test, RuleBasedCollator col, String source, String target, int result) {
        doTestVariant(test, col, source, target, result);
        if (result == -1) {
            doTestVariant(test, col, target, source, 1);
        } else if (result == 1) {
            doTestVariant(test, col, target, source, -1);
        } else {
            doTestVariant(test, col, target, source, 0);
        }

        CollationElementIterator iter = col.getCollationElementIterator(source);
        backAndForth(test, iter);
        iter.setText(target);
        backAndForth(test, iter);
    }

    /**
     * Return an integer array containing all of the collation orders returned by calls to next on
     * the specified iterator
     */
    static int[] getOrders(CollationElementIterator iter) {
        int maxSize = 100;
        int size = 0;
        int[] orders = new int[maxSize];

        int order;
        while ((order = iter.next()) != CollationElementIterator.NULLORDER) {
            if (size == maxSize) {
                maxSize *= 2;
                int[] temp = new int[maxSize];
                System.arraycopy(orders, 0, temp, 0, size);
                orders = temp;
            }
            orders[size++] = order;
        }

        if (maxSize > size) {
            int[] temp = new int[size];
            System.arraycopy(orders, 0, temp, 0, size);
            orders = temp;
        }
        return orders;
    }

    void backAndForth(TestFmwk test, CollationElementIterator iter) {
        // Run through the iterator forwards and stick it into an array
        iter.reset();
        int[] orders = getOrders(iter);

        // Now go through it backwards and make sure we get the same values
        int index = orders.length;
        int o;

        // reset the iterator
        iter.reset();

        while ((o = iter.previous()) != CollationElementIterator.NULLORDER) {
            if (o != orders[--index]) {
                if (o == 0) {
                    index++;
                } else {
                    while (index > 0 && orders[index] == 0) {
                        index--;
                    }
                    if (o != orders[index]) {
                        errln(
                                "Mismatch at index "
                                        + index
                                        + ": 0x"
                                        + Utility.hex(orders[index])
                                        + " vs 0x"
                                        + Utility.hex(o));
                        break;
                    }
                }
            }
        }

        while (index != 0 && orders[index - 1] == 0) {
            index--;
        }

        if (index != 0) {
            StringBuilder msg = new StringBuilder();
            msg.append("Didn't get back to beginning - index is ").append(index).append("\n");

            iter.reset();
            msg.append("next: ");
            while ((o = iter.next()) != CollationElementIterator.NULLORDER) {
                String hexString = "0x" + Utility.hex(o) + " ";
                msg.append(hexString);
            }
            msg.append("\n");
            msg.append("prev: ");
            while ((o = iter.previous()) != CollationElementIterator.NULLORDER) {
                String hexString = "0x" + Utility.hex(o) + " ";
                msg.append(hexString);
            }
            errln(msg.toString());
        }
    }

    static final String appendCompareResult(int result, String target) {
        if (result == -1) {
            target += "LESS";
        } else if (result == 0) {
            target += "EQUAL";
        } else if (result == 1) {
            target += "GREATER";
        } else {
            String huh = "?";
            target += huh + result;
        }
        return target;
    }

    static final String prettify(CollationKey key) {
        byte[] bytes = key.toByteArray();
        return prettify(bytes, bytes.length);
    }

    static final String prettify(RawCollationKey key) {
        return prettify(key.bytes, key.size);
    }

    static final String prettify(byte[] skBytes, int length) {
        StringBuilder target = new StringBuilder(length * 3 + 2).append('[');

        for (int i = 0; i < length; i++) {
            String numStr = Integer.toHexString(skBytes[i] & 0xff);
            if (numStr.length() < 2) {
                target.append('0');
            }
            target.append(numStr).append(' ');
        }
        target.append(']');
        return target.toString();
    }

    private void doTestVariant(
            TestFmwk test,
            RuleBasedCollator myCollation,
            String source,
            String target,
            int result) {
        int compareResult = myCollation.compare(source, target);
        if (compareResult != result) {

            // !!! if not mod build, error, else nothing.
            // warnln if not build, error, else always print warning.
            // do we need a 'quiet warning?' (err or log).  Hmmm,
            // would it work to have the 'verbose' flag let you
            // suppress warnings?  Are there ever some warnings you
            // want to suppress, and others you don't?
            errln(
                    "Comparing \""
                            + Utility.hex(source)
                            + "\" with \""
                            + Utility.hex(target)
                            + "\" expected "
                            + result
                            + " but got "
                            + compareResult);
        }
        CollationKey ssk = myCollation.getCollationKey(source);
        CollationKey tsk = myCollation.getCollationKey(target);
        compareResult = ssk.compareTo(tsk);
        if (compareResult != result) {
            errln(
                    "Comparing CollationKeys of \""
                            + Utility.hex(source)
                            + "\" with \""
                            + Utility.hex(target)
                            + "\" expected "
                            + result
                            + " but got "
                            + compareResult);
        }
        RawCollationKey srsk = new RawCollationKey();
        myCollation.getRawCollationKey(source, srsk);
        RawCollationKey trsk = new RawCollationKey();
        myCollation.getRawCollationKey(target, trsk);
        compareResult = ssk.compareTo(tsk);
        if (compareResult != result) {
            errln(
                    "Comparing RawCollationKeys of \""
                            + Utility.hex(source)
                            + "\" with \""
                            + Utility.hex(target)
                            + "\" expected "
                            + result
                            + " but got "
                            + compareResult);
        }
    }
}
