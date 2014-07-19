package teapot.web.filter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import teapot.common.utils.Utils;
import teapot.web.filter.ConfigHandler.XMLAttributes;

import com.google.javascript.jscomp.CompilationLevel;

/**
 * This class stores and processes individual merge requests configured in FileMergeLister configuration file.
 * @see CompilerFilter
 */
class CompilerBean {
    /**
     * the LOG object
     */
    private static final Logger    LOG = Logger.getLogger(CompilerBean.class);
    /**
     * base directory for the search
     */
    private String                 baseDirectory;
    /**
     * Compilation Level for the file merge
     */
    private CompilationLevel       compilationLevel;
    /**
     * dependency string passed on to the bean from xml
     */
    private String                 dependencies;
    /**
     * File type for the merger
     * @see FileType
     */
    private final FileType         fileType;
    /**
     * the directory to put the merged file in
     */
    private String                 mergeDirectory;
    /**
     * Name of the merged file to be created
     */
    private String                 name;
    /**
     * variable to store search pattern for including and excluding files<br/>
     */
    private final List<FileFinder> searchPatterns;

    /**
     * Constructor function. Instantiates FileMerger with values
     * @param compilationLevel the compilation level to use
     * @param baseDirectory context relative base directory to search files from
     * @param mergeDirectory context relative merge directory to store generated files
     * @param fileType the file type
     */
    CompilerBean(final CompilationLevel compilationLevel, final String baseDirectory, final String mergeDirectory, final FileType fileType) {
        super();
        this.compilationLevel = compilationLevel;
        this.baseDirectory = baseDirectory;
        this.mergeDirectory = mergeDirectory;
        this.fileType = fileType;
        this.searchPatterns = new ArrayList<>();
    }

    /**
     * @return the compilationLevel
     * @see CompilerBean#compilationLevel
     */
    public CompilationLevel getCompilationLevel() {
        return this.compilationLevel;
    }

    /**
     * Returns the context relative merged file path
     * @return the context relative merged file path
     */
    public String getMergedFile() {
        return Utils.sanitizePath(this.mergeDirectory + File.separator + this.name);
    }

    /**
     * @param baseDirectory the baseDirectory to set
     * @see CompilerBean#baseDirectory
     */
    public void setBaseDirectory(final String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    /**
     * @param mergeDirectory the mergeDirectory to set
     * @see CompilerBean#mergeDirectory
     */
    public void setMergeDirectory(final String mergeDirectory) {
        this.mergeDirectory = mergeDirectory;
    }

    /**
     * Includes the files identified by the search pattern from aggregation
     * @param contextPath the context path used for searching the files
     * @param localhostUrlPrefix the localhost url prefix
     * @throws IOException if the {@link FileFinder#findFiles(String, String, String)} throws an IOException
     * @return the list of files to be included for compilation
     */
    private List<Path> findFilesFromPattern(final String contextPath, final String localhostUrlPrefix)
    throws IOException {
        final List<Path> fileList = new ArrayList<>();
        List<Path> listPerIteration;
        for (final FileFinder searchPattern : this.searchPatterns) {
            if (searchPattern.isInclude()) {
                listPerIteration = searchPattern.findFiles(this.baseDirectory, contextPath, localhostUrlPrefix);
                // if the files are not in the file list, include it
                for (final Path onePath : listPerIteration) {
                    if (!fileList.contains(onePath)) {
                        fileList.add(onePath);
                    }
                }
            } else {
                listPerIteration = searchPattern.findFiles(this.baseDirectory, contextPath, localhostUrlPrefix);
                // remove the files found
                fileList.removeAll(listPerIteration);
            }
        }
        return fileList;
    }

    /**
     * Creates a dependency list based on a comma separated list of files sent in a string to the function
     * @return the list of path dependencies
     * @see CompilerBean#dependencies
     */
    private List<String> pathDependencies() {
        final List<String> dependencyList = new ArrayList<>();
        if (null == this.dependencies) {
            return new ArrayList<>();
        }
        final String[] dependencyArray = this.dependencies.split(XMLAttributes.DEPENDENCY_REGEX);
        for (final String dependency : dependencyArray) {
            dependencyList.add(Utils.sanitizePath(dependency));
        }
        return dependencyList;
    }

    /**
     * Adds a file search pattern in the compiler. File search patterns are used to search and compile the files at runtime
     * @param fileFinder the object specifying the file finder pattern
     */
    void addSearchPattern(final FileFinder fileFinder) {
        this.searchPatterns.add(fileFinder);
    }

    /**
     * Creates a clone of the current compiler bean with values of compression, baseDir, mergeDir and fileType copied
     * @return a new CompilerBean
     */
    CompilerBean createFromTemplate() {
        return new CompilerBean(this.compilationLevel, this.baseDirectory, this.mergeDirectory, this.fileType);
    }

    /**
     * Does a final aggregation of files to create a new file. Calls FileCompiler functions when required.
     * @param compilerBeanMap the compilerBeanMap to resolve dependencies
     * @param contextPath the context path of the application
     * @param localhostUrlPrefix the url prefix for localhost
     * @return The final merged file type
     * @see FileCompiler
     * @throws IOException if file reading/writing fails
     */
    FileType merge(final Map<String, CompilerBean> compilerBeanMap, final String contextPath, final String localhostUrlPrefix)
    throws IOException {
        // recursively resolve dependencies if there are any
        final List<String> dependencyList = this.pathDependencies();
        for (final String dependency : dependencyList) {
            CompilerBean.LOG.debug("resolving dependency on :" + dependency); //$NON-NLS-1$
            compilerBeanMap.get(dependency).merge(compilerBeanMap, contextPath, localhostUrlPrefix);
        }
        final File mergedFile = Paths.get(contextPath, this.mergeDirectory, this.name).toFile();
        CompilerBean.LOG.debug("destination: " + mergedFile); //$NON-NLS-1$
        mergedFile.delete();
        mergedFile.getParentFile().mkdirs();
        mergedFile.createNewFile();
        // use the search pattern to get the list of included and excluded files
        final List<Path> includedFiles = this.findFilesFromPattern(contextPath, localhostUrlPrefix);
        CompilerBean.LOG.debug("files to be compiled: " + includedFiles); //$NON-NLS-1$
        // if the debug mode is set, no compression is needed. combine all the files into one file
        switch (this.fileType) {
        case STYLE:
            FileCompiler.compileCSS(includedFiles, mergedFile, this.compilationLevel);
            break;
        case SCRIPT:
            FileCompiler.compileJS(includedFiles, mergedFile, this.compilationLevel);
            break;
        case TEMPLATE:
            FileCompiler.compileTemplates(includedFiles, mergedFile, this.compilationLevel);
            break;
        default:
            // impossible case. Do nothing here
            break;
        }
        return this.fileType;
    }

    /**
     * @param compilationLevel the compilation level to set
     * @see CompilerBean#compilationLevel
     */
    void setCompilationLevel(final CompilationLevel compilationLevel) {
        this.compilationLevel = compilationLevel;
    }

    /**
     * Creates a dependency list based on a comma separated list of files sent in a string to the function
     * @param dependency the dependency string to use to create the dependency list
     * @see CompilerBean#dependencies
     */
    void setDependencies(final String dependency) {
        this.dependencies = dependency;
    }

    /**
     * @param name the name to set
     * @see CompilerBean#name
     */
    void setName(final String name) {
        this.name = name;
    }
}
