import api.ITHApi.ith
import api.QuoterApi.quoter
import com.gt22.uadam.utils.str
import config.config
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jetbrains.exposed.sql.Database

object Core {

  //  private val connectUrl = "jdbc:mysql://${config["host"]!!.str}:3306/${config["database"]!!.str}?useUnicode=yes&characterEncoding=UTF-8&autoReconnect=true"
    private val connectUrl = "jdbc:mysql://192.168.0.101:3306/UADAF_DB?useUnicode=yes&characterEncoding=UTF-8&autoReconnect=true"

    @JvmStatic
    fun main(args: Array<String>) {
        val server = embeddedServer(Netty, 6741){
            routing{
                route("api") {
                    quoter()
                    ith()
                }
            }
        }
      //  Database.connect(connectUrl, "com.mysql.jdbc.Driver", config["user"]!!.str, config["pass"]!!.str)
        Database.connect(connectUrl, "com.mysql.jdbc.Driver", "clara", "6741")
        server.start(wait = true)
    }

}