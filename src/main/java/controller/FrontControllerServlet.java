package controller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import annotation.RestAPI;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.HTTPmethode;
import model.MethodeInfo;
import model.UrlInfo;
import util.AuthUtil;
import util.JsonUtil;
import util.ParamException;
import util.ParamUtil;

// Autorise l'envoi de fichiers (multipart/form-data) : 10 Mo par fichier, 50 Mo par envoi,
// gardés en mémoire jusqu'à 1 Mo puis écrits temporairement sur le disque par Tomcat
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 10L * 1024 * 1024,
        maxRequestSize = 50L * 1024 * 1024)
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

        // avant tout getParameter, sinon les accents envoyés en POST arrivent abîmés
        request.setCharacterEncoding("UTF-8");
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

        // @Auth : avant de lire les paramètres (sinon un upload serait lu pour un visiteur refusé)
        if (AuthUtil.estProtege(method.getMethode()) && !AuthUtil.estConnecte(request)) {
            refuserNonConnecte(method, route, request, response);
            return;
        }

        // @RestAPI : on renvoie uniquement du JSON, rien ne doit être écrit avant
        if (method.getMethode().isAnnotationPresent(RestAPI.class)) {
            processRestAPI(method, request, response);
            return;
        }

        // valeurs du formulaire converties avant d'écrire quoi que ce soit dans la réponse
        Object[] arguments;
        try {
            arguments = ParamUtil.getArguments(method.getMethode(), request);
        } catch (ParamException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        response.getWriter().println("<h2>URL trouvée</h2>");
        response.getWriter().println("<p>Route : " + route + "</p>");
        response.getWriter().println("<p>Méthode appelée : " + method.getMethode() + "</p>");

        try {
            Class<?> clazz = method.getMethode().getDeclaringClass();
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Object resultat = method.getMethode().invoke(instance, arguments);
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

    // Route @Auth sans utilisateur connecté : 401 en JSON pour une route @RestAPI,
    // sinon redirection vers login-url (web.xml), ou 401 si aucune page de connexion n'est configurée
    protected void refuserNonConnecte(MethodeInfo method, String route, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        // aide au diagnostic : une faute de frappe dans le nom de la donnée de session se voit ici
        getServletContext().log("Accès refusé à " + route + " : la session ne contient pas '"
                + AuthUtil.getCle(getServletContext()) + "' (auth-session-key)");

        if (method.getMethode().isAnnotationPresent(RestAPI.class)) {
            Map<String, Object> erreur = new LinkedHashMap<>();
            erreur.put("code", HttpServletResponse.SC_UNAUTHORIZED);
            erreur.put("erreur", "NonAuthentifie");
            erreur.put("message", "Authentification requise");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print(JsonUtil.toJson(erreur));
            return;
        }
        String loginUrl = AuthUtil.getLoginUrl(getServletContext());
        if (loginUrl != null && !loginUrl.equals(route)) {
            response.sendRedirect(request.getContextPath() + loginUrl);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentification requise");
        }
    }

    protected void processRestAPI(MethodeInfo method, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String json;
        try {
            Object[] arguments = ParamUtil.getArguments(method.getMethode(), request);
            Class<?> clazz = method.getMethode().getDeclaringClass();
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Object resultat = method.getMethode().invoke(instance, arguments);
            json = JsonUtil.toJson(resultat);
        } catch (Exception e) {
            // l'exception levée par la méthode du controller est enveloppée dans InvocationTargetException
            Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
            // valeur du formulaire invalide : erreur du client (400), sinon erreur du serveur (500)
            int code = (cause instanceof ParamException)
                    ? HttpServletResponse.SC_BAD_REQUEST
                    : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            Map<String, Object> erreur = new LinkedHashMap<>();
            erreur.put("code", code);
            erreur.put("erreur", cause.getClass().getSimpleName());
            erreur.put("message", cause.getMessage());
            response.setStatus(code);
            json = JsonUtil.toJson(erreur);
        }
        response.getWriter().print(json);
    }

}