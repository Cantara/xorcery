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
import no.cantara.binarytree.BreadthFirstTraversal;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

class BreadthFirstTraversalTest {

  @Test
  void traverseLevelOrder_sampleTree_traversesTreeInLevelOrder() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    new BreadthFirstTraversal(new TestTreeWithValues()).traverseLevelOrder(visitor);
    assertThat(visitor.getDataList(), contains(TestTreeWithValues.LEVEL_ORDER_VALUES));
  }

  @Test
  void traverseLevelOrder_emptyTree_traversesNoElement() {
    TestNodeVisitor visitor = new TestNodeVisitor();
    new BreadthFirstTraversal(emptyTree()).traverseLevelOrder(visitor);
    assertThat(visitor.getDataList(), is(empty()));
  }

  private BinaryTree emptyTree() {
    return () -> null;
  }
}
