package io.amona.twinkorea.service

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.Cell
import io.amona.twinkorea.domain.Land
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.LandDto
import io.amona.twinkorea.enums.MsgRevenueType
import io.amona.twinkorea.exception.AuthenticationException
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.exception.NotNullException
import io.amona.twinkorea.exception.ValidationException
import io.amona.twinkorea.repository.CellRepository
import io.amona.twinkorea.repository.LandRepository
import io.amona.twinkorea.repository.LandRepositorySupport
import io.amona.twinkorea.request.CellRequest
import io.amona.twinkorea.request.PointRequest
import io.amona.twinkorea.utils.CoordsTransform
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LandService(
    private val repo: LandRepository,
    private val dslRepo: LandRepositorySupport,
    private val cellRepo: CellRepository,
    private val cellService: CellService,
    private val pointService: PointService,
    private val appConfig: AppConfig,
) {
    fun readLand(landId: Long): Land {
        return repo.findById(id = landId)
            ?: throw NotFoundException("입력한 땅 ID로 땅을 찾아낼 수 없습니다.")
    }

    fun getLandsByUserId(user: User, pageRequest: Pageable): Page<LandDto> {
        return dslRepo.findAllByOwner(userId = user.id, pageable = pageRequest)
    }

    @Transactional
    fun deleteLand(user: User, landId: Long): String {
        if (!user.admin) throw AuthenticationException("구성된 땅을 삭제할 권한이 없습니다.")
        val target = readLand(landId)
        target.cells.forEach {
            cellService.updateCell(cellId = it.id, request = CellRequest(owner = null, land = null))
        }
        repo.delete(target)
        appConfig.logger.info {"[TWINKOREA API] 관리자#${user.id}가 랜드#${target.id}를 삭제했습니다."}
        return "SUCCESS"
    }

    // 삭제 후 재생성 방식
    @Transactional
    fun editLand(user: User, landId: Long, cellIds: List<Long>): Land {
        if (!user.admin) throw AuthenticationException("구성된 땅을 삭제할 권한이 없습니다.")
        val target = readLand(landId)

        // 기존 셀 초기화
        val targetCellIds = getCellIdsFromLand(target)
        targetCellIds.forEach {
            cellService.updateCell(cellId = it.toLong(), request = CellRequest(owner = null, land = null))
        }

        val cellInfo = cellService.readCellWithInfo(cellIds)            // 입려된 셀들의 식신 정보를 가져옴        셀 갯수당 식신 api 쿼리
        val landRangeArray = getLeftTopAndRightBottom(cellIds)       // 입력된 셀들의 최소 최대 범위를 가져옴    셀 갯수당 DB 쿼리
        val district = cellService.readCell(cellId = cellIds[0].toLong()).centerCity!!   // 1회 DB 쿼리
        val now = LocalDateTime.now()
        checkCellInSameDistrict(cellIds)
        val result = repo.save(target.copy(
            pricePerCell = appConfig.defaultCellValue.toLong(),         // 최초 구매 비용
            priceNearBy = getAvgPriceByDistrict(district),              // 셀과 동일한 행정동의 모든 땅 쿼리
            cellCount = cellIds.size.toLong(),
            district = district,
            leftTop = landRangeArray[0],
            rightBottom = landRangeArray[1],
            owner = user,
            siksinCount = cellInfo.siksinHot + cellInfo.siksinStar + cellInfo.siksinNormal,
            estEarn = 1.00,
            updatedAt = now,
        ))
        cellIds.forEach {
            if (cellRepo.findById(id = it)!!.land != null) throw NotNullException("해당 셀은 이미 땅으로 구성된 셀입니다.")
            cellService.updateCell(cellId = it, request = CellRequest(owner = user, land = result))    // 셀 갯수당 DB 쿼리
        }
        return result
    }

    @Transactional
    fun createLandByCellIds(user: User, cellIds: List<Long>): Land {
        val cellInfo = cellService.readCellWithInfo(cellIds)            // 입려된 셀들의 식신 정보를 가져옴        셀 갯수당 식신 api 쿼리
        val landRangeArray = getLeftTopAndRightBottom(cellIds)       // 입력된 셀들의 최소 최대 범위를 가져옴    셀 갯수당 DB 쿼리
        val district = cellService.readCell(cellId = cellIds[0]).centerCity!!   // 1회 DB 쿼리
        val totalPoint = cellIds.size.toLong() * appConfig.defaultCellValue.toLong()
        val now = LocalDateTime.now()
        checkCellInSameDistrict(cellIds)
        pointService.createPoint(request = PointRequest(msg = totalPoint, revenueType = MsgRevenueType.BUY, user = user))
        val result = repo.save(Land(
            pricePerCell = appConfig.defaultCellValue.toLong(),         // 최초 구매 비용
            priceNearBy = getAvgPriceByDistrict(district),              // 셀과 동일한 행정동의 모든 땅 쿼리
            cellCount = cellIds.size.toLong(),
            district = district,
            leftTop = landRangeArray[0],
            rightBottom = landRangeArray[1],
            owner = user,
            siksinCount = cellInfo.siksinHot + cellInfo.siksinStar + cellInfo.siksinNormal,
            estEarn = 1.00,
            createdAt = now,
            updatedAt = now,
        ))
        cellIds.forEach {
            if (cellRepo.findById(id = it)!!.land != null) throw NotNullException("해당 셀은 이미 땅으로 구성된 셀입니다.")
            cellService.updateCell(cellId = it, request = CellRequest(owner = user, land = result))    // 셀 갯수당 DB 쿼리
        }
        return result
    }

    private fun getAvgPriceByDistrict(district: String): Double {
        val landList: MutableList<Land> = repo.findAllByDistrict(district = district)
        var price: Long = 0
        landList.forEach {
            price += it.pricePerCell
        }
        return if (landList.size != 0) {
            (price/landList.size).toDouble()
        } else {
            1.00
        }
    }

    // cellIdList 로부터 최소좌표, 최대좌표를 구해냅니다.
    private fun getLeftTopAndRightBottom(cellIdList: List<Long>): Array<String> {
        var minX: Double = Double.MAX_VALUE
        var maxX = 0.00
        var minY: Double = Double.MAX_VALUE
        var maxY = 0.00
        cellIdList.forEach {
            val cell: Cell = cellRepo.findById(id = it)!!
            if (cell.centerX!! > maxX) {
                maxX = cell.centerX
            }
            if (cell.centerX < minX) {
                minX = cell.centerX
            }
            if (cell.centerY!! > maxY) {
                maxY = cell.centerY
            }
            if (cell.centerY < minY) {
                minY = cell.centerY
            }
        }
        val leftTopProj = CoordsTransform.fromUtmK(minX-50, maxY+50)
        val rightBottomProj = CoordsTransform.fromUtmK(maxX+50, minY-50)
        val leftTop = "${leftTopProj.x},${leftTopProj.y}"
        val rightBottom = "${rightBottomProj.x},${rightBottomProj.y}"
//        val leftTop = "$minX,$maxY"
//        val rightBottom = "$maxX,$minY"
        return arrayOf(leftTop, rightBottom)
    }

    private fun checkCellInSameDistrict(cellIdList: List<Long>) {
        val district: String = cellService.readCell(cellId = cellIdList[0]).centerCity!!
        cellIdList.forEach {
            val criteria = cellService.readCell(cellId = it).centerCity!!
            if (district != criteria) throw ValidationException("서로 다른 행정동의 셀들을 하나의 땅으로 묶을 수 없습니다.")
        }
    }

    private fun getCellIdsFromLand(land: Land): MutableList<String> {
        val cellIds: MutableList<String> = mutableListOf()
        land.cells.forEach {
            cellIds.add(it.id.toString())
        }
        return cellIds
    }
}