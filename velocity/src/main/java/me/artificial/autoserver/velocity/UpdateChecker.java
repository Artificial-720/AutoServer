package me.artificial.autoserver.velocity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UpdateChecker {

    private final Logger logger;
    private final String currentVersion;
    private String latest;

    UpdateChecker(Logger logger, String version) {
        this.logger = logger;
        this.currentVersion = version;
    }

    public boolean isUpdateAvailable() throws IndexOutOfBoundsException, IOException, InterruptedException {
        JsonArray versions = getVersions();
        if (versions == null) {
            return false;
        }

        JsonObject latest = versions.get(0).getAsJsonObject();
        this.latest = latest.get("version_number").getAsString();
        return !this.latest.equals(currentVersion);
    }

    public String latest() {
        return (latest == null) ? currentVersion : latest;
    }

    private JsonArray getVersions() throws IOException, InterruptedException, UncheckedIOException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/project/autoserver/version?loaders=[%22velocity%22]"))
                .header("User-Agent", "autoserver")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return JsonParser.parseString(response.body()).getAsJsonArray();
        } else {
            logger.error("Request failed with status code: {}", response.statusCode());
        }

        return null;
    }
}
