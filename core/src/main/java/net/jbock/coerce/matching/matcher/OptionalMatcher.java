package net.jbock.coerce.matching.matcher;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import net.jbock.coerce.Skew;
import net.jbock.coerce.matching.Match;
import net.jbock.compiler.ParameterContext;
import net.jbock.compiler.parameter.AbstractParameter;
import net.jbock.compiler.parameter.ParameterStyle;

import javax.inject.Inject;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

public class OptionalMatcher extends Matcher {

  @Inject
  OptionalMatcher(ParameterContext parameterContext) {
    super(parameterContext);
  }

  @Override
  public Optional<Match> tryMatch(AbstractParameter parameter) {
    if (parameter.style() == ParameterStyle.PARAMETERS) {
      // @Parameters doesn't do optional
      return Optional.empty();
    }
    Optional<Match> optionalPrimitive = getOptionalPrimitive(returnType());
    if (optionalPrimitive.isPresent()) {
      return optionalPrimitive;
    }
    return tool().getSingleTypeArgument(returnType(), Optional.class)
        .map(typeArg -> Match.create(typeArg, constructorParam(returnType()), Skew.OPTIONAL, tailExpr()));
  }

  private CodeBlock tailExpr() {
    return CodeBlock.of(".findAny()");
  }

  private Optional<Match> getOptionalPrimitive(TypeMirror type) {
    for (OptionalPrimitive optionalPrimitive : OptionalPrimitive.values()) {
      if (tool().isSameType(type, optionalPrimitive.type())) {
        ParameterSpec constructorParam = constructorParam(asOptional(optionalPrimitive));
        return Optional.of(Match.create(
            tool().asTypeElement(optionalPrimitive.wrappedObjectType()).asType(),
            constructorParam,
            Skew.OPTIONAL,
            tailExpr(),
            optionalPrimitive.extractExpr(constructorParam)));
      }
    }
    return Optional.empty();
  }

  private DeclaredType asOptional(OptionalPrimitive optionalPrimitive) {
    TypeElement optional = tool().asTypeElement(Optional.class.getCanonicalName());
    TypeElement element = tool().asTypeElement(optionalPrimitive.wrappedObjectType());
    return tool().types().getDeclaredType(optional, element.asType());
  }
}
