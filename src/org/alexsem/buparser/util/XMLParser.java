package org.alexsem.buparser.util;

import org.alexsem.buparser.model.Book;
import org.alexsem.buparser.model.Chapter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class which contains method for various XML parsing related operations
 * @author Semeniuk A.D.
 */
public abstract class XMLParser {

    /**
     * Parses metadata file (list of books and chapters)
     * @param input Stream to read data from
     * @return List of parsed books (with chapters)
     * @throws XmlPullParserException in case XML parsing fails
     * @throws IOException            in case server interaction fails
     */
    public static List<Book> parseMetadata(InputStream input) throws XmlPullParserException, IOException {
        List<Book> result = new ArrayList<Book>();
        XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
        try {
            xpp.setInput(input, "UTF-8");
            Book book = null;
            ArrayList<Chapter> chapters = null;
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {

                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    if (xpp.getName().equalsIgnoreCase("book")) {
                        book = new Book();
                        book.setOrd(Integer.valueOf(xpp.getAttributeValue(0)));
                        book.setSize(Integer.valueOf(xpp.getAttributeValue(1)));
                    } else if (book != null) {
                        if (xpp.getName().equalsIgnoreCase("name_cs")) {
                            book.setCsName(xpp.nextText());
                        } else if (xpp.getName().equalsIgnoreCase("name_ru")) {
                            book.setRuName(xpp.nextText());
                        } else if (xpp.getName().equalsIgnoreCase("chapname_cs")) {
                            book.setCsChapterName(xpp.nextText());
                        } else if (xpp.getName().equalsIgnoreCase("chapname_ru")) {
                            book.setRuChapterName(xpp.nextText());
                        } else if (xpp.getName().equalsIgnoreCase("shortname_ru")) {
                            book.setRuShortName(xpp.nextText());
                        } else if (xpp.getName().equalsIgnoreCase("chapters")) {
                            chapters = new ArrayList<Chapter>();
                        } else if (chapters != null) {
                            if (xpp.getName().equalsIgnoreCase("chapter")) {
                                int ord = Integer.valueOf(xpp.getAttributeValue(0));
                                int size = Integer.valueOf(xpp.getAttributeValue(1));
                                chapters.add(new Chapter(ord, size));
                            }
                        }
                    }
                } else if (xpp.getEventType() == XmlPullParser.END_TAG) {
                    if (xpp.getName().equalsIgnoreCase("book")) {
                        result.add(book);
                        book = null;
                    } else if (xpp.getName().equalsIgnoreCase("chapters")) {
                        Collections.sort(chapters);
                        book.setChapters(chapters);
                        chapters = null;
                    }
                }
                xpp.next();
            }
        } finally {
            input.close();
        }
        Collections.sort(result);
        return result;

    }


}
