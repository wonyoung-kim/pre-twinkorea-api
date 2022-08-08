package io.amona.twinkorea.repository

import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.MyInvitingRankingDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    fun findByIdAndDeactivateIsFalse(id: Long?): User?

    fun findByReferralCodeAndDeactivateIsFalse(referralCode: String): User?

    fun findBySnsIdAndDeactivateIsFalse(snsId: String): User?

    fun findByEmailAndDeactivateIsFalse(email: String): User?

    fun findTop10ByIdIsNotAndDeactivateIsFalseOrderByInvitingCountDesc(id: Long = 0): MutableList<User>

    @Query("SELECT ranking, nickname, email, inviting_count as invitingCount FROM (SELECT *, RANK() over (ORDER BY inviting_count desc) As ranking FROM users) users where users.id = :id AND users.deactivate != true", nativeQuery = true)
    fun getMyRanking(@Param(value = "id") id: Long): MyInvitingRankingDto
}
