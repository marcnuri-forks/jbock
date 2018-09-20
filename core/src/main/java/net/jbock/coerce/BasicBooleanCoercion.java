package net.jbock.coerce;

import net.jbock.com.squareup.javapoet.CodeBlock;
import net.jbock.com.squareup.javapoet.FieldSpec;

import java.util.Objects;

abstract class BasicBooleanCoercion extends CoercionFactory {

  BasicBooleanCoercion(Class<?> trigger) {
    super(trigger);
  }

  @Override
  final CodeBlock map() {
    return CodeBlock.builder().build();
  }

  @Override
  CodeBlock jsonExpr(String param) {
    return CodeBlock.builder().add("$L", param).build();
  }

  CodeBlock mapJsonExpr(FieldSpec field) {
    return CodeBlock.builder().add(".map($T::toString)", Objects.class).build();
  }
}
