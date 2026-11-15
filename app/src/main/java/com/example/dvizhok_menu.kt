package com.example

import android.app.Activity
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Shader
import android.os.Build
import android.view.Surface
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.ShriftIgry
import kotlin.math.abs
import kotlin.math.roundToInt

// системный движок цветов, шрифта, 60 гц и 1к1 адаптации интерфейса
object dvizhok_menu {
  private const val BAZOVAYA_SHIRINA = 640f
  private const val BAZOVAYA_VYSOTA = 360f
  const val CHASTOTA_KADROV_GTS = 60f
  const val VREMYA_KADRA_NS = 1_000_000_000L / 60L
  const val INTERVAL_BANNERA_MS = 4000L
  const val VREMYA_POVOROTA_MS = 110

  // шрифт, межбуквенный интервал (+3%) и мрачная пиксельная палитра
  val shrift_igry = ShriftIgry
  val mezhbukvennyy_interval = 0.03.em
  private val bazovyy_stil_teksta = TextStyle(fontFamily = shrift_igry, letterSpacing = mezhbukvennyy_interval)
  val tsvet_fona = Color(0xFF000000)
  val tsvet_paneli = Color(0xDB0D1017)
  val tsvet_knopki = Color(0xE311151F)
  val tsvet_knopki_temney = Color(0xE30E111A)
  val tsvet_knopki_bolotniy = Color(0xE3141620)
  val tsvet_knopki_aktivnoy = Color(0xF0191F2C)
  val tsvet_bannera = Color(0xE310141E)
  val tsvet_bannera_ottenok_1 = Color(0x129BAAC6)
  val tsvet_bannera_ottenok_2 = Color(0x201E273A)
  val tsvet_bannera_ottenok_3 = Color(0x1E231F2D)
  val tsvet_bannera_ottenok_4 = Color(0x20311D1C)
  val tsvet_ramki = Color(0xFF2D3443)
  val tsvet_ramki_svetliy = Color(0xFF4B556A)
  val tsvet_ramki_aktivnoy = Color(0xFF727F97)
  val tsvet_potertosti = Color(0x1BB8C5DC)
  val tsvet_pyli_svetloy = Color(0x18CFD8E6)
  val tsvet_pyli_temnoy = Color(0x3805070A)
  val tsvet_zaklepki = Color(0xFF424B5C)
  val tsvet_polosok = Color(0x24000000)
  val tsvet_teksta = Color(0xFFE2E6EB)
  val tsvet_ikonki_svetliy = Color(0xFFD9DCE0)
  val tsvet_teksta_tuskliy = Color(0xFF8A939E)
  val tsvet_aktsenta = Color(0xFF56AD46)
  val tsvet_knopki_boy = Color(0xFF4C1B16)
  val tsvet_knopki_boy_niz = Color(0xFF1B0807)
  val tsvet_knopki_boy_ramka = Color(0xFF7E3126)
  val tsvet_trevogi = Color(0xFFD93829)
  val tsvet_zolotoy = Color(0xFFE0B44A)
  val tsvet_serebra = Color(0xFFC2C9D2)
  val tsvet_avatara_fon = Color(0xFF161A23)
  val tsvet_vinyetki = Color(0x5205070B)
  val tsvet_vinyetki_inventarya = Color(0x9605070B)
  val tsvet_yacheyki = Color(0xD60B0E14)
  val tsvet_fona_slota_verh = Color(0xE0181E2B)
  val tsvet_plashki_igroka = Color(0xBD04060A)
  val kist_verha_slota = Brush.verticalGradient(listOf(tsvet_fona_slota_verh.copy(alpha = 0.55f), Color.Transparent))

  // кэш фоновой картинки в rgb_565 для мгновенного открытия экранов без нагрузки на память
  private var kesh_fona_id = 0
  private var kesh_fona_bitmap: ImageBitmap? = null
  var kesh_shrifta_view: android.graphics.Typeface? = null

  // шесть уровней редкости предметов от обычного до секретного
  enum class redkost_predmeta(val rang: Int, @StringRes val nazvanie_id: Int, val tsvet: Color) {
    OBYCHNOE(1, R.string.redkost_obychnoe, Color(0xEB3B4454)),
    REDKOE(2, R.string.redkost_redkoe, Color(0xEB254E78)),
    EPICHESKOE(3, R.string.redkost_epicheskoe, Color(0xEB4F2F6B)),
    LEGENDARNOE(4, R.string.redkost_legendarnoe, Color(0xEB72284B)),
    MIFICHESKOE(5, R.string.redkost_mificheskoe, Color(0xEB7D261F)),
    SEKRETNOE(6, R.string.redkost_sekretnoe, Color(0xEB8A6829)),
  }

  val tsvetovaya_shema =
    darkColorScheme(
      primary = tsvet_aktsenta,
      background = tsvet_fona,
      surface = tsvet_paneli,
      onPrimary = tsvet_teksta,
      onBackground = tsvet_teksta,
      onSurface = tsvet_teksta,
    )

  // текущие размеры экрана и масштаб
  var shirina_ekrana by mutableStateOf(640.dp)
    private set
  var vysota_ekrana by mutableStateOf(360.dp)
    private set
  var koefficient_masshtaba by mutableStateOf(1f)
    private set
  private var koef_shrifta = 1f

  // адаптивные отступы и пиксельный шаг
  var otstup_kraya by mutableStateOf(6.dp)
    private set
  var otstup_osnovnoy by mutableStateOf(7.dp)
    private set
  var otstup_malyy by mutableStateOf(4.dp)
    private set
  var tolshina_ramki by mutableStateOf(1.dp)
    private set
  var razmer_pikselya by mutableStateOf(2.dp)
    private set
  var shag_polosok by mutableStateOf(3.dp)
    private set

  // адаптивные размеры блоков главного меню
  var vysota_verhney_paneli by mutableStateOf(38.dp)
    private set
  var shirina_profilya by mutableStateOf(154.dp)
    private set
  var razmer_avatara by mutableStateOf(30.dp)
    private set
  var shirina_levoy_knopki by mutableStateOf(128.dp)
    private set
  var vysota_levoy_knopki by mutableStateOf(30.dp)
    private set
  var shirina_pravoy_paneli by mutableStateOf(152.dp)
    private set
  var vysota_bannera by mutableStateOf(52.dp)
    private set
  var vysota_kartochki_magazina by mutableStateOf(38.dp)
    private set
  var razmer_plyusa by mutableStateOf(18.dp)
    private set
  var shirina_knopki_boy by mutableStateOf(136.dp)
    private set
  var vysota_knopki_boy by mutableStateOf(38.dp)
    private set
  var vysota_chata by mutableStateOf(20.dp)
    private set
  var vysota_nizhney_paneli by mutableStateOf(26.dp)
    private set
  var vysota_shapki_razdela by mutableStateOf(30.dp)
    private set
  var shirina_vkladki_inventarya by mutableStateOf(96.dp)
    private set
  var vysota_podvkladki_rynka by mutableStateOf(26.dp)
    private set
  var vysota_stroki_rynka by mutableStateOf(40.dp)
    private set

  // адаптивные размеры шрифтов
  var shrift_boy by mutableStateOf(20.sp)
    private set
  var shrift_avatara by mutableStateOf(17.sp)
    private set
  var shrift_id by mutableStateOf(10.5.sp)
    private set
  var shrift_nika by mutableStateOf(14.sp)
    private set
  var shrift_zagolovka by mutableStateOf(13.sp)
    private set
  var shrift_kartochki by mutableStateOf(13.sp)
    private set
  var shrift_valyuty by mutableStateOf(15.sp)
    private set
  var shrift_osnovnoy by mutableStateOf(11.sp)
    private set
  var shrift_malyy by mutableStateOf(9.sp)
    private set
  var shrift_slota by mutableStateOf(8.sp)
    private set

  // пересчет 1к1 пропорций под полный размер экрана любого телефона
  fun obnova_razmerov(shirina: Dp, vysota: Dp, masshtab_shrifta: Float = 1f) {
    if (shirina <= 0.dp || vysota <= 0.dp) return
    val bezopasnyy_shrift = masshtab_shrifta.coerceAtLeast(0.5f)
    val shirina_land = maxOf(shirina.value, vysota.value)
    val vysota_land = minOf(shirina.value, vysota.value)
    if (
      shirina_land == shirina_ekrana.value &&
      vysota_land == vysota_ekrana.value &&
      bezopasnyy_shrift == koef_shrifta
    ) return

    val masshtab = minOf(shirina_land / BAZOVAYA_SHIRINA, vysota_land / BAZOVAYA_VYSOTA).coerceIn(0.65f, 2.5f)
    shirina_ekrana = shirina_land.dp
    vysota_ekrana = vysota_land.dp
    koefficient_masshtaba = masshtab
    koef_shrifta = bezopasnyy_shrift

    otstup_kraya = v_dp(6f)
    otstup_osnovnoy = v_dp(7f)
    otstup_malyy = v_dp(4f)
    tolshina_ramki = v_dp(1f).coerceAtLeast(1.dp)
    razmer_pikselya = v_dp(2f).coerceAtLeast(1.5.dp)
    shag_polosok = v_dp(2.5f).coerceAtLeast(2.dp)

    vysota_verhney_paneli = v_dp(38f)
    shirina_profilya = v_dp(154f)
    razmer_avatara = v_dp(30f)
    shirina_levoy_knopki = v_dp(128f)
    vysota_levoy_knopki = v_dp(30f)
    shirina_pravoy_paneli = v_dp(152f)
    vysota_bannera = v_dp(52f)
    vysota_kartochki_magazina = v_dp(38f)
    razmer_plyusa = v_dp(18f)
    shirina_knopki_boy = v_dp(136f)
    vysota_knopki_boy = v_dp(38f)
    vysota_chata = v_dp(20f)
    vysota_nizhney_paneli = v_dp(26f)
    vysota_shapki_razdela = v_dp(30f)
    shirina_vkladki_inventarya = v_dp(96f)
    vysota_podvkladki_rynka = v_dp(26f)
    vysota_stroki_rynka = v_dp(40f)

    shrift_boy = v_sp(20f)
    shrift_avatara = v_sp(17f)
    shrift_id = v_sp(10.5f)
    shrift_nika = v_sp(14f)
    shrift_zagolovka = v_sp(13f)
    shrift_kartochki = v_sp(13f)
    shrift_valyuty = v_sp(15f)
    shrift_osnovnoy = v_sp(11f)
    shrift_malyy = v_sp(9.5f)
    shrift_slota = v_sp(8f)
  }

  // форматирование золота (123,45) и серебра (5; 100; 1500)
  fun format_zolota(summa: Double): String {
    val sotye = kotlin.math.round(summa.coerceAtLeast(0.0) * 100.0).toLong()
    val tselye = sotye / 100L
    val drobnye = (sotye % 100L).toString().padStart(2, '0')
    return "$tselye,$drobnye"
  }

  fun format_serebra(summa: Long): String = summa.coerceAtLeast(0L).toString()

  // перевод базовых значений в 1к1 размеры
  fun v_dp(baza: Float): Dp = (baza * koefficient_masshtaba).dp

  fun v_sp(baza: Float): TextUnit = ((baza * koefficient_masshtaba) / koef_shrifta).sp

  // однократное чтение облегченного фона без повторного декодирования png
  private fun poluchit_fon_bitmap(resursy: Resources, fon_id: Int): ImageBitmap {
    val tekushiy = kesh_fona_bitmap
    if (kesh_fona_id == fon_id && tekushiy != null) return tekushiy
    val opcii = BitmapFactory.Options().apply {
      inPreferredConfig = Bitmap.Config.RGB_565
      inSampleSize = 2
    }
    val novyy = BitmapFactory.decodeResource(resursy, fon_id, opcii).asImageBitmap()
    kesh_fona_id = fon_id
    kesh_fona_bitmap = novyy
    return novyy
  }

  // обертка экрана: заполнение 100% экрана включая зону камеры и отключение растяжения скролла
  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  fun konteyner_1k1(
    modifikator: Modifier = Modifier,
    fon_id: Int? = null,
    soderzhimoe: @Composable BoxScope.() -> Unit,
  ) {
    val plotnost = LocalDensity.current
    val resursy = LocalContext.current.resources
    Box(modifier = modifikator.fillMaxSize().background(tsvet_fona)) {
      if (fon_id != null) {
        val bitmap_fona = remember(fon_id) { poluchit_fon_bitmap(resursy, fon_id) }
        Image(
          bitmap = bitmap_fona,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          filterQuality = FilterQuality.None,
          modifier = Modifier.fillMaxSize(),
        )
      }
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        obnova_razmerov(maxWidth, maxHeight, plotnost.fontScale)
        CompositionLocalProvider(
          LocalOverscrollConfiguration provides null,
          LocalTextStyle provides LocalTextStyle.current.merge(bazovyy_stil_teksta),
        ) {
          Box(modifier = Modifier.fillMaxSize(), content = soderzhimoe)
        }
      }
    }
  }

  // аппаратный шейдерный слой горизонтальных полосок поверх всего ui без нагрузки на cpu/gpu
  @Composable
  fun sloy_polosok_sverhu() {
    val shag_dp = shag_polosok
    Box(
      modifier = Modifier.fillMaxSize().drawWithCache {
        val shag = shag_dp.toPx().roundToInt().coerceAtLeast(3)
        val bitmap_polosok = Bitmap.createBitmap(1, shag, Bitmap.Config.ARGB_8888).apply {
          setPixel(0, 0, tsvet_polosok.toArgb())
        }
        val kist_polosok = ShaderBrush(
          BitmapShader(bitmap_polosok, Shader.TileMode.CLAMP, Shader.TileMode.REPEAT)
        )
        onDrawBehind { drawRect(brush = kist_polosok) }
      }
    )
  }

  // чистый клик без анимации нажатия для всех кнопок
  @Composable
  fun Modifier.edinoe_nazhatie(onNazhatie: () -> Unit): Modifier {
    val istochnik = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = istochnik, indication = null, role = Role.Button, onClick = onNazhatie)
  }

  // мгновенный слой выделения (фон + белая рамка) без задержки и лишних узлов
  @Composable
  fun BoxScope.plavnaya_podsvetka_knopki(
    aktivno: Boolean,
    variant: Int,
    s_skolami: Boolean = false,
  ) {
    if (aktivno) {
      Box(
        modifier = Modifier
          .matchParentSize()
          .bityy_piksel_stil(
            fon = tsvet_knopki_aktivnoy,
            ramka = tsvet_ramki_aktivnoy,
            variant = variant,
            aktivno = true,
            s_skolami = s_skolami,
          ),
      )
    }
  }

  // единый пиксельный слой кнопки/слота с мгновенным переключением активной рамки
  @Composable
  fun BoxScope.fon_s_podsvetkoy(
    fon_bazy: Color,
    aktivno: Boolean,
    variant: Int,
    s_skolami: Boolean = false,
  ) {
    Box(
      modifier = Modifier
        .matchParentSize()
        .bityy_piksel_stil(
          fon = if (aktivno) tsvet_knopki_aktivnoy else fon_bazy,
          ramka = if (aktivno) tsvet_ramki_aktivnoy else tsvet_ramki,
          variant = variant,
          aktivno = aktivno,
          s_skolami = s_skolami,
        ),
    )
  }

  // быстрый батчированный модификатор кнопки и блока без тяжелых path и шейдеров
  fun Modifier.bityy_piksel_stil(
    fon: Color,
    ramka: Color,
    variant: Int,
    aktivno: Boolean = false,
    s_skolami: Boolean = false,
  ): Modifier {
    val px_dp = razmer_pikselya
    val ramka_dp = tolshina_ramki
    return this.drawWithCache {
      val px = px_dp.toPx().coerceAtLeast(2f)
      val r = ramka_dp.toPx().coerceAtLeast(1f)
      val cvet_ramki = if (aktivno) tsvet_ramki_aktivnoy else ramka
      val sh = size.width
      val vs = size.height
      val bolshoy = sh >= px * 16f
      val kray_pravo = shum_pikselya(11, variant) >= 0.45f
      val kray_verh = shum_pikselya(12, variant) >= 0.50f
      val baza_cveta = if (fon.alpha > 0.1f) fon else cvet_ramki
      val cvet_treschiny = lerp(baza_cveta, Color.Black, 0.42f).copy(alpha = 0.65f)
      val cvet_grani = lerp(cvet_ramki, Color.White, 0.14f).copy(alpha = 0.26f)
      val cvet_skola = lerp(baza_cveta, Color.Black, 0.55f).copy(alpha = 0.78f)
      val cvet_vyreza = tsvet_fona.copy(alpha = 0.72f)
      val tochki_pyli_sv = ArrayList<Offset>(10)
      val tochki_pyli_tm = ArrayList<Offset>(6)
      val linii_tsar_sv = ArrayList<Offset>(6)
      val linii_tsar_tm = ArrayList<Offset>(6)
      val tochki_vyreza = ArrayList<Offset>(4)
      val tochki_kroshki = ArrayList<Offset>(4)
      if (bolshoy) {
        zapolnit_pyl(variant, sh, vs, r, tochki_pyli_sv, tochki_pyli_tm)
        zapolnit_tsarapiny(variant, sh, vs, px, r, linii_tsar_sv, linii_tsar_tm)
        if (s_skolami) {
          zapolnit_skoly_kraya(variant, sh, vs, r, tochki_vyreza, tochki_kroshki)
        }
      }
      val razmer_zerna = r * 1.35f
      val tolshina_tsar_tm = r * 0.75f
      val tolshina_tsar_sv = r * 0.65f
      val cvet_tsar_tm = cvet_treschiny.copy(alpha = 0.48f)
      val cvet_tsar_sv = cvet_grani.copy(alpha = 0.20f)
      val cvet_kroshek = cvet_ramki.copy(alpha = 0.30f)
      onDrawWithContent {
        if (fon.alpha > 0f) drawRect(fon)
        if (bolshoy) {
          if (tochki_pyli_tm.isNotEmpty()) drawPoints(tochki_pyli_tm, PointMode.Points, tsvet_pyli_temnoy, strokeWidth = razmer_zerna)
          if (tochki_pyli_sv.isNotEmpty()) drawPoints(tochki_pyli_sv, PointMode.Points, tsvet_pyli_svetloy, strokeWidth = razmer_zerna)
          if (linii_tsar_tm.isNotEmpty()) drawPoints(linii_tsar_tm, PointMode.Lines, cvet_tsar_tm, strokeWidth = tolshina_tsar_tm)
          if (linii_tsar_sv.isNotEmpty()) drawPoints(linii_tsar_sv, PointMode.Lines, cvet_tsar_sv, strokeWidth = tolshina_tsar_sv)
        }
        drawContent()
        drawRect(tsvet_potertosti, Offset(r, r), Size((sh - r * 2f).coerceAtLeast(0f), r))
        drawRect(cvet_treschiny, Offset(r, (vs - r * 2f).coerceAtLeast(0f)), Size((sh - r * 2f).coerceAtLeast(0f), r))
        risovat_potertuyu_ramku(variant, sh, vs, px, r, cvet_ramki, cvet_skola, cvet_grani)
        if (s_skolami && bolshoy) {
          if (tochki_vyreza.isNotEmpty()) drawPoints(tochki_vyreza, PointMode.Lines, cvet_vyreza, strokeWidth = r * 1.2f)
          if (tochki_kroshki.isNotEmpty()) drawPoints(tochki_kroshki, PointMode.Points, cvet_kroshek, strokeWidth = r)
        }
        val ugol_x = if (kray_pravo) sh - px else 0f
        val ugol_y = if (kray_verh) 0f else vs - px
        drawRect(cvet_skola, Offset(ugol_x, ugol_y), Size(px, px))
        drawRect(cvet_ramki, Offset(if (kray_pravo) sh - px - r else px, if (kray_verh) px else vs - px - r), Size(r, r))
        drawRect(tsvet_zaklepki, Offset(if (kray_pravo) px * 1.2f else sh - px * 1.2f - r, if (kray_verh) vs - px * 1.2f - r else px * 1.2f), Size(r, r))
      }
    }
  }

  // быстрая потертость и микропыль для цветной полоски названия в ячейке без path
  fun Modifier.potertost_polosy(tsvet_polosy: Color, variant: Int): Modifier {
    val px_dp = razmer_pikselya
    val ramka_dp = tolshina_ramki
    return this.drawWithCache {
      val px = px_dp.toPx().coerceAtLeast(2f)
      val r = ramka_dp.toPx().coerceAtLeast(1f)
      val sh = size.width
      val vs = size.height
      val cvet_temniy = lerp(tsvet_polosy, Color.Black, 0.35f).copy(alpha = 0.32f)
      val cvet_svetliy = lerp(tsvet_polosy, Color.White, 0.22f).copy(alpha = 0.16f)
      val tochki_pyli = ArrayList<Offset>(5)
      val linii_tsar = ArrayList<Offset>(4)
      for (indeks in 0 until 5) {
        val x = sh * (0.06f + shum_pikselya(indeks * 2 + 101, variant) * 0.88f)
        val y = vs * (0.16f + shum_pikselya(indeks * 2 + 102, variant) * 0.68f)
        tochki_pyli.add(Offset(x, y))
      }
      for (indeks in 0 until 2) {
        val x = sh * (0.14f + shum_pikselya(indeks + 130, variant) * 0.68f)
        val y = vs * (0.20f + shum_pikselya(indeks + 140, variant) * 0.60f)
        val dlina = px * (1.5f + shum_pikselya(indeks + 150, variant) * 2.0f)
        val sdvig_y = (shum_pikselya(indeks + 160, variant) - 0.5f) * vs * 0.45f
        linii_tsar.add(Offset(x, y))
        linii_tsar.add(Offset(x + dlina, y + sdvig_y))
      }
      val razmer_pyli = r * 1.2f
      val tolshina_tsar = r * 0.75f
      onDrawBehind {
        drawRect(tsvet_polosy)
        drawPoints(tochki_pyli, PointMode.Points, cvet_svetliy, strokeWidth = razmer_pyli)
        drawPoints(linii_tsar, PointMode.Lines, cvet_temniy, strokeWidth = tolshina_tsar)
      }
    }
  }

  private fun shum_pikselya(indeks: Int, variant: Int): Float {
    val h = ((indeks * 374761393 + variant * 668265263) xor 0x5bd1e995) * 1274126177
    return ((h ushr 8) and 0xFFFF) / 65535f
  }

  private fun zapolnit_pyl(
    variant: Int, sh: Float, vs: Float, r: Float, svetlaya: MutableList<Offset>, temnaya: MutableList<Offset>,
  ) {
    val kolvo = ((sh * vs) / 1600f).toInt().coerceIn(8, 14)
    for (indeks in 0 until kolvo) {
      val sx = shum_pikselya(indeks * 3 + 1, variant)
      val sy_raw = shum_pikselya(indeks * 3 + 2, variant)
      val sy = if (indeks % 4 == 0) sy_raw else if (sy_raw < 0.5f) sy_raw * sy_raw * 1.1f else 1f - (1f - sy_raw) * (1f - sy_raw) * 1.1f
      val x = sx * (sh - r * 4f) + r * 2f
      val y = sy.coerceIn(0.05f, 0.95f) * (vs - r * 4f) + r * 2f
      (if (indeks % 3 == 0) temnaya else svetlaya).add(Offset(x, y))
    }
  }

  private fun zapolnit_tsarapiny(
    variant: Int, sh: Float, vs: Float, px: Float, r: Float, svetlaya: MutableList<Offset>, temnaya: MutableList<Offset>,
  ) {
    val kolvo = 2 + (shum_pikselya(39, variant) * 2f).toInt().coerceAtMost(1)
    for (indeks in 0 until kolvo) {
      val x = sh * (0.08f + shum_pikselya(indeks + 40, variant) * 0.78f)
      val dolya_y = shum_pikselya(indeks + 50, variant)
      val y = when (indeks % 3) {
        0 -> vs * (0.08f + dolya_y * 0.18f)
        1 -> vs * (0.72f + dolya_y * 0.18f)
        else -> vs * (0.26f + dolya_y * 0.42f)
      }
      val dlina = px * (1.8f + shum_pikselya(indeks + 60, variant) * 2.6f)
      val sdvig_y = (shum_pikselya(indeks + 70, variant) - 0.5f) * px * 1.2f
      temnaya.add(Offset(x, y))
      temnaya.add(Offset(x + dlina, y + sdvig_y))
      svetlaya.add(Offset(x + r * 0.7f, y - r * 0.6f))
      svetlaya.add(Offset(x + dlina + r * 0.7f, y + sdvig_y - r * 0.6f))
    }
  }

  private fun DrawScope.risovat_potertuyu_ramku(
    variant: Int, sh: Float, vs: Float, px: Float, r: Float, cvet_ramki: Color, cvet_skola: Color, cvet_grani: Color,
  ) {
    drawRect(cvet_ramki, Offset.Zero, Size(sh, r))
    drawRect(cvet_ramki, Offset(0f, vs - r), Size(sh, r))
    drawRect(cvet_ramki, Offset.Zero, Size(r, vs))
    drawRect(cvet_ramki, Offset(sh - r, 0f), Size(r, vs))
    if (sh < px * 16f) return
    val p_verh = sh * (0.16f + shum_pikselya(80, variant) * 0.64f)
    val p_niz = sh * (0.18f + shum_pikselya(81, variant) * 0.62f)
    val dlin_verh = px * (1.2f + shum_pikselya(82, variant) * 1.2f)
    val dlin_niz = px * (1.0f + shum_pikselya(83, variant) * 1.1f)
    drawRect(cvet_skola, Offset(p_verh, 0f), Size(dlin_verh, r * 1.3f))
    drawRect(cvet_grani, Offset(p_verh, r * 1.3f), Size(dlin_verh, r * 0.8f))
    drawRect(cvet_skola, Offset(p_niz, vs - r * 1.3f), Size(dlin_niz, r * 1.3f))
    val bok_levo = shum_pikselya(84, variant) >= 0.5f
    val bok_y = vs * (0.24f + shum_pikselya(85, variant) * 0.50f)
    drawRect(cvet_skola, Offset(if (bok_levo) 0f else sh - r * 1.3f, bok_y), Size(r * 1.3f, px * 1.4f))
  }

  private fun zapolnit_skoly_kraya(
    variant: Int, sh: Float, vs: Float, r: Float, vyrez: MutableList<Offset>, kroshki: MutableList<Offset>,
  ) {
    for (storona in 0 until 4) {
      if (shum_pikselya(200 + storona, variant) < 0.58f) continue
      val poziciya = 0.14f + shum_pikselya(210 + storona, variant) * 0.68f
      val dlina = r * (1.4f + shum_pikselya(220 + storona, variant) * 1.8f)
      when (storona) {
        0 -> {
          val x = sh * poziciya
          vyrez.add(Offset(x, r * 0.5f)); vyrez.add(Offset(x + dlina, r * 0.5f))
          kroshki.add(Offset(x - r * 0.5f, r * 1.5f))
        }
        1 -> {
          val x = sh * poziciya
          vyrez.add(Offset(x, vs - r * 0.5f)); vyrez.add(Offset(x + dlina, vs - r * 0.5f))
          kroshki.add(Offset(x + dlina + r * 0.5f, vs - r * 1.5f))
        }
        2 -> {
          val y = vs * poziciya
          vyrez.add(Offset(r * 0.5f, y)); vyrez.add(Offset(r * 0.5f, y + dlina))
          kroshki.add(Offset(r * 1.5f, y - r * 0.5f))
        }
        else -> {
          val y = vs * poziciya
          vyrez.add(Offset(sh - r * 0.5f, y)); vyrez.add(Offset(sh - r * 0.5f, y + dlina))
          kroshki.add(Offset(sh - r * 1.5f, y + dlina + r * 0.5f))
        }
      }
    }
  }

  // дефолтная пиксельная аватарка с первой заглавной буквой ника игрока
  @Composable
  fun avatarka_igroka(
    imya_igroka: String,
    razmer: Dp,
    shrift: TextUnit,
    variant: Int,
    bitye_kraya: Boolean = true,
  ) {
    val pervaya_bukva = imya_igroka.firstOrNull()?.uppercase()?.toString() ?: stringResource(R.string.profil_zaglushka)
    val mod_ramki =
      if (bitye_kraya) Modifier.bityy_piksel_stil(tsvet_avatara_fon, tsvet_ramki_svetliy, variant = variant)
      else Modifier.background(tsvet_avatara_fon).border(tolshina_ramki, tsvet_ramki_svetliy)
    Box(
      modifier = Modifier.size(razmer).then(mod_ramki),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = pervaya_bukva,
        color = tsvet_teksta,
        fontSize = shrift,
        fontWeight = FontWeight.Bold,
        fontFamily = shrift_igry,
        style = TextStyle(
          letterSpacing = mezhbukvennyy_interval,
          platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        maxLines = 1,
        softWrap = false,
      )
    }
  }

  // вертикальный разделитель вкладок
  @Composable
  fun razdelitel_vkladok() {
    Box(modifier = Modifier.width(tolshina_ramki).height(v_dp(12f)).background(tsvet_ramki))
  }

  // увеличенный счетчик валюты в верхней панели (g и s)
  @Composable
  fun schetchik_valyuty(@StringRes metka_id: Int, tsvet_metki: Color, znachenie: String, onNazhatie: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = stringResource(metka_id),
        color = tsvet_metki,
        fontSize = shrift_valyuty,
        fontWeight = FontWeight.Bold,
        fontFamily = shrift_igry,
        maxLines = 1,
        softWrap = false,
      )
      Spacer(Modifier.width(otstup_malyy))
      Text(
        text = znachenie,
        color = tsvet_teksta,
        fontSize = shrift_valyuty,
        fontFamily = shrift_igry,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = v_dp(85f)),
      )
      Spacer(Modifier.width(otstup_malyy))
      Box(
        modifier = Modifier.size(razmer_plyusa).bityy_piksel_stil(tsvet_knopki, tsvet_ramki, variant = 1).edinoe_nazhatie(onNazhatie),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.size(v_dp(10f))) {
          val s = size.minDimension / 10f
          drawRect(tsvet_teksta, Offset(4f * s, s), Size(2f * s, 8f * s))
          drawRect(tsvet_teksta, Offset(s, 4f * s), Size(8f * s, 2f * s))
        }
      }
    }
  }

  // настройка полноэкранного окна и частоты 60 гц при запуске
  fun nastroit_okno(aktivnost: ComponentActivity) {
    aktivnost.enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
    )
    zadat_60_gts(aktivnost)
    skrit_paneli_sistemy(aktivnost.window)
  }

  // отключение системной анимации перехода между экранами (api 24-36)
  fun bez_perehoda(aktivnost: ComponentActivity, zakrytie: Boolean = false) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      val tip = if (zakrytie) Activity.OVERRIDE_TRANSITION_CLOSE else Activity.OVERRIDE_TRANSITION_OPEN
      aktivnost.overrideActivityTransition(tip, 0, 0)
    } else {
      @Suppress("DEPRECATION")
      aktivnost.overridePendingTransition(0, 0)
    }
  }

  // фиксация частоты экрана на 60 гц (api 24-36)
  fun zadat_60_gts(aktivnost: ComponentActivity) {
    val ekran =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) aktivnost.display
      else @Suppress("DEPRECATION") aktivnost.windowManager.defaultDisplay
    val tekushiy = ekran?.mode ?: return
    val rezhim_60 =
      ekran.supportedModes
        .filter {
          it.physicalWidth == tekushiy.physicalWidth &&
            it.physicalHeight == tekushiy.physicalHeight
        }
        .minByOrNull { abs(it.refreshRate - CHASTOTA_KADROV_GTS) }
        ?: return
    aktivnost.window.attributes =
      aktivnost.window.attributes.apply { preferredDisplayModeId = rezhim_60.modeId }
  }

  // фиксация 60 гц для игровой поверхности (api 30+)
  fun zadat_60_gts_poverhnosti(poverhnost: Surface) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      poverhnost.setFrameRate(
        CHASTOTA_KADROV_GTS,
        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
        Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS,
      )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      poverhnost.setFrameRate(
        CHASTOTA_KADROV_GTS,
        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
      )
    }
  }

  // ограничение шага кадра ровно под 60 гц
  fun ogranichit_kadr_60_gts(nachalo_kadra_ns: Long) {
    val zatracheno_ns = System.nanoTime() - nachalo_kadra_ns
    val ostatok_ns = VREMYA_KADRA_NS - zatracheno_ns
    if (ostatok_ns > 0L) {
      val millis = ostatok_ns / 1_000_000L
      val nanos = (ostatok_ns % 1_000_000L).toInt()
      try {
        Thread.sleep(millis, nanos)
      } catch (_: InterruptedException) {}
    }
  }

  // скрытие системных панелей и отрисовка во всю ширину экрана включая вырез камеры
  fun skrit_paneli_sistemy(okno: Window) {
    WindowCompat.setDecorFitsSystemWindows(okno, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      okno.attributes =
        okno.attributes.apply {
          layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      okno.attributes =
        okno.attributes.apply {
          layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    val upravlenie = WindowCompat.getInsetsController(okno, okno.decorView)
    upravlenie.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    upravlenie.hide(WindowInsetsCompat.Type.systemBars())
  }
}
