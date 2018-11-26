package model

import com.google.gson.annotations.SerializedName
import dao.Quoter
import org.jetbrains.exposed.sql.ResultRow

data class Quote(
        val id: Int,
        val adder: String,
        val author: String,
        val quote: String,
        @SerializedName("edited_by") val editedBy: String,
        @SerializedName("edited_at") val editedAt: Long
) {

    constructor(row: ResultRow): this(
            row[Quoter.id],
            row[Quoter.adder],
            row[Quoter.author],
            row[Quoter.quote],
            row[Quoter.editedBy] ?: "null",
            row[Quoter.editedAt] ?: -1)


    override fun equals(other: Any?): Boolean {
        return other is Quote && id == other.id
    }

    override fun hashCode(): Int {
        return 7 * id.hashCode()
    }

}