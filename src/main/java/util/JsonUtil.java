package util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Sérialiseur JSON maison (sans dépendance externe), basé sur la réflexion
public class JsonUtil {

    public static String toJson(Object objet) {
        StringBuilder sb = new StringBuilder();
        // objets en cours de sérialisation, pour détecter les références circulaires
        Set<Object> enCours = Collections.newSetFromMap(new IdentityHashMap<>());
        ecrire(objet, sb, enCours);
        return sb.toString();
    }

    private static void ecrire(Object objet, StringBuilder sb, Set<Object> enCours) {
        if (objet == null) {
            sb.append("null");
        } else if (objet instanceof String || objet instanceof Character) {
            ecrireTexte(objet.toString(), sb);
        } else if (objet instanceof Boolean) {
            sb.append(objet);
        } else if (objet instanceof Number) {
            ecrireNombre((Number) objet, sb);
        } else if (objet instanceof Enum) {
            ecrireTexte(((Enum<?>) objet).name(), sb);
        } else if (objet instanceof Date || objet instanceof TemporalAccessor) {
            ecrireTexte(objet.toString(), sb);
        } else {
            // types composés : on vérifie qu'on ne boucle pas sur le même objet
            if (!enCours.add(objet)) {
                throw new IllegalStateException(
                        "Référence circulaire détectée sur " + objet.getClass().getName());
            }
            if (objet instanceof Map) {
                ecrireMap((Map<?, ?>) objet, sb, enCours);
            } else if (objet instanceof Iterable) {
                ecrireListe((Iterable<?>) objet, sb, enCours);
            } else if (objet.getClass().isArray()) {
                ecrireTableau(objet, sb, enCours);
            } else {
                ecrireObjet(objet, sb, enCours);
            }
            enCours.remove(objet);
        }
    }

    private static void ecrireTexte(String texte, StringBuilder sb) {
        sb.append('"');
        for (char c : texte.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void ecrireNombre(Number nombre, StringBuilder sb) {
        // NaN et Infinity n'existent pas en JSON
        if ((nombre instanceof Double && !Double.isFinite(nombre.doubleValue()))
                || (nombre instanceof Float && !Float.isFinite(nombre.floatValue()))) {
            sb.append("null");
        } else {
            sb.append(nombre);
        }
    }

    private static void ecrireMap(Map<?, ?> map, StringBuilder sb, Set<Object> enCours) {
        sb.append('{');
        boolean premier = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!premier) {
                sb.append(',');
            }
            ecrireTexte(String.valueOf(entry.getKey()), sb);
            sb.append(':');
            ecrire(entry.getValue(), sb, enCours);
            premier = false;
        }
        sb.append('}');
    }

    private static void ecrireListe(Iterable<?> liste, StringBuilder sb, Set<Object> enCours) {
        sb.append('[');
        boolean premier = true;
        for (Object element : liste) {
            if (!premier) {
                sb.append(',');
            }
            ecrire(element, sb, enCours);
            premier = false;
        }
        sb.append(']');
    }

    private static void ecrireTableau(Object tableau, StringBuilder sb, Set<Object> enCours) {
        // Array.get marche aussi pour les tableaux de types primitifs (int[], double[]...)
        sb.append('[');
        int taille = Array.getLength(tableau);
        for (int i = 0; i < taille; i++) {
            if (i > 0) {
                sb.append(',');
            }
            ecrire(Array.get(tableau, i), sb, enCours);
        }
        sb.append(']');
    }

    private static void ecrireObjet(Object objet, StringBuilder sb, Set<Object> enCours) {
        sb.append('{');
        boolean premier = true;
        for (Field field : getAttributs(objet.getClass())) {
            Object valeur;
            try {
                field.setAccessible(true);
                valeur = field.get(objet);
            } catch (Exception e) {
                // classe de la JDK non accessible par réflexion : on ignore l'attribut
                continue;
            }
            if (!premier) {
                sb.append(',');
            }
            ecrireTexte(field.getName(), sb);
            sb.append(':');
            ecrire(valeur, sb, enCours);
            premier = false;
        }
        sb.append('}');
    }

    // Attributs de la classe et de ses classes mères (sans static, transient ni synthétiques)
    private static List<Field> getAttributs(Class<?> clazz) {
        List<Field> attributs = new ArrayList<>();
        List<Class<?>> hierarchie = new ArrayList<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            hierarchie.add(0, c);
        }
        for (Class<?> c : hierarchie) {
            for (Field field : c.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || field.isSynthetic()) {
                    continue;
                }
                attributs.add(field);
            }
        }
        return attributs;
    }

}
