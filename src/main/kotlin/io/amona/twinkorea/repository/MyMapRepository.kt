package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.MyMap
import io.amona.twinkorea.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface MyMapRepository: JpaRepository<MyMap, Long>, JpaSpecificationExecutor<MyMap> {
    fun findAllByUser(user: User): MutableList<MyMap>

    fun findById(id: Long?): MyMap?

    fun findByIdAndUser(id: Long, user: User): MyMap?
}