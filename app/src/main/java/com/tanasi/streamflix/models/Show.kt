package com.tanasi.streamflix.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.utils.toCalendar

sealed class Show : AppAdapter.Item

@Entity("movies")
class Movie(
    @PrimaryKey
    var id: String,
    var title: String,
    var overview: String = "",
    released: String? = null,
    var runtime: Int? = null,
    var trailer: String? = null,
    var quality: String? = null,
    var rating: Double? = null,
    var poster: String? = null,
    var banner: String? = null,

    @Ignore
    val genres: List<Genre> = listOf(),
    @Ignore
    val directors: List<People> = listOf(),
    @Ignore
    val cast: List<People> = listOf(),
    @Ignore
    val recommendations: List<Show> = listOf(),
) : Show(), WatchItem, AppAdapter.Item, Cloneable {

    constructor() : this("", "")

    var released = released?.toCalendar()
    var isFavorite: Boolean = false
    override var isWatched: Boolean = false
    @Embedded
    override var watchHistory: WatchItem.WatchHistory? = null


    @Ignore
    override lateinit var itemType: AppAdapter.Type

    public override fun clone() = super.clone() as Movie
}

@Entity("tv_shows")
class TvShow(
    @PrimaryKey
    var id: String,
    var title: String,
    var overview: String = "",
    released: String? = null,
    var runtime: Int? = null,
    var trailer: String? = null,
    var quality: String? = null,
    var rating: Double? = null,
    var poster: String? = null,
    var banner: String? = null,

    @Ignore
    val seasons: List<Season> = listOf(),
    @Ignore
    val genres: List<Genre> = listOf(),
    @Ignore
    val directors: List<People> = listOf(),
    @Ignore
    val cast: List<People> = listOf(),
    @Ignore
    val recommendations: List<Show> = listOf(),
) : Show(), AppAdapter.Item, Cloneable {

    constructor() : this("", "")

    var released = released?.toCalendar()
    var isFavorite: Boolean = false


    @Ignore
    override lateinit var itemType: AppAdapter.Type

    public override fun clone() = super.clone() as TvShow
}
