package bakend.model

import com.google.gson.annotations.SerializedName
import bakend.dao.QuoterV2
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

    constructor(row: ResultRow): this(
            row[QuoterV2.id],
            row[QuoterV2.adder],
            row[QuoterV2.authors]
                    .split(";")
                    .filter(String::isNotBlank),
            row[QuoterV2.dtype],
            row[QuoterV2.content],
            row[QuoterV2.date].millis,
            row[QuoterV2.editedBy] ?: "null",
            row[QuoterV2.editedAt] ?: -1,
            row[QuoterV2.attachments]
                    .split(";")
                    .filter(String::isNotBlank),
            row[QuoterV2.isOld])



    override fun equals(other: Any?): Boolean {
        return other is Quote && id == other.id
    }

    override fun hashCode(): Int {
        return 7 * id.hashCode()
    }

}