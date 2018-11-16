package model

import mysql.Quoter
import org.jetbrains.exposed.sql.ResultRow

data class Quote(
        val id: Int,
        val adder: String,
        val author: String,
        val text: String,
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

}