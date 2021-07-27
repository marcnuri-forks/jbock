package net.jbock.convert.matching;

import com.squareup.javapoet.CodeBlock;
import io.jbock.util.Either;
import net.jbock.Converter;
import net.jbock.annotated.AnnotatedMethod;
import net.jbock.common.Util;
import net.jbock.convert.ConvertScope;
import net.jbock.convert.Mapped;
import net.jbock.convert.matcher.Matcher;
import net.jbock.convert.reference.ReferenceTool;
import net.jbock.convert.reference.StringConverterType;
import net.jbock.processor.SourceElement;
import net.jbock.source.SourceMethod;

import javax.inject.Inject;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static io.jbock.util.Either.left;
import static javax.lang.model.element.Modifier.ABSTRACT;

@ConvertScope
public class ConverterValidator extends MatchValidator {

    private final List<Matcher> matchers;
    private final ReferenceTool referenceTool;
    private final Util util;
    private final SourceElement sourceElement;
    private final Types types;

    @Inject
    ConverterValidator(
            List<Matcher> matchers,
            ReferenceTool referenceTool,
            Util util,
            SourceElement sourceElement,
            Types types) {
        this.matchers = matchers;
        this.referenceTool = referenceTool;
        this.util = util;
        this.sourceElement = sourceElement;
        this.types = types;
    }

    public <M extends AnnotatedMethod> Either<String, Mapped<M>> validate(
            SourceMethod<M> parameter,
            TypeElement converter) {
        return util.commonTypeChecks(converter)
                .or(() -> checkNotAbstract(converter))
                .or(() -> checkNoTypevars(converter))
                .or(() -> checkConverterAnnotationPresent(converter))
                .<Either<String, StringConverterType>>map(Either::left)
                .orElseGet(() -> referenceTool.getReferencedType(converter))
                .flatMap(functionType -> tryAllMatchers(functionType, parameter, converter));
    }

    private <M extends AnnotatedMethod> Either<String, Mapped<M>> tryAllMatchers(
            StringConverterType functionType,
            SourceMethod<M> parameter,
            TypeElement converter) {
        List<Match> matches = new ArrayList<>();
        for (Matcher matcher : matchers) {
            Optional<Match> match = matcher.tryMatch(parameter);
            match.ifPresent(matches::add);
            match = match.filter(m -> isValidMatch(m, functionType));
            if (match.isPresent()) {
                Match m = match.orElseThrow();
                return validateMatch(parameter, m)
                        .<Either<String, CodeBlock>>map(Either::left)
                        .orElseGet(() -> Either.right(getMapExpr(functionType, converter)))
                        .map(code -> new MapExpr(code, m.baseType(), false))
                        .map(mapExpr -> m.toConvertedParameter(mapExpr, parameter));
            }
        }
        TypeMirror typeForErrorMessage = matches.stream()
                .max(Comparator.comparing(Match::multiplicity))
                .map(Match::baseType)
                .orElse(parameter.returnType());
        return left(noMatchError(typeForErrorMessage));
    }

    private Optional<String> checkConverterAnnotationPresent(TypeElement converter) {
        Converter converterAnnotation = converter.getAnnotation(Converter.class);
        boolean nestedMapper = util.getEnclosingElements(converter).contains(sourceElement.element());
        if (converterAnnotation == null && !nestedMapper) {
            return Optional.of("converter must be an inner class of the command class, or carry the @"
                    + Converter.class.getSimpleName() + " annotation");
        }
        return Optional.empty();
    }

    private Optional<String> checkNotAbstract(TypeElement converter) {
        if (converter.getModifiers().contains(ABSTRACT)) {
            return Optional.of("converter class may not be abstract");
        }
        return Optional.empty();
    }

    private Optional<String> checkNoTypevars(TypeElement converter) {
        if (!converter.getTypeParameters().isEmpty()) {
            return Optional.of("type parameters are not allowed in converter class declaration");
        }
        return Optional.empty();
    }

    private CodeBlock getMapExpr(StringConverterType functionType, TypeElement converter) {
        if (functionType.isSupplier()) {
            return CodeBlock.of("new $T().get()", converter.asType());
        }
        return CodeBlock.of("new $T()", converter.asType());
    }

    private boolean isValidMatch(Match match, StringConverterType functionType) {
        return types.isSameType(functionType.outputType(), match.baseType());
    }

    private String noMatchError(TypeMirror type) {
        return "converter should extend StringConverter<" + util.typeToString(type) + ">";
    }
}
