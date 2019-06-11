package bakend.model

import bakend.dao.QuoterTable
import com.google.gson.annotations.SerializedName
import org.jetbrains.exposed.sql.ResultRow

data class QuoteV2(
        val id: Int,
        val adder: String,
        val authors: List<String>,
        @SerializedName("dtype") val displayType: String,
        val content: String,
        val date: Long,
        @SerializedName("edited_by") val editedBy: String,
        @SerializedName("edited_at") val editedAt: Long,
        val attachments: List<String>,
        @SerializedName("is_old") val isOld: Boolean
) {

    constructor(db: Pair<QuoterTable, ResultRow>): this(db.first, db.second)

    constructor(table: QuoterTable, row: ResultRow): this(
            row[table.id],
            row[table.adder],
            row[table.authors]
                    .split(";")
                    .filter(String::isNotBlank),
            row[table.dtype],
            row[table.content],
            row[table.date].millis,
            row[table.editedBy] ?: "null",
            row[table.editedAt] ?: -1,
            row[table.attachments]
                    .split(";")
                    .filter(String::isNotBlank),
            row[table.isOld])



    override fun equals(other: Any?): Boolean {
        return other is Quote && id == other.id
    }

    override fun hashCode(): Int {
        return 7 * id.hashCode()
    }

}