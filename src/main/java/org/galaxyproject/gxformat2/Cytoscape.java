package org.galaxyproject.gxformat2;

import static org.galaxyproject.dockstore_galaxy_interface.language.GalaxyWorkflowPlugin.LOG;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * General notes: There's no guarantee that a normalized step has a label, step definition ID or
 * state
 */
public class Cytoscape {
  public static final ObjectMapper objectMapper = new ObjectMapper();
  public static final String MAIN_TS_PREFIX = "toolshed.g2.bx.psu.edu/repos/";
  public static final String START_ID = "UniqueBeginKey";
  public static final String END_ID = "UniqueEndKey";

  public static Map<String, Object> getElements(final String path) throws Exception {
    final Map<String, Object> object = (Map<String, Object>) IoUtils.readYamlFromPath(path);
    return getElements(object);
  }

  private static class IdAndLabel {
    private String id;
    private String label;

    IdAndLabel(String id, String label) {
      this.setId(id);
      this.setLabel(label);
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }
  }

  public static Map<String, Object> getElements(final Map<String, Object> object) {
    final String wfClass = (String) object.get("class");
    WorkflowAdapter adapter;
    if (wfClass == null) {
      adapter = new NativeWorkflowAdapter(object);
    } else {
      adapter = new Format2WorkflowAdapter(object);
    }
    final Map<String, Object> elements = new HashMap<>();
    final List<Object> nodeElements = new ArrayList<>();
    final List<Object> edgeElements = new ArrayList<>();
    elements.put("nodes", nodeElements);
    elements.put("edges", edgeElements);
    nodeElements.add(createStartNode());
    nodeElements.add(createEndNode());
    List<WorkflowAdapter.NormalizedStep> normalizedSteps = adapter.normalizedSteps();
    // Step definition ID is not really a perfect identifier because it may not exist, using label
    // as an identifier otherwise
    // TODO: Need to create another field that actually uniquely identifies the step (but still able
    // to map to input connections and state
    normalizedSteps.forEach(
        normalizedStep -> {
          if (normalizedStep.stepDefinition.get("id") == null) {
            normalizedStep.stepDefinition.put("id", normalizedStep.label);
          }
        });
    Set<String> endNodeIds =
        normalizedSteps.stream()
            .map(normalizedStep -> (normalizedStep.stepDefinition.get("id")).toString())
            .collect(Collectors.toSet());
    Set<IdAndLabel> allNodesIdsAndLabels =
        normalizedSteps.stream()
            .map(
                normalizedStep -> {
                  Map<String, Object> stepDefinition = normalizedStep.stepDefinition;
                  String id = (stepDefinition.get("id")).toString();
                  String label =
                      stepDefinition.get("label") != null
                          ? stepDefinition.get("label").toString()
                          : null;
                  return new IdAndLabel(id, label);
                })
            .collect(Collectors.toSet());
    for (final WorkflowAdapter.NormalizedStep normalizedStep : normalizedSteps) {
      Map<String, Object> stepDefinition = normalizedStep.stepDefinition;
      final Map<String, Object> step = stepDefinition;
      String stepId = (step.get("id")).toString();
      String stepType = step.get("type") != null ? (String) step.get("type") : "tool";
      List<String> classes = new ArrayList<>(Collections.singletonList("type_" + stepType));
      if (stepType.equals("tool") || stepType.equals("subworkflow")) {
        classes.add("runnable");
      } else {
        classes.add("input");
      }

      // It's not an end step if there's another step with a state that includes the node
      Object state = normalizedStep.stepDefinition.get("state");
      if (state != null) {
        LinkedHashMap linkedHashMapstate = (LinkedHashMap) state;
        linkedHashMapstate.keySet().forEach(key -> endNodeIds.remove(key));
      }
      String toolId = (String) step.get("tool_id");
      if (toolId != null && toolId.startsWith(MAIN_TS_PREFIX)) {
        toolId = toolId.substring(MAIN_TS_PREFIX.length());
      }
      String label = normalizedStep.label;
      if ((label == null || isOrderIndexLabel(label)) && toolId != null) {
        label = "tool:" + toolId;
      }
      final String repoLink;
      if (step.containsKey("tool_shed_repository")) {
        final Map<String, String> repo = (Map<String, String>) step.get("tool_shed_repository");
        repoLink =
            "https://"
                + repo.get("tool_shed")
                + "/view/"
                + repo.get("owner")
                + "/"
                + repo.get("name")
                + "/"
                + repo.get("changeset_revision");
      } else {
        repoLink = null;
      }
      final Map<String, Object> nodeData = new HashMap<>();
      nodeData.put("id", stepId);
      nodeData.put("label", label);
      // dockstore displays name, docker, type, tool, and run
      nodeData.put("name", label);
      // TODO: detect Docker image properly
      nodeData.put("docker", "TBD");
      nodeData.put("run", "TBD");

      nodeData.put("tool_id", step.get("tool_id"));
      nodeData.put("doc", normalizedStep.doc);
      nodeData.put("repo_link", repoLink);
      nodeData.put("step_type", stepType);
      nodeData.put("type", stepType);
      final Map<String, Object> nodeElement = new HashMap<>();
      nodeElement.put("group", "nodes");
      nodeElement.put("data", nodeData);
      nodeElement.put("classes", classes);
      nodeElement.put("position", elementPosition(step));
      nodeElements.add(nodeElement);

      // Create edge from start node if there are:
      // 1. no input connections or empty input connection
      // 2. no inputs
      // 3. no state (questionable)
      Object inputConnections = stepDefinition.get("input_connections");
      if (state == null
          && (normalizedStep.inputs.isEmpty()
              && (inputConnections == null || inputConnections.toString().equals("{}")))) {
        edgeElements.add(createEdge(START_ID, stepId));
      }
      if (state != null) {
        LinkedHashMap linkedHashMapstate = (LinkedHashMap) state;
        Set<String> keySet = linkedHashMapstate.keySet();
        for (String key : keySet) {
          createEdges(key, stepId, allNodesIdsAndLabels, edgeElements, endNodeIds, key, stepId);
        }
      }

      for (final WorkflowAdapter.Input input : normalizedStep.inputs) {
        createEdges(
            input.sourceStepLabel,
            stepId,
            allNodesIdsAndLabels,
            edgeElements,
            endNodeIds,
            input.inputName,
            input.sourceOutputName);
      }
    }
    // Create edges for end nodes to direct to the real end node
    endNodeIds.forEach(endNodeId -> edgeElements.add(createEdge(endNodeId, END_ID)));
    return elements;
  }

  private static void createEdges(
      String sourceStepLabel,
      String stepId,
      Set<IdAndLabel> allNodesIdsAndLabels,
      final List<Object> edgeElements,
      Set<String> endNodeIds,
      String inputName,
      String outputName) {
    final String edgeId = stepId + "__to__" + sourceStepLabel;
    final Map<String, Object> edgeData = new HashMap<>();
    edgeData.put("id", edgeId);
    // Look up what the step ID is based on sourceStepLabel which is either the step ID itself
    // or the step label
    Optional<IdAndLabel> sourceId =
        allNodesIdsAndLabels.stream()
            .filter(
                partialStep -> {
                  boolean isLabel = sourceStepLabel.equals(partialStep.getLabel());
                  boolean isId = sourceStepLabel.equals(partialStep.getId());
                  return isLabel || isId;
                })
            .findFirst();
    if (sourceId.isPresent()) {
      edgeData.put("source", sourceId.get().id);
      // Any node that's a source of an edge is not an end node
      endNodeIds.remove(sourceId.get().id);
      edgeData.put("target", stepId);
      edgeData.put("input", inputName);
      edgeData.put("output", outputName);
      final Map<String, Object> edgeElement = new HashMap<>();
      edgeElement.put("group", "edges");
      edgeElement.put("data", edgeData);
      edgeElements.add(edgeElement);
    } else {
      String errorMessage =
          String.format("Could not find input \"%s\" from the workflow steps.", sourceStepLabel);
      LOG.error(errorMessage);
    }
  }

  static boolean isOrderIndexLabel(final String label) {
    try {
      Integer.parseInt(label);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static CytoscapeDAG.Edge createEdge(String source, String target) {
    String id = source + "__to__" + target;
    CytoscapeDAG.Edge edge = new CytoscapeDAG.Edge();
    CytoscapeDAG.Edge.EdgeData edgeData = new CytoscapeDAG.Edge.EdgeData();
    edgeData.setId(id);
    edgeData.setSource(source);
    edgeData.setTarget(target);
    edge.setData(edgeData);
    return edge;
  }

  private static Map<String, Long> elementPosition(final Map<String, Object> step) {
    Map<String, Object> stepPosition = (Map<String, Object>) step.get("position");
    Map<String, Long> elementPosition = new HashMap();
    elementPosition.put("x", getIntegerValue(stepPosition, "left"));
    elementPosition.put("y", getIntegerValue(stepPosition, "top"));
    return elementPosition;
  }

  private static Long getIntegerValue(final Map<String, Object> fromMap, final String key) {
    final Object value = fromMap.get(key);
    if (value instanceof Float || value instanceof Double) {
      return (long) Math.floor((double) value);
    } else if (value instanceof Integer) {
      return ((Integer) value).longValue();
    } else {
      return (long) value;
    }
  }

  private static Map<String, Object> createStartNode() {
    return createStartOrEndNode(START_ID);
  }

  private static Map<String, Object> createEndNode() {
    return createStartOrEndNode(END_ID);
  }

  private static Map<String, Object> createStartOrEndNode(String id) {
    CytoscapeDAG.Node.NodeData nodeData = new CytoscapeDAG.Node.NodeData();
    nodeData.setId(id);
    nodeData.setName(id);
    CytoscapeDAG.Node node = new CytoscapeDAG.Node();
    node.setData(nodeData);
    return objectMapper.convertValue(node, Map.class);
  }
}
