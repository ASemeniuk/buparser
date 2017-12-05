package org.alexsem.buparser.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RomanNumbers {

    public static final Pattern VALIDATOR = Pattern.compile("(M{0,3})(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})");
    private static final Pattern ROMAN_PATTERN = Pattern.compile("M|CM|D|CD|C|XC|L|XL|X|IX|V|IV|I");
    private static final int[] DECIMAL_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_NUMERALS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
    
    public static boolean validateRomanNumber(String roman) {
        return roman != null && !roman.isEmpty() && VALIDATOR.matcher(roman).matches();
    }
    
    public static int romanToDecimal(String roman) {
        if (!validateRomanNumber(roman)) {
            return -1;
        }
        final Matcher matcher = ROMAN_PATTERN.matcher(roman);
        int result = 0;
        while (matcher.find()) {
            for (int i = 0; i < ROMAN_NUMERALS.length; i++) {
                if (ROMAN_NUMERALS[i].equals(matcher.group(0))) {
                    result += DECIMAL_VALUES[i];
                }
            }
        }
        return result;
    }

}
