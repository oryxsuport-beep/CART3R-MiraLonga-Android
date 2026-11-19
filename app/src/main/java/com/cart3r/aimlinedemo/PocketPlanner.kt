package com.cart3r.aimlinedemo

import kotlin.math.abs

/** Geometria ideal de miras para seis caçapas e uma tabela opcional.
 * Posições são fornecidas manualmente pelo usuário; não há leitura de tela.
 */
enum class Cushion { LEFT, RIGHT, TOP, BOTTOM }

data class PocketRoute(
    val pocketIndex: Int,
    val points: List<AimPoint>,
    val bank: Cushion? = null
)

object PocketPlanner {
    fun pockets(table: TableBounds): List<AimPoint> {
        if (!table.valid) return emptyList()
        val middleX = (table.left + table.right) / 2f
        return listOf(
            AimPoint(table.left, table.top),
            AimPoint(middleX, table.top),
            AimPoint(table.right, table.top),
            AimPoint(table.left, table.bottom),
            AimPoint(middleX, table.bottom),
            AimPoint(table.right, table.bottom)
        )
    }

    fun direct(from: AimPoint, table: TableBounds): List<PocketRoute> {
        if (!table.contains(from)) return emptyList()
        return pockets(table).mapIndexed { index,pocket ->
            PocketRoute(index,listOf(from,pocket))
        }
    }

    /**
     * Reflection method: mirror target across a cushion, then intersect
     * the straight ray with the cushion. The resulting 2-segment path is
     * a geometric guide, not a game-physics prediction.
     */
    fun oneBank(
        from: AimPoint, pocket: AimPoint, table: TableBounds, wall: Cushion
    ): List<AimPoint> {
        if (!table.contains(from) || !table.valid) return emptyList()
        val reflected = when(wall) {
            Cushion.LEFT -> AimPoint(2f*table.left-pocket.x,pocket.y)
            Cushion.RIGHT -> AimPoint(2f*table.right-pocket.x,pocket.y)
            Cushion.TOP -> AimPoint(pocket.x,2f*table.top-pocket.y)
            Cushion.BOTTOM -> AimPoint(pocket.x,2f*table.bottom-pocket.y)
        }
        val dx=reflected.x-from.x
        val dy=reflected.y-from.y
        val d = when(wall) {
            Cushion.LEFT -> dx
            Cushion.RIGHT -> dx
            Cushion.TOP -> dy
            Cushion.BOTTOM -> dy
        }
        if(abs(d)<0.0001f)return emptyList()
        val t = when(wall) {
            Cushion.LEFT -> (table.left-from.x)/d
            Cushion.RIGHT -> (table.right-from.x)/d
            Cushion.TOP -> (table.top-from.y)/d
            Cushion.BOTTOM -> (table.bottom-from.y)/d
        }
        if(t <= 0.001f || t >= 0.999f)return emptyList()
        val x=from.x+t*dx
        val y=from.y+t*dy
        if(x < table.left || x > table.right || y < table.top || y > table.bottom) return emptyList()
        return listOf(from,AimPoint(x,y),pocket)
    }

    /** Six direct lines and up to six one-cushion alternatives. */
    fun fan(from: AimPoint, table: TableBounds, showBanks: Boolean): List<PocketRoute> {
        val base=direct(from,table)
        if(!showBanks || base.isEmpty())return base
        val banked=pockets(table).mapIndexedNotNull { index,pocket ->
            val wall=if(index<3)Cushion.BOTTOM else Cushion.TOP
            val path=oneBank(from,pocket,table,wall)
            if(path.size==3)PocketRoute(index,path,wall) else null
        }
        return base+banked
    }
}
