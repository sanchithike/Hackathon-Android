package com.roposo.core.util;

import static android.text.TextUtils.isEmpty;

/**
 * @Author : Anil Sharma on 23/10/16.
 */

public class StringUtils {

    public static boolean isNumeric(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
