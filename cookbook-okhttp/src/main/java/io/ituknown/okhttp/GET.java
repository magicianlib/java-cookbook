package io.ituknown.okhttp;

import io.ituknown.okhttp.parameter.KeyValueParameter;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.function.Function;

enum GET {
    ;
    private static final Logger LOGGER = LoggerFactory.getLogger(GET.class);

    public static String get(String url, KeyValueParameter parameter, RequestConfig config) {
        return doGet(url, parameter, config, body -> {
            try {
                return body.string();
            } catch (Exception e) {
                throw new HttpException(e);
            }
        });
    }

    public static byte[] getFile(String url, KeyValueParameter parameter, RequestConfig config) {
        return doGet(url, parameter, config, body -> {
            try {
                return body.bytes();
            } catch (Exception e) {
                throw new HttpException(e);
            }
        });
    }

    public static String download(String url, KeyValueParameter parameter, RequestConfig config) {

        String filename;
        try {
            filename = File.createTempFile("tmp_", "_" + url.substring(url.lastIndexOf("/") + 1)).getPath();
            LOGGER.info("filepath is null, create tmp file: {}", filename);
        } catch (Exception e) {
            throw new HttpException("create tmp file fail", e);
        }

        doGet(url, parameter, config, body -> {
            try (InputStream in = body.byteStream(); FileOutputStream out = new FileOutputStream(filename)) {
                int len;
                byte[] buf = new byte[1024];
                while (-1 != (len = in.read(buf))) {
                    out.write(buf, 0, len);
                }
                return true;
            } catch (Exception e) {
                throw new HttpException("download fail or write file exception: " + filename, e);
            }
        });

        return filename;
    }

    public static <R> R doGet(String url, KeyValueParameter parameter, RequestConfig config, Function<ResponseBody, R> function) {
        Request request = new Request.Builder()
                .headers(config.toHeaders())
                .url(parameter.toUrl(url))
                .tag(RequestConfig.class, config)
                .get()
                .build();

        if (config.isNeedLog()) {
            LOGGER.info("Http {}", request);
        }

        Call call = Client.execute(config, request);
        try (Response response = call.execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                return function.apply(body);
            }

            if (config.isNeedLog()) {
                LOGGER.info("Request fail or no-content[code={}]", response.code());
            }

        } catch (Exception e) {
            throw new HttpException(e);
        }

        return null;
    }
}