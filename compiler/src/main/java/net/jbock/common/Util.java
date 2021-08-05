package net.jbock.common;

import net.jbock.util.StringConverter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Util {

    private final Types types;
    private final TypeTool tool;

    public Util(Types types, TypeTool tool) {
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
                    !throwsAnyCheckedExceptions(constructor)) {
                return true;
            }
        }
        return false;
    }

    public boolean throwsAnyCheckedExceptions(ExecutableElement element) {
        for (TypeMirror thrownType : element.getThrownTypes()) {
            if (!extendsRuntimeException(thrownType)) {
                return true;
            }
        }
        return false;
    }

    private boolean extendsRuntimeException(TypeMirror mirror) {
        if (mirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        if (tool.isSameType(mirror, RuntimeException.class)) {
            return true;
        }
        return TypeTool.AS_TYPE_ELEMENT.visit(types.asElement(mirror))
                .map(TypeElement::getSuperclass)
                .filter(this::extendsRuntimeException)
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
        if (type.getKind() != TypeKind.DECLARED) {
            return type.toString();
        }
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
                .collect(Collectors.toUnmodifiableList());
        if (present.size() >= 2) {
            return Optional.of(new ValidationFailure("annotate with either @" + present.get(0).getSimpleName() +
                    " or @" + present.get(1).getSimpleName() + " but not both", element));
        }
        return Optional.empty();
    }

    public String noMatchError(TypeMirror type) {
        return "define a converter that implements " +
                StringConverter.class.getSimpleName() +
                "<" + typeToString(type) + ">";
    }
}
