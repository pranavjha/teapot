package teapot.web.filter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import teapot.common.utils.Utils;

/**
 * Simple File Visitor implementation to look for a glob pattern in a base Directory
 * @see SimpleFileVisitor
 */
class FileFinder
extends SimpleFileVisitor<Path> {
    /**
     * Enumerates the different kinds of protocol supported by the file finder FileFinder
     */
    static enum Protocol {
        /**
         * the file protocol, picks file from the context base directory
         */
        FILE,
        /**
         * http protocol, picks files from an external server
         */
        HTTP,
        /**
         * the server protocol. picks files form localhost
         */
        SERVER;
    }

    /**
     * the LOG object
     */
    private static final Logger LOG            = Logger.getLogger(FileFinder.class);
    /**
     * the TEMP_FILE_NAME attribute is used to name the temporary files created when a non dynamic resource is pulled form the server or a url
     */
    private static final String TEMP_FILE_NAME = "teapot";                          //$NON-NLS-1$
    /**
     * The path to the base directory used for searching the files. This variable is used to relativize the path for glob matching
     */
    private Path                baseDirectory;
    /**
     * Temporary storage for file list to be served
     */
    private List<Path>          fileList;
    /**
     * The string pattern used for matching
     */
    private String              fileName;
    /**
     * specify if the files found is to be included or excluded
     */
    private final boolean       include;
    /**
     * The path matcher used for matching
     */
    private PathMatcher         matcher;
    /**
     * the protocol to find files with
     */
    private final Protocol      protocol;

    /**
     * Instantiates FileFinder.
     * @param protocol the protocol to find files with
     * @param include whether the file is to be included (true) or excluded (false)
     */
    FileFinder(final String protocol, final boolean include) {
        this.protocol = Protocol.valueOf(protocol);
        this.include = include;
    }

    /**
     * @return the include
     * @see FileFinder#include
     */
    public boolean isInclude() {
        return this.include;
    }

    /**
     * No Added functionality. Continues search
     * @see java.nio.file.SimpleFileVisitor#preVisitDirectory(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
    throws IOException {
        return FileVisitResult.CONTINUE;
    }

    /**
     * If the file matches the glob pattern, Adds it into the list of files to be returned and continues the search after that.
     * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
    throws IOException {
        final Path name = this.baseDirectory.relativize(file);
        if (name != null && this.matcher.matches(name)) {
            this.fileList.add(file);
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Logs an error when a file cannot be visited and continues the search
     * @see java.nio.file.SimpleFileVisitor#visitFileFailed(java.lang.Object, java.io.IOException)
     */
    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
        FileFinder.LOG.error(file, exc);
        return FileVisitResult.CONTINUE;
    }

    /**
     * This function finds the files with the basedir, the glob pattern and the protocol. If the protocol is anything other than FILE, the glob
     * pattern is considered as the file path and only one file is returned.
     * @param baseDir The context relative base directory for search
     * @param contextPath the context path for the application
     * @param localhostUrlPrefix the localhost url prefix
     * @return list of file paths matching the criteria
     * @throws IOException
     */
    List<Path> findFiles(final String baseDir, final String contextPath, final String localhostUrlPrefix)
    throws IOException {
        this.fileList = new ArrayList<>();
        Path temp;
        URL website;
        switch (this.protocol) {
        case FILE:
            this.baseDirectory = Paths.get(contextPath, baseDir);
            this.matcher = FileSystems.getDefault().getPathMatcher("glob:" + this.fileName); //$NON-NLS-1$
            Files.walkFileTree(this.baseDirectory, this);
            break;
        case HTTP:
            temp = File.createTempFile(FileFinder.TEMP_FILE_NAME, null).toPath();
            FileFinder.LOG.debug(String.format("Creating temporary file for %s at location '%s'", this.fileName, temp)); //$NON-NLS-1$
            website = new URL(this.fileName);
            Files.copy(website.openStream(), temp, StandardCopyOption.REPLACE_EXISTING);
            this.fileList.add(temp);
            break;
        case SERVER:
            temp = File.createTempFile(FileFinder.TEMP_FILE_NAME, null).toPath();
            FileFinder.LOG.debug(String.format("Creating temporary file for %s at location '%s'", this.fileName, temp)); //$NON-NLS-1$
            website = new URL(Utils.constructUrl(localhostUrlPrefix, baseDir, this.fileName));
            Files.copy(website.openStream(), temp, StandardCopyOption.REPLACE_EXISTING);
            this.fileList.add(temp);
            break;
        default:
            // impossible case
            break;
        }
        return this.fileList;
    }

    /**
     * sets the file name or the file pattern for finding the files. Only if the protocol is FILE, the name is used as a search pattern
     * @param fileName the file name or the file pattern for finding the files. Only if the protocol is FILE, the name is used as a search pattern
     */
    void setPattern(final String fileName) {
        this.fileName = fileName;
    }
}
