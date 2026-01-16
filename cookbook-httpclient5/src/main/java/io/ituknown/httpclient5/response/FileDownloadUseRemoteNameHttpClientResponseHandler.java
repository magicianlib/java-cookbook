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
    private Path fileName;

    public FileDownloadUseRemoteNameHttpClientResponseHandler(Path targetDir) {
        super(targetDir);
        this.targetDir = targetDir;
    }

    @Override
    public Path getFileName() {
        return fileName;
    }

    @Override
    public FileEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        // 从 Header 获取文件名，如果没有则给个默认值
        String fileName = Helper.fileNameParse(response.getFirstHeader("Content-Disposition"));
        if (fileName == null) {
            fileName = "download_" + System.currentTimeMillis();
            LOGGER.info("Since no file name was parsed from Content-Disposition, a temporary file name was used: {}", fileName);
        } else {
            LOGGER.info("The file name parsed from Content-Disposition: {}", fileName);
        }
        this.fileName = targetDir.resolve(fileName);
        return super.handleResponse(response);
    }
}