package org.alexsem.buparser;

import org.alexsem.buparser.util.RomanNumbers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.alexsem.buparser.CalendarEntry.Line;
import org.alexsem.buparser.model.Book;
import org.alexsem.buparser.model.Location;
import org.alexsem.buparser.model.LocationSet;
import org.alexsem.buparser.model.Metadata;
import org.alexsem.buparser.util.LocationCalculator;
import org.alexsem.buparser.util.XMLParser;
import org.xmlpull.v1.XmlPullParserException;

public class BuParser {

    private static final String URL_TITLE = "http://www.patriarchia.ru/bu/%1$tY-%1$tm-%1$td/";
    private static final String URL_READINGS = "http://www.patriarchia.ru/rpc/date=%1$tY-%1$tm-%1$td/kld.xml";

    private static final String PATH_DIRECTORY = "D:/calendar/%04d";
    private static final String PATH_READINGS = PATH_DIRECTORY + "/%03d.xml";
    private static final String PATH_INFO = PATH_DIRECTORY + "/info.csv";

    private static final Pattern PATTERN_TITLE = Pattern.compile("Версия для печати.*?<br> *?<p> *?<b>(.*?)</p>");
    private static final Pattern PATTERN_READINGS = Pattern.compile("<div class=\"read\">(.*?)</div> *?<div class");
    private static final Pattern PATTERN_READING = Pattern.compile("([^<]*?)[,:-]? *?<a.*?>([^<]*?)\\.?</a>( *?<div.*?</div>)? *?\\&nbsp;");
    private static final Pattern PATTERN_ROMAN = Pattern.compile("([IVXLCDM]+), ");
    private static final Pattern PATTERN_COMMENT_SEPARATOR = Pattern.compile("\\d\\.");
    private static final Pattern PATTERN_COMMENT_LEFT = Pattern.compile("(.*?)[,:\\–\\-]? *?(([123] )?[А-Я][а-я]+?\\.,.*\\d)\\.?");

    private static Set<Integer> EXCEPTIONAL_DAYS = new HashSet<>(Arrays.asList());

    private static List<String> HOLIDAYS_STATIC = Arrays.asList("09-21", "09-27", "12-04", "01-07", "01-19", "02-15", "04-07", "08-19", "08-28", "09-11", "10-14", "01-14", "07-07", "07-12");

    private static Metadata metadata;
    private static final Pattern VALIDATOR_READINGS;

    //==========================================================================
    static {
        try {
            InputStream stream = BuParser.class.getClassLoader().getResourceAsStream("resources/meta.xml");
            List<Book> books = XMLParser.parseMetadata(stream);
            metadata = new Metadata();
            for (Book book : books) {
                metadata.addBook(book);
            }
            VALIDATOR_READINGS = Pattern.compile(LocationCalculator.generateValidationPattern(metadata));
        } catch (Exception ex) {
            throw new RuntimeException("Metadata was not properly loaded!!!", ex);
        }
    }

    //==========================================================================
    private static List<String> getDynamicHolidaysForYear(int year) {
        int a = year % 19;
        int b = year % 4;
        int c = year % 7;
        int d = (19 * a + 15) % 30;
        int e = (2 * b + 4 * c + 6 * d + 6) % 7;
        int f = d + e;
        Calendar easter = Calendar.getInstance();
        if (f <= 9) {
            easter.set(year, Calendar.MARCH, f + 22);
        } else {
            easter.set(year, Calendar.APRIL, f - 9);
        }
        switch (year / 100 + 1) {
            case 16:
            case 17:
                easter.add(Calendar.DAY_OF_YEAR, 10);
                break;
            case 18:
                easter.add(Calendar.DAY_OF_YEAR, 11);
                break;
            case 19:
                easter.add(Calendar.DAY_OF_YEAR, 12);
                break;
            case 20:
            case 21:
                easter.add(Calendar.DAY_OF_YEAR, 13);
                break;
            case 22:
                easter.add(Calendar.DAY_OF_YEAR, 14);
                break;
            case 23:
                easter.add(Calendar.DAY_OF_YEAR, 15);
                break;
        }
        List<String> holidays = new ArrayList<>();
        holidays.add(String.format("%1$tm-%1$td", easter.getTime()));
        easter.add(Calendar.DAY_OF_YEAR, -7); //Palm
        holidays.add(String.format("%1$tm-%1$td", easter.getTime()));
        easter.add(Calendar.DAY_OF_YEAR, 7 + 39); //Ascension
        holidays.add(String.format("%1$tm-%1$td", easter.getTime()));
        easter.add(Calendar.DAY_OF_YEAR, 10); //Pentacost
        holidays.add(String.format("%1$tm-%1$td", easter.getTime()));
        return holidays;
    }

    //==========================================================================
    private static String readDataFromURL(String url) throws MalformedURLException, IOException {
        StringBuilder builder = new StringBuilder();

        HttpURLConnection connection = null;
        do {
            connection = (HttpURLConnection) new URL(url).openConnection();
        } while (connection == null || connection.getResponseCode() != 200);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    //==========================================================================
    private static CalendarEntry parseBasicInfo(Calendar day, Collection<String> holidays) throws Exception {
        CalendarEntry entry = new CalendarEntry();

        //--- Check if holiday --- 
        boolean holiday = day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
        if (!holiday) {
            holiday = holidays.contains(String.format("%1$tm-%1$td", day.getTime()));
        }
        entry.setHoliday(holiday);

        //--- Parse title ---
        String titleData = readDataFromURL(String.format(URL_TITLE, day));
        String title = "";
        Matcher titleMatcher = PATTERN_TITLE.matcher(titleData);
        if (titleMatcher.find()) {
            title = titleMatcher.group(1).trim();
            title = title.replaceAll("\\&#769;", "");
            title = title.replaceAll("<a.*?/a>", "");
            title = title.replaceAll("<.*?>", "");
            title = title.replaceAll(" Аллилуия\\.", "");
        }
//        System.out.println(title); //TODO remove
        if (title.isEmpty()) {
            throw new Exception("Empty title: " + titleData);
        }
        int firstDot = title.indexOf('.');
        String subtitle = title.substring(firstDot + 1).trim();
        title = title.substring(0, firstDot);
        entry.setTitle(title);
        entry.setFeast(subtitle);
        entry.setFeastingIndex(0);

        return entry;
    }

    //==========================================================================
    private static String beautifyLink(String link) {
        link = link.replaceAll(", *?\\d+ зач\\.( \\(от полу&#769;\\))? *?,", "");
        link = link.replaceAll("последи\\&#769;", "");
        link = link.replaceAll("\\*", "");
        link = link.replaceAll("^(\\d) ([А-Яа-я])", "$1$2");
        link = link.replaceAll("Сол\\.", "Фес.");
        link = link.replaceAll("Притч\\.", "Прит.");
        link = link.replaceAll("(\\d\\d)\\d\\d\\d", "$1");
        Matcher romanMatcher = PATTERN_ROMAN.matcher(link);
        while (romanMatcher.find()) {
            String numbers = link.substring(romanMatcher.end()).replaceAll("(\\d), (\\d)", "$1,$2");
            link = link.substring(0, romanMatcher.start()) + RomanNumbers.romanToDecimal(romanMatcher.group(1)) + ":" + numbers;
            romanMatcher = PATTERN_ROMAN.matcher(link);
        }
        return link;
    }
    //==========================================================================

    private static Line splitLineAndComment(String data) throws Exception {
        Matcher leftMatcher = PATTERN_COMMENT_LEFT.matcher(data);
        if (leftMatcher.matches()) {
            String link = leftMatcher.group(2);
            link = beautifyLink(link);
            if (VALIDATOR_READINGS.matcher(link).matches()) {
                throw new Exception("Invalid readings: " + link);
            }
            String comment = leftMatcher.group(1).trim();
            if (PATTERN_ROMAN.matcher(comment).find()) {
                throw new Exception("Illegal comment: " + comment);
            }
            return new Line(link, comment);
        } else {
            throw new Exception("Cannot split line: " + data);
        }
    }

    //==========================================================================
    private static List<Line> extractLinesFromComment(String data) throws Exception {
        List<Line> lines = new ArrayList<>();
        Matcher separator = PATTERN_COMMENT_SEPARATOR.matcher(data);
        if (separator.find()) {
            //--- Process left part ---
            String left = data.substring(0, separator.end());
            lines.add(splitLineAndComment(left));
            //--- Process right part ---
            String right = data.substring(separator.end() + 1).trim();
            if (PATTERN_ROMAN.matcher(right).find()) { //2 links present
                Line line = splitLineAndComment(right);
                lines.add(line);
                lines.add(new Line(null, line.getComment()));
            } else { //Only 1 link
                if (right.endsWith(":") || right.endsWith("-") || right.endsWith("–")) {
                    right = right.substring(0, right.length() - 1).trim();
                }
                lines.add(new Line(null, right));
            }
        } else {
            throw new Exception("Illegal comment: " + data);
        }
        return lines;
    }

    //==========================================================================
    private static List<Line> parseReadings(Calendar day) throws Exception {
        List<Line> result = new ArrayList<>();

        //--- Parse readings ---
        String readingsData = readDataFromURL(String.format(URL_READINGS, day));
        String readings = "";
        Matcher readingsMatcher = PATTERN_READINGS.matcher(readingsData);
        if (readingsMatcher.find()) {
            readings = readingsMatcher.group(1).trim() + "&nbsp;";
        }
//            System.out.println(readings); //TODO remove
        if (readings.length() == 0 && !EXCEPTIONAL_DAYS.contains(day.get(Calendar.DAY_OF_YEAR))) {
            throw new Exception("No readings data found" + readingsData);
        }
        int numberOfNbspsCalc = (readings.length() - readings.replaceAll(Pattern.quote("&nbsp;"), "").length()) / "&nbsp;".length();
        Matcher readingMatcher = PATTERN_READING.matcher(readings);
        String lastComment = "";
        int numberOfNbspsEmp = 0;
        while (readingMatcher.find()) {
            String link = readingMatcher.group(2).trim();
            link = beautifyLink(link);
            if (VALIDATOR_READINGS.matcher(link).matches()) {
                throw new Exception("Invalid readings: " + link);
            }

            String comment = readingMatcher.group(1).trim();
            if (PATTERN_ROMAN.matcher(comment).find()) {
                List<Line> lines = extractLinesFromComment(comment);
                lines.get(lines.size() - 1).setLink(link);
                lastComment = lines.get(lines.size() - 1).getComment();
                result.addAll(lines);
                numberOfNbspsEmp++;
            } else {
                if (comment.isEmpty()) {
                    comment = lastComment;
                } else {
                    lastComment = comment;
                }
//            System.out.println("Link: " + link + " Comment: " + comment); //TODO remove

                result.add(new Line(link, comment));
                numberOfNbspsEmp++;
            }
        }
        if (numberOfNbspsCalc != numberOfNbspsEmp) {
            throw new Exception("Not all readings found: " + readings);
        }

        return result;
    }

    //==========================================================================
    private static void parseYear(int year) {
        //--- Determine holidays list ---
        List<String> holidays = getDynamicHolidaysForYear(year);
        holidays.addAll(HOLIDAYS_STATIC);

        //--- Prepare data ---
        StringBuilder info = new StringBuilder();
        new File(String.format(PATH_DIRECTORY, year)).mkdirs();
        int errorCount = 0;
        Calendar currentDay = Calendar.getInstance();
        currentDay.set(year, Calendar.NOVEMBER, 19);

        //--- Run entry loop ---
        while (currentDay.get(Calendar.YEAR) == year) {
            System.out.println(currentDay.get(Calendar.DAY_OF_YEAR) + ": " + currentDay.getTime()); //TODO change

            //--- Parse basic info ---
            CalendarEntry currentEntry;
            try {
                currentEntry = parseBasicInfo(currentDay, holidays);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }

            //--- Parse readings ---
            try {
                List<Line> readings = parseReadings(currentDay);
                for (Line line : readings) {
                    LocationSet locationSet = LocationCalculator.parseSearchString(metadata, line.getLink());
                    if (locationSet == null) {
                        throw new NullPointerException(line.getLink());
                    }
                    Location location = locationSet.getLocations().get(0);
                    if (location.getBook() <= 50) { //Old Testament
                        currentEntry.addReadingsOld(line);
                    } else if (location.getBook() <= 54) {
                        currentEntry.addReadingsGospel(line);
                    } else {
                        currentEntry.addReadingsApostle(line);
                    }
                }

//                System.out.println(currentEntry.toXML()); //TODO remove
                try (PrintWriter writer = new PrintWriter(String.format(PATH_READINGS, year, currentDay.get(Calendar.DAY_OF_YEAR)))) {
                    writer.write(currentEntry.toXML());
                }
            } catch (Exception ex) {
                System.err.println(currentDay.get(Calendar.DAY_OF_YEAR) + ": " + currentDay.getTime()); //TODO change
                ex.printStackTrace();
                errorCount++;
            }

            //--- Append infodata ---
            if (currentDay.get(Calendar.DAY_OF_MONTH) == 1 && currentDay.get(Calendar.MONTH) != Calendar.JANUARY) {
                info.append('\n');
            }
            if (currentDay.get(Calendar.DAY_OF_MONTH) != 1) {
                info.append(',');
            }
            info.append(currentEntry.isHoliday() ? '1' : '0');
            info.append('0');
            currentDay.add(Calendar.DAY_OF_YEAR, 1);
//            break; //TODO remove
        }

        //--- Save info file ---
        try (PrintWriter writer = new PrintWriter(String.format(PATH_INFO, year, currentDay.get(Calendar.DAY_OF_YEAR)))) {
            writer.write(info.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("Errors: " + errorCount);
    }

    public static void main(String[] args) {

        parseYear(2017);
    }

}
