package com.example

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.example.dvizhok_menu.bityy_piksel_stil
import com.example.dvizhok_menu.edinoe_nazhatie
import com.example.dvizhok_menu.fon_s_podsvetkoy
import com.example.dvizhok_menu.plavnaya_podsvetka_knopki
import com.example.dvizhok_menu.potertost_polosy
import com.example.dvizhok_menu.redkost_predmeta
import com.example.ui.theme.Typography

// экран каркаса инвентаря, магазина и рынка игроков
class inventar_menu : ComponentActivity() {
  private data class karkas_yacheyki(
    val id: Int,
    @StringRes val nazvanie_id: Int,
    val redkost: redkost_predmeta,
    val tip: Int,
    val ekipirovan: Boolean,
    val tsena: Int,
  )

  private data class tovar_magazina(
    @StringRes val nazvanie_id: Int,
    @StringRes val valyuta_id: Int,
    @StringRes val tsena_id: Int,
    val tsvet_valyuty: Color,
    val tsvet_polosy: Color,
  )

  private val bazovye_yacheyki = listOf(
    redkost_predmeta.SEKRETNOE,
    redkost_predmeta.MIFICHESKOE,
    redkost_predmeta.MIFICHESKOE,
    redkost_predmeta.LEGENDARNOE,
    redkost_predmeta.LEGENDARNOE,
    redkost_predmeta.LEGENDARNOE,
    redkost_predmeta.EPICHESKOE,
    redkost_predmeta.EPICHESKOE,
    redkost_predmeta.EPICHESKOE,
    redkost_predmeta.EPICHESKOE,
    redkost_predmeta.REDKOE,
    redkost_predmeta.REDKOE,
    redkost_predmeta.REDKOE,
    redkost_predmeta.REDKOE,
    redkost_predmeta.REDKOE,
    redkost_predmeta.OBYCHNOE,
    redkost_predmeta.OBYCHNOE,
    redkost_predmeta.OBYCHNOE,
    redkost_predmeta.OBYCHNOE,
    redkost_predmeta.OBYCHNOE,
  ).mapIndexed { indeks, redkost ->
    val tip_predmeta = if (indeks % 4 == 3) 1 else 0
    karkas_yacheyki(
      id = indeks,
      nazvanie_id = if (tip_predmeta == 1) R.string.yacheyka_graffiti else R.string.yacheyka_predmet,
      redkost = redkost,
      tip = tip_predmeta,
      ekipirovan = indeks in listOf(0, 1, 3, 6),
      tsena = redkost.rang * 100 + (20 - indeks),
    )
  }

  private val tovary_konteynerov = listOf(
    tovar_magazina(R.string.magazin_keys, R.string.valyuta_zoloto, R.string.kartochka_tsena, dvizhok_menu.tsvet_zolotoy, dvizhok_menu.tsvet_knopki_aktivnoy),
    tovar_magazina(R.string.magazin_keys, R.string.valyuta_zoloto, R.string.kartochka_tsena, dvizhok_menu.tsvet_zolotoy, dvizhok_menu.tsvet_knopki_aktivnoy),
    tovar_magazina(R.string.magazin_keys, R.string.valyuta_zoloto, R.string.kartochka_tsena, dvizhok_menu.tsvet_zolotoy, dvizhok_menu.tsvet_knopki_aktivnoy),
    tovar_magazina(R.string.magazin_boks, R.string.valyuta_serebro, R.string.kartochka_tsena_serebro, dvizhok_menu.tsvet_serebra, dvizhok_menu.tsvet_knopki),
    tovar_magazina(R.string.magazin_boks, R.string.valyuta_serebro, R.string.kartochka_tsena_serebro, dvizhok_menu.tsvet_serebra, dvizhok_menu.tsvet_knopki),
    tovar_magazina(R.string.magazin_boks, R.string.valyuta_serebro, R.string.kartochka_tsena_serebro, dvizhok_menu.tsvet_serebra, dvizhok_menu.tsvet_knopki),
    tovar_magazina(R.string.magazin_graffiti_pak, R.string.valyuta_zoloto, R.string.kartochka_tsena, dvizhok_menu.tsvet_zolotoy, dvizhok_menu.tsvet_knopki_bolotniy),
  )

  private val tovary_donata = List(5) {
    tovar_magazina(R.string.kartochka_nabor, R.string.valyuta_zoloto, R.string.kartochka_tsena, dvizhok_menu.tsvet_zolotoy, dvizhok_menu.tsvet_knopki_aktivnoy)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    dvizhok_menu.bez_perehoda(this, zakrytie = false)
    dvizhok_menu.bez_perehoda(this, zakrytie = true)
    dvizhok_menu.nastroit_okno(this)
    setContent {
      MaterialTheme(colorScheme = dvizhok_menu.tsvetovaya_shema, typography = Typography) {
        ekran_inventarya()
      }
    }
  }

  override fun finish() {
    super.finish()
    dvizhok_menu.bez_perehoda(this, zakrytie = true)
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) dvizhok_menu.skrit_paneli_sistemy(window)
  }

  // расстановка ячеек по выбранному критерию и направлению
  private fun otsortirovat_yacheyki(sortirovka: Int, po_vozrastaniyu: Boolean): List<karkas_yacheyki> {
    val spisok = when (sortirovka) {
      R.string.inv_sort_tip -> bazovye_yacheyki.sortedWith(compareBy({ it.tip }, { -it.redkost.rang }, { it.id }))
      R.string.inv_sort_ekipirovka -> bazovye_yacheyki.sortedWith(compareBy({ !it.ekipirovan }, { -it.redkost.rang }, { it.id }))
      R.string.rynok_sort_tsena -> bazovye_yacheyki.sortedWith(compareBy({ -it.tsena }, { it.id }))
      else -> bazovye_yacheyki.sortedWith(compareBy({ -it.redkost.rang }, { it.id }))
    }
    return if (po_vozrastaniyu) spisok.reversed() else spisok
  }

  // основной каркас экрана: постоянная верхняя навигация и мгновенное переключение разделов
  @Composable
  private fun ekran_inventarya() {
    var tekushaya_vkladka by remember { mutableIntStateOf(R.string.menu_inventar) }
    var podvkladka_rynka by remember { mutableIntStateOf(R.string.vkladka_rynok) }
    var vybrannaya_yacheyka by remember { mutableIntStateOf(-1) }

    BackHandler(enabled = tekushaya_vkladka == R.string.vkladka_rynok && podvkladka_rynka != R.string.vkladka_rynok) {
      podvkladka_rynka = R.string.vkladka_rynok
    }

    dvizhok_menu.konteyner_1k1(
      modifikator = Modifier.testTag("inventar_ekran"),
      fon_id = R.drawable.fon02,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(dvizhok_menu.tsvet_vinyetki_inventarya)
          .padding(horizontal = dvizhok_menu.otstup_kraya, vertical = dvizhok_menu.otstup_malyy),
      ) {
        verhnyaya_panel(
          aktivnaya_vkladka = tekushaya_vkladka,
          onNazad = { finish() },
          onVkladka = { vkladka ->
            if (vkladka == R.string.vkladka_rynok && tekushaya_vkladka == R.string.vkladka_rynok) {
              podvkladka_rynka = R.string.vkladka_rynok
            }
            tekushaya_vkladka = vkladka
          },
        )
        Spacer(Modifier.height(dvizhok_menu.otstup_malyy))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (tekushaya_vkladka) {
            R.string.menu_inventar -> karkas_inventarya(vybrannaya_yacheyka) { id ->
              vybrannaya_yacheyka = if (vybrannaya_yacheyka == id) -1 else id
            }
            R.string.vkladka_magazin -> karkas_magazina()
            R.string.vkladka_rynok -> karkas_rynka(podvkladka_rynka) { vybrannaya ->
              podvkladka_rynka =
                if (vybrannaya == R.string.vkladka_rynok || podvkladka_rynka == vybrannaya) R.string.vkladka_rynok
                else vybrannaya
            }
          }
        }
      }

      dvizhok_menu.sloy_polosok_sverhu()
    }
  }

  // верхняя полоса: постоянные 4 кнопки (назад | инвентарь | магазин | рынок) и баланс
  @Composable
  private fun verhnyaya_panel(
    aktivnaya_vkladka: Int,
    onNazad: () -> Unit,
    onVkladka: (Int) -> Unit,
  ) {
    val vkladki = listOf(R.string.menu_inventar, R.string.vkladka_magazin, R.string.vkladka_rynok)
    Row(
      modifier = Modifier.fillMaxWidth().height(dvizhok_menu.vysota_shapki_razdela),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.fillMaxHeight().padding(vertical = dvizhok_menu.v_dp(1f)),
        horizontalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        knopka_vkladki(
          stroka_id = R.string.knopka_nazad,
          aktivno = false,
          fon_bazy = dvizhok_menu.tsvet_knopki_temney,
          tsvet_nadpisi = dvizhok_menu.tsvet_teksta,
          variant = 0,
          tag = "knopka_nazad",
          onNazhatie = onNazad,
        )
        vkladki.forEachIndexed { indeks, stroka_id ->
          val aktivno = aktivnaya_vkladka == stroka_id
          knopka_vkladki(
            stroka_id = stroka_id,
            aktivno = aktivno,
            fon_bazy = dvizhok_menu.tsvet_knopki,
            tsvet_nadpisi = if (aktivno) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta_tuskliy,
            variant = indeks + 1,
            tag = "vkladka_inv_$indeks",
            onNazhatie = { onVkladka(stroka_id) },
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        dvizhok_menu.schetchik_valyuty(R.string.valyuta_zoloto, dvizhok_menu.tsvet_zolotoy, dvizhok_menu.format_zolota(123.45))
        Spacer(Modifier.width(dvizhok_menu.otstup_osnovnoy))
        dvizhok_menu.schetchik_valyuty(R.string.valyuta_serebro, dvizhok_menu.tsvet_serebra, dvizhok_menu.format_serebra(1500L))
      }
    }
  }

  // единая кнопка навигации с мгновенной подсветкой текста и белой рамки (без жирного текста)
  @Composable
  private fun knopka_vkladki(
    @StringRes stroka_id: Int,
    aktivno: Boolean,
    fon_bazy: Color,
    tsvet_nadpisi: Color,
    variant: Int,
    tag: String,
    onNazhatie: () -> Unit,
  ) {
    Box(
      modifier = Modifier
        .width(dvizhok_menu.shirina_vkladki_inventarya)
        .fillMaxHeight()
        .edinoe_nazhatie(onNazhatie)
        .testTag(tag),
      contentAlignment = Alignment.Center,
    ) {
      fon_s_podsvetkoy(fon_bazy = fon_bazy, aktivno = aktivno, variant = variant)
      Text(
        text = stringResource(stroka_id),
        color = tsvet_nadpisi,
        fontSize = dvizhok_menu.shrift_osnovnoy,
        fontFamily = dvizhok_menu.shrift_igry,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = dvizhok_menu.otstup_malyy),
      )
    }
  }

  // каркас инвентаря: полоса фильтра с поиском и сетка 5 в ряд
  @Composable
  private fun karkas_inventarya(vybrannaya: Int, onVybor: (Int) -> Unit) {
    razdel_s_filtrom_i_setkoy(
      opcii_sortirovki = listOf(R.string.rynok_sort_redkost, R.string.inv_sort_tip, R.string.inv_sort_ekipirovka),
      vybrannaya = vybrannaya,
      s_tsenoy = false,
      tag_prefiks = "inv",
      rezhim_rynka = R.string.menu_inventar,
      onVybor = onVybor,
    )
  }

  // каркас магазина: контейнеры | донат и сетка 5 в ряд без задержки и без растяжения
  @Composable
  private fun karkas_magazina() {
    var podvkladka by remember { mutableIntStateOf(R.string.magazin_konteynery) }
    val podvkladki = listOf(R.string.magazin_konteynery, R.string.magazin_donat)
    val sostoyanie_skrolla = rememberScrollState()
    val tovary = if (podvkladka == R.string.magazin_konteynery) tovary_konteynerov else tovary_donata
    val stroki_tovarov = remember(tovary) { tovary.chunked(5) }
    val otstup_setki = dvizhok_menu.otstup_malyy

    LaunchedEffect(podvkladka) {
      if (sostoyanie_skrolla.value != 0) sostoyanie_skrolla.scrollTo(0)
    }

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(otstup_setki),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().height(dvizhok_menu.vysota_podvkladki_rynka),
        horizontalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_osnovnoy),
      ) {
        podvkladki.forEachIndexed { indeks, stroka_id ->
          val aktivno = podvkladka == stroka_id
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .edinoe_nazhatie { podvkladka = stroka_id }
              .testTag("podvkladka_magazin_$indeks"),
            contentAlignment = Alignment.Center,
          ) {
            fon_s_podsvetkoy(fon_bazy = dvizhok_menu.tsvet_knopki_temney, aktivno = aktivno, variant = indeks)
            Text(
              text = stringResource(stroka_id),
              color = if (aktivno) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta_tuskliy,
              fontSize = dvizhok_menu.shrift_osnovnoy,
              fontFamily = dvizhok_menu.shrift_igry,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .bityy_piksel_stil(dvizhok_menu.tsvet_paneli, dvizhok_menu.tsvet_ramki, variant = 1)
          .padding(otstup_setki)
          .clipToBounds()
          .verticalScroll(sostoyanie_skrolla),
        verticalArrangement = Arrangement.spacedBy(otstup_setki),
      ) {
        stroki_tovarov.forEachIndexed { nomer_stroki, stroka ->
          Row(
            modifier = Modifier.fillMaxWidth().graphicsLayer(),
            horizontalArrangement = Arrangement.spacedBy(otstup_setki),
          ) {
            for (kolonka in 0 until 5) {
              val indeks = nomer_stroki * 5 + kolonka
              val tovar = stroka.getOrNull(kolonka)
              if (tovar != null) {
                kartochka_magazina(
                  indeks = indeks,
                  tovar = tovar,
                  modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.42f)
                    .testTag("tovar_mag_$indeks"),
                )
              } else {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }

  // каркас рынка: поиск по названию между "цене" и "фильтр", фильтр и две кнопки справа
  @Composable
  private fun karkas_rynka(podvkladka: Int, onPodvkladka: (Int) -> Unit) {
    val dop_knopki = listOf(R.string.rynok_moi_zaprosy, R.string.rynok_istoriya)
    razdel_s_filtrom_i_setkoy(
      opcii_sortirovki = listOf(R.string.rynok_sort_redkost, R.string.rynok_sort_tsena),
      vybrannaya = -1,
      s_tsenoy = true,
      tag_prefiks = "rynok",
      rezhim_rynka = podvkladka,
      onPoiskAktiven = { onPodvkladka(R.string.vkladka_rynok) },
      pravaya_chast = {
        dop_knopki.forEachIndexed { indeks, stroka_id ->
          dvizhok_menu.razdelitel_vkladok()
          val aktivno = podvkladka == stroka_id
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .edinoe_nazhatie { onPodvkladka(stroka_id) }
              .padding(horizontal = dvizhok_menu.otstup_malyy)
              .testTag("rynok_dop_$indeks"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = stringResource(stroka_id),
              color = if (aktivno) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta_tuskliy,
              fontSize = dvizhok_menu.shrift_osnovnoy,
              fontFamily = dvizhok_menu.shrift_igry,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      },
    )
  }

  private fun podvkladki_dlya_rezhima(rezhim: Int): List<Int> = when (rezhim) {
    R.string.rynok_moi_zaprosy -> listOf(R.string.rynok_zaprosy_prodazha, R.string.rynok_zaprosy_pokupka)
    R.string.rynok_istoriya -> listOf(R.string.rynok_istoriya_prodazh, R.string.rynok_istoriya_pokupok)
    else -> emptyList()
  }

  // общая компоновка для инвентаря и рынка с аппаратно кэшированными строками скролла
  @Composable
  private fun razdel_s_filtrom_i_setkoy(
    opcii_sortirovki: List<Int>,
    vybrannaya: Int,
    s_tsenoy: Boolean,
    tag_prefiks: String,
    rezhim_rynka: Int,
    pravaya_chast: (@Composable RowScope.() -> Unit)? = null,
    onPoiskAktiven: (() -> Unit)? = null,
    onVybor: ((Int) -> Unit)? = null,
  ) {
    var sortirovka by remember(tag_prefiks) { mutableIntStateOf(opcii_sortirovki.first()) }
    var po_vozrastaniyu by remember(tag_prefiks) { mutableStateOf(false) }
    var filtr_aktiven by remember(tag_prefiks) { mutableStateOf(false) }
    var tekst_poiska by remember(tag_prefiks) { mutableStateOf("") }
    val podvkladki_bokovoy = remember(rezhim_rynka) { podvkladki_dlya_rezhima(rezhim_rynka) }
    val est_bokovaya = podvkladki_bokovoy.isNotEmpty()
    var podrazdel_rynka by remember(rezhim_rynka) {
      mutableIntStateOf(podvkladki_bokovoy.firstOrNull() ?: 0)
    }
    val resursy = LocalContext.current.resources
    val bazovyy_spisok = remember(sortirovka, po_vozrastaniyu, tekst_poiska) {
      val otsortirovannye = otsortirovat_yacheyki(sortirovka, po_vozrastaniyu)
      val zapros = tekst_poiska.trim()
      if (zapros.isEmpty()) otsortirovannye
      else otsortirovannye.filter { resursy.getString(it.nazvanie_id).contains(zapros, ignoreCase = true) }
    }
    val yacheyki = remember(bazovyy_spisok, est_bokovaya, podrazdel_rynka) {
      if (!est_bokovaya) bazovyy_spisok else when (podrazdel_rynka) {
        R.string.rynok_zaprosy_pokupka, R.string.rynok_istoriya_pokupok -> bazovyy_spisok.filter { it.id % 2 == 1 }
        else -> bazovyy_spisok.filter { it.id % 2 == 0 }
      }
    }
    val kolvo_kolonok = if (est_bokovaya) 4 else 5
    val stroki_yacheek = remember(yacheyki, kolvo_kolonok) { yacheyki.chunked(kolvo_kolonok) }
    val sostoyanie_skrolla = rememberScrollState()

    LaunchedEffect(sortirovka, po_vozrastaniyu, tekst_poiska, rezhim_rynka, podrazdel_rynka) {
      if (sostoyanie_skrolla.value != 0) {
        sostoyanie_skrolla.scrollTo(0)
      }
    }

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(dvizhok_menu.vysota_podvkladki_rynka)
          .bityy_piksel_stil(dvizhok_menu.tsvet_paneli, dvizhok_menu.tsvet_ramki, variant = 3)
          .padding(horizontal = dvizhok_menu.otstup_osnovnoy),
        horizontalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.rynok_sortirovat),
          color = dvizhok_menu.tsvet_teksta_tuskliy,
          fontSize = dvizhok_menu.shrift_malyy,
          fontFamily = dvizhok_menu.shrift_igry,
          maxLines = 1,
          softWrap = false,
        )
        opcii_sortirovki.forEachIndexed { indeks, stroka_id ->
          dvizhok_menu.razdelitel_vkladok()
          val vybrano = sortirovka == stroka_id
          punkt_sortirovki(
            stroka_id = stroka_id,
            vybrano = vybrano,
            po_vozrastaniyu = po_vozrastaniyu,
            tag = "${tag_prefiks}_sort_$indeks",
            onNazhatie = {
              if (vybrano) po_vozrastaniyu = !po_vozrastaniyu
              else {
                sortirovka = stroka_id
                po_vozrastaniyu = false
              }
            },
          )
        }
        dvizhok_menu.razdelitel_vkladok()
        pole_poiska_predmetov(
          znachenie = tekst_poiska,
          tag_prefiks = tag_prefiks,
          modifier = Modifier.weight(1f),
          onIzmenenie = { novyy ->
            tekst_poiska = novyy
            if (novyy.isNotEmpty()) onPoiskAktiven?.invoke()
          },
          onGotovo = { dvizhok_menu.skrit_paneli_sistemy(window) },
        )
        dvizhok_menu.razdelitel_vkladok()
        punkt_filtra(
          filtr_aktiven = filtr_aktiven,
          tag = "${tag_prefiks}_filtr",
          onNazhatie = { filtr_aktiven = !filtr_aktiven },
        )
        pravaya_chast?.invoke(this)
      }

      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .bityy_piksel_stil(dvizhok_menu.tsvet_paneli, dvizhok_menu.tsvet_ramki, variant = 0)
          .padding(dvizhok_menu.otstup_malyy),
      ) {
        val otstup_setki = dvizhok_menu.otstup_malyy
        val shirina_yacheyki = ((maxWidth - otstup_setki * 4) / 5f).coerceAtLeast(0.dp)
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.spacedBy(otstup_setki),
        ) {
          if (est_bokovaya) {
            bokovaya_panel_rynka(
              podvkladki = podvkladki_bokovoy,
              vybrannaya = podrazdel_rynka,
              shirina = shirina_yacheyki,
              onVybor = { podrazdel_rynka = it },
            )
          }

          Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (yacheyki.isEmpty()) {
              Text(
                text = stringResource(R.string.rynok_nichego_ne_naydeno),
                color = dvizhok_menu.tsvet_teksta_tuskliy,
                fontSize = dvizhok_menu.shrift_osnovnoy,
                fontFamily = dvizhok_menu.shrift_igry,
                modifier = Modifier.align(Alignment.Center),
              )
            } else {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .clipToBounds()
                  .verticalScroll(sostoyanie_skrolla),
                verticalArrangement = Arrangement.spacedBy(otstup_setki),
              ) {
                stroki_yacheek.forEachIndexed { nomer_stroki, stroka ->
                  Row(
                    modifier = Modifier.fillMaxWidth().graphicsLayer(),
                    horizontalArrangement = Arrangement.spacedBy(otstup_setki),
                  ) {
                    for (kolonka in 0 until kolvo_kolonok) {
                      val indeks = nomer_stroki * kolvo_kolonok + kolonka
                      val yacheyka = stroka.getOrNull(kolonka)
                      if (yacheyka != null) {
                        val vybrano = vybrannaya == yacheyka.id
                        val mod_klika = if (onVybor != null) Modifier.edinoe_nazhatie { onVybor(yacheyka.id) } else Modifier
                        karkasnaya_kartochka_slota(
                          yacheyka = yacheyka,
                          aktivno = vybrano,
                          s_tsenoy = s_tsenoy,
                          rezhim_rynka = rezhim_rynka,
                          modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.42f)
                            .then(mod_klika)
                            .testTag("yacheyka_${tag_prefiks}_$indeks"),
                        )
                      } else {
                        Spacer(modifier = Modifier.weight(1f))
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // безрамочное поле поиска с системным edittext для показа preview-строки и кнопки done в горизонтали
  @Composable
  private fun pole_poiska_predmetov(
    znachenie: String,
    tag_prefiks: String,
    modifier: Modifier = Modifier,
    onIzmenenie: (String) -> Unit,
    onGotovo: () -> Unit,
  ) {
    val kontekst = LocalContext.current
    val podskazka = stringResource(R.string.rynok_poisk_podskazka)
    val razmer_shrifta = dvizhok_menu.shrift_malyy.value
    var ssylka_polya by remember { mutableStateOf<EditText?>(null) }
    var v_fokuse by remember { mutableStateOf(false) }
    val tsvet_lupy = if (v_fokuse || znachenie.isNotEmpty()) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta_tuskliy

    Row(
      modifier = modifier
        .fillMaxHeight()
        .edinoe_nazhatie {
          ssylka_polya?.let { pole ->
            pole.requestFocus()
            val menedzher = kontekst.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            menedzher?.showSoftInput(pole, InputMethodManager.SHOW_IMPLICIT)
          }
        }
        .padding(horizontal = dvizhok_menu.otstup_malyy),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy),
    ) {
      Canvas(modifier = Modifier.size(dvizhok_menu.v_dp(9f))) {
        val radius_lupy = size.minDimension * 0.34f
        val centr = Offset(size.width * 0.4f, size.height * 0.4f)
        drawCircle(color = tsvet_lupy, radius = radius_lupy, center = centr, style = Stroke(width = 1.6f))
        drawLine(
          color = tsvet_lupy,
          start = Offset(centr.x + radius_lupy * 0.72f, centr.y + radius_lupy * 0.72f),
          end = Offset(size.width * 0.92f, size.height * 0.92f),
          strokeWidth = 1.6f,
        )
      }
      AndroidView(
        factory = { android_kontekst ->
          EditText(android_kontekst).apply {
            background = null
            minHeight = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            isSingleLine = true
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_DONE
            typeface = dvizhok_menu.kesh_shrifta_view ?: ResourcesCompat.getFont(android_kontekst, R.font.handjet)?.also {
              dvizhok_menu.kesh_shrifta_view = it
            }
            setTextColor(dvizhok_menu.tsvet_teksta.toArgb())
            setHintTextColor(dvizhok_menu.tsvet_teksta_tuskliy.toArgb())
            hint = podskazka
            letterSpacing = 0.03f
            setTextSize(TypedValue.COMPLEX_UNIT_SP, razmer_shrifta)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              textCursorDrawable = null
            }
            setOnFocusChangeListener { _, fokus_est -> v_fokuse = fokus_est }
            addTextChangedListener(object : TextWatcher {
              override fun beforeTextChanged(tekst: CharSequence?, nachalo: Int, kolvo: Int, posle: Int) {}
              override fun onTextChanged(tekst: CharSequence?, nachalo: Int, do_etogo: Int, kolvo: Int) {}
              override fun afterTextChanged(tekst: Editable?) {
                if (tag as? Boolean != true) {
                  onIzmenenie(tekst?.toString().orEmpty())
                }
              }
            })
            setOnEditorActionListener { pole, deystvie_id, _ ->
              if (deystvie_id == EditorInfo.IME_ACTION_DONE || deystvie_id == EditorInfo.IME_NULL) {
                pole.clearFocus()
                val menedzher = android_kontekst.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                menedzher?.hideSoftInputFromWindow(pole.windowToken, 0)
                onGotovo()
                true
              } else false
            }
            ssylka_polya = this
          }
        },
        update = { pole ->
          ssylka_polya = pole
          if (pole.hint != podskazka) pole.hint = podskazka
          if (pole.text.toString() != znachenie) {
            pole.tag = true
            pole.setText(znachenie)
            pole.setSelection(znachenie.length)
            pole.tag = false
          }
        },
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .testTag("${tag_prefiks}_poisk_vvod"),
      )
      if (znachenie.isNotEmpty()) {
        val opisanie_sbrosa = stringResource(R.string.opisanie_ochistit_poisk)
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .edinoe_nazhatie { onIzmenenie("") }
            .padding(horizontal = dvizhok_menu.v_dp(2f))
            .semantics { contentDescription = opisanie_sbrosa }
            .testTag("${tag_prefiks}_poisk_ochistit"),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.size(dvizhok_menu.v_dp(7f))) {
            drawLine(dvizhok_menu.tsvet_teksta_tuskliy, Offset.Zero, Offset(size.width, size.height), strokeWidth = 1.6f)
            drawLine(dvizhok_menu.tsvet_teksta_tuskliy, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 1.6f)
          }
        }
      }
    }
  }

  // плоский пункт сортировки внутри блока фильтра (только подсветка текста)
  @Composable
  private fun punkt_sortirovki(
    @StringRes stroka_id: Int,
    vybrano: Boolean,
    po_vozrastaniyu: Boolean,
    tag: String,
    onNazhatie: () -> Unit,
  ) {
    Row(
      modifier = Modifier
        .fillMaxHeight()
        .edinoe_nazhatie(onNazhatie)
        .padding(horizontal = dvizhok_menu.otstup_malyy)
        .testTag(tag),
      horizontalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(stroka_id),
        color = if (vybrano) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta_tuskliy,
        fontSize = dvizhok_menu.shrift_osnovnoy,
        fontFamily = dvizhok_menu.shrift_igry,
        maxLines = 1,
        softWrap = false,
      )
      treugolnik_sortirovki(
        vverh = vybrano && po_vozrastaniyu,
        tsvet = if (vybrano) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta_tuskliy.copy(alpha = 0.45f),
      )
    }
  }

  // плоский пункт фильтра внутри блока (только подсветка текста)
  @Composable
  private fun punkt_filtra(
    filtr_aktiven: Boolean,
    tag: String,
    onNazhatie: () -> Unit,
  ) {
    val tsvet = if (filtr_aktiven) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta_tuskliy
    Row(
      modifier = Modifier
        .fillMaxHeight()
        .edinoe_nazhatie(onNazhatie)
        .padding(horizontal = dvizhok_menu.otstup_malyy)
        .testTag(tag),
      horizontalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.rynok_filtr),
        color = tsvet,
        fontSize = dvizhok_menu.shrift_osnovnoy,
        fontFamily = dvizhok_menu.shrift_igry,
        maxLines = 1,
        softWrap = false,
      )
      Canvas(modifier = Modifier.size(dvizhok_menu.v_dp(10f))) {
        val razmer = size.minDimension
        val voronka = Path().apply {
          moveTo(0f, razmer * 0.1f); lineTo(razmer, razmer * 0.1f)
          lineTo(razmer * 0.62f, razmer * 0.55f); lineTo(razmer * 0.62f, razmer * 0.95f)
          lineTo(razmer * 0.38f, razmer * 0.85f); lineTo(razmer * 0.38f, razmer * 0.55f); close()
        }
        drawPath(voronka, tsvet)
      }
    }
  }

  // треугольник направления сортировки с плавным gpu-поворотом
  @Composable
  private fun treugolnik_sortirovki(vverh: Boolean, tsvet: Color) {
    val ugol_povorota = animateFloatAsState(
      targetValue = if (vverh) 180f else 0f,
      animationSpec = tween(durationMillis = dvizhok_menu.VREMYA_POVOROTA_MS, easing = FastOutSlowInEasing),
      label = "povorot_sortirovki",
    )
    Canvas(
      modifier = Modifier
        .size(dvizhok_menu.v_dp(8f))
        .graphicsLayer { rotationZ = ugol_povorota.value },
    ) {
      val shirina = size.width
      val vysota = size.height
      val treugolnik = Path().apply {
        moveTo(shirina * 0.1f, vysota * 0.18f)
        lineTo(shirina * 0.9f, vysota * 0.18f)
        lineTo(shirina * 0.5f, vysota * 0.82f)
        close()
      }
      drawPath(treugolnik, tsvet)
    }
  }

  // левая панель подразделов шириной ровно в первую колонку 5-колоночной сетки
  @Composable
  private fun bokovaya_panel_rynka(
    podvkladki: List<Int>,
    vybrannaya: Int,
    shirina: Dp,
    onVybor: (Int) -> Unit,
  ) {
    Column(
      modifier = Modifier
        .width(shirina)
        .fillMaxHeight()
        .bityy_piksel_stil(dvizhok_menu.tsvet_yacheyki, dvizhok_menu.tsvet_ramki, variant = 3)
        .padding(dvizhok_menu.otstup_malyy),
      verticalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy),
    ) {
      podvkladki.forEachIndexed { indeks, stroka_id ->
        val aktivno = vybrannaya == stroka_id
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(dvizhok_menu.vysota_podvkladki_rynka)
            .edinoe_nazhatie { onVybor(stroka_id) }
            .testTag("rynok_bok_${indeks}"),
          contentAlignment = Alignment.CenterStart,
        ) {
          plavnaya_podsvetka_knopki(aktivno = aktivno, variant = indeks)
          Text(
            text = stringResource(stroka_id),
            color = if (aktivno) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta_tuskliy,
            fontSize = dvizhok_menu.shrift_osnovnoy,
            fontFamily = dvizhok_menu.shrift_igry,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = dvizhok_menu.otstup_osnovnoy),
          )
        }
        if (indeks < podvkladki.lastIndex) {
          Box(Modifier.fillMaxWidth().height(dvizhok_menu.tolshina_ramki).background(dvizhok_menu.tsvet_ramki.copy(alpha = 0.6f)))
        }
      }
    }
  }

  // карточка товара в магазине с общей кистью градиента без лишних аллокаций
  @Composable
  private fun kartochka_magazina(
    indeks: Int,
    tovar: tovar_magazina,
    modifier: Modifier = Modifier,
  ) {
    val variant_uzora = indeks * 13 + 7
    Column(
      modifier = modifier
        .bityy_piksel_stil(dvizhok_menu.tsvet_yacheyki, dvizhok_menu.tsvet_ramki, variant = variant_uzora, s_skolami = true)
        .padding(dvizhok_menu.tolshina_ramki),
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .background(dvizhok_menu.kist_verha_slota),
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .potertost_polosy(tovar.tsvet_polosy, variant_uzora)
          .padding(horizontal = dvizhok_menu.otstup_malyy, vertical = dvizhok_menu.v_dp(2f)),
        contentAlignment = Alignment.CenterStart,
      ) {
        Text(
          text = stringResource(tovar.nazvanie_id),
          color = dvizhok_menu.tsvet_teksta,
          fontSize = dvizhok_menu.shrift_slota,
          fontWeight = FontWeight.Bold,
          fontFamily = dvizhok_menu.shrift_igry,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = dvizhok_menu.otstup_malyy, vertical = dvizhok_menu.v_dp(1.5f)),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(tovar.valyuta_id),
          color = tovar.tsvet_valyuty,
          fontSize = dvizhok_menu.shrift_slota,
          fontWeight = FontWeight.Bold,
          fontFamily = dvizhok_menu.shrift_igry,
          maxLines = 1,
          softWrap = false,
        )
        Spacer(Modifier.width(dvizhok_menu.v_dp(2f)))
        Text(
          text = stringResource(tovar.tsena_id),
          color = dvizhok_menu.tsvet_teksta,
          fontSize = dvizhok_menu.shrift_slota,
          fontFamily = dvizhok_menu.shrift_igry,
          maxLines = 1,
          softWrap = false,
        )
      }
    }
  }

  // карточка слота для инвентаря, рынка, запросов и истории сделок в один слой отрисовки
  @Composable
  private fun karkasnaya_kartochka_slota(
    yacheyka: karkas_yacheyki,
    aktivno: Boolean,
    s_tsenoy: Boolean,
    rezhim_rynka: Int,
    modifier: Modifier = Modifier,
  ) {
    val eto_istoriya = rezhim_rynka == R.string.rynok_istoriya
    val eto_zaprosy = rezhim_rynka == R.string.rynok_moi_zaprosy
    val levyy_tekst_id = when {
      eto_istoriya -> R.string.rynok_vremya_zaglushka
      eto_zaprosy -> R.string.yacheyka_odna_sht
      s_tsenoy -> R.string.kartochka_kolvo
      yacheyka.tip == 1 -> R.string.yacheyka_zaryady
      else -> R.string.yacheyka_odna_sht
    }
    val variant_uzora = yacheyka.id * 17 + 11

    Column(
      modifier = modifier
        .bityy_piksel_stil(
          fon = if (aktivno) dvizhok_menu.tsvet_knopki_aktivnoy else dvizhok_menu.tsvet_yacheyki,
          ramka = if (aktivno) dvizhok_menu.tsvet_ramki_aktivnoy else dvizhok_menu.tsvet_ramki,
          variant = variant_uzora,
          aktivno = aktivno,
          s_skolami = true,
        )
        .padding(dvizhok_menu.tolshina_ramki),
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .background(dvizhok_menu.kist_verha_slota),
      ) {
        if (eto_istoriya) {
          val imya_igroka = stringResource(R.string.rynok_igrok_zaglushka)
          Row(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(dvizhok_menu.v_dp(2f))
              .background(dvizhok_menu.tsvet_plashki_igroka)
              .padding(horizontal = dvizhok_menu.v_dp(4f), vertical = dvizhok_menu.v_dp(2.5f)),
            horizontalArrangement = Arrangement.spacedBy(dvizhok_menu.v_dp(4f)),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            dvizhok_menu.avatarka_igroka(
              imya_igroka = imya_igroka,
              razmer = dvizhok_menu.v_dp(13f),
              shrift = dvizhok_menu.v_sp(8.5f),
              variant = yacheyka.id,
              bitye_kraya = false,
            )
            Text(
              text = imya_igroka,
              color = dvizhok_menu.tsvet_teksta,
              fontSize = dvizhok_menu.shrift_malyy,
              fontFamily = dvizhok_menu.shrift_igry,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        if (!s_tsenoy && yacheyka.ekipirovan) {
          Text(
            text = stringResource(R.string.yacheyka_ekipirovano),
            color = dvizhok_menu.tsvet_teksta,
            fontSize = dvizhok_menu.shrift_slota,
            fontFamily = dvizhok_menu.shrift_igry,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(horizontal = dvizhok_menu.otstup_malyy, vertical = dvizhok_menu.v_dp(2f)),
          )
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .potertost_polosy(yacheyka.redkost.tsvet, variant_uzora)
          .padding(horizontal = dvizhok_menu.otstup_malyy, vertical = dvizhok_menu.v_dp(2f)),
        contentAlignment = Alignment.CenterStart,
      ) {
        Text(
          text = stringResource(yacheyka.nazvanie_id),
          color = dvizhok_menu.tsvet_teksta,
          fontSize = dvizhok_menu.shrift_slota,
          fontFamily = dvizhok_menu.shrift_igry,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = dvizhok_menu.otstup_malyy, vertical = dvizhok_menu.v_dp(1.5f)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(levyy_tekst_id),
          color = dvizhok_menu.tsvet_teksta_tuskliy,
          fontSize = dvizhok_menu.shrift_slota,
          fontFamily = dvizhok_menu.shrift_igry,
          maxLines = 1,
          softWrap = false,
        )
        if (s_tsenoy) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(dvizhok_menu.v_dp(2f)),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.valyuta_zoloto),
              color = dvizhok_menu.tsvet_zolotoy,
              fontSize = dvizhok_menu.shrift_slota,
              fontWeight = FontWeight.Bold,
              fontFamily = dvizhok_menu.shrift_igry,
              maxLines = 1,
              softWrap = false,
            )
            Text(
              text = stringResource(R.string.kartochka_tsena),
              color = dvizhok_menu.tsvet_teksta,
              fontSize = dvizhok_menu.shrift_slota,
              fontFamily = dvizhok_menu.shrift_igry,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }
  }
}
