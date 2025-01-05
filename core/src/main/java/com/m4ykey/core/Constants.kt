package com.m4ykey.core

object Constants {

    const val PAGE_SIZE = 20
    const val SPACE_BETWEEN_ITEMS = 10

    const val SPOTIFY_AUTH_URL = "https://accounts.spotify.com/"
    const val SPOTIFY_BASE_URL = "https://api.spotify.com/v1/"
    const val LYRICS_BASE_URL = "https://lrclib.net/api/"
    const val NEWS_BASE_URL = "https://newsapi.org/"

    const val ALBUM = "Album"
    const val COMPILATION = "Compilation"
    const val SINGLE = "Single"
    const val EP = "EP"

    const val DOMAINS = "${MusicSources.ROLLING_STONE}, ${MusicSources.BILLBOARD}, ${MusicSources.PITCHFORK}, ${MusicSources.NME}, " +
            "${MusicSources.CONSEQUENCE}, ${MusicSources.STEREOGUM}, ${MusicSources.THE_FADER}"
}