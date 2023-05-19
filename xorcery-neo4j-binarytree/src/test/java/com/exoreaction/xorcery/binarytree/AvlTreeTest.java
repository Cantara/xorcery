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
import no.cantara.binarytree.AvlTree;
import no.cantara.binarytree.BinarySearchTree;
import no.cantara.binarytree.Node;

class AvlTreeTest extends BinarySearchTreeTest {

    @Override
    protected BinarySearchTree createBST() {
        return new AvlTree(NodeFactorySingleton.instance());
    }

    @Override
    protected void assertSpecificTreeInvariants(BinarySearchTree tree) {
        validateAVLInvariant(tree.getRoot());
    }

    private void validateAVLInvariant(Node node) {
        if (node == null) return;

        int leftHeight = node.left() != null ? node.left().height() : -1;
        int rightHeight = node.right() != null ? node.right().height() : -1;

        validateHeight(node, leftHeight, rightHeight);
        validateBalanceFactor(node, leftHeight, rightHeight);

        // Validate AVL invariant for children (recursion)
        validateAVLInvariant(node.left());
        validateAVLInvariant(node.right());
    }

    private void validateHeight(Node node, int leftHeight, int rightHeight) {
        int expectedHeight = 1 + Math.max(leftHeight, rightHeight);
        if (node.height() != expectedHeight) {
            throw new AssertionError(
                    "Height of node %d is %d (expected: %d)"
                            .formatted(node.data(), node.height(), expectedHeight));
        }
    }

    private void validateBalanceFactor(Node node, int leftHeight, int rightHeight) {
        int bf = rightHeight - leftHeight;
        if (bf < -1 || bf > 1) {
            throw new AssertionError(
                    "Balance factor (bf) of node %d is %d (expected: -1 <= bf <= 1)"
                            .formatted(node.data(), bf));
        }
    }
}
