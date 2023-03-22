package com.exoreaction.xorcery.binarytree;

import no.cantara.binarytree.Node;
import no.cantara.binarytree.NodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class TestNodeVisitor implements NodeVisitor {

  private List<Long> dataList = new ArrayList<>();

  @Override
  public void visit(Node node) {
    dataList.add(node.data());
  }

  public List<Long> getDataList() {
    return dataList;
  }
}
