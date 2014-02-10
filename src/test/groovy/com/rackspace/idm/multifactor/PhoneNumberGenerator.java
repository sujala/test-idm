package com.rackspace.idm.multifactor;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Generates a phone number that appears valid but is not actually assigned to a user because it is a reserved number. There are a finite number
 * of reserved numbers that will pass google's isValid check so there is a realistic chance that a number will be repeated. In a random test generating
 * 1000 numbers, 8 were duplicates.
 */
public final class PhoneNumberGenerator {
    private static PhoneNumberGenerator instance = new PhoneNumberGenerator();

    /**
     * List of valid area codes that can be used and will pass google's libphonenumber isValid function
     */
    private static List<Integer> areaCodes = new ArrayList<Integer>(Arrays.asList(
            201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 212, 213, 214, 215
            , 216, 217, 218, 219, 224, 225, 226, 228, 229, 231, 234, 236, 239, 240
            , 242, 246, 248, 250, 251, 252, 253, 254, 256, 260, 262, 267, 269, 270
            , 276, 281, 283, 289, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310
            , 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 323, 325, 330, 331
            , 334, 336, 337, 339, 340, 347, 351, 352, 360, 361, 380, 385, 386, 401
            , 402, 403, 404, 405, 406, 407, 408, 409, 410, 412, 413, 414, 415, 416
            , 417, 418, 419, 423, 424, 425, 430, 431, 432, 434, 435, 438, 440, 442
            , 443, 450, 456, 469, 470, 475, 478, 479, 480, 484, 500, 501, 502, 503
            , 504, 505, 506, 507, 508, 509, 510, 512, 513, 514, 515, 516, 517, 518
            , 519, 520, 530, 539, 540, 541, 551, 559, 561, 562, 563, 567, 570, 571
            , 573, 574, 575, 580, 585, 586, 587, 600, 601, 602, 603, 604, 605, 606
            , 607, 608, 609, 610, 612, 613, 614, 615, 616, 617, 618, 619, 620, 623
            , 626, 630, 631, 636, 639, 641, 646, 647, 650, 651, 657, 660, 661, 662
            , 669, 670, 671, 678, 681, 682, 701, 702, 703, 704, 705, 706, 707
            , 708, 709, 710, 712, 713, 714, 715, 716, 717, 718, 719, 720, 724, 727
            , 731, 732, 734, 740, 747, 754, 757, 760, 762, 763, 765, 769, 770, 772
            , 773, 774, 775, 778, 779, 780, 781, 784, 785, 786, 787, 801, 802, 803
            , 804, 805, 806, 807, 808, 809, 810, 812, 813, 814, 815, 816, 817, 818
            , 819, 828, 829, 830, 831, 832, 835, 843, 845, 847, 848, 849, 850, 855
            , 856, 857, 858, 859, 860, 862, 863, 864, 865, 866, 867, 870, 872, 876
            , 877, 878, 900, 901, 902, 903, 904, 905, 906, 907, 908, 909, 910, 912
            , 913, 914, 915, 916, 917, 918, 919, 920, 925, 928, 931, 936, 937, 939
            , 940, 941, 947, 949, 951, 952, 954, 956, 970, 971, 972, 973, 975, 978
            , 979, 980, 984, 985, 989));

    private PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    /**
     * XXX-555-01XX is reserved for test phone numbers that are not be assigned to real users. So generate a phone
     * number per this pattern
     */
    private String testNumberForSub = "55501";

    private Random ran = new Random();
    private final int MAX_RANDOM_NUM = 99;
    private final int RANDOM_BIT_LENGTH = String.valueOf(MAX_RANDOM_NUM).length();

    private PhoneNumberGenerator() {
    }

    public static PhoneNumberGenerator getInstance() {
        return instance;
    }

    public static Phonenumber.PhoneNumber randomUSNumber() {
        return getInstance().internalRandomUSNumber();
    }

    public static String randomUSNumberAsString() {
        return canonicalizePhoneNumberToString(randomUSNumber());
    }

    public static String canonicalizePhoneNumberToString(Phonenumber.PhoneNumber phoneNumber) {
        return getInstance().internalCanonicalizePhoneNumberToString(phoneNumber);
    }

    private Phonenumber.PhoneNumber internalRandomUSNumber() {
        try {
            String randomVals = getPaddedRandomInt(MAX_RANDOM_NUM, RANDOM_BIT_LENGTH, '0');
            int ranAreaCodeIndex = ran.nextInt(areaCodes.size());
            Integer areaCodeInt = areaCodes.get(ranAreaCodeIndex);
            String areaCode = areaCodeInt.toString();

            String finalNumber = areaCode + testNumberForSub + randomVals;
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(finalNumber, "US");

            //ensure it's "valid"
            if (!phoneNumberUtil.isValidNumber(number)) {
                throw new RuntimeException("Generated phone number '" + finalNumber + "' is not valid");
            }

            return number;
        } catch (NumberParseException e) {
            throw new RuntimeException("Error generating random phone number");
        }
    }

    private String getPaddedRandomInt(int maxInteger, int paddedSize, char padChar) {
        return StringUtils.leftPad(String.valueOf(ran.nextInt(maxInteger)), paddedSize, padChar);
    }

    private String internalCanonicalizePhoneNumberToString(Phonenumber.PhoneNumber phoneNumber) {
        return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    }

}
