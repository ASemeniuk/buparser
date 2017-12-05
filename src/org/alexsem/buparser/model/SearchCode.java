package org.alexsem.buparser.model;

/**
 * Instances of this class represent some code which is used for unique line identification
 * Commonly found in search procedures
 * @author Semeniuk A.D.
 */
public class SearchCode implements Comparable<SearchCode> {

    /**
     * Book number
     */
    private int book;
    /**
     * Chapter number
     */
    private int chapter;
    /**
     * Line number
     */
    private int line;

    /**
     * Constructor
     * @param book    Book number
     * @param chapter Chapter number
     * @param line    Line number
     */
    public SearchCode(int book, int chapter, int line) {
        this.book = book;
        this.chapter = chapter;
        this.line = line;
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

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    /**
     * Comparable interface method.
     * Defines whether one code is bigger/smaller/equal to another one
     * @param o Another SearchCode object
     * @return 1 if this object is bigger than o, -1 if smaller, 0 if two objects are equal
     */
    public int compareTo(SearchCode code) {
        if (this.getBook() > code.getBook()) {
            return 1;
        } else if (this.getBook() < code.getBook()) {
            return -1;
        }
        if (this.getChapter() > code.getChapter()) {
            return 1;
        } else if (this.getChapter() < code.getChapter()) {
            return -1;
        }
        if (this.getLine() > code.getLine()) {
            return 1;
        } else if (this.getLine() < code.getLine()) {
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format("%d,%d,%d", book, chapter, line);
    }
}
