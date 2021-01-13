package net.jbock.compiler;

import com.google.common.collect.ImmutableList;
import dagger.Module;
import dagger.Provides;
import dagger.Reusable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Optional;

@Module
class ParameterModule {

  private final TypeElement sourceElement;
  private final Optional<TypeElement> mapperClass;
  private final String bundleKey;

  ParameterModule(TypeElement sourceElement, Optional<TypeElement> mapperClass, String bundleKey) {
    this.sourceElement = sourceElement;
    this.mapperClass = mapperClass;
    this.bundleKey = bundleKey;
  }

  @Provides
  @SourceElement
  TypeElement sourceElement() {
    return sourceElement;
  }

  @Provides
  @MapperClass
  Optional<TypeElement> mapperClass() {
    return mapperClass;
  }

  @Provides
  @BundleKey
  public String getBundleKey() {
    return bundleKey;
  }

  @Reusable
  @Provides
  EnumName enumName(ExecutableElement sourceMethod, ImmutableList<Parameter> alreadyCreated) {
    String methodName = sourceMethod.getSimpleName().toString();
    EnumName result = EnumName.create(methodName);
    for (Parameter param : alreadyCreated) {
      if (param.paramName().enumConstant().equals(result.enumConstant())) {
        return result.append(Integer.toString(alreadyCreated.size()));
      }
    }
    return result;
  }
}
