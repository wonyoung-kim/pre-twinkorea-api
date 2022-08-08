package io.amona.twinkorea.service

import io.amona.twinkorea.configuration.AppConfig
import io.amona.twinkorea.domain.MapRestaurant
import io.amona.twinkorea.domain.MyMap
import io.amona.twinkorea.domain.User
import io.amona.twinkorea.dtos.MyMapDto
import io.amona.twinkorea.exception.AuthenticationException
import io.amona.twinkorea.exception.NotFoundException
import io.amona.twinkorea.exception.NotNullException
import io.amona.twinkorea.repository.MapRestaurantRepository
import io.amona.twinkorea.repository.MyMapRepository
import io.amona.twinkorea.request.MyMapRequest
import io.amona.twinkorea.service.external.PlaceService
import io.amona.twinkorea.transformer.MyMapTransformer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


@Service
class MyMapService(
    private val repo: MyMapRepository,
    private val mapRestaurantRepo: MapRestaurantRepository,
    private val placeService: PlaceService,
    private val transformer: MyMapTransformer,
    private val appConfig: AppConfig,
){
    fun addMyMap(request: MyMapRequest, user: User): MyMap {
        if  (
            request.mapName == null ||
            request.leftTop == null ||
            request.rightBottom == null
        ) throw NotNullException("mapName, leftTop, rightBottom 은 필수 입력값입니다.")

        val transformed = transformer.from(request, user)
        val result = repo.save(transformed)
        appConfig.logger.info{
            "[TWINKOREA API] 유저#${result.user.id}가 새로운 맛집지도 \"${result.mapName}\"를 만들었습니다."
        }
        return result
    }

    fun getMyMapList(user: User): MutableList<MyMapDto> {
        val myMapDtoList: MutableList<MyMapDto> = mutableListOf()
        val myMapList = repo.findAllByUser(user = user)
        myMapList.forEach {
            myMapDtoList.add(
                MyMapDto(
                    id = it.id,
                    mapName = it.mapName,
                    iconUrl = it.iconUrl
                )
            )
        }
        appConfig.logger.info{
            "[TWINKOREA API] 유저#${user.id}의 맛집지도 ${myMapDtoList.size}개 불러오기 완료"
        }
        return myMapDtoList
    }

    @Transactional
    fun addRestaurantToMyMap(user: User, mapId: Long, pid: Long): MapRestaurant {
        val myMap = repo.findByIdAndUser(id = mapId, user = user) ?: throw NotFoundException("나의 지도(#${mapId}를 찾을 수 없습니다.")
        val now = LocalDateTime.now()
        // 지도에 식당 추가
        val newMapRestaurant = mapRestaurantRepo.save(MapRestaurant(
            pid = pid, createdAt = now, updatedAt = now, myMap = myMap
        ))

        // 기존 지도의 좌표 범위와 비교해서 새로 추가된 식당이 그 좌표 범위를 벗어나는 경우,
        // 즉 기존 좌표 범위의 최소값보다 작은 위경도에 식당이 있거나, 최대값보다 큰 위경도에 식당이 있는경우,
        // 기존 지도의 좌표 범위를 새로 추가된 식당 기준으로 늘려준다.
        val originalLeftTop = myMap.leftTop.split(",")
        val originalRightBottom = myMap.rightBottom.split(",")
        var minLng = originalLeftTop[0].toDouble()
        var maxLat =  originalLeftTop[1].toDouble()
        var maxLng = originalRightBottom[0].toDouble()
        var minLat = originalRightBottom[1].toDouble()

        val restaurantInfo = placeService.getRestaurantInfo(pid)
        val restaurantLng = restaurantInfo.get("lng").toString().toDouble()
        val restaurantLat = restaurantInfo.get("lat").toString().toDouble()

        if (minLng > restaurantLng) {minLng = restaurantLng}
        if (minLat > restaurantLat) {minLat = restaurantLat}
        if (maxLng < restaurantLng) {maxLng = restaurantLng}
        if (maxLat < restaurantLat) {maxLat = restaurantLat}

        // 지도의 범위 업데이트
        repo.save(myMap.copy(
            leftTop = "${minLng},${maxLat}",
            rightBottom = "${maxLng},${minLat}",
            updatedAt = now
        ))
        return newMapRestaurant
    }

    @Transactional
    fun editMyMap(request: MyMapRequest, id: Long, user: User): MyMap {
        val now =  LocalDateTime.now()
        val targetMap = repo.findById(id = id) ?: throw NotFoundException("요청한 맛집 지도를 찾을 수 없습니다.")
        if (targetMap.user != user) throw AuthenticationException("요청한 회원은 해당 맛집에 대한 수정권한을 갖고있지 않습니다.")

        // 입력된 파라미터만 수정하고 아닌건 기존 내용 유지
        val mapName = request.mapName ?: targetMap.mapName
        val iconUrl = request.iconUrl ?: targetMap.iconUrl

        val result = repo.save(targetMap.copy(mapName = mapName, iconUrl = iconUrl, updatedAt = now))
        appConfig.logger.info{
            "[TWINKOREA API] 유저#${result.user.id}가 새로운 맛집지도 \"${result.mapName}\"를 수정하였습니다."
        }
        return result
    }
}