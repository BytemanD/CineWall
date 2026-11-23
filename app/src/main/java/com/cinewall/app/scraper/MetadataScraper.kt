package com.cinewall.app.scraper

import com.cinewall.app.model.MediaItem
import kotlinx.coroutines.Deferred

/** 元数据刮削器接口：从文件名/外部源抓取海报、概述、评分等 */
interface MetadataScraper {
    /** 刮削单个媒体项，返回补充了元数据的副本 */
    suspend fun scrape(item: MediaItem): MediaItem

    /** 批量刮削 */
    suspend fun scrapeAll(items: List<MediaItem>): List<MediaItem> =
        items.map { scrape(it) }

    /** 是否可用（如是否配置了 API Key） */
    val isAvailable: Boolean
}

/** 空刮削器：不联网，仅保留本地信息，用于未配置 API Key 时的 fallback */
object LocalScraper : MetadataScraper {
    override val isAvailable: Boolean = true

    override suspend fun scrape(item: MediaItem): MediaItem =
        item.copy(scraped = false, source = "local")
}
