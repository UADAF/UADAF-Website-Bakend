package api.quoterv2.resolvers

import dao.QuoterV2
import dao.getTable
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.transactions.transaction
import utils.StatusCodeException


typealias ResolverCreator = (String) -> IQuoterV2APIResolver
typealias ResolverSpec = Pair<(String) -> Boolean, ResolverCreator>

object ResolverRegistry {

    private val resolvers = mutableListOf<ResolverSpec>()


    fun registerSimple(name: String, resolver: IQuoterV2APIResolver): ResolverSpec {
        return register({ it == name }) { resolver }
    }

    fun register(predicate: (String) -> Boolean, creator: ResolverCreator): ResolverSpec {
        return register(predicate to creator)
    }

    fun register(s: ResolverSpec): ResolverSpec {
        resolvers.add(s)
        return s
    }

    fun unregister(s: ResolverSpec): ResolverSpec {
        resolvers.remove(s)
        return s
    }

    fun getResolver(spec: String): IQuoterV2APIResolver {
        return resolvers.first { it.first(spec) }.second(spec)
    }

    init {
        register({ it.startsWith("uadaf") }) {
            val table = if (":" in it) {
                transaction {
                    getTable(it.split(":", limit = 2)[1], false) ?:
                            throw StatusCodeException(HttpStatusCode.ExpectationFailed)
                }
            } else {
                QuoterV2
            }
            QuoterV2APIDatabaseResolver(table)
        }
        registerSimple("modder", ModderPwResolver)
    }

}