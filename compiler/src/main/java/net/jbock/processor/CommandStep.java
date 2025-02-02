package net.jbock.processor;

import io.jbock.javapoet.JavaFile;
import io.jbock.javapoet.TypeSpec;
import io.jbock.util.Either;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import net.jbock.Command;
import net.jbock.SuperCommand;
import net.jbock.common.Util;
import net.jbock.common.ValidationFailure;
import net.jbock.validate.CommandProcessor;
import net.jbock.validate.ValidateComponent;
import net.jbock.writing.ContextComponent;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.jbock.auto.common.BasicAnnotationProcessor.Step;
import static io.jbock.util.Either.right;
import static java.util.stream.Collectors.toSet;
import static net.jbock.common.Annotations.typeLevelAnnotations;
import static net.jbock.common.Util.checkNoDuplicateAnnotations;

/**
 * This step handles the {@link Command} and {@link SuperCommand} annotations.
 * It performs validation and source generation.
 */
class CommandStep implements Step {

    private final Messager messager;
    private final Util util;
    private final SourceFileGenerator sourceFileGenerator;
    private final Provider<ValidateComponent.Builder> validateComponentProvider;
    private final Provider<ContextComponent.Factory> contextComponentProvider;

    @Inject
    CommandStep(
            Messager messager,
            Util util,
            SourceFileGenerator sourceFileGenerator,
            Provider<ValidateComponent.Builder> validateComponentProvider,
            Provider<ContextComponent.Factory> contextComponentProvider) {
        this.messager = messager;
        this.util = util;
        this.sourceFileGenerator = sourceFileGenerator;
        this.validateComponentProvider = validateComponentProvider;
        this.contextComponentProvider = contextComponentProvider;
    }

    @Override
    public Set<String> annotations() {
        return typeLevelAnnotations().stream()
                .map(Class::getCanonicalName)
                .collect(toSet());
    }

    @Override
    public Set<? extends Element> process(Map<String, Set<Element>> elementsByAnnotation) {
        List<Element> elements = elementsByAnnotation.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toList());
        ElementFilter.typesIn(elements)
                .forEach(element ->
                        validateSourceElement(element).ifLeftOrElse(
                                this::printFailures,
                                this::processSourceElement));
        return Set.of();
    }

    private void processSourceElement(SourceElement sourceElement) {
        CommandProcessor processor = validateComponentProvider.get()
                .sourceElement(sourceElement)
                .build()
                .processor();
        processor.generate()
                .map(commandRepresentation -> contextComponentProvider.get().create(commandRepresentation))
                .ifLeftOrElse(
                        this::printFailures,
                        component -> writeSpec(sourceElement, component.parserClass().define()));
    }

    private void writeSpec(SourceElement sourceElement, TypeSpec typeSpec) {
        if (typeSpec.originatingElements.size() != 1) {
            throw new AssertionError();
        }
        String packageName = sourceElement.generatedClass().packageName();
        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .skipJavaLangImports(true)
                .build();
        sourceFileGenerator.write(sourceElement, javaFile);
    }

    private Either<List<ValidationFailure>, SourceElement> validateSourceElement(
            TypeElement element) {
        return util.commonTypeChecks(element)
                .map(List::of)
                .<Either<List<ValidationFailure>, SourceElement>>map(Either::left)
                .orElseGet(() -> right(SourceElement.create(element)))
                .filter(sourceElement -> checkNoDuplicateAnnotations(
                        sourceElement.element(), typeLevelAnnotations()).map(List::of));
    }

    private void printFailures(List<ValidationFailure> failures) {
        for (ValidationFailure failure : failures) {
            failure.writeTo(messager);
        }
    }
}
