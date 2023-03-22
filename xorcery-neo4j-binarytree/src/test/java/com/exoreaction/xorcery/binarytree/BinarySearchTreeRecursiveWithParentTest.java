package com.exoreaction.xorcery.binarytree;

import com.exoreaction.xorcery.binarytree.neo4j.NodeFactorySingleton;
import no.cantara.binarytree.BinarySearchTree;
import no.cantara.binarytree.BinarySearchTreeRecursiveWithParent;

import static com.exoreaction.xorcery.binarytree.BinaryTreeAssert.assertThatTree;

class BinarySearchTreeRecursiveWithParentTest extends BinarySearchTreeTest {

  @Override
  protected BinarySearchTree createBST() {
    return new BinarySearchTreeRecursiveWithParent(NodeFactorySingleton.instance());
  }

  @Override
  protected void assertSpecificTreeInvariants(BinarySearchTree tree) {
    assertThatTree(tree).hasAllParentsSetCorrectly();
  }
}
