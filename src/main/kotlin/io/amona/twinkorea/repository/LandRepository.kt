package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.Land
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface LandRepository: JpaRepository<Land, Long>, JpaSpecificationExecutor<Land>  {
    fun findById(id: Long?): Land?

    fun findAllByDistrict(district: String): MutableList<Land>
}