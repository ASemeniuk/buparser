package org.alexsem.buparser.model;

/**
 * Class that represents one specific chapter
 * @author Semeniuk A.D.
 */
public class Chapter implements Comparable<Chapter> {
    private int ord;
    private int size;

    public Chapter(int ord, int size) {
        this.ord = ord;
        this.size = size;
    }

    public int getOrd() {
        return ord;
    }

    public int getSize() {
        return size;
    }

    @Override
    public int compareTo(Chapter another) {
        if (ord > another.ord) {
            return 1;
        }
        if (ord < another.ord) {
            return -1;
        }
        return 0;
    }
}
