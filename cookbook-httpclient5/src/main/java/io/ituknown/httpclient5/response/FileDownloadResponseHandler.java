package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.HeaderHelper;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileDownloadResponseHandler extends AbstractHttpResponseHandler<FileEntityResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadResponseHandler.class);

    private final Path filePath;

    public FileDownloadResponseHandler(String filePath) {
        this(Paths.get(filePath));
    }

    public FileDownloadResponseHandler(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    protected FileEntityResponse handleSuccessful(ClassicHttpResponse response, int statusCode) throws IOException {
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

        // Files.copy 已将输入流读到 EOF，entity 随之耗尽，无需再 EntityUtils.consume
        try (final InputStream in = entity.getContent()) {
            long bytesCopied = Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            result.setFileSize(bytesCopied);
            result.setFilePath(filePath);
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
