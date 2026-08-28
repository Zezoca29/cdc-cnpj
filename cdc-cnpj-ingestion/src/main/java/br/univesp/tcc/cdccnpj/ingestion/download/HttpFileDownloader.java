package br.univesp.tcc.cdccnpj.ingestion.download;

import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public final class HttpFileDownloader {

    private final HttpClient httpClient;

    public HttpFileDownloader() {
        this(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    public HttpFileDownloader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void download(DownloadTarget target, Path destination) {
        try {
            Files.createDirectories(destination.getParent());
            Path tempFile = Files.createTempFile(destination.getParent(), "download-", ".part");
            HttpRequest request = HttpRequest.newBuilder(URI.create(target.url()))
                    .timeout(Duration.ofHours(6))
                    .GET()
                    .build();
            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ParseStageException(
                        "HTTP " + response.statusCode() + " downloading " + target.url());
            }
            try (InputStream body = response.body()) {
                Files.copy(body, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ParseStageException("Interrupted downloading " + target.url(), e);
        } catch (IOException e) {
            throw new ParseStageException("Failed downloading " + target.url(), e);
        }
    }
}
