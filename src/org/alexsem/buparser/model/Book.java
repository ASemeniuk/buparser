package org.alexsem.buparser.model;

import java.util.List;

/**
 * Class that represents one specific book
 * @author Semeniuk A.D.
 */
public class Book implements Comparable<Book> {
    private long id;
    private int ord;
    private int size;

    private String csName;
    private String ruName;
    private String csChapterName;
    private String ruChapterName;
    private String ruShortName;

    private List<Chapter> chapters;

    public Book() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getOrd() {
        return ord;
    }

    public void setOrd(int ord) {
        this.ord = ord;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getCsName() {
        return csName;
    }

    public void setCsName(String csName) {
        this.csName = csName;
    }

    public String getRuName() {
        return ruName;
    }

    public void setRuName(String ruName) {
        this.ruName = ruName;
    }

    public String getCsChapterName() {
        return csChapterName;
    }

    public void setCsChapterName(String csChapterName) {
        this.csChapterName = csChapterName;
    }

    public String getRuChapterName() {
        return ruChapterName;
    }

    public void setRuChapterName(String ruChapterName) {
        this.ruChapterName = ruChapterName;
    }

    public String getRuShortName() {
        return ruShortName;
    }

    public void setRuShortName(String ruShortName) {
        this.ruShortName = ruShortName;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    @Override
    public int compareTo(Book another) {
        if (ord > another.ord) {
            return 1;
        }
        if (ord < another.ord) {
            return -1;
        }
        return 0;
    }

}
