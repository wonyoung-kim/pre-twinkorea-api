package io.amona.twinkorea.service

import io.amona.twinkorea.domain.AdminWhiteList
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.exception.NotNullException
import io.amona.twinkorea.repository.AdminWhiteListRepository
import io.amona.twinkorea.request.IpWhiteListRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AdminIpAuthService(
    private val repo: AdminWhiteListRepository,
) {
    /**
     * IP 주소로 IP 화이트리스트 테이블에서 데이터를 찾아옵니다.
     */
    fun readIp(ip: String): AdminWhiteList {
        return repo.findByIp(ip) ?: throw NotFoundException("입력한 IP와 매칭되는 데이터를 찾을 수 없습니다.")
    }

    /**
     * IP 추소와 설명을 화이트리스트 테이블에 입력합니다.
     */
    fun addIpToWhiteList(request: IpWhiteListRequest): AdminWhiteList {
        if (request.description == null) throw NotNullException("description 은 필수 파라미터 입니다.")
        val now = LocalDateTime.now()
        return repo.save(AdminWhiteList(
            ip = request.ip,
            description = request.description,
            createdAt = now,
            updatedAt = now,
        ))
    }

    /**
     * IP 화이트리스트 테이블에서 IP 주소를 삭제합니다.
     */
    fun deleteIpFromWhiteList(ip: String): Boolean {
        val targetIp = readIp(ip)
        repo.delete(targetIp)
        return true
    }

    /**
     * IP 화이트리스트 테이블의 IP의 설명을 수정합니다.
     */
    fun editIpWhiteListDescription(request: IpWhiteListRequest): Boolean {
        val now = LocalDateTime.now()
        if (request.description == null) throw NotNullException("description 은 필수 파라미터 입니다.")
        val targetIp = readIp(request.ip)
        repo.save(targetIp.copy(description = request.description, updatedAt = now))
        return true
    }

    /**
     * IP 화이트리스트의 리스트를 조회합니다.
     */
    fun getListOfIpWhiteList(pageRequest: Pageable): Page<AdminWhiteList> {
        return repo.findAllWithPaging(pageRequest)
    }
}