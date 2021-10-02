package net.jbock.annotated;

import javax.lang.model.element.Name;
import java.util.List;
import java.util.Map;

final class AnnotatedMethodsBuilder {

    private final Step1 step1;
    private final Map<Name, String> enumNames;

    private AnnotatedMethodsBuilder(Step1 step1, Map<Name, String> enumNames) {
        this.step1 = step1;
        this.enumNames = enumNames;
    }

    static Step1 builder(List<Executable> methods) {
        return new Step1(methods);
    }

    static final class Step1 {

        private final List<Executable> methods;

        private Step1(List<Executable> methods) {
            this.methods = methods;
        }

        List<Executable> methods() {
            return methods;
        }

        AnnotatedMethodsBuilder withEnumNames(Map<Name, String> enumNames) {
            return new AnnotatedMethodsBuilder(this, enumNames);
        }
    }

    Map<Name, String> enumNames() {
        return enumNames;
    }

    List<Executable> methods() {
        return step1.methods;
    }
}
