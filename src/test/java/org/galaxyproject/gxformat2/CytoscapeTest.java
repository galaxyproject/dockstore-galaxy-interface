package org.galaxyproject.gxformat2;

import static org.galaxyproject.gxformat2.Cytoscape.END_ID;
import static org.galaxyproject.gxformat2.Cytoscape.START_ID;

import com.google.gson.Gson;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class CytoscapeTest {
  public static final Gson gson = new Gson();

  /**
   * Test with steps that don't have labels
   *
   * @throws Exception Cytoscape exception
   */
  @Test
  public void testGetElementsWithoutLabels() throws Exception {
    File testFile = new File("src/test/resources/transcriptomics-denovo-workflow.ga");
    Map<String, Object> elements = Cytoscape.getElements(testFile.getAbsolutePath());
    String json = gson.toJson(elements);
    Assert.assertFalse(elements.isEmpty());
    List<String> knownStartingSteps = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8");
    List<String> knownEndingSteps =
        Arrays.asList(
            "46", "45", "36", "35", "33", "32", "30", "29", "27", "26", "19", "18", "16", "15",
            "13", "12", "10", "9");
    generalTest(knownStartingSteps, knownEndingSteps, json);
  }

  /**
   * Test with steps that have labels
   *
   * @throws Exception Cytoscape exception
   */
  @Test
  public void testGetElementsWithLabels() throws Exception {
    File testFile = new File("src/test/resources/anotherFile.ga");
    Map<String, Object> elements = Cytoscape.getElements(testFile.getAbsolutePath());
    String json = gson.toJson(elements);
    List<String> knownStartingSteps = new java.util.ArrayList<>(Collections.singletonList("0"));
    List<String> knownEndingSteps = new java.util.ArrayList<>(Collections.singletonList("1"));
    generalTest(knownStartingSteps, knownEndingSteps, json);
  }

  @Test
  public void testExample1() throws Exception {
    File testFile =
        new File(
            "src/test/resources/jmchilton/galaxy-workflow-dockstore-example-1/Dockstore.gxwf.yml");
    Map<String, Object> elements = Cytoscape.getElements(testFile.getAbsolutePath());
    String json = gson.toJson(elements);
    List<String> knownStartingSteps =
        new java.util.ArrayList<>(Collections.singletonList("input1"));
    List<String> knownEndingSteps =
        new java.util.ArrayList<>(Collections.singletonList("first_cat"));
    generalTest(knownStartingSteps, knownEndingSteps, json);
  }

  @Test
  public void testExample2() throws Exception {
    File testFile =
        new File("src/test/resources/jmchilton/galaxy-workflow-dockstore-example-2/Dockstore.ga");
    Map<String, Object> elements = Cytoscape.getElements(testFile.getAbsolutePath());
    String json = gson.toJson(elements);
    List<String> knownStartingSteps = new java.util.ArrayList<>(Collections.singletonList("0"));
    List<String> knownEndingSteps = new java.util.ArrayList<>(Collections.singletonList("1"));
    generalTest(knownStartingSteps, knownEndingSteps, json);
  }

  public void generalTest(
      List<String> knownStartingSteps, List<String> knownEndingSteps, String json) {
    CytoscapeDAG cytoscapeDAG = gson.fromJson(json, CytoscapeDAG.class);
    List<CytoscapeDAG.Node> nodes = cytoscapeDAG.getNodes();
    Set<String> collect =
        nodes.stream().map(node -> node.getData().getId()).collect(Collectors.toSet());
    Assert.assertEquals("Ids are distinct", collect.size(), nodes.size());
    List<CytoscapeDAG.Edge> edges = cytoscapeDAG.getEdges();
    Assert.assertTrue("A graph has at least n - 1 edges", edges.size() >= nodes.size() - 1);
    Assert.assertTrue(collect.stream().anyMatch(thing -> thing.equals(START_ID)));
    Assert.assertTrue(collect.stream().anyMatch(thing -> thing.equals(END_ID)));
    edges.stream()
        .map(CytoscapeDAG.Edge::getData)
        .forEach(
            data ->
                Assert.assertNotEquals(
                    "Source and target should be different", data.getSource(), data.getTarget()));
    List<String> startingSteps =
        edges.stream()
            .filter(edge -> edge.getData().getSource().equals(START_ID))
            .map(edge -> edge.getData().getTarget())
            .collect(Collectors.toList());
    List<String> endingSteps =
        edges.stream()
            .filter(edge -> edge.getData().getTarget().equals(END_ID))
            .map(edge -> edge.getData().getSource())
            .collect(Collectors.toList());
    Collections.sort(knownStartingSteps);
    Collections.sort(knownEndingSteps);
    Collections.sort(startingSteps);
    Collections.sort(endingSteps);
    Assert.assertEquals(knownStartingSteps, startingSteps);
    Assert.assertEquals(knownEndingSteps, endingSteps);
  }
}
