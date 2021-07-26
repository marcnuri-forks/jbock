package net.jbock.common;

import net.jbock.Parameter;
import net.jbock.method.MethodAnnotation;

import javax.lang.model.element.ExecutableElement;
import java.lang.annotation.Annotation;

public final class AnnotatedMethod {

    private final ExecutableElement sourceMethod;
    private final Annotation annotation;

    private AnnotatedMethod(ExecutableElement sourceMethod, Annotation annotation) {
        this.sourceMethod = sourceMethod;
        this.annotation = annotation;
    }

    public static AnnotatedMethod create(ExecutableElement sourceMethod, Annotation annotation) {
        return new AnnotatedMethod(sourceMethod, annotation);
    }

    public boolean isParameter() {
        return annotation instanceof Parameter;
    }

    public ExecutableElement sourceMethod() {
        return sourceMethod;
    }

    public MethodAnnotation annotation(int numberOfParameters) {
        return MethodAnnotation.create(sourceMethod, annotation, numberOfParameters);
    }
}
