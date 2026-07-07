package listener;

import java.util.HashMap;

import annotation.Annotation;
import annotation.UrlAnnotation;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import model.MethodeInfo;
import model.UrlInfo;
import util.Utilitaire;

public class AppContextListener implements ServletContextListener{

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

    }
    
}
