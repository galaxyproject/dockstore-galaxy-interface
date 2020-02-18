package org.galaxyproject.dockstore_galaxy_interface.language;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.dockstore.common.VersionTypeValidation;
import io.dockstore.language.CompleteLanguageInterface;
import io.dockstore.language.MinimalLanguageInterface;
import io.dockstore.language.RecommendedLanguageInterface;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

public class GalaxyWorkflowLanguagePluginTest {
  public static final String REPO_ID_1 = "jmchilton/galaxy-workflow-dockstore-example-1";
  public static final String REPO_FORMAT_2 = "https://raw.githubusercontent.com/" + REPO_ID_1;
  public static final String REPO_ID_2 = "jmchilton/galaxy-workflow-dockstore-example-2";
  public static final String REPO_NATIVE = "https://raw.githubusercontent.com/" + REPO_ID_2;
  public static final String EXAMPLE_FILENAME_1 = "Dockstore.gxwf.yml";
  public static final String EXAMPLE_FILENAME_1_PATH = "/" + EXAMPLE_FILENAME_1;
  public static final String EXAMPLE_FILENAME_2 = "Dockstore.gxwf.yaml";
  public static final String EXAMPLE_FILENAME_2_PATH = "/" + EXAMPLE_FILENAME_2;

  public static final String EXAMPLE_FILENAME_NATIVE = "Dockstore.ga";
  public static final String EXAMPLE_FILENAME_NATIVE_PATH = "/" + EXAMPLE_FILENAME_NATIVE;
  public static final String CURRENT_BRANCH = "master";

  @Test
  public void testFormat2WorkflowParsing() {
    final GalaxyWorkflowPlugin.GalaxyWorkflowPluginImpl plugin =
        new GalaxyWorkflowPlugin.GalaxyWorkflowPluginImpl();
    final HttpFileReader reader = new HttpFileReader(REPO_FORMAT_2);
    final String initialPath = EXAMPLE_FILENAME_1_PATH;
    final String contents = reader.readFile(EXAMPLE_FILENAME_1);
    final Map<String, Pair<String, MinimalLanguageInterface.GenericFileType>> fileMap =
        plugin.indexWorkflowFiles(initialPath, contents, reader);
    Assert.assertEquals(1, fileMap.size());
    final Pair<String, MinimalLanguageInterface.GenericFileType> discoveredFile =
        fileMap.get("/Dockstore.gxwf-test.yml");
    Assert.assertEquals(
        discoveredFile.getRight(), MinimalLanguageInterface.GenericFileType.TEST_PARAMETER_FILE);
    final RecommendedLanguageInterface.WorkflowMetadata metadata =
        plugin.parseWorkflowForMetadata(initialPath, contents, fileMap);
    // We don't track these currently, but we could pull out the CWL parsing and mimic that.
    Assert.assertNull(metadata.getAuthor());
    Assert.assertNull(metadata.getEmail());
    // We have name and annotation - not sure if this should just be "<name>"", or "<name>.
    // <annotation>", or
    // "<name>/n<annotation>".
    Assert.assertEquals("Test Workflow", metadata.getDescription());

    // Test validation stubs...
    final VersionTypeValidation wfValidation =
        plugin.validateWorkflowSet(initialPath, contents, fileMap);
    Assert.assertTrue(wfValidation.isValid());
    final VersionTypeValidation testValidation = plugin.validateTestParameterSet(fileMap);
    Assert.assertTrue(testValidation.isValid());
    // No validation messages because everything is fine...
    Assert.assertTrue(wfValidation.getMessage().isEmpty());

    final Map<String, Object> cytoscapeElements =
        plugin.loadCytoscapeElements(initialPath, contents, fileMap);
    // do a sanity check for a valid cytoscape JSON
    // http://manual.cytoscape.org/en/stable/Supported_Network_File_Formats.html#cytoscape-js-json
    Assert.assertTrue(
        cytoscapeElements.containsKey("nodes") && cytoscapeElements.containsKey("edges"));
    final List<CompleteLanguageInterface.RowData> rowData =
        plugin.generateToolsTable(initialPath, contents, fileMap);
    Assert.assertFalse(rowData.isEmpty());
  }

  @Test
  public void testNativeWorkflowParsing() {
    final GalaxyWorkflowPlugin.GalaxyWorkflowPluginImpl plugin =
        new GalaxyWorkflowPlugin.GalaxyWorkflowPluginImpl();
    final HttpFileReader reader = new HttpFileReader(REPO_NATIVE);
    final String initialPath = EXAMPLE_FILENAME_NATIVE_PATH;
    final String contents = reader.readFile(EXAMPLE_FILENAME_NATIVE);
    final Map<String, Pair<String, MinimalLanguageInterface.GenericFileType>> fileMap =
        plugin.indexWorkflowFiles(initialPath, contents, reader);
    Assert.assertEquals(1, fileMap.size());
    final Pair<String, MinimalLanguageInterface.GenericFileType> discoveredFile =
        fileMap.get("/Dockstore-test.yml");
    Assert.assertEquals(
        discoveredFile.getRight(), MinimalLanguageInterface.GenericFileType.TEST_PARAMETER_FILE);
    final RecommendedLanguageInterface.WorkflowMetadata metadata =
        plugin.parseWorkflowForMetadata(initialPath, contents, fileMap);

    // We don't track these currently - especially with native format.
    Assert.assertNull(metadata.getAuthor());
    Assert.assertNull(metadata.getEmail());
    // We have name and annotation - not sure if this should just be "<name>"", or "<name>.
    // <annotation>", or
    // "<name>/n<annotation>".
    Assert.assertEquals("Test Workflow", metadata.getDescription());

    final VersionTypeValidation wfValidation =
        plugin.validateWorkflowSet(initialPath, contents, fileMap);
    Assert.assertTrue(wfValidation.isValid());
    // No validation messages because everything is fine...
    Assert.assertTrue(wfValidation.getMessage().isEmpty());

    final Map<String, Object> cytoscapeElements =
        plugin.loadCytoscapeElements(initialPath, contents, fileMap);
    // do a sanity check for a valid cytoscape JSON
    // http://manual.cytoscape.org/en/stable/Supported_Network_File_Formats.html#cytoscape-js-json
    Assert.assertTrue(
        cytoscapeElements.containsKey("nodes") && cytoscapeElements.containsKey("edges"));

    final List<CompleteLanguageInterface.RowData> rowData =
        plugin.generateToolsTable(initialPath, contents, fileMap);
    Assert.assertFalse(rowData.isEmpty());
  }

  @Test
  public void testValidationIssues() {
    final GalaxyWorkflowPlugin.GalaxyWorkflowPluginImpl plugin =
        new GalaxyWorkflowPlugin.GalaxyWorkflowPluginImpl();
    final ResourceFileReader reader = new ResourceFileReader("invalid_report_ga");
    final String initialPath = "missing_markdown.ga";
    final String contents = reader.readFile(initialPath);
    final Map<String, Pair<String, MinimalLanguageInterface.GenericFileType>> fileMap =
        plugin.indexWorkflowFiles(initialPath, contents, reader);
    Assert.assertEquals(0, fileMap.size());
    final VersionTypeValidation wfValidation =
        plugin.validateWorkflowSet(initialPath, contents, fileMap);
    Assert.assertFalse(wfValidation.isValid());
    final Map<String, String> messages = wfValidation.getMessage();
    Assert.assertTrue(messages.containsKey(initialPath));
    final String validationProblem = messages.get(initialPath);
    Assert.assertTrue(validationProblem.indexOf("markdown") > 0);
  }

  @Test
  public void testTwoValidationIssues() {
    final GalaxyWorkflowPlugin.GalaxyWorkflowPluginImpl plugin =
        new GalaxyWorkflowPlugin.GalaxyWorkflowPluginImpl();
    final ResourceFileReader reader = new ResourceFileReader("invalid_report_ga");
    final String initialPath = "two_validation_errors.ga";
    final String contents = reader.readFile(initialPath);
    final Map<String, Pair<String, MinimalLanguageInterface.GenericFileType>> fileMap =
        plugin.indexWorkflowFiles(initialPath, contents, reader);
    Assert.assertEquals(0, fileMap.size());
    final VersionTypeValidation wfValidation =
        plugin.validateWorkflowSet(initialPath, contents, fileMap);
    Assert.assertFalse(wfValidation.isValid());
    final Map<String, String> messages = wfValidation.getMessage();
    final String validationProblem = messages.get(initialPath);
    Assert.assertTrue(validationProblem.contains("- .. ERROR"));
    Assert.assertTrue(validationProblem.contains("- .. WARNING"));
  }

  @Test
  public void testInitialPathPattern() {
    // TODO: This doesn't seem to be called by Dockstore anywhere - is that right?
    final GalaxyWorkflowPlugin.GalaxyWorkflowPluginImpl plugin =
        new GalaxyWorkflowPlugin.GalaxyWorkflowPluginImpl();
    Matcher m = plugin.initialPathPattern().matcher(EXAMPLE_FILENAME_1_PATH);
    Assert.assertTrue("File name matches for initial path pattern", m.matches());
    m = plugin.initialPathPattern().matcher(EXAMPLE_FILENAME_2_PATH);
    Assert.assertTrue("File name matches for initial path pattern", m.matches());
    m = plugin.initialPathPattern().matcher(EXAMPLE_FILENAME_NATIVE_PATH);
    Assert.assertTrue("File name matches for initial path pattern (native workflows)", m.matches());
    m = plugin.initialPathPattern().matcher("/Dockerstore.cwl");
    Assert.assertFalse(m.matches());
    m = plugin.initialPathPattern().matcher("/Dockerstore.nf");
    Assert.assertFalse(m.matches());
  }

  abstract static class URLFileReader implements MinimalLanguageInterface.FileReader {
    // URL to repo
    protected final String repo;
    // extracted ID
    protected final Optional<String> id;

    URLFileReader(final String repo) {
      this.repo = repo;
      final String[] split = repo.split("/");
      if (split.length >= 2) {
        id = Optional.of(split[split.length - 2] + "/" + split[split.length - 1]);
      } else {
        id = Optional.empty();
      }
    }

    protected abstract URL getUrl(final String path) throws IOException;

    @Override
    public String readFile(String path) {
      try {
        if (path.startsWith("/")) {
          path = path.substring(1);
        }
        URL url = this.getUrl(path);
        return Resources.toString(url, StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public List<String> listFiles(String pathToDirectory) {
      if (id.isEmpty()) {
        return new ArrayList<>();
      }
      Gson gson = new GsonBuilder().create();
      try {
        final String fileContent =
            FileUtils.readFileToString(
                new File("src/test/resources/" + this.id.get() + "/listing.json"));
        return gson.fromJson(
            fileContent, TypeToken.getParameterized(List.class, String.class).getType());
      } catch (IOException e) {
        throw new RuntimeException("test failed to read directory listing");
      }
    }
  }

  static class ResourceFileReader extends URLFileReader {

    ResourceFileReader(final String repo) {
      super(repo);
    }

    @Override
    protected URL getUrl(String path) throws IOException {
      final String classPath = "repos/" + this.repo + "/" + path;
      final URL url = GalaxyWorkflowLanguagePluginTest.class.getResource(classPath);
      if (url == null) {
        throw new IOException("No such file " + classPath);
      }
      return url;
    }
  }

  static class HttpFileReader extends URLFileReader {

    HttpFileReader(final String repo) {
      super(repo);
    }

    @Override
    protected URL getUrl(final String path) throws IOException {
      return new URL(this.repo + "/" + CURRENT_BRANCH + "/" + path);
    }
  }
}
