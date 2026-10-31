package model;

import java.util.HashMap;
import java.util.Map;

// Vue (JSP) + données à lui transmettre. Les valeurs peuvent être de n'importe quel type :
// mav.set("etudiant", unEtudiant) puis ${etudiant.nom} dans la JSP.
public class ModelAndView {
    private String urlSuivant;
    private HashMap<String, Object> list = new HashMap<>();

    public ModelAndView() {
    }

    // new ModelAndView("page") : vue /WEB-INF/views/page.jsp avec view-prefix et view-suffix
    public ModelAndView(String urlSuivant) {
        this.urlSuivant = urlSuivant;
    }

    public String getUrlSuivant() {
        return urlSuivant;
    }
    public void setUrlSuivant(String urlSuivant) {
        this.urlSuivant = urlSuivant;
    }

    public HashMap<String, Object> getList() {
        return list;
    }
    // une Map avec des valeurs de n'importe quel type
    public void setList(Map<String, ?> list) {
        this.list = new HashMap<>();
        if (list != null) {
            this.list.putAll(list);
        }
    }
    // ancienne signature, gardée pour que les applications déjà compilées avec les sprints 5 à 10 continuent de marcher
    public void setList(HashMap<String, String> list) {
        setList((Map<String, ?>) list);
    }

    public void set(String nom, Object valeur) {
        list.put(nom, valeur);
    }
    public Object get(String nom) {
        return list.get(nom);
    }
    public void remove(String nom) {
        list.remove(nom);
    }
}
