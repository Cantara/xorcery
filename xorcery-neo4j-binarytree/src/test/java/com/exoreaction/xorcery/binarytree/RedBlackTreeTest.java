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

import com.exoreaction.xorcery.binarytree.neo4j.NodeFactorySingleton;
import no.cantara.binarytree.BinarySearchTree;
import no.cantara.binarytree.Node;
import no.cantara.binarytree.RedBlackTree;

import static com.exoreaction.xorcery.binarytree.BinaryTreeAssert.assertThatTree;
import static no.cantara.binarytree.Node.BLACK;
import static no.cantara.binarytree.Node.RED;

class RedBlackTreeTest extends BinarySearchTreeTest {

  @Override
  protected BinarySearchTree createBST() {
    return new RedBlackTree(NodeFactorySingleton.instance());
  }

  @Override
  protected void assertSpecificTreeInvariants(BinarySearchTree tree) {
    assertThatTree(tree).hasAllParentsSetCorrectly();

    Node root = tree.getRoot();
    if (root != null) {
      // Check rule 2?
      // if (root.color == RED) {
      //   throw new AssertionError("Root is red");
      // }
      validateRedBlackInvariant(null, root, 0, new MutableValueContainer());
    }
  }

  private void validateRedBlackInvariant(
      Node parent, Node node, int blackHeightThisPath, MutableValueContainer blackHeightFirstPath) {
    // NIL node reached?
    if (node == null) {
      // We're not counting the NIL nodes on our path. That makes each path 1 black node shorter.
      // That makes no difference in comparing the black-heights of every path.

      // First NIL node?
      if (blackHeightFirstPath.value == null) {
        blackHeightFirstPath.value = blackHeightThisPath;
      } else if (blackHeightFirstPath.value != blackHeightThisPath) {
        throw new AssertionError(
            "Black-height rule violated (blackHeightFirstPath.value = "
                + blackHeightFirstPath.value
                + "; blackHeightThisPath = "
                + blackHeightThisPath
                + ")");
      }
      return;
    }

    // Count black nodes on path
    if (node.color() == BLACK) {
      blackHeightThisPath++;
    }

    // Red node must not have a red parent
    else if (parent != null && parent.color() == RED) {
      throw new AssertionError(
          "Node " + node.data() + " and its parent " + parent.data() + " are both red");
    }

    // We're using the simplified approach of not forcing the root to be black
    validateRedBlackInvariant(node, node.left(), blackHeightThisPath, blackHeightFirstPath);
    validateRedBlackInvariant(node, node.right(), blackHeightThisPath, blackHeightFirstPath);
  }

  private static class MutableValueContainer {
    private Integer value;
  }
}
