package api.quoterv2.resolvers

import api.quoterv2.IQuoterV2APIResolver
import api.quoterv2.QuoterV2APIDatabaseResolver
import dao.QuoterV2
import dao.getTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.function.Predicate


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
                    getTable(it.split(":", limit = 2)[1], true)!!
                }
            } else {
                QuoterV2
            }
            QuoterV2APIDatabaseResolver(table)
        }
        registerSimple("modder", ModderPwResolver)
    }

}