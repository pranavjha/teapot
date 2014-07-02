package teapot.web.filter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;

import teapot.common.utils.Context;
import teapot.common.utils.Utils;

import com.google.javascript.jscomp.CompilationLevel;

/**
 * Filter Class for compiling the static files as specified in the configuration. To use it, define the Filter in web.xml as below
 * 
 * <pre>
 * 
 *  &lt;filter&gt;
 *         &lt;filter-name&gt;CompilerFilter&lt;/filter-name&gt;
 *         &lt;filter-class&gt;teapot.web.filter.CompilerFilter&lt;/filter-class&gt;
 *         &lt;init-param&gt;
 *             &lt;param-name&gt;compilerConfigLocation&lt;/param-name&gt;
 *             &lt;param-value&gt;<b>&lt;&lt;configuration file location&gt;&gt;</b>&lt;/param-value&gt;
 *         &lt;/init-param&gt;
 *     &lt;/filter&gt;
 *     &lt;filter-mapping&gt;
 *         &lt;filter-name&gt;CompilerFilter&lt;/filter-name&gt;
 *         &lt;url-pattern&gt;/*.js&lt;/url-pattern&gt;
 *     &lt;/filter-mapping&gt;
 *     &lt;filter-mapping&gt;
 *         &lt;filter-name&gt;CompilerFilter&lt;/filter-name&gt;
 *         &lt;url-pattern&gt;/*.css&lt;/url-pattern&gt;
 *     &lt;/filter-mapping&gt;
 * </pre>
 * 
 * The configuration xml file contains details of
 * <ul>
 * <li>files to be created</li>
 * <li>files to be included/excluded in aggregation</li>
 * <li>compression aggressiveness</li>
 * <li>debug options</li>
 * </ul>
 * For details on how to write the configuration xml, refer to the DTD documentation
 */
public class CompilerFilter
implements Filter {
    /**
     * the LOG object
     */
    private static final Logger       LOG = Logger.getLogger(CompilerFilter.class);
    /**
     * This string represents the local path of the context root. It is used to remove and revert all compilation changes on server shutdown
     */
    private String                    basePath;
    /**
     * Global level compilation options. Used for files which are not included for merging and served independently
     */
    private CompilationLevel          compilationLevel;
    /**
     * Internal storage of the list of compiled files
     */
    private TreeSet<String>           compiledFiles;
    /**
     * Keeps the map of File Path of the merge file and the attributes for creating the file at runtime
     */
    private Map<String, CompilerBean> compilerBeanMap;

    /**
     * Cleans up all the compiled files and resets the server to its original state
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        CompilerFilter.LOG.debug("destroy called.. clearing files in " + this.compiledFiles); //$NON-NLS-1$
        for (final String path : this.compiledFiles) {
            try {
                // the backup file has the same name as the main file with a .bak extension
                final Path backupFile = Paths.get(this.basePath, path + ".bak");//$NON-NLS-1$ 
                final Path originalFile = Paths.get(this.basePath, path);
                if (backupFile.toFile().exists()) {
                    // if there is a backup file. replace the compiled file with it
                    CompilerFilter.LOG.debug(String.format("replacing %s by its backup", path)); //$NON-NLS-1$
                    Files.copy(backupFile, originalFile, StandardCopyOption.REPLACE_EXISTING);
                    Files.delete(backupFile);
                } else {
                    // if there is no backup file. delete the compiler file
                    CompilerFilter.LOG.debug(String.format("deleting %s", originalFile)); //$NON-NLS-1$
                    Files.delete(originalFile);
                }
            } catch (final IOException e) {
                CompilerFilter.LOG.error("Error doing cleanup", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * This function scans the static file request and checks if a merged file is requested. If so, it creates the file. If a merged file is not
     * requested and the request is for a static file, the file is compiled with the default compilation specified in the compile:configuration root
     * element in the configurations file
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain)
    throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        // construct the relative resource path
        final String filePath = Utils.sanitizePath(request.getRequestURI().substring(request.getContextPath().length() + 1));
        // construct localhost url prefix
        // TODO: is there a better way?
        final String urlPrefix = "http://127.0.0.1:" + request.getServerPort() + request.getContextPath(); //$NON-NLS-1$
        // this variable stores the local compilation levels.
        CompilationLevel thisCompilationLevel = this.compilationLevel;
        // get the file path
        final Path path = Paths.get(this.basePath, filePath);
        if (this.compiledFiles.contains(filePath)) {
            // if the file is already compiled. send it from the server
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            if (this.compilerBeanMap.containsKey(filePath)) {
                // if the file is a compiled file and it is not compiled, compile it
                CompilerFilter.LOG.info(String.format("Merge requested. Compiling '%s'", filePath)); //$NON-NLS-1$
                // get the corresponding compiler bean
                final CompilerBean merger = this.compilerBeanMap.get(filePath);
                // set the compilation level
                thisCompilationLevel = merger.getCompilationLevel();
                // merge the files and respond to the request
                final FileType responseFileType = merger.merge(this.compilerBeanMap, this.basePath, urlPrefix);
                response.setContentType(responseFileType.getContentType());
                response.getOutputStream().write(Files.readAllBytes(path));
                response.getOutputStream().flush();
            } else if (path.toFile().exists()) {
                // if the file is not a compiled file and it is a static resource, compile it using the default compilation
                CompilerFilter.LOG.info(String.format("Atomic compilation requested. Compiling %s", filePath)); //$NON-NLS-1$
                final FileType responseFileType = FileCompiler.compileAtomic(path, this.compilationLevel);
                // write to the response
                response.setContentType(responseFileType.getContentType());
                response.getOutputStream().write(Files.readAllBytes(path));
                response.getOutputStream().flush();
            } else {
                // if the file is not a compiled file and it is not a static resource, let the corresponding servlet take care of it (in case a dwr
                // file is accessed)
                CompilerFilter.LOG.warn(String.format("The file '%s' is requested and it is not static. This can impact performance", filePath)); //$NON-NLS-1$
                filterChain.doFilter(servletRequest, servletResponse);
            }
            // if the compilation level is set, remove the path from the map to prevent re-compilation
            if (thisCompilationLevel != null) {
                CompilerFilter.LOG.debug(String.format("Compilation of '%s' complete. This file will not compile again", filePath)); //$NON-NLS-1$
                this.compiledFiles.add(filePath);
                this.compilerBeanMap.remove(filePath);
                // make the response cacheable for 30 days
                response.addHeader("Cache-Control", "max-age=2592000"); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                // if there is no compilation level set, the file should be retrieved every time from server
                CompilerFilter.LOG.warn("The compiler configuration is not optimized for performance."); //$NON-NLS-1$
                response.addHeader("Cache-Control", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /**
     * Loads the configuration file from the location in init parameter. The actual parsing is done in the doFilter method.
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(final FilterConfig filterConfig)
    throws ServletException {
        try {
            this.compiledFiles = new TreeSet<>();
            // construct the base path
            this.basePath = filterConfig.getServletContext().getRealPath(""); //$NON-NLS-1$ 
            // load the configurations
            final String compilerConfig = filterConfig.getInitParameter("compilerConfigLocation"); //$NON-NLS-1$
            CompilerFilter.LOG.info(String.format("loading configurations from location '%s'", compilerConfig)); //$NON-NLS-1$
            final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            final ConfigHandler configHandler = new ConfigHandler();
            saxParser.parse(Context.getApplicationContext().getResource(compilerConfig).getFile(), configHandler);
            this.compilerBeanMap = configHandler.toCompilerBeanMap();
            this.compilationLevel = configHandler.getRootCompilationLevel();
        } catch (final Throwable e) {
            // application startup should fail if the merges fail
            CompilerFilter.LOG.fatal("Error loading configurations", e); //$NON-NLS-1$
            throw new RuntimeException(e);
        }
    }
}
