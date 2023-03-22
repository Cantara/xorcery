package com.exoreaction.xorcery.binarytree;

import com.exoreaction.xorcery.binarytree.neo4j.NodeFactorySingleton;
import no.cantara.binarytree.BinaryTree;
import no.cantara.binarytree.Node;
import no.cantara.binarytree.NodeFactory;

public class TestTreeWithValues implements BinaryTree {

  static final Long[] PRE_ORDER_VALUES = {3L, 1L, 13L, 5L, 6L, 10L, 11L, 16L, 15L, 9L, 4L, 2L};
  static final Long[] POST_ORDER_VALUES = {13L, 6L, 5L, 1L, 11L, 9L, 4L, 15L, 2L, 16L, 10L, 3L};
  static final Long[] IN_ORDER_VALUES = {13L, 1L, 6L, 5L, 3L, 11L, 10L, 9L, 15L, 4L, 16L, 2L};
  static final Long[] REVERSE_IN_ORDER_VALUES = {2L, 16L, 4L, 15L, 9L, 10L, 11L, 3L, 5L, 6L, 1L, 13L};
  static final Long[] LEVEL_ORDER_VALUES = {3L, 1L, 10L, 13L, 5L, 11L, 16L, 6L, 15L, 2L, 9L, 4L};

  @Override
  public Node getRoot() {
    NodeFactory factory = NodeFactorySingleton.instance();
    Node ROOT = factory.createNode(3);
    ROOT.left(factory.createNode(1));
    ROOT.left().left(factory.createNode(13));
    ROOT.left().right(factory.createNode(5));
    ROOT.left().right().left(factory.createNode(6));
    ROOT.right(factory.createNode(10));
    ROOT.right().left(factory.createNode(11));
    ROOT.right().right(factory.createNode(16));
    ROOT.right().right().left(factory.createNode(15));
    ROOT.right().right().left().left(factory.createNode(9));
    ROOT.right().right().left().right(factory.createNode(4));
    ROOT.right().right().right(factory.createNode(2));
    return ROOT;
  }
}
