package io.ituknown.httpclient5.response;

import java.nio.file.Path;

public class FileEntityResponse {
    private Headers headers;
    private Path filename;
    private long fileSize;

    public Headers getHeader() {
        return headers;
    }

    public void setHeader(Headers headers) {
        this.headers = headers;
    }

    public Path getFilename() {
        return filename;
    }

    public void setFilename(Path filename) {
        this.filename = filename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public String toString() {
        return "[filename: " + filename + ", size: " + fileSize + "] headers: " + headers;
    }
}