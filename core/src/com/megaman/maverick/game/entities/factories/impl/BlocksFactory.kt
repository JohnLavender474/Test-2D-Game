package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.blocks.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.special.RailTrackPlatform

class BlocksFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val TAG = "BlockFactory"
        const val STANDARD = "Standard"
        const val ANIMATED_BLOCK = "AnimatedBlock"
        const val ICE_BLOCK = "IceBlock"
        const val GEAR_TROLLEY = "GearTrolley"
        const val CONVEYOR_BELT = "ConveyorBelt"
        const val ROCKET_PLATFORM = "RocketPlatform"
        const val DROPPER_LIFT = "DropperLift"
        const val LIFT = "Lift"
        const val PROPELLER_PLATFORM = "PropellerPlatform"
        const val SWINGIN_PLATFORM = "SwinginPlatform"
        const val RAIL_TRACK_PLATFORM = "RailTrackPlatform"
        const val BREAKABLE_ICE = "BreakableIce"
        const val BREAKABLE_BLOCK = "BreakableBlock"
    }

    override fun init() {
        pools.put(STANDARD, EntityPoolCreator.create { Block(game) })
        pools.put(ANIMATED_BLOCK, EntityPoolCreator.create { AnimatedBlock(game) })
        pools.put(ICE_BLOCK, EntityPoolCreator.create { IceBlock(game) })
        pools.put(GEAR_TROLLEY, EntityPoolCreator.create { GearTrolley(game) })
        pools.put(CONVEYOR_BELT, EntityPoolCreator.create { ConveyorBelt(game) })
        pools.put(ROCKET_PLATFORM, EntityPoolCreator.create { RocketPlatform(game) })
        pools.put(DROPPER_LIFT, EntityPoolCreator.create { DropperLift(game) })
        pools.put(LIFT, EntityPoolCreator.create { Lift(game) })
        pools.put(PROPELLER_PLATFORM, EntityPoolCreator.create { PropellerPlatform(game) })
        pools.put(SWINGIN_PLATFORM, EntityPoolCreator.create { SwinginPlatform(game) })
        pools.put(RAIL_TRACK_PLATFORM, EntityPoolCreator.create { RailTrackPlatform(game) })
        pools.put(BREAKABLE_ICE, EntityPoolCreator.create { BreakableIce(game) })
        pools.put(BREAKABLE_BLOCK, EntityPoolCreator.create { BreakableBlock(game) })
    }

    override fun fetch(key: Any) =
        pools.get(if (key == "" || key.toString().lowercase() == "block") STANDARD else key)?.fetch()
}
