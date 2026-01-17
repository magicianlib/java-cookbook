package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.Helper;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class FileDownloadUseRemoteNameHttpClientResponseHandler extends FileDownloadHttpClientResponseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadUseRemoteNameHttpClientResponseHandler.class);

    private final Path targetDir;
    private final String requestUrl;
    private Path fileName;

    public FileDownloadUseRemoteNameHttpClientResponseHandler(Path targetDir, String requestUrl) {
        super(targetDir);
        this.targetDir = targetDir;
        this.requestUrl = requestUrl;
    }

    @Override
    public Path getFileName() {
        return fileName;
    }

    @Override
    public FileEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        // 从请求头获取文件名
        String fileName = Helper.fileNameParse(response.getFirstHeader("Content-Disposition"));

        // 如果 Header 没有，尝试从 URL 获取
        if (fileName == null || fileName.isBlank()) {
            fileName = Helper.getFileNameFromUrl(requestUrl);
            if (fileName != null) {
                LOGGER.info("Parsed file name from URL: {}", fileName);
            }
        } else {
            LOGGER.info("Parsed file name from Content-Disposition: {}", fileName);
        }

        // 保底策略
        if (fileName == null || fileName.isBlank()) {
            fileName = "download_" + System.currentTimeMillis();
            LOGGER.warn("Failed to parse name from Header and URL, using fallback: {}", fileName);
        }

        this.fileName = targetDir.resolve(fileName);
        return super.handleResponse(response);
    }
}