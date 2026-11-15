package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.example.dvizhok_menu.bityy_piksel_stil
import com.example.dvizhok_menu.edinoe_nazhatie
import com.example.dvizhok_menu.fon_s_podsvetkoy
import com.example.dvizhok_menu.plavnaya_podsvetka_knopki
import com.example.ui.theme.Typography
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

// основной класс запуска и главного меню
class glavnoe_menu : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    dvizhok_menu.nastroit_okno(this)
    setContent {
      MaterialTheme(colorScheme = dvizhok_menu.tsvetovaya_shema, typography = Typography) {
        ekran_menu()
      }
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) dvizhok_menu.skrit_paneli_sistemy(window)
  }

  // главный экран лобби с битыми пиксельными кнопками и статичным интерфейсом
  @Composable
  fun ekran_menu(modifikator: Modifier = Modifier) {
    var aktivniy_razdel by remember { mutableIntStateOf(0) }

    dvizhok_menu.konteyner_1k1(
      modifikator = modifikator.testTag("glavnoe_menu_ekran"),
      fon_id = R.drawable.fon02,
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(dvizhok_menu.tsvet_vinyetki)
          .padding(horizontal = dvizhok_menu.otstup_kraya, vertical = dvizhok_menu.otstup_malyy)
      ) {
        verhnyaya_panel(modifier = Modifier.align(Alignment.TopCenter))

        levaya_kolonka(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = dvizhok_menu.vysota_verhney_paneli + dvizhok_menu.v_dp(18f)),
          aktivniy = aktivniy_razdel,
          onVybor = { razdel ->
            if (razdel == R.string.menu_inventar) {
              startActivity(Intent(this@glavnoe_menu, inventar_menu::class.java))
              dvizhok_menu.bez_perehoda(this@glavnoe_menu)
            } else {
              aktivniy_razdel = razdel
            }
          },
        )

        pravaya_kolonka(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = dvizhok_menu.vysota_verhney_paneli + dvizhok_menu.otstup_osnovnoy),
        )

        nizhnyaya_levaya_zona(
          modifier = Modifier.align(Alignment.BottomStart),
          aktivnaya_vkladka = aktivniy_razdel,
          onVyborVkladki = { aktivniy_razdel = it },
        )

        knopka_v_boy(modifier = Modifier.align(Alignment.BottomEnd))
      }

      // общий кэшированный слой мрачных полосок поверх всего интерфейса
      dvizhok_menu.sloy_polosok_sverhu()
    }
  }

  // верхняя полоса профиля, dcl/уведомлений и двух валют (g и s)
  @Composable
  private fun verhnyaya_panel(modifier: Modifier = Modifier) {
    Row(
      modifier = modifier.fillMaxWidth().height(dvizhok_menu.vysota_verhney_paneli),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        kartochka_profilya()
        Spacer(Modifier.width(dvizhok_menu.otstup_osnovnoy))
        knopka_vektor_ikonka(R.drawable.ic_cloud_download, R.string.opisanie_dcl, "dcl")
        Spacer(Modifier.width(dvizhok_menu.otstup_osnovnoy))
        knopka_vektor_ikonka(R.drawable.ic_notifications, R.string.opisanie_uvedomleniya, "uvedomleniya")
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        dvizhok_menu.schetchik_valyuty(
          metka_id = R.string.valyuta_zoloto,
          tsvet_metki = dvizhok_menu.tsvet_zolotoy,
          znachenie = dvizhok_menu.format_zolota(123.45),
        )
        Spacer(Modifier.width(dvizhok_menu.otstup_osnovnoy))
        dvizhok_menu.schetchik_valyuty(
          metka_id = R.string.valyuta_serebro,
          tsvet_metki = dvizhok_menu.tsvet_serebra,
          znachenie = dvizhok_menu.format_serebra(1500L),
        )
      }
    }
  }

  // карточка игрока в левом верхнем углу (стиль old-strike2d)
  @Composable
  private fun kartochka_profilya(
    imya_igroka: String = stringResource(R.string.profil_imya),
    id_igroka: String = stringResource(R.string.profil_id_net),
    onNazhatie: () -> Unit = {},
  ) {
    val stil_bez_otstupov = TextStyle(
      letterSpacing = dvizhok_menu.mezhbukvennyy_interval,
      platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    Row(
      modifier = Modifier
        .height(dvizhok_menu.vysota_verhney_paneli)
        .width(dvizhok_menu.shirina_profilya)
        .bityy_piksel_stil(dvizhok_menu.tsvet_paneli, dvizhok_menu.tsvet_ramki, variant = 0)
        .edinoe_nazhatie(onNazhatie)
        .padding(dvizhok_menu.v_dp(4f))
        .testTag("profil_kartochka"),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      dvizhok_menu.avatarka_igroka(
        imya_igroka = imya_igroka,
        razmer = dvizhok_menu.razmer_avatara,
        shrift = dvizhok_menu.shrift_avatara,
        variant = 2,
      )
      Spacer(Modifier.width(dvizhok_menu.v_dp(7f)))
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(dvizhok_menu.v_dp(2f), Alignment.CenterVertically),
      ) {
        Text(
          text = stringResource(R.string.profil_id_format, id_igroka),
          color = dvizhok_menu.tsvet_teksta_tuskliy,
          fontSize = dvizhok_menu.shrift_id,
          fontFamily = dvizhok_menu.shrift_igry,
          lineHeight = dvizhok_menu.shrift_id,
          style = stil_bez_otstupov,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = imya_igroka,
          color = dvizhok_menu.tsvet_teksta,
          fontSize = dvizhok_menu.shrift_nika,
          fontWeight = FontWeight.Bold,
          fontFamily = dvizhok_menu.shrift_igry,
          lineHeight = dvizhok_menu.shrift_nika,
          style = stil_bez_otstupov,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }

  // левая колонка: инвентарь, контакты, редактор карт и кнопки правила/лидеры
  @Composable
  private fun levaya_kolonka(modifier: Modifier = Modifier, aktivniy: Int, onVybor: (Int) -> Unit) {
    val fony = listOf(
      dvizhok_menu.tsvet_knopki,
      dvizhok_menu.tsvet_knopki_temney,
      dvizhok_menu.tsvet_knopki_bolotniy,
    )
    val punkty = listOf(
      Triple(R.string.menu_inventar, R.drawable.ic_menu_work, false),
      Triple(R.string.menu_kontakty, R.drawable.ic_menu_people, true),
      Triple(R.string.menu_redaktor_kart, R.drawable.ic_menu_map, false),
    )
    val nizhnie_knopki = listOf(
      Pair(R.string.menu_pravila, R.drawable.ic_description),
      Pair(R.string.menu_lidery, R.drawable.ic_leaderboard),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy)) {
      punkty.forEachIndexed { indeks, (stroka_id, ikonka_id, est_tochka) ->
        val vybrano = aktivniy == stroka_id
        val nazvanie = stringResource(stroka_id)
        Box(
          modifier = Modifier
            .width(dvizhok_menu.shirina_levoy_knopki)
            .height(dvizhok_menu.vysota_levoy_knopki)
            .edinoe_nazhatie { onVybor(stroka_id) }
            .testTag("knopka_$indeks"),
          contentAlignment = Alignment.CenterStart,
        ) {
          fon_s_podsvetkoy(fon_bazy = fony[indeks], aktivno = vybrano, variant = indeks)
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = dvizhok_menu.otstup_osnovnoy),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            vektor_ikonka(
              resurs_id = ikonka_id,
              opisanie = nazvanie,
              tsvet = dvizhok_menu.tsvet_teksta,
              razmer = dvizhok_menu.v_dp(14f),
            )
            Spacer(Modifier.width(dvizhok_menu.otstup_osnovnoy))
            Text(
              text = nazvanie,
              color = dvizhok_menu.tsvet_teksta,
              fontSize = dvizhok_menu.shrift_zagolovka,
              fontFamily = dvizhok_menu.shrift_igry,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            if (est_tochka) {
              Box(modifier = Modifier.size(dvizhok_menu.v_dp(5f)).background(dvizhok_menu.tsvet_trevogi))
            }
          }
        }
      }

      // ряд из двух кнопок под меню: правила и лидеры
      Row(
        modifier = Modifier.width(dvizhok_menu.shirina_levoy_knopki),
        horizontalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy),
      ) {
        nizhnie_knopki.forEachIndexed { indeks, (stroka_id, ikonka_id) ->
          val vybrano = aktivniy == stroka_id
          val podpis = stringResource(stroka_id)
          Box(
            modifier = Modifier
              .weight(1f)
              .height(dvizhok_menu.vysota_levoy_knopki)
              .edinoe_nazhatie { onVybor(stroka_id) }
              .testTag("knopka_niz_$indeks"),
            contentAlignment = Alignment.Center,
          ) {
            fon_s_podsvetkoy(fon_bazy = dvizhok_menu.tsvet_knopki_temney, aktivno = vybrano, variant = indeks + 3)
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dvizhok_menu.otstup_malyy),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              vektor_ikonka(
                resurs_id = ikonka_id,
                opisanie = podpis,
                tsvet = dvizhok_menu.tsvet_teksta,
                razmer = dvizhok_menu.v_dp(13f),
              )
              Spacer(Modifier.width(dvizhok_menu.otstup_malyy))
              Text(
                text = podpis,
                color = dvizhok_menu.tsvet_teksta,
                fontSize = dvizhok_menu.shrift_osnovnoy,
                fontFamily = dvizhok_menu.shrift_igry,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }

  // правая колонка с цикличным баннером и карточками событий
  @Composable
  private fun pravaya_kolonka(modifier: Modifier = Modifier) {
    Column(
      modifier = modifier.width(dvizhok_menu.shirina_pravoy_paneli),
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy),
    ) {
      banner_sobytiy()

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dvizhok_menu.otstup_malyy)) {
        kartochka_pravaya(R.string.kartochka_novosti, R.drawable.ic_newspaper, dvizhok_menu.tsvet_knopki_temney, 2, Modifier.weight(1f))
        kartochka_pravaya(R.string.kartochka_test_pass, R.drawable.ic_local_activity, dvizhok_menu.tsvet_knopki, 4, Modifier.weight(1f))
      }
    }
  }

  // цикличный баннер без лишних перекомпозиций: отрисовка оттенка и полосок на gpu
  @Composable
  private fun banner_sobytiy(onNazhatie: () -> Unit = {}) {
    val ottenki = remember {
      listOf(
        dvizhok_menu.tsvet_bannera_ottenok_1,
        dvizhok_menu.tsvet_bannera_ottenok_2,
        dvizhok_menu.tsvet_bannera_ottenok_3,
        dvizhok_menu.tsvet_bannera_ottenok_4,
      )
    }
    val kolichestvo = ottenki.size
    val vsego_stranits = kolichestvo * 200
    val sostoyanie_pager = rememberPagerState(initialPage = kolichestvo * 100) { vsego_stranits }
    val tekst_bannera = stringResource(R.string.banner_test)

    LaunchedEffect(sostoyanie_pager) {
      while (true) {
        delay(dvizhok_menu.INTERVAL_BANNERA_MS)
        while (sostoyanie_pager.isScrollInProgress) delay(100L)
        val sled_stranitsa = (sostoyanie_pager.currentPage + 1) % vsego_stranits
        sostoyanie_pager.animateScrollToPage(
          page = sled_stranitsa,
          animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(dvizhok_menu.vysota_bannera)
        .bityy_piksel_stil(dvizhok_menu.tsvet_bannera, dvizhok_menu.tsvet_ramki_svetliy, variant = 1)
        .clipToBounds()
        .testTag("banner_kraft"),
    ) {
      HorizontalPager(
        state = sostoyanie_pager,
        beyondViewportPageCount = 1,
        flingBehavior = PagerDefaults.flingBehavior(
          state = sostoyanie_pager,
          snapAnimationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        ),
        modifier = Modifier
          .fillMaxSize()
          .padding(dvizhok_menu.tolshina_ramki)
          .drawBehind {
            val baza = ((sostoyanie_pager.currentPage % kolichestvo) + kolichestvo) % kolichestvo
            val drob = sostoyanie_pager.currentPageOffsetFraction
            val sled = if (drob >= 0f) (baza + 1) % kolichestvo else (baza - 1 + kolichestvo) % kolichestvo
            drawRect(lerp(ottenki[baza], ottenki[sled], drob.absoluteValue.coerceIn(0f, 1f)))
          },
      ) { stranitsa ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .edinoe_nazhatie(onNazhatie)
            .padding(dvizhok_menu.otstup_osnovnoy)
            .graphicsLayer {
              val otklonenie = (sostoyanie_pager.currentPage - stranitsa) + sostoyanie_pager.currentPageOffsetFraction
              alpha = 1f - otklonenie.absoluteValue.coerceIn(0f, 1f) * 0.7f
              translationX = otklonenie * size.width * 0.12f
            },
          contentAlignment = Alignment.CenterStart,
        ) {
          Text(
            text = tekst_bannera,
            color = dvizhok_menu.tsvet_teksta,
            fontSize = dvizhok_menu.shrift_zagolovka,
            fontFamily = dvizhok_menu.shrift_igry,
            lineHeight = dvizhok_menu.v_sp(14f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // индикатор слайдов без перестроения layout на кадрах
      val shirina_indikatora = dvizhok_menu.v_dp(33f)
      val vysota_indikatora = dvizhok_menu.v_dp(2.5f)
      Canvas(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = dvizhok_menu.v_dp(6f), bottom = dvizhok_menu.v_dp(5f))
          .width(shirina_indikatora)
          .height(vysota_indikatora),
      ) {
        val aktiv_indeks = ((sostoyanie_pager.currentPage % kolichestvo) + kolichestvo) % kolichestvo
        val h = size.height
        val shag = h * 0.8f
        val korotkaya = h * 2f
        val dlinnaya = h * 4.8f
        var x = 0f
        for (indeks in 0 until kolichestvo) {
          val aktivno = indeks == aktiv_indeks
          val w = if (aktivno) dlinnaya else korotkaya
          val cvet = if (aktivno) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta.copy(alpha = 0.35f)
          drawRect(cvet, Offset(x, 0f), Size(w, h))
          x += w + shag
        }
      }
    }
  }

  // нижняя левая зона: статичный сворачиваемый чат и панель "> | НАСТРОЙКИ | МАГАЗИН | КЛАНЫ"
  @Composable
  private fun nizhnyaya_levaya_zona(
    modifier: Modifier = Modifier,
    aktivnaya_vkladka: Int,
    onVyborVkladki: (Int) -> Unit,
  ) {
    var pokazan_chat by remember { mutableStateOf(true) }
    val ugol_strelki = animateFloatAsState(
      targetValue = if (pokazan_chat) 90f else 0f,
      animationSpec = tween(durationMillis = dvizhok_menu.VREMYA_POVOROTA_MS, easing = FastOutSlowInEasing),
      label = "strelka_chata",
    )

    Column(modifier = modifier.width(dvizhok_menu.v_dp(310f))) {
      if (pokazan_chat) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(dvizhok_menu.vysota_chata)
            .bityy_piksel_stil(Color(0xA60C0E11), dvizhok_menu.tsvet_ramki, variant = 4)
            .padding(horizontal = dvizhok_menu.otstup_malyy),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.chat_soobshenie),
            color = dvizhok_menu.tsvet_teksta,
            fontSize = dvizhok_menu.shrift_malyy,
            fontFamily = dvizhok_menu.shrift_igry,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
        }
        Spacer(Modifier.height(dvizhok_menu.otstup_malyy))
      }

      Row(
        modifier = Modifier.fillMaxWidth().height(dvizhok_menu.vysota_nizhney_paneli),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .width(dvizhok_menu.v_dp(26f))
            .edinoe_nazhatie { pokazan_chat = !pokazan_chat }
            .testTag("knopka_skryt_chat"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stringResource(R.string.chat_strelka),
            color = if (pokazan_chat) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta_tuskliy,
            fontSize = dvizhok_menu.shrift_zagolovka,
            fontFamily = dvizhok_menu.shrift_igry,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.graphicsLayer { rotationZ = ugol_strelki.value },
          )
        }
        dvizhok_menu.razdelitel_vkladok()
        vkladka_knopka(
          stroka_id = R.string.vkladka_nastroyki,
          est_tochka = false,
          vybrano = aktivnaya_vkladka == R.string.vkladka_nastroyki,
          variant = 0,
          tag = "vkladka_nastroyki",
          modifier = Modifier.weight(1f),
        ) { onVyborVkladki(R.string.vkladka_nastroyki) }
        dvizhok_menu.razdelitel_vkladok()
        vkladka_knopka(
          stroka_id = R.string.vkladka_magazin,
          est_tochka = false,
          vybrano = aktivnaya_vkladka == R.string.vkladka_magazin,
          variant = 1,
          tag = "vkladka_magazin",
          modifier = Modifier.weight(1f),
        ) { onVyborVkladki(R.string.vkladka_magazin) }
        dvizhok_menu.razdelitel_vkladok()
        vkladka_knopka(
          stroka_id = R.string.vkladka_klany,
          est_tochka = true,
          vybrano = aktivnaya_vkladka == R.string.vkladka_klany,
          variant = 2,
          tag = "vkladka_klany",
          modifier = Modifier.weight(1f),
        ) { onVyborVkladki(R.string.vkladka_klany) }
      }
    }
  }

  // кнопка вкладки нижней панели в едином статичном стиле
  @Composable
  private fun vkladka_knopka(
    @StringRes stroka_id: Int,
    est_tochka: Boolean,
    vybrano: Boolean,
    variant: Int,
    tag: String,
    modifier: Modifier = Modifier,
    onNazhatie: () -> Unit,
  ) {
    Box(
      modifier = modifier
        .fillMaxHeight()
        .edinoe_nazhatie(onNazhatie)
        .testTag(tag),
      contentAlignment = Alignment.Center,
    ) {
      plavnaya_podsvetka_knopki(aktivno = vybrano, variant = variant)
      Row(
        modifier = Modifier.padding(horizontal = dvizhok_menu.otstup_malyy),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(stroka_id),
          color = if (vybrano) dvizhok_menu.tsvet_teksta else dvizhok_menu.tsvet_teksta_tuskliy,
          fontSize = dvizhok_menu.shrift_osnovnoy,
          fontFamily = dvizhok_menu.shrift_igry,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
        if (est_tochka) {
          Spacer(Modifier.width(dvizhok_menu.v_dp(3f)))
          Box(modifier = Modifier.size(dvizhok_menu.v_dp(4f)).background(dvizhok_menu.tsvet_trevogi))
        }
      }
    }
  }

  // главная кнопка "играть" в глубоких темных тонах
  @Composable
  private fun knopka_v_boy(modifier: Modifier = Modifier, onNazhatie: () -> Unit = {}) {
    Box(
      modifier = modifier
        .width(dvizhok_menu.shirina_knopki_boy)
        .height(dvizhok_menu.vysota_knopki_boy)
        .background(Brush.verticalGradient(listOf(dvizhok_menu.tsvet_knopki_boy, dvizhok_menu.tsvet_knopki_boy_niz)))
        .bityy_piksel_stil(Color.Transparent, dvizhok_menu.tsvet_knopki_boy_ramka, variant = 2)
        .edinoe_nazhatie(onNazhatie)
        .padding(horizontal = dvizhok_menu.otstup_malyy)
        .testTag("knopka_v_boy"),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = stringResource(R.string.knopka_igrat),
        color = dvizhok_menu.tsvet_teksta,
        fontSize = dvizhok_menu.shrift_boy,
        fontWeight = FontWeight.Bold,
        fontFamily = dvizhok_menu.shrift_igry,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }

  // карточка новости / test pass с векторной иконкой
  @Composable
  private fun kartochka_pravaya(
    @StringRes stroka_id: Int,
    ikonka_id: Int,
    fon: Color,
    variant: Int,
    modifier: Modifier = Modifier,
    onNazhatie: () -> Unit = {},
  ) {
    val zagolovok = stringResource(stroka_id)
    Box(
      modifier = modifier
        .height(dvizhok_menu.vysota_kartochki_magazina)
        .bityy_piksel_stil(fon, dvizhok_menu.tsvet_ramki, variant = variant)
        .edinoe_nazhatie(onNazhatie)
        .padding(dvizhok_menu.otstup_malyy)
        .testTag("kartochka_$variant"),
    ) {
      Text(
        text = zagolovok,
        color = dvizhok_menu.tsvet_teksta,
        fontSize = dvizhok_menu.shrift_kartochki,
        fontFamily = dvizhok_menu.shrift_igry,
        lineHeight = dvizhok_menu.shrift_kartochki,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = dvizhok_menu.v_dp(2f), end = dvizhok_menu.v_dp(6f)),
      )
      Box(modifier = Modifier.align(Alignment.BottomEnd)) {
        vektor_ikonka(
          resurs_id = ikonka_id,
          opisanie = zagolovok,
          tsvet = dvizhok_menu.tsvet_ikonki_svetliy,
          razmer = dvizhok_menu.v_dp(14f),
        )
      }
    }
  }

  // общая векторная иконка (dcl, уведомления, новости, test pass и левое меню)
  @Composable
  private fun vektor_ikonka(resurs_id: Int, opisanie: String, tsvet: Color, razmer: Dp) {
    Image(
      painter = painterResource(id = resurs_id),
      contentDescription = opisanie,
      colorFilter = ColorFilter.tint(tsvet),
      modifier = Modifier.size(razmer),
    )
  }

  // векторная иконка без рамки кнопки (dcl / уведомления)
  @Composable
  private fun knopka_vektor_ikonka(
    resurs_id: Int,
    @StringRes opisanie_id: Int,
    tag: String,
    onNazhatie: () -> Unit = {},
  ) {
    val opisanie = stringResource(opisanie_id)
    Box(
      modifier = Modifier
        .size(dvizhok_menu.v_dp(22f))
        .edinoe_nazhatie(onNazhatie)
        .testTag("ikonka_$tag"),
      contentAlignment = Alignment.Center,
    ) {
      vektor_ikonka(resurs_id, opisanie, dvizhok_menu.tsvet_ikonki_svetliy, dvizhok_menu.v_dp(18f))
    }
  }
}
