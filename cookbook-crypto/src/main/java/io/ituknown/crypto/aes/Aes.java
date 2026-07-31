package io.ituknown.crypto.aes;

/**
 * 对称加解密入口：按加密家族返回对应构建器，在编译期阻止非法组合。
 *
 * <p><b>三种家族</b>（按是否带认证、是否需填充划分）：
 * <ul>
 * <li>认证加密（AEAD）家族：GCM、CCM、OCB。同时保证机密性与完整性，密文末尾自带防篡改认证标签，无需填充，是大多数业务场景的首选。</li>
 * <li>分组家族：CBC。仅保证机密性、不保证完整性，需选择填充模式，密文须为整块倍数。</li>
 * <li>流式家族：CTR、CFB、OFB。将分组加密转为流式，无需填充、密文与明文等长，不保证完整性。</li>
 * </ul>
 *
 * <p><b>各模式特点与适用场景</b>：
 * <ul>
 * <li>GCM——硬件加速成熟、最常用，适合通用数据加密与网络协议。</li>
 * <li>CCM——常见于受限设备、嵌入式与无线协议（如物联网、近距离通信）。</li>
 * <li>OCB——吞吐高、延迟低，但历史专利争议较多，选用前需确认许可。</li>
 * <li>CBC——兼容既有系统或配合外部校验时使用；单独用于传输有被篡改风险。</li>
 * <li>CTR——可随机访问、常作底层原语，也可直接用于等长流式加密。</li>
 * <li>CFB / OFB——适合逐字节或小块的流式处理。</li>
 * </ul>
 *
 * <p><b>选型建议</b>：无特殊需求默认用 GCM——库提供的默认门面亦以 GCM 加密；需完整性认证选认证加密家族；仅机密性且输入可整块选 CBC；流式或需等长密文选 CTR/CFB/OFB。
 *
 * <p><b>类型状态保护</b>：认证加密构建器可配置认证标签长度、不接受填充；分组构建器须选择填充、无认证标签；流式构建器两者皆不接受。"认证加密配填充""分组配认证标签"等非法组合无法通过编译。
 */
public final class Aes {

    private Aes() {
    }

    public static AeadAesBuilder gcm() {
        return new AeadAesBuilder(AesMode.GCM);
    }

    public static AeadAesBuilder ccm() {
        return new AeadAesBuilder(AesMode.CCM);
    }

    public static AeadAesBuilder ocb() {
        return new AeadAesBuilder(AesMode.OCB);
    }

    public static BlockAesBuilder cbc() {
        return new BlockAesBuilder(AesMode.CBC);
    }

    public static StreamAesBuilder ctr() {
        return new StreamAesBuilder(AesMode.CTR);
    }

    public static StreamAesBuilder cfb() {
        return new StreamAesBuilder(AesMode.CFB);
    }

    public static StreamAesBuilder ofb() {
        return new StreamAesBuilder(AesMode.OFB);
    }
}