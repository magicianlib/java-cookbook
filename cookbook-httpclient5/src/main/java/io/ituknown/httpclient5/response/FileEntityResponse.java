package io.ituknown.httpclient5.response;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Setter
@Getter
public class FileEntityResponse {
    private Headers headers;
    private Path filePath;
    private long fileSize;

    @Override
    public String toString() {
        return "[filePath: " + filePath + ", size: " + fileSize + "] headers: " + headers;
    }
}