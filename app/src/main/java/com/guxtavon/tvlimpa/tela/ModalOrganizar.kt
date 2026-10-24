package com.guxtavon.tvlimpa.tela

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.guxtavon.tvlimpa.nucleo.AppInfo
import com.guxtavon.tvlimpa.nucleo.Tecla

/** Uma opção do modal. [perigo] pinta de vermelho (só o "Tirar da tela inicial"). */
data class Opcao(val texto: String, val icone: Int, val perigo: Boolean, val faz: () -> Unit)

/**
 * O modal de organizar (`.fundo-modal` / `.modal` / `.opcao` do protótipo): a tela de trás
 * desfocada e escurecida (55% preto), e a caixa com a capa, o nome e as opções.
 *
 * Abre com o fundo em fade de 350 ms e a caixa subindo 4% da altura e indo de 0,96 pra 1 em 450 ms
 * na mola (opacidade em 300 ms). Fecha fazendo o caminho de volta. Cima e baixo andam nas opções;
 * OK fecha e executa a opção; o voltar só fecha (cancela).
 */
class ModalOrganizar(ctx: Context) : FrameLayout(ctx) {
    private val fundo = Fundo(ctx)
    private val caixa = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false
        setPadding(px(14f), px(20f), px(14f), px(14f))
        background = GradientDrawable().apply {
            setColor(0xEB18181B.toInt())
            cornerRadius = ctx.dp(14f)
            setStroke(1, LINHA)
        }
        elevation = ctx.dp(20f)
        outlineSpotShadowColor = Color.BLACK
        outlineAmbientShadowColor = Color.BLACK
    }
    private val capa = View(ctx).apply {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, ctx.dp(6f))
            }
        }
        clipToOutline = true
    }
    private val nome = TextView(ctx).apply {
        typeface = Estilo.negrito(ctx)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        letterSpacing = -.02f
        setTextColor(Estilo.TEXTO)
        includeFontPadding = false
        setSingleLine(true)
        ellipsize = TextUtils.TruncateAt.END
    }
    private val linhas = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false
    }

    private var opcoes: List<Opcao> = emptyList()
    private var vistas: List<OpcaoView> = emptyList()
    private var mi = 0
    private var anim: Animator? = null

    /** Aberto (ou abrindo); fechando já conta como fechado. */
    var aberto = false
        private set

    init {
        visibility = View.GONE
        clipChildren = false
        addView(fundo, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val cab = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(6f), 0, px(6f), px(14f))
            addView(capa, LinearLayout.LayoutParams(px(80f), px(45f)))
            addView(nome, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = px(11f) })
        }
        caixa.addView(cab, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        caixa.addView(linhas, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(caixa, LayoutParams(px(LARGURA_DP), LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    /** Abre por cima de tudo. [fundo] é a tela de trás já reduzida e borrada ([Desfoque.de]). */
    fun abrir(app: AppInfo, capa: Drawable?, opcoes: List<Opcao>, fundo: Bitmap?) {
        anim?.cancel()
        aberto = true
        this.opcoes = opcoes
        this.capa.background = capa ?: Companion.capa(context, null, app.nome)
        nome.text = app.nome
        linhas.removeAllViews()
        vistas = opcoes.map { o -> OpcaoView(context, o.icone, o.texto, o.perigo) }
        for (v in vistas) linhas.addView(v, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, px(36f)))
        mi = 0
        vistas.firstOrNull()?.focar(true, animar = false)

        this.fundo.imagem(fundo)
        visibility = View.VISIBLE
        // A caixa ainda não foi medida com as opções novas: mede agora pra saber os 4% da altura.
        caixa.measure(
            MeasureSpec.makeMeasureSpec(px(LARGURA_DP), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        val desce = caixa.measuredHeight * .04f
        this.fundo.alpha = 0f
        caixa.alpha = 0f
        caixa.scaleX = .96f
        caixa.scaleY = .96f
        caixa.translationY = desce
        caixa.setLayerType(LAYER_TYPE_HARDWARE, null)
        anim = AnimatorSet().apply {
            playTogether(
                anima(this@ModalOrganizar.fundo, View.ALPHA, 1f, FUNDO_MS, Estilo.EASE),
                anima(caixa, View.ALPHA, 1f, OPACIDADE_MS, Estilo.EASE),
                anima(caixa, View.SCALE_X, 1f, CAIXA_MS, Estilo.MOLA),
                anima(caixa, View.SCALE_Y, 1f, CAIXA_MS, Estilo.MOLA),
                anima(caixa, View.TRANSLATION_Y, 0f, CAIXA_MS, Estilo.MOLA),
            )
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    caixa.setLayerType(LAYER_TYPE_NONE, null)
                }
            })
            start()
        }
    }

    /** Uma tecla com o modal aberto. */
    fun tecla(t: Tecla): Boolean {
        if (!aberto) return false
        when (t) {
            Tecla.CIMA -> if (mi > 0) mover(mi - 1) else return false
            Tecla.BAIXO -> if (mi < vistas.size - 1) mover(mi + 1) else return false
            Tecla.OK -> {
                val o = opcoes.getOrNull(mi) ?: return false
                fechar()
                o.faz()
            }
            Tecla.VOLTAR -> fechar()
            else -> return false
        }
        return true
    }

    /** Fecha com a animação de volta (cancelar). */
    fun fechar() {
        if (!aberto) return
        aberto = false
        anim?.cancel()
        caixa.setLayerType(LAYER_TYPE_HARDWARE, null)
        val desce = caixa.height * .04f
        anim = AnimatorSet().apply {
            playTogether(
                anima(fundo, View.ALPHA, 0f, FUNDO_MS, Estilo.EASE),
                anima(caixa, View.ALPHA, 0f, OPACIDADE_MS, Estilo.EASE),
                anima(caixa, View.SCALE_X, .96f, CAIXA_MS, Estilo.MOLA),
                anima(caixa, View.SCALE_Y, .96f, CAIXA_MS, Estilo.MOLA),
                anima(caixa, View.TRANSLATION_Y, desce, CAIXA_MS, Estilo.MOLA),
            )
            addListener(object : AnimatorListenerAdapter() {
                private var cancelado = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelado = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    caixa.setLayerType(LAYER_TYPE_NONE, null)
                    if (!cancelado) soltar()
                }
            })
            start()
        }
    }

    /** Some na hora, sem animação (o Home). */
    fun sumir() {
        aberto = false
        anim?.cancel()
        anim = null
        caixa.setLayerType(LAYER_TYPE_NONE, null)
        soltar()
    }

    private fun soltar() {
        visibility = View.GONE
        fundo.imagem(null)
        capa.background = null
        opcoes = emptyList()
    }

    private fun mover(novo: Int) {
        vistas.getOrNull(mi)?.focar(false)
        mi = novo
        vistas[mi].focar(true)
    }

    private fun px(dp: Float): Int = context.dp(dp).toInt()

    /** A tela de trás: o bitmap pequeno esticado com filtro, mais 55% de preto. */
    private class Fundo(ctx: Context) : View(ctx) {
        private var bitmap: Bitmap? = null
        private val tinta = Paint(Paint.FILTER_BITMAP_FLAG)
        private val destino = Rect()

        fun imagem(b: Bitmap?) {
            bitmap = b
            invalidate()
        }

        override fun hasOverlappingRendering() = false

        override fun onDraw(canvas: Canvas) {
            val b = bitmap
            if (b != null) {
                destino.set(0, 0, width, height)
                canvas.drawBitmap(b, null, destino, tinta)
            } else {
                canvas.drawColor(0xFF070707.toInt())
            }
            canvas.drawColor(ESCURO)
        }
    }

    /**
     * Uma opção: ícone de linha e texto. Com foco, fundo branco e texto quase preto (o "Tirar":
     * vermelho escuro), e cresce 2%. Fundo e cor em 250 ms no `ease`; a escala em 300 ms na mola.
     */
    private class OpcaoView(ctx: Context, iconeRes: Int, private val texto: String, perigo: Boolean) : View(ctx) {
        private val raio = ctx.dp(8f)
        private val margem = ctx.dp(12f)
        private val vao = ctx.dp(9f)
        private val ladoIcone = ctx.dp(14f).toInt()
        private val icone: Bitmap = mascaraDe(ctx, iconeRes, ladoIcone)
        private val corTexto = if (perigo) Estilo.VERMELHO_CLARO else NORMAL
        private val corTextoFoco = if (perigo) Estilo.VERMELHO else Estilo.ESCURO
        private val fundo = Paint(Paint.ANTI_ALIAS_FLAG)
        private val tinta = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            typeface = Estilo.regular(ctx)
            textSize = ctx.dp(13f)
        }
        private val meioTexto = tinta.fontMetrics.let { -(it.ascent + it.descent) / 2f }
        private var cor = 0f
        private var corDe = 0f
        private var mov = 0f
        private var movDe = 0f
        private var alvo = 0f
        private var anim: ValueAnimator? = null

        init {
            pintar()
        }

        fun focar(sim: Boolean, animar: Boolean = true) {
            val novo = if (sim) 1f else 0f
            if (novo == alvo && animar) return
            alvo = novo
            anim?.cancel()
            if (!animar) {
                cor = novo
                mov = novo
                pintar()
                return
            }
            corDe = cor
            movDe = mov
            anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = MOV_MS
                interpolator = LinearInterpolator()
                addUpdateListener {
                    val f = it.animatedFraction
                    cor = lerp(corDe, alvo, Estilo.EASE.getInterpolation(minOf(1f, f * MOV_MS / COR_MS)))
                    mov = lerp(movDe, alvo, Estilo.MOLA.getInterpolation(f))
                    pintar()
                }
                start()
            }
        }

        private fun pintar() {
            val e = lerp(1f, 1.02f, mov)
            scaleX = e
            scaleY = e
            fundo.color = lerpCor(Estilo.TEXTO and 0xFFFFFF, Estilo.TEXTO, cor)
            tinta.color = lerpCor(corTexto, corTextoFoco, cor)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (cor > 0f) canvas.drawRoundRect(0f, 0f, w, h, raio, raio, fundo)
            canvas.drawBitmap(icone, margem, (h - ladoIcone) / 2f, tinta)
            canvas.drawText(texto, margem + ladoIcone + vao, h / 2f + meioTexto, tinta)
        }

        private companion object {
            const val MOV_MS = 300L
            const val COR_MS = 250L

            /** `#d4d4d8`, o texto das opções sem foco. */
            const val NORMAL = 0xFFD4D4D8.toInt()
        }
    }

    companion object {
        private const val LARGURA_DP = 310f
        private const val FUNDO_MS = 350L
        private const val OPACIDADE_MS = 300L
        private const val CAIXA_MS = 450L

        /** `rgba(0,0,0,.55)`. */
        private const val ESCURO = 0x8C000000.toInt()

        /** `rgba(255,255,255,.08)`. */
        private const val LINHA = 0x14FFFFFF

        /** A capa no tamanho do modal (80x45 dp, raio 6 dp; sem banner, ícone de 15 dp e nome de 8 dp). */
        fun capa(ctx: Context, pronta: CapaPronta?, nome: String): Drawable =
            CapaDrawable(ctx, pronta, nome, ctx.dp(6f), ctx.dp(15f), ctx.dp(8f), ctx.dp(2f))
    }
}
