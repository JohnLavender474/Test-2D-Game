package com.megaman.maverick.game.entities.megaman

import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.pairTo
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.bosses.*
import com.megaman.maverick.game.entities.bosses.gutstank.GutsTank
import com.megaman.maverick.game.entities.bosses.gutstank.GutsTankFist
import com.megaman.maverick.game.entities.bosses.sigmarat.SigmaRat
import com.megaman.maverick.game.entities.bosses.sigmarat.SigmaRatClaw
import com.megaman.maverick.game.entities.enemies.*
import com.megaman.maverick.game.entities.explosions.*
import com.megaman.maverick.game.entities.hazards.*
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.entities.special.Togglee

object MegamanDamageNegotations {

    private val dmgNegotiations = objectMapOf<String, DamageNegotiation>(
        Bullet.TAG pairTo dmgNeg(2),
        ChargedShot.TAG pairTo dmgNeg(4),
        Bat.TAG pairTo dmgNeg(2),
        Met.TAG pairTo dmgNeg(2),
        DragonFly.TAG pairTo dmgNeg(3),
        FloatingCan.TAG pairTo dmgNeg(2),
        FlyBoy.TAG pairTo dmgNeg(3),
        GapingFish.TAG pairTo dmgNeg(2),
        SpringHead.TAG pairTo dmgNeg(3),
        SuctionRoller.TAG pairTo dmgNeg(2),
        MagFly.TAG pairTo dmgNeg(3),
        Explosion.TAG pairTo dmgNeg(2),
        JoeBall.TAG pairTo dmgNeg(3),
        Snowball.TAG pairTo dmgNeg(1),
        // SnowballExplosion.TAG pairTo dmgNeg(1),
        SwinginJoe.TAG pairTo dmgNeg(2),
        SniperJoe.TAG pairTo dmgNeg(3),
        ShieldAttacker.TAG pairTo dmgNeg(4),
        Penguin.TAG pairTo dmgNeg(3),
        Hanabiran.TAG pairTo dmgNeg(3),
        Petal.TAG pairTo dmgNeg(3),
        CaveRock.TAG pairTo dmgNeg(3),
        CaveRocker.TAG pairTo dmgNeg(3),
        CaveRockExplosion.TAG pairTo dmgNeg(2),
        Elecn.TAG pairTo dmgNeg(3),
        ElectricBall.TAG pairTo dmgNeg(3),
        Ratton.TAG pairTo dmgNeg(2),
        PicketJoe.TAG pairTo dmgNeg(3),
        Picket.TAG pairTo dmgNeg(3),
        LaserBeamer.TAG pairTo dmgNeg(3),
        CartinJoe.TAG pairTo dmgNeg(3),
        Bolt.TAG pairTo dmgNeg(3),
        ElectrocutieChild.TAG pairTo dmgNeg(3),
        Togglee.TAG pairTo dmgNeg(3),
        Eyee.TAG pairTo dmgNeg(3),
        Adamski.TAG pairTo dmgNeg(3),
        UpNDown.TAG pairTo dmgNeg(3),
        BigJumpingJoe.TAG pairTo dmgNeg(6),
        SniperJoeShield.TAG pairTo dmgNeg(2),
        SuicideBummer.TAG pairTo dmgNeg(3),
        Gachappan.TAG pairTo dmgNeg(5),
        ExplodingBall.TAG pairTo dmgNeg(3),
        Imorm.TAG pairTo dmgNeg(3),
        SpikeBall.TAG pairTo dmgNeg(8),
        Peat.TAG pairTo dmgNeg(2),
        BulbBlaster.TAG pairTo dmgNeg(2),
        Bospider.TAG pairTo dmgNeg(5),
        BabySpider.TAG pairTo dmgNeg(2),
        GutsTank.TAG pairTo dmgNeg(3),
        GutsTankFist.TAG pairTo dmgNeg(3),
        PurpleBlast.TAG pairTo dmgNeg(3),
        HeliMet.TAG pairTo dmgNeg(3),
        SigmaRat.TAG pairTo dmgNeg(3),
        SigmaRatElectricBall.TAG pairTo dmgNeg(3),
        SigmaRatElectricBallExplosion.TAG pairTo dmgNeg(2),
        SigmaRatClaw.TAG pairTo dmgNeg(2),
        Fireball.TAG pairTo dmgNeg(3),
        BoulderProjectile.TAG pairTo dmgNeg {
            it as BoulderProjectile
            when (it.size) {
                Size.LARGE -> 4
                Size.MEDIUM -> 2
                Size.SMALL -> 1
            }
        },
        PetitDevil.TAG pairTo dmgNeg(3),
        PetitDevilChild.TAG pairTo dmgNeg(2),
        Shotman.TAG pairTo dmgNeg(2),
        Snowhead.TAG pairTo dmgNeg(2),
        SnowheadThrower.TAG pairTo dmgNeg(3),
        Spiky.TAG pairTo dmgNeg(4),
        PenguinMiniBoss.TAG pairTo dmgNeg(3),
        BabyPenguin.TAG pairTo dmgNeg(2),
        UFOBomb.TAG pairTo dmgNeg(3),
        RollingBot.TAG pairTo dmgNeg(3),
        RollingBotShot.TAG pairTo dmgNeg(3),
        AcidGoop.TAG pairTo dmgNeg(3),
        ToxicBarrelBot.TAG pairTo dmgNeg(3),
        ToxicGoopShot.TAG pairTo dmgNeg(3),
        ToxicGoopSplash.TAG pairTo dmgNeg(3),
        ReactorMonkeyBall.TAG pairTo dmgNeg(3),
        ReactorMonkeyMiniBoss.TAG pairTo dmgNeg(3),
        SmokePuff.TAG pairTo dmgNeg(2),
        TubeBeam.TAG pairTo dmgNeg(5),
        ReactorMan.TAG pairTo dmgNeg(3),
        ReactManProjectile.TAG pairTo dmgNeg(3),
        FlameThrower.TAG pairTo dmgNeg(6),
        Popoheli.TAG pairTo dmgNeg(3),
        AngryFlameBall.TAG pairTo dmgNeg(3),
        LavaDrop.TAG pairTo dmgNeg(6),
        PopupCanon.TAG pairTo dmgNeg(3),
        Asteroid.TAG pairTo dmgNeg(3),
        AsteroidExplosion.TAG pairTo dmgNeg(3),
        MoonHeadMiniBoss.TAG pairTo dmgNeg(3),
        BunbyRedRocket.TAG pairTo dmgNeg(3),
        BunbyTank.TAG pairTo dmgNeg(3),
        FireMet.TAG pairTo dmgNeg(3),
        FireMetFlame.TAG pairTo dmgNeg(3),
        Robbit.TAG pairTo dmgNeg(3),
        Pipi.TAG pairTo dmgNeg(3),
        PipiEgg.TAG pairTo dmgNeg(3),
        Copipi.TAG pairTo dmgNeg(3),
        MechaDragonMiniBoss.TAG pairTo dmgNeg(3),
        SpitFireball.TAG pairTo dmgNeg(3),
        BombPotton.TAG pairTo dmgNeg(3),
        SmallMissile.TAG pairTo dmgNeg(3),
        GreenExplosion.TAG pairTo dmgNeg(3),
        Arigock.TAG pairTo dmgNeg(3),
        ArigockBall.TAG pairTo dmgNeg(3),
        TotemPolem.TAG pairTo dmgNeg(3),
        CactusLauncher.TAG pairTo dmgNeg(3),
        CactusMissile.TAG pairTo dmgNeg(3),
        ColtonJoe.TAG pairTo dmgNeg(3),
        SphinxBall.TAG pairTo dmgNeg(3),
        SphinxMiniBoss.TAG pairTo dmgNeg(3),
        SpikeBot.TAG pairTo dmgNeg(3),
        Needle.TAG pairTo dmgNeg(3),
        JetpackIceBlaster.TAG pairTo dmgNeg(3),
        TeardropBlast.TAG pairTo dmgNeg(3),
        FallingIcicle.TAG pairTo dmgNeg(3),
        SmallIceCube.TAG pairTo dmgNeg(1),
        UnderwaterFan.TAG pairTo dmgNeg(5),
        Tropish.TAG pairTo dmgNeg(3),
        SeaMine.TAG pairTo dmgNeg(3),
        Sealion.TAG pairTo dmgNeg(3),
        SealionBall.TAG pairTo dmgNeg(3),
        YellowTiggerSquirt.TAG pairTo dmgNeg(3),
        UFOhNoBot.TAG pairTo dmgNeg(3),
        TankBot.TAG pairTo dmgNeg(3),
        Darspider.TAG pairTo dmgNeg(3),
        WalrusBot.TAG pairTo dmgNeg(3),
        BigFishNeo.TAG pairTo dmgNeg(4),
        GlacierMan.TAG pairTo dmgNeg(4),
        Matasaburo.TAG pairTo dmgNeg(3),
        CarriCarry.TAG pairTo dmgNeg(3),
        Cactus.TAG pairTo dmgNeg(2),
        DesertMan.TAG pairTo dmgNeg(3),
        FireDispensenator.TAG pairTo dmgNeg(3),
        FireWall.TAG pairTo dmgNeg(4),
        DemonMet.TAG pairTo dmgNeg(3),
        FirePellet.TAG pairTo dmgNeg(3),
        InfernoMan.TAG pairTo dmgNeg(3),
        MagmaOrb.TAG pairTo dmgNeg(4),
        MagmaMeteor.TAG pairTo dmgNeg(4),
        MagmaExplosion.TAG pairTo dmgNeg(4),
        MagmaGoop.TAG pairTo dmgNeg(4),
        MagmaGoopExplosion.TAG pairTo dmgNeg(4),
        MagmaWave.TAG pairTo dmgNeg(4),
        MagmaPellet.TAG pairTo dmgNeg(3),
        MagmaFlame.TAG pairTo dmgNeg(3),
        MoonScythe.TAG pairTo dmgNeg(3),
        MoonMan.TAG pairTo dmgNeg(3),
        SharpStar.TAG pairTo dmgNeg(3),
        StarExplosion.TAG pairTo dmgNeg(3),
        Wanaan.TAG pairTo dmgNeg(3),
        RatRobot.TAG pairTo dmgNeg(3),
        NuttGlider.TAG pairTo dmgNeg(3),
        Nutt.TAG pairTo dmgNeg(3),
        Screwie.TAG pairTo dmgNeg(3),
        Saw.TAG pairTo dmgNeg(6)
    )

    fun contains(tag: String) = dmgNegotiations.containsKey(tag)

    fun get(tag: String): DamageNegotiation = dmgNegotiations[tag]
}
