/*
 *******************************************************************************
 * Copyright (C) 2002-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;

import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

abstract public class Pick {
    private static boolean DEBUG = false;
    
    // for using to get strings
    
    static class Target {
        private Pick pick;
        private Random random;
        private Quoter quoter;
        
        public static Target make(Pick pick, Random random, Quoter quoter) {
            Target result = new Target();
            result.pick = pick;
            result.random = random;
            result.quoter = quoter;
            return result;
        }
        public String next() {
            quoter.clear();
            pick.addTo(this);
            return get();
        }        
        public String get() {
            return quoter.toString();
        }
        private void copyState(Target other) {
            random = other.random;
        }
        private void clear() {
            quoter.clear();
        }
        /*private int length() {
            return quoter.length();
        }*/
        private Target append(int codepoint) {
            quoter.append(codepoint);
            return this;
        }
        private Target append(String s) {
            quoter.append(s);
            return this;
        }
        // must return value between 0 (inc) and 1 (exc)
        private double nextDouble() {
            return random.nextDouble();
        }
    }

    // for Building
    
    public Pick replace(String toReplace, Pick replacement) {
       Replacer visitor = new Replacer(toReplace, replacement);
       return visit(visitor);
    }

    public Pick name(String nameStr) {
        name = nameStr;
        return this;
    }
    
    static public Pick.Sequence makeSequence() {
        return new Sequence();
    }
    static public Pick.Alternation makeAlternation() {
        return new Alternation();
    }
    /*
    static public Pick.Sequence and(Object item) {
        return new Sequence().and2(item);
    }
    static public Pick.Sequence and(Object[] items) {
        return new Sequence().and2(items);
    }
    static public Pick.Alternation or(int itemWeight, Object item) {
        return new Alternation().or2(itemWeight, item);
    }
    static public Pick.Alternation or(Object[] items) {
        return new Alternation().or2(1, items);
    }
    static public Pick.Alternation or(int itemWeight, Object[] items) {
        return new Alternation().or2(itemWeight, items);
    }
    static public Pick.Alternation or(int[] itemWeights, Object[] items) {
        return new Alternation().or2(itemWeights, items);
    }
    
    static public Pick maybe(int percent, Object item) {
        return new Repeat(0, 1, new int[]{100-percent, percent}, item);
        //return Pick.or(1.0-percent, NOTHING).or2(percent, item);
    }
    static public Pick repeat(int minCount, int maxCount, int itemWeights, Object item) {
        return new Repeat(minCount, maxCount, itemWeights, item);
    }
    
    static public Pick codePoint(String source) {
        return new CodePoint(new UnicodeSet(source));
    }
    */
    
    static public Pick repeat(int minCount, int maxCount, int[] itemWeights, Pick item) {
        return new Repeat(minCount, maxCount, itemWeights, item);
    }
    
    static public Pick codePoint(UnicodeSet source) {
        return new CodePoint(source);
    }
    static public Pick string(String source) {
        return new Literal(source);
    }
    /*
    static public Pick unquoted(String source) {
        return new Literal(source);
    }
    static public Pick string(int minLength, int maxLength, Pick item) {
        return new Morph(item, minLength, maxLength);
    }
    */
    
    public abstract String getInternal(int depth, Set alreadySeen);
    // Internals
   
    protected String name;
    
    protected abstract void addTo(Target target);
    protected abstract boolean match(String input, Position p);
    
    public static class Sequence extends ListPick {
        public Sequence and2 (Pick item) {
            addInternal(new Pick[] {item}); // we don't care about perf
            return this; // for chaining
        }
        public Sequence and2 (Pick[] itemArray) {
            addInternal(itemArray);
            return this; // for chaining
        }
        protected void addTo(Target target) {
            for (int i = 0; i < items.length; ++i) {
                items[i].addTo(target);
            }
        }
        public String getInternal(int depth, Set alreadySeen) {
            String result = checkName(name, alreadySeen);
            if (result.startsWith("$")) return result;
            result = indent(depth) + result + "SEQ(";
            for (int i = 0; i < items.length; ++i) {
                if (i != 0) result += ", ";
                result += items[i].getInternal(depth+1, alreadySeen);
            }
            result += ")";
            return result;
        }
        // keep private
        private Sequence() {}
        protected boolean match(String input, Position p) {
            int originalIndex = p.index;
            for (int i = 0; i < items.length; ++i) {
                if (!items[i].match(input, p)) {
                    p.index = originalIndex;
                    return false;
                }
            }
            return true;
        }
    }
    
    String checkName(String nameStr, Set alreadySeen) {
        if (nameStr == null) return "";
        if (alreadySeen.contains(nameStr)) return nameStr;
        alreadySeen.add(nameStr);
        return "{" + nameStr + "=}";
    }

    public static class Alternation extends ListPick {
        private WeightedIndex weightedIndex = new WeightedIndex(0);
           
        public Alternation or2 (Pick[] newItems) {
            return or2(1, newItems);
        }
        public Alternation or2 (int itemWeight, Pick item) {
            return or2(itemWeight, new Pick[] {item}); // we don't care about perf
        }
        public Alternation or2 (int itemWeight, Pick[] newItems) {
            int[] itemWeights = new int[newItems.length];
            Arrays.fill(itemWeights,itemWeight);
            return or2(itemWeights, newItems); // we don't care about perf
        }
        public Alternation or2 (int[] itemWeights, Pick[] newItems) {
            if (newItems.length != itemWeights.length) {
                throw new ArrayIndexOutOfBoundsException(
                    "or lengths must be equal: " + newItems.length + " != " + itemWeights.length);
            }
           // int lastLen = this.items.length;
            addInternal(newItems);
            weightedIndex.add(itemWeights);
            return this; // for chaining
        }
        protected void addTo(Target target) {
            items[weightedIndex.toIndex(target.nextDouble())].addTo(target);
        }    
        
        public String getInternal(int depth, Set alreadySeen) {
            String result = checkName(name, alreadySeen);
            if (result.startsWith("$")) return result;
            result = indent(depth) + result + "OR(";
            for (int i = 0; i < items.length; ++i) {
                if (i != 0) result += ", ";
                result += items[i].getInternal(depth+1, alreadySeen) + "/" + weightedIndex.weights[i];
            }
            return result + ")";
        }
        // keep private
        private Alternation() {}
        // take first matching option
        protected boolean match(String input, Position p) {
            for (int i = 0; i < weightedIndex.weights.length; ++i) {
                if (p.isFailure(this,i)) continue;
                if (items[i].match(input, p)) return true;
                p.setFailure(this, i);
            }
            return false;
        }
    }
    
    private static String indent(int depth) {
        String result = "\r\n";
        for (int i = 0; i < depth; ++i) {
            result += " ";
        }
        return result;
    }
    
    private static class Repeat extends ItemPick {
        WeightedIndex weightedIndex;
        int minCount = 0;
            
        private Repeat(int minCount, int maxCount, int[] itemWeights, Pick item) {
            super(item);
            weightedIndex = new WeightedIndex(minCount).add(maxCount-minCount+1, itemWeights);
        }
        /*private Repeat(int minCount, int maxCount, int itemWeight, Pick item) {
            super(item);
            weightedIndex = new WeightedIndex(minCount).add(maxCount-minCount+1, itemWeight);
        }*/
        /*
        private Repeat(int minCount, int maxCount, Object item) {
            this.item = convert(item);
            weightedIndex = new WeightedIndex(minCount).add(maxCount-minCount+1, 1);
        }
        */
        protected void addTo(Target target) {
            //int count ;
            for (int i = weightedIndex.toIndex(target.nextDouble()); i > 0; --i) {
                item.addTo(target);
            }
        }
        public String getInternal(int depth, Set alreadySeen) {
            String result = checkName(name, alreadySeen);
            if (result.startsWith("$")) return result;
            result = indent(depth) + result + "REPEAT(" + weightedIndex
            + "; "+ item.getInternal(depth+1, alreadySeen) 
            + ")";
            return result;
        }
        
        // match longest, e.g. up to just before a failure
        protected boolean match(String input, Position p) {
            //int bestMatch = p.index;
            int count = 0;
            for (int i = 0; i < weightedIndex.weights.length; ++i) {
                if (p.isFailure(this,i)) break;
                if (!item.match(input, p)) {
                    p.setFailure(this,i);
                    break;
                } 
                //bestMatch = p.index;
                count++;               
            }
            if (count >= minCount) {
                return true;
            }
            // TODO fix failure
            return false;
        }
    }
    
    private static class CodePoint extends FinalPick {
        private UnicodeSet source;
        
        private CodePoint(UnicodeSet source) {
            this.source = source;
        }
        protected void addTo(Target target) {
            target.append(source.charAt(pick(target.random,0,source.size()-1)));
        }
        protected boolean match(String s, Position p) {
            int cp = UTF16.charAt(s, p.index);
            if (source.contains(cp)) {
                p.index += UTF16.getCharCount(cp);
                return true;
            }
            p.setMax("codePoint");
            return false;
        }
        public String getInternal(int depth, Set alreadySeen) {
            String result = checkName(name, alreadySeen);
            if (result.startsWith("$")) return result;
            return source.toString();
        }
    }

    static class Morph extends ItemPick {
        Morph(Pick item) {
            super(item);
        }
    
        private String lastValue = null;
        private Target addBuffer = Target.make(this, null, new Quoter.RuleQuoter());
        private StringBuffer mergeBuffer = new StringBuffer();
    
        private static final int COPY_NEW = 0, COPY_BOTH = 1, COPY_LAST = 3, SKIP = 4,
                                 LEAST_SKIP = 4;
        // give weights to the above. make sure we delete about the same as we insert
        private static final WeightedIndex choice = new WeightedIndex(0)
            .add(new int[] {10, 10, 100, 10});
        
        protected void addTo(Target target) {
            // get contents into separate buffer
            addBuffer.copyState(target);
            addBuffer.clear();
            item.addTo(addBuffer);
            String newValue = addBuffer.get();
            if (DEBUG) System.out.println("Old: " + lastValue + ", New:" + newValue);

            // if not first one, merge with old
            if (lastValue != null) {
                mergeBuffer.setLength(0);
                int lastIndex = 0;
                int newIndex = 0;
                // the new length is a random value between old and new.
                int newLenLimit = (int) pick(target.random, lastValue.length(), newValue.length());
                
                while (mergeBuffer.length() < newLenLimit
                  && newIndex < newValue.length()
                  && lastIndex < lastValue.length()) {
                    int c = choice.toIndex(target.nextDouble());
                    if (c == COPY_NEW || c == COPY_BOTH || c == SKIP) {
                        newIndex = getChar(newValue, newIndex, mergeBuffer, c < LEAST_SKIP);
                        if (mergeBuffer.length() >= newLenLimit) break;
                    }
                    if (c == COPY_LAST || c == COPY_BOTH || c == SKIP) {
                        lastIndex = getChar(lastValue, lastIndex, mergeBuffer, c < LEAST_SKIP);
                    }
                }
                newValue = mergeBuffer.toString();
            }
            lastValue = newValue;
            target.append(newValue);
            if (DEBUG) System.out.println("Result: " + newValue);
        }

        public String getInternal(int depth, Set alreadySeen) {
            String result = checkName(name, alreadySeen);
            if (result.startsWith("$")) return result;
            return indent(depth) + result + "MORPH("
                + item.getInternal(depth+1, alreadySeen)
                + ")";
        }

        /* (non-Javadoc)
         * @see Pick#match(java.lang.String, Pick.Position)
         */
        protected boolean match(String input, Position p) {
            // TODO Auto-generated method stub
            return false;
        }
    }
    
    /* Add character if we can
     */
    static int getChar(String newValue, int newIndex, StringBuffer mergeBuffer, boolean copy) {
        if (newIndex >= newValue.length()) return newIndex;
        int cp = UTF16.charAt(newValue,newIndex);
        if (copy) UTF16.append(mergeBuffer, cp);
        return newIndex + UTF16.getCharCount(cp);
    }

    /*   
            // quoted add
            appendQuoted(target, addBuffer.toString(), quoteBuffer);
            // fix buffers
            StringBuffer swapTemp = addBuffer;
            addBuffer = source;
            source = swapTemp;
        }
    }
    */


    static class Quote extends ItemPick {
        Quote(Pick item) {
            super(item);
        }
        protected void addTo(Target target) {
            target.quoter.setQuoting(true);
            item.addTo(target);
            target.quoter.setQuoting(false);
        }
        
        protected boolean match(String s, Position p) {
            return false;
        }

        public String getInternal(int depth, Set alreadySeen) {
            String result = checkName(name, alreadySeen);
            if (result.startsWith("$")) return result;
            return indent(depth) + result + "QUOTE(" + item.getInternal(depth+1, alreadySeen)
                + ")";
        }
    }
    
    private static class Literal extends FinalPick {
        public String toString() {
            return name;     
        }        
        private Literal(String source) {  
            this.name = source;
        }
        protected void addTo(Target target) {
            target.append(name);
        }
        protected boolean match(String input, Position p) {
            int len = name.length();
            if (input.regionMatches(p.index, name, 0, len)) {
                p.index += len;
                return true;
            }
            p.setMax("literal");
            return false;
        }
        public String getInternal(int depth, Set alreadySeen) {
            return "'" + name + "'";
        }
    }
    
    public static class Position {
        public ArrayList failures = new ArrayList();
        public int index;
        public int maxInt;
        public String maxType;
        public void setMax(String type) {
            if (index >= maxInt) {
                maxType = type;
            }
        }
        public String toString() {
            return "index; " + index
                + ", maxInt:" + maxInt
                + ", maxType: " + maxType;
        }
        /*private static final Object BAD = new Object();
        private static final Object GOOD = new Object();*/
        
        public boolean isFailure(Pick pick, int item) {
            ArrayList val = (ArrayList)failures.get(index);
            if (val == null) return false;
            Set set = (Set)val.get(item);
            if (set == null) return false;
            return !set.contains(pick);
        }
        public void setFailure(Pick pick, int item) {
            ArrayList val = (ArrayList)failures.get(index);
            if (val == null) {
                val = new ArrayList();
                failures.set(index, val);
            }
            Set set = (Set)val.get(item);
            if (set == null) {
                set = new HashSet();
                val.set(item, set);
            }
            set.add(pick);
        }
    }
    
    /*
    public static final Pick NOTHING = new Nothing();
    

    private static class Nothing extends FinalPick {
        protected void addTo(Target target) {}
        protected boolean match(String input, Position p) {
            return true;
        }
        public String getInternal(int depth, Set alreadySeen) {
            return indent(depth) + "\u00F8";
        }
    }
    */
    
    // intermediates
    
    abstract static class Visitor {
        Set already = new HashSet();
        // Note: each visitor should return the Pick that will replace a (or a itself)
        abstract Pick handle(Pick a);
        boolean alreadyEntered(Pick item) {
            boolean result = already.contains(item);
            already.add(item);
            return result;
        }
        void reset() {
            already.clear();
        }
    }
    
    protected abstract Pick visit(Visitor visitor);
    
    static class Replacer extends Visitor {
        String toReplace;
        Pick replacement;
        Replacer(String toReplace, Pick replacement) {
            this.toReplace = toReplace;
            this.replacement = replacement;
        }
        public Pick handle(Pick a) {
            if (toReplace.equals(a.name)) {
                a = replacement;
            } 
            return a;
       }
    }

    abstract private static class FinalPick extends Pick {
        public Pick visit(Visitor visitor) {
            return visitor.handle(this);
       }
    }
    
    private abstract static class ItemPick extends Pick {
       protected Pick item;
       
       ItemPick (Pick item) {
           this.item = item;
       }
       
       public Pick visit(Visitor visitor) {
           Pick result = visitor.handle(this);
           if (visitor.alreadyEntered(this)) return result;
           if (item != null) item = item.visit(visitor);
           return result;
       }
    }
   
    @SuppressWarnings("unused")
    private abstract static class ListPick extends Pick {
        protected Pick[] items = new Pick[0];

        Pick simplify() {
            if (items.length > 1) return this;
            if (items.length == 1) return items[0];
            return null;
        }
        
        int size() {
            return items.length;
        }

        Pick getLast() {
            return items[items.length-1];
        }

        void setLast(Pick newOne) {
            items[items.length-1] = newOne;
        }

        protected void addInternal(Pick[] objs) {
            int lastLen = items.length;
            items = realloc(items, items.length + objs.length);
            for (int i = 0; i < objs.length; ++i) {
                items[lastLen + i] = objs[i];
            }
        }

        public Pick visit(Visitor visitor) {
            Pick result = visitor.handle(this);
            if (visitor.alreadyEntered(this)) return result;
            for (int i = 0; i < items.length; ++i) {
                items[i] = items[i].visit(visitor);
            }
            return result;
        }
    }
    
    /**
     * Simple class to distribute a number between 0 (inclusive) and 1 (exclusive) among
     * a number of indices, where each index is weighted.
     * Item weights may be zero, but cannot be negative.
     * @author Davis
     */
    // As in other case, we use an array for runtime speed; don't care about buildspeed.
    public static class WeightedIndex {
        private int[] weights = new int[0];
        private int minCount = 0;
        private double total;
        
        public WeightedIndex(int minCount) {
            this.minCount = minCount;
        }
        
        public WeightedIndex add(int count, int itemWeights) {
            if (count > 0) {
                int[] newWeights = new int[count];
                if (itemWeights < 1) itemWeights = 1;
                Arrays.fill(newWeights, 0, count, itemWeights);
                add(1, newWeights);
            }
            return this; // for chaining
        }

        public WeightedIndex add(int[] newWeights) {
            return add(newWeights.length, newWeights);
        }
        
        public WeightedIndex add(int maxCount, int[] newWeights) {
            if (newWeights == null) newWeights = new int[]{1};
            int oldLen = weights.length;
            if (maxCount < newWeights.length) maxCount = newWeights.length;
            weights = (int[]) realloc(weights, weights.length + maxCount);
            System.arraycopy(newWeights, 0, weights, oldLen, newWeights.length);
            int lastWeight = weights[oldLen + newWeights.length-1];
            for (int i = oldLen + newWeights.length; i < maxCount; ++i) {
                weights[i] = lastWeight;
            }
            total = 0;
            for (int i = 0; i < weights.length; ++i) {
                if (weights[i] < 0) {
                    throw new RuntimeException("only positive weights: " + i);
                } 
                total += weights[i];
            }
            return this; // for chaining
        }
        
        // TODO, make this more efficient
        public int toIndex(double zeroToOne) {
            double weight = zeroToOne*total;
            int i;
            for (i = 0; i < weights.length; ++i) {
                weight -= weights[i];
                if (weight <= 0) break;
            }
            return i + minCount;
        }
        public String toString() {
            String result = "";
            for (int i = 0; i < minCount; ++i) {
                if (result.length() != 0) result += ",";
                result += "0";
            }
            for (int i = 0; i < weights.length; ++i) {
                if (result.length() != 0) result += ",";
                result += weights[i];
            }
            return result;
        }
    }
    /*
    private static Pick convert(Object obj) {
        if (obj instanceof Pick) return (Pick)obj;
        return new Literal(obj.toString(), false);
    }
    */
    // Useful statics
    
    static public int pick(Random random, int start, int end) {
        return start + (int)(random.nextDouble() * (end + 1 - start));
    }
    
    static public double pick(Random random, double start, double end) {
        return start + (random.nextDouble() * (end + 1 - start));
    }
    
    static public boolean pick(Random random, double percent) {
        return random.nextDouble() <= percent;
    }
    
    static public int pick(Random random, UnicodeSet s) {
        return s.charAt(pick(random, 0,s.size()-1));
    }
    
    static public String pick(Random random, String[] source) {
        return source[pick(random, 0, source.length-1)];
    }
    
    // these utilities really ought to be in Java
    
public static double[] realloc(double[] source, int newSize) {
    double[] temp = new double[newSize];
    if (newSize > source.length) newSize = source.length;
    if (newSize != 0) System.arraycopy(source,0,temp,0,newSize);
    return temp;
}
    
public static int[] realloc(int[] source, int newSize) {
    int[] temp = new int[newSize];
    if (newSize > source.length) newSize = source.length;
    if (newSize != 0) System.arraycopy(source,0,temp,0,newSize);
    return temp;
}
    
    public static Pick[] realloc(Pick[] source, int newSize) {
        Pick[] temp = new Pick[newSize];
        if (newSize > source.length) newSize = source.length;
        if (newSize != 0) System.arraycopy(source,0,temp,0,newSize);
        return temp;
    }
    
    // test utilities
    /*private static void append(StringBuffer target, String toAdd, StringBuffer quoteBuffer) {
        Utility.appendToRule(target, (int)-1, true, false, quoteBuffer); // close previous quote
        if (DEBUG) System.out.println("\"" + toAdd + "\"");
        target.append(toAdd);
    }

    private static void appendQuoted(StringBuffer target, String toAdd, StringBuffer quoteBuffer) {
        if (DEBUG) System.out.println("\"" + toAdd + "\"");
        Utility.appendToRule(target, toAdd, false, false, quoteBuffer);
    }*/

    /*
    public static abstract class MatchHandler {
        public abstract void handleString(String source, int start, int limit);
        public abstract void handleSequence(String source, int start, int limit);
        public abstract void handleAlternation(String source, int start, int limit);
            
    }
    */
    /*
    // redistributes random value
    // values are still between 0 and 1, but with a different distribution
    public interface Spread {
        public double spread(double value);
    }
    
    // give the weight for the high end.
    // values are linearly scaled according to the weight.
    static public class SimpleSpread implements Spread {
        static final Spread FLAT = new SimpleSpread(1.0);
        boolean flat = false;
        double aa, bb, cc;
        public SimpleSpread(double maxWeight) {   
            if (maxWeight > 0.999 && maxWeight < 1.001) {
                flat = true;
            } else { 
                double q = (maxWeight - 1.0);
                aa = -1/q;
                bb = 1/(q*q);
                cc = (2.0+q)/q;
           }                 
        }
        public double spread(double value) {
            if (flat) return value;
            value = aa + Math.sqrt(bb + cc*value);
            if (value < 0.0) return 0.0;    // catch math gorp
            if (value >= 1.0) return 1.0;
            return value;
        }
    }
    static public int pick(Spread spread, Random random, int start, int end) {
        return start + (int)(spread.spread(random.nextDouble()) * (end + 1 - start));
    }
    
   */
    

}