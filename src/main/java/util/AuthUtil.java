package util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import annotation.Auth;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

// Authentification et rôles : une route @Auth n'est accessible que si la session contient l'utilisateur
// et, si @Auth(roles = ...) est précisé, que son rôle fait partie des rôles autorisés
public class AuthUtil {

    public static final String CLE_PAR_DEFAUT = "utilisateur";
    public static final String CLE_ROLE_PAR_DEFAUT = "role";

    // Nom de la donnée de session qui représente "l'utilisateur connecté" (context-param auth-session-key)
    public static String getCle(ServletContext context) {
        return lireParametre(context, "auth-session-key", CLE_PAR_DEFAUT);
    }

    // Nom de la donnée de session qui contient le ou les rôles (context-param role-session-key)
    public static String getCleRole(ServletContext context) {
        return lireParametre(context, "role-session-key", CLE_ROLE_PAR_DEFAUT);
    }

    // Page de connexion vers laquelle on redirige (context-param login-url), ou null
    public static String getLoginUrl(ServletContext context) {
        String url = context.getInitParameter("login-url");
        return (url == null || url.trim().isEmpty()) ? null : url.trim();
    }

    private static String lireParametre(ServletContext context, String nom, String parDefaut) {
        String valeur = context.getInitParameter(nom);
        return (valeur == null || valeur.trim().isEmpty()) ? parDefaut : valeur.trim();
    }

    // @Auth de la méthode, sinon celui de sa classe, sinon null (route publique)
    public static Auth getAuth(Method methode) {
        Auth auth = methode.getAnnotation(Auth.class);
        return (auth != null) ? auth : methode.getDeclaringClass().getAnnotation(Auth.class);
    }

    public static boolean estProtege(Method methode) {
        return getAuth(methode) != null;
    }

    public static boolean estConnecte(HttpServletRequest request) {
        // pas de session du tout : visiteur anonyme, on n'en crée pas une pour rien
        if (request.getSession(false) == null) {
            return false;
        }
        String cle = getCle(request.getServletContext());
        return SessionUtil.getSession(request).getAttribute(cle) != null;
    }

    // Rôles exigés par la route ; vide si @Auth sans roles
    public static Set<String> getRolesRequis(Method methode) {
        Auth auth = getAuth(methode);
        Set<String> roles = new LinkedHashSet<>();
        if (auth != null) {
            roles.addAll(Arrays.asList(auth.roles()));
        }
        return roles;
    }

    // Rôles de l'utilisateur connecté : la donnée de session peut être un String, un tableau ou une collection
    public static Set<String> getRolesUtilisateur(HttpServletRequest request) {
        Set<String> roles = new LinkedHashSet<>();
        if (request.getSession(false) == null) {
            return roles;
        }
        Object valeur = SessionUtil.getSession(request).getAttribute(getCleRole(request.getServletContext()));
        if (valeur == null) {
            return roles;
        }
        if (valeur instanceof Collection) {
            for (Object role : (Collection<?>) valeur) {
                ajouter(roles, role);
            }
        } else if (valeur instanceof Object[]) {
            for (Object role : (Object[]) valeur) {
                ajouter(roles, role);
            }
        } else {
            ajouter(roles, valeur);
        }
        return roles;
    }

    private static void ajouter(Set<String> roles, Object role) {
        if (role != null) {
            roles.add(role.toString());
        }
    }

    // Aucun rôle exigé : OK. Sinon l'utilisateur doit avoir au moins un des rôles autorisés (comparaison exacte).
    public static boolean aUnRoleAutorise(Method methode, HttpServletRequest request) {
        Set<String> requis = getRolesRequis(methode);
        if (requis.isEmpty()) {
            return true;
        }
        for (String role : getRolesUtilisateur(request)) {
            if (requis.contains(role)) {
                return true;
            }
        }
        return false;
    }

}
