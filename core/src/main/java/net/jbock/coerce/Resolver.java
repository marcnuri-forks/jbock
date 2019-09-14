package net.jbock.coerce;

import net.jbock.compiler.HierarchyUtil;
import net.jbock.compiler.TypeTool;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.jbock.compiler.TypeTool.asDeclared;

class Resolver {

  private final TypeTool tool;

  // visible for testing
  Resolver(TypeTool tool) {
    this.tool = tool;
  }

  /**
   * Check if {@code x} is a {@code something}, and also infer
   * the typevars in {@code something} where possible.
   *
   * @param x a type
   * @param something what we're hoping {@code x} is
   * @param tool a tool
   *
   * @return the {@code something} type, with typevars resolved
   */
  static Optional<TypeMirror> typecheck(TypeElement x, Class<?> something, TypeTool tool) {
    List<TypeElement> hierarchy = new HierarchyUtil(tool).getHierarchy(x);
    TypeMirror currentGoal = tool.erasure(something);
    List<ImplementsRelation> path = new ArrayList<>();
    Resolver resolver = new Resolver(tool);
    ImplementsRelation relation;
    while ((relation = resolver.findRelation(hierarchy, currentGoal)) != null) {
      path.add(relation);
      currentGoal = tool.erasure(relation.dog);
    }
    if (path.isEmpty()) {
      return Optional.empty();
    }
    Collections.reverse(path);
    return Optional.of(resolver.dogToAnimal(path));
  }

  private ImplementsRelation findRelation(List<TypeElement> hierarchy, TypeMirror something) {
    for (TypeElement type : hierarchy) {
      ImplementsRelation implementsRelation = findRelation(type, something);
      if (implementsRelation != null) {
        return implementsRelation;
      }
    }
    return null;
  }

  private ImplementsRelation findRelation(TypeElement type, TypeMirror something) {
    for (TypeMirror mirror : type.getInterfaces()) {
      if (tool.isSameType(something, tool.erasure(mirror))) {
        return new ImplementsRelation(type, mirror);
      }
    }
    return null;
  }

  /**
   * Go from dog to animal, successively replacing type parameters on the way.
   * The end result is a type that has the same erasure as the last segment's animal,
   * and uses the same type parameter names as the first segment.
   *
   * @param path a path of implements relations
   * @return a type that has the same erasure as the final animal
   *
   * <ul>
   *   <li>ascending from dog to animal</li>
   *   <li>{@code path[1].dog} and {@code path[0].animal} have the same erasure</li>
   * </ul>
   */
  private TypeMirror dogToAnimal(List<ImplementsRelation> path) {
    TypeMirror animal = path.get(0).animal;
    for (int i = 1; i < path.size(); i++) {
      animal = dogToAnimal(animal, path.get(i));
    }
    return animal;
  }

  /**
   * @param animal a declared type
   * @param relation a relation the dog of which is the {@code animal}
   * @return a type that has the erasure of {@code relation.animal}
   */
  TypeMirror dogToAnimal(TypeMirror animal, ImplementsRelation relation) {
    List<? extends TypeMirror> typeArguments = asDeclared(animal).getTypeArguments();
    List<? extends TypeParameterElement> typeParameters = relation.dog.getTypeParameters();
    Map<String, TypeMirror> solution = new HashMap<>();
    for (int i = 0; i < typeParameters.size(); i++) {
      solution.put(typeParameters.get(i).toString(), typeArguments.get(i));
    }
    TypeMirror result = tool.substitute(relation.animal, solution);
    if (result == null) {
      throw new AssertionError("Bad input");
    }
    return result;
  }
}
