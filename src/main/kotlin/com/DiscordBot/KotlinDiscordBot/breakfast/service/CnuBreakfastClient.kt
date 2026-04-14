package com.DiscordBot.KotlinDiscordBot.breakfast.service

import com.DiscordBot.KotlinDiscordBot.breakfast.domain.BreakfastInfo
import com.DiscordBot.KotlinDiscordBot.breakfast.domain.BreakfastUniversity
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class CnuBreakfastClient(
    webClientBuilder: WebClient.Builder
) {

    private val log = LoggerFactory.getLogger(CnuBreakfastClient::class.java)
    private val client = webClientBuilder.build()

    fun fetch(targetDate: LocalDate): BreakfastInfo {
        val menuUrl = "$CNU_MENU_URL?searchYmd=${targetDate.format(CNU_DATE_FORMAT)}&searchLang=OCL04.10&searchView=cafeteria&searchCafeteria=OCL03.02"
        val html = client.get()
            .uri(menuUrl)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
            ?: throw IllegalStateException("CNU menu page is empty")
        val document = Jsoup.parse(html, menuUrl)
        val table = document.selectFirst("table.menu-tbl")
            ?: throw IllegalStateException("CNU menu table not found")

        val headerNames = extractHeaderNames(table)
        val bodyRows = buildTableGrid(table.select("tbody > tr"), headerNames.size + 2)
        val breakfastRow = bodyRows.firstOrNull { row ->
            row.getOrNull(0)?.text == "조식" && row.getOrNull(1)?.text == "학생"
        } ?: throw IllegalArgumentException("CNU breakfast row not found for $targetDate")

        val menuColumnIndex = breakfastRow.indexOfFirst { cell ->
            cell.text.contains("(1000)")
        }
        if (menuColumnIndex < 2) {
            throw IllegalArgumentException("CNU KRW 1,000 breakfast not available for $targetDate")
        }

        val cafeteria = headerNames[menuColumnIndex - 2]
        val menuCell = breakfastRow[menuColumnIndex]
        val menuItems = extractMenuItems(menuCell.element)

        log.info("CNU breakfast selected. date={}, cafeteria={}", targetDate, cafeteria)
        return BreakfastInfo(
            university = BreakfastUniversity.CNU,
            date = targetDate,
            time = "08:00 ~ 09:00",
            location = "$cafeteria 학생식당",
            price = menuCell.element.selectFirst("h3")?.text()?.trim() ?: "정식(1000)",
            menuItems = menuItems,
            sourceUrls = listOf(menuUrl, CNU_OPERATION_NOTICE_URL)
        )
    }

    internal fun extractHeaderNames(table: Element): List<String> {
        return table.select("thead tr").lastOrNull()
            ?.select("th")
            ?.drop(2)
            ?.map { it.text().trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    internal fun extractMenuItems(menuCell: Element): List<String> {
        val paragraph = menuCell.selectFirst("p")
        if (paragraph == null) {
            return emptyList()
        }

        return paragraph.html()
            .split("<br>", "<br/>", "<br />")
            .map { Jsoup.parse(it).text().trim() }
            .filter { it.isNotBlank() }
    }

    internal fun buildTableGrid(rows: List<Element>, totalColumns: Int): List<List<TableCell>> {
        val pendingRowspans = mutableMapOf<Int, ActiveCell>()
        val grid = mutableListOf<List<TableCell>>()

        for (row in rows) {
            val current = MutableList<TableCell?>(totalColumns) { null }
            var columnIndex = 0

            while (columnIndex < totalColumns) {
                val active = pendingRowspans[columnIndex]
                if (active != null) {
                    current[columnIndex] = active.cell
                    active.remainingRows -= 1
                    if (active.remainingRows <= 0) {
                        pendingRowspans.remove(columnIndex)
                    }
                }
                columnIndex += 1
            }

            columnIndex = 0
            for (element in row.select("> th, > td")) {
                while (columnIndex < totalColumns && current[columnIndex] != null) {
                    columnIndex += 1
                }
                if (columnIndex >= totalColumns) {
                    break
                }

                val rowspan = element.attr("rowspan").toIntOrNull() ?: 1
                val colspan = element.attr("colspan").toIntOrNull() ?: 1
                val cell = TableCell(
                    text = element.text().trim(),
                    element = element
                )

                repeat(colspan) { offset ->
                    val targetColumn = columnIndex + offset
                    current[targetColumn] = cell
                    if (rowspan > 1) {
                        pendingRowspans[targetColumn] = ActiveCell(cell, rowspan - 1)
                    }
                }
                columnIndex += colspan
            }

            grid += current.map { it ?: TableCell("", row) }
        }

        return grid
    }

    internal data class TableCell(
        val text: String,
        val element: Element
    )

    private data class ActiveCell(
        val cell: TableCell,
        var remainingRows: Int
    )

    companion object {
        private val CNU_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        private const val CNU_MENU_URL = "https://mobileadmin.cnu.ac.kr/food/index.jsp"
        private const val CNU_OPERATION_NOTICE_URL = "https://plus.cnu.ac.kr/_prog/_board/?code=sub07_0701&menu_dvs_cd=0701&mode=V&no=2512171&site_dvs_cd=kr&upr_ntt_no=2512171"
    }
}
