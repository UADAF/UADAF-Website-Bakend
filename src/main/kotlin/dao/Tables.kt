package dao

import org.jetbrains.exposed.sql.Table

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
    val issuedAt = datetime("issued_at")
}

object Users : Table("users") {
    val user = varchar("user", 255).primaryKey()
    val story = integer("story")
    val rate = integer("rate")
}

object Attachments : Table("attachments") {
    val id = text("id")
    val type = text("type")
    val data = blob("data")
}
