package util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import annotation.Param;
import jakarta.servlet.http.HttpServletRequest;

// Construit les arguments d'une méthode de controller à partir des paramètres de la requête
public class ParamUtil {

    public static Object[] getArguments(Method methode, HttpServletRequest request) {
        Parameter[] parametres = methode.getParameters();
        Object[] arguments = new Object[parametres.length];
        for (int i = 0; i < parametres.length; i++) {
            String nom = getNom(methode, parametres[i], i);
            Class<?> type = parametres[i].getType();
            if (estTypeSimple(type)) {
                arguments[i] = convertir(request.getParameter(nom), type, nom);
            } else {
                verifierObjet(type, nom);
                // les champs de l'objet arrivent préfixés : personne.nom, personne.age...
                arguments[i] = construireObjet(type, nom, request, new HashSet<>());
            }
        }
        return arguments;
    }

    public static boolean estTypeSimple(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Integer.class || type == Long.class || type == Double.class
                || type == Float.class || type == Short.class || type == Byte.class
                || type == Character.class || type == Boolean.class;
    }

    // Objet construisible : une classe concrète du projet (pas un tableau, une liste, une interface...)
    private static boolean estObjetConstructible(Class<?> type) {
        return !type.isArray() && !type.isInterface() && !type.isEnum()
                && !Modifier.isAbstract(type.getModifiers())
                && !type.getName().startsWith("java.") && !type.getName().startsWith("jakarta.");
    }

    private static void verifierObjet(Class<?> type, String nom) {
        if (!estObjetConstructible(type)) {
            throw new IllegalStateException(nom + " : type " + type.getName() + " non supporté en paramètre");
        }
    }

    // enCours : classes en cours de construction, pour ne pas boucler (ex : Personne qui contient une Personne)
    private static Object construireObjet(Class<?> type, String prefixe, HttpServletRequest request,
            Set<Class<?>> enCours) {
        Object objet;
        try {
            Constructor<?> constructeur = type.getDeclaredConstructor();
            constructeur.setAccessible(true);
            objet = constructeur.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(type.getSimpleName()
                    + " doit avoir un constructeur sans argument pour être construit depuis un formulaire");
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de créer " + type.getSimpleName() + " : " + e.getMessage(), e);
        }

        enCours.add(type);
        for (Field field : getAttributs(type)) {
            String nom = prefixe + "." + field.getName();
            Class<?> typeAttribut = field.getType();
            if (estTypeSimple(typeAttribut)) {
                String valeur = request.getParameter(nom);
                // champ absent du formulaire : on garde la valeur initiale de l'objet
                if (valeur != null) {
                    affecter(objet, field, convertir(valeur, typeAttribut, nom), nom);
                }
            } else if (estObjetConstructible(typeAttribut) && !enCours.contains(typeAttribut)
                    && contientPrefixe(request, nom)) {
                // objet imbriqué (personne.adresse.ville) : construit seulement si le formulaire en parle
                affecter(objet, field, construireObjet(typeAttribut, nom, request, enCours), nom);
            }
            // autres types (List, Date, tableau...) : ignorés pour l'instant
        }
        enCours.remove(type);
        return objet;
    }

    private static boolean contientPrefixe(HttpServletRequest request, String prefixe) {
        Enumeration<String> noms = request.getParameterNames();
        while (noms.hasMoreElements()) {
            if (noms.nextElement().startsWith(prefixe + ".")) {
                return true;
            }
        }
        return false;
    }

    // setNom(...) s'il existe, sinon affectation directe de l'attribut
    private static void affecter(Object objet, Field field, Object valeur, String nom) {
        String nomSetter = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        try {
            Method setter = objet.getClass().getMethod(nomSetter, field.getType());
            setter.invoke(objet, valeur);
        } catch (NoSuchMethodException e) {
            try {
                field.setAccessible(true);
                field.set(objet, valeur);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(nom + " : impossible d'affecter l'attribut", ex);
            }
        } catch (InvocationTargetException e) {
            // le setter a refusé la valeur (ex : throw new IllegalArgumentException("âge négatif"))
            throw new ParamException(nom + " : " + e.getCause().getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(nom + " : setter " + nomSetter + " inaccessible", e);
        }
    }

    // Attributs de la classe et de ses classes mères (sans static ni final)
    private static List<Field> getAttributs(Class<?> clazz) {
        List<Field> attributs = new ArrayList<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || field.isSynthetic()) {
                    continue;
                }
                attributs.add(field);
            }
        }
        return attributs;
    }

    // @Param("nom") en priorité, sinon le nom Java (seulement si compilé avec -parameters)
    private static String getNom(Method methode, Parameter parametre, int index) {
        Param param = parametre.getAnnotation(Param.class);
        if (param != null) {
            return param.value();
        }
        if (parametre.isNamePresent()) {
            return parametre.getName();
        }
        // sans -parameters, Java renomme les paramètres en arg0, arg1... : on refuse plutôt que d'injecter null
        throw new IllegalStateException("Paramètre n°" + (index + 1) + " de "
                + methode.getDeclaringClass().getSimpleName() + "." + methode.getName()
                + "() : ajoutez @Param(\"nomDuChamp\") ou compilez avec l'option -parameters");
    }

    public static Object convertir(String valeur, Class<?> type, String nom) {
        if (type == String.class) {
            return valeur;
        }
        if (type == boolean.class || type == Boolean.class) {
            return convertirBoolean(valeur, type, nom);
        }
        // champ absent ou laissé vide : valeur par défaut (0 pour un primitif, null sinon)
        if (valeur == null || valeur.trim().isEmpty()) {
            return valeurParDefaut(type);
        }
        String v = valeur.trim();
        try {
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(v);
            }
            if (type == long.class || type == Long.class) {
                return Long.parseLong(v);
            }
            if (type == double.class || type == Double.class) {
                return Double.parseDouble(v);
            }
            if (type == float.class || type == Float.class) {
                return Float.parseFloat(v);
            }
            if (type == short.class || type == Short.class) {
                return Short.parseShort(v);
            }
            if (type == byte.class || type == Byte.class) {
                return Byte.parseByte(v);
            }
        } catch (NumberFormatException e) {
            throw new ParamException(nom + " : '" + valeur + "' n'est pas un " + type.getSimpleName() + " valide");
        }
        if (type == char.class || type == Character.class) {
            if (valeur.length() != 1) {
                throw new ParamException(nom + " : '" + valeur + "' doit contenir un seul caractère");
            }
            return valeur.charAt(0);
        }
        throw new IllegalStateException(nom + " : type " + type.getName() + " non supporté");
    }

    // une case à cocher non cochée n'est pas envoyée, une case cochée envoie "on"
    private static Object convertirBoolean(String valeur, Class<?> type, String nom) {
        if (valeur == null || valeur.trim().isEmpty()) {
            return type == boolean.class ? false : null;
        }
        String v = valeur.trim().toLowerCase();
        if (v.equals("on") || v.equals("true") || v.equals("1") || v.equals("oui")) {
            return true;
        }
        if (v.equals("off") || v.equals("false") || v.equals("0") || v.equals("non")) {
            return false;
        }
        throw new ParamException(nom + " : '" + valeur + "' n'est pas un boolean valide");
    }

    private static Object valeurParDefaut(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }

}
