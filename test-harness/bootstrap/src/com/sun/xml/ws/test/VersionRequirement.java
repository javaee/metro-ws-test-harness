package com.sun.xml.ws.test;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

@Retention(value= RetentionPolicy.RUNTIME)
@Target({TYPE})
/**
 * This captures the Version requirements for the Java based test (junit test).
 * This is added in bootstrap such that the harness based tests can easily depend on it without it just like any other
 * junit classes. 
 *
 * @author Rama Pulavarthi
 */

public @interface VersionRequirement {
    String since() default "";
    String until() default "";
    String exlcudeFrom() default "";
}
