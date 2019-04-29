package dao

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.booleanParam

object IthFavorites : Table("ith_favorites") {
    val user = text("user")
    val storyId = integer("story_id")
}

object Pictures : Table("pictures") {
    val name = varchar("name", 255).primaryKey()
    val base64 = text("base64")
}

object Quoter : Table("quoter") {
    val id = integer("id").primaryKey().autoIncrement()
    val adder = text("adder")
    val author = text("author")
    val quote = text("quote")
    val editedBy = text("edited_by").nullable()
    val editedAt = long("edited_at").nullable()
}

object  Tokens : Table("tokens") {
    val token = varchar("token", 255).primaryKey()
    val userId = integer("user_id")
    val issued_at = datetime("issued_at")
}

object Users : Table("users") {
    val user = varchar("user", 255).primaryKey()
    val story = integer("story")
    val rate = integer("rate")
}

object QuoterV2 : Table("quoterv2") {
    val id = integer("id").primaryKey().autoIncrement()
    val adder = text("adder")
    val author = text("author")
    val date = datetime("date")
    val content = text("content")
    val editedBy = text("edited_by").nullable()
    val editedAt = long("edited_at").nullable()
    val previousContent = text("previous_content").nullable()
    val attachments = text("attachments")
    val isOld = bool("is_old")
}

object Attachments : Table("attachments") {
    val id = text("id")
    val type = text("type")
    val data = blob("data")
}
