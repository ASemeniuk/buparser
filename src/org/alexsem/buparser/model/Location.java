package org.alexsem.buparser.model;

import java.util.HashSet;

/**
 * Class which defines data for one particular reader page
 * @author Semeniuk A.D.
 */
public class Location {

    private int book;
    private int chapter;
    private HashSet<Integer> filter = null;

    public Location() {
    }

    public Location(int book, int chapter, HashSet<Integer> filter) {
        this.book = book;
        this.chapter = chapter;
        this.filter = filter;
    }

    public int getBook() {
        return book;
    }

    public void setBook(int book) {
        this.book = book;
    }

    public int getChapter() {
        return chapter;
    }

    public void setChapter(int chapter) {
        this.chapter = chapter;
    }

    public void appendFilter(int line) {
        if (filter == null) {
            filter = new HashSet<Integer>();
        }
        filter.add(line);
    }

    public HashSet<Integer> getFilter() {
        return filter;
    }

    public void setFilter(HashSet<Integer> filter) {
        this.filter = filter;
    }

    /**
     * Parse filter from string
     * @param source String to path
     */
    public void parseFilter(String source) {
        if (source == null || source.length() == 0) {
            this.filter = null;
            return;
        }
        this.filter = new HashSet<Integer>();
        String[] split = source.split(",");
        int size = split.length;
        for (int i = 0; i < size; i++) {
            this.filter.add(Integer.valueOf(split[i]));
        }
        if (this.filter.size() == 0) {
            this.filter = null;
        }
    }

    /**
     * Convert filter to string (CSV)
     * @return Formatted string
     */
    public String formatFilter() {
        if (filter == null || filter.size() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i : this.filter) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(i);
        }
        return builder.toString();
    }
}
