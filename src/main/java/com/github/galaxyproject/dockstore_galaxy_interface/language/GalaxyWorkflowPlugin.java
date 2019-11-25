package com.github.galaxyproject.dockstore_galaxy_interface.language;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import io.dockstore.common.VersionTypeValidation;
import io.dockstore.language.RecommendedLanguageInterface;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * @author jmchilton
 */
public class GalaxyWorkflowPlugin implements RecommendedLanguageInterface {
    public static final Logger LOG = LoggerFactory.getLogger(GalaxyWorkflowPlugin.class);
    public static final String[] TEST_SUFFIXES = {"-tests", "_tests", "-test", "-tests"};
    public static final String[] TEST_EXTENSIONS = {".yml", ".yaml", ".json"};

    @Override
    public String launchInstructions(String trsID) {
        return null;
    }

    @Override
    public VersionTypeValidation validateWorkflowSet(String initialPath, String contents,
        Map<String, Pair<String, GenericFileType>> indexedFiles) {
        final VersionTypeValidation validation = new VersionTypeValidation(true, new HashMap<>());
        // TODO: fill this in
        return validation;
    }

    @Override
    public VersionTypeValidation validateTestParameterSet(Map<String, Pair<String, GenericFileType>> indexedFiles) {
        return new VersionTypeValidation(true, new HashMap<>());
    }

    @Override
    public Pattern initialPathPattern() {
        // Why doesn't this seem to be called anywhere?
        return Pattern.compile("/.*\\.gxwf\\.y[a]?ml");
    }

    @Override
    public Map<String, Pair<String, GenericFileType>> indexWorkflowFiles(final String initialPath, final String contents, final FileReader reader) {
        Map<String, Pair<String, GenericFileType>> results = new HashMap<>();
        final Optional<String> testParameterFile = findTestParameterFile(initialPath, reader);
        if(testParameterFile.isPresent()) {
            results.put(testParameterFile.get(), new ImmutablePair<>(testParameterFile.get(), GenericFileType.TEST_PARAMETER_FILE));
        }
        return results;
    }

    protected Optional<String> findTestParameterFile(final String initialPath, final FileReader reader) {
        final int extensionPos = initialPath.lastIndexOf(".");
        final String base = initialPath.substring(0, extensionPos);
        for(final String suffix : TEST_SUFFIXES)  {
            for(final String extension : TEST_EXTENSIONS) {
                final String testFile = base + suffix + extension;
                if(pathExistsFromReader(reader, testFile)) {
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
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    public RecommendedLanguageInterface.WorkflowMetadata parseWorkflowForMetadata(String initialPath, String content,
        Map<String, Pair<String, GenericFileType>> indexedFiles) {
        RecommendedLanguageInterface.WorkflowMetadata metadata = new RecommendedLanguageInterface.WorkflowMetadata();
        if (content != null && !content.isEmpty()) {
            try {
                Yaml yaml = new Yaml();
                Map map = yaml.loadAs(content, Map.class);

                String name = null;
                try {
                    name = (String)map.get("name");
                } catch (ClassCastException e) {
                    LOG.debug("\"name:\" is malformed");
                }

                // FOLLOWING CODE PULLED FROM cwl handler...
                String label = null;
                try {
                    label = (String)map.get("label");
                } catch (ClassCastException e) {
                    LOG.debug("\"label:\" is malformed");
                }

                // "doc:" added for CWL 1.0
                String doc = null;
                if (map.containsKey("doc")) {
                    Object objectDoc = map.get("doc");
                    if (objectDoc instanceof String) {
                        doc = (String)objectDoc;
                    } else if (objectDoc instanceof Map) {
                        Map docMap = (Map)objectDoc;
                        if (docMap.containsKey("$include")) {
                            String enclosingFile = (String)docMap.get("$include");
                            Optional<Pair<String, GenericFileType>> first = indexedFiles.values().stream().filter(pair -> pair.getLeft().equals(enclosingFile))
                                .findFirst();
                            if (first.isPresent()) {
                                // No way to fetch this here...
                                LOG.info("$include would have this but reader not passed through, not implemented");
                                doc = null;
                            }
                        }
                    } else if (objectDoc instanceof List) {
                        // arrays for "doc:" added in CWL 1.1
                        List docList = (List)objectDoc;
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

