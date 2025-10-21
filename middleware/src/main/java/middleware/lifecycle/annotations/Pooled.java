package middleware.lifecycle.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * indicar que as instâncias devem ser gerenciadas por um ObjectPool
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE) 
public @interface Pooled {
}
