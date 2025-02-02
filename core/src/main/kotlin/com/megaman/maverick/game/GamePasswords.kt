package com.megaman.maverick.game

import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.MultiCollectionIterable
import com.megaman.maverick.game.entities.bosses.BossType
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition

object GamePasswords {

    private val indices =
        gdxArrayOf(8, 31, 18, 21, 15, 2, 17, 12, 7, 22, 35, 3, 33, 1, 23, 16, 6, 14, 5, 11, 28, 10, 13, 0)

    fun getGamePassword(state: GameState): IntArray {
        val password = IntArray(36)

        // TODO: fix
        val multiCollectionIterable = MultiCollectionIterable(
            gdxArrayOf(
                BossType.entries.toTypedArray().toGdxArray(),
                MegaHeartTank.entries.toTypedArray().toGdxArray(),
                MegaHealthTank.entries.toTypedArray().toGdxArray(),
                // MegaAbility.values().toGdxArray()
            )
        )

        // TODO: fix
        multiCollectionIterable.forEach { value, outerIndex, _ ->
            val index = indices[outerIndex]
            val digit = when (value) {
                is LevelDefinition -> if (state.levelsDefeated.contains(value)) 1 else 0
                is MegaHeartTank -> if (state.heartTanksCollected.contains(value)) 1 else 0
                is MegaHealthTank -> if (state.healthTanksCollected.containsKey(value)) 1 else 0
                // is MegaAbility -> if (state.abilitiesAttained.contains(value)) 1 else 0
                else -> throw IllegalStateException("Unknown value type: ${value::class}")
            }
            password[index] = digit
        }
        return password
    }

    fun loadGamePassword(state: GameState, password: IntArray) {
        state.reset()
        val (bossesDefeated, heartTanksCollected, healthTanksCollected) = state

        val passwordArray = password.map { it.toString().toInt() }.toIntArray()

        // TODO: fix
        val multiCollectionIterable = MultiCollectionIterable(
            gdxArrayOf(
                BossType.entries.toTypedArray().toGdxArray(),
                MegaHeartTank.entries.toTypedArray().toGdxArray(),
                MegaHealthTank.entries.toTypedArray().toGdxArray(),
                // MegaAbility.values().toGdxArray()
            )
        )

        // TODO: fix
        multiCollectionIterable.forEach { value, outerIndex, _ ->
            val index = indices[outerIndex]
            when (value) {
                is LevelDefinition-> if (passwordArray[index] == 1) bossesDefeated.add(value)
                is MegaHeartTank -> if (passwordArray[index] == 1) heartTanksCollected.add(value)
                is MegaHealthTank -> if (passwordArray[index] == 1) healthTanksCollected.put(value, 0)
                // is MegaAbility -> if (passwordArray[index] == 1) state.abilitiesAttained.add(value)
                else -> throw IllegalStateException("Unknown value type: ${value::class}")
            }
        }
    }
}
