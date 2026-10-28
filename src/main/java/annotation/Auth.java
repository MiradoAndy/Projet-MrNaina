package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Sur une méthode (ou une classe entière) de controller : réservé aux utilisateurs connectés.
// "Connecté" = la session contient une valeur sous le nom auth-session-key du web.xml.
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {

}
