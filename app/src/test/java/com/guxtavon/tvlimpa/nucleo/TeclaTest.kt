package com.guxtavon.tvlimpa.nucleo

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TeclaTest {
    @Test fun ok_aceita_os_tres() {
        for (k in listOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)) {
            assertEquals(Tecla.OK, teclaDe(k))
        }
    }

    @Test fun voltar() {
        assertEquals(Tecla.VOLTAR, teclaDe(KeyEvent.KEYCODE_BACK))
        assertEquals(Tecla.VOLTAR, teclaDe(KeyEvent.KEYCODE_ESCAPE))
    }

    @Test fun setas() = assertEquals(Tecla.DIREITA, teclaDe(KeyEvent.KEYCODE_DPAD_RIGHT))
    @Test fun outra_tecla_e_nula() = assertNull(teclaDe(KeyEvent.KEYCODE_VOLUME_UP))
}
