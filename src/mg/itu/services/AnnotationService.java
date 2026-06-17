package mg.itu.services;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mg.itu.annotation.MonController;

public class AnnotationService {
    public static List<Class<?>> scanPackage(String packageName) throws Exception {
        List<Class<?>> result = new ArrayList<>();

        // Convertir le package en chemin de fichier
        String path = packageName.replace('.', '/');
        File folder = new File(
            Thread.currentThread()
                .getContextClassLoader()
                .getResource(path)
                .toURI()
        );

        // Parcourir les fichiers .class
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".class")) {
                String className = packageName + "." 
                                + file.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(MonController.class)) {
                    result.add(clazz);
                }
            }
        }

        return result;
    }
}
