package util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import annotation.Param;
import jakarta.servlet.http.HttpServletRequest;

// Construit les arguments d'une méthode de controller à partir des paramètres de la requête
public class ParamUtil {

    public static Object[] getArguments(Method methode, HttpServletRequest request) {
        Parameter[] parametres = methode.getParameters();
        Object[] arguments = new Object[parametres.length];
        for (int i = 0; i < parametres.length; i++) {
            String nom = getNom(methode, parametres[i], i);
            String valeur = request.getParameter(nom);
            arguments[i] = convertir(valeur, parametres[i].getType(), nom);
        }
        return arguments;
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
