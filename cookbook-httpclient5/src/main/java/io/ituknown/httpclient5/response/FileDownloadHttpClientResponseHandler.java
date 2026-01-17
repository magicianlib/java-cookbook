package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.Helper;
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

public class FileDownloadHttpClientResponseHandler implements HttpClientResponseHandler<FileEntityResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadHttpClientResponseHandler.class);

    private final Path fileName;

    public FileDownloadHttpClientResponseHandler(String fileName) {
        this(Paths.get(fileName));
    }

    public FileDownloadHttpClientResponseHandler(Path fileName) {
        this.fileName = fileName;
    }

    public Path getFileName() {
        return fileName;
    }

    @Override
    public FileEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        if (response.getCode() >= 300) {
            EntityUtils.consume(response.getEntity());
            LOGGER.warn("Download failed, status code: {}, reason: {}", response.getCode(), response.getReasonPhrase());
            throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
        }

        FileEntityResponse result = new FileEntityResponse();
        result.setHeader(Helper.resolveHeader(response));

        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            LOGGER.warn("Http response has no entity content. Header: {}", result.getHeader());
            return result;
        }

        Path targetFile = getFileName();
        LOGGER.debug("Starting file download. Target: {}, Expected Size: {} bytes", targetFile, entity.getContentLength());

        // 确保父目录存在
        Path parent = targetFile.getParent();
        if (parent != null && Files.notExists(parent)) {
            LOGGER.info("Creating directory: {}", parent);
            Files.createDirectories(parent);
        }

        try (final InputStream in = entity.getContent()) {
            // StandardCopyOption.REPLACE_EXISTING 视业务需求而定
            long bytesCopied = Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            result.setFileSize(bytesCopied);
            result.setFilename(targetFile);

            // 确保实体被完全消耗
            EntityUtils.consume(entity);
        } catch (IOException e) {
            LOGGER.error("Failed to save file [{}]. Cleaning up fragment...", targetFile, e);
            try {
                Files.deleteIfExists(targetFile);
            } catch (IOException cleanupEx) {
                LOGGER.error("Failed to delete fragmented file: {}", targetFile, cleanupEx);
            }
            throw e;
        }

        LOGGER.info("Download completed: {}, Size: {} bytes", targetFile.getFileName(), result.getFileSize());
        return result;
    }
}