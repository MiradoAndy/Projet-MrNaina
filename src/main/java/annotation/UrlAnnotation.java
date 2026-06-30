package annotation;

import java.lang.annotation.Target;

import model.HTTPmethode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UrlAnnotation {
    String value();
    HTTPmethode httpmethode() default HTTPmethode.GET;
}