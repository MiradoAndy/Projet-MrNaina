package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Sur une méthode (ou une classe entière) de controller : réservé aux utilisateurs connectés.
// "Connecté" = la session contient une valeur sous le nom auth-session-key du web.xml.
//
//   @Auth                              connecté, n'importe quel rôle
//   @Auth(roles = "dg")                connecté ET rôle dg
//   @Auth(roles = {"admin", "dg"})     connecté ET (rôle admin OU rôle dg)
//
// Le rôle de l'utilisateur est lu dans la session, sous le nom role-session-key du web.xml.
// Si la méthode et sa classe ont chacune un @Auth, celui de la méthode l'emporte.
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {

    // rôles autorisés (un seul suffit) ; vide = tous les utilisateurs connectés
    String[] roles() default {};

}
