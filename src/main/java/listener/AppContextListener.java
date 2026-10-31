package listener;

import java.util.HashMap;

import annotation.Annotation;
import annotation.UrlAnnotation;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import model.MethodeInfo;
import model.UrlInfo;
import util.ControleurUtil;
import util.SessionUtil;
import util.Utilitaire;

// @WebListener : Tomcat l'enregistre tout seul, il n'y a plus de <listener> à écrire dans le web.xml
// (le déclarer quand même dans le web.xml reste possible : il n'est enregistré qu'une fois)
@WebListener
public class AppContextListener implements ServletContextListener, HttpSessionListener {

    @Override
    public void contextInitialized(ServletContextEvent sc){
        ServletContext context = sc.getServletContext();
        String packageNames = context.getInitParameter("controller-package");
        if (packageNames == null || packageNames.trim().isEmpty()) {
            throw new IllegalStateException(
                    "context-param 'controller-package' manquant dans le web.xml (package des controllers)");
        }
        HashMap<UrlInfo, MethodeInfo> mapping = new HashMap<>();
        try {
            // plusieurs packages possibles, séparés par ; ou , : app.controllers;app.api
            for (String packageName : packageNames.split("[;,]")) {
                if (!packageName.trim().isEmpty()) {
                    Utilitaire.getClassesWithAnnotation(packageName.trim(), Annotation.class, UrlAnnotation.class, mapping);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("erreur",e);
        }
        context.setAttribute("mapping", mapping);
        // cache des Session (une instance par utilisateur)
        SessionUtil.getCache(context);
        // cache des contrôleurs (une instance par classe, créée à la première utilisation)
        ControleurUtil.getCache(context);

    }

    // session expirée ou fermée par invalidate() : on la retire du cache pour qu'il ne grossisse pas indéfiniment
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        SessionUtil.supprimer(event.getSession().getServletContext(), event.getSession().getId());
    }

}
