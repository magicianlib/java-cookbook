package io.ituknown.fesod.write;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 导出处理上下文（贯穿单次导出全程）。
 * <p>
 * 由 {@link AbstractExporter} 在写入开始时创建，承担两类职责：
 * <ul>
 *   <li><b>统计</b>：累计数据量（{@link #addRows}）、处理耗时（{@link #finish()} 计算）、摘要日志；</li>
 *   <li><b>过程状态</b>：通过 {@link #accumulate} / {@link #get} 的通用属性容器，
 *       让 {@link AbstractExporter#loadData} 累积中间结果
 *       （如金额合计、分类计数），供 {@link TemplateExporter#summary}
 *       等后续钩子读取——两者经本对象解耦。</li>
 * </ul>
 * <p>
 * 设计为非 final、统计字段受保护（{@code protected}），便于后续扩展更多统计维度
 * （如成功/失败行数、各阶段耗时等）：直接添加字段并在 {@link #finish()} 中输出即可，
 * 或继承本类重写 {@link #finish()}。
 * <p>
 * 本对象为"单次导出、用完即弃"，{@link #attributes} 使用 {@link HashMap}，无需并发安全。
 *
 * @author magicianlib@gmail.com
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ExporterContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExporterContext.class);

    /** 业务类型 */
    protected final ExportBizType bizType;
    /** 开始时间（毫秒） */
    protected final long startTime;
    /** 已写入数据量 */
    protected long rows;
    /** 处理耗时（毫秒），{@link #finish()} 时计算 */
    protected long elapsedMs;

    /** 业务自定义过程属性（loadData 累积、summary 等读取）。已初始化，不计入构造参数。 */
    private final Map<String, Object> attributes = new HashMap<>();

    /** 创建并记录开始时间 */
    public static ExporterContext start(ExportBizType bizType) {
        return new ExporterContext(bizType, System.currentTimeMillis());
    }

    /** 累加已写入数据量 */
    public void addRows(int n) {
        this.rows += n;
    }

    /**
     * 累积一个过程属性（如金额合计、分类计数），供后续钩子（如 summary）读取。
     * <p>同名 key 后写覆盖前写。链式返回 {@code this}，便于多值累积。
     */
    public <T> ExporterContext accumulate(String key, T value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * 按类型读取过程属性。
     *
     * @param key          属性键
     * @param type         期望类型，用于安全转换（不匹配抛 {@link ClassCastException}）
     * @param defaultValue 不存在时的默认值
     */
    public <T> T get(String key, Class<T> type, T defaultValue) {
        Object value = attributes.get(key);
        return value == null ? defaultValue : type.cast(value);
    }

    /** 结束统计：计算耗时并打印摘要日志 */
    public void finish() {
        this.elapsedMs = System.currentTimeMillis() - startTime;
        LOGGER.info("Excel 写入完成 | bizType={} | rows={} | elapsedMs={}", bizType, rows, elapsedMs);
    }
}
