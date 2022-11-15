package annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Options {
    boolean useGeneratedKeys() default false;
    String keyProperty() default "";
    boolean flushCache() default false;
}
