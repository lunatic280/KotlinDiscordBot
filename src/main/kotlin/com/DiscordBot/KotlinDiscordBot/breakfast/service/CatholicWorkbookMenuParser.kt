package com.DiscordBot.KotlinDiscordBot.breakfast.service

import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.util.CellRangeAddress
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.Locale

@Component
class CatholicWorkbookMenuParser {

    fun extractMenuItems(workbookBytes: ByteArray, targetDate: LocalDate): List<String> {
        ByteArrayInputStream(workbookBytes).use { input ->
            WorkbookFactory.create(input).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                val formatter = DataFormatter(Locale.KOREA)
                val evaluator = workbook.creationHelper.createFormulaEvaluator()
                val dateToken = "${targetDate.monthValue}/${targetDate.dayOfMonth}"

                for (row in sheet) {
                    for (cell in row) {
                        val cellText = formatter.formatCellValue(cell, evaluator).trim()
                        if (!matchesDateToken(cellText, dateToken)) {
                            continue
                        }

                        val dateRegion = findMergedRegion(sheet, cell.rowIndex, cell.columnIndex)
                        val firstColumn = dateRegion?.firstColumn ?: cell.columnIndex
                        val startRow = (dateRegion?.lastRow ?: cell.rowIndex) + 1

                        val items = (startRow until startRow + 6)
                            .mapNotNull { rowIndex ->
                                resolveMergedText(sheet, rowIndex, firstColumn, formatter, evaluator)
                            }
                            .map { it.replace('\n', ' ').trim() }
                            .filter { it.isNotBlank() }
                            .distinct()

                        if (items.isNotEmpty()) {
                            return items
                        }
                    }
                }
            }
        }

        throw IllegalArgumentException("Menu not found in workbook for $targetDate")
    }

    private fun matchesDateToken(cellText: String, dateToken: String): Boolean {
        val normalized = cellText.replace(" ", "")
        return normalized.contains(dateToken)
    }

    private fun resolveMergedText(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        rowIndex: Int,
        columnIndex: Int,
        formatter: DataFormatter,
        evaluator: FormulaEvaluator
    ): String? {
        if (rowIndex > sheet.lastRowNum) {
            return null
        }

        val region = findMergedRegion(sheet, rowIndex, columnIndex)
        val targetRow = region?.firstRow ?: rowIndex
        val targetColumn = region?.firstColumn ?: columnIndex
        val row = sheet.getRow(targetRow) ?: return null
        val cell = row.getCell(targetColumn) ?: return null
        return formatter.formatCellValue(cell, evaluator).trim()
    }

    private fun findMergedRegion(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        rowIndex: Int,
        columnIndex: Int
    ): CellRangeAddress? {
        for (index in 0 until sheet.numMergedRegions) {
            val region = sheet.getMergedRegion(index)
            if (region.isInRange(rowIndex, columnIndex)) {
                return region
            }
        }
        return null
    }
}
