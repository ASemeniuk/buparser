package org.alexsem.buparser;

import org.alexsem.buparser.util.RomanNumbers;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.alexsem.buparser.CalendarEntry.Line;
import org.alexsem.buparser.model.Book;
import org.alexsem.buparser.model.Location;
import org.alexsem.buparser.model.LocationSet;
import org.alexsem.buparser.model.Metadata;
import org.alexsem.buparser.util.LocationCalculator;
import org.alexsem.buparser.util.XMLParser;

public class BuParser {

    private static final String URL_TITLE = "http://www.patriarchia.ru/bu/%1$tY-%1$tm-%1$td/";
    private static final String URL_READINGS = "http://www.patriarchia.ru/rpc/date=%1$tY-%1$tm-%1$td/kld.xml";

    private static final String PATH_DIRECTORY = "D:/calendar/%04d";
    private static final String PATH_READINGS = PATH_DIRECTORY + "/%03d.xml";
    private static final String PATH_INFO = PATH_DIRECTORY + "/info.csv";

    private static final Pattern PATTERN_TITLE = Pattern.compile("Версия для печати.*?<br> *?<p> *?<b>(.*?)</p>");
    private static final Pattern PATTERN_READINGS = Pattern.compile("<div class=\"read\">(.*?)</div> *?<div class");
    private static final Pattern PATTERN_READING = Pattern.compile("([^<]*?)[,:\\-]? *?<a.*?>([^<]*?)[\\.,]?</a>( *?<div.*?</div>)? *?");
    private static final Pattern PATTERN_DOUBLE_LINE = Pattern.compile("((.*?)[,:\\-]? *?(([123] )?[А-Я][а-я]+?\\.,.*?\\d)\\.?){2}");
    private static final Pattern PATTERN_ROMAN = Pattern.compile("([IVXLCDM]+), ");
    private static final Pattern PATTERN_COMMENT_SEPARATOR = Pattern.compile("\\d\\.");
    private static final Pattern PATTERN_COMMENT_LEFT = Pattern.compile("(.*?)[,:\\-]? *?(([123] )?[А-Я][а-я]+?\\.,.*\\d)\\.?");
    private static final Pattern PATTERN_SUBSTITUTE = Pattern.compile("(.*?) - (за (понедельник|вторник|среду|четверг|пятницу|субботу|воскресенье) и за (понедельник|вторник|среду|четверг|пятницу|субботу|воскресенье))( \\(под зачало\\))?");
    private static final Pattern PATTERN_COMPLEX_GROUPS1 = Pattern.compile("([123]?[А-Я][а-я]*\\. ([0-9]{1,3}):.+?),([0-9]{1,3}) - ([0-9]{1,3}:[0-9]{1,3})");
    private static final Pattern PATTERN_COMPLEX_GROUPS2 = Pattern.compile("([123]?[А-Я][а-я]*\\. .*?[0-9]{1,3}:[0-9]{1,3} - ([0-9]{1,3}):[0-9]{1,3}),(.+?)");

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
        LocalDate easter;
        if (f <= 9) {
            easter = LocalDate.of(year, Month.MARCH, f + 22);
        } else {
            easter = LocalDate.of(year, Month.APRIL, f - 9);
        }
        switch (year / 100 + 1) {
            case 16:
            case 17:
                easter = easter.plusDays(10);
                break;
            case 18:
                easter = easter.plusDays(11);
                break;
            case 19:
                easter = easter.plusDays(12);
                break;
            case 20:
            case 21:
                easter = easter.plusDays(13);
                break;
            case 22:
                easter = easter.plusDays(14);
                break;
            case 23:
                easter = easter.plusDays(15);
                break;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<String> holidays = new ArrayList<>();
        holidays.add(formatter.format(easter)); //Easter
        holidays.add(formatter.format(easter.minusDays(7)));  //Palm
        holidays.add(formatter.format(easter.plusDays(40))); //Ascension
        holidays.add(formatter.format(easter.plusDays(50))); //Pentacost
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
    private static CalendarEntry parseBasicInfo(LocalDate day, Collection<String> holidays) throws Exception {
        CalendarEntry entry = new CalendarEntry();

        //--- Check if holiday --- 
        boolean holiday = day.getDayOfWeek() == DayOfWeek.SUNDAY || day.getDayOfWeek() == DayOfWeek.SATURDAY;
        if (!holiday) {
            holiday = holidays.contains(DateTimeFormatter.ofPattern("MM-dd").format(day));
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
    public static String beautifyLink(String link) throws Exception {
        link = link.replace('–', '-');
        link = link.replaceAll("последи\\&#769;", "");
        link = link.replaceAll(", *?\\d+-\\d+ зач\\.( \\(от полу&#769;\\))? *?,", "");
        link = link.replaceAll(", *?\\d+ зач\\.( \\(от полу&#769;\\))? *?,", "");
        link = link.replaceAll("\\*", "");
        link = link.replaceAll("^(\\d) ([А-Яа-я])", "$1$2");
        link = link.replaceAll("Сол\\.", "Фес.");
        link = link.replaceAll("Притч\\.", "Прит.");
        link = link.replaceAll("Прем\\. Солом\\.", "Прем.");
        link = link.replaceAll(" \\(Недели \\d{1,2}-й\\)", "").trim();
        link = link.replaceAll("(\\d{2})\\d{2,3}", "$1");
        link = link.replaceAll(" \\(о Закхее\\)", "");
        link = link.replaceAll(" \\(о хананеянке\\)", "");
        link = link.replaceAll("; ", ", ");
        if (link.endsWith(";") || link.endsWith(".") || link.endsWith(",")) {
            link = link.substring(0, link.length() - 1);
        }
        Matcher romanMatcher = PATTERN_ROMAN.matcher(link);
        while (romanMatcher.find()) {
            String numbers = link.substring(romanMatcher.end()).replaceAll("(\\d), (\\d)", "$1,$2");
            link = link.substring(0, romanMatcher.start()) + RomanNumbers.romanToDecimal(romanMatcher.group(1)) + ":" + numbers;
            romanMatcher = PATTERN_ROMAN.matcher(link);
        }
        Matcher cgm1 = PATTERN_COMPLEX_GROUPS1.matcher(link);
        if (cgm1.matches()) {
//            System.out.print("CG: " + link + " replaced with "); //TODO remove
            link = String.format("%s, %s:%s - %s", cgm1.group(1), cgm1.group(2), cgm1.group(3), cgm1.group(4));
//            System.out.println(link); //TODO remove
        }
        Matcher cgm2 = PATTERN_COMPLEX_GROUPS2.matcher(link);
        if (cgm2.matches()) {
//            System.out.print("CG: " + link + " replaced with "); //TODO remove
            link = String.format("%s, %s:%s", cgm2.group(1), cgm2.group(2), cgm2.group(3));
//            System.out.println(link); //TODO remove
        }
        if (!VALIDATOR_READINGS.matcher(link.toLowerCase() + ";").matches()) {
            throw new Exception("Invalid readings: " + link);
        }
        return link;
    }
    //==========================================================================

    private static Line splitLineAndComment(String data) throws Exception {
        Matcher leftMatcher = PATTERN_COMMENT_LEFT.matcher(data);
        if (leftMatcher.matches()) {
            String link = leftMatcher.group(2);
            link = beautifyLink(link);
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
        if (data.endsWith(";")) {
            data = data.substring(0, data.length() - 1) + ".";
        }
        data = data.replaceAll(" ?Ев\\. составное:? ?", "");
        List<Line> lines = new ArrayList<>();
        Matcher separator = PATTERN_COMMENT_SEPARATOR.matcher(data);
        if (separator.find()) {
            //--- Process left part ---
            String left = data.substring(0, separator.end());
            lines.add(splitLineAndComment(left));
            //--- Process right part ---
            String right;
            if (separator.end() >= data.length()) {
                right = lines.get(lines.size() - 1).getComment();
            } else {
                right = data.substring(separator.end() + 1).trim();
            }
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
    private static List<Line> parseReadings(LocalDate day) throws Exception {
        List<Line> result = new ArrayList<>();

        //--- Parse readings ---
        String readingsData = readDataFromURL(String.format(URL_READINGS, day));
        readingsData = readingsData.replace('–', '-');
        String readings = "";
        Matcher readingsMatcher = PATTERN_READINGS.matcher(readingsData);
        if (readingsMatcher.find()) {
            readings = readingsMatcher.group(1).trim();
            if (readings.endsWith("&nbsp;")) {
                readings = readings.substring(0, readings.length() - "&nbsp;".length()).trim();
            }
            readings = readings + "&nbsp;";
        }
//            System.out.println(readings); //TODO remove
        if (readings.length() == 0) {
            throw new Exception("No readings data found" + readingsData);
        }
        int numberOfNbspsCalc = (readings.length() - readings.replaceAll(Pattern.quote("&nbsp;"), "").length()) / "&nbsp;".length();
        int numberOfPericopes = (readings.length() - readings.replaceAll(Pattern.quote("зач."), "").length()) / "зач.".length();
        String lastComment = "";
        int numberOfNbspsEmp = 0;
        for (String nbspPart : readings.split("\\&nbsp;")) {

            if (!PATTERN_ROMAN.matcher(nbspPart).find()) {
                System.out.println("Skipped NBSP part: " + nbspPart);
                numberOfNbspsCalc--;
                continue;
            }

            if (PATTERN_DOUBLE_LINE.matcher(nbspPart).matches()) { //2 lines in one nbsp part
                Matcher separator = PATTERN_COMMENT_SEPARATOR.matcher(nbspPart);
                if (separator.find()) {
                    String left = nbspPart.substring(0, separator.end());
                    String right = nbspPart.substring(separator.end() + 1).trim();
                    result.add(splitLineAndComment(left));
                    result.add(splitLineAndComment(right));
                    numberOfNbspsEmp++;
                    continue;
                }
            }

            Matcher readingMatcher = PATTERN_READING.matcher(nbspPart);
            if (readingMatcher.matches()) {

                String link = readingMatcher.group(2).trim();
                link = beautifyLink(link);
                String comment = readingMatcher.group(1).trim();

                if (comment.endsWith(", или")) {
                    result.add(splitLineAndComment(comment.substring(0, comment.length() - ", или".length())));
                    comment = "или";
                }

                Matcher substituteMatcher = PATTERN_SUBSTITUTE.matcher(comment);
                if (substituteMatcher.matches()) {
                    String subLink = substituteMatcher.group(1);
                    subLink = beautifyLink(subLink);
                    result.add(new Line(subLink, lastComment));
                    comment = substituteMatcher.group(2).trim();
                }

                if (PATTERN_ROMAN.matcher(comment).find()) {
                    List<Line> lines = extractLinesFromComment(comment);
                    lines.get(lines.size() - 1).setLink(link);
                    lastComment = lines.get(lines.size() - 1).getComment();
                    result.addAll(lines);
                } else {
                    if (comment.isEmpty()) {
                        comment = lastComment;
                    } else {
                        lastComment = comment;
                    }
//            System.out.println("Link: " + link + " Comment: " + comment); //TODO remove

                    result.add(new Line(link, comment));
                }
                numberOfNbspsEmp++;
            } else if (PATTERN_ROMAN.matcher(nbspPart).find()) {
                Line line = splitLineAndComment(nbspPart);
                if (line.getComment().isEmpty()) {
                    line.setComment(lastComment);
                } else {
                    lastComment = line.getComment();
                }
                result.add(line);
                numberOfNbspsEmp++;
            } else {
                throw new Exception("NBSP part does not match: " + nbspPart);
            }
        }
        if (numberOfNbspsCalc != numberOfNbspsEmp) {
            throw new Exception("Not every reading found (nbsp): " + readings);
        }
        if (result.size() != numberOfPericopes) {
            for (Line line : result) {
                LocationSet locationSet = LocationCalculator.parseSearchString(metadata, line.getLink());
                Location location = locationSet.getLocations().get(0);
                if (location.getBook() <= 50) { //Old Testament
                    numberOfPericopes++;
                }
            }
            if (result.size() != numberOfPericopes) {
                throw new Exception("Not every reading found (pericope): " + readings);
            }
        }
        if (result.isEmpty()) {
            throw new Exception("Zero readings");
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
        LocalDate currentDay = LocalDate.of(year, Month.JANUARY, 1);

        //--- Run entry loop ---
        while (currentDay.getYear() == year) {
            System.out.println(currentDay.getDayOfYear() + ": " + currentDay.toString()); //TODO change

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
            } catch (Exception ex) {
                System.err.println(currentDay.getDayOfYear() + ": " + currentDay.toString()); //TODO change
                ex.printStackTrace();
                errorCount++;
            }

//                System.out.println(currentEntry.toXML()); //TODO remove
            try (PrintWriter writer = new PrintWriter(String.format(PATH_READINGS, year, currentDay.getDayOfYear()))) {
                writer.write(currentEntry.toXML());
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }

            //--- Append infodata ---
            if (currentDay.getDayOfMonth() == 1 && currentDay.getMonth() != Month.JANUARY) {
                info.append('\n');
            }
            if (currentDay.getDayOfMonth() != 1) {
                info.append(',');
            }
            info.append(currentEntry.isHoliday() ? '1' : '0');
            info.append('0');
            currentDay = currentDay.plusDays(1);
//            break; //TODO remove
        }

        //--- Save info file ---
        try (PrintWriter writer = new PrintWriter(String.format(PATH_INFO, year, currentDay.getDayOfYear()))) {
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
