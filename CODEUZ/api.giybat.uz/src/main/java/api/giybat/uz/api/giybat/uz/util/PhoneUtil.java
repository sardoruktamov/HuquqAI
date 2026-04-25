package api.giybat.uz.api.giybat.uz.util;

import java.util.regex.Pattern;

public class PhoneUtil {
    public static boolean isPhone(String value) {
        String phoneRegex = "^998\\d{9}$";
        return Pattern.matches(phoneRegex,value);

    }
}
