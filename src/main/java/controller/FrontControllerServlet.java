package controller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import annotation.RestAPI;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.HTTPmethode;
import model.MethodeInfo;
import model.UrlInfo;
import util.JsonUtil;

public class FrontControllerServlet extends HttpServlet {
    private HashMap<UrlInfo, MethodeInfo> mapping = new HashMap<>();
    private String prefixe = "";
    private String suffixe = "";

    @Override
    public void init() throws ServletException {
        this.mapping = (HashMap<UrlInfo, MethodeInfo>) getServletContext().getAttribute("mapping");
        this.prefixe = (String) getServletContext().getInitParameter("view-prefix");
        this.suffixe = (String) getServletContext().getInitParameter("view-suffix");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        String route = requestURI.substring(contextPath.length());
        if (route.equals("")) {
            route = "/";
        }

        HTTPmethode httpMethode = HTTPmethode.valueOf(request.getMethod());

        UrlInfo recherche = new UrlInfo();
        recherche.setUrl(route);
        recherche.setAction(httpMethode);

        MethodeInfo method = mapping.get(recherche);

        if (method == null) {
            for (HashMap.Entry<UrlInfo, MethodeInfo> entry : mapping.entrySet()) {
                UrlInfo url = entry.getKey();
                MethodeInfo m = entry.getValue();

                response.getWriter().println(
                        "<p>URL : " + url.getUrl() + " ,  Méthode : " + m.getMethode() + "  , Action :"
                                + url.getAction());
            }
            return;
        }

        // @RestAPI : on renvoie uniquement du JSON, rien ne doit être écrit avant
        if (method.getMethode().isAnnotationPresent(RestAPI.class)) {
            processRestAPI(method, response);
            return;
        }

        response.getWriter().println("<h2>URL trouvée</h2>");
        response.getWriter().println("<p>Route : " + route + "</p>");
        response.getWriter().println("<p>Méthode appelée : " + method.getMethode() + "</p>");

        try {
            Class<?> clazz = method.getMethode().getDeclaringClass();
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Object resultat = method.getMethode().invoke(instance);
            if (resultat != null) {
                response.getWriter().println("<p>Résultat : " + resultat + "</p>");
            }
            
            if(resultat instanceof model.ModelAndView) {
                model.ModelAndView mv = (model.ModelAndView) resultat;
                String urlSuivant = mv.getUrlSuivant();
                urlSuivant = prefixe + urlSuivant + suffixe;
                request.setAttribute("prefixe", prefixe);
                for(Map.Entry<String, String> entry : mv.getList().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }
                request.getRequestDispatcher(urlSuivant).forward(request, response);
            }

        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'invocation de la méthode", e);
        }
    }

    protected void processRestAPI(MethodeInfo method, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String json;
        try {
            Class<?> clazz = method.getMethode().getDeclaringClass();
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Object resultat = method.getMethode().invoke(instance);
            json = JsonUtil.toJson(resultat);
        } catch (Exception e) {
            // l'exception levée par la méthode du controller est enveloppée dans InvocationTargetException
            Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
            Map<String, Object> erreur = new LinkedHashMap<>();
            erreur.put("code", 500);
            erreur.put("erreur", cause.getClass().getSimpleName());
            erreur.put("message", cause.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            json = JsonUtil.toJson(erreur);
        }
        response.getWriter().print(json);
    }

}