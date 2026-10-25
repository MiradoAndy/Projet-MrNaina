package util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import model.Session;

// Une seule instance de Session par utilisateur : elles sont rangées dans le contexte de l'application,
// avec l'id de la session Tomcat comme clé
public class SessionUtil {

    public static final String ATTRIBUT_CACHE = "sessions";
    private static final Object VERROU = new Object();

    // Session de l'utilisateur de cette requête : celle du cache si elle existe, sinon on la crée et on la range
    public static Session getSession(HttpServletRequest request) {
        HttpSession httpSession = request.getSession(true);
        return getCache(request.getServletContext())
                .computeIfAbsent(httpSession.getId(), id -> new Session(httpSession));
    }

    // Appelée quand Tomcat détruit une session (expiration ou invalidate) : on la retire du cache
    public static void supprimer(ServletContext context, String idSession) {
        getCache(context).remove(idSession);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Session> getCache(ServletContext context) {
        Object cache = context.getAttribute(ATTRIBUT_CACHE);
        if (cache == null) {
            synchronized (VERROU) {
                cache = context.getAttribute(ATTRIBUT_CACHE);
                if (cache == null) {
                    cache = new ConcurrentHashMap<String, Session>();
                    context.setAttribute(ATTRIBUT_CACHE, cache);
                }
            }
        }
        return (Map<String, Session>) cache;
    }

}
