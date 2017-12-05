package org.alexsem.buparser.util;

import org.alexsem.buparser.model.Location;
import org.alexsem.buparser.model.LocationSet;
import org.alexsem.buparser.model.Metadata;
import org.alexsem.buparser.model.SearchCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class LocationCalculator {

    /**
     * Generates complete regular expression pattern for input validation
     * @param metadata List of books with sizes and short names
     * @return Generated pattern
     */
    public static String generateValidationPattern(Metadata metadata) {
        String pChapBlank = "(\\.?)"; //No chapters

        String pVerseOne = "([0-9]{1,3})"; //One verse
        String pVerseInt = String.format("(%s\\-%s)", pVerseOne, pVerseOne); //Verse interval
        String pVerseOneOrInt = String.format("(%s|%s)", pVerseOne, pVerseInt); //Either one verse or verse interval
        String pVerseCombo = String.format("( *: *%s(,%s)*)", pVerseOneOrInt, pVerseOneOrInt); //Combination of verses and verse intervals
        String pVerseOneSpecial = String.format("( *: *%s)", pVerseOne); //One verse (special)

        String pChapOneSimp = String.format("( *[0-9]{1,3})"); //One chapter (simple)
        String pChapOneExt = String.format("( *[0-9]{1,3}%s)", pVerseCombo); //One chapter (extended)
        String pChapOneSimpOrExt = String.format("(%s|%s)", pChapOneSimp, pChapOneExt); //One chapter (simple or extended)
        String pChapOneSpecial = String.format("( *[0-9]{1,3}%s)", pVerseOneSpecial); //One chapter (special)

        String pChapIntSimp = String.format("(%s * \\- %s)", pChapOneSimp, pChapOneSimp); //Chapter interval (simple)
        String pChapIntSpecial = String.format("(%s * \\- %s)", pChapOneSpecial, pChapOneSpecial); //Chapter interval (special)
        String pChapIntSimpOrSpecial = String.format("(%s|%s)", pChapIntSimp, pChapIntSpecial); //Chapter interval (simple or special)

        String pChapOneOrInt = String.format("(%s|%s)", pChapOneSimpOrExt, pChapIntSimpOrSpecial); //Either one chapter (simple or extended) or chapter interval (simple or special)
        String pChapCombo = String.format("([\\. ]%s( *,  *%s)*)", pChapOneOrInt, pChapOneOrInt); //Combination of chapters and chapter intervals

        String pBook = String.format("( *%s(%s|%s)? *;)+", generateBookShortNamesRegExp(metadata), pChapBlank, pChapCombo); //Book with chapters

        return pBook;
    }

    /**
     * Transforms Search code that contains zero-values into a list of non-zero valued search codes
     * @param metadata List of books with sizes and short names
     * @param code     Search code
     * @return List of respective search codes
     */
    private static List<SearchCode> transformCode(Metadata metadata, SearchCode code) {
        List<SearchCode> result = new ArrayList<SearchCode>();
        if (code.getBook() == 0) { //All available books
            return result;
        }
        if (code.getBook() < 1 || code.getBook() > 77) { //Book index out of bounds
            return result;
        }
        int bookSize = metadata.getBookSize(code.getBook());
        if (code.getChapter() == 0) { //All chapters of the specific book
            for (int j = 1; j <= bookSize; j++) {
                result.add(new SearchCode(code.getBook(), j, 0));
            }
            return result;
        }
        if (code.getChapter() < 1 || code.getChapter() > bookSize) { //Chapter index out of bounds
            return result;
        }
        result.add(new SearchCode(code.getBook(), code.getChapter(), code.getLine()));
        return result;
    }

    /**
     * Transforms search codes with zero values into search codes interval
     * @param metadata List of books with sizes and short names
     * @param code1    Interval beginning
     * @param code2    Interval end
     * @return List of search codes
     */
    private static List<SearchCode> transformCodeInterval(Metadata metadata, SearchCode code1, SearchCode code2) {
        List<SearchCode> result = new ArrayList<SearchCode>();
        if (code1.getBook() == 0 || code2.getBook() == 0 || code1.getBook() != code2.getBook()) { //Book not specified
            return result;
        }
        if (code1.getBook() < 1 || code1.getBook() > 77 || code2.getBook() < 1 || code2.getBook() > 77) { //Book index is out of bounds
            return result;
        }
        int chapter1 = code1.getChapter();
        int chapter2 = code2.getChapter();
        if (chapter1 == 0 || chapter2 == 0) { //Chapter not specified
            return result;
        }
        if (code1.getLine() == 0 && code2.getLine() == 0) { //Chapter interval
            for (int cInd = chapter1; cInd <= chapter2; cInd++) {
                result.addAll(LocationCalculator.transformCode(metadata, new SearchCode(code1.getBook(), cInd, 0)));
            }
            return result;
        }
        switch (code1.compareTo(code2)) {
            case 1: //First code exceeds second code
                return result;
            case 0: //First code equals to second code
                result.addAll(LocationCalculator.transformCode(metadata, code1));
                return result;
            case -1: //Second code exceeds first code
                if (chapter1 > metadata.getBookSize(code1.getBook()) || chapter1 < 1) { //Chapter index is out of bounds
                    return result;
                }
                int cInd = chapter1;
                int lInd = code1.getLine();
                while (true) {
                    result.addAll(transformCode(metadata, new SearchCode(code1.getBook(), cInd, lInd)));
                    lInd++;
                    if (lInd > metadata.getChapterSize(code1.getBook(), cInd)) { //Line index exceeds line count
                        lInd = 1;
                        cInd++;
                        if (cInd > metadata.getBookSize(code1.getBook())) { //Chapter index exceeds chapter count
                            break;
                        }
                    }
                    if (cInd > chapter2 || (cInd == chapter2 && lInd > code2.getLine())) { //Interval end reached
                        break;
                    }
                }
                break;
        }
        return result;
    }

    /**
     * Returns list of codes parsed from current substring
     * @param metadata List of books with sizes and short names
     * @param subInput Substring to parse
     * @return List of search codes
     */
    private static List<SearchCode> parseSearchSubString(Metadata metadata, String subInput) {
        List<SearchCode> result = new ArrayList<SearchCode>();
        final String letters1 = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
        final String letters2 = "abcdefghijklmnopqrstuvwxyz";
        final String numbers = "0123456789";
        String bookName = "";
        String chapName = "";
        String lineName = "";
        int book = -1;
        int chap1 = -1;
        int chap2 = -1;
        int line1 = -1;
        int line2 = -1;
        /**
         * 0 - beginning of text (no book specified yet)
         * 1 - entering book name
         * 2 - finished entering book name
         * 3 - entering first chapter number
         * 4 - finished entering first chapter number
         * 5 - found '-' after first chapter number (chapter interval)
         * 6 - entering second chapter number
         * 7 - finished entering second chapter number
         * 8 - found ':' (preparing to fill line number)
         * 9 - entering first verse number
         * 10 - entered first verse number
         * 11 - found ',' after entering verse number (not sure what's up next)
         * 12 - found '-' after first verse name (verse interval)
         * 13 - entering second verse number
         * 14 - finished entering second verse number
         * 15 - found ',' after entering verse number (not sure what's up next)
         * 16 - found '-' after first verse name (extended interval)
         * 17 - entering second chapter number of extended interval
         * 18 - finished entering second chapter number of extended interval
         * 19 - found ':' after second chapter number of extended interval
         * 20 - entering second verse number of extended interval
         * 21 - finished entering second verse number of extended interval
         * 22 - found ',' after extended interval
         */
        int mode = 0;
        char[] chars = (subInput.toLowerCase() + ";").toCharArray();
        for (char c : chars) {
            if (c == ' ') { //Space
                if (mode == 1) { //Entering book name
                    mode = 2;
                    book = metadata.getBookOrd(bookName);
                } else if (mode == 3) { //Entering first chapter number
                    mode = 4;
                    chap1 = Integer.valueOf(chapName);
                } else if (mode == 6) { //Entering second chapter number
                    mode = 7;
                    chap2 = Integer.valueOf(chapName);
                } else if (mode == 9) { //Entering first verse number
                    mode = 10;
                    line1 = Integer.valueOf(lineName);
                } else if (mode == 11) { //Entered comma after first verse number
                    mode = 2;
                    result.addAll(transformCode(metadata, new SearchCode(book, chap1, line1)));
                } else if (mode == 13) { //Entering second verse interval
                    mode = 14;
                    line2 = Integer.valueOf(lineName);
                } else if (mode == 15) { //Entered comma after second verse number
                    mode = 2;
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, line1), new SearchCode(book, chap1, line2)));
                } else if (mode == 17) { //Entering second chapter number of extended interval
                    mode = 18;
                    chap2 = Integer.valueOf(chapName);
                } else if (mode == 20) { //Entering second verse number of extended interval
                    mode = 21;
                    line2 = Integer.valueOf(lineName);
                } else if (mode == 22) { //Found comma after extended interval
                    mode = 2;
                }
            } else if (letters1.indexOf(c) != -1 || letters2.indexOf(c) != -1) { //Letter
                if (mode == 0) { //Beginning of text
                    mode = 1;
                    bookName = "";
                    bookName += c;
                } else if (mode == 1) { //Entering book name
                    bookName += c;
                }
            } else if (numbers.indexOf(c) != -1) { //Number
                if (mode == 0) { //Beginning of text
                    mode = 1;
                    bookName = "";
                    bookName += c;
                } else if (mode == 2) { //Entered book name
                    mode = 3;
                    chapName = "";
                    chapName += c;
                } else if (mode == 3) { //Entering first chapter number
                    chapName += c;
                } else if (mode == 5) { //Second part of chapter interval
                    mode = 6;
                    chapName = "";
                    chapName += c;
                } else if (mode == 6) { //Entering second chapter interval
                    chapName += c;
                } else if (mode == 8) { //Prepared to enter verse number
                    mode = 9;
                    lineName = "";
                    lineName += c;
                } else if (mode == 9) { //Entering first verse number
                    lineName += c;
                } else if (mode == 11) { //Comma after first verse number
                    mode = 9;
                    lineName = "";
                    lineName += c;
                } else if (mode == 12) { //Second part of verse interval
                    mode = 13;
                    lineName = "";
                    lineName += c;
                } else if (mode == 13) { //Entering second verse number
                    lineName += c;
                } else if (mode == 15) { //Comma after second verse number
                    mode = 9;
                    lineName = "";
                    lineName += c;
                } else if (mode == 16) { //Second part of extended verse interval
                    mode = 17;
                    chapName = "";
                    chapName += c;
                } else if (mode == 17) { //Entering second chapter number of extended interval
                    chapName += c;
                } else if (mode == 19) { //Prepared to enter second verse number of extended interval
                    mode = 20;
                    lineName = "";
                    lineName += c;
                } else if (mode == 20) { //Entering second verse number of extended interval
                    lineName += c;
                }
            } else if (c == '.') { //Dot
                if (mode == 1) {//Entering book name
                    mode = 2;
                    book = metadata.getBookOrd(bookName);
                }
            } else if (c == ',') { //Comma
                if (mode == 3) { //Entering first chapter number
                    mode = 2;
                    chap1 = Integer.valueOf(chapName);
                    result.addAll(LocationCalculator.transformCode(metadata, new SearchCode(book, chap1, 0)));
                } else if (mode == 4) { //Entered first chapter number
                    mode = 2;
                    result.addAll(LocationCalculator.transformCode(metadata, new SearchCode(book, chap1, 0)));
                } else if (mode == 6) { //Entering second chapter number
                    mode = 2;
                    chap2 = Integer.valueOf(chapName);
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, 0), new SearchCode(book, chap2, 0)));
                } else if (mode == 7) { //Entered second chapter number
                    mode = 2;
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, 0), new SearchCode(book, chap2, 0)));
                } else if (mode == 9) { //Entering first verse number
                    mode = 11;
                    line1 = Integer.valueOf(lineName);
                    result.addAll(transformCode(metadata, new SearchCode(book, chap1, line1)));
                } else if (mode == 10) { //Entered first verse number
                    mode = 11;
                    result.addAll(transformCode(metadata, new SearchCode(book, chap1, line1)));
                } else if (mode == 13) { //Entering second verse number
                    mode = 15;
                    line2 = Integer.valueOf(lineName);
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, line1), new SearchCode(book, chap1, line2)));
                } else if (mode == 20) { //Entering second verse number of extended interval
                    mode = 22;
                    line2 = Integer.valueOf(lineName);
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, line1), new SearchCode(book, chap2, line2)));
                } else if (mode == 21) { //Entered second verse number of extended interval
                    mode = 22;
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, line1), new SearchCode(book, chap2, line2)));
                }
            } else if (c == '-') { //Interval divisor
                if (mode == 4) { //Entered first chapter number
                    mode = 5;
                } else if (mode == 9) { //Entering first verse number
                    line1 = Integer.valueOf(lineName);
                    mode = 12;
                } else if (mode == 10) { //Entered first verse number
                    mode = 16;
                }
            } else if (c == ':') { //Verse divisor
                if (mode == 3) { //Entering first chapter number
                    mode = 8;
                    chap1 = Integer.valueOf(chapName);
                } else if (mode == 4) { //Entered first chapter number
                    mode = 8;
                } else if (mode == 17) { //Entering second chapter number of extended interval
                    mode = 19;
                    chap2 = Integer.valueOf(chapName);
                } else if (mode == 18) { //Entered second chapter number of extended interval
                    mode = 19;
                }
            } else if (c == ';') { //End of the text
                if (mode == 1) { //Entering book name
                    book = metadata.getBookOrd(bookName);
                    result.addAll(LocationCalculator.transformCode(metadata, new SearchCode(book, 0, 0)));
                } else if (mode == 2) { //Entered book name
                    result.addAll(LocationCalculator.transformCode(metadata, new SearchCode(book, 0, 0)));
                } else if (mode == 3) { //Entering first chapter number
                    chap1 = Integer.valueOf(chapName);
                    result.addAll(LocationCalculator.transformCode(metadata, new SearchCode(book, chap1, 0)));
                } else if (mode == 4) { //Entered first chapter number
                    result.addAll(LocationCalculator.transformCode(metadata, new SearchCode(book, chap1, 0)));
                } else if (mode == 6) { //Entering second chapter number
                    chap2 = Integer.valueOf(chapName);
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, 0), new SearchCode(book, chap2, 0)));
                } else if (mode == 7) { //Entered second chapter number
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, 0), new SearchCode(book, chap2, 0)));
                } else if (mode == 9) { //Entering first verse number
                    line1 = Integer.valueOf(lineName);
                    result.addAll(LocationCalculator.transformCode(metadata, new SearchCode(book, chap1, line1)));
                } else if (mode == 10) { //Entered first verse number
                    result.addAll(LocationCalculator.transformCode(metadata, new SearchCode(book, chap1, line1)));
                } else if (mode == 13) { //Entering second verse number
                    line2 = Integer.valueOf(lineName);
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, line1), new SearchCode(book, chap1, line2)));
                } else if (mode == 14) { //Entered second verse number
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, line1), new SearchCode(book, chap1, line2)));
                } else if (mode == 20) { //Entering second verse number of extended interval
                    line2 = Integer.valueOf(lineName);
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, line1), new SearchCode(book, chap2, line2)));
                } else if (mode == 21) { //Entered second verse number of extended interval
                    result.addAll(LocationCalculator.transformCodeInterval(metadata, new SearchCode(book, chap1, line1), new SearchCode(book, chap2, line2)));
                }
            }
        }
        return result;
    }

    /**
     * Transforms list of codes into list of locations
     * @param codes List of codes
     * @return List oof locations
     */
    private static List<Location> transformCodesToLocations(List<SearchCode> codes) {
        List<Location> locations = new ArrayList<Location>();
        Location location = null;
        int book = -1;
        int chapter = -1;
        for (SearchCode code : codes) {
            if (code.getBook() != book || code.getChapter() != chapter) { //New location record
                if (location != null) {
                    locations.add(location);
                }
                book = code.getBook();
                chapter = code.getChapter();
                location = new Location();
                location.setBook(book);
                location.setChapter(chapter);
            }
            if (code.getLine() == 0) { //Entire chapter
                location.setFilter(null);
            } else { //Should be filtered
                location.appendFilter(code.getLine());
            }
        }
        if (location != null) {
            locations.add(location);
        }
        return locations;
    }

    /**
     * Returns list of codes parsed from the given string
     * @param metadata List of books with sizes and short names
     * @return List of search codes
     */
    public static LocationSet parseSearchString(Metadata metadata, String input) {
        List<SearchCode> codes = new ArrayList<SearchCode>();
        String[] split = input.split(";");
        for (String part : split) {
            codes.addAll(parseSearchSubString(metadata, part));
        }
        if (codes.size() <= 0) { //No codes found
            return null;
        }
        return new LocationSet(input, transformCodesToLocations(codes), 0, -1f);
    }

    /**
     * Constructs the part of general RegExp pattern which defines the book name options
     * @param metadata List of books with sizes and short names
     * @return Regular expressions pattern
     */
    private static String generateBookShortNamesRegExp(Metadata metadata) {
        StringBuilder result = new StringBuilder();
        result.append('(');
        boolean first = true;
        Set<String> names = metadata.getAllShortNames();
        for (String sname : names) {
            if (!first) {
                result.append('|');
            }
            result.append(sname);
            first = false;
        }
        result.append(')');
        return result.toString().toLowerCase();
    }

}
