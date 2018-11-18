package model

import kotlinx.html.DIV
import kotlinx.html.HTML
import kotlinx.html.TagConsumer
import kotlinx.html.stream.createHTML
import mysql.Quoter
import org.jetbrains.exposed.sql.ResultRow
import kotlinx.html.*

data class Quote(
        val id: Int,
        val adder: String,
        val author: String,
        val quote: String,
        val editedBy: String,
        val editedAt: Long
) {

    constructor(row: ResultRow): this(
            row[Quoter.id],
            row[Quoter.adder],
            row[Quoter.author],
            row[Quoter.quote],
            (row[Quoter.editedBy] ?: "null"),
            (row[Quoter.editedAt] ?: -1))


    override fun equals(other: Any?): Boolean {
        return other is Quote && id == other.id
    }

    override fun hashCode(): Int {
        return 7 * id.hashCode()
    }

}