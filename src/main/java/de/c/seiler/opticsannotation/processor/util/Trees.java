package de.c.seiler.opticsannotation.processor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Trees
{
  public static <T> Tree<T> unit(T v)
  {
    return new Tree<>(v);
  }

  public static <T> Tree<T> join(Tree<Tree<T>> tt)
  {
    if (tt.getLeaves().isEmpty())
      return tt.getValue();
    Tree<T> tv = tt.getValue();
    List<Tree<T>> nl = new ArrayList<>(tv.getLeaves());
    List<Tree<T>> tl = tt.getLeaves().stream().map(Trees::join).collect(Collectors.toList());
    nl.addAll(tl);
    return new Tree<>(tv.getValue(), nl);
  }

  public static <T, R> Tree<R> bind(Tree<T> t, Function<T, Tree<R>> f)
  {
    return join(t.map(f));
  }

  public static <T, U> U reduce(
      Tree<T> tree, U identity,
      BiFunction<U, T, U> accumulator,
      BinaryOperator<U> combiner)
  {
    U result = tree.getLeaves().stream()
        .flatMap(tr -> Stream.of(reduce(tr, identity, accumulator, combiner)))
        .reduce(combiner).orElse(identity);
    result = accumulator.apply(result, tree.getValue());

    return result;
  }

  public static <T> List<List<T>> treePaths(Tree<T> tree)
  {
    List<List<T>> result = treePathInner(Collections.emptyList(), tree);
    return result;
  }

  private static <T> List<List<T>> treePathInner(List<T> pre, Tree<T> tree)
  {
    ArrayList<T> ppre = new ArrayList<>(pre);
    ppre.add(tree.getValue());
    List<List<T>> result = new ArrayList<>();
    result.add(ppre);
    tree.getLeaves().forEach(t -> {
      result.addAll(treePathInner(ppre, t));
    });
    return result;
  }

}
