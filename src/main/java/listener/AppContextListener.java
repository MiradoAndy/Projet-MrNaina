package listener;

import java.util.HashMap;

import annotation.Annotation;
import annotation.UrlAnnotation;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import model.MethodeInfo;
import model.UrlInfo;
import util.SessionUtil;
import util.Utilitaire;

public class AppContextListener implements ServletContextListener, HttpSessionListener {

    @Override
    public void contextInitialized(ServletContextEvent sc){
        ServletContext context = sc.getServletContext();
        String packageName = context.getInitParameter("controller-package");
        HashMap<UrlInfo, MethodeInfo> mapping = new HashMap<>();
        try {
            Utilitaire.getClassesWithAnnotation(packageName, Annotation.class, UrlAnnotation.class, mapping);
        } catch (Exception e) {
            throw new RuntimeException("erreur",e);
        }
        context.setAttribute("mapping", mapping);
        // cache des Session (une instance par utilisateur)
        SessionUtil.getCache(context);

    }

    // session expirée ou fermée par invalidate() : on la retire du cache pour qu'il ne grossisse pas indéfiniment
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        SessionUtil.supprimer(event.getSession().getServletContext(), event.getSession().getId());
    }
    
}
