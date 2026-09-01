// SSS 表示毫秒位数，6位：SSSSSS
yyyyMMddHHmmssSSS
yyMMddHHmmssSSS

yyyyMMddHHmmss
yyMMddHHmmss

yyyy-MM-dd HH:mm:ss.SSS
yyyy-MM-dd HH:mm:ss
yyyy-MM-dd

yyyy/MM/dd HH:mm:ss.SSS
yyyy/MM-dd HH:mm:ss
yyyy/MM/dd

HH:mm:ss.SSS
HH:mm:ss

// UTC+Offset 时区偏移格式（ISO 8601），带时区偏移量（如+08:00）
// OffsetDateTime.now() 示例：2026-09-01T16:18:15.480+08:00
// 日期时间（毫秒）带T分隔符和时区偏移（XXX表示+/-HH:mm）
yyyy-MM-dd'T'HH:mm:ss.SSSXXX
// 日期时间（无毫秒）带T和时区偏移
yyyy-MM-dd'T'HH:mm:ssXXX

yyyy/MM/dd'T'HH:mm:ss.SSSXXX
yyyy/MM/dd'T'HH:mm:ssXXX

// 纯时间（毫秒）带时区偏移，无日期
HH:mm:ss.SSSXXX
// 纯时间（无毫秒）带时区偏移
HH:mm:ssXXX

// UTC Zulu 时区（零时区），用字面量'Z'表示
// 示例：2026-09-01T08:19:07.142Z
// 日期时间（毫秒）带T和'Z'，表示UTC时间
yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
yyyy-MM-dd'T'HH:mm:ss'Z'

yyyy/MM/dd'T'HH:mm:ss.SSS'Z'
yyyy/MM/dd'T'HH:mm:ss'Z'

// 纯时间（毫秒）带'Z'，表示UTC时间
HH:mm:ss.SSS'Z'
HH:mm:ss'Z'

// ZonedDateTime 带时区ID（如 Asia/Shanghai）和偏移量
// 示例：2026-09-01 16:21:34.696 +08:00 [Asia/Shanghai]
// 日期时间（毫秒）带空格、偏移（XXX）和方括号内的时区ID（VV）
yyyy-MM-dd HH:mm:ss.SSS XXX '['VV']'
yyyy-MM-dd HH:mm:ss XXX '['VV']'

yyyy/MM/dd HH:mm:ss.SSS XXX '['VV']'
yyyy/MM/dd HH:mm:ss XXX '['VV']'
