package org.galaxyproject.dockstore_galaxy_interface.language;

import static org.galaxyproject.dockstore_galaxy_interface.language.GalaxyWorkflowLanguagePluginTest.CURRENT_BRANCH;
import static org.galaxyproject.dockstore_galaxy_interface.language.GalaxyWorkflowLanguagePluginTest.REPO_ID_1;
import static org.galaxyproject.dockstore_galaxy_interface.language.GalaxyWorkflowLanguagePluginTest.REPO_ID_2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

public class PrepareSerializedDirectoryListings {

  /**
   * GitHub calls in this repo are not authorized and thus are subject to rate limit. This method
   * serializes directory listings. Run this to re-generate directory listings for testing.
   *
   * @param args none
   */
  public static void main(String[] args) throws IOException {

    List<String> content1 = getDirectoryListing(REPO_ID_1);
    List<String> content2 = getDirectoryListing(REPO_ID_2);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    System.out.println(gson.toJson(content1));
    System.out.println(gson.toJson(content2));

    FileUtils.writeStringToFile(
        new File("src/test/resources/" + REPO_ID_1 + "/listing.json"), gson.toJson(content1));
    FileUtils.writeStringToFile(
        new File("src/test/resources/" + REPO_ID_2 + "/listing.json"), gson.toJson(content2));
  }

  private static List<String> getDirectoryListing(String id) throws IOException {
    final GitHub github = new GitHubBuilder().build();
    GHRepository repo1 = github.getRepository(id);
    // TODO: parameterize path for nested directories
    List<GHContent> directoryContent1 = repo1.getDirectoryContent("/", CURRENT_BRANCH);
    return directoryContent1.stream()
        .map(content -> "/" + content.getName())
        .collect(Collectors.toList());
  }
}
