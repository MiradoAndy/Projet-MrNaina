package util;

import java.lang.reflect.Method;

import annotation.Auth;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

// Authentification : une route @Auth n'est accessible que si la session contient l'utilisateur
public class AuthUtil {

    public static final String CLE_PAR_DEFAUT = "utilisateur";

    // Nom de la donnée de session qui représente "l'utilisateur connecté" (context-param auth-session-key)
    public static String getCle(ServletContext context) {
        String cle = context.getInitParameter("auth-session-key");
        return (cle == null || cle.trim().isEmpty()) ? CLE_PAR_DEFAUT : cle.trim();
    }

    // Page de connexion vers laquelle on redirige (context-param login-url), ou null
    public static String getLoginUrl(ServletContext context) {
        String url = context.getInitParameter("login-url");
        return (url == null || url.trim().isEmpty()) ? null : url.trim();
    }

    // @Auth sur la méthode, ou sur sa classe
    public static boolean estProtege(Method methode) {
        return methode.isAnnotationPresent(Auth.class)
                || methode.getDeclaringClass().isAnnotationPresent(Auth.class);
    }

    public static boolean estConnecte(HttpServletRequest request) {
        // pas de session du tout : visiteur anonyme, on n'en crée pas une pour rien
        if (request.getSession(false) == null) {
            return false;
        }
        String cle = getCle(request.getServletContext());
        return SessionUtil.getSession(request).getAttribute(cle) != null;
    }

}
