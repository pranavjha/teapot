package teapot.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Utility class to access the spring context.<br>
 * <b>Usages</b>
 * <ul>
 * <li>To get a classpath resource</li>
 * </ul>
 */
@Component
@Scope("singleton")
public class Context
implements ApplicationContextAware {
    /**
     * Static application context. Will be initialized by spring
     */
    private static ApplicationContext applicationContext = null;

    /**
     * Returns the spring application context
     * @return the spring application context
     */
    public static ApplicationContext getApplicationContext() {
        return Context.applicationContext;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext)
    throws BeansException {
        // Assigning the ApplicationContext into a static variable
        Context.applicationContext = applicationContext;
    }
}