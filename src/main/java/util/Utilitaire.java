package util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import annotation.UrlAnnotation;
import model.MethodeInfo;
import model.UrlInfo;

public class Utilitaire {

    public static List<Class<?>> getClasses(String packageName)
            throws IOException, ClassNotFoundException {

        List<Class<?>> classes = new ArrayList<>();

        String packagePath = packageName.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Enumeration<URL> resources = classLoader.getResources(packagePath);

        while (resources.hasMoreElements()) {

            URL resource = resources.nextElement();

            File directory = new File(resource.getFile());

            if (directory.exists()) {

                File[] files = directory.listFiles();

                if (files != null) {

                    for (File file : files) {

                        if (file.getName().endsWith(".class")) {

                            String simpleName = file.getName()
                                    .substring(0, file.getName().length() - 6);

                            String fullName = packageName + "." + simpleName;

                            Class<?> clazz = Class.forName(fullName);

                            classes.add(clazz);
                        }
                    }
                }
            }
        }

        return classes;
    }

    public static List<Class<?>> getClassesWithAnnotation(String packageName, Class annotationClass , Class methodeClass ,HashMap<UrlInfo,MethodeInfo> mapping)
            throws IOException, ClassNotFoundException {
        List<Class<?>> result = new ArrayList<>();
        List<Class<?>> classes = getClasses(packageName);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(annotationClass)) {
                List<Method> meth = getMethodsWithAnnotation(clazz, methodeClass);
                for (Method methode : meth) {
                            UrlAnnotation urlAnnotation = methode.getAnnotation(UrlAnnotation.class);
                            UrlInfo u = new UrlInfo();
                            u.setAction(urlAnnotation.httpmethode());
                            u.setUrl(urlAnnotation.value()); 
                            MethodeInfo m = new MethodeInfo();
                            m.setMethode(methode);
                            
                            if(mapping.containsKey(u)){
                                throw new IllegalStateException("route en double");
                            };
                            mapping.put(u , m);
                        }
                result.add(clazz);
            }
        }

        return result;
    }

    public static List<Method> getMethodsWithAnnotation(Class<?> clazz, Class annotationClass) {
        List<Method> result = new ArrayList<>();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {

            if (method.isAnnotationPresent(annotationClass)) {
                result.add(method);
            }
        }
        return result;
    }
}