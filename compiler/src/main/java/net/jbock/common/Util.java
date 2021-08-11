package net.jbock.common;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Util {

    private final SafeTypes types;
    private final TypeTool tool;

    public Util(SafeTypes types, TypeTool tool) {
        this.types = types;
        this.tool = tool;
    }

    /* Left-Optional
     */
    public Optional<ValidationFailure> commonTypeChecks(TypeElement classToCheck) {
        if (classToCheck.getNestingKind().isNested() && !classToCheck.getModifiers().contains(Modifier.STATIC)) {
            return Optional.of(new ValidationFailure("nested class must be static", classToCheck));
        }
        for (TypeElement element : getEnclosingElements(classToCheck)) {
            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                return Optional.of(new ValidationFailure("class cannot be private", classToCheck));
            }
        }
        if (!hasDefaultConstructor(classToCheck)) {
            return Optional.of(new ValidationFailure("default constructor not found", classToCheck));
        }
        return Optional.empty();
    }

    private boolean hasDefaultConstructor(TypeElement classToCheck) {
        List<ExecutableElement> constructors = ElementFilter.constructorsIn(classToCheck.getEnclosedElements());
        if (constructors.isEmpty()) {
            return true;
        }
        for (ExecutableElement constructor : constructors) {
            if (!constructor.getModifiers().contains(Modifier.PRIVATE) &&
                    constructor.getParameters().isEmpty() &&
                    invalidExceptionsInDeclaration(constructor).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public List<TypeMirror> invalidExceptionsInDeclaration(ExecutableElement element) {
        return element.getThrownTypes().stream()
                .filter(thrownType -> !isPermissibleException(thrownType))
                .collect(toList());
    }

    private boolean isPermissibleException(TypeMirror mirror) {
        if (mirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        if (tool.isSameType(mirror, RuntimeException.class) ||
                tool.isSameType(mirror, Error.class)) {
            return true;
        }
        return types.asElement(mirror)
                .flatMap(TypeTool.AS_TYPE_ELEMENT::visit)
                .filter(t -> commonTypeChecks(t).isEmpty())
                .map(TypeElement::getSuperclass)
                .filter(this::isPermissibleException)
                .isPresent();
    }

    public List<TypeElement> getEnclosingElements(TypeElement sourceElement) {
        LinkedList<TypeElement> result = new LinkedList<>();
        result.add(sourceElement);
        while (result.getLast().getNestingKind() == NestingKind.MEMBER) {
            Element enclosingElement = result.getLast().getEnclosingElement();
            TypeTool.AS_TYPE_ELEMENT.visit(enclosingElement)
                    .ifPresent(result::add);
        }
        return new ArrayList<>(result);
    }

    public String typeToString(TypeMirror type) {
        return TypeTool.AS_DECLARED.visit(type).flatMap(declared ->
                TypeTool.AS_TYPE_ELEMENT.visit(declared.asElement()).map(el -> {
                    String base = el.getSimpleName().toString();
                    if (declared.getTypeArguments().isEmpty()) {
                        return base;
                    }
                    return base + declared.getTypeArguments().stream().map(this::typeToString)
                            .collect(Collectors.joining(", ", "<", ">"));
                })).orElseGet(type::toString);
    }

    /* Left-Optional
     */
    public Optional<ValidationFailure> checkNoDuplicateAnnotations(
            ExecutableElement element,
            List<Class<? extends Annotation>> annotations) {
        List<Class<? extends Annotation>> present = annotations.stream()
                .filter(ann -> element.getAnnotation(ann) != null)
                .collect(toList());
        if (present.size() >= 2) {
            return Optional.of(new ValidationFailure("annotate with either @" + present.get(0).getSimpleName() +
                    " or @" + present.get(1).getSimpleName() + " but not both", element));
        }
        return Optional.empty();
    }

    public SafeTypes types() {
        return types;
    }
}
