package io.ituknown.httpclient5.response;

import io.ituknown.httpclient5.HeaderHelper;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class RemoteNameFileDownloadResponseHandler implements HttpClientResponseHandler<FileEntityResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteNameFileDownloadResponseHandler.class);

    private final Path targetDir;
    private final String requestUrl;

    public RemoteNameFileDownloadResponseHandler(Path targetDir, String requestUrl) {
        this.targetDir = targetDir;
        this.requestUrl = requestUrl;
    }

    @Override
    public FileEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        String detectedName = HeaderHelper.fileNameParse(response.getFirstHeader("Content-Disposition"));

        if (detectedName == null || detectedName.isBlank()) {
            detectedName = HeaderHelper.getFileNameFromUrl(requestUrl);
            if (detectedName != null) {
                LOGGER.info("Parsed file name from URL: {}", detectedName);
            }
        } else {
            LOGGER.info("Parsed file name from Content-Disposition: {}", detectedName);
        }

        if (detectedName == null || detectedName.isBlank()) {
            detectedName = "download_" + System.currentTimeMillis();
            LOGGER.warn("Failed to parse name from Header and URL, using fallback: {}", detectedName);
        }

        // 防止服务器返回的文件名逃逸目标目录（CWE-22）：绝对路径名会被 resolve 直接采用，
        // 含 ../ 的相对名会越界，配合 REPLACE_EXISTING 即可覆盖任意可写文件。
        Path filePath = targetDir.resolve(detectedName).normalize();
        if (!filePath.startsWith(targetDir.normalize())) {
            throw new IOException("Refused unsafe remote file name (escapes target dir): " + detectedName);
        }

        return new FileDownloadResponseHandler(filePath).handleResponse(response);
    }
}
