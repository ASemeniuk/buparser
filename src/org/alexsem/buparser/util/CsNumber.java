package org.alexsem.buparser.util;

public abstract class CsNumber {

    private static final String[][] BEAUTIFIERS = {{"а7", "№"}, {"г7", "G"}, {"и7", "}"}, {"i7", "‹"}, {"ч7", "§"}, {"х7", "¦"}, {"р7", "R"}, {"с7", "©"}};

    /**
     * Returns only one-character Slavonic number representations (without title)
     * @param num Number in question
     * @return Number representation
     */
    private static String getBasicCsNumber(int num) {
        switch (num) {
            case 0: //It is used (just to make sure)
                return "";
            case 1:
                return "а";
            case 2:
                return "в";
            case 3:
                return "г";
            case 4:
                return "д";
            case 5:
                return "є";
            case 6:
                return "ѕ";
            case 7:
                return "з";
            case 8:
                return "и";
            case 9:
                return "f";
            case 10:
                return "i";
            case 20:
                return "к";
            case 30:
                return "л";
            case 40:
                return "м";
            case 50:
                return "н";
            case 60:
                return "x";
            case 70:
                return "o";
            case 80:
                return "п";
            case 90:
                return "ч";
            case 100:
                return "р";
            case 200:
                return "с";
            case 300:
                return "т";
            case 400:
                return "µ";
            case 500:
                return "ф";
            case 600:
                return "х";
            case 700:
                return "p";
            case 800:
                return "t";
            case 900:
                return "ц";
            default:
                return "";
        }
    }

    /**
     * Returns the Church-Slavonic representation of a number
     * Works for numbers from 1 to 999
     * @param num Number in question
     * @return Number representation
     */
    private static String generateRawCsNumber(int num) {
        if (num <= 10) {
            return getBasicCsNumber(num) + "7";
        }
        if (num < 20) {
            return getBasicCsNumber(num % 10) + "7" + getBasicCsNumber(10);
        }
        if (num < 100) {
            return getBasicCsNumber(num / 10 * 10) + "7" + getBasicCsNumber(num % 10);
        }
        if (num < 1000) {
            if (num % 100 == 0) {
                return getBasicCsNumber(num) + (num == 800 ? "&" : "7");
            } else if (num % 100 % 10 == 0) {
                return getBasicCsNumber(num / 100 * 100) + (num / 100 == 8 ? "&" : "7") + getBasicCsNumber(num % 100);
            } else if (num % 100 < 10) {
                return getBasicCsNumber(num / 100 * 100) + (num / 100 == 8 ? "&" : "7") + getBasicCsNumber(num % 10);
            } else {
                return getBasicCsNumber(num / 100 * 100) + generateCsNumber(num % 100);
            }
        }
        return "";
    }

    /**
     * Returns the Church-Slavonic representation of a number (beautified)
     * Works for numbers from 1 to 999
     * @param num Number in question
     * @return Number representation
     */
    public static String generateCsNumber(int num) {
        String number = generateRawCsNumber(num);
        if (number.indexOf('7') > -1) {
            for (String[] beautifier : BEAUTIFIERS) {
                number = number.replace(beautifier[0], beautifier[1]);
            }
        }
        return number;
    }
}