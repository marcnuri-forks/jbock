package net.jbock.processor;

import io.jbock.auto.common.BasicAnnotationProcessor.Step;
import jakarta.inject.Inject;
import net.jbock.Command;
import net.jbock.SuperCommand;
import net.jbock.common.Util;
import net.jbock.common.ValidationFailure;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.jbock.common.Annotations.methodLevelAnnotations;

class MethodStep implements Step {

    private static final Set<TypeKind> FORBIDDEN_KINDS = EnumSet.of(
            TypeKind.VOID,
            TypeKind.TYPEVAR,
            TypeKind.WILDCARD,
            TypeKind.OTHER,
            TypeKind.UNION,
            TypeKind.NONE);

    private final Messager messager;
    private final Util util;

    @Inject
    MethodStep(Messager messager, Util util) {
        this.messager = messager;
        this.util = util;
    }

    @Override
    public Set<String> annotations() {
        return methodLevelAnnotations().stream()
                .map(Class::getCanonicalName)
                .collect(toSet());
    }

    @Override
    public Set<? extends Element> process(Map<String, Set<Element>> elementsByAnnotation) {
        List<Element> elements = elementsByAnnotation.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toList());
        for (ExecutableElement method : methodsIn(elements)) {
            validateCommandAnnotationPresent(method)
                    .or(() -> validateAbstract(method))
                    .or(() -> validateTypeParameters(method))
                    .or(() -> validateReturnType(method))
                    .or(() -> util.checkExceptionsInDeclaration(method))
                    .ifPresent(failure -> failure.writeTo(messager));
        }
        return Set.of();
    }

    private Optional<ValidationFailure> validateCommandAnnotationPresent(ExecutableElement method) {
        Element enclosingElement = method.getEnclosingElement();
        if (enclosingElement.getAnnotation(Command.class) != null
                || enclosingElement.getAnnotation(SuperCommand.class) != null) {
            return Optional.empty();
        }
        String enclosingElementKind = enclosingElement.getKind() == ElementKind.INTERFACE ?
                "interface" : "abstract class";
        return Optional.of(new ValidationFailure("missing command annotation: " +
                enclosingElementKind + " '" + enclosingElement.getSimpleName() +
                "' must be annotated with " + Command.class.getCanonicalName() + " or " + SuperCommand.class.getCanonicalName(),
                enclosingElement));
    }

    private Optional<ValidationFailure> validateAbstract(ExecutableElement method) {
        if (method.getModifiers().contains(ABSTRACT)) {
            return Optional.empty();
        }
        return Optional.of(new ValidationFailure("missing method modifier: annotated method '" +
                method.getSimpleName() +
                "' must be abstract", method));
    }

    private Optional<ValidationFailure> validateTypeParameters(ExecutableElement method) {
        if (method.getTypeParameters().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ValidationFailure("invalid type parameters: annotated method '" +
                method.getSimpleName() +
                "' may not have type parameters, but found: " +
                method.getTypeParameters(), method));
    }

    private Optional<ValidationFailure> validateReturnType(ExecutableElement method) {
        TypeKind kind = method.getReturnType().getKind();
        if (!FORBIDDEN_KINDS.contains(kind)) {
            return Optional.empty();
        }
        return Optional.of(new ValidationFailure("invalid return type: annotated method '" +
                method.getSimpleName() +
                "' may not return " + kind, method));
    }
}
