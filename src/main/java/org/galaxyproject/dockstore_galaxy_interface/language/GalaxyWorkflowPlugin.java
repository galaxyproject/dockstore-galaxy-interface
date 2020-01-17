package org.galaxyproject.dockstore_galaxy_interface.language;

import com.google.common.collect.Sets;
import io.dockstore.common.DescriptorLanguage;
import io.dockstore.common.VersionTypeValidation;
import io.dockstore.language.CompleteLanguageInterface;
import io.dockstore.language.RecommendedLanguageInterface;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.galaxyproject.gxformat2.Cytoscape;
import org.galaxyproject.gxformat2.Lint;
import org.galaxyproject.gxformat2.LintContext;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/** @author jmchilton */
public class GalaxyWorkflowPlugin extends Plugin {
  public static final Logger LOG = LoggerFactory.getLogger(GalaxyWorkflowPlugin.class);
  public static final String[] TEST_SUFFIXES = {"-tests", "_tests", "-test", "-tests"};
  public static final String[] TEST_EXTENSIONS = {".yml", ".yaml", ".json"};

  /**
   * Constructor to be used by plugin manager for plugin instantiation. Your plugins have to provide
   * constructor with this exact signature to be successfully loaded by manager.
   *
   * @param wrapper
   */
  public GalaxyWorkflowPlugin(PluginWrapper wrapper) {
    super(wrapper);
  }

  @Extension
  public static class GalaxyWorkflowPluginImpl implements CompleteLanguageInterface {

    @Override
    public String launchInstructions(String trsID) {
      return null;
    }

    @Override
    public List<Map<String, Object>> loadCytoscapeElements(
        String initialPath,
        String contents,
        Map<String, Pair<String, GenericFileType>> indexedFiles) {
      final Map<String, Object> workflow = loadWorkflow(contents);
      return Cytoscape.getElements(workflow);
    }

    @Override
    public List<RowData> generateToolsTable(
        String initialPath,
        String contents,
        Map<String, Pair<String, GenericFileType>> indexedFiles) {
      throw new UnsupportedOperationException();
    }

    @Override
    public VersionTypeValidation validateWorkflowSet(
        String initialPath,
        String contents,
        Map<String, Pair<String, GenericFileType>> indexedFiles) {
      final LintContext lintContext = Lint.lint(loadWorkflow(contents));
      final boolean valid;
      valid = !lintContext.getFoundErrors();
      final Map<String, String> messagesAsMap = new HashMap<>();
      final List<String> validationMessages = lintContext.collectMessages();
      final StringBuilder builder = new StringBuilder();
      if (validationMessages.size() == 1) {
        builder.append(validationMessages.get(0));
      } else if (validationMessages.size() > 1) {
        for (final String validationMessage : validationMessages) {
          builder.append("- ").append(validationMessage).append("\n");
        }
      }
      final String validationMessageMerged = builder.toString();
      if (validationMessageMerged.length() > 0) {
        messagesAsMap.put(initialPath, validationMessageMerged);
      }
      final VersionTypeValidation validation = new VersionTypeValidation(valid, messagesAsMap);
      return validation;
    }

    @Override
    public VersionTypeValidation validateTestParameterSet(
        Map<String, Pair<String, GenericFileType>> indexedFiles) {
      return new VersionTypeValidation(true, new HashMap<>());
    }

    @Override
    public DescriptorLanguage getDescriptorLanguage() {
      return DescriptorLanguage.GXFORMAT2;
    }

    @Override
    public Pattern initialPathPattern() {
      // Why doesn't this seem to be called anywhere?
      return Pattern.compile("/(.*\\.gxwf\\.y[a]?ml|.*\\.ga)");
    }

    @Override
    public Map<String, Pair<String, GenericFileType>> indexWorkflowFiles(
        final String initialPath, final String contents, final FileReader reader) {
      Map<String, Pair<String, GenericFileType>> results = new HashMap<>();
      final Optional<String> testParameterFile = findTestParameterFile(initialPath, reader);
      if (testParameterFile.isPresent()) {
        results.put(
            testParameterFile.get(),
            new ImmutablePair<>(testParameterFile.get(), GenericFileType.TEST_PARAMETER_FILE));
      }
      return results;
    }

    protected Optional<String> findTestParameterFile(
        final String initialPath, final FileReader reader) {
      final int extensionPos = initialPath.lastIndexOf(".");
      final String base = initialPath.substring(0, extensionPos);

      final Path parent = Paths.get(initialPath).getParent();
      // listing files is more rate limit friendly (e.g. GitHub counts each 404 "miss" as an API
      // call,
      // but listing a directory can be free if previously requested/cached)
      final Set<String> filenameset =
          parent == null ? Sets.newHashSet() : Sets.newHashSet(reader.listFiles(parent.toString()));

      for (final String suffix : TEST_SUFFIXES) {
        for (final String extension : TEST_EXTENSIONS) {
          final String testFile = base + suffix + extension;
          if (filenameset.contains(testFile)) {
            return Optional.of(testFile);
          }
        }
      }
      return Optional.empty();
    }

    protected Boolean pathExistsFromReader(final FileReader reader, final String path) {
      try {
        reader.readFile(path);
        return true;
      } catch (Exception e) {
        return false;
      }
    }

    static Map<String, Object> loadWorkflow(final String content) {
      final Yaml yaml = new Yaml();
      final Map map = yaml.loadAs(content, Map.class);
      return (Map<String, Object>) map;
    }

    @Override
    public RecommendedLanguageInterface.WorkflowMetadata parseWorkflowForMetadata(
        String initialPath,
        String content,
        Map<String, Pair<String, GenericFileType>> indexedFiles) {
      RecommendedLanguageInterface.WorkflowMetadata metadata =
          new RecommendedLanguageInterface.WorkflowMetadata();
      if (content != null && !content.isEmpty()) {
        try {
          final Map<String, Object> map = loadWorkflow(content);
          String name = null;
          try {
            name = (String) map.get("name");
          } catch (ClassCastException e) {
            LOG.debug("\"name:\" is malformed");
          }

          // FOLLOWING CODE PULLED FROM cwl handler...
          String label = null;
          try {
            label = (String) map.get("label");
          } catch (ClassCastException e) {
            LOG.debug("\"label:\" is malformed");
          }

          // "doc:" added for CWL 1.0
          String doc = null;
          if (map.containsKey("doc")) {
            Object objectDoc = map.get("doc");
            if (objectDoc instanceof String) {
              doc = (String) objectDoc;
            } else if (objectDoc instanceof Map) {
              Map docMap = (Map) objectDoc;
              if (docMap.containsKey("$include")) {
                String enclosingFile = (String) docMap.get("$include");
                Optional<Pair<String, GenericFileType>> first =
                    indexedFiles.values().stream()
                        .filter(pair -> pair.getLeft().equals(enclosingFile))
                        .findFirst();
                if (first.isPresent()) {
                  // No way to fetch this here...
                  LOG.info(
                      "$include would have this but reader not passed through, not implemented");
                  doc = null;
                }
              }
            } else if (objectDoc instanceof List) {
              // arrays for "doc:" added in CWL 1.1
              List docList = (List) objectDoc;
              doc = String.join(System.getProperty("line.separator"), docList);
            }
          }

          final String finalChoiceForDescription = ObjectUtils.firstNonNull(name, label, doc);
          if (finalChoiceForDescription != null) {
            metadata.setDescription(finalChoiceForDescription);
          } else {
            LOG.info("Description not found!");
          }

        } catch (YAMLException | NullPointerException | ClassCastException ex) {
          String message;
          if (ex.getCause() != null) {
            // seems to be possible to get underlying cause in some cases
            message = ex.getCause().toString();
          } else {
            // in other cases, the above will NullPointer
            message = ex.toString();
          }
          LOG.info("Galaxy Workflow file is malformed " + message);
          // CWL parser gets to put validation information in here,
          // plugin interface doesn't consume the right information though.
          // https://github.com/dockstore/dockstore/blob/develop/dockstore-webservice/src/main/java/io/dockstore/webservice/languages/CWLHandler.java#L139
        }
      }
      return metadata;
    }
  }
}
