package model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpSession;

// Session personnalisée injectée dans les méthodes de controller : public String profil(Session session)
// Elle enveloppe la HttpSession de Tomcat et garde les données de l'utilisateur dans sa propre Map.
public class Session {
    // transient : ignoré par JsonUtil, on ne sérialise que les données
    private final transient HttpSession httpSession;
    // nom de la donnée -> valeur (ConcurrentHashMap : plusieurs requêtes du même utilisateur en parallèle)
    private final Map<String, Object> donnees = new ConcurrentHashMap<>();

    public Session(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    // ---- données de la session (comme HttpSession) ----

    public Object getAttribute(String nom) {
        return donnees.get(nom);
    }

    // une valeur null supprime la donnée, comme le fait HttpSession
    public void setAttribute(String nom, Object valeur) {
        if (valeur == null) {
            donnees.remove(nom);
        } else {
            donnees.put(nom, valeur);
        }
    }

    public void removeAttribute(String nom) {
        donnees.remove(nom);
    }

    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(new HashSet<>(donnees.keySet()));
    }

    // ---- infos de la session (déléguées à la HttpSession) ----

    public String getId() {
        return httpSession.getId();
    }

    public long getCreationTime() {
        return httpSession.getCreationTime();
    }

    public long getLastAccessedTime() {
        return httpSession.getLastAccessedTime();
    }

    // durée d'inactivité (en secondes) avant que la session expire
    public int getMaxInactiveInterval() {
        return httpSession.getMaxInactiveInterval();
    }

    public void setMaxInactiveInterval(int secondes) {
        httpSession.setMaxInactiveInterval(secondes);
    }

    public boolean isNew() {
        return httpSession.isNew();
    }

    // ferme la session : le listener la retire du cache et ses données sont effacées
    public void invalidate() {
        donnees.clear();
        httpSession.invalidate();
    }
}
