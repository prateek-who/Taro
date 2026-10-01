package com.prateek.taro.food

import kotlin.math.abs

data class OcrToken(val text: String, val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2
    val height: Float get() = bottom - top
}

data class LabelValues(
    val kcal: Double?,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val servingGrams: Double?,
    val unit: String = Units.GRAMS,
) {
    val found: Int get() = listOfNotNull(kcal, protein, carbs, fat).size
}

object LabelParser {
    private const val KJ_PER_KCAL = 4.184
    private const val MAX_KCAL_PER_100 = 900.0
    private val number = Regex("""(\d+(?:[.,]\d+)?)""")
    private val per100 = Regex("""100\s?(g|gm|ml)\b""", RegexOption.IGNORE_CASE)
    private val serving = Regex("""serv(ing|e)\s*(size)?[^0-9]{0,12}(\d+(?:[.,]\d+)?)\s?(g|gm|ml)\b""", RegexOption.IGNORE_CASE)

    private data class Row(val tokens: List<OcrToken>) {
        val text: String get() = tokens.joinToString(" ") { it.text }
    }

    private fun rows(tokens: List<OcrToken>): List<Row> {
        val rows = mutableListOf<MutableList<OcrToken>>()
        for (token in tokens.sortedBy { it.centerY }) {
            val row = rows.lastOrNull()?.takeIf { existing ->
                val center = existing.map { it.centerY }.average()
                abs(token.centerY - center) <= existing.maxOf { it.height }.coerceAtLeast(token.height) * 0.55
            }
            if (row != null) row += token else rows += mutableListOf(token)
        }
        return rows.map { Row(it.sortedBy { token -> token.left }) }
    }

    private fun clean(text: String) = text
        .replace('O', '0').replace('o', '0')
        .replace(',', '.')

    private data class Value(val amount: Double, val x: Float, val unit: String)

    private fun unitOf(text: String): String = when {
        text.startsWith("kj") -> "kj"
        text.startsWith("kcal") || text.startsWith("cal") -> "kcal"
        else -> ""
    }

    private fun values(row: Row): List<Value> = row.tokens.flatMapIndexed { index, token ->
        val lower = token.text.lowercase()
        if (lower.contains('%')) return@flatMapIndexed emptyList()
        val digits = if (token.text.any(Char::isDigit)) clean(token.text) else token.text
        val next = row.tokens.getOrNull(index + 1)?.text?.lowercase()?.trim().orEmpty()
        number.findAll(digits).mapNotNull { match ->
            val amount = match.value.toDoubleOrNull() ?: return@mapNotNull null
            val after = lower.substring(minOf(lower.length, match.range.last + 1)).trim()
            Value(amount, token.centerX, unitOf(after).ifEmpty { if (after.isEmpty()) unitOf(next) else "" })
        }.toList()
    }

    private fun pick(row: Row, column: Float?): Value? {
        val found = values(row).filterNot { it.amount == 100.0 && per100.containsMatchIn(row.text) }
        if (found.isEmpty()) return null
        return if (column != null) found.minBy { abs(it.x - column) } else found.first()
    }

    private fun matches(text: String, keys: List<String>, exclude: List<String> = emptyList()): Boolean {
        val lower = text.lowercase()
        return keys.any { lower.contains(it) } && exclude.none { lower.contains(it) }
    }

    fun parse(tokens: List<OcrToken>): LabelValues {
        val rows = rows(tokens)
        val column = rows.firstNotNullOfOrNull { row ->
            row.tokens.firstOrNull { per100.containsMatchIn(it.text) }?.centerX
                ?: row.tokens.zipWithNext().firstOrNull { (a, b) -> a.text.trim() == "100" && b.text.lowercase().startsWith("g") }?.first?.centerX
        }

        fun nutrient(keys: List<String>, exclude: List<String> = emptyList()): Double? =
            rows.firstOrNull { matches(it.text, keys, exclude) && values(it).isNotEmpty() }?.let { pick(it, column)?.amount }

        val energyRows = rows.filter { matches(it.text, listOf("energy", "calorie", "kcal")) && values(it).isNotEmpty() }
        val kcal = energyRows.firstNotNullOfOrNull { row ->
            val all = values(row)
            all.firstOrNull { it.unit == "kcal" }?.amount
                ?: if (row.text.lowercase().contains("kcal")) pick(row, column)?.amount else null
        } ?: energyRows.firstNotNullOfOrNull { row ->
            val value = pick(row, column) ?: return@firstNotNullOfOrNull null
            val isKj = value.unit == "kj" || row.text.lowercase().contains("kj") || value.amount > MAX_KCAL_PER_100
            if (isKj) value.amount / KJ_PER_KCAL else value.amount
        }

        val servingGrams = rows.firstNotNullOfOrNull { row -> serving.find(row.text)?.groupValues?.get(3)?.replace(',', '.')?.toDoubleOrNull() }

        return LabelValues(
            kcal = kcal?.takeIf { it in 0.0..MAX_KCAL_PER_100 },
            protein = nutrient(listOf("protein"))?.takeIf { it <= 100 },
            carbs = nutrient(listOf("carbohydrate", "carbs", "total carb"), listOf("of which"))?.takeIf { it <= 100 },
            fat = nutrient(listOf("total fat", "fat"), listOf("saturated", "trans", "mono", "poly", "sat."))?.takeIf { it <= 100 },
            servingGrams = servingGrams?.takeIf { it in 1.0..2_000.0 },
            unit = if (rows.firstNotNullOfOrNull { per100.find(it.text) }?.groupValues?.get(1).equals(Units.ML, ignoreCase = true)) Units.ML else Units.GRAMS,
        )
    }
}
