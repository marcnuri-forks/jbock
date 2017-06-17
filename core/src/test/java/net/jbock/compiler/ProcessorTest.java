package net.jbock.compiler;

import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static java.util.Collections.singletonList;

public class ProcessorTest {

  @Test
  public void process() throws Exception {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.jbock.CommandLineArguments;",
        "import net.jbock.LongName;",
        "import java.util.Optional;",
        "class JJob {",
        "  @CommandLineArguments JJob(@LongName(\"x\") Optional<String> a, Optional<String> b) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.JJobParser", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .compilesWithoutError();
  }

  @Test
  public void duplicateName() throws Exception {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.jbock.CommandLineArguments;",
        "import net.jbock.LongName;",
        "import java.util.Optional;",
        "class JJob {",
        "  @CommandLineArguments JJob(@LongName(\"x\") Optional<String> a, @LongName(\"x\") Optional<String> b) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.JJobParser", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Duplicate longName: x");
  }

  @Test
  public void wrongType() throws Exception {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.jbock.CommandLineArguments;",
        "import net.jbock.LongName;",
        "class JJob {",
        "  @CommandLineArguments JJob(@LongName(\"x\") int a) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.JJobParser", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Only Optional<String>, List<String> and boolean allowed, " +
            "but parameter a has type int");
  }

  @Test
  public void privateException() throws Exception {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.jbock.CommandLineArguments;",
        "import net.jbock.LongName;",
        "import java.util.Optional;",
        "class JJob {",
        "  @CommandLineArguments JJob(String a) throws Hammer {}",
        "  private static final class Hammer extends Exception {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.JJobParser", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Class 'Hammer' may not be private");
  }

  @Test
  public void whitespace() throws Exception {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.jbock.CommandLineArguments;",
        "import net.jbock.LongName;",
        "import java.util.Optional;",
        "class JJob {",
        "  @CommandLineArguments JJob(@LongName(\"a b c\") Optional<String> a) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.JJobParser", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("The name may not contain whitespace characters");
  }

  @Test
  public void booleanWrapper() throws Exception {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.jbock.CommandLineArguments;",
        "import net.jbock.LongName;",
        "class JJob {",
        "  @CommandLineArguments JJob(@LongName(\"a\") Boolean a) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.JJobParser", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("Only Optional<String>, List<String> and boolean allowed, " +
            "but parameter a has type java.lang.Boolean");
  }

  @Test
  public void otherTokensAndEverythingAfter() throws Exception {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.jbock.CommandLineArguments;",
        "import net.jbock.EverythingAfter;",
        "import net.jbock.OtherTokens;",
        "import java.util.List;",
        "class JJob {",
        "  @CommandLineArguments JJob(@OtherTokens @EverythingAfter(\"--\") List<String> a) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.JJobParser", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("@OtherTokens and @EverythingAfter cannot be on the same parameter");
  }

  @Test
  public void everythingAfterCollidesWithOption() throws Exception {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.jbock.CommandLineArguments;",
        "import net.jbock.EverythingAfter;",
        "import java.util.List;",
        "import java.util.Optional;",
        "class JJob {",
        "  @CommandLineArguments JJob(Optional<String> a, @EverythingAfter(\"--a\") List<String> b) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.JJobParser", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new Processor())
        .failsToCompile()
        .withErrorContaining("@EverythingAfter coincides with a long option");
  }
}