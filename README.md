# CineWall

安卓电视优先本地视频播放器（TV-first Android Video Player）。

- **Kotlin + Compose for TV** 界面，遥控器/手柄导航优先
- **多本地目录扫描**：支持选择多个本地磁盘/U盘文件夹作为媒体库
- **自动识别视频**：按扩展名识别，自动分为“电影”和“剧集”两类（文件名匹配 `SxxExx` / `1x01` / `第xx集` 等）
- **自动刮削元数据**：TMDb 抓取海报、概述、评分、年份、类型；找不到时自动用豆瓣兜底
- **海报墙**：Compose for TV `LazyVerticalGrid` 海报网格，支持聚焦
- **主题切换**：明亮 / 夜间 / 跟随系统 + 6 套 TV 配色（经典黑金 / 拜占庭 / 豆沙墨 / 松针琥珀 / 石榴奶杏 / 藤紫，移植自 Luma 主题方案）
- **本地播放**：ExoPlayer 纯黑全屏播放（不跟随主题，避免白边）；自动同名字幕、自动下一集、记住速度、音轨/字幕切换、
- 画面比例、亮度音量
- **手势操作**：单击显示/隐藏控制条，双击中间暂停，双击左右快退快进（步长可配）；左侧上下拖动调亮度、右侧上下拖动调音量，中间左右拖动预览快进/快退（上方显示时间，松手才跳转）；设置面板只通过右上角配置图标或遥控器菜单键打开
- **友好的目录显示**：媒体文件夹显示 `本地磁盘:/路径` 或 `U盘:/路径`，中文正常显示，可逐个移除
- **本地数据库**：Room 存储媒体元数据，Flow 响应式刷新 UI

## 项目结构

```
app/src/main/java/com/cinewall/app/
├── db/               # Room 数据库：AppDatabase, MediaDao, MediaEntity, TypeConverters
├── di/               # 简单 DI：AppModule, AppScopeHolder
├── model/            # 数据模型：MediaItem, MediaType, ScanResult
├── repository/       # 仓库：MediaRepository, LocalMediaRepository（Room + SharedPreferences）
├── scanner/          # 扫描：MediaScanner, TvShowMatcher, FolderPicker（SAF 目录选择）
├── scraper/          # 刮削：TmdbScraper, DoubanScraper, FallbackScraper, TitleMatcher
├── ui/
│   ├── theme/        # Compose 主题：Color, Type, Theme（明暗）
│   ├── navigation/   # Navigation Compose 导航图
│   ├── screen/       # 屏幕：MainScreen（海报墙）, DetailScreen（详情）, SettingsScreen（设置）, PosterCard, PosterGrid
│   ├── player/       # 播放器：VideoPlayer（ExoPlayer）, PlayerScreen, PlayerActivity
│   └── MainActivity.kt
├── utils/            # DocumentsPathUtils（SAF Uri → 路径）
└── ...
```

## 环境要求

- Android Studio / AGP 8.5+
- compileSdk 36 / targetSdk 36
- minSdk 24
- Kotlin 2.0.20
- Jetpack Compose for TV (`androidx.tv:tv-material`, `androidx.tv:tv-foundation`)
- ExoPlayer 2.18.7
- Room 2.6.1, Coil 2.6.0

## 权限

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.ACCESS_MEDIA_LOCATION" />
```

电视启动器入口已声明（`android.intent.category.LEANBACK_LAUNCHER`），适配 Android TV 桌面。

## 使用

1. 打开“设置” → “添加文件夹”，选择一个或多个本地磁盘/U盘目录（SAF 文档树选择，会持久化 URI 权限）。
2. 点击“扫描媒体库”，自动识别视频并分为电影/剧集，写入 Room 数据库。
3. 点击“刮削元数据”，自动补全海报与详情（密钥已随应用构建，无需在界面填写）。
4. 主页海报墙点击影片 → 播放（ExoPlayer 全屏）。

## 刮削密钥配置

密钥在**构建期**注入到 `BuildConfig`，构建出的 APK 直接可用：

- 项目根目录 `cinewall.properties`（可参考 `cinewall.properties.example`）：
  ```properties
  TMDB_API_KEY=...
  DOUBAN_API_KEY=...
  ```
- 或用环境变量覆盖（优先级更高）：`TMDB_API_KEY=... DOUBAN_API_KEY=... ./gradlew assembleDebug`

支持 TMDb 传统 `api_key` 和 `eyJ` 开头的 Read Access Token（Bearer）。TMDb 无匹配或请求失败时自动切换到豆瓣 API v2；设置页仍可临时覆盖 TMDb 密钥并一键恢复构建配置。TMDb 元数据语言可在设置页「刮削」里选择（简体中文 / 繁体中文 / English / 日本語），配置语言匹配不到时会自动用 `en-US` 再搜一次；「重新刮削全部」可覆盖已有结果。
5. 顶部标签切换“电影/剧集”；设置中切换“主题模式”（明亮/夜间/跟随系统）。

## 构建

```bash
cd /home/byteman/code/CineWall
./gradlew assembleDebug
```

首次构建会下载 Gradle 与 Android Gradle Plugin，请保持网络畅通。

## 注意

- TMDb 刮削需自行申请 [TMDb API Key](https://www.themoviedb.org/settings/api) 并填入设置页。
- Android 10+ 对共享存储访问受限，部分目录可能无法直接转为文件路径，此时会退回到 `MediaStore` 查询。
- 本版本为框架实现，播放控件、详情页可按需进一步丰富。
