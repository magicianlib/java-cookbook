package io.ituknown.crypto;

import java.nio.charset.StandardCharsets;

/**
 * PEM/DER 自动识别与转换的包级工具（仅 {@code io.ituknown.crypto} 包内使用，不对外暴露）。
 * <p>
 * 被各密钥加载工具类（如 {@link RsaKeys}、{@link EcdsaKeys}）共用，避免重复实现。
 */
final class Pem {

    private Pem() {
    }

    /** PEM 头部前缀（ASCII）。 */
    static final String PEM_BEGIN = "-----BEGIN ";

    /**
     * 判断给定内容是否为 PEM 文本。
     *
     * @param content 原始字节
     * @return 内容以 {@code -----BEGIN } 开头时返回 true
     */
    static boolean isPem(byte[] content) {
        byte[] marker = PEM_BEGIN.getBytes(StandardCharsets.US_ASCII);
        if (content.length < marker.length) {
            return false;
        }
        for (int i = 0; i < marker.length; i++) {
            if (content[i] != marker[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从 PEM 文本中提取 DER 字节：去除 {@code -----BEGIN-----} / {@code -----END-----} 行，
     * 拼接其余行后 Base64 解码。非 PEM 内容原样返回。
     *
     * @param content 原始字节（可能为 PEM 或 DER）
     * @return DER 字节
     */
    static byte[] extractPemContent(byte[] content) {
        if (!isPem(content)) {
            return content;
        }
        String text = new String(content, StandardCharsets.US_ASCII);
        StringBuilder base64 = new StringBuilder();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-----") || trimmed.isEmpty()) {
                continue; // 跳过头尾行与空行
            }
            base64.append(trimmed);
        }
        return Base64.toByte(base64.toString());
    }
}
