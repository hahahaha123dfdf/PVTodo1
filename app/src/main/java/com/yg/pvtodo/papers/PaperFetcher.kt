package com.yg.pvtodo.papers

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object PaperFetcher {
    // 内置 SCI Q1 期刊白名单(近似判定,可自行增删)
    private val Q1_JOURNALS = listOf(
        "applied energy", "ieee transactions on smart grid", "renewable energy",
        "energy conversion and management", "ieee transactions on power systems",
        "energy", "ieee transactions on sustainable energy", "solar energy",
        "electric power systems research", "international journal of electrical power",
        "applied soft computing", "expert systems with applications", "energy reports"
    )

    fun fetchAll(): List<Paper> {
        val out = mutableListOf<Paper>()
        try { out += arxiv() } catch (_: Exception) {}
        try { out += crossref() } catch (_: Exception) {}
        return out.map { p ->
            val j = p.journal?.lowercase() ?: ""
            if (p.tier != Paper.Tier.PREPRINT && Q1_JOURNALS.any { j.contains(it) }) p.copy(tier = Paper.Tier.Q1) else p
        }
    }

    private fun httpGet(url: String, ua: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", ua)
        c.connectTimeout = 15000
        c.readTimeout = 15000
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    private fun arxiv(): List<Paper> {
        val q = URLEncoder.encode("all:\"photovoltaic power forecasting\"", "UTF-8")
        val xml = httpGet("http://export.arxiv.org/api/query?search_query=$q&sortBy=submittedDate&sortOrder=descending&max_results=10", "PVTodo/1.0")
        val entries = xml.split("<entry>").drop(1)
        val strip = { s: String -> s.replace(Regex("<[^>]*>"), "").trim().replace(Regex("\\s+"), " ") }
        return entries.mapNotNull { e ->
            val title = strip(e.substringAfter("<title>").substringBefore("</title>"))
            val link = e.substringAfter("<id>").substringBefore("</id>").trim()
            if (title.isEmpty() || link.isEmpty()) return@mapNotNull null
            Paper(
                title = title,
                authors = Regex("<name>([^<]+)</name>").matcher(e).let { m ->
                    buildString { while (m.find()) { if (isNotEmpty()) append(", "); append(m.group(1)) } }
                },
                abstract = strip(e.substringAfter("<summary>").substringBefore("</summary>")),
                published = e.substringAfter("<published>").substringBefore("</published>").take(10),
                doi = null, link = link, source = "arXiv", journal = null, tier = Paper.Tier.PREPRINT
            )
        }
    }

    private fun crossref(): List<Paper> {
        val q = URLEncoder.encode("photovoltaic power forecasting", "UTF-8")
        val url = "https://api.crossref.org/works?query=$q&filter=from-pub-date:2024-01-01,type:journal-article&rows=15&sort=published&order=desc"
        val json = JSONObject(httpGet(url, "PVTodo/1.0 (mailto:you@example.com)"))
        return json.getJSONObject("message").getJSONArray("items").let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val it = arr.getJSONObject(i)
                val title = it.getJSONArray("title").optString(0)
                if (title.isEmpty()) return@mapNotNull null
                val authors = if (it.has("author")) it.getJSONArray("author").let { a ->
                    (0 until a.length()).joinToString(", ") { k ->
                        a.getJSONObject(k).optString("family") + " " + a.getJSONObject(k).optString("given")
                    }
                } else ""
                val year = it.getJSONObject("published").getJSONObject("date-parts").getJSONArray(0).optInt(0)
                Paper(
                    title = title, authors = authors,
                    abstract = it.optString("abstract", "").replace(Regex("<[^>]*>"), "").trim(),
                    published = year.toString(), doi = it.optString("DOI", null),
                    link = "https://doi.org/" + it.optString("DOI", ""), source = "Crossref",
                    journal = it.getJSONObject("container-title").optString(0, null)
                )
            }
        }
    }
}
