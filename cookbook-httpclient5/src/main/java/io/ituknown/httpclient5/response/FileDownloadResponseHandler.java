package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.HeaderHelper;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileDownloadResponseHandler implements HttpClientResponseHandler<FileEntityResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadResponseHandler.class);

    private final Path filePath;

    public FileDownloadResponseHandler(String filePath) {
        this(Paths.get(filePath));
    }

    public FileDownloadResponseHandler(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public FileEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        if (response.getCode() >= 300) {
            EntityUtils.consume(response.getEntity());
            LOGGER.warn("Download failed, status code: {}, reason: {}", response.getCode(), response.getReasonPhrase());
            throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
        }

        FileEntityResponse result = new FileEntityResponse();
        result.setHeaders(HeaderHelper.resolveHeader(response));

        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            LOGGER.warn("Http response has no entity content. Header: {}", result.getHeaders());
            return result;
        }

        LOGGER.debug("Starting file download. Target: {}, Expected Size: {} bytes", filePath, entity.getContentLength());

        Path parent = filePath.getParent();
        if (parent != null && Files.notExists(parent)) {
            LOGGER.info("Creating directory: {}", parent);
            Files.createDirectories(parent);
        }

        try (final InputStream in = entity.getContent()) {
            long bytesCopied = Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            result.setFileSize(bytesCopied);
            result.setFilePath(filePath);

            EntityUtils.consume(entity);
        } catch (IOException e) {
            LOGGER.error("Failed to save file [{}]. Cleaning up fragment...", filePath, e);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException cleanupEx) {
                LOGGER.error("Failed to delete fragmented file: {}", filePath, cleanupEx);
            }
            throw e;
        }

        LOGGER.info("Download completed: {}, Size: {} bytes", filePath.getFileName(), result.getFileSize());
        return result;
    }
}
