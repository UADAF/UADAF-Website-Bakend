package bakend

import bakend.api.ITHApi.ith
import bakend.api.MusicApi.music
import bakend.api.QuoterApi.quoter
import bakend.api.quoterv2.QuoterV2Api.quoterV2
import com.google.common.hash.Hashing
import com.gt22.uadam.utils.obj
import com.gt22.uadam.utils.str
import org.jetbrains.exposed.sql.Database
import bakend.web.QuoterWeb.quoterWeb
import com.google.gson.JsonParser
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import java.io.File
import java.nio.charset.StandardCharsets


val config = JsonParser.parseString(File("config.json").readText()).obj
val jdbcParams = config["params"] ?: ""
private val connectUrl = "jdbc:mysql://${config["host"]!!.str}:3306/${config["database"]!!.str}?useUnicode=yes&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=UTC$jdbcParams"

fun main(args: Array<String>) {

    val server = embeddedServer(Netty, 6741) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
        install(CORS) {
            anyHost()
        }
        routing {
            route("api") {
                quoter()
                ith()
                music()

                route("v2") {
                    quoterV2()
                }
            }
            route("web") {
                quoterWeb()
            }
            static("static") {
                files("styles")
            }
        }
    }
    Database.connect(connectUrl, "com.mysql.cj.jdbc.Driver", config["user"]!!.str, config["pass"]!!.str)
    server.start(wait = true)
}

fun verifyKey(key: String): Boolean {
    return Hashing.sha256().hashString(key, StandardCharsets.UTF_8).toString() ==
            "bf077926f1f26e2e3552001461c1e51ec078c7d488f1519bd570cc86f0efeb1a"
}
