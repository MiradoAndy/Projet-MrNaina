package util;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;

// Un contrôleur n'est créé qu'une seule fois, à la première requête qui l'utilise,
// puis la même instance sert toutes les requêtes (de tous les utilisateurs).
public class ControleurUtil {

    public static final String ATTRIBUT_CACHE = "controllers";
    private static final Object VERROU = new Object();

    // Instance du contrôleur : celle du cache si elle existe, sinon on la crée et on la range.
    // computeIfAbsent est atomique : même si 50 requêtes arrivent en même temps sur un contrôleur
    // pas encore créé, le constructeur n'est appelé qu'une fois.
    // Si le constructeur échoue, rien n'est rangé : la requête suivante réessaie.
    public static Object getInstance(ServletContext context, Class<?> classe) {
        return getCache(context).computeIfAbsent(classe, ControleurUtil::creer);
    }

    private static Object creer(Class<?> classe) {
        try {
            return classe.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(classe.getSimpleName()
                    + " doit avoir un constructeur sans argument pour être utilisé comme contrôleur", e);
        } catch (InvocationTargetException e) {
            // exception levée par le constructeur lui-même
            Throwable cause = e.getCause();
            throw new IllegalStateException("Le constructeur de " + classe.getSimpleName()
                    + " a échoué : " + cause, cause);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de créer le contrôleur " + classe.getSimpleName()
                    + " : " + e, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<Class<?>, Object> getCache(ServletContext context) {
        Object cache = context.getAttribute(ATTRIBUT_CACHE);
        if (cache == null) {
            synchronized (VERROU) {
                cache = context.getAttribute(ATTRIBUT_CACHE);
                if (cache == null) {
                    cache = new ConcurrentHashMap<Class<?>, Object>();
                    context.setAttribute(ATTRIBUT_CACHE, cache);
                }
            }
        }
        return (Map<Class<?>, Object>) cache;
    }

}
