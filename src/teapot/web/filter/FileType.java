package teapot.web.filter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Available file types for merger
 * @see CompilerBean
 */
enum FileType {
    /**
     * JavaScript file type
     */
    SCRIPT("scripts", "application/javascript", "js"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    /**
     * css file type
     */
    STYLE("styles", "text/css", "css", "gss"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    /**
     * soy template file type
     */
    TEMPLATE("templates", "application/javascript", "soy"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    /**
     * Tries to identify the fileType of the path using extensions
     * @param path the path of the file
     * @return the identified file type. null if the file type is not supported
     */
    static FileType fromPath(final Path path) {
        if (path == null) {
            return null;
        }
        final String[] split = path.toString().split("\\."); //$NON-NLS-1$
        final String extension = split[split.length - 1].toLowerCase();
        for (final FileType b : FileType.values()) {
            if (b.extensions.contains(extension)) {
                return b;
            }
        }
        return null;
    }

    /**
     * Returns the FileType corresponding to the Tag Name
     * @param tagName the tag Name
     * @return FileType representing the tag name
     */
    static FileType fromString(final String tagName) {
        if (tagName == null) {
            return null;
        }
        for (final FileType b : FileType.values()) {
            if (tagName.equalsIgnoreCase(b.tagName)) {
                return b;
            }
        }
        return null;
    }

    /**
     * the content type for the FileType
     */
    private String       contentType;
    /**
     * the extensions supported by the file type
     */
    private List<String> extensions;
    /**
     * name of the tag representing the FileType
     */
    private String       tagName;

    /**
     * Private constructor
     * @param tagName name of the tag representing the FileType
     * @param contentType the content type for the FileType
     * @param extensions the extensions supported by the file type
     */
    private FileType(final String tagName, final String contentType, final String... extensions) {
        this.tagName = tagName;
        this.contentType = contentType;
        this.extensions = Arrays.asList(extensions);
    }

    /**
     * Returns the content type for the FileType
     * @return the content type for the FileType
     */
    String getContentType() {
        return this.contentType;
    }
}