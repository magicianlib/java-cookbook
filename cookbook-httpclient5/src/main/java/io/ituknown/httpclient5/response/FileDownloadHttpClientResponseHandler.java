package io.ituknown.httpclient5.response;

import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
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

    protected Path getFilename() {
        return fileName;
    }

    @Override
    public FileEntityResponse handleResponse(ClassicHttpResponse response) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Header header : response.getHeaders()) {
            builder.append(" ").append(header.getName()).append(": ").append(header.getValue());
        }
        LOGGER.info("http response content: [file: {}] header: [{}]", getFileName(), builder.substring(1));

        if (response.getCode() >= 300) {
            EntityUtils.consume(response.getEntity());
            throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
        }

        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            return new FileEntityResponse(response.getHeaders());
        }

        // 确保父目录存在
        Path parent = getFilename().getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }

        FileEntityResponse result = new FileEntityResponse(response.getHeaders());
        result.setFilename(getFilename());

        try (final InputStream in = entity.getContent()) {
            // StandardCopyOption.REPLACE_EXISTING 视业务需求而定
            long bytesCopied = Files.copy(in, getFilename(), StandardCopyOption.REPLACE_EXISTING);
            result.setFileSize(bytesCopied);

            // 确保实体被完全消耗
            EntityUtils.consume(entity);
        } catch (IOException e) {
            // 发生异常时尝试删除可能创建的残缺文件
            Files.deleteIfExists(getFilename());
            throw e;
        }
        return result;
    }
}
