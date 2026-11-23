package com.yg.pvtodo.papers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PaperRepository(private val dao: PaperDao, private val fetcher: PaperFetcher) {
    fun observeAll(): Flow<List<PaperEntity>> = dao.observeAll()
    suspend fun hasAny(): Boolean = dao.count() > 0
    suspend fun refresh(): List<Paper> = withContext(Dispatchers.IO) {
        dao.clearUnfav()
        val fresh = fetcher.fetchAll()
        val known = dao.allDois().toSet()
        val entities = fresh.map { p ->
            PaperEntity(
                doi = p.doi ?: ("nodoi:" + p.title.hashCode()),
                title = p.title, authors = p.authors, abstract = p.abstract,
                published = p.published, link = p.link, source = p.source,
                journal = p.journal, tier = p.tier.name
            )
        }
        dao.insertAll(entities)
        fresh.filter { (it.doi ?: ("nodoi:" + it.title.hashCode())) !in known }
    }

    suspend fun setFav(doi: String, fav: Boolean) = dao.setFav(doi, fav)
}
