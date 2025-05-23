package sk.tuke.meta.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Column {
    String name() default "";   // optional, default is field name
    boolean nullable() default true;
    boolean unique() default false;
}
