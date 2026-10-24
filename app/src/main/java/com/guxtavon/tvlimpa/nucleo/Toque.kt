package com.guxtavon.tvlimpa.nucleo

/** O que uma apertada do OK pede. */
enum class Gesto { NADA, SEGURAR, OK }

/** Quanto tempo o OK fica apertado pra virar "segurar". */
const val SEGURAR_MS = 600L

/**
 * O OK do controle, decidido pelo tempo e não pelo `FLAG_LONG_PRESS` (o controle da TCL pode
 * mandar as próprias repetições depois de 100-250 ms, e cada uma dessas o Android marca como long
 * press). Regras:
 * - a primeira descida marca o começo; as repetições só contam o tempo desde ela;
 * - a primeira repetição com [limiteMs] ou mais pede [Gesto.SEGURAR], uma vez só;
 * - ao soltar, quem não usou o segurar (não chegou ao tempo, ou a camada não tinha o que fazer
 *   com ele) é um [Gesto.OK]; quem usou não abre nada;
 * - repetição ou subida sem a descida ter chegado aqui (o foco entrou no meio) não fazem nada.
 *
 * O começo é o `eventTime` da primeira descida, guardado aqui: no Android 9 a repetição que vem do
 * próprio controle pode chegar com um `downTime` novo.
 */
class Toque(private val limiteMs: Long = SEGURAR_MS) {
    private var inicio = SEM
    private var pediu = false
    private var usou = false

    /** Uma descida do OK ([repeticao] é o `repeatCount` do evento). */
    fun descer(repeticao: Int, eventTime: Long): Gesto {
        if (repeticao == 0) {
            inicio = eventTime
            pediu = false
            usou = false
            return Gesto.NADA
        }
        if (inicio == SEM || pediu || eventTime - inicio < limiteMs) return Gesto.NADA
        pediu = true
        return Gesto.SEGURAR
    }

    /** A camada de cima fez algo com o [Gesto.SEGURAR]: o soltar não abre nada. */
    fun segurou() {
        if (pediu) usou = true
    }

    /** O OK subiu. [cancelado]: o sistema cancelou a apertada (`KeyEvent.isCanceled`). */
    fun subir(cancelado: Boolean): Gesto {
        val curto = inicio != SEM && !usou && !cancelado
        soltar()
        return if (curto) Gesto.OK else Gesto.NADA
    }

    /** Esquece a apertada (a tela pausou, o Home chegou, o modal abriu). */
    fun soltar() {
        inicio = SEM
        pediu = false
        usou = false
    }

    private companion object {
        const val SEM = Long.MIN_VALUE
    }
}
