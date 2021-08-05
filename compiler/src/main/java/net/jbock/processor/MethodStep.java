package net.jbock.processor;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.ImmutableSetMultimap;
import net.jbock.Command;
import net.jbock.common.Util;

import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.jbock.common.Annotations.methodLevelAnnotations;

@ProcessorScope
public class MethodStep implements BasicAnnotationProcessor.Step {

    private static final Set<TypeKind> FORBIDDEN_KINDS = EnumSet.of(
            TypeKind.VOID,
            TypeKind.TYPEVAR,
            TypeKind.WILDCARD,
            TypeKind.OTHER,
            TypeKind.ERROR,
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
                .collect(Collectors.toSet());
    }

    @Override
    public Set<? extends Element> process(ImmutableSetMultimap<String, Element> elementsByAnnotation) {
        for (ExecutableElement method : methodsIn(elementsByAnnotation.values())) {
            validateAnnotatedMethod(method).ifPresent(message ->
                    messager.printMessage(ERROR, message, method));
        }
        return Set.of();
    }

    private Optional<String> validateAnnotatedMethod(ExecutableElement method) {
        Element enclosingElement = method.getEnclosingElement();
        if (enclosingElement.getAnnotation(Command.class) == null) {
            return Optional.of("missing command annotation: add the @" +
                    Command.class.getSimpleName() +
                    " annotation to the enclosing class or interface '" +
                    enclosingElement.getSimpleName() + "'");
        }
        if (!method.getModifiers().contains(ABSTRACT)) {
            return Optional.of("abstract method expected");
        }
        if (!method.getParameters().isEmpty()) {
            return Optional.of("empty argument list expected");
        }
        if (!method.getTypeParameters().isEmpty()) {
            return Optional.of("type parameter" +
                    (method.getTypeParameters().size() >= 2 ? "s" : "") +
                    " not expected here");
        }
        TypeKind kind = method.getReturnType().getKind();
        if (FORBIDDEN_KINDS.contains(kind)) {
            return Optional.of("method may not return " + kind);
        }
        if (util.throwsAnyCheckedExceptions(method)) {
            return Optional.of("checked exceptions are not allowed here");
        }
        return Optional.empty();
    }
}
