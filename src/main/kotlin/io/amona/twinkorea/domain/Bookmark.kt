package io.amona.twinkorea.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.amona.twinkorea.enums.GroupColor
import io.swagger.annotations.ApiModelProperty
import org.hibernate.Hibernate
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "bookmarks")
data class Bookmark(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "group_name")
    val groupName: String = "default",

    @Column(name = "group_color")
    val groupColor: GroupColor = GroupColor.DEFAULT,

    @Column(name = "icon_url")
    val iconUrl: String? = null,

    @Column(name = "is_default")
    val default: Boolean = false,

    @Column(name = "is_deleted")
    val deleted: Boolean = false,

    @Column(name = "counts")
    val counts: Long = 0,

    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "bookmark")
    @OrderBy(value = "updated_at")
    val restaurants: MutableList<BookmarkRestaurant> = ArrayList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Bookmark

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id )"
    }
}


@Entity
@Table(name = "bookmark_restaurants")
data class BookmarkRestaurant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "pid")
    val pid: Long = 0,

    @ApiModelProperty(value="생성시간")
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,

    @ApiModelProperty(value="업데이트 시간")
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "group_id")
    var bookmark: Bookmark,
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as BookmarkRestaurant

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id )"
    }
}