package sk.tuke.meta.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method whose execution should be treated as an atomic
 * persistence operation, managed within a single database transaction.
 * The transaction will be committed upon successful completion
 * or rolled back if an exception occurs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AtomicPersistenceOperation {
}
