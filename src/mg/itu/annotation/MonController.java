package mg.itu.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)  // accessible à l'exécution
@Target(ElementType.TYPE)            // utilisable sur une classe
public @interface MonController {
    
}
