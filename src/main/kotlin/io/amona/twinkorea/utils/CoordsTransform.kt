package io.amona.twinkorea.utils

import org.locationtech.proj4j.BasicCoordinateTransform
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.ProjCoordinate

object CoordsTransform {
    private val factory: CRSFactory = CRSFactory()

    fun fromUtmK(x: Double, y: Double): ProjCoordinate {
        val srcCrs = factory.createFromName("EPSG:5179")
        val dstCrs = factory.createFromName("EPSG:4326")
        val transform = BasicCoordinateTransform(srcCrs, dstCrs)
        val srcCoords = ProjCoordinate(x, y)
        val dstCoords = ProjCoordinate()
        transform.transform(srcCoords, dstCoords)
        return dstCoords
    }

    fun toUtmK(x: Double, y: Double): ProjCoordinate {
        val srcCrs = factory.createFromName("EPSG:4326")
        val dstCrs = factory.createFromName("EPSG:5179")
        val transform = BasicCoordinateTransform(srcCrs, dstCrs)
        val srcCoords = ProjCoordinate(x, y)
        val dstCoords = ProjCoordinate()
        transform.transform(srcCoords, dstCoords)
        return dstCoords
    }
}
