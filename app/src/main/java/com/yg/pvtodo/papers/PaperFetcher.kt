package com.yg.pvtodo.papers

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object PaperFetcher {
    // 三类关键词:必须同时命中才算"光伏功率预测"
    private val PV_TERMS = listOf(
        "photovoltaic", "pv ", "solar power", "solar energy", "solar plant",
        "pv system", "pv power", "solar irradiance"
    )
    private val POWER_TERMS = listOf(
        "power forecasting", "power prediction", "power output", "power generation",
        "generation forecasting", "output prediction", "irradiance forecasting",
        "power estimation"
    )
    private val METHOD_TERMS = listOf(
        "forecasting", "prediction", "deep learning", "neural network", "lstm",
        "gru", "transformer", "cnn", "attention", "machine learning",
        "short-term", "short term", "ultra-short-term", "nowcasting", "ai "
    )

    // 每次刷新轮换的检索词(保证有新内容)
    private val QUERIES = listOf(
        "photovoltaic power forecasting deep learning",
        "PV power prediction LSTM",
        "solar power forecasting transformer",
        "photovoltaic output prediction neural network",
        "solar irradiance forecasting deep learning",
        "PV generation forecasting short-term",
        "photovoltaic power nowcasting CNN",
        "solar power prediction attention model"
    )

    private fun score(text: String): Int {
        val t = text.lowercase()
        val hasPv = PV_TERMS.any { t.contains(it) }
        val hasPower = POWER_TERMS.any { t.contains(it) }
        val hasMethod = METHOD_TERMS.any { t.contains(it) }
        // 三类必须全中
        if (!(hasPv && hasPower && hasMethod)) return 0
        // 命中越多越靠前
        return PV_TERMS.count { t.contains(it) } +
               POWER_TERMS.count { t.contains(it) } * 2 +
               METHOD_TERMS.count { t.contains(it) }
    }

    fun fetchAll(): List<Paper> {
        val idx = (System.currentTimeMillis() / (6 * 3600 * 1000L)).toInt()
        val q = QUERIES[idx.mod(QUERIES.size)]
        val out = mutableListOf<Pair<Paper, Int>>()
        try { arxiv(q).forEach { p -> val s = score(p.title + " " + p.abstract); if (s > 0) out += p to s } } catch (_: Exception) {}
        try { crossref(q).forEach { p -> val s = score(p.title + " " + p.abstract); if (s > 0) out += p to s } } catch (_: Exception) {}
        return out.sortedByDescending { it.second }.map { it.first }.distinctBy { it.title.lowercase() }
    }

    private fun httpGet(url: String, ua: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", ua)
        c.connectTimeout = 15000
        c.readTimeout = 15000
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    private fun arxiv(q: String): List<Paper> {
        val query = URLEncoder.encode("$q AND (cat:cs.LG OR cat:stat.ML OR cat:cs.CV OR cat:eess.SP)", "UTF-8")
        val xml = httpGet(
            "http://export.arxiv.org/api/query?search_query=$query&sortBy=submittedDate&sortOrder=descending&max_results=25",
            "PVTodo/1.0"
        )
        val entries = xml.split("<entry>").drop(1)
        fun strip(s: String): String = s.replace(Regex("<[^>]*>"), "").trim().replace(Regex("\\s+"), " ")
        return entries.mapNotNull { e ->
            val title = strip(e.substringAfter("<title>").substringBefore("</title>"))
            val link = e.substringAfter("<id>").substringBefore("</id>").trim()
            if (title.isEmpty() || link.isEmpty()) return@mapNotNull null
            val authors = Regex("<name>([^<]+)</name>").findAll(e)
                .map { it.groupValues[1] }.joinToString(", ")
            Paper(
                title = title, authors = authors,
                abstract = strip(e.substringAfter("<summary>").substringBefore("</summary>")),
                published = e.substringAfter("<published>").substringBefore("</published>").take(10),
                doi = null, link = link, source = "arXiv", journal = null,
                tier = Paper.Tier.PREPRINT
            )
        }
    }

    private fun crossref(q: String): List<Paper> {
        val url = "https://api.crossref.org/works?query.bibliographic=" + URLEncoder.encode(q, "UTF-8") +
                "&filter=from-pub-date:2023-01-01,type:journal-article&rows=30&sort=relevance"
        val json = JSONObject(httpGet(url, "PVTodo/1.0 (mailto:you@example.com)"))
        val items: JSONArray = json.getJSONObject("message").getJSONArray("items")
        return (0 until items.length()).mapNotNull { i ->
            val it = items.getJSONObject(i)
            val titleArr: JSONArray? = it.optJSONArray("title")
            val title = titleArr?.optString(0, "") ?: ""
            if (title.isEmpty()) return@mapNotNull null
            val authors: String = it.optJSONArray("author")?.let { a ->
                (0 until a.length()).joinToString(", ") { k ->
                    val o = a.getJSONObject(k)
                    (o.optString("family", "") + " " + o.optString("given", "")).trim()
                }
            } ?: ""
            val year: Int = it.optJSONObject("published")
                ?.optJSONObject("date-parts")?.optJSONArray("0")?.optInt(0, 0) ?: 0
            val doi: String = it.optString("DOI", "")
            val journalArr: JSONArray? = it.optJSONArray("container-title")
            val journal = journalArr?.optString(0, "") ?: ""
            Paper(
                title = title, authors = authors,
                abstract = it.optString("abstract", "").replace(Regex("<[^>]*>"), "").trim(),
                published = year.toString(), doi = doi.ifEmpty { null },
                link = "https://doi.org/$doi", source = "Crossref", journal = journal
            )
        }
    }
}
