package network.utils;

/**
 * Used as a comparable key-value per in for example priority queue
 */
public class Entry implements Comparable<Entry> {

    private int key;
    private int value;

    public Entry(int key, int value) {
        this.key = key;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public Entry add(int value){
        this.value += value;
        return this;
    }

    public int getKey() {
        return key;
    }

    // getters

    /**
     * This will make smaller values higher in priority
     * @param other
     * @return
     */
    @Override
    public int compareTo(Entry other) {
        if(value > other.value){
            return 1;
        }else if(value < other.value){
            return -1;
        }
        return 0;
    }
}