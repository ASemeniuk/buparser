package org.alexsem.buparser.model;

import java.util.List;

/**
 * Class which holds reference to one location set (number of pages with respective filters)
 * @author Semeniuk A.D.
 */
public class LocationSet {

    private String name;
    private boolean isLimited;

    private int book = -1;
    private int chapter = 0;
    private float scrollRatio = -1f;

    private List<Location> locations = null;

    /**
     * Create unlimited set of coordinates
     * @param name        Set name
     * @param book        Book index
     * @param chapter     Chapter index
     * @param scrollRatio Current scroll ratio
     */
    public LocationSet(String name, int book, int chapter, float scrollRatio) {
        this.name = name;
        this.isLimited = false;
        this.book = book;
        this.chapter = chapter;
        this.locations = null;
        this.scrollRatio = scrollRatio;
    }

    /**
     * Create limited set of coordinates
     * @param name         Set name
     * @param locations    List of locations
     * @param currentIndex Number of currently selected location
     * @param scrollRatio  Current scroll ratio
     */
    public LocationSet(String name, List<Location> locations, int currentIndex, float scrollRatio) {
        this.name = name;
        this.isLimited = true;
        this.book = -1;
        this.chapter = currentIndex;
        this.locations = locations;
        this.scrollRatio = scrollRatio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLimited() {
        return isLimited;
    }

    public void setLimited(boolean isLimited) {
        this.isLimited = isLimited;
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

    public float getScrollRatio() {
        return scrollRatio;
    }

    public void setScrollRatio(float scrollRatio) {
        this.scrollRatio = scrollRatio;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    @Override
    public String toString() {
        return name;
    }
}
