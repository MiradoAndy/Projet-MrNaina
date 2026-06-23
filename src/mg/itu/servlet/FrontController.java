package mg.itu.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import mg.itu.annotation.MaFonction;
import mg.itu.annotation.MonController;
import mg.itu.services.AnnotationService;

public class FrontController extends HttpServlet {

    private String controllerPackage;

    @Override
    public void init() throws ServletException {
        controllerPackage = getInitParameter("controllerPackage");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        // chemin relatif après le context path (ex: "/" ou "/test1")
        String path = requestURI.substring(contextPath.length());
        if (path.isEmpty()) path = "/";

        try {
            List<Class<?>> controllers = AnnotationService.scanPackage(controllerPackage);

            if (path.equals("/")) {
                afficherListeControllers(out, controllers, contextPath);
            } else {
                afficherDetailController(out, controllers, path, contextPath);
            }
        } catch (Exception e) {
            out.println("<html><body>");
            out.println("<p style='color:red'>Erreur : " + e.getMessage() + "</p>");
            out.println("</body></html>");
            e.printStackTrace();
        }
    }

    private void afficherListeControllers(PrintWriter out, List<Class<?>> controllers, String contextPath) {
        out.println("<html><body>");
        out.println("<h2>Controllers disponibles</h2>");
        out.println("<ul>");
        for (Class<?> clazz : controllers) {
            MonController annotation = clazz.getAnnotation(MonController.class);
            String url = annotation.url();
            out.println("<li><a href='" + contextPath + url + "'>" + url + "</a> → " + clazz.getSimpleName() + "</li>");
        }
        out.println("</ul>");
        out.println("</body></html>");
    }

    private void afficherDetailController(PrintWriter out, List<Class<?>> controllers, String path, String contextPath) {
        Class<?> found = null;
        for (Class<?> clazz : controllers) {
            MonController annotation = clazz.getAnnotation(MonController.class);
            if (annotation.url().equals(path)) {
                found = clazz;
                break;
            }
        }

        out.println("<html><body>");
        if (found == null) {
            out.println("<p style='color:red'>Aucun controller trouvé pour l'URL : " + path + "</p>");
        } else {
            out.println("<h2>Controller : " + found.getSimpleName() + "</h2>");
            out.println("<p>URL : " + path + "</p>");
            out.println("<h3>Fonctions annotées @MaFonction :</h3>");
            out.println("<ul>");
            boolean aucune = true;
            for (Method method : found.getDeclaredMethods()) {
                if (method.isAnnotationPresent(MaFonction.class)) {
                    out.println("<li>" + method.getName() + "()</li>");
                    aucune = false;
                }
            }
            if (aucune) {
                out.println("<li>Aucune méthode annotée @MaFonction.</li>");
            }
            out.println("</ul>");
            out.println("<a href='" + contextPath + "/'>← Retour à la liste</a>");
        }
        out.println("</body></html>");
    }
}
