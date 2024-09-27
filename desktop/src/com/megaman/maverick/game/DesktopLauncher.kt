package com.megaman.maverick.game

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.megaman.maverick.game.screens.levels.Level

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
object DesktopLauncher {

    private const val DEFAULT_FPS = ConstVals.MID_FPS
    private const val DEFAULT_WIDTH = 800
    private const val DEFAULT_HEIGHT = 600
    private const val DEFAULT_FULLSCREEN = false
    private const val DEFAULT_VSYNC = false
    private const val DEFAULT_DEBUG = false
    private const val DEFAULT_FIXED_STEP_SCALAR = 1.0f
    private const val DEFAULT_MUSIC_VOLUME = 0.5f
    private const val DEFAULT_SOUND_VOLUME = 0.5f
    private const val DEFAULT_START_SCREEN = "simple"
    private const val DEFAULT_LEVEL = "null"
    private const val TITLE = "Megaman Maverick"

    @JvmStatic
    fun main(args: Array<String>) {
        val appArgs = DesktopAppArgs()
        val jCommander = JCommander.newBuilder().addObject(appArgs).build()
        try {
            jCommander.parse(*args)
        } catch (e: ParameterException) {
            System.err.println("[Application] Error in main method while parsing parameters: " + e.message)
            e.printStackTrace()
            jCommander.usage()
            System.exit(1)
        }

        System.out.println("Game loaded with arguments:")
        System.out.println("- FPS: " + appArgs.fps)
        System.out.println("- Width: " + appArgs.width)
        System.out.println("- Height: " + appArgs.height)
        System.out.println("- Fullscreen: " + appArgs.fullScreen)
        System.out.println("- Vsync: " + appArgs.vsync)
        System.out.println("- Debug: " + appArgs.debug)
        System.out.println("- Start Screen: " + appArgs.startScreen)
        System.out.println("- Level: " + appArgs.level)
        System.out.println("- Fixed Step Scalar: " + appArgs.fixedStepScalar)
        System.out.println("- Music volume: " + appArgs.musicVolume)
        System.out.println("- Sound volume: " + appArgs.soundVolume)

        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle(TITLE)
        config.useVsync(appArgs.vsync)
        // TODO: disable dynamic fps until issues regarding physics tied to fps are resolved,
        //  for now use default fps
        config.setIdleFPS(DEFAULT_FPS)
        config.setForegroundFPS(DEFAULT_FPS)
        /*
        config.setIdleFPS(appArgs.fps);
        config.setForegroundFPS(appArgs.fps);
         */
        config.setWindowedMode(appArgs.width, appArgs.height)
        if (appArgs.fullScreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())
        }

        val params = MegamanMaverickGameParams()
        params.debug = appArgs.debug
        params.fixedStepScalar = appArgs.fixedStepScalar
        params.musicVolume = appArgs.musicVolume
        params.soundVolume = appArgs.soundVolume
        val startScreenOption = if (appArgs.startScreen.isBlank() || appArgs.startScreen.lowercase() == "main") {
            StartScreenOption.MAIN
        } else if (appArgs.startScreen.lowercase() == "level") {
            StartScreenOption.LEVEL
        } else if (appArgs.startScreen.lowercase() == "simple") {
            StartScreenOption.SIMPLE
        } else {
            System.err.println("Invalid start screen option: " + appArgs.startScreen)
            jCommander.usage()
            System.exit(1)
            return
        }
        params.startScreen = startScreenOption

        if (startScreenOption == StartScreenOption.LEVEL) {
            val level = Level.valueOf(appArgs.level.uppercase())
            params.startLevel = level
        }

        val game = MegamanMaverickGame(params)
        // TODO: disable dynamic fps until issues regarding physics tied to fps are resolved,
        //  for now always use the default fps
        // game.setTargetFPS(appArgs.fps);
        game.setTargetFPS(DEFAULT_FPS)
        game.setUseVsync(appArgs.vsync)

        // TODO: refine pausing/resuming logic in regards to game window focus
        config.setWindowListener(object : Lwjgl3WindowAdapter() {
            @Override
            override fun iconified(isIconified: Boolean) {
                game.pause()
            }

            @Override
            override fun focusGained() {
                game.resume()
            }

            @Override
            override fun focusLost() {
                game.pause()
            }
        })

        try {
            Lwjgl3Application(game, config)
        } catch (e: Exception) {
            System.err.println("Exception while running game!")
            System.err.println("Exception message: " + e.message)
            System.err.println("Exception stacktrace: ")
            e.printStackTrace()
            game.dispose()
        }
    }

    class DesktopAppArgs {
        @Parameter(
            names = ["--fps"],
            description = "Frames per second: min of 30 and max of 90. Default value = " + DEFAULT_FPS + "."
        )
        var fps: Int = DEFAULT_FPS

        @Parameter(
            names = ["--width"],
            description = "Window width: min of 600. Default value = " + DEFAULT_WIDTH + "."
        )
        var width: Int = DEFAULT_WIDTH

        @Parameter(
            names = ["--height"],
            description = "Window height: min of 400. Default value = " + DEFAULT_HEIGHT + "."
        )
        var height: Int = DEFAULT_HEIGHT

        @Parameter(
            names = ["--fullscreen"],
            description = "Enable fullscreen. Default value = " + DEFAULT_FULLSCREEN + "."
        )
        var fullScreen: Boolean = DEFAULT_FULLSCREEN

        @Parameter(names = ["--vsync"], description = "Enable vsync. Default value = " + DEFAULT_VSYNC + ".")
        var vsync: Boolean = DEFAULT_VSYNC

        @Parameter(
            names = ["--debug"], description = ("Enable debugging mode which turns on debug text rendering and " +
                    "debug shape rendering. Default value = " + DEFAULT_DEBUG + ".")
        )
        var debug: Boolean = DEFAULT_DEBUG

        @Parameter(
            names = ["--startScreen"], description = ("The screen to start the game app on. Options: \"main\", " +
                    "\"level\", \"simple\". Options not case sensitive. Default value = " + DEFAULT_START_SCREEN + ".")
        )
        var startScreen: String = DEFAULT_START_SCREEN

        @Parameter(
            names = ["--level"], description = ("The level to start the game app on. This option only works if " +
                    "\"level\" has been selected as the start screen. Choose the name of the level from the Level " +
                    "enum class (not case sensitive). No default value. If the level is not found, an exception is " +
                    "thrown.")
        )
        var level: String = DEFAULT_LEVEL

        @Parameter(
            names = ["--fixedStepScalar"], description = ("Sets the world fixed step scalar, useful for " +
                    "debugging. Default value is " + DEFAULT_FIXED_STEP_SCALAR + ". Should be default value if not " +
                    "debugging")
        )
        var fixedStepScalar: Float = DEFAULT_FIXED_STEP_SCALAR

        @Parameter(
            names = ["--musicVolume"], description = ("Sets the music volume. Must be between 0 and 1. Default " +
                    "value is " + DEFAULT_MUSIC_VOLUME)
        )
        var musicVolume: Float = DEFAULT_MUSIC_VOLUME

        @Parameter(
            names = ["--soundVolume"], description = ("Sets the sound volume. Must be between 0 and 1. Default " +
                    "value is " + DEFAULT_SOUND_VOLUME)
        )
        var soundVolume: Float = DEFAULT_SOUND_VOLUME
    }
}
