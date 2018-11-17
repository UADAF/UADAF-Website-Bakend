import api.ITHApi.ith
import api.MusicApi.music
import api.QuoterApi.quoter
import com.gt22.uadam.utils.str
import config.config
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jetbrains.exposed.sql.Database

object Core {

    private val connectUrl = "jdbc:mysql://${config["host"]!!.str}:3306/${config["database"]!!.str}?useUnicode=yes&characterEncoding=UTF-8&autoReconnect=true"

    @JvmStatic
    fun main(args: Array<String>) {
        val server = embeddedServer(Netty, 6741){
            routing{
                route("api") {
                    quoter()
                    ith()
                    music()
                }
            }
        }
        Database.connect(connectUrl, "com.mysql.jdbc.Driver", config["user"]!!.str, config["pass"]!!.str)
        server.start(wait = true)
    }

}