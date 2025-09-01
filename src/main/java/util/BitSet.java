package util;

import java.util.Arrays;

public class BitSet {

    //NOTE: This file is from the bachelor project.

    //This class contains a custom BitSet implementation with fast operations.

    protected long[] set;
    private final int size;

    //Creates BitSet with specified maximum size.
    public BitSet(int maxSize) {
        set = new long[(maxSize-1)/64+1];
        size = maxSize;
    }

    //Creates BitSet from a string of bits.
    public BitSet(String bitString) {
        this(bitString.length());
        for (int i = 0; i < bitString.length(); i++) {
            if (bitString.charAt(i) == '1') {
                add(i);
            }
            else if (bitString.charAt(i) != '0') {
                throw new IllegalArgumentException("Illegal bit string provided.");
            }
        }
    }

    //Adds participant with specific index to set (constant time complexity).
    public void add(int index) {
        int longIndex = index >> 6; //index / 64
        index = index & 63; //index % 64
        index = 63-index;
        set[longIndex] = set[longIndex] | (1L << index);
    }

    //Sets all bits to 1.
    public void setAll() {
        for (int i = 0; i < set.length; i++) {
            set[i] = -1;
            if (i == set.length-1) {
                int bitsInLastLong = size() % 64;
                set[i] = set[i] << (64-bitsInLastLong);
            }
        }
    }

    //Removes participant with specific index to set (constant time complexity).
    public void remove(int index) {
        int longIndex = index >> 6; //index / 64
        index = index & 63; //index % 64
        index = 63-index;
        set[longIndex] = set[longIndex] & ~(1L << index);
    }

    //Flips the bit at the specified index.
    public void flip(int index) {
        if (get(index)) {
            remove(index);
        }
        else {
            add(index);
        }
    }

    //Sets the bit at the specified index to the specified value.
    public void setValue(int index, boolean value) {
        if (value) {
            add(index);
        }
        else {
            remove(index);
        }
    }

    //Returns the value of the bit at the specified index.
    public boolean get(int index) {
        int longIndex = index >> 6; //index / 64
        index = index & 63; //index % 64
        index = 63-index;
        return (set[longIndex] & (1L << index)) != 0;
    }

    //Returns the size of the set.
    public int size() {
        return size;
    }

    //Returns the number of 1's in the bit set.
    public int count() {
        int count = 0;
        for (long l : set) {
            count += Long.bitCount(l);
        }
        return count;
    }

    //Returns the number of 0's in the bit set if flip is true. Returns the number of 1's otherwise.
    public int countFlipped(boolean flip) {
        return flip ? size() - count() : count();
    }

    //Calculates the similarity of two BitSets using bitwise XNor.
    public static int XNor(BitSet set1, BitSet set2) {
        int count = 0;
        for (int i = 0; i < set1.set.length; i++) {
            count += Long.bitCount((~set1.set[i]) ^ set2.set[i]);
            if (i == set1.set.length-1) { //Last part of last long is not part of the set.
                int bitsInLastLong = set1.size() % 64;
                if (bitsInLastLong > 0) {
                    count -= 64 - bitsInLastLong;
                }
            }
        }
        return count;
    }

    //Calculates the similarity of two BitSets using bitwise XOR.
    public static int XOR(BitSet set1, BitSet set2) {
        int count = 0;
        for (int i = 0; i < set1.set.length; i++) {
            count += Long.bitCount(set1.set[i] ^ set2.set[i]);
        }
        return count;
    }

    //Unions this bitset with otherSet.
    public void unionWith(BitSet otherSet) {
        for (int i = 0; i < set.length; i++) {
            set[i] = set[i] | otherSet.set[i];
        }
    }

    //Returns the size of the intersection between two bitsets.
    //Requires same maximum size. flip specifies if the corresponding bit set should have all bits flipped before calculating the intersection.
    public static int intersection(BitSet set1, BitSet set2, boolean flip1, boolean flip2) {
        return intersectionEarlyStop(set1, set2, flip1, flip2, Integer.MAX_VALUE);
    }

    //Returns the size of the intersection between three bitsets.
    //Requires same maximum size. flip specifies if the corresponding bit set should have all bits flipped before calculating the intersection.
    public static int intersection(BitSet set1, BitSet set2, BitSet set3, boolean flip1, boolean flip2, boolean flip3) {
        return intersectionEarlyStop(set1, set2, set3, flip1, flip2, flip3, Integer.MAX_VALUE);
    }

    //Intersection between two sets that stops when the intersection is known to be greater than a.
    //Requires same maximum size. flip specifies if the corresponding bit set should have all bits flipped before calculating the intersection.
    public static int intersectionEarlyStop(BitSet set1, BitSet set2, boolean flip1, boolean flip2, int a) {
        int count = 0;
        for (int i = 0; i < set1.set.length; i++) {
            int amountFlipped = 0;
            long long1 = set1.set[i];
            long long2 = set2.set[i];
            if (flip1) {
                long1 = ~long1;
                amountFlipped++;
            }
            if (flip2) {
                long2 = ~long2;
                amountFlipped++;
            }
            count += Long.bitCount(long1 & long2);
            if (i == set1.set.length-1 && amountFlipped == 2) { //Last part of last long is not part of the set.
                int bitsInLastLong = set1.size() % 64;
                if (bitsInLastLong > 0) {
                    count -= 64 - bitsInLastLong;
                }
            }
            if (count >= a) {
                return count;
            }
        }
        return count;
    }

    //Intersection between three sets that stops when the intersection is known to be greater than a.
    //Requires same maximum size. flip specifies if the corresponding bit set should have all bits flipped before calculating the intersection.
    public static int intersectionEarlyStop(BitSet set1, BitSet set2, BitSet set3, boolean flip1, boolean flip2, boolean flip3, int a) {
        int count = 0;
        for (int i = 0; i < set1.set.length; i++) {
            int amountFlipped = 0;
            long long1 = set1.set[i];
            long long2 = set2.set[i];
            long long3 = set3.set[i];
            if (flip1) {
                long1 = ~long1;
                amountFlipped++;
            }
            if (flip2) {
                long2 = ~long2;
                amountFlipped++;
            }
            if (flip3) {
                long3 = ~long3;
                amountFlipped++;
            }
            count += Long.bitCount(long1 & long2 & long3);
            if (i == set1.set.length-1 && amountFlipped == 3) { //Last part of last long is not part of the set.
                int bitsInLastLong = set1.size() % 64;
                if (bitsInLastLong > 0) {
                    count -= 64 - bitsInLastLong;
                }
            }
            if (count >= a) {
                return count;
            }
        }
        return count;
    }

    //Converts the bit set to a string of bits. Used for debugging.
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (long l : set) {
            result.append("0".repeat(Long.numberOfLeadingZeros(l)));
            if (l != 0) {
                result.append(Long.toBinaryString(l));
            }
        }
        result.delete(result.length() - (64-size()%64), result.length());
        return result.toString();
    }

    //Prints the bit set as a string of bits and prints the size of the set and the number of longs used for the bit set. Used for debugging.
    public void print() {
        System.out.println(size + " " + set.length);
        System.out.println(this);
    }

    //Checks if this object is equal to the given object.
    @Override
    public boolean equals(Object o) {
        return o instanceof BitSet && Arrays.equals(((BitSet) o).set, set);
    }

}
