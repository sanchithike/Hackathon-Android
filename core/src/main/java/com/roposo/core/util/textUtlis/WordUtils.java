package com.roposo.core.util.textUtlis;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

import com.roposo.core.util.AndroidUtilities;

import java.util.ArrayList;


/**
 * @author muddassir on 3/14/16.
 */
public class WordUtils {

    // Wrapping
    //-----------------------------------------------------------------------

    /**
     * <p>Wraps a single line of text, identifying words by <code>' '</code>.</p>
     * <p/>
     * <p>New lines will be separated by the system property line separator.
     * Very long words, such as URLs will <i>not</i> be wrapped.</p>
     * <p/>
     * <p>Leading spaces on a new line are stripped.
     * Trailing spaces are not stripped.</p>
     * <p>
     * <pre>
     * WordUtils.wrap(null, *) = null
     * WordUtils.wrap("", *) = ""
     * </pre>
     *
     * @param str        the String to be word wrapped, may be null
     * @param wrapLength the column to wrap the words at, less than 1 is treated as 1
     * @return a line with newlines inserted, <code>null</code> if null input
     */
    public static String wrap(String str, int wrapLength) {
        return wrap(str, wrapLength, null, false);
    }

    /**
     * <p>Wraps a single line of text, identifying words by <code>' '</code>.</p>
     * <p/>
     * <p>Leading spaces on a new line are stripped.
     * Trailing spaces are not stripped.</p>
     * <p>
     * <pre>
     * WordUtils.wrap(null, *, *, *) = null
     * WordUtils.wrap("", *, *, *) = ""
     * </pre>
     *
     * @param str           the String to be word wrapped, may be null
     * @param wrapLength    the column to wrap the words at, less than 1 is treated as 1
     * @param newLineStr    the string to insert for a new line,
     *                      <code>null</code> uses the system property line separator
     * @param wrapLongWords true if long words (such as URLs) should be wrapped
     * @return a line with newlines inserted, <code>null</code> if null input
     */
    public static String wrap(String str, int wrapLength, String newLineStr, boolean wrapLongWords) {
        if (str == null) {
            return null;
        }
        if (newLineStr == null) {
            newLineStr = System.getProperty("line.separator", "\n");
        }
        if (wrapLength < 1) {
            wrapLength = 1;
        }
        int inputLineLength = str.length();
        int offset = 0;
        StringBuffer wrappedLine = new StringBuffer(inputLineLength + 32);

        while ((inputLineLength - offset) > wrapLength) {
            if (str.charAt(offset) == ' ') {
                offset++;
                continue;
            }
            int spaceToWrapAt = str.lastIndexOf(' ', wrapLength + offset);

            if (spaceToWrapAt >= offset) {
                // normal case
                wrappedLine.append(str.substring(offset, spaceToWrapAt));
                wrappedLine.append(newLineStr);
                offset = spaceToWrapAt + 1;

            } else {
                // really long word or URL
                if (wrapLongWords) {
                    // wrap really long word one line at a time
                    wrappedLine.append(str.substring(offset, wrapLength + offset));
                    wrappedLine.append(newLineStr);
                    offset += wrapLength;
                } else {
                    // do not wrap really long word, just extend beyond limit
                    spaceToWrapAt = str.indexOf(' ', wrapLength + offset);
                    if (spaceToWrapAt >= 0) {
                        wrappedLine.append(str.substring(offset, spaceToWrapAt));
                        wrappedLine.append(newLineStr);
                        offset = spaceToWrapAt + 1;
                    } else {
                        wrappedLine.append(str.substring(offset));
                        offset = inputLineLength;
                    }
                }
            }
        }

        // Whatever is left in line is short enough to just pass through
        wrappedLine.append(str.substring(offset));

        return wrappedLine.toString();
    }

    // Capitalizing
    //-----------------------------------------------------------------------

    /**
     * <p>Capitalizes all the whitespace separated words in a String.
     * Only the first letter of each word is changed. To convert the
     * rest of each word to lowercase at the same time,
     * use {@link #capitalizeFully(String)}.</p>
     * <p/>
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.
     * Capitalization uses the unicode title case, normally equivalent to
     * upper case.</p>
     * <p>
     * <pre>
     * WordUtils.capitalize(null)        = null
     * WordUtils.capitalize("")          = ""
     * WordUtils.capitalize("i am FINE") = "I Am FINE"
     * </pre>
     *
     * @param str the String to capitalize, may be null
     * @return capitalized String, <code>null</code> if null String input
     * @see #uncapitalize(String)
     * @see #capitalizeFully(String)
     */
    public static String capitalize(String str) {
        return capitalize(str, null);
    }

    /**
     * <p>Capitalizes all the delimiter separated words in a String.
     * Only the first letter of each word is changed. To convert the
     * rest of each word to lowercase at the same time,
     * use {@link #capitalizeFully(String, char[])}.</p>
     * <p/>
     * <p>The delimiters represent a set of characters understood to separate words.
     * The first string character and the first non-delimiter character after a
     * delimiter will be capitalized. </p>
     * <p/>
     * <p>A <code>null</code> input String returns <code>null</code>.
     * Capitalization uses the unicode title case, normally equivalent to
     * upper case.</p>
     * <p>
     * <pre>
     * WordUtils.capitalize(null, *)            = null
     * WordUtils.capitalize("", *)              = ""
     * WordUtils.capitalize(*, new char[0])     = *
     * WordUtils.capitalize("i am fine", null)  = "I Am Fine"
     * WordUtils.capitalize("i aM.fine", {'.'}) = "I aM.Fine"
     * </pre>
     *
     * @param str        the String to capitalize, may be null
     * @param delimiters set of characters to determine capitalization, null means whitespace
     * @return capitalized String, <code>null</code> if null String input
     * @see #uncapitalize(String)
     * @see #capitalizeFully(String)
     * @since 2.1
     */
    public static String capitalize(String str, char[] delimiters) {
        int delimLen = (delimiters == null ? -1 : delimiters.length);
        if (str == null || str.length() == 0 || delimLen == 0) {
            return str;
        }
        int strLen = str.length();
        StringBuffer buffer = new StringBuffer(strLen);
        boolean capitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = str.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                buffer.append(ch);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer.append(Character.toTitleCase(ch));
                capitalizeNext = false;
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Converts all the whitespace separated words in a String into capitalized words,
     * that is each word is made up of a titlecase character and then a series of
     * lowercase characters.  </p>
     * <p/>
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.
     * Capitalization uses the unicode title case, normally equivalent to
     * upper case.</p>
     * <p>
     * <pre>
     * WordUtils.capitalizeFully(null)        = null
     * WordUtils.capitalizeFully("")          = ""
     * WordUtils.capitalizeFully("i am FINE") = "I Am Fine"
     * </pre>
     *
     * @param str the String to capitalize, may be null
     * @return capitalized String, <code>null</code> if null String input
     */
    public static String capitalizeFully(String str) {
        return capitalizeFully(str, null);
    }

    /**
     * <p>Converts all the delimiter separated words in a String into capitalized words,
     * that is each word is made up of a titlecase character and then a series of
     * lowercase characters. </p>
     * <p/>
     * <p>The delimiters represent a set of characters understood to separate words.
     * The first string character and the first non-delimiter character after a
     * delimiter will be capitalized. </p>
     * <p/>
     * <p>A <code>null</code> input String returns <code>null</code>.
     * Capitalization uses the unicode title case, normally equivalent to
     * upper case.</p>
     * <p>
     * <pre>
     * WordUtils.capitalizeFully(null, *)            = null
     * WordUtils.capitalizeFully("", *)              = ""
     * WordUtils.capitalizeFully(*, null)            = *
     * WordUtils.capitalizeFully(*, new char[0])     = *
     * WordUtils.capitalizeFully("i aM.fine", {'.'}) = "I am.Fine"
     * </pre>
     *
     * @param str        the String to capitalize, may be null
     * @param delimiters set of characters to determine capitalization, null means whitespace
     * @return capitalized String, <code>null</code> if null String input
     * @since 2.1
     */
    public static String capitalizeFully(String str, char[] delimiters) {
        int delimLen = (delimiters == null ? -1 : delimiters.length);
        if (str == null || str.length() == 0 || delimLen == 0) {
            return str;
        }
        str = str.toLowerCase();
        return capitalize(str, delimiters);
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Uncapitalizes all the whitespace separated words in a String.
     * Only the first letter of each word is changed.</p>
     * <p/>
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.</p>
     * <p>
     * <pre>
     * WordUtils.uncapitalize(null)        = null
     * WordUtils.uncapitalize("")          = ""
     * WordUtils.uncapitalize("I Am FINE") = "i am fINE"
     * </pre>
     *
     * @param str the String to uncapitalize, may be null
     * @return uncapitalized String, <code>null</code> if null String input
     * @see #capitalize(String)
     */
    public static String uncapitalize(String str) {
        return uncapitalize(str, null);
    }

    /**
     * <p>Uncapitalizes all the whitespace separated words in a String.
     * Only the first letter of each word is changed.</p>
     * <p/>
     * <p>The delimiters represent a set of characters understood to separate words.
     * The first string character and the first non-delimiter character after a
     * delimiter will be uncapitalized. </p>
     * <p/>
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.</p>
     * <p>
     * <pre>
     * WordUtils.uncapitalize(null, *)            = null
     * WordUtils.uncapitalize("", *)              = ""
     * WordUtils.uncapitalize(*, null)            = *
     * WordUtils.uncapitalize(*, new char[0])     = *
     * WordUtils.uncapitalize("I AM.FINE", {'.'}) = "i AM.fINE"
     * </pre>
     *
     * @param str        the String to uncapitalize, may be null
     * @param delimiters set of characters to determine uncapitalization, null means whitespace
     * @return uncapitalized String, <code>null</code> if null String input
     * @see #capitalize(String)
     * @since 2.1
     */
    public static String uncapitalize(String str, char[] delimiters) {
        int delimLen = (delimiters == null ? -1 : delimiters.length);
        if (str == null || str.length() == 0 || delimLen == 0) {
            return str;
        }
        int strLen = str.length();
        StringBuffer buffer = new StringBuffer(strLen);
        boolean uncapitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = str.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                buffer.append(ch);
                uncapitalizeNext = true;
            } else if (uncapitalizeNext) {
                buffer.append(Character.toLowerCase(ch));
                uncapitalizeNext = false;
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Swaps the case of a String using a word based algorithm.</p>
     * <p/>
     * <ul>
     * <li>Upper case character converts to Lower case</li>
     * <li>Title case character converts to Lower case</li>
     * <li>Lower case character after Whitespace or at start converts to Title case</li>
     * <li>Other Lower case character converts to Upper case</li>
     * </ul>
     * <p/>
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.</p>
     * <p>
     * <pre>
     * StringUtils.swapCase(null)                 = null
     * StringUtils.swapCase("")                   = ""
     * StringUtils.swapCase("The dog has a BONE") = "tHE DOG HAS A bone"
     * </pre>
     *
     * @param str the String to swap case, may be null
     * @return the changed String, <code>null</code> if null String input
     */
    public static String swapCase(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        StringBuffer buffer = new StringBuffer(strLen);

        boolean whitespace = true;
        char ch = 0;
        char tmp = 0;

        for (int i = 0; i < strLen; i++) {
            ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                tmp = Character.toLowerCase(ch);
            } else if (Character.isTitleCase(ch)) {
                tmp = Character.toLowerCase(ch);
            } else if (Character.isLowerCase(ch)) {
                if (whitespace) {
                    tmp = Character.toTitleCase(ch);
                } else {
                    tmp = Character.toUpperCase(ch);
                }
            } else {
                tmp = ch;
            }
            buffer.append(tmp);
            whitespace = Character.isWhitespace(ch);
        }
        return buffer.toString();
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Extracts the initial letters from each word in the String.</p>
     * <p/>
     * <p>The first letter of the string and all first letters after
     * whitespace are returned as a new string.
     * Their case is not changed.</p>
     * <p/>
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.</p>
     * <p>
     * <pre>
     * WordUtils.initials(null)             = null
     * WordUtils.initials("")               = ""
     * WordUtils.initials("Ben John Lee")   = "BJL"
     * WordUtils.initials("Ben J.Lee")      = "BJ"
     * </pre>
     *
     * @param str the String to get initials from, may be null
     * @return String of initial letters, <code>null</code> if null String input
     * @see #initials(String, char[])
     * @since 2.2
     */
    public static String initials(String str) {
        return initials(str, null);
    }

    /**
     * <p>Extracts the initial letters from each word in the String.</p>
     * <p/>
     * <p>The first letter of the string and all first letters after the
     * defined delimiters are returned as a new string.
     * Their case is not changed.</p>
     * <p/>
     * <p>If the delimiters array is null, then Whitespace is used.
     * Whitespace is defined by {@link Character#isWhitespace(char)}.
     * A <code>null</code> input String returns <code>null</code>.
     * An empty delimiter array returns an empty String.</p>
     * <p>
     * <pre>
     * WordUtils.initials(null, *)                = null
     * WordUtils.initials("", *)                  = ""
     * WordUtils.initials("Ben John Lee", null)   = "BJL"
     * WordUtils.initials("Ben J.Lee", null)      = "BJ"
     * WordUtils.initials("Ben J.Lee", [' ','.']) = "BJL"
     * WordUtils.initials(*, new char[0])         = ""
     * </pre>
     *
     * @param str        the String to get initials from, may be null
     * @param delimiters set of characters to determine words, null means whitespace
     * @return String of initial letters, <code>null</code> if null String input
     * @see #initials(String)
     * @since 2.2
     */
    public static String initials(String str, char[] delimiters) {
        if (str == null || str.length() == 0) {
            return str;
        }
        if (delimiters != null && delimiters.length == 0) {
            return "";
        }
        int strLen = str.length();
        char[] buf = new char[strLen / 2 + 1];
        int count = 0;
        boolean lastWasGap = true;
        for (int i = 0; i < strLen; i++) {
            char ch = str.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                lastWasGap = true;
            } else if (lastWasGap) {
                buf[count++] = ch;
                lastWasGap = false;
            } else {
                // ignore ch
            }
        }
        return new String(buf, 0, count);
    }

    //-----------------------------------------------------------------------

    /**
     * Is the character a delimiter.
     *
     * @param ch         the character to check
     * @param delimiters the delimiters
     * @return true if it is a delimiter
     */
    private static boolean isDelimiter(char ch, char[] delimiters) {
        if (delimiters == null) {
            return Character.isWhitespace(ch);
        }
        for (int i = 0, isize = delimiters.length; i < isize; i++) {
            if (ch == delimiters[i]) {
                return true;
            }
        }
        return false;
    }


    public static int nLastIndexOf(String str, char c, int nLastIndex) {

        int i;
        int second_last_index = 0;
        for (i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) == c)
                second_last_index++;
            if (second_last_index == nLastIndex)
                break;
        }
        return i + 1;
    }

    public static String getExtention(String url) {
        int index = nLastIndexOf(url, '.', 1) - 1;
        return url.substring(index);
    }

    public static String getFileNameFromUrl(String url) {
        int index = nLastIndexOf(url, '.', 1) - 1;
        return url.substring(0, index);
    }

    public static String removeSubString(String url, String subString) {
        if (url.contains(subString)) {
            url = url.replace(subString, "");
        }
        return url;
    }

    public final static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public static String getUserIconText(String userName) {
        if (!TextUtils.isEmpty(userName)) {
            userName = userName.trim();
        }
        String result = "";
        if (!TextUtils.isEmpty(userName)) {
            String[] userInitials = userName.split(" ");
            for (int i = 0; userInitials.length > 0 && i < userInitials.length && i < 2; i++) {
                if (userInitials[i].length() > 0) {
                    result += (userInitials[i].charAt(0) + "").toUpperCase();
                }
            }
            return result;
        }
        return "R";
    }

    public static int getTextWidth(String text, Typeface typeface, float textSize) {
        Rect bounds = new Rect();
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        paint.setTypeface(typeface);
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds.width();
    }

    public static int getLineCount(String text, float textSize, int leftPadding, int rightPadding) {
        Rect bounds = new Rect();
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        paint.getTextBounds(text, 0, text.length(), bounds);

        int widthPixels = AndroidUtilities.widthInPixel() - leftPadding - rightPadding;
        int width = bounds.width();
        return (int) Math.ceil(width / (float) widthPixels) + countLines(text);
    }

    private static int countLines(String str) {
        String[] lines = str.split("\r\n|\r|\n");
        return lines.length - 1;
    }

    public static String formatStringToFitWidth(String mText, int mMaxWidth, TextPaint mTextPaint) {
        String[] textArray = mText.split("[ \n]");
        ArrayList<String> outputs = new ArrayList<>();
        int count = 0;
        do {
            count = computeTextForLine(textArray, mMaxWidth, mTextPaint, count, outputs);
        } while (count < textArray.length && count != -1);

        String formattedString = "";
        for (int i = 0; i < outputs.size(); i++) {
            String s = outputs.get(i);
            if (i == outputs.size() - 1) {
                formattedString += s.trim();
            } else {
                formattedString += s.trim() + "\n";
            }
        }
        return formattedString;
    }

    private static int computeTextForLine(String[] textArray, int fixedWidth, TextPaint mTextPaint, int count, ArrayList<String> outputs) {
        String newText = "";
        Rect mTextBounds = new Rect();
        for (int i = count; i < textArray.length; i++) {
            String tempText;
            String nextWord = textArray[i];
            if (newText.isEmpty()) {
                tempText = nextWord;
            } else {
                tempText = newText + " " + nextWord;
            }
            mTextPaint.getTextBounds(tempText, 0, tempText.length(), mTextBounds);
            if (mTextBounds.width() <= fixedWidth) {
                newText = tempText;
                count++;
            } else {
                break;
            }
        }
        if (newText.isEmpty()) {
            newText = textArray[count];
            count = -1;
        }
        newText = newText.trim();
        if (outputs != null) {
            outputs.add(newText);
        }
        return count;
    }
}

