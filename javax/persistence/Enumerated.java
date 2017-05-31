package javax.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @since 2017-05-31
 * @version 1.0
 * @author marcos morise
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Enumerated {
    public String columnDefinition() default "";
    public boolean nullable() default false;
}
