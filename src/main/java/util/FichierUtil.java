package util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import model.Fichier;

// Lecture des fichiers envoyés par un formulaire multipart/form-data
public class FichierUtil {

    // Paramètre de type Fichier, Fichier[] ou List<Fichier>
    public static boolean estParametreFichier(Parameter parametre) {
        Class<?> type = parametre.getType();
        return type == Fichier.class
                || type == Fichier[].class
                || (List.class.isAssignableFrom(type) && typeElement(parametre) == Fichier.class);
    }

    public static Object getValeur(Parameter parametre, String nom, HttpServletRequest request) {
        List<Fichier> fichiers = getFichiers(nom, request);
        Class<?> type = parametre.getType();
        if (type == Fichier.class) {
            // un seul fichier attendu : le premier, ou null si le champ est vide
            return fichiers.isEmpty() ? null : fichiers.get(0);
        }
        if (type == Fichier[].class) {
            return fichiers.toArray(new Fichier[0]);
        }
        return fichiers;
    }

    public static boolean estMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");
    }

    // Lit toutes les parties de la requête une fois : un fichier trop gros est signalé ici (erreur 400)
    public static Collection<Part> getParts(HttpServletRequest request) {
        try {
            return request.getParts();
        } catch (IllegalStateException e) {
            // levée par Tomcat quand maxFileSize ou maxRequestSize (@MultipartConfig) est dépassé
            throw new ParamException("Fichier trop volumineux : " + TAILLE_MAX_TEXTE);
        } catch (IOException | ServletException e) {
            throw new IllegalStateException("Lecture des fichiers impossible : " + e.getMessage(), e);
        }
    }

    public static final String TAILLE_MAX_TEXTE = "10 Mo maximum par fichier, 50 Mo par envoi";

    private static List<Fichier> getFichiers(String nom, HttpServletRequest request) {
        List<Fichier> fichiers = new ArrayList<>();
        // formulaire sans enctype="multipart/form-data" : aucun fichier n'est transmis
        if (!estMultipart(request)) {
            return fichiers;
        }
        for (Part part : getParts(request)) {
            String nomOrigine = part.getSubmittedFileName();
            // getSubmittedFileName() est null pour un champ texte ; vide pour un <input type="file"> non rempli
            if (!part.getName().equals(nom) || nomOrigine == null || nomOrigine.isEmpty()) {
                continue;
            }
            fichiers.add(lireFichier(part, nomOrigine));
        }
        return fichiers;
    }

    private static Fichier lireFichier(Part part, String nomOrigine) {
        Fichier fichier = new Fichier();
        // certains navigateurs envoient le chemin complet (C:\fakepath\photo.jpg) : on garde le nom seul
        String nom = nomOrigine.substring(Math.max(nomOrigine.lastIndexOf('/'), nomOrigine.lastIndexOf('\\')) + 1);
        fichier.setNom(nom);
        int point = nom.lastIndexOf('.');
        fichier.setExtension(point > 0 ? nom.substring(point + 1).toLowerCase() : "");
        fichier.setContentType(part.getContentType());
        fichier.setTaille(part.getSize());
        try (InputStream in = part.getInputStream()) {
            fichier.setContenu(in.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Lecture du fichier " + nom + " impossible : " + e.getMessage(), e);
        }
        return fichier;
    }

    // Type des éléments d'une liste : List<Fichier> → Fichier
    private static Type typeElement(Parameter parametre) {
        Type type = parametre.getParameterizedType();
        if (type instanceof ParameterizedType) {
            Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
            if (arguments.length == 1) {
                return arguments[0];
            }
        }
        return null;
    }

}
