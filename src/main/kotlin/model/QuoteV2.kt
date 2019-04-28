package model

import com.google.gson.annotations.SerializedName
import dao.Quoter
import dao.QuoterV2
import org.jetbrains.exposed.sql.ResultRow
import org.joda.time.DateTime

data class QuoteV2(
        val id: Int,
        val adder: String,
        val author: String,
        val content: String,
        val date: Long,
        @SerializedName("edited_by") val editedBy: String,
        @SerializedName("edited_at") val editedAt: Long,
        val attachments: String
) {

    constructor(row: ResultRow): this(
            row[QuoterV2.id],
            row[QuoterV2.adder],
            row[QuoterV2.author],
            row[QuoterV2.content],
            row[QuoterV2.date].millis,
            row[QuoterV2.editedBy] ?: "null",
            row[QuoterV2.editedAt] ?: -1,
            row[QuoterV2.attachments])



    override fun equals(other: Any?): Boolean {
        return other is Quote && id == other.id
    }

    override fun hashCode(): Int {
        return 7 * id.hashCode()
    }

}