package org.galaxyproject.gxformat2;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

public class CytoscapeTest {
    @Test
    public void testGetElements() throws Exception {
        Gson gson = new Gson();
        File testFile = new File("src/test/resources/transcriptomics-denovo-workflow.ga");
        Map<String, Object> elements = Cytoscape.getElements(testFile.getAbsolutePath());
        String json = gson.toJson(elements);
        CytoscapeDAG cytoscapeDAG = gson.fromJson(json, CytoscapeDAG.class);
        Set<String> collect = cytoscapeDAG.getNodes().stream().map(node -> node.getData().getId()).collect(Collectors.toSet());
        Assert.assertEquals("Ids are distinct", collect.size(), cytoscapeDAG.getNodes().size());
        Assert.assertTrue(collect.stream().anyMatch(thing -> thing.equals("UniqueBeginKey")));
        Assert.assertTrue(collect.stream().anyMatch(thing -> thing.equals("UniqueEndKey")));
        cytoscapeDAG.getEdges().stream().map(CytoscapeDAG.Edge::getData)
                .forEach(data -> Assert.assertNotEquals("Source and target should be different", data.getSource(), data.getTarget()));
        Assert.assertFalse(elements.isEmpty());
      List<String> startingSteps = cytoscapeDAG.getEdges().stream().filter(edge -> edge.getData().getSource().equals("UniqueBeginKey")).map(edge -> edge.getData().getTarget())
              .collect(Collectors.toList());
      List<String> endingSteps = cytoscapeDAG.getEdges().stream().filter(edge -> edge.getData().getTarget().equals("UniqueEndKey")).map(edge -> edge.getData().getSource())
              .collect(Collectors.toList());
      List<String> knownStartingSteps = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8");
      List<String> knownEndingSteps = Arrays.asList("46", "45", "36", "35", "33", "32", "30", "29", "27", "26", "19", "18", "16", "15", "13", "12", "10", "9");
      Collections.sort(knownStartingSteps);
      Collections.sort(knownEndingSteps);
      Collections.sort(startingSteps);
      Collections.sort(endingSteps);
      Assert.assertEquals(knownStartingSteps, startingSteps);
      Assert.assertEquals(knownEndingSteps, endingSteps);
    }
}
