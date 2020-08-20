//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVAC_OPTIONS -Xlint:unchecked
//DEPS com.google.code.gson:gson:2.8.6
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.google.gson.*;

/**
 */
public class lastweek {

  private Collection<String> java = List.of("redhat-developer/vscode-java", "eclipse/eclipse.jdt.ls");
  private Collection<String> quarkus = List.of("redhat-developer/vscode-quarkus", "redhat-developer/vscode-microprofile", "eclipse/lsp4mp", "redhat-developer/quarkus-ls");
  private Collection<String> xml = List.of("redhat-developer/vscode-xml", "eclipse/lemminx");

  private LocalDate since = LocalDate.now().minusWeeks(1);
  public static void main(String[] args) throws Exception {
    new lastweek().getCommits();
  }

  private String getQuery(String repo) {
    String url = "https://api.github.com/repos/"+repo+"/commits?since="+since;
    return url;
  }

  public void getCommits() throws Exception {
    generateCommits("Java",java);
    generateCommits("Quarkus && Microprofile", quarkus);
    generateCommits("XML",xml);
    System.exit(0);
  }

  private void generateCommits(String header, Collection<String> repos) throws Exception {
    if (repos == null || repos.isEmpty()) {
      return;
    }
    LinkedHashSet<String> commits = new LinkedHashSet<>();
    for (String repo : repos) {
      String url = getQuery(repo);
      commits.addAll(listCommits(url));
    }
    if (!commits.isEmpty()) {
      generateLine("# "+header);
      commits.forEach(c -> generateLine( " - "+c));
    }
  }

  private void generateLine(String line) {
    System.out.println(line);
  }

  private LinkedHashSet<String> listCommits(String url) throws Exception {
    int page = 1;
    boolean hasMore = true;
    Gson gson = new Gson();
    LinkedHashSet<String> commitMsgs = new LinkedHashSet<>();
    do {
      String pageUrl = url+"&page="+(page++);
      //System.out.println("Fetching issues from " + pageUrl);
      URL u = new URL(pageUrl);
      URLConnection connection = u.openConnection();
      try (InputStream in = connection.getInputStream()) {
        Reader reader = new InputStreamReader(in, "UTF-8");
        JsonArray commits = gson.fromJson(reader, JsonArray.class);
        for (JsonElement el : commits) {
          JsonObject commit = el.getAsJsonObject().get("commit").getAsJsonObject();
          String message = commit.get("message").getAsString();
          int lineBreak = message.indexOf("\n");
          String title = lineBreak < 0 ? message : message.substring(0, lineBreak);
          if (!isIgnored(title)) {
            commitMsgs.add(title);
          }
        }
        hasMore = false;
      }
    } while (hasMore);
    return commitMsgs;
  }

  private boolean isIgnored(String title){
    if (title.trim().isBlank()) {
      return true;
    }
    String t = title.toLowerCase();
    return t.contains("upversion") || t.contains("changelog") || t.contains("jenkinsfile");
  }
}