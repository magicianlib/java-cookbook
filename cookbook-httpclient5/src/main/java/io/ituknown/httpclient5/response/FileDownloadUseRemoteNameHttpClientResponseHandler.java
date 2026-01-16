package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.Helper;
import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.IOException;
import java.nio.file.Path;

public class FileDownloadUseRemoteNameHttpClientResponseHandler extends FileDownloadHttpClientResponseHandler {
    private final Path targetDir;
    private Path fileName;

    public FileDownloadUseRemoteNameHttpClientResponseHandler(Path targetDir) {
        super(targetDir);
        this.targetDir = targetDir;
    }

    @Override
    protected Path getFileName() {
        return fileName;
    }

    @Override
    public FileEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        // 从 Header 获取文件名，如果没有则给个默认值
        String fileName = Helper.fileNameParse(response.getFirstHeader("Content-Disposition"));
        if (fileName == null) {
            fileName = "download_" + System.currentTimeMillis();
        }
        this.fileName = targetDir.resolve(fileName);
        return super.handleResponse(response);
    }
}