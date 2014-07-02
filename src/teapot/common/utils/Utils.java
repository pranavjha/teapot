package teapot.common.utils;

import java.io.File;
import java.util.regex.Matcher;

/**
 * This class is a utility class with all static utility functions.
 */
public class Utils {
    /**
     * Constructs a url from the path parts, The new returned string is essentially a join of all the input parameters with the '/' character
     * @param pathParts the parts of the path
     * @return the new constructed url
     */
    public static String constructUrl(final String... pathParts) {
        final StringBuilder sb = new StringBuilder();
        for (final String path : pathParts) {
            sb.append("/").append(path); //$NON-NLS-1$
        }
        return sb.substring(1).toString();
    }

    /**
     * Sanitizes the file path and returns a new file path with the file separators fixed to the system file separator
     * <p>
     * <b>Example:</b> if the input is &quot;///somepath////somepath\\\\somepath//&quot;, the output will be &quot;somepath\somepath\somepath&quot;.<br/>
     * The &quot;\&quot; character will be the system dependent {@link File#separator} (&quot;/&quot; for unix)
     * </p>
     * @param path input path to sanitize
     * @return the sanitized path
     */
    public static String sanitizePath(final String path) {
        final String fileSeparator = Matcher.quoteReplacement(File.separator);
        return path.replaceAll("[/\\\\]+", fileSeparator).replaceAll(fileSeparator + "$|^" + fileSeparator, ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
