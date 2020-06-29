package io.github.jsnimda.common.vanilla.render

import io.github.jsnimda.common.gui.Rectangle
import io.github.jsnimda.common.gui.Size
import io.github.jsnimda.common.vanilla.Vanilla
import io.github.jsnimda.common.vanilla.VanillaState
import io.github.jsnimda.common.vanilla.alias.LiteralText
import io.github.jsnimda.common.vanilla.alias.Screen

val rScreenWidth
  get() = Vanilla.window().scaledWidth
val rScreenHeight
  get() = Vanilla.window().scaledHeight
val rScreenSize
  get() = Size(rScreenWidth, rScreenHeight)
val rScreenBounds
  get() = Rectangle(0, 0, rScreenWidth, rScreenHeight)

fun rRenderDirtBackground() { // Screen.renderDirtBackground
  (Vanilla.screen() ?: dummyScreen).renderDirtBackground(0)
}

fun rRenderBlackOverlay() { // Screen.renderBackground
  rFillGradient(0, 0, rScreenWidth, rScreenHeight, -1072689136, -804253680)
}

fun rRenderVanillaScreenBackground() { // Screen.renderBackground
  if (VanillaState.inGame()) {
    rRenderBlackOverlay()
  } else {
    rRenderDirtBackground()
  }
}

private val dummyScreen = object : Screen(
  LiteralText(
    ""
  )
) {}