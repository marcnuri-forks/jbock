package net.jbock.coerce.matching.matcher;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import net.jbock.coerce.Skew;
import net.jbock.coerce.matching.UnwrapSuccess;
import net.jbock.compiler.ParameterContext;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ListMatcher extends Matcher {

  @Inject
  ListMatcher(ParameterContext parameterContext) {
    super(parameterContext);
  }

  @Override
  public Optional<UnwrapSuccess> tryUnwrapReturnType() {
    ParameterSpec constructorParam = constructorParam(returnType());
    return tool().getSingleTypeArgument(returnType(), List.class)
        .map(typeArg -> UnwrapSuccess.create(typeArg, constructorParam, 1));
  }

  @Override
  public Skew skew() {
    return Skew.REPEATABLE;
  }

  @Override
  public CodeBlock tailExpr() {
    return CodeBlock.of(".collect($T.toList())", Collectors.class);
  }
}
