package com.exoreaction.xorcery.binarytree;

import com.exoreaction.xorcery.binarytree.neo4j.NodeFactorySingleton;
import no.cantara.binarytree.Node;
import no.cantara.binarytree.SimpleBinaryTree;
import no.cantara.binarytree.SimpleBinaryTree.Side;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleBinaryTreeTest {

  @Test
  void insertRoot_rootIsNull_rootIsSetToNewNodeWithGivenValue() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);

    assertThat(root, equalTo(tree.getRoot()));

    assertThat(root.data(), is(3L));
    assertThat(root.left(), is(nullValue()));
    assertThat(root.right(), is(nullValue()));
    assertThat(root.parent(), is(nullValue()));
  }

  @Test
  void insertRoot_rootIsAlreadySet_throwsException() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    tree.insertRoot(3);
    assertThrows(IllegalStateException.class, () -> tree.insertRoot(5));
  }

  @Test
  void insertNode_parentIsNull_throwsException() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    assertThrows(NullPointerException.class, () -> tree.insertNode(1, null, Side.LEFT));
  }

  @Test
  void insertNode_leftUnderEmptyRoot_newNodeIsLeftUnderRootAndParentIsSet() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);
    Node node = tree.insertNode(1, root, Side.LEFT);

    assertThat(root.left(), equalTo(node));
    assertThat(root.right(), is(nullValue()));

    assertThat(node.data(), is(1L));
    assertThat(node.left(), is(nullValue()));
    assertThat(node.right(), is(nullValue()));
    assertThat(node.parent(), equalTo(root));
  }

  @Test
  void insertNode_leftUnderFullRoot_newNodeIsLeftUnderRootAndPreviousLeftIsLeftUnderNewNode() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);
    Node node1 = tree.insertNode(1, root, Side.LEFT);
    Node node10 = tree.insertNode(10, root, Side.RIGHT);

    Node node = tree.insertNode(2, root, Side.LEFT);

    assertThat(root.left(), equalTo(node));
    assertThat(root.right(), equalTo(node10));

    assertThat(node.data(), is(2L));
    assertThat(node.left(), equalTo(node1));
    assertThat(node.right(), is(nullValue()));
    assertThat(node.parent(), equalTo(root));

    assertThat(node1.parent(), equalTo(node));
  }

  @Test
  void insertNode_rightUnderEmptyRoot_newNodeIsRightUnderRootAndParentIsSet() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);
    Node node = tree.insertNode(10, root, Side.RIGHT);

    assertThat(root.left(), is(nullValue()));
    assertThat(root.right(), equalTo(node));

    assertThat(node.data(), is(10L));
    assertThat(node.left(), is(nullValue()));
    assertThat(node.right(), is(nullValue()));
    assertThat(node.parent(), equalTo(root));
  }

  @Test
  void insertNode_rightUnderFullRoot_newNodeIsRightUnderRootAndPreviousRightIsRightUnderNewNode() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);
    Node node1 = tree.insertNode(1, root, Side.LEFT);
    Node node10 = tree.insertNode(10, root, Side.RIGHT);

    Node node = tree.insertNode(15, root, Side.RIGHT);

    assertThat(root.left(), equalTo(node1));
    assertThat(root.right(), equalTo(node));

    assertThat(node.data(), is(15L));
    assertThat(node.left(), is(nullValue()));
    assertThat(node.right(), equalTo(node10));
    assertThat(node.parent(), equalTo(root));

    assertThat(node10.parent(), equalTo(node));
  }

  @Test
  void deleteNode_leaf_leafIsRemoved() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);
    Node node1 = tree.insertNode(1, root, Side.LEFT);
    Node node10 = tree.insertNode(10, root, Side.RIGHT);

    tree.deleteNode(node10);

    assertThat(root.left(), equalTo(node1));
    assertThat(root.right(), is(nullValue()));
  }

  @Test
  void deleteNode_nonRootNodeWithoutParent_throwsException() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);
    Node node1 = tree.insertNode(1, root, Side.LEFT);
    Node node10 = tree.insertNode(10, root, Side.RIGHT);
    node10.parent(null);

    assertThrows(IllegalStateException.class, () -> tree.deleteNode(node10));
  }

  @Test
  void deleteNode_nodeWithLeftChildOnly_nodeIsReplacedByChild() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);
    Node node1 = tree.insertNode(1, root, Side.LEFT);
    Node node10 = tree.insertNode(10, root, Side.RIGHT);
    Node node8 = tree.insertNode(8, node10, Side.LEFT);
    Node node7 = tree.insertNode(7, node8, Side.LEFT);
    Node node9 = tree.insertNode(9, node8, Side.RIGHT);

    tree.deleteNode(node10);

    assertThat(root.left(), equalTo(node1));
    assertThat(root.right(), equalTo(node8));
    assertThat(node8.parent(), equalTo(root));
  }

  @Test
  void deleteNode_nodeWithRightChildOnly_nodeIsReplacedByChild() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);
    Node node1 = tree.insertNode(1, root, Side.LEFT);
    Node node10 = tree.insertNode(10, root, Side.RIGHT);
    Node node12 = tree.insertNode(12, node10, Side.RIGHT);
    Node node11 = tree.insertNode(11, node12, Side.LEFT);
    Node node13 = tree.insertNode(13, node12, Side.RIGHT);

    tree.deleteNode(node10);

    assertThat(root.left(), equalTo(node1));
    assertThat(root.right(), equalTo(node12));
    assertThat(node12.parent(), equalTo(root));
  }

  @Test
  void
      deleteNode_rightChildNodeWithTwoChildren_nodeIsReplacedByLeftSubtreeAndRightSubtreeIsAppendedToRightmostNodeOfLeftSubtree() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);
    Node node1 = tree.insertNode(1, root, Side.LEFT);
    Node node10 = tree.insertNode(10, root, Side.RIGHT);

    // left subtree of 10
    Node node8 = tree.insertNode(8, node10, Side.LEFT);
    Node node7 = tree.insertNode(7, node8, Side.LEFT);
    Node node9 = tree.insertNode(9, node8, Side.RIGHT);

    // right subtree of 10
    Node node12 = tree.insertNode(12, node10, Side.RIGHT);
    Node node11 = tree.insertNode(11, node12, Side.LEFT);
    Node node13 = tree.insertNode(13, node12, Side.RIGHT);

    tree.deleteNode(node10);

    assertThat(root.left(), equalTo(node1));
    assertThat(root.right(), equalTo(node8));

    assertThat(node8.parent(), equalTo(root));
    assertThat(node9.right(), equalTo(node12));
    assertThat(node12.parent(), equalTo(node9));
  }

  @Test
  void
      deleteNode_leftChildNodeWithTwoChildren_nodeIsReplacedByLeftSubtreeAndRightSubtreeIsAppendedToRightmostNodeOfLeftSubtree() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(15);
    Node node10 = tree.insertNode(10, root, Side.LEFT);
    Node node20 = tree.insertNode(20, root, Side.RIGHT);

    // left subtree of 10
    Node node8 = tree.insertNode(8, node10, Side.LEFT);
    Node node7 = tree.insertNode(7, node8, Side.LEFT);
    Node node9 = tree.insertNode(9, node8, Side.RIGHT);

    // right subtree of 10
    Node node12 = tree.insertNode(12, node10, Side.RIGHT);
    Node node11 = tree.insertNode(11, node12, Side.LEFT);
    Node node13 = tree.insertNode(13, node12, Side.RIGHT);

    tree.deleteNode(node10);

    assertThat(root.left(), equalTo(node8));
    assertThat(root.right(), equalTo(node20));

    assertThat(node8.parent(), equalTo(root));
    assertThat(node9.right(), equalTo(node12));
    assertThat(node12.parent(), equalTo(node9));
  }

  @Test
  void
      deleteNode_rootWithTwoChildren_nodeIsReplacedByLeftSubtreeAndRightSubtreeIsAppendedToRightmostNodeOfLeftSubtree() {
    SimpleBinaryTree tree = new SimpleBinaryTree(NodeFactorySingleton.instance());
    Node root = tree.insertRoot(3);
    Node node1 = tree.insertNode(1, root, Side.LEFT);
    Node node10 = tree.insertNode(10, root, Side.RIGHT);

    tree.deleteNode(root);

    assertThat(tree.getRoot(), equalTo(node1));
    assertThat(node1.parent(), is(nullValue()));

    assertThat(tree.getRoot().right(), equalTo(node10));
    assertThat(node10.parent(), equalTo(tree.getRoot()));
  }
}
