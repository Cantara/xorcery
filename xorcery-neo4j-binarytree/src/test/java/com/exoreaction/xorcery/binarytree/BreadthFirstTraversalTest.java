package com.exoreaction.xorcery.binarytree;

import no.cantara.binarytree.BinaryTree;
import no.cantara.binarytree.BreadthFirstTraversal;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

class BreadthFirstTraversalTest {

  @Test
  void traverseLevelOrder_sampleTree_traversesTreeInLevelOrder() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    new BreadthFirstTraversal(new TestTreeWithValues()).traverseLevelOrder(visitor);
    assertThat(visitor.getDataList(), contains(TestTreeWithValues.LEVEL_ORDER_VALUES));
  }

  @Test
  void traverseLevelOrder_emptyTree_traversesNoElement() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    new BreadthFirstTraversal(emptyTree()).traverseLevelOrder(visitor);
    assertThat(visitor.getDataList(), is(empty()));
  }

  private BinaryTree emptyTree() {
    return () -> null;
  }
}
