package com.guxtavon.tvlimpa.nucleo

import android.view.KeyEvent

enum class Tecla { CIMA, BAIXO, ESQUERDA, DIREITA, OK, VOLTAR }

/** Traduz o código da tecla do controle; teclas que o app não usa dão nulo. */
fun teclaDe(keyCode: Int): Tecla? = when (keyCode) {
    KeyEvent.KEYCODE_DPAD_UP -> Tecla.CIMA
    KeyEvent.KEYCODE_DPAD_DOWN -> Tecla.BAIXO
    KeyEvent.KEYCODE_DPAD_LEFT -> Tecla.ESQUERDA
    KeyEvent.KEYCODE_DPAD_RIGHT -> Tecla.DIREITA
    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> Tecla.OK
    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> Tecla.VOLTAR
    else -> null
}
