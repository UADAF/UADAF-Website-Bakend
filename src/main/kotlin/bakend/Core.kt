package bakend

import bakend.api.AttachmentsApi.attachments
import bakend.api.ITHApi.ith
import bakend.api.MusicApi.music
import bakend.api.QuoterApi.quoter
import bakend.api.quoterv2.QuoterV2Api.quoterV2
import com.google.common.hash.Hashing
import com.gt22.uadam.utils.obj
import com.gt22.uadam.utils.str
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jetbrains.exposed.sql.Database
import bakend.utils.jsonParser
import bakend.web.QuoterWeb.quoterWeb
import java.io.File
import java.nio.charset.StandardCharsets


val config = jsonParser.parse(File("config.json").readText()).obj
private val connectUrl = "jdbc:mysql://${config["host"]!!.str}:3306/${config["database"]!!.str}?useUnicode=yes&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=UTC"

fun main(args: Array<String>) {

    val server = embeddedServer(Netty, 6741) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
        routing {
            route("bakend/apiend/api") {
                quoter()
                ith()
                music()

                route("v2") {
                    attachments()
                    quoterV2()
                }
            }
            route("bakend/webend/web") {
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
