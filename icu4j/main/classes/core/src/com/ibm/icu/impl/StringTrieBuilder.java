/*
*******************************************************************************
*   Copyright (C) 2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   created on: 2011jan05
*   created by: Markus W. Scherer
*   ported from ICU4C stringtriebuilder/.cpp
*/
package com.ibm.icu.impl;

import java.util.HashMap;

/**
 * Base class for string trie builder classes.
 *
 * @author Markus W. Scherer
 */
public abstract class StringTrieBuilder {
    public enum Option {
        FAST,
        SMALL
    }

    protected StringTrieBuilder() {}

    protected final void createCompactBuilder(int sizeGuess) {
        nodes=new HashMap<Node, Node>(sizeGuess);
    }
    protected final void deleteCompactBuilder() {
        nodes=null;
        lookupFinalValueNode=null;
    }

    protected final void build(Option buildOption, int elementsLength) {
        if(buildOption==Option.FAST) {
            writeNode(0, elementsLength, 0);
        } else /* Option.SMALL */ {
            createCompactBuilder(2*elementsLength);
            Node root=makeNode(0, elementsLength, 0);
            root.markRightEdgesFirst(-1);
            root.write(this);
            deleteCompactBuilder();
        }
    }

    // Requires start<limit,
    // and all strings of the [start..limit[ elements must be sorted and
    // have a common prefix of length unitIndex.
    protected int writeNode(int start, int limit, int unitIndex) {
        boolean hasValue=false;
        int value=0;
        int type;
        if(unitIndex==getElementStringLength(start)) {
            // An intermediate or final value.
            value=getElementValue(start++);
            if(start==limit) {
                return writeValueAndFinal(value, true);  // final-value node
            }
            hasValue=true;
        }
        // Now all [start..limit[ strings are longer than unitIndex.
        int minUnit=getElementUnit(start, unitIndex);
        int maxUnit=getElementUnit(limit-1, unitIndex);
        if(minUnit==maxUnit) {
            // Linear-match node: All strings have the same character at unitIndex.
            int lastUnitIndex=getLimitOfLinearMatch(start, limit-1, unitIndex);
            writeNode(start, limit, lastUnitIndex);
            // Break the linear-match sequence into chunks of at most kMaxLinearMatchLength.
            int length=lastUnitIndex-unitIndex;
            int maxLinearMatchLength=getMaxLinearMatchLength();
            while(length>maxLinearMatchLength) {
                lastUnitIndex-=maxLinearMatchLength;
                length-=maxLinearMatchLength;
                writeElementUnits(start, lastUnitIndex, maxLinearMatchLength);
                write(getMinLinearMatch()+maxLinearMatchLength-1);
            }
            writeElementUnits(start, unitIndex, length);
            type=getMinLinearMatch()+length-1;
        } else {
            // Branch node.
            int length=countElementUnits(start, limit, unitIndex);
            // length>=2 because minUnit!=maxUnit.
            writeBranchSubNode(start, limit, unitIndex, length);
            if(--length<getMinLinearMatch()) {
                type=length;
            } else {
                write(length);
                type=0;
            }
        }
        return writeValueAndType(hasValue, value, type);
    }

    // start<limit && all strings longer than unitIndex &&
    // length different units at unitIndex
    protected int writeBranchSubNode(int start, int limit, int unitIndex, int length) {
        char[] middleUnits=new char[kMaxSplitBranchLevels];
        int[] lessThan=new int[kMaxSplitBranchLevels];
        int ltLength=0;
        while(length>getMaxBranchLinearSubNodeLength()) {
            // Branch on the middle unit.
            // First, find the middle unit.
            int i=skipElementsBySomeUnits(start, unitIndex, length/2);
            // Encode the less-than branch first.
            middleUnits[ltLength]=getElementUnit(i, unitIndex);  // middle unit
            lessThan[ltLength]=writeBranchSubNode(start, i, unitIndex, length/2);
            ++ltLength;
            // Continue for the greater-or-equal branch.
            start=i;
            length=length-length/2;
        }
        // For each unit, find its elements array start and whether it has a final value.
        int[] starts=new int[kMaxBranchLinearSubNodeLength];
        boolean[] isFinal=new boolean[kMaxBranchLinearSubNodeLength-1];
        int unitNumber=0;
        do {
            int i=starts[unitNumber]=start;
            char unit=getElementUnit(i++, unitIndex);
            i=indexOfElementWithNextUnit(i, unitIndex, unit);
            isFinal[unitNumber]= start==i-1 && unitIndex+1==getElementStringLength(start);
            start=i;
        } while(++unitNumber<length-1);
        // unitNumber==length-1, and the maxUnit elements range is [start..limit[
        starts[unitNumber]=start;

        // Write the sub-nodes in reverse order: The jump lengths are deltas from
        // after their own positions, so if we wrote the minUnit sub-node first,
        // then its jump delta would be larger.
        // Instead we write the minUnit sub-node last, for a shorter delta.
        int[] jumpTargets=new int[kMaxBranchLinearSubNodeLength-1];
        do {
            --unitNumber;
            if(!isFinal[unitNumber]) {
                jumpTargets[unitNumber]=writeNode(starts[unitNumber], starts[unitNumber+1], unitIndex+1);
            }
        } while(unitNumber>0);
        // The maxUnit sub-node is written as the very last one because we do
        // not jump for it at all.
        unitNumber=length-1;
        writeNode(start, limit, unitIndex+1);
        int offset=write(getElementUnit(start, unitIndex));
        // Write the rest of this node's unit-value pairs.
        while(--unitNumber>=0) {
            start=starts[unitNumber];
            int value;
            if(isFinal[unitNumber]) {
                // Write the final value for the one string ending with this unit.
                value=getElementValue(start);
            } else {
                // Write the delta to the start position of the sub-node.
                value=offset-jumpTargets[unitNumber];
            }
            writeValueAndFinal(value, isFinal[unitNumber]);
            offset=write(getElementUnit(start, unitIndex));
        }
        // Write the split-branch nodes.
        while(ltLength>0) {
            --ltLength;
            writeDeltaTo(lessThan[ltLength]);
            offset=write(middleUnits[ltLength]);
        }
        return offset;
    }

    // Requires start<limit,
    // and all strings of the [start..limit[ elements must be sorted and
    // have a common prefix of length unitIndex.
    protected Node makeNode(int start, int limit, int unitIndex) {
        boolean hasValue=false;
        int value=0;
        if(unitIndex==getElementStringLength(start)) {
            // An intermediate or final value.
            value=getElementValue(start++);
            if(start==limit) {
                return registerFinalValue(value);
            }
            hasValue=true;
        }
        Node node;
        // Now all [start..limit[ strings are longer than unitIndex.
        int minUnit=getElementUnit(start, unitIndex);
        int maxUnit=getElementUnit(limit-1, unitIndex);
        if(minUnit==maxUnit) {
            // Linear-match node: All strings have the same character at unitIndex.
            int lastUnitIndex=getLimitOfLinearMatch(start, limit-1, unitIndex);
            Node nextNode=makeNode(start, limit, lastUnitIndex);
            // Break the linear-match sequence into chunks of at most kMaxLinearMatchLength.
            int length=lastUnitIndex-unitIndex;
            int maxLinearMatchLength=getMaxLinearMatchLength();
            while(length>maxLinearMatchLength) {
                lastUnitIndex-=maxLinearMatchLength;
                length-=maxLinearMatchLength;
                node=createLinearMatchNode(start, lastUnitIndex, maxLinearMatchLength, nextNode);
                nextNode=registerNode(node);
            }
            node=createLinearMatchNode(start, unitIndex, length, nextNode);
        } else {
            // Branch node.
            int length=countElementUnits(start, limit, unitIndex);
            // length>=2 because minUnit!=maxUnit.
            Node subNode=makeBranchSubNode(start, limit, unitIndex, length);
            node=new BranchHeadNode(length, subNode);
        }
        if(hasValue && node!=null) {
            if(matchNodesCanHaveValues()) {
                ((ValueNode )node).setValue(value);
            } else {
                node=new IntermediateValueNode(value, registerNode(node));
            }
        }
        return registerNode(node);
    }

    // start<limit && all strings longer than unitIndex &&
    // length different units at unitIndex
    protected Node makeBranchSubNode(int start, int limit, int unitIndex,
                            int length) {
        char[] middleUnits=new char[kMaxSplitBranchLevels];
        Node[] lessThan=new Node[kMaxSplitBranchLevels];
        int ltLength=0;
        while(length>getMaxBranchLinearSubNodeLength()) {
            // Branch on the middle unit.
            // First, find the middle unit.
            int i=skipElementsBySomeUnits(start, unitIndex, length/2);
            // Create the less-than branch.
            middleUnits[ltLength]=getElementUnit(i, unitIndex);  // middle unit
            lessThan[ltLength]=makeBranchSubNode(start, i, unitIndex, length/2);
            ++ltLength;
            // Continue for the greater-or-equal branch.
            start=i;
            length=length-length/2;
        }
        ListBranchNode listNode=new ListBranchNode();
        // For each unit, find its elements array start and whether it has a final value.
        int unitNumber=0;
        do {
            int i=start;
            char unit=getElementUnit(i++, unitIndex);
            i=indexOfElementWithNextUnit(i, unitIndex, unit);
            if(start==i-1 && unitIndex+1==getElementStringLength(start)) {
                listNode.add(unit, getElementValue(start));
            } else {
                listNode.add(unit, makeNode(start, i, unitIndex+1));
            }
            start=i;
        } while(++unitNumber<length-1);
        // unitNumber==length-1, and the maxUnit elements range is [start..limit[
        char unit=getElementUnit(start, unitIndex);
        if(start==limit-1 && unitIndex+1==getElementStringLength(start)) {
            listNode.add(unit, getElementValue(start));
        } else {
            listNode.add(unit, makeNode(start, limit, unitIndex+1));
        }
        Node node=registerNode(listNode);
        // Create the split-branch nodes.
        while(ltLength>0) {
            --ltLength;
            node=registerNode(
                new SplitBranchNode(middleUnits[ltLength], lessThan[ltLength], node));
        }
        return node;
    }

    protected abstract int getElementStringLength(int i) /*const*/;
    protected abstract char getElementUnit(int i, int unitIndex) /*const*/;
    protected abstract int getElementValue(int i) /*const*/;

    // Finds the first unit index after this one where
    // the first and last element have different units again.
    protected abstract int getLimitOfLinearMatch(int first, int last, int unitIndex) /*const*/;

    // Number of different bytes at unitIndex.
    protected abstract int countElementUnits(int start, int limit, int unitIndex) /*const*/;
    protected abstract int skipElementsBySomeUnits(int i, int unitIndex, int count) /*const*/;
    protected abstract int indexOfElementWithNextUnit(int i, int unitIndex, char unit) /*const*/;

    protected abstract boolean matchNodesCanHaveValues() /*const*/;

    protected abstract int getMaxBranchLinearSubNodeLength() /*const*/;
    protected abstract int getMinLinearMatch() /*const*/;
    protected abstract int getMaxLinearMatchLength() /*const*/;

    // max(BytesTrie::kMaxBranchLinearSubNodeLength, UCharsTrie::kMaxBranchLinearSubNodeLength).
    protected static final int kMaxBranchLinearSubNodeLength=5;

    // Maximum number of nested split-branch levels for a branch on all 2^16 possible char units.
    // log2(2^16/kMaxBranchLinearSubNodeLength) rounded up.
    protected static final int kMaxSplitBranchLevels=14;

    /**
     * Makes sure that there is only one unique node registered that is
     * equivalent to newNode.
     * @param newNode Input node. The builder takes ownership.
     * @param errorCode ICU in/out UErrorCode.
                        Set to U_MEMORY_ALLOCATION_ERROR if it was success but newNode==null.
     * @return newNode if it is the first of its kind, or
     *         an equivalent node if newNode is a duplicate.
     */
    protected final Node registerNode(Node newNode) {
        Node old=nodes.get(newNode);
        if(old!=null) {
            return old;
        }
        // If put() returns a non-null value from an equivalent, previously
        // registered node, then get() failed to find that and we will leak newNode.
        Node oldValue=nodes.put(newNode, newNode);
        assert(oldValue==null);
        return newNode;
    }

    /**
     * Makes sure that there is only one unique FinalValueNode registered
     * with this value.
     * Avoids creating a node if the value is a duplicate.
     * @param value A final value.
     * @param errorCode ICU in/out UErrorCode.
                        Set to U_MEMORY_ALLOCATION_ERROR if it was success but newNode==null.
     * @return A FinalValueNode with the given value.
     */
    protected final Node registerFinalValue(int value) {
        lookupFinalValueNode.setValue(value);
        Node old=nodes.get(lookupFinalValueNode);
        if(old!=null) {
            return old;
        }
        Node newNode=new FinalValueNode(value);
        // If put() returns a non-null value from an equivalent, previously
        // registered node, then get() failed to find that and we will leak newNode.
        Node oldValue=nodes.put(newNode, newNode);
        assert(oldValue==null);
        return newNode;
    }

    // Hash set of nodes, maps from nodes to integer 1.
    protected HashMap<Node, Node> nodes;
    protected FinalValueNode lookupFinalValueNode;

    protected abstract class Node {
        public Node(int initialHash) {
            hash=initialHash;
            offset=0;
        }
        @Override
        public final int hashCode() /*const*/ { return hash; }
        // Base class equals() compares the actual class types.
        @Override
        public boolean equals(Object other) {
            return this==other || (this.getClass()==other.getClass() && hash==((Node)other).hash);
        }
        /**
         * Traverses the Node graph and numbers branch edges, with rightmost edges first.
         * This is to avoid writing a duplicate node twice.
         *
         * Branch nodes in this trie data structure are not symmetric.
         * Most branch edges "jump" to other nodes but the rightmost branch edges
         * just continue without a jump.
         * Therefore, write() must write the rightmost branch edge last
         * (trie units are written backwards), and must write it at that point even if
         * it is a duplicate of a node previously written elsewhere.
         *
         * This function visits and marks right branch edges first.
         * Edges are numbered with increasingly negative values because we share the
         * offset field which gets positive values when nodes are written.
         * A branch edge also remembers the first number for any of its edges.
         *
         * When a further-left branch edge has a number in the range of the rightmost
         * edge's numbers, then it will be written as part of the required right edge
         * and we can avoid writing it first.
         *
         * After root.markRightEdgesFirst(-1) the offsets of all nodes are negative
         * edge numbers.
         *
         * @param edgeNumber The first edge number for this node and its sub-nodes.
         * @return An edge number that is at least the maximum-negative
         *         of the input edge number and the numbers of this node and all of its sub-nodes.
         */
        public int markRightEdgesFirst(int edgeNumber) {
            if(offset==0) {
                offset=edgeNumber;
            }
            return edgeNumber;
        }
        // write() must set the offset to a positive value.
        public abstract void write(StringTrieBuilder builder);
        // See markRightEdgesFirst.
        public final void writeUnlessInsideRightEdge(int firstRight, int lastRight,
                                               StringTrieBuilder builder) {
            // Note: Edge numbers are negative, lastRight<=firstRight.
            // If offset>0 then this node and its sub-nodes have been written already
            // and we need not write them again.
            // If this node is part of the unwritten right branch edge,
            // then we wait until that is written.
            if(offset<0 && (offset<lastRight || firstRight<offset)) {
                write(builder);
            }
        }
        public final int getOffset() /*const*/ { return offset; }

        protected int hash;
        protected int offset;
    }

    // This class should not be overridden because
    // registerFinalValue() compares a stack-allocated FinalValueNode
    // (stack-allocated so that we don't unnecessarily create lots of duplicate nodes)
    // with the input node, and the
    // !Node::operator==(other) used inside FinalValueNode::operator==(other)
    // will be false if the typeid's are different.
    protected final class FinalValueNode extends Node {
        public FinalValueNode(int v) {
            super(0x111111*37+v);
            value=v;
        }
        @Override
        public boolean equals(Object other) {
            if(this==other) {
                return true;
            }
            if(!super.equals(other)) {
                return false;
            }
            FinalValueNode o=(FinalValueNode)other;
            return value==o.value;
        }
        @Override
        public void write(StringTrieBuilder builder) {
            offset=builder.writeValueAndFinal(value, true);
        }

        protected int value;

        /**
         * Must be called only by registerFinalValue() and only on the lookupFinalValueNode.
         * This is a workaround: C++ just stack-allocates a FinalValueNode
         * inside registerFinalValue().
         * In Java, we keep a FinalValueNode instance and modify it.
         * Otherwise, FinalValueNode instances are immutable.
         */
        private void setValue(int v) {
            hash=0x111111*37+v;
            value=v;
        }
    }

    protected abstract class ValueNode extends Node {
        public ValueNode(int initialHash) {
            super(initialHash);
        }
        @Override
        public boolean equals(Object other) {
            if(this==other) {
                return true;
            }
            if(!super.equals(other)) {
                return false;
            }
            ValueNode o=(ValueNode)other;
            return hasValue==o.hasValue && (!hasValue || value==o.value);
        }
        public final void setValue(int v) {
            hasValue=true;
            value=v;
            hash=hash*37+v;
        }

        protected boolean hasValue;
        protected int value;
    }

    protected final class IntermediateValueNode extends ValueNode {
        public IntermediateValueNode(int v, Node nextNode) {
            super(0x222222*37+nextNode.hashCode());
            next=nextNode;
            setValue(v);
        }
        @Override
        public boolean equals(Object other) {
            if(this==other) {
                return true;
            }
            if(!super.equals(other)) {
                return false;
            }
            IntermediateValueNode o=(IntermediateValueNode)other;
            return next==o.next;
        }
        @Override
        public int markRightEdgesFirst(int edgeNumber) {
            if(offset==0) {
                offset=edgeNumber=next.markRightEdgesFirst(edgeNumber);
            }
            return edgeNumber;
        }
        @Override
        public void write(StringTrieBuilder builder) {
            next.write(builder);
            offset=builder.writeValueAndFinal(value, false);
        }

        protected Node next;
    }

    protected abstract class LinearMatchNode extends ValueNode {
        public LinearMatchNode(int len, Node nextNode) {
            super((0x333333*37+len)*37+nextNode.hashCode());
            length=len;
            next=nextNode;
        }
        @Override
        public boolean equals(Object other) {
            if(this==other) {
                return true;
            }
            if(!super.equals(other)) {
                return false;
            }
            LinearMatchNode o=(LinearMatchNode)other;
            return length==o.length && next==o.next;
        }
        @Override
        public int markRightEdgesFirst(int edgeNumber) {
            if(offset==0) {
                offset=edgeNumber=next.markRightEdgesFirst(edgeNumber);
            }
            return edgeNumber;
        }

        protected int length;
        public Node next;
    }

    protected abstract class BranchNode extends Node {
        public BranchNode(int initialHash) {
            super(initialHash);
        }

        protected int firstEdgeNumber;
    }

    protected final class ListBranchNode extends BranchNode {
        public ListBranchNode() {
            super(0x444444);
        }
        @Override
        public boolean equals(Object other) {
            if(this==other) {
                return true;
            }
            if(!super.equals(other)) {
                return false;
            }
            ListBranchNode o=(ListBranchNode)other;
            for(int i=0; i<length; ++i) {
                if(units[i]!=o.units[i] || values[i]!=o.values[i] || equal[i]!=o.equal[i]) {
                    return false;
                }
            }
            return true;
        }
        @Override
        public int markRightEdgesFirst(int edgeNumber) {
            if(offset==0) {
                firstEdgeNumber=edgeNumber;
                int step=0;
                int i=length;
                do {
                    Node edge=equal[--i];
                    if(edge!=null) {
                        edgeNumber=edge.markRightEdgesFirst(edgeNumber-step);
                    }
                    // For all but the rightmost edge, decrement the edge number.
                    step=1;
                } while(i>0);
                offset=edgeNumber;
            }
            return edgeNumber;
        }
        @Override
        public void write(StringTrieBuilder builder) {
            // Write the sub-nodes in reverse order: The jump lengths are deltas from
            // after their own positions, so if we wrote the minUnit sub-node first,
            // then its jump delta would be larger.
            // Instead we write the minUnit sub-node last, for a shorter delta.
            int unitNumber=length-1;
            Node rightEdge=equal[unitNumber];
            int rightEdgeNumber= rightEdge==null ? firstEdgeNumber : rightEdge.getOffset();
            do {
                --unitNumber;
                if(equal[unitNumber]!=null) {
                    equal[unitNumber].writeUnlessInsideRightEdge(firstEdgeNumber, rightEdgeNumber, builder);
                }
            } while(unitNumber>0);
            // The maxUnit sub-node is written as the very last one because we do
            // not jump for it at all.
            unitNumber=length-1;
            if(rightEdge==null) {
                builder.writeValueAndFinal(values[unitNumber], true);
            } else {
                rightEdge.write(builder);
            }
            offset=builder.write(units[unitNumber]);
            // Write the rest of this node's unit-value pairs.
            while(--unitNumber>=0) {
                int value;
                boolean isFinal;
                if(equal[unitNumber]==null) {
                    // Write the final value for the one string ending with this unit.
                    value=values[unitNumber];
                    isFinal=true;
                } else {
                    // Write the delta to the start position of the sub-node.
                    assert(equal[unitNumber].getOffset()>0);
                    value=offset-equal[unitNumber].getOffset();
                    isFinal=false;
                }
                builder.writeValueAndFinal(value, isFinal);
                offset=builder.write(units[unitNumber]);
            }
        }
        // Adds a unit with a final value.
        public void add(int c, int value) {
            units[length]=(char)c;
            equal[length]=null;
            values[length]=value;
            ++length;
            hash=(hash*37+c)*37+value;
        }
        // Adds a unit which leads to another match node.
        public void add(int c, Node node) {
            units[length]=(char)c;
            equal[length]=node;
            values[length]=0;
            ++length;
            hash=(hash*37+c)*37+node.hashCode();
        }

        protected Node[] equal=new Node[kMaxBranchLinearSubNodeLength];  // null means "has final value".
        protected int length;
        protected int[] values=new int[kMaxBranchLinearSubNodeLength];
        protected char[] units=new char[kMaxBranchLinearSubNodeLength];
    }

    protected final class SplitBranchNode extends BranchNode {
        public SplitBranchNode(char middleUnit, Node lessThanNode, Node greaterOrEqualNode) {
            super(((0x555555*37+middleUnit)*37+
                    lessThanNode.hashCode())*37+greaterOrEqualNode.hashCode());
            unit=middleUnit;
            lessThan=lessThanNode;
            greaterOrEqual=greaterOrEqualNode;
        }
        @Override
        public boolean equals(Object other) {
            if(this==other) {
                return true;
            }
            if(!super.equals(other)) {
                return false;
            }
            SplitBranchNode o=(SplitBranchNode)other;
            return unit==o.unit && lessThan==o.lessThan && greaterOrEqual==o.greaterOrEqual;
        }
        @Override
        public int markRightEdgesFirst(int edgeNumber) {
            if(offset==0) {
                firstEdgeNumber=edgeNumber;
                edgeNumber=greaterOrEqual.markRightEdgesFirst(edgeNumber);
                offset=edgeNumber=lessThan.markRightEdgesFirst(edgeNumber-1);
            }
            return edgeNumber;
        }
        @Override
        public void write(StringTrieBuilder builder) {
            // Encode the less-than branch first.
            lessThan.writeUnlessInsideRightEdge(firstEdgeNumber, greaterOrEqual.getOffset(), builder);
            // Encode the greater-or-equal branch last because we do not jump for it at all.
            greaterOrEqual.write(builder);
            // Write this node.
            assert(lessThan.getOffset()>0);
            builder.writeDeltaTo(lessThan.getOffset());  // less-than
            offset=builder.write(unit);
        }

        protected char unit;
        protected Node lessThan;
        protected Node greaterOrEqual;
    }

    // Branch head node, for writing the actual node lead unit.
    protected final class BranchHeadNode extends ValueNode {
        public BranchHeadNode(int len, Node subNode) {
            super((0x666666*37+len)*37+subNode.hashCode());
            length=len;
            next=subNode;
        }
        @Override
        public boolean equals(Object other) {
            if(this==other) {
                return true;
            }
            if(!super.equals(other)) {
                return false;
            }
            BranchHeadNode o=(BranchHeadNode)other;
            return length==o.length && next==o.next;
        }
        @Override
        public int markRightEdgesFirst(int edgeNumber) {
            if(offset==0) {
                offset=edgeNumber=next.markRightEdgesFirst(edgeNumber);
            }
            return edgeNumber;
        }
        @Override
        public void write(StringTrieBuilder builder) {
            next.write(builder);
            if(length<=builder.getMinLinearMatch()) {
                offset=builder.writeValueAndType(hasValue, value, length-1);
            } else {
                builder.write(length-1);
                offset=builder.writeValueAndType(hasValue, value, 0);
            }
        }

        protected int length;
        protected Node next;  // A branch sub-node.
    }

    protected abstract Node createLinearMatchNode(int i, int unitIndex, int length,
                                        Node nextNode) /*const*/;

    protected abstract int write(int unit);
    protected abstract int writeElementUnits(int i, int unitIndex, int length);
    protected abstract int writeValueAndFinal(int i, boolean isFinal);
    protected abstract int writeValueAndType(boolean hasValue, int value, int node);
    protected abstract int writeDeltaTo(int jumpTarget);
}
