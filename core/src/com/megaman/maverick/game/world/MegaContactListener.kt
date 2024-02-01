package com.megaman.maverick.game.world

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.extensions.objectSetOf
import com.engine.common.shapes.GameLine
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.ShapeUtils
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.world.Contact
import com.engine.world.Fixture
import com.engine.world.IContactListener
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.sensors.Gate
import com.megaman.maverick.game.entities.special.Water
import com.megaman.maverick.game.utils.VelocityAlterator

@Suppress("UNCHECKED_CAST")
class MegaContactListener(private val game: MegamanMaverickGame) : IContactListener {

  companion object {
    const val TAG = "MegaContactListener"
  }

  override fun beginContact(contact: Contact, delta: Float) {
    // do not check for contacts within the same entity
    if (contact.fixture1.getEntity() == contact.fixture2.getEntity()) return

    // consumer
    if (contact.oneFixtureMatches(FixtureType.CONSUMER)) {
      GameLogger.debug(TAG, "beginContact(): Consumer, contact = $contact")
      val (consumer, consumable) = contact.getFixturesIfOneMatches(FixtureType.CONSUMER)!!

      (consumer.getProperty(ConstKeys.CONSUMER) as (Fixture) -> Unit)(consumable)
    }

    // damager, damageable
    else if (contact.fixturesMatch(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
      GameLogger.debug(TAG, "beginContact(): Damager-Damageable, contact = $contact")
      val (damagerFixture, damageableFixture) =
          contact.getFixturesInOrder(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)!!

      val damager = damagerFixture.getEntity() as IDamager
      val damageable = damageableFixture.getEntity() as IDamageable

      if (damageable.canBeDamagedBy(damager) && damager.canDamage(damageable)) {
        damageable.takeDamageFrom(damager)
        damager.onDamageInflictedTo(damageable)
      }
    }

    // death, damageable
    else if (contact.fixturesMatch(FixtureType.DEATH, FixtureType.DAMAGEABLE)) {
      GameLogger.debug(TAG, "beginContact(): Death-Damageable, contact = $contact")
      val (_, damageable) = contact.getFixturesInOrder(FixtureType.DEATH, FixtureType.DAMAGEABLE)!!

      damageable.depleteHealth()
    }

    // block, side
    else if (contact.fixturesMatch(FixtureType.BLOCK, FixtureType.SIDE)) {
      GameLogger.debug(TAG, "beginContact(): Block-Side, contact = $contact")
      val (block, side) = contact.getFixturesInOrder(FixtureType.BLOCK, FixtureType.SIDE)!!

      if (block.bodyHasLabel(BodyLabel.NO_SIDE_TOUCHIE)) return

      val body = side.getBody()
      val sideType = side.getProperty(ConstKeys.SIDE)

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
      else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
    }

    // side, gate
    else if (contact.fixtureSetsMatch(
        objectSetOf(FixtureType.SIDE, FixtureType.FEET, FixtureType.HEAD),
        objectSetOf(FixtureType.GATE))) {
      GameLogger.debug(TAG, "beginContact(): Side/Feet/Head-Gate")
      val (other, gateFixture) =
          contact.getFixtureSetsInOrder(
              objectSetOf(FixtureType.SIDE, FixtureType.FEET, FixtureType.HEAD),
              objectSetOf(FixtureType.GATE))!!
      val entity = other.getEntity()
      if (entity is Megaman) {
        val gate = gateFixture.getEntity() as Gate
        if (gate.state == Gate.GateState.OPENABLE) gate.trigger()
      }
    }

    // side, ice
    else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
      GameLogger.debug(TAG, "beginContact(): Side-Ice, contact = $contact")
      val (side, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE)!!

      val body = side.getBody()
      val sideType = side.getProperty(ConstKeys.SIDE)

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true)
      else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, true)
    }

    // feet, block
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
      GameLogger.debug(TAG, "beginContact(): Feet-Block, contact = $contact")
      val (feet, block) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK)!!

      val body = feet.getBody()
      val posDelta = block.getBody().positionDelta

      body.x += posDelta.x
      body.y += posDelta.y

      val entity = feet.getEntity()
      if (entity is Megaman) entity.aButtonTask = AButtonTask.JUMP

      body.setBodySense(BodySense.FEET_ON_GROUND, true)
    }

    // feet, ice
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
      GameLogger.debug(TAG, "beginContact(): Feet-Ice, contact = $contact")
      val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_ON_ICE, true)

      body.physics.frictionOnSelf.set(1.0175f, 1.0175f)
    }

    // bouncer, feet or head or side
    else if (contact.fixtureSetsMatch(
        objectSetOf(FixtureType.BOUNCER),
        objectSetOf(FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE))) {
      GameLogger.debug(TAG, "beginContact(): Bouncer-Feet/Head/Side, contact = $contact")
      val (bouncer, bounceable) =
          contact.getFixtureSetsInOrder(
              objectSetOf(FixtureType.BOUNCER),
              objectSetOf(FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE))!!

      val bounce = bouncer.getVelocityAlteration(bounceable, delta)
      VelocityAlterator.alterate(bounceable.getBody(), bounce)

      bouncer.getRunnable()?.invoke()
    }

    // head, block
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
      GameLogger.debug(TAG, "beginContact(): Head-Block, contact = $contact")
      val (head, block) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK)!!

      if (block.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) return

      val body = head.getBody()
      body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
      body.physics.velocity.y = 0f
    }

    // water listener, water
    else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
      GameLogger.debug(TAG, "beginContact(): WaterListener-Water, contact = $contact")
      val (listener, water) =
          contact.getFixturesInOrder(FixtureType.WATER_LISTENER, FixtureType.WATER)!!

      val body = listener.getBody()
      body.setBodySense(BodySense.IN_WATER, true)

      val entity = listener.getEntity()
      if (entity is Megaman &&
          !entity.body.isSensing(BodySense.FEET_ON_GROUND) &&
          !entity.isBehaviorActive(BehaviorType.WALL_SLIDING))
          entity.aButtonTask = AButtonTask.SWIM

      Splash.generate(game, listener.getBody(), water.getBody())

      val waterEntity = water.getEntity() as Water
      if ((entity is Megaman || entity is AbstractEnemy) && waterEntity.splashSound)
          game.audioMan.playSound(SoundAsset.SPLASH_SOUND, false)
    }

    // head, ladder
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
      GameLogger.debug(TAG, "beginContact(): Head-Ladder, contact = $contact")
      val (head, ladderFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER)!!

      val body = head.getBody()
      body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)

      body.properties.put(ConstKeys.LADDER, ladderFixture.getEntity())
    }

    // feet, ladder
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
      GameLogger.debug(TAG, "beginContact(): Feet-Ladder, contact = $contact")
      val (feet, ladderFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)

      body.properties.put(ConstKeys.LADDER, ladderFixture.getEntity())
    }

    // body, force
    else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
      GameLogger.debug(TAG, "beginContact(): Body-Force, contact = $contact")
      val (bodyFixture, force) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE)!!

      val forceAlteration = force.getVelocityAlteration(bodyFixture, delta)
      VelocityAlterator.alterate(bodyFixture.getBody(), forceAlteration, delta)
    }

    // body, gravity change
    else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.GRAVITY_CHANGE)) {
      val (bodyFixture, gravityChangeFixture) =
          contact.getFixturesInOrder(FixtureType.BODY, FixtureType.GRAVITY_CHANGE)!!

      val canChangeGravity =
          bodyFixture.properties.getOrDefault(ConstKeys.GRAVITY_ROTATABLE, true) as Boolean
      if (!canChangeGravity) return

      val body = bodyFixture.getBody()
      val pd = body.positionDelta
      val bodyPointToCheck =
          if (pd.x > 0f) {
            if (pd.y > 0f) body.getTopRightPoint()
            else if (pd.y < 0f) body.getBottomRightPoint() else body.getCenterRightPoint()
          } else if (pd.x < 0f) {
            if (body.positionDelta.y > 0f) body.getTopLeftPoint()
            else if (body.positionDelta.y < 0f) body.getBottomLeftPoint()
            else body.getCenterLeftPoint()
          } else {
            if (pd.y > 0f) body.getTopCenterPoint()
            else if (pd.y < 0f) body.getBottomCenterPoint() else body.getCenter()
          }

      if (!gravityChangeFixture.bodyRelativeShape!!.contains(bodyPointToCheck)) return

      val direction =
          gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return

      val entity = bodyFixture.getEntity()
      if (entity is IDirectionRotatable && entity.directionRotation != direction)
          entity.directionRotation = direction
    }

    // projectile, block or body or shield or water
    else if (contact.fixtureSetsMatch(
        objectSetOf(FixtureType.PROJECTILE),
        objectSetOf(FixtureType.BLOCK, FixtureType.BODY, FixtureType.SHIELD, FixtureType.WATER))) {
      GameLogger.debug(
          TAG, "beginContact(): Projectile-Block/Body/Shield/Water, contact = $contact")
      val (projectile, other) =
          contact.getFixtureSetsInOrder(
              objectSetOf(FixtureType.PROJECTILE),
              objectSetOf(
                  FixtureType.BLOCK, FixtureType.BODY, FixtureType.SHIELD, FixtureType.WATER))!!

      if (other.getBody().hasBodyLabel(BodyLabel.NO_PROJECTILE_COLLISION)) return

      val projectileEntity = projectile.getEntity() as IProjectileEntity

      when (other.fixtureLabel) {
        FixtureType.BLOCK -> {
          GameLogger.debug(TAG, "beginContact(): Projectile-Block, contact = $contact")
          projectileEntity.hitBlock(other)
        }
        FixtureType.BODY -> {
          GameLogger.debug(TAG, "beginContact(): Projectile-Body, contact = $contact")
          projectileEntity.hitBody(other)
        }
        FixtureType.SHIELD -> {
          GameLogger.debug(TAG, "beginContact(): Projectile-Shield, contact = $contact")
          projectileEntity.hitShield(other)
        }
        FixtureType.WATER -> {
          GameLogger.debug(TAG, "beginContact(): Projectile-Water, contact = $contact")
          projectileEntity.hitWater(other)
        }
      }
    }

    // player, item
    else if (contact.fixturesMatch(FixtureType.PLAYER, FixtureType.ITEM)) {
      GameLogger.debug(TAG, "beginContact(): Player-Item, contact = $contact")
      val (player, item) = contact.getFixturesInOrder(FixtureType.PLAYER, FixtureType.ITEM)!!

      val playerEntity = player.getEntity()
      val itemEntity = item.getEntity()

      if (playerEntity is Megaman && itemEntity is ItemEntity)
          itemEntity.contactWithPlayer(playerEntity)
    }
  }

  override fun continueContact(contact: Contact, delta: Float) {
    // do not check for contacts within the same entity
    if (contact.fixture1.getEntity() == contact.fixture2.getEntity()) return

    // consumer
    if (contact.oneFixtureMatches(FixtureType.CONSUMER)) {
      val (consumer, consumable) = contact.getFixturesIfOneMatches(FixtureType.CONSUMER)!!

      (consumer.getProperty(ConstKeys.CONSUMER) as (Fixture) -> Unit)(consumable)
    }

    // damager, damageable
    else if (contact.fixturesMatch(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)) {
      val (damagerFixture, damageableFixture) =
          contact.getFixturesInOrder(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)!!

      val damager = damagerFixture.getEntity() as IDamager
      val damageable = damageableFixture.getEntity() as IDamageable

      if (damageable.canBeDamagedBy(damager) && damager.canDamage(damageable)) {
        damageable.takeDamageFrom(damager)
        damager.onDamageInflictedTo(damageable)
      }
    }

    // feet, block
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
      val (feet, block) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK)!!

      val body = feet.getBody()
      val posDelta = block.getBody().positionDelta

      body.x += posDelta.x
      body.y += posDelta.y

      val entity = feet.getEntity()
      if (entity is Megaman) entity.aButtonTask = AButtonTask.JUMP

      body.setBodySense(BodySense.FEET_ON_GROUND, true)
    }

    // feet, ladder
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
      val (feet, ladderFixture) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER)!!

      if (ladderFixture.shape.contains(feet.shape.getBoundingRectangle().getBottomCenterPoint())) {
        val body = feet.getBody()
        body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)
        body.properties.put(ConstKeys.LADDER, ladderFixture.getEntity())
      }
    }

    // head, ladder
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
      val (head, ladderFixture) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER)!!

      if (ladderFixture.shape.contains(head.shape.getBoundingRectangle().getTopCenterPoint())) {
        val body = head.getBody()
        body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)
        body.properties.put(ConstKeys.LADDER, ladderFixture.getEntity())
      }
    }

    // head, block
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
      val (head, block) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK)!!

      if (block.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) return

      val body = head.getBody()
      body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
    }

    // feet, ice
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
      val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_ON_ICE, true)

      body.physics.frictionOnSelf.set(1.0175f, 1.0175f)
    }

    // water listener, water
    else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
      val (listener, _) =
          contact.getFixturesInOrder(FixtureType.WATER_LISTENER, FixtureType.WATER)!!

      val body = listener.getBody()
      body.setBodySense(BodySense.IN_WATER, true)

      val entity = listener.getEntity()
      if (entity is Megaman &&
          !entity.body.isSensing(BodySense.FEET_ON_GROUND) &&
          !entity.isBehaviorActive(BehaviorType.WALL_SLIDING))
          entity.aButtonTask = AButtonTask.SWIM
    }

    // body, force
    else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
      val (bodyFixture, force) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE)!!

      val forceAlteration = force.getVelocityAlteration(bodyFixture, delta)
      VelocityAlterator.alterate(bodyFixture.getBody(), forceAlteration, delta)

      force.getRunnable()?.invoke()
    }

    // TODO: should this be in the continue contact phase?
    // body, gravity change
    else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.GRAVITY_CHANGE)) {
      val (bodyFixture, gravityChangeFixture) =
          contact.getFixturesInOrder(FixtureType.BODY, FixtureType.GRAVITY_CHANGE)!!

      val canChangeGravity =
          bodyFixture.properties.getOrDefault(ConstKeys.GRAVITY_ROTATABLE, true) as Boolean
      if (!canChangeGravity) return

      val body = bodyFixture.getBody()
      val pd = body.positionDelta
      val bodyPointToCheck =
          if (pd.x > 0f) {
            if (pd.y > 0f) body.getTopRightPoint()
            else if (pd.y < 0f) body.getBottomRightPoint() else body.getCenterRightPoint()
          } else if (pd.x < 0f) {
            if (body.positionDelta.y > 0f) body.getTopLeftPoint()
            else if (body.positionDelta.y < 0f) body.getBottomLeftPoint()
            else body.getCenterLeftPoint()
          } else {
            if (pd.y > 0f) body.getTopCenterPoint()
            else if (pd.y < 0f) body.getBottomCenterPoint() else body.getCenter()
          }

      if (!gravityChangeFixture.bodyRelativeShape!!.contains(bodyPointToCheck)) return

      val direction =
          gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return

      val entity = bodyFixture.getEntity()
      if (entity is IDirectionRotatable && entity.directionRotation != direction)
          entity.directionRotation = direction
    }

    // laser, block
    else if (contact.fixturesMatch(FixtureType.LASER, FixtureType.BLOCK)) {
      GameLogger.debug(TAG, "continueContact(): Laser-Block, contact = $contact")
      val (laser, block) = contact.getFixturesInOrder(FixtureType.LASER, FixtureType.BLOCK)!!

      val laserEntity = laser.getEntity()
      val blockEntity = block.getEntity()

      if (laserEntity != blockEntity) {
        val blockRectangle = block.shape as GameRectangle
        val laserLine = laser.shape as GameLine

        val intersections = laser.properties.get(ConstKeys.COLLECTION) as MutableCollection<Vector2>?
        GameLogger.debug(
            TAG,
            "continueContact(): Laser-Block, current intersections = $intersections")
        intersections?.let {
          val temp = Array<Vector2>()
          if (ShapeUtils.intersectRectangleAndLine(blockRectangle, laserLine, temp)) {
            it.addAll(temp)
            GameLogger.debug(
                TAG, "continueContact(): Laser-Block, new intersections = $it")
          }
        }
      }
    }

    // side, ice
    else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
      val (side, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE)!!

      val body = side.getBody()
      val sideType = side.getProperty(ConstKeys.SIDE)

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true)
      else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, true)
    }

    // side, block
    else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.BLOCK)) {
      val (side, block) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.BLOCK)!!

      if (block.bodyHasLabel(BodyLabel.NO_SIDE_TOUCHIE)) return

      val body = side.getBody()
      val sideType = side.getProperty(ConstKeys.SIDE)

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
      else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
    }
  }

  override fun endContact(contact: Contact, delta: Float) {
    // do not check for contacts within the same entity
    if (contact.fixture1.getEntity() == contact.fixture2.getEntity()) return

    // side, block
    if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.BLOCK)) {
      GameLogger.debug(TAG, "End Contact: Side-Block, contact = $contact")
      val (side, other) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.BLOCK)!!

      if (other.getBody().hasBodyLabel(BodyLabel.NO_SIDE_TOUCHIE)) return

      val body = side.getBody()
      val sideType = side.getProperty(ConstKeys.SIDE)

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, false)
      else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, false)
    }

    // side, ice
    else if (contact.fixturesMatch(FixtureType.SIDE, FixtureType.ICE)) {
      GameLogger.debug(TAG, "End Contact: Side-Ice, contact = $contact")
      val (side, _) = contact.getFixturesInOrder(FixtureType.SIDE, FixtureType.ICE)!!

      val body = side.getBody()
      val sideType = side.getProperty(ConstKeys.SIDE)

      if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, false)
      else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, false)
    }

    // feet, block
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.BLOCK)) {
      GameLogger.debug(TAG, "End Contact: Feet-Block, contact = $contact")
      val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.BLOCK)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_ON_GROUND, false)

      val entity = feet.getEntity()
      if (entity is Megaman)
          entity.aButtonTask =
              if (entity.body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM
              else AButtonTask.AIR_DASH
    }

    // feet, ice
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.ICE)) {
      GameLogger.debug(TAG, "End Contact: Feet-Ice, contact = $contact")
      val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.ICE)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_ON_ICE, false)
    }

    // head, block
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.BLOCK)) {
      GameLogger.debug(TAG, "End Contact: Head-Block, contact = $contact")
      val (head, _) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.BLOCK)!!

      val body = head.getBody()
      body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, false)
    }

    // feet, ladder
    else if (contact.fixturesMatch(FixtureType.FEET, FixtureType.LADDER)) {
      GameLogger.debug(TAG, "End Contact: Feet-Ladder, contact = $contact")
      val (feet, _) = contact.getFixturesInOrder(FixtureType.FEET, FixtureType.LADDER)!!

      val body = feet.getBody()
      body.setBodySense(BodySense.FEET_TOUCHING_LADDER, false)

      if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) body.properties.remove(ConstKeys.LADDER)
    }

    // head, ladder
    else if (contact.fixturesMatch(FixtureType.HEAD, FixtureType.LADDER)) {
      GameLogger.debug(TAG, "End Contact: Head-Ladder, contact = $contact")
      val (head, _) = contact.getFixturesInOrder(FixtureType.HEAD, FixtureType.LADDER)!!

      val body = head.getBody()
      body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, false)

      if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) body.properties.remove(ConstKeys.LADDER)
    }

    // body, force
    else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.FORCE)) {
      GameLogger.debug(TAG, "End Contact: Body-Force, contact = $contact")
      val (bodyFixture, force) = contact.getFixturesInOrder(FixtureType.BODY, FixtureType.FORCE)!!

      val forceAlteration = force.getVelocityAlteration(bodyFixture, delta)
      VelocityAlterator.alterate(bodyFixture.getBody(), forceAlteration, delta)

      force.getRunnable()?.invoke()
    }

    // TODO:
    //   nothing should be done on end contact for gravity change, and instead there should
    //   be a gravity change object the player enters to set the direction back to UP
    // body, gravity change
    /*
    else if (contact.fixturesMatch(FixtureType.BODY, FixtureType.GRAVITY_CHANGE)) {
      GameLogger.debug(TAG, "End Contact: Body-GravityChange, contact = $contact")
      val (bodyFixture, _) =
          contact.getFixturesInOrder(FixtureType.BODY, FixtureType.GRAVITY_CHANGE)!!
      val entity = bodyFixture.getEntity()
      if (entity is IDirectionRotatable && entity.isDirectionRotated(Direction.DOWN))
          entity.directionRotation = Direction.UP
      /*
      if (entity is IUpsideDownable) entity.upsideDown = false
       */
    }
     */

    // water-listener, water
    else if (contact.fixturesMatch(FixtureType.WATER_LISTENER, FixtureType.WATER)) {
      val (listener, water) =
          contact.getFixturesInOrder(FixtureType.WATER_LISTENER, FixtureType.WATER)!!
      listener.getBody().setBodySense(BodySense.IN_WATER, false)

      val listenerEntity = listener.getEntity()
      if (listenerEntity is Megaman) listenerEntity.aButtonTask = AButtonTask.AIR_DASH

      game.audioMan.playSound(SoundAsset.SPLASH_SOUND, false)
      Splash.generate(game, listener.getBody(), water.getBody())
    }
  }
}
