package com.DiscordBot.KotlinDiscordBot.breakfast.service

import com.DiscordBot.KotlinDiscordBot.breakfast.domain.BreakfastInfo
import com.DiscordBot.KotlinDiscordBot.breakfast.domain.BreakfastUniversity
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap

@Service
open class CatholicBreakfastClient(
    webClientBuilder: WebClient.Builder,
    private val workbookMenuParser: CatholicWorkbookMenuParser
) {

    private val log = LoggerFactory.getLogger(CatholicBreakfastClient::class.java)
    private val client = webClientBuilder.build()
    private val monthCache = ConcurrentHashMap<YearMonth, CachedCatholicMonth>()

    fun fetch(targetDate: LocalDate): BreakfastInfo {
        val monthlySource = resolveMonthlySource(targetDate)
        val menuItems = workbookMenuParser.extractMenuItems(monthlySource.workbookBytes, targetDate)
        val bodyText = monthlySource.articleBodyText

        return BreakfastInfo(
            university = BreakfastUniversity.CATHOLIC,
            date = targetDate,
            time = extractDetail(bodyText, "운영시간"),
            location = extractDetail(bodyText, "운영장소"),
            price = extractDetail(bodyText, "식사가격"),
            menuItems = menuItems,
            sourceUrls = listOf(monthlySource.articleUrl, monthlySource.attachmentUrl),
            note = extractOptionalNotes(bodyText)
        )
    }

    private fun resolveMonthlySource(targetDate: LocalDate): CatholicMonthlySource {
        val monthKey = YearMonth.from(targetDate)
        val now = currentInstant()
        val cached = monthCache[monthKey]

        if (cached != null && !needsRefresh(cached, now)) {
            log.info("Catholic breakfast cache hit. month={}, fetchedAt={}", monthKey, cached.fetchedAt)
            return cached.source
        }

        return synchronized(this) {
            val refreshedNow = currentInstant()
            val latest = monthCache[monthKey]
            if (latest != null && !needsRefresh(latest, refreshedNow)) {
                log.info("Catholic breakfast cache hit after lock. month={}, fetchedAt={}", monthKey, latest.fetchedAt)
                return@synchronized latest.source
            }

            val source = loadMonthlySource(targetDate)
            monthCache[monthKey] = CachedCatholicMonth(source, refreshedNow)
            log.info("Catholic breakfast cache refreshed. month={}, fetchedAt={}", monthKey, refreshedNow)
            source
        }
    }

    protected open fun currentInstant(): Instant = Instant.now()

    protected open fun refreshInterval(): Duration = DEFAULT_REFRESH_INTERVAL

    protected open fun loadMonthlySource(targetDate: LocalDate): CatholicMonthlySource {
        val article = findMonthlyArticle(targetDate)
        val articleDocument = fetchDocument(article.articleUrl)
        val articleContent = articleDocument.selectFirst(".fr-view")
            ?: throw IllegalStateException("Catholic article content not found")

        val attachmentHref = articleDocument.selectFirst("a.file-down-btn.xlsx[href*=\"mode=download\"]")
            ?.attr("href")
            ?: throw IllegalStateException("Catholic workbook attachment not found")
        val attachmentUrl = absoluteUrl(attachmentHref)
        val workbookBytes = client.get()
            .uri(attachmentUrl)
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block()
            ?: throw IllegalStateException("Catholic workbook download failed")

        return CatholicMonthlySource(
            articleUrl = article.articleUrl,
            attachmentUrl = attachmentUrl,
            articleBodyText = articleContent.text(),
            workbookBytes = workbookBytes
        )
    }

    private fun findMonthlyArticle(targetDate: LocalDate): CatholicArticle {
        val searchUrl = "$CATHOLIC_BOARD_URL?mode=list&srCategoryId=20&srSearchKey=title&srSearchVal=천원의 아침밥&article.offset=0&articleLimit=20"
        val searchDocument = fetchDocument(searchUrl)
        val expectedTitlePart = "${targetDate.monthValue}월 메뉴 안내"

        val articleLink = searchDocument.select("a.b-title[data-article-no]")
            .firstOrNull { it.text().contains(expectedTitlePart) }
            ?: throw IllegalArgumentException("Catholic article not found for ${targetDate.year}-${targetDate.monthValue}")

        val articleTitle = articleLink.text().trim()
        val articleUrl = absoluteUrl(articleLink.attr("href"))
        log.info("Catholic breakfast article selected. date={}, title={}", targetDate, articleTitle)
        return CatholicArticle(articleTitle, articleUrl)
    }

    private fun fetchDocument(url: String): Document {
        val html = client.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
            ?: throw IllegalStateException("HTTP response body is empty for $url")
        return Jsoup.parse(html, url)
    }

    private fun extractDetail(text: String, label: String): String {
        val regex = Regex("$label\\s*:\\s*(.+?)(?=\\s*[0-9]+\\.|$)")
        return regex.find(text)?.groupValues?.get(1)?.trim()
            ?: throw IllegalStateException("Catholic article detail missing: $label")
    }

    private fun extractOptionalNotes(text: String): String? {
        val regex = Regex("\\*\\s*(.+?)(?=\\*|$)")
        return regex.findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .joinToString(" | ")
            .ifBlank { null }
    }

    private fun needsRefresh(cached: CachedCatholicMonth, now: Instant): Boolean {
        return now.isAfter(cached.fetchedAt.plus(refreshInterval()))
    }

    private fun absoluteUrl(href: String): String {
        val normalized = href.replace("&amp;", "&")
        return when {
            normalized.startsWith("http") -> normalized
            normalized.startsWith("?") -> "$CATHOLIC_BOARD_URL$normalized"
            normalized.startsWith("/") -> "$CATHOLIC_BASE_URL$normalized"
            else -> "$CATHOLIC_BOARD_URL?$normalized"
        }
    }

    data class CatholicArticle(
        val title: String,
        val articleUrl: String
    )

    data class CatholicMonthlySource(
        val articleUrl: String,
        val attachmentUrl: String,
        val articleBodyText: String,
        val workbookBytes: ByteArray
    )

    private data class CachedCatholicMonth(
        val source: CatholicMonthlySource,
        val fetchedAt: Instant
    )

    companion object {
        private const val CATHOLIC_BASE_URL = "https://www.catholic.ac.kr"
        private const val CATHOLIC_BOARD_URL = "$CATHOLIC_BASE_URL/ko/campuslife/notice.do"
        private val DEFAULT_REFRESH_INTERVAL: Duration = Duration.ofHours(6)
    }
}
