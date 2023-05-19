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

import no.cantara.binarytree.BinaryTree;
import no.cantara.binarytree.DepthFirstTraversal;
import org.junit.jupiter.api.Test;

import static com.exoreaction.xorcery.binarytree.TestTree.emptyTree;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public abstract class DepthFirstTraversalTest {

  @Test
  void traversePreOrder_sampleTree_traversesTreeInPreOrder() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    getTraversal(new TestTreeWithValues()).traversePreOrder(visitor);
    assertThat(visitor.getDataList(), contains(TestTreeWithValues.PRE_ORDER_VALUES));
  }

  @Test
  void traversePreOrder_emptyTree_traversesNoElement() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    getTraversal(emptyTree()).traversePreOrder(visitor);
    assertThat(visitor.getDataList(), is(empty()));
  }

  @Test
  void traversePostOrder_sampleTree_traversesTreeInPostOrder() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    getTraversal(new TestTreeWithValues()).traversePostOrder(visitor);
    assertThat(visitor.getDataList(), contains(TestTreeWithValues.POST_ORDER_VALUES));
  }

  @Test
  void traversePostOrder_emptyTree_traversesNoElement() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    getTraversal(emptyTree()).traversePostOrder(visitor);
    assertThat(visitor.getDataList(), is(empty()));
  }

  @Test
  void traverseInOrder_sampleTree_traversesTreeInOrder() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    getTraversal(new TestTreeWithValues()).traverseInOrder(visitor);
    assertThat(visitor.getDataList(), contains(TestTreeWithValues.IN_ORDER_VALUES));
  }

  @Test
  void traverseInOrder_emptyTree_traversesNoElement() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    getTraversal(emptyTree()).traverseInOrder(visitor);
    assertThat(visitor.getDataList(), is(empty()));
  }

  @Test
  void traverseReverseInOrder_sampleTree_traversesTreeInReverseOrder() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    getTraversal(new TestTreeWithValues()).traverseReverseInOrder(visitor);
    assertThat(visitor.getDataList(), contains(TestTreeWithValues.REVERSE_IN_ORDER_VALUES));
  }

  @Test
  void traverseReverseInOrder_emptyTree_traversesNoElement() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    getTraversal(emptyTree()).traverseReverseInOrder(visitor);
    assertThat(visitor.getDataList(), is(empty()));
  }

  abstract DepthFirstTraversal getTraversal(BinaryTree tree);
}
