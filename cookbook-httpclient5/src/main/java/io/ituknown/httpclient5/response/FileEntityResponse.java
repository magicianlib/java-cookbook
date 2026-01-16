package io.ituknown.httpclient5.response;

import java.nio.file.Path;

public class FileEntityResponse {
    private Header header;
    private Path filename;
    private long fileSize;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
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
        return "[filename: " + filename + ", size: " + fileSize + "] header: " + header;
    }
}