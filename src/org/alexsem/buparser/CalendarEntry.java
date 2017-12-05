package org.alexsem.buparser;

import java.util.ArrayList;
import java.util.List;

public class CalendarEntry {

    public static class Line {

        private String link;
        private String comment;

        public Line() {
        }

        public Line(String link, String comment) {
            this.link = link;
            this.comment = comment;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String toXML() {
            return String.format("<r><l>%s</l><c>%s</c></r>", link, comment);
        }

        @Override
        public String toString() {
            return String.format("%s (%s)", link, comment);
        }
        
        

    }

    private boolean holiday;
    private String title;
    private String feast = "";
    private int feastingIndex = 0;

    private List<Line> readingsOld = new ArrayList<>();
    private List<Line> readingsApostle = new ArrayList<>();
    private List<Line> readingsGospel = new ArrayList<>();

    public void setHoliday(boolean holiday) {
        this.holiday = holiday;
    }

    public boolean isHoliday() {
        return holiday;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    public void setFeast(String feast) {
        this.feast = feast;
    }

    public void setFeastingIndex(int feastingIndex) {
        this.feastingIndex = feastingIndex;
    }

    public void addReadingsOld(Line line) {
        this.readingsOld.add(line);
    }

    public void addReadingsApostle(Line line) {
        this.readingsApostle.add(line);
    }

    public void addReadingsGospel(Line line) {
        this.readingsGospel.add(line);
    }

    public String toXML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<entry h=\"").append(holiday).append("\">");
        builder.append("<t>").append(title).append("</t>");
        builder.append("<f>").append(feast).append("</f>");
        builder.append("<fi>").append(feastingIndex).append("</fi>");
        
        builder.append("<o>");
        for (Line line : readingsOld) {
            builder.append(line.toXML());
        }
        builder.append("</o>");

        builder.append("<a>");
        for (Line line : readingsApostle) {
            builder.append(line.toXML());
        }
        builder.append("</a>");

        builder.append("<g>");
        for (Line line : readingsGospel) {
            builder.append(line.toXML());
        }
        builder.append("</g>");

        builder.append("</entry>");
        return builder.toString();
    }

}
