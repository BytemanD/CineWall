package com.cinewall.app.model

/** TMDb 刮削时使用的元数据语言，code 直接作为 API 的 language 参数。 */
enum class PosterScrollDelay(val label: String, val value: Int ) {
    Second5("5秒", 5 * 1000),
    Second7("7秒", 7 * 1000),
    Second10("10秒", 10 * 1000),
    Second15("15秒", 15 * 1000);

    companion object {
        fun fromValue(code: Int?): PosterScrollDelay =
            entries.firstOrNull { it.value == code } ?: Second5
    }
}
