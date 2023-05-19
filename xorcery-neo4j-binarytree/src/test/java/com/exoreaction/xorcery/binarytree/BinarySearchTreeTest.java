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

import no.cantara.binarytree.BinarySearchTree;
import no.cantara.binarytree.Node;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

import static com.exoreaction.xorcery.binarytree.BinaryTreeAssert.assertThatTree;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

abstract class BinarySearchTreeTest {

  private static final int TEST_TREE_MIN_SIZE = 1;
  private static final int TEST_TREE_MAX_SIZE = 1000;

  @RepeatedTest(10)
  void insertingKeysShouldCreateAValidBSTWithKeysInOrder() {
    List<Long> keysOrdered = createOrderedSequenceOfKeys();

    var tree = createBST();
    insertKeysInRandomOrder(tree, keysOrdered);

    assertThatTree(tree) //
        .isValid()
        .hasKeysInGivenOrder(keysOrdered);

    assertSpecificTreeInvariants(tree);
  }

  @RepeatedTest(10)
  void shouldThrowExceptionWhenInsertingExistingKey() {
    List<Long> keysOrdered = createOrderedSequenceOfKeys();

    var tree = createBST();
    insertKeysInRandomOrder(tree, keysOrdered);

    long randomKey = pickRandomKey(keysOrdered);
    assertThrows(IllegalArgumentException.class, () -> tree.insertNode(randomKey));
  }

  @RepeatedTest(10)
  void searchFindsAllKeys() {
    List<Long> keysOrdered = createOrderedSequenceOfKeys();

    var tree = createBST();
    insertKeysInRandomOrder(tree, keysOrdered);

    for (Long key : keysOrdered) {
      Node node = tree.searchNode(key);
      assertThat(node.data(), is(key));
    }
  }

  @RepeatedTest(10)
  void searchReturnsNullWhenKeyNotFound() {
    List<Long> keysOrdered = createOrderedSequenceOfKeys();

    var tree = createBST();
    insertKeysInRandomOrder(tree, keysOrdered);

    Long highestKey = keysOrdered.get(keysOrdered.size() - 1);
    MatcherAssert.assertThat(tree.searchNode(highestKey + 1), is(nullValue()));
  }

  @RepeatedTest(3)
  void deleteNodeShouldLeaveAValidBSTWithoutTheDeletedNode() {
    List<Long> keysOrdered = createOrderedSequenceOfKeys();

    var tree = createBST();
    insertKeysInRandomOrder(tree, keysOrdered);

    // Remove every key... and after each key check if the BST is valid
    List<Long> keysToDelete = shuffle(keysOrdered);
    List<Long> keysRemaining = new ArrayList<>(keysOrdered);

    for (Long keyToDelete : keysToDelete) {
      tree.deleteNode(keyToDelete);

      keysRemaining.remove(keyToDelete);

      assertThatTree(tree) //
          .isValid()
          .hasKeysInGivenOrder(keysRemaining);

      assertSpecificTreeInvariants(tree);
    }
  }

  @RepeatedTest(10)
  void deleteNotExistingKeyShouldNotChangeTheBST() {
    List<Long> keysOrdered = createOrderedSequenceOfKeys();
    Long highestKey = keysOrdered.get(keysOrdered.size() - 1);

    var tree = createBST();
    insertKeysInRandomOrder(tree, keysOrdered);

    tree.deleteNode(highestKey + 1);

    assertThatTree(tree) //
        .isValid()
        .hasKeysInGivenOrder(keysOrdered);

    assertSpecificTreeInvariants(tree);
  }

  /**
   * Override this in tests for specific trees, e.g. AVL trees or red-black trees.
   *
   * @param tree the tree
   */
  protected void assertSpecificTreeInvariants(BinarySearchTree tree) {}

  protected abstract BinarySearchTree createBST();

  private List<Long> createOrderedSequenceOfKeys() {
    int size = ThreadLocalRandom.current().nextInt(TEST_TREE_MIN_SIZE, TEST_TREE_MAX_SIZE);
    return LongStream.range(0, size).boxed().toList();
  }

  private void insertKeysInRandomOrder(BinarySearchTree tree, List<Long> keysOrdered) {
    List<Long> keys = shuffle(keysOrdered);
    for (Long key : keys) {
      tree.insertNode(key);
    }
  }

  private List<Long> shuffle(List<Long> keysOrdered) {
    List<Long> keys = new ArrayList<>(keysOrdered);
    Collections.shuffle(keys);
    return Collections.unmodifiableList(keys);
  }

  private long pickRandomKey(List<Long> keysOrdered) {
    int randomIndex = ThreadLocalRandom.current().nextInt(keysOrdered.size());
    return keysOrdered.get(randomIndex);
  }
}
