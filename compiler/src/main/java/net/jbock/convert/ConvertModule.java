package net.jbock.convert;

import dagger.Module;
import dagger.Provides;
import net.jbock.common.Constants;
import net.jbock.common.EnumName;
import net.jbock.common.SafeElements;
import net.jbock.common.TypeTool;
import net.jbock.common.Util;
import net.jbock.convert.matcher.ExactMatcher;
import net.jbock.convert.matcher.ListMatcher;
import net.jbock.convert.matcher.Matcher;
import net.jbock.convert.matcher.OptionalMatcher;
import net.jbock.parameter.NamedOption;
import net.jbock.parameter.PositionalParameter;
import net.jbock.parameter.SourceMethod;
import net.jbock.processor.SourceElement;
import net.jbock.validate.ParameterStyle;

import javax.lang.model.util.Types;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Module
public class ConvertModule {

    private final AnnotationUtil annotationUtil = new AnnotationUtil();

    private final TypeTool tool;
    private final SourceElement sourceElement;
    private final Types types;
    private final SafeElements elements;

    public ConvertModule(
            TypeTool tool,
            Types types,
            SourceElement sourceElement,
            SafeElements elements) {
        this.tool = tool;
        this.sourceElement = sourceElement;
        this.elements = elements;
        this.types = types;
    }

    @ConvertScope
    @Provides
    List<Matcher> matchers(
            OptionalMatcher optionalMatcher,
            ListMatcher listMatcher,
            ExactMatcher exactMatcher) {
        return List.of(optionalMatcher, listMatcher, exactMatcher);
    }

    @ConvertScope
    @Provides
    TypeTool tool() {
        return tool;
    }

    @ConvertScope
    @Provides
    Types types() {
        return types;
    }

    @ConvertScope
    @Provides
    SafeElements elements() {
        return elements;
    }

    @ConvertScope
    @Provides
    SourceElement sourceElement() {
        return sourceElement;
    }

    @ConvertScope
    @Provides
    ConverterClass converter(SourceMethod sourceMethod) {
        return new ConverterClass(annotationUtil.getConverter(sourceMethod.method()));
    }

    @ConvertScope
    @Provides
    Util util(TypeTool tool) {
        return new Util(types, tool);
    }

    @ConvertScope
    @Provides
    ParameterStyle parameterStyle(SourceMethod sourceMethod) {
        return sourceMethod.style();
    }
}
