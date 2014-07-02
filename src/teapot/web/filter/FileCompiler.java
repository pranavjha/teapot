package teapot.web.filter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.google.common.css.compiler.commandline.ClosureCommandLineCompiler;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.jssrc.SoyJsSrcOptions;
import com.google.template.soy.msgs.SoyMsgBundle;

/**
 * This is a utility class for compiling javascript, css and template files.
 */
class FileCompiler {
    /**
     * the LOG object
     */
    private static final Logger  LOG                      = Logger.getLogger(FileCompiler.class);
    /**
     * Resource locator pattern for style files
     */
    private static final Pattern RESOURCE_LOCATOR_PATTERN = Pattern.compile("url\\(\\s*[\"']?(.*?)[\"']?\\s*\\)", Pattern.CASE_INSENSITIVE //$NON-NLS-1$
                                                              | Pattern.DOTALL | Pattern.MULTILINE);

    /**
     * Moves all resources used by the included file into the merged file directory so that the relative paths do not break
     * @param sourceFile the file which has to be checked for resources
     * @param destinationPath the file relative to which the resources have to be moved
     * @param compilationLevel the file compilation level
     * @throws IOException if file parsing fails
     */
    private static void moveResources(final Path sourceFile, final Path destinationPath, final CompilationLevel compilationLevel)
    throws IOException {
        // read all lines and merge them
        final List<String> lines = Files.readAllLines(sourceFile, Charset.defaultCharset());
        final StringBuilder allLines = new StringBuilder();
        for (final String line : lines) {
            allLines.append(line);
        }
        final Matcher matcher = FileCompiler.RESOURCE_LOCATOR_PATTERN.matcher(allLines);
        while (matcher.find()) {
            final String fileName = matcher.group(1);
            final Path destination = destinationPath.getParent().resolve(fileName);
            // if the destination directory does not exist, make it
            destination.getParent().toFile().mkdirs();
            // if the destination file exists, and compilation level is not null (debugging is off) then skip the file copy
            if (destination.toFile().exists() && null != compilationLevel) {
                continue;
            }
            // otherwise, copy the file
            FileCompiler.LOG.debug("moving static resource: " + fileName); //$NON-NLS-1$
            Files
                .copy(sourceFile.getParent().resolve(fileName), destination, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Statically merges all input files into the merge file without any processing
     * @param includedFiles the list of Paths to be included in the compilation and aggregation
     * @param mergedFile the final file output
     * @throws IOException if the merge fails
     */
    private static void staticMerge(final List<Path> includedFiles, final File mergedFile)
    throws IOException {
        final List<String> lines = new ArrayList<>();
        for (final Path file : includedFiles) {
            FileCompiler.LOG.debug(String.format("merging '%s'", file)); //$NON-NLS-1$
            lines.addAll(Files.readAllLines(file, Charset.defaultCharset()));
        }
        Files.write(mergedFile.toPath(), lines, Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE);
    }

    /**
     * Does an atomic compilation of the file and replaces it with the compiled version
     * @param path the file path
     * @param compilationLevel the default compilation level
     * @return the FileType corresponding to the compiled file
     * @throws IOException if the file reading/ writing fails
     */
    static FileType compileAtomic(final Path path, final CompilationLevel compilationLevel)
    throws IOException {
        final FileType fileType = FileType.fromPath(path);
        if (null == fileType) {
            FileCompiler.LOG.error("Invalid file requested."); //$NON-NLS-1$
            throw new FileNotFoundException("Invalid File Name"); //$NON-NLS-1$
        }
        FileCompiler.LOG.info(String.format("compiling %s in '%s'...", fileType, path)); //$NON-NLS-1$
        // create or override the backup file if the source file is new or modified recently
        final File backupFile = new File(path.toAbsolutePath().toString() + ".bak");//$NON-NLS-1$ 
        if (Files.getLastModifiedTime(path).toMillis() != 0) {
            FileCompiler.LOG.debug("File is new or modified recently. Creating backup for the file."); //$NON-NLS-1$
            Files.copy(path, backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        switch (fileType) {
        case SCRIPT:
            FileCompiler.compileJS(Arrays.asList(new Path[] { backupFile.toPath() }), path.toFile(), compilationLevel);
            break;
        case STYLE:
            FileCompiler.compileCSS(Arrays.asList(new Path[] { backupFile.toPath() }), path.toFile(), compilationLevel);
            break;
        case TEMPLATE:
            FileCompiler.compileTemplates(Arrays.asList(new Path[] { backupFile.toPath() }), path.toFile(), compilationLevel);
            break;
        default:
            // this is an impossibility
            break;
        }
        // set the file modified time to 0 post compilation so that any runtime updates can be tracked
        Files.setLastModifiedTime(path, FileTime.fromMillis(0));
        return fileType;
    }

    /**
     * Aggregates and compresses the input file list and creates a merged css file with the contents compressed
     * @param includedFiles the list of Paths to be included in the compilation and aggregation
     * @param mergedFile the final file output
     * @param compilationLevel compilation level to be used
     * @throws IOException when reading any of the included files or writing output to the merged file fails
     */
    static void compileCSS(final List<Path> includedFiles, final File mergedFile, final CompilationLevel compilationLevel)
    throws IOException {
        // command line usage
        // java -jar closure-stylesheets.jar --output-file output.gss input1.css input2.css input3.css
        final List<String> args = new ArrayList<>();
        // if compilation level is null, then use pretty print
        if (null == compilationLevel) {
            args.add("--pretty-print"); //$NON-NLS-1$
        }
        args.add("--output-file"); //$NON-NLS-1$
        args.add(mergedFile.getAbsolutePath());
        // copy the resources
        for (final Path includedFile : includedFiles) {
            FileCompiler.moveResources(includedFile, mergedFile.toPath(), compilationLevel);
            args.add(includedFile.toFile().getAbsolutePath());
        }
        ClosureCommandLineCompiler.main(args.toArray(new String[0]));
    }

    /**
     * Aggregates and compresses the input file list and creates a merged js file with the contents compressed
     * @param includedFiles the list of Paths to be included in the compilation and aggregation
     * @param mergedFile the final file output
     * @param compilationLevel compilation level to be used
     * @throws IOException when reading any of the included files or writing output to the merged file fails
     */
    static void compileJS(final List<Path> includedFiles, final File mergedFile, final CompilationLevel compilationLevel)
    throws IOException {
        // if compilation level is not set. do a static merge
        if (null == compilationLevel) {
            FileCompiler.staticMerge(includedFiles, mergedFile);
            return;
        }
        final com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();
        final CompilerOptions options = new CompilerOptions();
        compilationLevel.setOptionsForCompilationLevel(options);
        final List<SourceFile> input = new ArrayList<>();
        for (final Path oneFile : includedFiles) {
            input.add(SourceFile.fromFile(oneFile.toFile()));
        }
        final Result result = compiler.compile(CommandLineRunner.getDefaultExterns(), input, options);
        FileCompiler.LOG.debug("Compilation debug messages: \n" + result.debugLog); //$NON-NLS-1$
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedFile))) {
            writer.write(compiler.toSource());
            writer.flush();
            writer.close();
        }
    }

    /**
     * Aggregates, compiles and compresses the input file list and creates a merged js file with the contents compressed
     * @param includedFiles the list of Paths to be included in the compilation and aggregation
     * @param mergedFile the final file output
     * @param compilationLevel compilation level to be used
     * @throws IOException when reading any of the included files or writing output to the merged file fails
     */
    static void compileTemplates(final List<Path> includedFiles, final File mergedFile, final CompilationLevel compilationLevel)
    throws IOException {
        final List<Path> jsFiles = new ArrayList<>();
        final SoyFileSet.Builder builder = new SoyFileSet.Builder();
        for (final Path file : includedFiles) {
            builder.add(file.toFile());
        }
        final SoyJsSrcOptions options = new SoyJsSrcOptions();
        options.setCodeStyle(SoyJsSrcOptions.CodeStyle.CONCAT);
        final List<String> jsSrc = builder.build().compileToJsSrc(options, SoyMsgBundle.EMPTY);
        FileCompiler.LOG.debug("SOY Compilation successful."); //$NON-NLS-1$ 
        // print the compiled files into the merged file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedFile))) {
            for (final String src : jsSrc) {
                writer.write(src);
            }
            writer.flush();
            writer.close();
        }
        // if javaScript files are included, do not compile them
        for (final Path jsFile : jsFiles) {
            Files.write(mergedFile.toPath(), Files.readAllLines(jsFile, Charset.defaultCharset()), Charset.defaultCharset(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        }
        // if compilation level is not set. do not compile using closure
        if (null != compilationLevel) {
            final List<Path> jsFile = new ArrayList<>(1);
            jsFile.add(mergedFile.toPath());
            FileCompiler.compileJS(jsFile, mergedFile, compilationLevel);
        }
    }
}
