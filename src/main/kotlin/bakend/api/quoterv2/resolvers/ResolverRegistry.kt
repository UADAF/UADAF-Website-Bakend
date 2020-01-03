package bakend.api.quoterv2.resolvers

import bakend.api.quoterv2.resolvers.interfaces.Resolver
import bakend.dao.QuoterV2
import bakend.dao.getTable
import org.jetbrains.exposed.sql.transactions.transaction


typealias ResolverCreator = (String) -> Resolver
typealias ResolverSpec = Pair<(String) -> Boolean, ResolverCreator>

object ResolverRegistry {

    private val resolvers = mutableListOf<ResolverSpec>()


    fun registerSimple(name: String, resolver: Resolver): ResolverSpec {
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

    fun getResolver(spec: String): Resolver {
        return requireNotNull(resolvers.firstOrNull { it.first(spec) }).second(spec)
    }

    init {
        register({ it.startsWith("uadaf") }) {
            val table = if (":" in it) {
                transaction {
                    requireNotNull(getTable(it.split(":", limit = 2)[1], false))
                }
            } else {
                QuoterV2
            }
            QuoterV2APIDatabaseResolver(table)
        }
        registerSimple("modder", ModderPwResolver)
    }

}