package com.ibm.text.UCD;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.lang.UCharacter;
import com.ibm.text.utility.*;
import java.util.*;
import java.io.*;

// Enumerated properties will be IntCodePointProperty.
// The string values they return will be the property value names.
// Binary properties are Enumerated properties. They return 0 or 1

public final class TernaryStore {
    
    static final int DONE = Integer.MIN_VALUE;
    static final int NOT_FOUND = Integer.MIN_VALUE+1;
    
    // for testing
    static DepthPrinter dp;
    
    static void test() throws java.io.IOException {
        Default.setUCD();
        
        PrintWriter pw = Utility.openPrintWriter("TestTernary.txt", Utility.LATIN1_WINDOWS);
        try {
            dp = new DepthPrinter(pw);
            
            String[] tests = {"the", "quick", "fish", "fisherman", "fishes", 
                "brown", "brow", "bracket", "bright", "brat",
                "brough", "dogs", "upper", "zebra",
                "fisher"};
            test("Simple: ", tests, tests.length);
            
            
            tests = new String[300000];
            int counter = 0;
            int i;
            for (i = 0; counter < tests.length && i <= 0x10FFFF; ++i) {
                if (Default.ucd.hasComputableName(i)) continue;
                
                String temp = UCharacter.getName(i);
                if (temp != null) tests[counter++] = temp.trim();
            }
            System.out.println("max-cp: " + Utility.hex(i));
            test("Unicode Names: ", tests, counter);
            
            //if (true) return;
            
            BufferedReader br = Utility.openReadFile(UCD_Types.BASE_DIR + "dict\\DiploFreq.txt", Utility.LATIN1);
            String line;
            counter = 0;
            while (counter < tests.length) {
                line = Utility.readDataLine(br);
                if (line == null) break;
                if (line.length() == 0) continue;
                Utility.dot(counter);
                int tabPos = line.indexOf('\t');
                if (tabPos < 0) {
                    System.out.println("???" + line);
                    continue;
                }
                tests[counter++] = line.substring(tabPos+1);
            }
            test("French: ", tests, counter);
        } finally {
            pw.close();
        }
    }
    
    static void test(String title, String[] tests, int len) {
        System.out.println();
        System.out.println(title);
        dp.println();
        dp.print(title, 0);
        dp.println();
        TernaryStore.Builder builder = new TernaryStore.Builder();
        int charCount = 0;
        for (int i = 0; i < len; ++i) {
            builder.add(tests[i], i);
            charCount += tests[i].length();
        }
        System.out.println("charCount: " + charCount);
        TernaryStore store = builder.build();
        store.showNodes();
        store.checkNodes();
        
        dp.println("Storage");
        dp.println(store.stringStore.toString());
        System.out.println("StorageSize: " + store.stringStore.toString().length());
        
        Matcher matcher = store.getMatcher();
        for (int i = 0; i < len; ++i) {
            int check = test(tests[i], matcher);
            if (check != i) {
                System.out.println("\tFail, result: " + tests[i] + ", " + check);
            }
        }
    }
    
    static int test(String s, Matcher matcher) {
        matcher.reset(s, 0);
        int lastResult = -1;
        for (int result = matcher.next(); result != DONE; result = matcher.next()) {
            lastResult = result;
        }
        return lastResult;
    }
    
    static final class Node {
        String getString(StringStore stringStore) {
            if (stringCode < 0) return tempString;
            return stringStore.get(stringCode);
        }
        void setString(String s) {
            tempString = s;
        }
        String tempString;
        int stringCode = -1;
        Node less;
        Node greater;
        Node next;
        int result = NOT_FOUND;
        
        public String toString(StringStore store) {
            return getString(store)
                + (result != NOT_FOUND ? "(" + result + ")" : "")
                + (next != null ? next.toString() : "");
        }
    }
    
    Node base;
    StringStore stringStore = new StringStore();
    
    final static class Matcher {
        TernaryStore store;
        String s;
        int position;
        Node lastNode;
        
        void reset(String s, int position) {
            this.s = s;
            this.position = position;
            this.lastNode = store.base;
        }
        
        // returns the next result
        // or DONE when done
        // sets position to point after end of found string
        
        int next() {
            while (lastNode != null && position < s.length()) {
                char ch = s.charAt(position++);
                do {
                    String nodeString = lastNode.getString(store.stringStore);
                    char first = nodeString.charAt(0);
                    if (ch == first) {
                        // now check the rest of the string
                        for (int i = 1; i < nodeString.length(); ++i) {
                            char other = nodeString.charAt(i);
                            if (other != s.charAt(position++)) {
                                return DONE;
                            }
                        }
                        
                        // if we succeed, return result if there is one
                        int result = lastNode.result;
                        lastNode = lastNode.next;
                        if (result != NOT_FOUND) return result;
                        break; // get next char
                    }
                    // otherwise branch sideways, keeping same char
                    if (ch > first) {
                        lastNode = lastNode.greater;
                    } else {
                        lastNode = lastNode.less;
                    }
                } while (lastNode != null);
            }
            return DONE;
        }
    }
    
    public Matcher getMatcher() {
        Matcher result = new Matcher();
        result.store = this;
        return result;
    }
    
    public void showNodes() {
        showNodes2(base, "", 5);
    }
    
    public void showNodes2(Node n, String path, int depth) {
        if (n.less != null) {
            showNodes2(n.less, path+"-", depth);
        }
        dp.print("", depth);
        if (false) dp.print(path);
        dp.print(n.getString(stringStore));
        if (n.result != NOT_FOUND) dp.print("/" + n.result);
        dp.println();
        if (n.next != null) {
            showNodes2(n.next, path+".", depth+n.getString(stringStore).length());
        }
        if (n.greater != null) {
            showNodes2(n.greater, path+"+", depth);
        }
    }
    
    static class NodeInfo {
        int nodeCount;
        int resultCount;
        int nullLessCount;
        int nullGreaterCount;
        int nullSimpleCount;
        int nullNextCount;
    }
    
    public void checkNodes() {
        NodeInfo nodeInfo = new NodeInfo();
        checkNodes(base, nodeInfo);
        System.out.println("Nodes: " + nodeInfo.nodeCount);
        System.out.println("nullLessCount: " + nodeInfo.nullLessCount);
        System.out.println("nullGreaterCount: " + nodeInfo.nullGreaterCount);
        System.out.println("nullNextCount: " + nodeInfo.nullNextCount);
        System.out.println("resultCount: " + nodeInfo.resultCount);
        System.out.println("nullSimpleCount: " + nodeInfo.nullSimpleCount);
    }
    
    public void checkNodes(Node n, NodeInfo nodeInfo) {
        nodeInfo.nodeCount++;
        if (n.result != NOT_FOUND) nodeInfo.resultCount++;
        if (n.less != null) {
            checkNodes(n.less, nodeInfo);
        } else {
            nodeInfo.nullLessCount++;
            if (n.greater == null && n.result == NOT_FOUND) nodeInfo.nullSimpleCount++;
        }
        if (n.next != null) {
            checkNodes(n.next, nodeInfo);
        } else {
            nodeInfo.nullNextCount++;
        }
        if (n.greater != null) {
            checkNodes(n.greater, nodeInfo);
        } else {
            nodeInfo.nullGreaterCount++;
        }
    }
    
    final static class DepthPrinter {
        private PrintWriter pw;
        private int currentDepth = 0;
        private String leader = ".";
        
        DepthPrinter(PrintWriter pw) {
            this.pw = pw;
        }
        
        void print(char ch) {
            print(ch, 0);
        }
        
        void print(String s) {
            print(s, 0);
        }       
        
        void print(char ch, int depth) {
            print(String.valueOf(ch), depth);
        }
        
        void print(String s, int depth) {
            int delta = depth - currentDepth;
            if (delta > 0) {
                pw.print(Utility.repeat(leader, delta - 1));
                currentDepth = depth;
            }
            pw.print(s);
            currentDepth += s.length();
        }
        
        void println() {
            pw.println();
            currentDepth = 0;
        }

        void println(String s) {
            pw.print(s);
            pw.println();
            currentDepth = 0;
        }
    }
    
    final static class StringStore {
        // initially, there is a simple strategy

        private String buffer = "";
        private static final char TERMINATOR = '\u007E';
        private static final int PIECE_LENGTH = 5;
        private static String[] pieces = new String[50]; // HACK
        private static Set strings = new HashSet();
        
        public void add(String s) {
            strings.add(s);
        }
        
        public void compact() {
            System.out.println("Adding Pieces");
            // add all the pieces
            Iterator it = strings.iterator();
            Set additions = new HashSet();
            while (it.hasNext()) {
                String s = (String)it.next();
                int len = Utility.split(s, ' ', pieces);
                for (int i = 0; i < len; ++i) {
                    additions.add(pieces[i]);
                }
            }
            
            store(additions);
            store(strings);
        }
        
        private void store(Set stuff) {
            System.out.println("Sorting");
            // sort them by length, longest first
            Set ordered = new TreeSet();
            Iterator it = stuff.iterator();
            while (it.hasNext()) {
                String s = (String)it.next();
                ordered.add(new Pair(new Integer(-s.length()), s));
            }
            System.out.println("Storing");
            // add them
            it = ordered.iterator();
            while (it.hasNext()) {
                String s = (String)(((Pair)it.next()).second);
                get(s);
            }
        }
            
        private int get(String s) {
            System.out.println("Adding: \'" + s + "\'");
            int index;
            if (s.indexOf(' ') < 0) {
                index = addNoSplit(s);
                System.out.println("\tReturning: " + index);
                return index;
            }
            int len = Utility.split(s, ' ', pieces);
            StringBuffer itemCodes = new StringBuffer();
            for (int i = 0; i < len; ++i) {
                String piece = pieces[i];
                itemCodes.append((char)addNoSplit(piece));
                /*for (int j = 0; j < piece.length(); j += PIECE_LENGTH) {
                    int maxLen = j + PIECE_LENGTH;
                    if (maxLen > piece.length()) maxLen = piece.length();
                    itemCodes.append((char)addNoSplit(piece.substring(j, maxLen)));
                }*/
            }
            index = 0x8000 | addNoSplit(itemCodes.toString());   // mark it as composite
            System.out.println("\tReturning: " + index);
            return index;
        }
        
        private int addNoSplit(String s) {
            System.out.println("\tAdding2: \'" + s + "\'");
            String sTerm = s + TERMINATOR;
            int index = buffer.indexOf(sTerm);
            if (index >= 0) return index;

            index = buffer.length();
            buffer += sTerm;
            System.out.println("\t\tReturning2: " + index);
            return index;
        }
        
        public String get(int index) {
            String result;
            System.out.println("Fetching: " + index);
            
            if ((index & 0x8000) == 0) {
                int end = buffer.indexOf(TERMINATOR, index);
                result = buffer.substring(index, end);
                System.out.println("\tReturning: '" + result + "'");
                return result;
            }
            index &= ~0x8000; // remove 1 bit
            
            int end = buffer.indexOf(TERMINATOR, index);
            result = "";
            for (int i = index; i < end; ++i) {
                if (result.length() != 0) result += " ";
                result += get(buffer.charAt(i));
            }
            System.out.println("\tReturning: '" + result + "'");
            return result;
        }
        
        public String toString() {
            return buffer;
        }

    }
            
    final static class Builder {
        Map map = new TreeMap();
        String[] names;
        TernaryStore store;
        Set set = new TreeSet();
        
        public void add(String name, int result) {
            map.put(name, new Integer(result));
        }
        
        public TernaryStore build() {
            // flatten strings into array
            names = new String[map.size()];
            Iterator it = map.keySet().iterator();
            int count = 0;
            while (it.hasNext()) {
                names[count++] = (String) it.next();
                if (false) {
                    dp.print((count-1) + " " + names[count-1]);
                    dp.println();
                }
            }
            
            // build nodes
            store = new TernaryStore();
            addNode(0, names.length);
            
            // free storage
            names = null;
            map.clear();
            
            System.out.println("compacting");
            compactStore(store.base);
            store.stringStore.compact();
            
            //compactStrings(store);
            //set.clear();    // free more storage
            
            replaceStrings(store.base);
            //map.clear();    // free storage
            
            // free storage
            TernaryStore result = store;
            store = null;
            
            return result;
        }
        
        /*
        void compactStrings(TernaryStore t) {
            // we have a set of Pairs, first is length, second is string
            // compact them, word by word
            Iterator it = set.iterator();
            while (it.hasNext()) {
                String string = ((String)((Pair)it.next()).second);
                int index = t.stringStore.add(string);
                if (true) {
                    System.out.println("Checking: " + index);
                    String reverse = t.stringStore.get(index);
                    if (!reverse.equals(string)) {
                        System.out.println("source: \'" + string + "\'");
                        System.out.println("reverse: \'" + reverse + "\'");
                        throw new IllegalArgumentException("Failed roundtrip");
                    }
                }
                        
                map.put(string, new Integer(index));
            }
        }
        */
        
        public void replaceStrings(Node n) {
            n.stringCode = store.stringStore.get(n.getString(store.stringStore));
            n.setString(null);
            if (n.less != null) replaceStrings(n.less);
            if (n.next != null) replaceStrings(n.next);
            if (n.greater != null) replaceStrings(n.greater);
        }
        
        public void compactStore(Node n) {
            Node nextNode = n.next;
            if (false) dp.println(n.toString());
            while (n.result == NOT_FOUND && nextNode != null && nextNode.greater == null
                && nextNode.less == null) {
                n.setString(n.getString(store.stringStore) + nextNode.getString(store.stringStore));
                n.result = nextNode.result;
                n.next = nextNode = nextNode.next; // remove old node
            }
            // add strings sorted by length, longest first
            store.stringStore.add(n.getString(store.stringStore)); 
            
            if (n.less != null) compactStore(n.less);
            if (n.next != null) compactStore(n.next);
            if (n.greater != null) compactStore(n.greater);
        }
        
        private void addNode(int start, int limit) {
            if (start >= limit) return;
            int mid = (start + limit) / 2;
            //System.out.println("start: " + start + ", mid: " + mid + ", limit: " + limit);
            //System.out.println("adding: " + names[mid]);
            addNode(names[mid], ((Integer)map.get(names[mid])).intValue());
            addNode(start, mid);
            addNode(mid+1, limit);
        }
        
        private void addNode(String s, int result) {
            if (store.base == null) {
                store.base = addRest(s, 0, result);
                return;
            }
            Node n = store.base;
            Node lastNode = n;
                
            for (int i = 0; i < s.length(); ++i) {
                char ch = s.charAt(i);
                while (true) {
                    char first = n.getString(store.stringStore).charAt(0);
                    if (ch == first) {
                        if (n.next == null) {
                            n.next = addRest(s, i+1, result);
                            return;
                        }
                        lastNode = n;
                        n = n.next;
                        break; // get next char
                    }
                    // otherwise branch sideways, keeping same char
                    if (ch > first) {
                        if (n.greater == null) {
                            n.greater = addRest(s, i, result);
                            return;
                        }
                        n = n.greater;
                    } else {
                        if (n.less == null) {
                            n.less = addRest(s, i, result);
                            return;
                        }
                        n = n.less;
                    }
                }
            }
            lastNode.result = result;
        }
        
        private Node addRest(String s, int position, int result) {
            Node lastNode = null;
            for (int i = s.length() - 1; i >= position; --i) {
                Node n = new Node();
                n.setString(s.substring(i, i+1)); // + "" to force a new string
                if (lastNode == null) {
                    n.result = result;
                }
                n.next = lastNode;
                lastNode = n;
            }
            return lastNode;
        }
    }
}
    
