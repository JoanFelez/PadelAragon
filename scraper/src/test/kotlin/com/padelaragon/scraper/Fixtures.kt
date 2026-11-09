package com.padelaragon.scraper

import com.padelaragon.shared.data.remote.dto.BundleJson
import com.padelaragon.shared.data.remote.dto.LeagueBundleDto
import java.io.File

object Fixtures {
    private val dir = File(System.getProperty("contracts.dir") ?: "../contracts/fixtures")

    fun sampleBundle(): LeagueBundleDto =
        BundleJson.decodeFromString(LeagueBundleDto.serializer(), File(dir, "league-bundle.sample.json").readText())
}
