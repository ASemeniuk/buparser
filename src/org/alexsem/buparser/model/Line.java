package org.alexsem.buparser.model;

/**
 * Class that represents a single line (number and text)
 * @author Semeniuk A.D.
 */
public class Line {
    /**
     * String representation of line number
     */
    private String number;
    /**
     * Text contained within line
     */
    private String csText;
    /**
     * Russian translation
     */
    private String ruText = null;
    /**
     * List of parallel readings
     */
    private String[] parallel = null;
    /**
     * Shows whether current line should be half-visible
     */
    private boolean ghost;

    /**
     * Constructor (blank line)
     */
    public Line() {
        this.number = "";
        this.csText = "";
        this.ruText = "";
        this.ghost = false;
        this.parallel = new String[]{};
    }

    /**
     * Constructor
     * @param number Line number
     */
    public Line(String number) {
        this();
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    public String getCsText() {
        return csText;
    }

    public String getRuText() {
        return ruText;
    }

    public String[] getParallel() {
        return parallel;
    }

    public void setCsText(String csText) {
        this.csText = csText;
    }

    public void setRuText(String ruText) {
        this.ruText = ruText;
    }

    public void parseParallel(String source) {
        this.parallel = source.split(";");
    }

    public boolean isGhost() {
        return ghost;
    }

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }
    
    
}
