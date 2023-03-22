package com.exoreaction.xorcery.binarytree;

import no.cantara.binarytree.BinaryTree;
import no.cantara.binarytree.DepthFirstTraversal;
import no.cantara.binarytree.DepthFirstTraversalIterative;

class DepthFirstTraversalIterativeTest extends DepthFirstTraversalTest {

  @Override
  DepthFirstTraversal getTraversal(BinaryTree tree) {
    return new DepthFirstTraversalIterative(tree);
  }
}
