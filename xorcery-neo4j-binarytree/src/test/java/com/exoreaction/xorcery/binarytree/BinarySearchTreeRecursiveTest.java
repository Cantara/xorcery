package com.exoreaction.xorcery.binarytree;

import com.exoreaction.xorcery.binarytree.neo4j.NodeFactorySingleton;
import no.cantara.binarytree.BinarySearchTree;
import no.cantara.binarytree.BinarySearchTreeRecursive;

class BinarySearchTreeRecursiveTest extends BinarySearchTreeTest {

  @Override
  protected BinarySearchTree createBST() {
    return new BinarySearchTreeRecursive(NodeFactorySingleton.instance());
  }
}
