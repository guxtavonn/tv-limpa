package com.guxtavon.tvlimpa

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.guxtavon.tvlimpa.nucleo.AppInfo
import com.guxtavon.tvlimpa.nucleo.Fileira
import com.guxtavon.tvlimpa.nucleo.Gesto
import com.guxtavon.tvlimpa.nucleo.Padrao
import com.guxtavon.tvlimpa.nucleo.Tecla
import com.guxtavon.tvlimpa.nucleo.Toque
import com.guxtavon.tvlimpa.nucleo.listaGrade
import com.guxtavon.tvlimpa.nucleo.teclaDe
import com.guxtavon.tvlimpa.sistema.Faxina
import com.guxtavon.tvlimpa.sistema.Guardado
import com.guxtavon.tvlimpa.sistema.Instalados
import com.guxtavon.tvlimpa.sistema.Lancador
import com.guxtavon.tvlimpa.tela.Abrindo
import com.guxtavon.tvlimpa.tela.Alvo
import com.guxtavon.tvlimpa.tela.Capas
import com.guxtavon.tvlimpa.tela.Desfoque
import com.guxtavon.tvlimpa.tela.GradeApps
import com.guxtavon.tvlimpa.tela.ModalOrganizar
import com.guxtavon.tvlimpa.tela.Opcao
import com.guxtavon.tvlimpa.tela.TelaInicial
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A tela inicial. Camadas, de baixo pra cima: a [TelaInicial] e a [GradeApps] (dentro do `palco`,
 * que é o que o modal desfoca), o [ModalOrganizar] e o [Abrindo]. O foco é de cada camada; aqui
 * chegam as teclas (mandadas pra camada de cima que estiver aberta) e o ciclo de vida.
 */
class MainActivity : Activity() {
    private lateinit var palco: FrameLayout
    private lateinit var tela: TelaInicial
    private lateinit var grade: GradeApps
    private lateinit var modal: ModalOrganizar
    private lateinit var abrindo: Abrindo
    private lateinit var lancador: Lancador
    private lateinit var instalados: Instalados
    private lateinit var guardado: Guardado
    private val fundo: ExecutorService = Executors.newSingleThreadExecutor()
    private val principal = Handler(Looper.getMainLooper())
    @Volatile
    private var pedido = 0

    /** Todos os apps instalados, da última leitura. */
    private var todos: List<AppInfo> = emptyList()

    /** A fileira como está na tela (já reconciliada com o que está instalado). */
    private var fileira: List<AppInfo> = emptyList()

    /** A configuração da última vez (pra saber o que mudou em [onConfigurationChanged]). */
    private lateinit var config: Configuration

    /** A apertada do OK, decidida pelo tempo (curta abre ao soltar; 600 ms segurando organiza). */
    private val toque = Toque()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = Configuration(resources.configuration)
        lancador = Lancador(this)
        instalados = Instalados(applicationContext)
        guardado = Guardado(applicationContext)
        tela = TelaInicial(this, aoOk = ::abrir, aoSegurar = ::organizarFileira)
        grade = GradeApps(this, aoOk = ::abrirApp, aoSegurar = ::organizarGrade)
        grade.aoFocar = { tela.brilhar(it) }
        modal = ModalOrganizar(this)
        abrindo = Abrindo(this)
        palco = FrameLayout(this).apply {
            clipChildren = false
            addView(tela, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(grade, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        val raiz = FrameLayout(this).apply {
            addView(palco, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(modal, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(abrindo, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        setContentView(raiz)
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    override fun onPause() {
        // O app abriu: a tela de abrindo sai (a de erro também).
        abrindo.esconder()
        soltarOk()
        super.onPause()
    }

    override fun onStop() {
        // Ao voltar pra cá, nada de tela preta por cima.
        abrindo.sumir()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // O Home: fecha o que estiver aberto e volta pro primeiro app.
        soltarOk()
        modal.sumir()
        abrindo.sumir()
        if (grade.aberta) grade.fecharJa()
        tela.cobrir(false, animar = false)
        tela.focarPrimeiro()
    }

    /**
     * Controle que conecta ou cai, idioma, modo noturno, tamanho de letra, resolução: a tela não é
     * recriada (estão no `configChanges` do manifest). O desenho não depende deles (cores fixas,
     * textos em dp e em português); o idioma muda o nome dos apps, então relê a lista. O resto só
     * pede um layout novo.
     */
    override fun onConfigurationChanged(novo: Configuration) {
        super.onConfigurationChanged(novo)
        val mudou = config.diff(novo)
        config = Configuration(novo)
        if (mudou and ActivityInfo.CONFIG_LOCALE != 0) carregar()
        window.decorView.requestLayout()
    }

    override fun onDestroy() {
        fundo.shutdownNow()
        super.onDestroy()
    }

    /**
     * Lê os apps e a fileira fora da thread principal (rótulos, banners, ícones e cores pesam na
     * TV). Primeiro a fileira, com as capas dela; depois, já com a tela pronta, as capas da grade.
     */
    private fun carregar() {
        val meu = ++pedido
        fundo.execute {
            val lista = instalados.apps()
            val porPacote = lista.associateBy { it.pacote }
            Capas.manter(porPacote.keys)
            val reconciliada = guardado.fileira.reconciliar(porPacote.keys)
            val naFileira: List<AppInfo> = reconciliada.pacotes.mapNotNull { porPacote[it] }
            for (app in naFileira) Capas.preparar(applicationContext, app)
            principal.post {
                if (meu != pedido || isDestroyed) return@post
                todos = lista
                fileira = naFileira
                tela.mostrar(naFileira)
                if (grade.aberta) grade.mostrar(listaGrade(lista, Padrao.ESCONDIDOS), pacotesDaFileira())
            }
            Faxina.limpar(applicationContext)
            // As capas da grade ficam prontas antes de alguém abrir "Apps" (na volta, já estão guardadas).
            for (app in listaGrade(lista, Padrao.ESCONDIDOS)) {
                if (meu != pedido) return@execute
                Capas.preparar(applicationContext, app)
            }
            principal.post {
                if (isDestroyed) return@post
                tela.atualizarCapas()
                grade.atualizarCapas()
            }
        }
    }

    // --- teclas ---

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val t = teclaDe(keyCode) ?: return super.onKeyDown(keyCode, event)
        if (abrindo.visivel) {
            // Enquanto abre (ou avisa que não abriu), só o voltar faz algo: some com a tela.
            if (t == Tecla.VOLTAR && event.repeatCount == 0) abrindo.esconder()
            return true
        }
        when (t) {
            Tecla.OK -> {
                if (event.repeatCount == 0) {
                    when {
                        modal.aberto -> Unit
                        grade.aberta -> grade.apertar(true)
                        else -> tela.apertar(true)
                    }
                }
                if (toque.descer(event.repeatCount, event.eventTime) == Gesto.SEGURAR) segurar()
            }
            Tecla.VOLTAR -> if (event.repeatCount == 0) {
                when {
                    modal.aberto -> modal.fechar()
                    grade.aberta -> fecharGrade()
                    else -> Unit // na tela inicial o voltar não faz nada
                }
            }
            else -> when {
                modal.aberto -> modal.tecla(t)
                grade.aberta -> grade.tecla(t)
                else -> tela.tecla(t)
            }
        }
        return true
    }

    /**
     * O OK chegou aos 600 ms apertado. Só a camada de cima responde: o modal não tem segurar, e nos
     * atalhos da tela inicial não há o que organizar; nesses casos o soltar ainda abre ([Toque]).
     */
    private fun segurar() {
        val fez = when {
            // segurar dentro do modal não faz nada (nem ao soltar): evita "Tirar" sem querer
            modal.aberto -> true
            grade.aberta -> grade.segurar()
            else -> tela.segurar()
        }
        if (fez) toque.segurou()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val t = teclaDe(keyCode) ?: return super.onKeyUp(keyCode, event)
        if (t == Tecla.OK) {
            val curto = toque.subir(event.isCanceled) == Gesto.OK
            soltarOk()
            if (curto && !abrindo.visivel) {
                when {
                    modal.aberto -> modal.tecla(Tecla.OK)
                    grade.aberta -> grade.tecla(Tecla.OK)
                    else -> tela.tecla(Tecla.OK)
                }
            }
        }
        return true
    }

    private fun soltarOk() {
        tela.apertar(false)
        grade.apertar(false)
        toque.soltar()
    }

    @Deprecated("Activity.onBackPressed", ReplaceWith(""))
    override fun onBackPressed() {
        // Tela inicial: o voltar não sai daqui (o voltar das camadas é tratado no onKeyDown).
    }

    // --- abrir ---

    private fun abrir(alvo: Alvo) {
        when (alvo) {
            is Alvo.App -> abrirApp(alvo.app)
            Alvo.Apps -> abrirGrade()
            Alvo.Tv -> if (!lancador.abrirTv()) abrindo.erro("TV aberta")
            Alvo.Hdmi -> if (!lancador.abrirHdmi()) abrindo.erro("HDMI")
            Alvo.Wifi -> if (!lancador.abrirWifi()) abrindo.erro("Wi-Fi")
            Alvo.Configuracoes -> if (!lancador.abrirConfiguracoes()) abrindo.erro("Configurações")
        }
    }

    private fun abrirApp(app: AppInfo) {
        abrindo.mostrar(app.nome, Abrindo.capa(this, Capas.pronta(app.pacote), app.nome))
        if (!lancador.abrir(app)) {
            abrindo.erro(app.nome)
            // O app sumiu ou foi desligado: relê a lista, e a reconciliação tira ele da fileira.
            carregar()
        }
    }

    private fun abrirGrade() {
        // Primeiro cobre a tela: com ela coberta, o brilho passa a seguir o foco da grade.
        tela.cobrir(true)
        grade.mostrar(listaGrade(todos, Padrao.ESCONDIDOS), pacotesDaFileira())
        grade.abrir()
    }

    private fun fecharGrade() {
        grade.fechar()
        tela.cobrir(false)
        tela.focarApps()
    }

    // --- organizar ---

    /** Segurou OK no app [i] da fileira mostrada (a reconciliada, não a guardada). */
    private fun organizarFileira(i: Int) {
        val app = fileira.getOrNull(i) ?: return
        val n = fileira.size
        val opcoes = buildList {
            if (i > 0) add(Opcao("Mover pra esquerda", R.drawable.ic_esq, false) { mudar(app.pacote, app.pacote) { f, j -> f.moverEsquerda(j) } })
            if (i < n - 1) add(Opcao("Mover pra direita", R.drawable.ic_dir, false) { mudar(app.pacote, app.pacote) { f, j -> f.moverDireita(j) } })
            if (n > 1) add(Opcao("Tirar da tela inicial", R.drawable.ic_tira, true) { mudar(app.pacote, null) { f, j -> f.tirar(j) } })
            add(Opcao("Cancelar", R.drawable.ic_x, false) {})
        }
        abrirModal(app, opcoes)
    }

    /** Segurou OK num app da grade. */
    private fun organizarGrade(app: AppInfo) {
        val naFileira = fileira.any { it.pacote == app.pacote }
        val opcoes = buildList {
            if (!naFileira) {
                add(Opcao("Pôr na tela inicial", R.drawable.ic_poe, false) { salvar(atual().por(app.pacote), null) })
            } else if (fileira.size > 1) {
                add(Opcao("Tirar da tela inicial", R.drawable.ic_tira, true) { mudar(app.pacote, null) { f, j -> f.tirar(j) } })
            }
            add(Opcao("Cancelar", R.drawable.ic_x, false) {})
        }
        abrirModal(app, opcoes)
    }

    private fun abrirModal(app: AppInfo, opcoes: List<Opcao>) {
        soltarOk()
        // O modal ainda está fora do palco: o desfoque pega só a tela e a grade.
        val fundoDesfocado = Desfoque.de(palco)
        modal.abrir(app, ModalOrganizar.capa(this, Capas.pronta(app.pacote), app.nome), opcoes, fundoDesfocado)
    }

    /** A fileira mostrada agora, como [Fileira]. */
    private fun atual(): Fileira = Fileira(fileira.map { it.pacote })

    /**
     * Aplica [muda] no índice ATUAL de [pacote] na fileira mostrada (se a lista mudou desde que o
     * modal abriu, o índice antigo não vale mais). Pacote que já não está lá: não faz nada.
     */
    private fun mudar(pacote: String, foco: String?, muda: (Fileira, Int) -> Fileira) {
        val f = atual()
        val j = f.pacotes.indexOf(pacote)
        if (j < 0) return
        salvar(muda(f, j), foco)
    }

    /** Guarda a fileira nova e redesenha a tela e, se aberta, a grade. */
    private fun salvar(nova: Fileira, foco: String?) {
        if (nova == atual()) return
        guardado.fileira = nova
        val porPacote = todos.associateBy { it.pacote }
        fileira = nova.pacotes.mapNotNull { porPacote[it] }
        tela.mostrar(fileira, foco)
        if (grade.aberta) grade.mostrar(listaGrade(todos, Padrao.ESCONDIDOS), pacotesDaFileira())
    }

    private fun pacotesDaFileira(): Set<String> = fileira.mapTo(HashSet()) { it.pacote }
}
