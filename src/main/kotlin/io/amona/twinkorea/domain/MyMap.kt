package io.amona.twinkorea.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModelProperty
import org.hibernate.Hibernate
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "my_map")
data class MyMap(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "map_name")
    val mapName: String,

    @Column(name = "left_top")
    val leftTop: String,

    @Column(name = "right_bottom")
    val rightBottom: String,

    @Column(name = "icon_url")
    val iconUrl: String? = null,

    @ApiModelProperty(value="생성시간")
    @Column(name = "created_at")
    val createdAt: LocalDateTime,

    @ApiModelProperty(value="업데이트 시간")
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "myMap")
    @OrderBy(value = "updated_at")
    val restaurants: MutableList<MapRestaurant> = ArrayList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as MyMap

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id )"
    }
}

@Entity
@Table(name = "map_restaurants")
data class MapRestaurant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "pid")
    val pid: Long = 0,

    @ApiModelProperty(value="생성시간")
    @Column(name = "created_at")
    val createdAt: LocalDateTime,

    @ApiModelProperty(value="업데이트 시간")
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime,

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "map_id")
    val myMap: MyMap,
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as MapRestaurant

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id )"
    }
}