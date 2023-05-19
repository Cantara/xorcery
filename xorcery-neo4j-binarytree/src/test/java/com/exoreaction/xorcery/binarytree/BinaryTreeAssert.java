/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.binarytree;

import no.cantara.binarytree.BinarySearchTreeValidator;
import no.cantara.binarytree.BinaryTree;
import no.cantara.binarytree.DepthFirstTraversalRecursive;
import no.cantara.binarytree.Node;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BinaryTreeAssert {

  private final BinaryTree tree;

  private BinaryTreeAssert(BinaryTree tree) {
    this.tree = tree;
  }

  public static BinaryTreeAssert assertThatTree(BinaryTree tree) {
    return new BinaryTreeAssert(tree);
  }

  public BinaryTreeAssert isValid() {
    if (!BinarySearchTreeValidator.isBstWithoutDuplicates(tree)) {
      throw new AssertionError("Tree is not a valid BST");
    }
    return this;
  }

  public BinaryTreeAssert hasKeysInGivenOrder(List<Long> keys) {
    TestNodeVisitor visitor = new TestNodeVisitor();
    new DepthFirstTraversalRecursive(tree).traverseInOrder(visitor);
    assertThat(visitor.getDataList(), is(keys));
    return this;
  }

  public BinaryTreeAssert hasAllParentsSetCorrectly() {
    hasAllParentsSetCorrectly(null, tree.getRoot());
    return this;
  }

  private void hasAllParentsSetCorrectly(Node parent, Node node) {
    if (node == null) return;

    // Root must not have a parent
    if (node.equals(tree.getRoot())) {
      if (node.parent() != null) {
        throw new AssertionError("Not all parents set correctly: root must not have a parent");
      }
    }

    // All other nodes must have a parent
    else {
      if (node.parent() == null) {
        throw new AssertionError(
            "Not all parents set correctly: node " + node.data() + " has no parent");
      }

      if (!parent.equals(node.parent())) {
        throw new AssertionError(
            "Not all parents set correctly: parent "
                + node.parent().data()
                + " of node "
                + node.data()
                + " isn't the expected parent "
                + parent.data());
      }
    }

    hasAllParentsSetCorrectly(node, node.left());
    hasAllParentsSetCorrectly(node, node.right());
  }
}
