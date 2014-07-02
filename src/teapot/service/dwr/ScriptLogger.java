package teapot.service.dwr;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.directwebremoting.WebContextFactory;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;

/**
 * Logs the javascript messages on the server
 */
@RemoteProxy
public class ScriptLogger {
    /**
     * the LOG object
     */
    private static final Logger LOG       = Logger.getLogger(ScriptLogger.class);
    /**
     * The log format for the script
     */
    private static final String logFormat = "%s\t\t[%s]";                        //$NON-NLS-1$

    /**
     * Logs an debug message from client on the server
     * @param message the message to be logged
     */
    @RemoteMethod
    public static void debug(final String message) {
        ScriptLogger.LOG.debug(String.format(ScriptLogger.logFormat, message, ScriptLogger.getContext()));
    }

    /**
     * Logs an error message from client on the server
     * @param message the message to be logged
     */
    @RemoteMethod
    public static void error(final String message) {
        ScriptLogger.LOG.error(String.format(ScriptLogger.logFormat, message, ScriptLogger.getContext()));
    }

    /**
     * Logs an info message from client on the server
     * @param message the message to be logged
     */
    @RemoteMethod
    public static void info(final String message) {
        ScriptLogger.LOG.info(String.format(ScriptLogger.logFormat, message, ScriptLogger.getContext()));
    }

    /**
     * Logs an warning message from client on the server
     * @param message the message to be logged
     */
    @RemoteMethod
    public static void warn(final String message) {
        ScriptLogger.LOG.warn(String.format(ScriptLogger.logFormat, message, ScriptLogger.getContext()));
    }

    /**
     * Reads and returns the client request session and user agent details
     * @return the client request session and user agent details
     */
    private static String getContext() {
        final HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        return String.format("%s | %s ", req.getHeader("cookie"), req.getHeader("user-agent")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
