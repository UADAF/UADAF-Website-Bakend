import api.ITHApi.ith
import api.MusicApi.music
import api.QuoterApi.quoter
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
import utils.jsonParser
import web.QuoterWeb.quoterWeb
import java.io.File

object Core {

    val config =  jsonParser.parse(File("config.json").readText()).obj
    private val connectUrl = "jdbc:mysql://${config["host"]!!.str}:3306/${config["database"]!!.str}?useUnicode=yes&characterEncoding=UTF-8&autoReconnect=true"

    @JvmStatic
    fun main(args: Array<String>) {

        val server = embeddedServer(Netty, 6741){
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                }
            }
            routing{
                route("api") {
                    quoter()
                    ith()
                    music()
                }
                route("web") {
                    quoterWeb()
                }
                static("static") {
                    files("styles")
                }
            }
        }
        Database.connect(connectUrl, "com.mysql.jdbc.Driver", config["user"]!!.str, config["pass"]!!.str)
        server.start(wait = true)
    }

}