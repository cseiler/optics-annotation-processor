package com.cseiler.lensannotation.processor.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import de.c.seiler.opticsannotation.processor.util.Tree;
import de.c.seiler.opticsannotation.processor.util.Trees;

public class TreeTest
{

  @SuppressWarnings("boxing")
  @Test
  public void testMap()
  {
    String s = "1";
    Tree<String> t = new Tree<>(s, Collections.emptyList());
    Tree<Integer> tt = t.map(v -> Integer.valueOf(v));
    assertNotNull(tt);
    Integer i = tt.getValue();
    assertEquals(Integer.valueOf(s), i);
    Integer v0 = 1;
    Integer v1 = 2;
    Integer v2 = 3;

    Tree<Integer> t0 = Trees.unit(v0);
    Tree<Integer> t1 = Trees.unit(v1);
    Tree<Integer> t2 = Trees.unit(v2);

    List<Tree<Integer>> l = Arrays.asList(t1, t2);

    t0 = t0.withLeaves(l);

    Tree<String> t4 = t0.map(x -> String.valueOf((char) (x.intValue() + 65)));
    System.err.println(t4);
  }

  @Test
  public void testFlatMap()
  {
    String s = "ABC";
    Tree<String> t = new Tree<>(s, Collections.emptyList());
    Tree<String> tt = t.flatMap(x -> new Tree<String>(x, Collections.singletonList(Trees.unit("Z"))));
    System.err.println(tt);
  }

  @Test
  public void testUnit()
  {
    String s = "A";
    Tree<String> t = Trees.unit(s);
    assertNotNull(t);
    assertTrue(t.getLeaves().isEmpty());
    assertEquals(s, t.getValue());
  }

  @Test
  public void testJoin()
  {
    String s = "ABC";
    Tree<String> t = new Tree<>(s, Collections.emptyList());
    Tree<Tree<String>> tt = t.map(v -> {
      char[] ca = v.toCharArray();
      List<String> ls = new ArrayList<>();
      for (char c : ca)
        ls.add("" + c);
      List<Tree<String>> leaves = ls.stream().map(Trees::unit).map(tr -> {
        List<Tree<String>> l2 = new ArrayList<>();
        l2.add(Trees.unit(String.valueOf(tr.getValue().length())));
        l2.add(Trees.unit("Z"));
        return tr.withLeaves(l2);
      }).collect(Collectors.toList());
      return new Tree<>(v, leaves);
    });
    System.err.println(tt);
    Tree<String> j = Trees.join(tt);
    System.err.println(j);
  }

  @SuppressWarnings("boxing")
  @Test
  public void testReduce()
  {
    Integer v0 = 1;
    Integer v1 = 2;
    Integer v2 = 3;

    Tree<Integer> t0 = Trees.unit(v0);
    Tree<Integer> t1 = Trees.unit(v1);
    Tree<Integer> t2 = Trees.unit(v2);

    List<Tree<Integer>> l = Arrays.asList(t1, t2);

    t0 = t0.withLeaves(l);

    System.err.println(t0);
    Integer sum = t0.reduce(Integer.valueOf(0), (a, x) -> a + x, (a0, a1) -> a0 + a1);
    assertEquals(Integer.valueOf(6), sum);
    System.err.println(sum);
  }

  @SuppressWarnings("boxing")
  @Test
  public void testPaths()
  {
    Integer v0 = 1;
    Integer v1 = 2;
    Integer v2 = 3;

    Tree<Integer> t0 = Trees.unit(v0);
    Tree<Integer> t1 = Trees.unit(v1);
    Tree<Integer> t2 = Trees.unit(v2);

    List<Tree<Integer>> l = Arrays.asList(t1, t2);

    t0 = t0.withLeaves(l);

    System.err.println(t0);
    List<List<Integer>> paths = Trees.treePaths(t0);
    for (List<Integer> list : paths)
    {
      System.err.println();
      list.forEach(System.err::println);
    }
  }

  @SuppressWarnings("boxing")
  @Test
  public void testPaths3()
  {
    Integer v0 = 1;
    Integer v1 = 2;

    Tree<Integer> t0 = Trees.unit(v0);
    Tree<Integer> t1 = Trees.unit(v1);

    List<Tree<Integer>> l = Arrays.asList(t1);

    t0 = t0.withLeaves(l);

    System.err.println(t0);
    List<List<Integer>> paths = Trees.treePaths(t0);
    for (List<Integer> list : paths)
    {
      System.err.println();
      list.forEach(System.err::println);
    }
  }

}
