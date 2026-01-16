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

    protected Path getFileName() {
        return fileName;
    }

    @Override
    public FileEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        if (response.getCode() >= 300) {
            EntityUtils.consume(response.getEntity());
            throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
        }

        FileEntityResponse result = new FileEntityResponse();
        result.setHeader(Helper.resolveHeader(response));

        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            LOGGER.info("http response no data, header: {}", result.getHeader());
            return result;
        }

        // 确保父目录存在
        Path parent = getFileName().getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }

        result.setFilename(getFileName());

        try (final InputStream in = entity.getContent()) {
            // StandardCopyOption.REPLACE_EXISTING 视业务需求而定
            long bytesCopied = Files.copy(in, getFileName(), StandardCopyOption.REPLACE_EXISTING);
            result.setFileSize(bytesCopied);

            // 确保实体被完全消耗
            EntityUtils.consume(entity);
        } catch (IOException e) {
            // 发生异常时尝试删除可能创建的残缺文件
            Files.deleteIfExists(getFileName());
            throw e;
        }

        LOGGER.info("http response {}", result);
        return result;
    }
}