package de.c.seiler.opticsannotation.processor.util;

import static de.c.seiler.opticsannotation.processor.util.Trees.bind;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
@AllArgsConstructor
public class Tree<T>
{
  T value;
  @NonNull
  List<Tree<T>> leaves;

  
  public Tree(T value) 
  {
    this(value, Collections.emptyList());
  }
  
  public <R> Tree<R> map(Function<T, R> f)
  {
    return new Tree<>(
        f.apply(value),
        leaves.stream().map(tr -> tr.map(f)).collect(Collectors.toList()));
  }

  public <R> Tree<R> flatMap(Function<T, Tree<R>> f)
  {
    return bind(this, f);
  }

  public <U> U reduce(U identity,
      BiFunction<U, T, U> accumulator,
      BinaryOperator<U> combiner)
  {
    return Trees.reduce(this, identity, accumulator, combiner);
  }
}
