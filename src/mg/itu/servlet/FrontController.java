package mg.itu.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

        out.println("<html><body>");
        out.println("<h2>Classes annotées @MonController dans le package : " + controllerPackage + "</h2>");
        out.println("<ul>");

        try {
            List<Class<?>> controllers = AnnotationService.scanPackage(controllerPackage);
            if (controllers.isEmpty()) {
                out.println("<li>Aucune classe trouvée.</li>");
            } else {
                for (Class<?> clazz : controllers) {
                    out.println("<li>" + clazz.getName() + "</li>");
                }
            }
        } catch (Exception e) {
            out.println("<li style='color:red'>Erreur : " + e.getMessage() + "</li>");
            e.printStackTrace();
        }

        out.println("</ul>");
        out.println("</body></html>");
    }
}
