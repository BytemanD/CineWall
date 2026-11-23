package com.cinewall.app.scraper

import android.util.Log
import com.cinewall.app.model.MediaItem

/**
 * 刮削顺序：TMDb → 豆瓣 → 本地信息。
 * 主数据源抛异常或没有匹配结果（scraped=false）时自动尝试备用源。
 */
class FallbackScraper(
    private val primary: MetadataScraper,
    private val fallback: MetadataScraper
) : MetadataScraper {
    override val isAvailable: Boolean
        get() = primary.isAvailable || fallback.isAvailable

    override suspend fun scrape(item: MediaItem): MediaItem {
        if (primary.isAvailable) {
            try {
                val result = primary.scrape(item)
                if (result.scraped) return result
            } catch (e: Exception) {
                Log.w("FallbackScraper", "Primary failed for ${item.fileName}", e)
            }
        }
        if (fallback.isAvailable) {
            try {
                val result = fallback.scrape(item)
                if (result.scraped) return result
            } catch (e: Exception) {
                Log.w("FallbackScraper", "Fallback failed for ${item.fileName}", e)
            }
        }
        return item.copy(scraped = false, source = "local")
    }
}
