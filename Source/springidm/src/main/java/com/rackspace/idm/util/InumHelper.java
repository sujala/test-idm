package com.rackspace.idm.util;

import java.util.Random;

public class InumHelper {

    private static final int NUMBER_OF_HEX_DIGITS = 4;
    private static final int HEX_MAX = 16;

    public static String getRandomInum(int sections) {

        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= sections; i++) {
            if (i == 1) {
                sb.append("!");
            }
            for (int x = 1; x <= NUMBER_OF_HEX_DIGITS; x++) {
                sb.append(getRandomHexDigit());
            }
            if (i < sections) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    private static String getRandomHexDigit() {

        String[] hexDigits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F"};

        Random generator = new Random();
        int randomIndex = generator.nextInt(HEX_MAX);
        return hexDigits[randomIndex];
    }
}
