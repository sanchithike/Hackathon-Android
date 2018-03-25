package com.roposo.core.events;

import java.util.regex.Pattern;

/**
 * Created by amud on 18/09/17.
 */

public class EventUtil {

    private static Pattern EVENT_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*[a-z0-9]$");

    public static boolean isValid(String str) {
        return EVENT_NAME_PATTERN.matcher(str).find();
    }

}
