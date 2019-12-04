package org.galaxyproject.dockstore_galaxy_interface.language;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;

import io.dockstore.common.VersionTypeValidation;
import io.dockstore.language.RecommendedLanguageInterface;
import io.dockstore.language.MinimalLanguageInterface;

import com.google.common.io.Resources;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;


public class GalaxyWorkflowLanguagePluginTest {
    public static final String REPO_FORMAT_2 = "https://raw.githubusercontent.com/jmchilton/galaxy-workflow-dockstore-example-1";
    public static final String REPO_NATIVE = "https://raw.githubusercontent.com/jmchilton/galaxy-workflow-dockstore-example-2";
    public static final String EXAMPLE_FILENAME_1 = "Dockstore.gxwf.yml";
    public static final String EXAMPLE_FILENAME_1_PATH = "/" + EXAMPLE_FILENAME_1;
    public static final String EXAMPLE_FILENAME_2 = "Dockstore.gxwf.yaml";
    public static final String EXAMPLE_FILENAME_2_PATH = "/" + EXAMPLE_FILENAME_2;

    public static final String EXAMPLE_FILENAME_NATIVE = "Dockstore.ga";
    public static final String EXAMPLE_FILENAME_NATIVE_PATH = "/" + EXAMPLE_FILENAME_NATIVE;

    @Test
    public void testFormat2WorkflowParsing() {
        final GalaxyWorkflowPlugin plugin = new GalaxyWorkflowPlugin();
        final HttpFileReader reader = new HttpFileReader(REPO_FORMAT_2);
        final String initialPath = EXAMPLE_FILENAME_1_PATH;
        final String contents = reader.readFile(EXAMPLE_FILENAME_1);
        final Map<String, Pair<String, MinimalLanguageInterface.GenericFileType>> fileMap = plugin
            .indexWorkflowFiles(initialPath, contents, reader);
        Assert.assertEquals(1, fileMap.size());
        final Pair<String, MinimalLanguageInterface.GenericFileType> discoveredFile = fileMap.get("/Dockstore.gxwf-test.yml");
        Assert.assertEquals(discoveredFile.getRight(), MinimalLanguageInterface.GenericFileType.TEST_PARAMETER_FILE);
        final RecommendedLanguageInterface.WorkflowMetadata metadata = plugin.parseWorkflowForMetadata(initialPath, contents, fileMap);
        // We don't track these currently, but we could pull out the CWL parsing and mimic that.
        Assert.assertEquals(null, metadata.getAuthor());
        Assert.assertEquals(null, metadata.getEmail());
        // We have name and annotation - not sure if this should just be "<name>"", or "<name>. <annotation>", or
        // "<name>/n<annotation>".
        Assert.assertEquals("Test Workflow", metadata.getDescription());

        // Test validation stubs...
        final VersionTypeValidation wfValidation = plugin.validateWorkflowSet(initialPath, contents, fileMap);
        Assert.assertTrue(wfValidation.isValid());
        final VersionTypeValidation testValidation = plugin.validateTestParameterSet(fileMap);
        Assert.assertTrue(testValidation.isValid());
    }

    @Test
    public void testNativeWorkflowParsing() {
        final GalaxyWorkflowPlugin plugin = new GalaxyWorkflowPlugin();
        final HttpFileReader reader = new HttpFileReader(REPO_NATIVE);
        final String initialPath = EXAMPLE_FILENAME_NATIVE_PATH;
        final String contents = reader.readFile(EXAMPLE_FILENAME_NATIVE);
        final Map<String, Pair<String, MinimalLanguageInterface.GenericFileType>> fileMap = plugin
            .indexWorkflowFiles(initialPath, contents, reader);
        Assert.assertEquals(1, fileMap.size());
        final Pair<String, MinimalLanguageInterface.GenericFileType> discoveredFile = fileMap.get("/Dockstore-test.yml");
        Assert.assertEquals(discoveredFile.getRight(), MinimalLanguageInterface.GenericFileType.TEST_PARAMETER_FILE);
        final RecommendedLanguageInterface.WorkflowMetadata metadata = plugin.parseWorkflowForMetadata(initialPath, contents, fileMap);

        // We don't track these currently - especially with native format.
        Assert.assertEquals(null, metadata.getAuthor());
        Assert.assertEquals(null, metadata.getEmail());
        // We have name and annotation - not sure if this should just be "<name>"", or "<name>. <annotation>", or
        // "<name>/n<annotation>".
        Assert.assertEquals("Test Workflow", metadata.getDescription());
    }

    @Test
    public void testIsNotAService() {
        final GalaxyWorkflowPlugin plugin = new GalaxyWorkflowPlugin();
        Assert.assertFalse(plugin.isService());
    }

    @Test
    public void testInitialPathPattern() {
        // TODO: This doesn't seem to be called by Dockstore anywhere - is that right?
        final GalaxyWorkflowPlugin plugin = new GalaxyWorkflowPlugin();
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

    class HttpFileReader implements MinimalLanguageInterface.FileReader {
        private final String repo;

        HttpFileReader(final String repo) {
            this.repo = repo;
        }

        @Override
        public String readFile(String path) {
            try {
                if(path.startsWith("/")) {
                    path = path.substring(1);
                }
                return Resources.toString(new URL(this.repo + "/master/" + path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
