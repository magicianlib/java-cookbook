package io.ituknown.httpclient5.response;

import org.apache.hc.core5.http.Header;

import java.nio.file.Path;

public class FileEntityResponse extends HeaderResponse {
    private Path filename;
    private long fileSize;

    public FileEntityResponse(Header[] headers) {
        super(headers);
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
}