package org.galaxyproject.gxformat2;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * @author gluu
 * @since 2020-03-03
 */
public class CytoscapeDAG {
  @JsonProperty("nodes")
  private List<Node> nodes = null;

  @JsonProperty("edges")
  private List<Edge> edges = null;

  @JsonProperty("nodes")
  public List<Node> getNodes() {
    return nodes;
  }

  @JsonProperty("nodes")
  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  @JsonProperty("edges")
  public List<Edge> getEdges() {
    return edges;
  }

  @JsonProperty("edges")
  public void setEdges(List<Edge> edges) {
    this.edges = edges;
  }

  public static class Node {
    @JsonProperty("data")
    private Node.NodeData data;

    @JsonProperty("data")
    public NodeData getData() {
      return data;
    }

    @JsonProperty("data")
    public void setData(Node.NodeData data) {
      this.data = data;
    }

    public static class NodeData {

      @JsonProperty("name")
      private String name;

      @JsonProperty("run")
      private String run;

      @JsonProperty("id")
      private String id;

      @JsonProperty("docker")
      private String docker;

      @JsonProperty("name")
      public String getName() {
        return name;
      }

      @JsonProperty("name")
      public void setName(String name) {
        this.name = name;
      }

      @JsonProperty("run")
      public String getRun() {
        return run;
      }

      @JsonProperty("run")
      public void setRun(String run) {
        this.run = run;
      }

      @JsonProperty("id")
      public String getId() {
        return id;
      }

      @JsonProperty("id")
      public void setId(String id) {
        this.id = id;
      }

      @JsonProperty("docker")
      public String getDocker() {
        return docker;
      }

      @JsonProperty("docker")
      public void setDocker(String docker) {
        this.docker = docker;
      }
    }
  }

  public static class Edge {
    @JsonProperty("data")
    private EdgeData data;

    @JsonProperty("data")
    public EdgeData getData() {
      return data;
    }

    @JsonProperty("data")
    public void setData(EdgeData data) {
      this.data = data;
    }

    public static class EdgeData {
      @JsonProperty("id")
      private String id;

      @JsonProperty("source")
      private String source;

      @JsonProperty("target")
      private String target;

      @JsonProperty("id")
      public String getId() {
        return id;
      }

      @JsonProperty("id")
      public void setId(String id) {
        this.id = id;
      }

      @JsonProperty("source")
      public String getSource() {
        return source;
      }

      @JsonProperty("source")
      public void setSource(String source) {
        this.source = source;
      }

      @JsonProperty("target")
      public String getTarget() {
        return target;
      }

      @JsonProperty("target")
      public void setTarget(String target) {
        this.target = target;
      }
    }
  }
}
