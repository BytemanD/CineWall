package com.cinewall.app.model

/** TMDb 刮削时使用的元数据语言，code 直接作为 API 的 language 参数。 */
enum class TmdbLanguage(val code: String, val label: String) {
    ZH_CN("zh-CN", "简体中文"),
    ZH_TW("zh-TW", "繁体中文"),
    EN_US("en-US", "English"),
    JA_JP("ja-JP", "日本語");

    companion object {
        fun fromCode(code: String?): TmdbLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ZH_CN
    }
}
