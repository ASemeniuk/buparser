package org.alexsem.buparser.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.alexsem.buparser.util.CsNumber;

/**
 * Class which holds useful references to all books data
 * @author Semeniuk A.D.
 */
public class Metadata {

    private List<Book> mBookList;
    private Map<String, Book> mBookMap;

    public Metadata() {
        this.mBookList = new ArrayList<>();
        this.mBookMap = new HashMap<>();
    }

    /**
     * Add new book both to the list and to the map
     * @param book Book to add
     */
    public void addBook(Book book) {
        mBookList.add(book);
        mBookMap.put(book.getRuShortName().toLowerCase(), book);
    }

    /**
     * Return set of books short names (lower-case)
     * @return Set of short names (not ordered) for all books
     */
    public Set<String> getAllShortNames() {
        return mBookMap.keySet();
    }

    /**
     * Return size of the specific book
     * @param book Book index (1-based)
     * @return Book size
     */
    public int getBookSize(int book) {
        return mBookList.get(book - 1).getSize();
    }

    /**
     * Return short name of the specific book
     * @param book Book index (1-based)
     * @return Book short name (Russian)
     */
    public String getBookShortName(int book) {
        return mBookList.get(book - 1).getRuShortName();
    }

    /**
     * Return order of specific book
     * @param sname Book short name (lower-case)
     * @return Book order
     */
    public int getBookOrd(String sname) {
        if (mBookMap.containsKey(sname)) {
            return mBookMap.get(sname).getOrd();
        }
        return -1;
    }

    /**
     * Return size of the specific chapter
     * @param book    Book index (1-based)
     * @param chapter Chapter index (1-based)
     * @return Chapter size
     */
    public int getChapterSize(int book, int chapter) {
        Book tempBook = mBookList.get(book - 1);
        if (tempBook.getChapters() == null) { //Load chapters from database
            throw new UnsupportedOperationException("Reading database");
//            Uri uri = Uri.withAppendedPath(BibleProvider.Chapter.CONTENT_BOOK_URI, String.valueOf(tempBook.getId()));
//            String[] proj = {BibleProvider.Chapter.ID, BibleProvider.Chapter.ORD, BibleProvider.Chapter.SIZE};
//            List<Chapter> chapters = new ArrayList<Chapter>();
//            Cursor cursor = mContext.getContentResolver().query(uri, proj, null, null, BibleProvider.Chapter.ORD);
//            try {
//                if (cursor.moveToFirst()) {
//                    do {
//                        int ord = cursor.getInt(cursor.getColumnIndex(BibleProvider.Chapter.ORD));
//                        int size = cursor.getInt(cursor.getColumnIndex(BibleProvider.Chapter.SIZE));
//                        chapters.add(new Chapter(ord, size));
//                    } while (cursor.moveToNext());
//                }
//            } finally {
//                cursor.close();
//            }
//            tempBook.setChapters(chapters);
        }
        return tempBook.getChapters().get(chapter - 1).getSize();
    }

    /**
     * Construct Russian title which consists of chapter name and chapter number
     * @param book    Book index (1-based)
     * @param chapter Chapter index (1-based)
     * @return Tab title
     */
    public String constructRuTitle(int book, int chapter) {
        return String.format("%s %d", mBookList.get(book - 1).getRuChapterName(), chapter);
    }

    /**
     * Construct Russian title which consists of book name and chapter number
     * @param book    Book index (1-based)
     * @param chapter Chapter index (1-based)
     * @return Tab title
     */
    public String constructRuTitleComplex(int book, int chapter) {
        return String.format("%s.%d", mBookList.get(book - 1).getRuShortName(), chapter);
    }

    /**
     * Construct CS title which consists of chapter name and chapter number
     * @param book    Book index (1-based)
     * @param chapter Chapter index (1-based)
     * @return Chapter title
     */
    public String constructCsTitle(int book, int chapter) {
        return String.format("%s %s", mBookList.get(book - 1).getCsChapterName(), CsNumber.generateCsNumber(chapter));
    }
}
