package com.exoreaction.xorcery.binarytree;

import no.cantara.binarytree.BaseBinaryTree;
import no.cantara.binarytree.Node;

public class TestTree extends BaseBinaryTree {

  public TestTree(Node root) {
    this.root = root;
  }

  public static TestTree emptyTree() {
    return new TestTree(null);
  }
}
