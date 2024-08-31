package com.megaman.maverick.game.world

import com.engine.common.enums.ProcessState
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.engine.world.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.VelocityAlteration

fun IFixture.getBody(): Body = getEntity().body

fun IFixture.setEntity(entity: IBodyEntity): IFixture {
    properties.put(ConstKeys.ENTITY, entity)
    return this
}

fun IFixture.hasFixtureType(fixtureType: Any) = fixtureType == getFixtureType()

fun IFixture.getEntity() = properties.get(ConstKeys.ENTITY) as IBodyEntity

fun IFixture.depleteHealth(): Boolean {
    val entity = getEntity()
    if (entity !is IHealthEntity) return false

    entity.getHealthPoints().setToMin()
    return true
}

fun IFixture.setVelocityAlteration(alteration: (IFixture, Float) -> VelocityAlteration): IFixture {
    properties.put(ConstKeys.VELOCITY_ALTERATION, alteration)
    return this
}

fun IFixture.getVelocityAlteration(alterableBodyFixture: IFixture, delta: Float) =
    (properties.get(ConstKeys.VELOCITY_ALTERATION) as (IFixture, Float) -> VelocityAlteration).invoke(
            alterableBodyFixture,
            delta
        )

fun IFixture.setRunnable(runnable: () -> Unit): IFixture {
    properties.put(ConstKeys.RUNNABLE, runnable)
    return this
}

fun IFixture.getRunnable() = properties.get(ConstKeys.RUNNABLE) as (() -> Unit)?

fun IFixture.hasConsumer() = properties.containsKey(ConstKeys.CONSUMER)

fun IFixture.setConsumer(consumer: (ProcessState, IFixture) -> Unit): IFixture {
    properties.put(ConstKeys.CONSUMER, consumer)
    return this
}

fun IFixture.getConsumer() = properties.get(ConstKeys.CONSUMER) as ((ProcessState, IFixture) -> Unit)?

fun IFixture.setHitByBodyReceiver(receiver: (IBodyEntity) -> Unit) {
    putProperty(ConstKeys.HIT_BY_BODY, receiver)
}

fun IFixture.hasHitByBodyReceiver() = hasProperty(ConstKeys.HIT_BY_BODY)

fun IFixture.getHitByBody(body: IBodyEntity) = (getProperty(ConstKeys.HIT_BY_BODY) as (IBodyEntity) -> Unit).invoke(body)

fun IFixture.setHitByBlockReceiver(receiver: (Block) -> Unit) {
    putProperty(ConstKeys.HIT_BY_BLOCK, receiver)
}

fun IFixture.hasHitByBlockReceiver() = hasProperty(ConstKeys.HIT_BY_BLOCK)

fun IFixture.getHitByBlock(block: Block) = (getProperty(ConstKeys.HIT_BY_BLOCK) as (Block) -> Unit).invoke(block)

fun IFixture.setHitByPlayerReceiver(receiver: (Megaman) -> Unit) {
    putProperty(ConstKeys.HIT_BY_PLAYER, receiver)
}

fun IFixture.hasHitByPlayerReceiver() = hasProperty(ConstKeys.HIT_BY_PLAYER)

fun IFixture.getHitByPlayer(player: Megaman) =
    (getProperty(ConstKeys.HIT_BY_PLAYER) as (Megaman) -> Unit).invoke(player)

fun IFixture.setHitByProjectileReceiver(receiver: (IProjectileEntity) -> Unit) {
    putProperty(ConstKeys.HIT_BY_PROJECTILE, receiver)
}

fun IFixture.hasHitByProjectileReceiver() = hasProperty(ConstKeys.HIT_BY_PROJECTILE)

fun IFixture.getHitByProjectile(projectile: IProjectileEntity) =
    (getProperty(ConstKeys.HIT_BY_PROJECTILE) as (IProjectileEntity) -> Unit).invoke(projectile)