package io.ituknown.okhttp.parameter;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MultipartParameter {

    private final List<Multipart> parameters = new ArrayList<>();

    public void add(String name, String value) {
        parameters.add(new StringPart(name, value));
    }

    public void add(String name, File file) {
        parameters.add(new FilePart(name, file));
    }

    public void add(String name, String fileName, byte[] bytes) {
        parameters.add(new BufferPart(name, fileName, bytes));
    }

    public MultipartBody toMultipart() {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        for (Multipart part : parameters) {
            part.toMultipart(builder);
        }
        return builder.build();
    }

    public static class Builder {
        private final MultipartParameter parameter = new MultipartParameter();

        public Builder add(String name, String value) {
            parameter.parameters.add(new StringPart(name, value));
            return this;
        }

        public Builder add(String name, File file) {
            parameter.parameters.add(new FilePart(name, file));
            return this;
        }

        public Builder add(String name, String fileName, byte[] bytes) {
            parameter.parameters.add(new BufferPart(name, fileName, bytes));
            return this;
        }

        public MultipartParameter build() {
            return parameter;
        }
    }

    public interface Multipart {
        void toMultipart(MultipartBody.Builder builder);
    }

    public static class StringPart implements Multipart {
        private final String name;
        private final String value;

        public StringPart(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public void toMultipart(MultipartBody.Builder builder) {
            builder.addFormDataPart(name, value);
        }
    }

    public static class FilePart implements Multipart {
        private final String name;
        private final File file;

        public FilePart(String name, File file) {
            this.name = name;
            this.file = file;
        }

        @Override
        public void toMultipart(MultipartBody.Builder builder) {
            String fileName = file.getName();
            String type = URLConnection.guessContentTypeFromName(fileName);
            if (type == null) {
                type = "application/octet-stream"; // 默认二进制流
            }

            builder.addFormDataPart(name, fileName, RequestBody.create(file, MediaType.parse(type)));
        }
    }

    public static class BufferPart implements Multipart {
        private final String name;
        private final String fileName;
        private final byte[] bytes;

        public BufferPart(String name, String fileName, byte[] bytes) {
            this.name = name;
            this.fileName = fileName;
            this.bytes = bytes;
        }

        @Override
        public void toMultipart(MultipartBody.Builder builder) {
            String type = URLConnection.guessContentTypeFromName(fileName);
            if (type == null) {
                type = "application/octet-stream"; // 默认二进制流
            }

            builder.addFormDataPart(name, fileName, RequestBody.create(bytes, MediaType.parse(type)));
        }
    }
}