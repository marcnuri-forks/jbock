package net.jbock.convert.matching;

import com.squareup.javapoet.CodeBlock;
import io.jbock.util.Either;
import net.jbock.annotated.AnnotatedMethod;
import net.jbock.common.Util;
import net.jbock.common.ValidationFailure;
import net.jbock.convert.Mapping;
import net.jbock.convert.reference.ReferenceTool;
import net.jbock.convert.reference.StringConverterType;
import net.jbock.processor.SourceElement;
import net.jbock.util.StringConverter;
import net.jbock.validate.ValidateScope;

import javax.inject.Inject;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import java.util.Optional;
import java.util.function.Supplier;

import static javax.lang.model.element.Modifier.ABSTRACT;

@ValidateScope
public class ConverterValidator {

    private final ReferenceTool referenceTool;
    private final Util util;
    private final SourceElement sourceElement;
    private final Types types;
    private final MatchFinder matchFinder;

    @Inject
    ConverterValidator(
            ReferenceTool referenceTool,
            Util util,
            SourceElement sourceElement,
            Types types,
            MatchFinder matchFinder) {
        this.referenceTool = referenceTool;
        this.util = util;
        this.sourceElement = sourceElement;
        this.types = types;
        this.matchFinder = matchFinder;
    }

    public <M extends AnnotatedMethod>
    Either<ValidationFailure, Mapping<M>> findMapping(
            M sourceMethod,
            TypeElement converter) {
        return checkConverterIsInnerClass(sourceMethod, converter)
                .or(() -> util.commonTypeChecks(converter))
                .or(() -> checkNotAbstract(sourceMethod, converter))
                .or(() -> checkNoTypeVars(sourceMethod, converter))
                .or(() -> checkConverterIsInnerClass(sourceMethod, converter))
                .<Either<ValidationFailure, StringConverterType>>map(Either::left)
                .orElseGet(() -> referenceTool.getReferencedType(sourceMethod, converter))
                .mapLeft(failure -> failure.prepend("invalid converter class: "))
                .flatMap(converterType -> tryAllMatchers(converterType, sourceMethod, converter));
    }

    private <M extends AnnotatedMethod>
    Either<ValidationFailure, Mapping<M>> tryAllMatchers(
            StringConverterType converterType,
            M sourceMethod,
            TypeElement converter) {
        return matchFinder.findMatch(sourceMethod)
                .filter(match -> isValidMatch(match, converterType))
                .map(match -> Mapping.create(getMapExpr(converterType, converter), match, false));
    }

    /* Left-Optional
     */
    private <M extends AnnotatedMethod>
    Optional<ValidationFailure> checkConverterIsInnerClass(
            M sourceMethod,
            TypeElement converter) {
        boolean nestedMapper = util.getEnclosingElements(converter).contains(sourceElement.element());
        if (!nestedMapper) {
            return Optional.of(sourceMethod.fail("converter of '" +
                    sourceMethod.methodName() +
                    "' must be an inner class of the command class '" +
                    sourceElement.element().getSimpleName() + "'"));
        }
        return Optional.empty();
    }

    /* Left-Optional
     */
    private <M extends AnnotatedMethod>
    Optional<ValidationFailure> checkNotAbstract(
            M sourceMethod,
            TypeElement converter) {
        if (converter.getModifiers().contains(ABSTRACT)) {
            return Optional.of(sourceMethod.fail("the converter class '" +
                    converter.getSimpleName() +
                    "' may not be abstract"));
        }
        return Optional.empty();
    }

    /* Left-Optional
     */
    private <M extends AnnotatedMethod>
    Optional<ValidationFailure> checkNoTypeVars(
            M sourceMethod,
            TypeElement converter) {
        if (!converter.getTypeParameters().isEmpty()) {
            return Optional.of(sourceMethod.fail("type parameters are not allowed in the declaration of" +
                    " converter class '" +
                    converter.getSimpleName() +
                    "'"));
        }
        return Optional.empty();
    }

    private CodeBlock getMapExpr(
            StringConverterType functionType,
            TypeElement converter) {
        if (functionType.isSupplier()) {
            return CodeBlock.of("new $T().get()", converter.asType());
        }
        return CodeBlock.of("new $T()", converter.asType());
    }

    private <M extends AnnotatedMethod>
    Optional<ValidationFailure> isValidMatch(
            ValidMatch<M> match,
            StringConverterType converterType) {
        if (!types.isSameType(converterType.outputType(), match.baseType())) {
            String expectedType = StringConverter.class.getSimpleName() +
                    "<" + util.typeToString(match.baseType()) + ">";
            return Optional.of(match.sourceMethod().fail("invalid converter class: should extend " +
                    expectedType + " or implement " + Supplier.class.getSimpleName() + "<" + expectedType + ">"));
        }
        return Optional.empty();
    }
}
