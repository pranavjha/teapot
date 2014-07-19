package teapot.web.filter;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.javascript.jscomp.CompilationLevel;

/**
 * The ConfigHandler class is the SAX parser class for Compiler config.
 * @see DefaultHandler
 */
public class ConfigHandler
extends DefaultHandler {
    /**
     * Stores constant string values for all XML attributes in configuration file
     * @see CompilerFilter
     */
    static interface XMLAttributes {
        /**
         * The base directory attribute
         */
        String ATTR_BASE_DIR          = "basedir";              //$NON-NLS-1$
        /**
         * the compression attribute
         */
        String ATTR_COMPILATION_LEVEL = "compilation-level";    //$NON-NLS-1$
        /**
         * the dependency attribute
         */
        String ATTR_DEPENDENCY        = "dependency";           //$NON-NLS-1$
        /**
         * The merge directory Attribute
         */
        String ATTR_MERGE_DIR         = "mergedir";             //$NON-NLS-1$
        /**
         * the protocol attribute
         */
        String ATTR_PROTOCOL          = "protocol";             //$NON-NLS-1$
        /**
         * The regular expression to split the dependency string
         */
        String DEPENDENCY_REGEX       = "\\s*\\,\\s*";          //$NON-NLS-1$
        /**
         * compiler tag
         */
        String TG_COMPILE_CONFIG      = "compile:configuration"; //$NON-NLS-1$
        /**
         * exclude tag
         */
        String TG_EXCLUDE             = "exclude";              //$NON-NLS-1$
        /**
         * include tag
         */
        String TG_INCLUDE             = "include";              //$NON-NLS-1$
        /**
         * name tag
         */
        String TG_NAME                = "name";                 //$NON-NLS-1$
        /**
         * scripts tag
         */
        String TG_SCRIPTS             = "scripts";              //$NON-NLS-1$
        /**
         * styles tag
         */
        String TG_STYLES              = "styles";               //$NON-NLS-1$
        /**
         * templates tag
         */
        String TG_TEMPLATES           = "templates";            //$NON-NLS-1$
        /**
         * to file tag
         */
        String TG_TO_FILE             = "to-file";              //$NON-NLS-1$
    }

    /**
     * a CompilerBean instance used to create beans to be added to the {@link ConfigHandler#beanMap}
     */
    private CompilerBean                    bean;
    /**
     * a map of merged file Path as key and CompilerBean as value
     */
    private final Map<String, CompilerBean> beanMap;
    /**
     * fileFinder is used to create finders for include and exclude tags
     */
    private FileFinder                      fileFinder;
    /**
     * Value of the currently traversed node
     */
    private String                          nodeValue;
    /**
     * Global level compilation options. Used for files which are not included for merging and served independentlyon
     */
    private CompilationLevel                rootCompilationLevel;
    /**
     * templateBean acts as a template for {@link ConfigHandler#bean}
     */
    private CompilerBean                    templateBean;

    /**
     * Instantiates ConfigHandler class
     */
    ConfigHandler() {
        this.beanMap = new HashMap<>();
    }

    @Override
    public void characters(final char ch[], final int start, final int length)
    throws SAXException {
        this.nodeValue = new String(ch, start, length);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName)
    throws SAXException {
        switch (qName) {
        case XMLAttributes.TG_TO_FILE:
            // new file ends. add the created bean into the map
            this.beanMap.put(this.bean.getMergedFile(), this.bean);
            break;
        case XMLAttributes.TG_NAME:
            // name tag ends. copy the name value from string
            this.bean.setName(this.nodeValue);
            break;
        case XMLAttributes.TG_INCLUDE:
        case XMLAttributes.TG_EXCLUDE:
            // include/exclude tag ends. add the file finder element to the search pattern
            this.fileFinder.setPattern(this.nodeValue);
            this.bean.addSearchPattern(this.fileFinder);
            break;
        default:
            // for all other cases, do nothing
            break;
        }
    }

    /**
     * @return the root compilation level
     * @see ConfigHandler#rootCompilationLevel
     */
    public CompilationLevel getRootCompilationLevel() {
        return this.rootCompilationLevel;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
    throws SAXException {
        CompilationLevel compilationLevel = null;
        switch (qName) {
        case XMLAttributes.TG_COMPILE_CONFIG:
            // root tag. has compilation level property
            if (null != attributes.getValue(XMLAttributes.ATTR_COMPILATION_LEVEL)) {
                this.rootCompilationLevel = CompilationLevel.valueOf(attributes.getValue(XMLAttributes.ATTR_COMPILATION_LEVEL));
            }
            break;
        case XMLAttributes.TG_SCRIPTS:
        case XMLAttributes.TG_TEMPLATES:
        case XMLAttributes.TG_STYLES:
            // a new file category starts here. Create a template
            // compilation level can be null, so a null check is required
            if (null != attributes.getValue(XMLAttributes.ATTR_COMPILATION_LEVEL)) {
                compilationLevel = CompilationLevel.valueOf(attributes.getValue(XMLAttributes.ATTR_COMPILATION_LEVEL));
            } else {
                compilationLevel = this.getRootCompilationLevel();
            }
            this.templateBean = new CompilerBean(compilationLevel, attributes.getValue(XMLAttributes.ATTR_BASE_DIR),
                attributes.getValue(XMLAttributes.ATTR_MERGE_DIR), FileType.fromString(qName));
            break;
        case XMLAttributes.TG_TO_FILE:
            // new file starts. create a new bean form template
            this.bean = this.templateBean.createFromTemplate();
            // handle the overriding attributes if they exist
            if (null != attributes.getValue(XMLAttributes.ATTR_DEPENDENCY)) {
                this.bean.setDependencies(attributes.getValue(XMLAttributes.ATTR_DEPENDENCY));
            }
            if (null != attributes.getValue(XMLAttributes.ATTR_MERGE_DIR)) {
                this.bean.setMergeDirectory(attributes.getValue(XMLAttributes.ATTR_MERGE_DIR));
            }
            if (null != attributes.getValue(XMLAttributes.ATTR_BASE_DIR)) {
                this.bean.setBaseDirectory(attributes.getValue(XMLAttributes.ATTR_BASE_DIR));
            }
            if (null != attributes.getValue(XMLAttributes.ATTR_COMPILATION_LEVEL)) {
                this.bean.setCompilationLevel(CompilationLevel.valueOf(attributes.getValue(XMLAttributes.ATTR_COMPILATION_LEVEL)));
            }
            break;
        case XMLAttributes.TG_INCLUDE:
            // include tag starts. add the file finder element to the search pattern
            this.fileFinder = new FileFinder(attributes.getValue(XMLAttributes.ATTR_PROTOCOL), true);
            break;
        case XMLAttributes.TG_EXCLUDE:
            // exclude tag starts. add the file finder element to the search pattern
            this.fileFinder = new FileFinder(attributes.getValue(XMLAttributes.ATTR_PROTOCOL), false);
            break;
        default:
            // for all other cases, do nothing
            break;
        }
    }

    /**
     * Returns a map of merged file Path as key and CompilerBean as value
     * @return a map of merged file Path as key and CompilerBean as value
     */
    public Map<String, CompilerBean> toCompilerBeanMap() {
        return this.beanMap;
    }
}
