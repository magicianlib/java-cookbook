package io.ituknown.fesod.write;

/**
 * Excel 导出业务类型枚举。
 * <p>
 * 实际应用中按自身业务替换或扩展常量；cookbook 中提供两个示例常量，
 * 供 {@link AbstractExporter} 子类声明业务类型，外部据此定位具体子类。
 * <p>
 * 枚举本身只承担"业务类型标识"职责，<b>不</b>声明导出模式（Class / 模板）——
 * 模式由子类继承哪个模式基类（{@link ClassExporter} / {@link TemplateExporter}）体现。
 *
 * @author magicianlib@gmail.com
 */
public enum ExportBizType {

    /** 示例：用户列表导出（{@link ClassExporter} 注解模式） */
    USER_EXPORT,

    /** 示例：订单导出（{@link TemplateExporter} 模板填充模式） */
    ORDER_EXPORT
}
