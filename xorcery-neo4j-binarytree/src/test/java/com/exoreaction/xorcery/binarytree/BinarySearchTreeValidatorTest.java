package com.exoreaction.xorcery.binarytree;

import com.exoreaction.xorcery.binarytree.neo4j.NodeFactorySingleton;
import no.cantara.binarytree.BinaryTree;
import no.cantara.binarytree.Node;
import no.cantara.binarytree.NodeFactory;
import org.junit.jupiter.api.Test;

import static no.cantara.binarytree.BinarySearchTreeValidator.isBstWithDuplicates;
import static no.cantara.binarytree.BinarySearchTreeValidator.isBstWithoutDuplicates;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class BinarySearchTreeValidatorTest {

  NodeFactory factory = NodeFactorySingleton.instance();


  @Test
  void shouldReturnTrueForEmptyTree() {
    BinaryTree tree = TestTree.emptyTree();
    assertThat(isBstWithoutDuplicates(tree), is(true));
  }

  @Test
  void shouldReturnTrueForTreeWithOneNode() {
    Node root = factory.createNode(100);
    BinaryTree tree = new TestTree(root);
    assertThat(isBstWithoutDuplicates(tree), is(true));
  }

  @Test
  void shouldReturnTrueForTreeWithNodeAndSmallerLeftChild() {
    Node root = factory.createNode(100);
    root.left(factory.createNode(50));
    BinaryTree tree = new TestTree(root);
    assertThat(isBstWithoutDuplicates(tree), is(true));
  }

  @Test
  void shouldReturnFalseForTreeWithNodeAndGreaterLeftChild() {
    Node root = factory.createNode(100);
    root.left(factory.createNode(110));
    BinaryTree tree = new TestTree(root);
    assertThat(isBstWithoutDuplicates(tree), is(false));
  }

  @Test
  void shouldReturnTrueForTreeWithNodeAndGreaterRightChild() {
    Node root = factory.createNode(100);
    root.right(factory.createNode(110));
    BinaryTree tree = new TestTree(root);
    assertThat(isBstWithoutDuplicates(tree), is(true));
  }

  @Test
  void shouldReturnFalseForTreeWithNodeAndSmallerRightChild() {
    Node root = factory.createNode(100);
    root.right(factory.createNode(90));
    BinaryTree tree = new TestTree(root);
    assertThat(isBstWithoutDuplicates(tree), is(false));
  }

  @Test
  void shouldReturnTrueForComplexValidTree() {
    BinaryTree tree = generateComplexValidTree();

    assertThat(isBstWithoutDuplicates(tree), is(true));
  }

  @Test
  void shouldReturnFalseForComplexInvalidTree1() {
    BinaryTree tree = generateComplexInvalidTree1();

    assertThat(isBstWithoutDuplicates(tree), is(false));
  }

  @Test
  void shouldReturnFalseForComplexInvalidTree2() {
    BinaryTree tree = generateComplexInvalidTree2();

    assertThat(isBstWithoutDuplicates(tree), is(false));
  }

  @Test
  void withDuplicates_shouldReturnTrueForDuplicatesOfRoot() {
    Node root = factory.createNode(100);
    root.left(factory.createNode(100));
    root.right(factory.createNode(100));
    BinaryTree tree = new TestTree(root);

    assertThat(isBstWithDuplicates(tree), is(true));
  }

  @Test
  void withDuplicates_shouldReturnTrueForDuplicateInValidComplexTree() {
    BinaryTree tree = generateComplexValidTree();

    tree.getRoot().left().left().left(factory.createNode(1));
    tree.getRoot().right().right().right().left(factory.createNode(16));

    assertThat(isBstWithDuplicates(tree), is(true));
  }

  @Test
  void withDuplicates_shouldReturnFalseForDuplicatesWithErrorInValidComplexTree() {
    BinaryTree tree = generateComplexValidTree();

    tree.getRoot().left().left().left(factory.createNode(1));
    tree.getRoot().left().left().left().right(factory.createNode(2));

    assertThat(isBstWithDuplicates(tree), is(false));
  }

  @Test
  void withDuplicates_shouldReturnFalseForComplexInvalidTree1() {
    BinaryTree tree = generateComplexInvalidTree1();

    assertThat(isBstWithDuplicates(tree), is(false));
  }

  @Test
  void withDuplicates_shouldReturnFalseForComplexInvalidTree2() {
    BinaryTree tree = generateComplexInvalidTree2();

    assertThat(isBstWithDuplicates(tree), is(false));
  }

  private BinaryTree generateComplexValidTree() {
    Node root = factory.createNode(5);

    root.left(factory.createNode(2));
    root.left().left(factory.createNode(1));
    root.left().right(factory.createNode(4));
    root.left().right().left(factory.createNode(3));

    root.right(factory.createNode(9));
    root.right().left(factory.createNode(6));
    root.right().right(factory.createNode(15));
    root.right().right().left(factory.createNode(11));
    root.right().right().left().left(factory.createNode(10));
    root.right().right().left().right(factory.createNode(13));
    root.right().right().right(factory.createNode(16));

    return new TestTree(root);
  }

  private BinaryTree generateComplexInvalidTree1() {
    Node root = factory.createNode(5);

    root.left(factory.createNode(2));
    root.left().left(factory.createNode(1));
    root.left().right(factory.createNode(4));
    root.left().right().right(factory.createNode(3)); // right instead of left

    root.right(factory.createNode(9));
    root.right().left(factory.createNode(6));
    root.right().right(factory.createNode(15));
    root.right().right().left(factory.createNode(11));
    root.right().right().left().left(factory.createNode(10));
    root.right().right().left().right(factory.createNode(13));
    root.right().right().right(factory.createNode(16));

    return new TestTree(root);
  }

  private BinaryTree generateComplexInvalidTree2() {
    Node root = factory.createNode(5);

    root.left(factory.createNode(2));
    root.left().left(factory.createNode(1));
    root.left().right(factory.createNode(4));
    root.left().right().left(factory.createNode(3));

    root.right(factory.createNode(9));
    root.right().left(factory.createNode(6));
    root.right().right(factory.createNode(15));
    root.right().right().left(factory.createNode(11));
    root.right().right().left().left(factory.createNode(10));
    root.right().right().right(factory.createNode(16));
    root.right().right().right().left(factory.createNode(13));

    return new TestTree(root);
  }
}
