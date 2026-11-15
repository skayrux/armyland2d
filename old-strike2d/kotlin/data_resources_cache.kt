package skayrux.shootout2d

import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Process
import android.os.SystemClock
import androidx.annotation.RawRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ResourceCache {
    @Volatile
    private var textures: ImageBitmap? = null

    @Volatile
    private var upravleniye: ImageBitmap? = null

    @Volatile
    private var objects: ImageBitmap? = null

    @Volatile
    private var fonGame: ImageBitmap? = null

    @Volatile
    private var atlasButtons: ImageBitmap? = null

    @Volatile
    private var unitsAtlas: ImageBitmap? = null

    @Volatile
    private var fireAtlas: ImageBitmap? = null

    @Volatile
    private var snarAtlas: ImageBitmap? = null

    @Volatile
    private var smkAtlas: ImageBitmap? = null

    @Volatile
    private var carsAtlas: ImageBitmap? = null

    @Volatile
    private var bugsAtlas: ImageBitmap? = null

    @Volatile
    private var ptAtlas: ImageBitmap? = null

    @Volatile
    private var glAtlas: ImageBitmap? = null

    @Volatile
    private var markersAtlas: ImageBitmap? = null

    // параметры 64-канального pcm аудио-движка
    private const val MAX_SOUNDS = 84
    private const val MAX_VOICES = 64
    private const val SAMPLE_RATE = 44100
    private const val MIX_FRAMES = 256
    private const val HEARING_RADIUS = 1100f

    private val soundLock = Any()
    private val voiceLock = Any()

    private val soundResIds = IntArray(MAX_SOUNDS)
    private val soundIds = IntArray(MAX_SOUNDS) { it + 1 }
    private val soundPeriods = IntArray(MAX_SOUNDS)
    private val soundPriorities = IntArray(MAX_SOUNDS)
    private val soundMaxVoices = IntArray(MAX_SOUNDS) { 4 }
    private val soundClusterWindowMs = LongArray(MAX_SOUNDS) { 15L }
    private val pcmSamples = arrayOfNulls<ShortArray>(MAX_SOUNDS)
    private val decodedCount = AtomicInteger(0)

    @Volatile
    private var tableInitialized = false

    @Volatile
    private var pcmDecoded = false

    @Volatile
    private var mixerRunning = false

    @Volatile
    private var mixerThread: Thread? = null

    @Volatile
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var forceTrackReset = false

    // состояние 64 виртуальных каналов микшера
    private val vActive = BooleanArray(MAX_VOICES)
    private val vSoundId = IntArray(MAX_VOICES) { -1 }
    private val vStreamId = IntArray(MAX_VOICES)
    private val vPos = DoubleArray(MAX_VOICES)
    private val vRate = DoubleArray(MAX_VOICES) { 1.0 }
    private val vVolL = FloatArray(MAX_VOICES)
    private val vVolR = FloatArray(MAX_VOICES)
    private val vPriority = IntArray(MAX_VOICES)
    private val vLoop = BooleanArray(MAX_VOICES)
    private val vStartMs = LongArray(MAX_VOICES)
    private val nextAllowedPlayMs = LongArray(MAX_SOUNDS)
    private val lastTriggeredSlot = IntArray(MAX_SOUNDS) { -1 }
    private var nextStreamSeq = 10

    // таблица всех 84 звуков игры со старыми периодами speriod
    private fun initSoundTable() {
        if (tableInitialized) return
        synchronized(soundLock) {
            if (tableInitialized) return

            fun reg(id: Int, @RawRes res: Int, period: Int, prio: Int) {
                soundResIds[id] = res
                soundIds[id] = id + 1
                soundPeriods[id] = period
                soundPriorities[id] = prio
            }

            reg(0, R.raw.s_angr, 5, 2)
            reg(1, R.raw.s_p3, 3, 2)
            reg(2, R.raw.s_p2, 3, 2)
            reg(3, R.raw.s_dr1, 5, 2)
            reg(4, R.raw.s_zatdr1, 5, 1)
            reg(5, R.raw.s_perdr, 5, 1)
            reg(6, R.raw.s_rpg, 3, 2)
            reg(7, R.raw.s_an2, 5, 2)
            reg(8, R.raw.s_ak47, 2, 2)
            reg(9, R.raw.s_p90, 2, 2)
            reg(10, R.raw.s_vintorez, 2, 1)
            reg(11, R.raw.s_osv, 5, 3)
            reg(12, R.raw.s_minig, 2, 2)
            reg(13, R.raw.s_kor, 5, 1)
            reg(14, R.raw.s_slp, 2, 1)
            reg(15, R.raw.s_dim, 2, 1)
            reg(16, R.raw.s_dr3, 10, 3)
            reg(17, R.raw.s_pls1, 4, 1)
            reg(18, R.raw.s_pls2, 4, 1)
            reg(19, R.raw.s_pls3, 4, 1)
            reg(20, R.raw.s_pls4, 4, 1)
            reg(21, R.raw.s_per1, 3, 1)
            reg(22, R.raw.s_per2, 3, 1)
            reg(23, R.raw.s_setsv, 3, 1)
            reg(24, R.raw.s_setsvpatr, 3, 1)
            reg(25, R.raw.s_setsvapt, 3, 1)
            reg(26, R.raw.s_axe1, 2, 1)
            reg(27, R.raw.s_axe2, 2, 1)
            reg(28, R.raw.s_grl, 5, 2)
            reg(29, R.raw.s_grbr, 5, 1)
            reg(30, R.raw.s_rip, 5, 1)
            reg(31, R.raw.s_der, 5, 2)
            reg(32, R.raw.s_die1, 5, 1)
            reg(33, R.raw.s_die2, 5, 1)
            reg(34, R.raw.s_die3, 5, 1)
            reg(35, R.raw.s_win2, 30, 3)
            reg(36, R.raw.s_fail1, 30, 3)
            reg(37, R.raw.s_zat, 3, 1)
            reg(38, R.raw.s_zat1, 5, 1)
            reg(39, R.raw.s_zat2, 3, 1)
            reg(40, R.raw.s_zvzr, 3, 2)
            reg(41, R.raw.s_zvzr1, 5, 2)
            reg(42, R.raw.s_zvzr2, 5, 2)
            reg(43, R.raw.s_ognm1, 5, 2)
            reg(44, R.raw.s_ognm2, 5, 1)
            reg(45, R.raw.s_sack, 10, 1)
            reg(46, R.raw.s_zom_at1, 2, 1)
            reg(47, R.raw.s_zom_at2, 2, 1)
            reg(48, R.raw.s_zom_at3, 2, 1)
            reg(49, R.raw.s_zom_at4, 2, 1)
            reg(50, R.raw.s_zom_at5, 2, 1)
            reg(51, R.raw.s_zom_at6, 2, 1)
            reg(52, R.raw.s_zom_at7, 2, 1)
            reg(53, R.raw.s_zom_at8, 2, 1)
            reg(54, R.raw.s_zom_die, 5, 1)
            reg(55, R.raw.s_molotov, 5, 2)
            reg(56, R.raw.s_ogon1, 19, 1)
            reg(57, R.raw.s_ogon2, 18, 1)
            reg(58, R.raw.s_m16, 2, 1)
            reg(59, R.raw.s_wall, 10, 1)
            reg(60, R.raw.s_armor, 3, 1)
            reg(61, R.raw.s_bomb, 30, 3)
            reg(62, R.raw.s_pik1, 3, 1)
            reg(63, R.raw.s_pik2, 3, 1)
            reg(64, R.raw.s_pik4, 3, 1)
            reg(65, R.raw.s_pik3, 3, 1)
            reg(66, R.raw.s_obezvr, 10, 2)
            reg(67, R.raw.s_tlpt, 10, 1)
            reg(68, R.raw.s_car1, 0, 1)
            reg(69, R.raw.s_plane, 0, 1)
            reg(70, R.raw.s_par, 3, 1)
            reg(71, R.raw.s_polet, 0, 1)
            reg(72, R.raw.s_arbalet, 2, 1)
            reg(73, R.raw.s_aug, 2, 1)
            reg(74, R.raw.s_svd, 2, 1)
            reg(75, R.raw.s_knife1, 2, 1)
            reg(76, R.raw.s_knife2, 2, 1)
            reg(77, R.raw.s_knife3, 2, 1)
            reg(78, R.raw.s_deagle, 2, 2)
            reg(79, R.raw.s_glock, 2, 2)
            reg(80, R.raw.s_tap, 2, 2)
            reg(81, R.raw.s_hitunit, 2, 2)
            reg(82, R.raw.s_famas, 2, 2)
            reg(83, R.raw.s_car2, 0, 1)

            tableInitialized = true
        }
    }

    // декодирование wav и mp3 ресурсов в 16-битный моно pcm 44100 гц
    private fun decodeSoundToPcm(context: Context, index: Int, @RawRes resId: Int): ShortArray {
        if (resId == 0) return ShortArray(0)
        try {
            val rawBytes = context.resources.openRawResource(resId).use { it.readBytes() }
            if (rawBytes.size >= 44 &&
                rawBytes[0] == 'R'.code.toByte() &&
                rawBytes[1] == 'I'.code.toByte() &&
                rawBytes[2] == 'F'.code.toByte() &&
                rawBytes[3] == 'F'.code.toByte()
            ) {
                val wav = decodeWavBytes(rawBytes, index)
                if (wav.isNotEmpty()) return wav
            }
        } catch (_: Exception) {
        }
        var decoded = decodeCompressedResource(context, resId, index)
        if (decoded.isEmpty()) {
            SystemClock.sleep(12)
            decoded = decodeCompressedResource(context, resId, index)
        }
        return decoded
    }

    // быстрый парсер wav файлов
    private fun decodeWavBytes(bytes: ByteArray, index: Int): ShortArray {
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12
        var channels = 1
        var sampleRate = SAMPLE_RATE
        var bitsPerSample = 16
        var dataOffset = -1
        var dataSize = 0

        while (pos + 8 <= bytes.size) {
            val chunkId = String(bytes, pos, 4, Charsets.US_ASCII)
            val chunkSize = bb.getInt(pos + 4)
            val bodyPos = pos + 8
            if (chunkId == "fmt " && bodyPos + 16 <= bytes.size) {
                channels = bb.getShort(bodyPos + 2).toInt().coerceAtLeast(1)
                sampleRate = bb.getInt(bodyPos + 4).coerceAtLeast(8000)
                bitsPerSample = bb.getShort(bodyPos + 14).toInt()
            } else if (chunkId == "data") {
                dataOffset = bodyPos
                dataSize = min(chunkSize, bytes.size - bodyPos)
                break
            }
            pos = bodyPos + max(0, chunkSize) + (chunkSize and 1)
        }

        if (dataOffset < 0 || dataSize <= 0) return ShortArray(0)

        val monoSamples: ShortArray
        if (bitsPerSample == 16) {
            val frameSize = 2 * channels
            val frames = dataSize / frameSize
            monoSamples = ShortArray(frames)
            var offset = dataOffset
            for (i in 0 until frames) {
                if (channels == 1) {
                    monoSamples[i] = bb.getShort(offset)
                } else {
                    val l = bb.getShort(offset).toInt()
                    val r = bb.getShort(offset + 2).toInt()
                    monoSamples[i] = ((l + r) shr 1).toShort()
                }
                offset += frameSize
            }
        } else {
            val frameSize = channels
            val frames = dataSize / frameSize
            monoSamples = ShortArray(frames)
            var offset = dataOffset
            for (i in 0 until frames) {
                val u = bytes[offset].toInt() and 0xFF
                monoSamples[i] = ((u - 128) shl 8).toShort()
                offset += frameSize
            }
        }

        return resampleToTargetRate(monoSamples, sampleRate)
    }

    // аппаратно-программное декодирование mp3 через mediacodec
    private fun decodeCompressedResource(context: Context, @RawRes resId: Int, index: Int): ShortArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            val afd = context.resources.openRawResourceFd(resId) ?: return ShortArray(0)
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            if (extractor.trackCount <= 0) return ShortArray(0)
            val format = extractor.getTrackFormat(0)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return ShortArray(0)
            var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                SAMPLE_RATE
            }
            var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                2
            }

            extractor.selectTrack(0)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            var outBuf = ShortArray(65536)
            var outLen = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var idleSpins = 0

            while (!outputDone && idleSpins < 400) {
                var progressed = false
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(15000)
                    if (inIdx >= 0) {
                        progressed = true
                        val inBuffer = codec.getInputBuffer(inIdx)
                        if (inBuffer != null) {
                            val read = extractor.readSampleData(inBuffer, 0)
                            if (read < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, read, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(info, 15000)
                when {
                    outIdx >= 0 -> {
                        progressed = true
                        val outByteBuf = codec.getOutputBuffer(outIdx)
                        if (outByteBuf != null && info.size > 0) {
                            outByteBuf.position(info.offset)
                            outByteBuf.limit(info.offset + info.size)
                            outByteBuf.order(ByteOrder.LITTLE_ENDIAN)
                            val shortBuf = outByteBuf.asShortBuffer()
                            val totalShorts = shortBuf.remaining()
                            val ch = channels.coerceAtLeast(1)
                            val frames = totalShorts / ch

                            if (outLen + frames > outBuf.size) {
                                outBuf = outBuf.copyOf(max(outBuf.size * 2, outLen + frames + 8192))
                            }

                            if (ch == 1) {
                                shortBuf.get(outBuf, outLen, frames)
                                outLen += frames
                            } else {
                                for (f in 0 until frames) {
                                    val l = shortBuf.get().toInt()
                                    val r = shortBuf.get().toInt()
                                    for (skip in 2 until ch) shortBuf.get()
                                    outBuf[outLen++] = ((l + r) shr 1).toShort()
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        progressed = true
                        val newFmt = codec.outputFormat
                        if (newFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = newFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (newFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = newFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                    }
                }

                if (progressed) {
                    idleSpins = 0
                } else {
                    idleSpins++
                }
            }

            val rawMono = outBuf.copyOf(outLen)
            return resampleToTargetRate(rawMono, sampleRate)
        } catch (_: Exception) {
            return ShortArray(0)
        } finally {
            try {
                codec?.stop()
            } catch (_: Exception) {
            }
            try {
                codec?.release()
            } catch (_: Exception) {
            }
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }

    // ресемплинг к 44100 гц
    private fun resampleToTargetRate(input: ShortArray, srcRate: Int): ShortArray {
        if (input.isEmpty() || srcRate == SAMPLE_RATE || srcRate <= 0) return input
        val ratio = srcRate.toDouble() / SAMPLE_RATE.toDouble()
        val outSize = (input.size / ratio).toInt().coerceAtLeast(1)
        val out = ShortArray(outSize)
        val lastIdx = input.size - 1
        for (i in 0 until outSize) {
            val srcPos = i * ratio
            val idx = srcPos.toInt()
            if (idx >= lastIdx) {
                out[i] = input[lastIdx]
            } else {
                val frac = (srcPos - idx).toFloat()
                val s0 = input[idx].toInt()
                val s1 = input[idx + 1].toInt()
                out[i] = (s0 + ((s1 - s0) * frac).toInt()).toShort()
            }
        }
        return out
    }

    // запуск фонового потока программного микшера
    @JvmStatic
    fun startMixerIfNeeded() {
        if (mixerRunning && mixerThread?.isAlive == true) return
        synchronized(soundLock) {
            if (mixerRunning && mixerThread?.isAlive == true) return
            mixerRunning = true
            val t = Thread({ runSoftwareMixerLoop() }, "StrikePcmMixer")
            t.isDaemon = true
            t.priority = Thread.MAX_PRIORITY
            mixerThread = t
            t.start()
        }
    }

    // создание и запуск audiotrack с резервным режимом для бюджетных чипсетов
    private fun createAndStartAudioTrack(trackBufBytes: Int): AudioTrack? {
        return try {
            var track = if (Build.VERSION.SDK_INT >= 26) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(trackBufBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    trackBufBytes,
                    AudioTrack.MODE_STREAM
                )
            }
            if (track.state != AudioTrack.STATE_INITIALIZED && Build.VERSION.SDK_INT >= 26) {
                try {
                    track.release()
                } catch (_: Exception) {
                }
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(max(trackBufBytes, MIX_FRAMES * 4 * 4))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_NONE)
                    .build()
            }
            track.play()
            track
        } catch (_: Exception) {
            null
        }
    }

    // основной цикл микширования 64 голосов в один низколатентный audiotrack
    private fun runSoftwareMixerLoop() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (_: Exception) {
        }

        val minBufBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(MIX_FRAMES * 4)

        val trackBufBytes = max(minBufBytes, MIX_FRAMES * 4 * 2)

        var track = createAndStartAudioTrack(trackBufBytes)
        audioTrack = track
        forceTrackReset = false

        val mixL = IntArray(MIX_FRAMES)
        val mixR = IntArray(MIX_FRAMES)
        val outStereo = ShortArray(MIX_FRAMES * 2)

        while (mixerRunning) {
            if (forceTrackReset || track == null || track.state != AudioTrack.STATE_INITIALIZED || track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                forceTrackReset = false
                try {
                    track?.pause()
                    track?.flush()
                    track?.stop()
                } catch (_: Exception) {
                }
                try {
                    track?.release()
                } catch (_: Exception) {
                }
                track = createAndStartAudioTrack(trackBufBytes)
                audioTrack = track
                if (track == null) {
                    SystemClock.sleep(10)
                    continue
                }
            }

            java.util.Arrays.fill(mixL, 0)
            java.util.Arrays.fill(mixR, 0)
            var anyActive = false

            synchronized(voiceLock) {
                for (v in 0 until MAX_VOICES) {
                    if (!vActive[v]) continue
                    val sndId = vSoundId[v]
                    val pcm = if (sndId in 0 until MAX_SOUNDS) pcmSamples[sndId] else null
                    if (pcm == null || pcm.isEmpty()) {
                        vActive[v] = false
                        continue
                    }

                    val gainL = (vVolL[v] * 28672f).toInt()
                    val gainR = (vVolR[v] * 28672f).toInt()
                    val loop = vLoop[v]
                    val rate = vRate[v]
                    val size = pcm.size

                    if (gainL <= 32 && gainR <= 32 && !loop) {
                        val nextPos = vPos[v] + MIX_FRAMES * rate
                        if (nextPos >= size) {
                            vActive[v] = false
                        } else {
                            vPos[v] = nextPos
                        }
                        continue
                    }

                    anyActive = true

                    if (!loop && rate == 1.0) {
                        var pos = vPos[v].toInt()
                        val count = min(MIX_FRAMES, size - pos)
                        for (i in 0 until count) {
                            val s = pcm[pos + i].toInt()
                            mixL[i] += (s * gainL) shr 15
                            mixR[i] += (s * gainR) shr 15
                        }
                        pos += count
                        if (pos >= size) {
                            vActive[v] = false
                        } else {
                            vPos[v] = pos.toDouble()
                        }
                    } else {
                        var pos = vPos[v]
                        for (i in 0 until MIX_FRAMES) {
                            var idx = pos.toInt()
                            if (idx >= size) {
                                if (loop) {
                                    pos %= size.toDouble()
                                    if (pos < 0.0) pos = 0.0
                                    idx = pos.toInt()
                                } else {
                                    vActive[v] = false
                                    break
                                }
                            }
                            val s = pcm[idx].toInt()
                            mixL[i] += (s * gainL) shr 15
                            mixR[i] += (s * gainR) shr 15
                            pos += rate
                        }
                        vPos[v] = pos
                    }
                }
            }

            if (anyActive) {
                var outIdx = 0
                for (i in 0 until MIX_FRAMES) {
                    outStereo[outIdx++] = softClip(mixL[i])
                    outStereo[outIdx++] = softClip(mixR[i])
                }
            } else {
                java.util.Arrays.fill(outStereo, 0.toShort())
            }

            try {
                val written = track.write(outStereo, 0, outStereo.size)
                if (written < 0) {
                    forceTrackReset = true
                    SystemClock.sleep(4)
                }
            } catch (_: Exception) {
                forceTrackReset = true
                SystemClock.sleep(4)
            }
        }

        try {
            track?.stop()
            track?.release()
        } catch (_: Exception) {
        }
    }

    // мягкий лимитер против перегруза и хрипов при 40+ одновременных звуках
    private fun softClip(sample: Int): Short {
        return when {
            sample in -26000..26000 -> sample.toShort()
            sample > 26000 -> {
                val excess = sample - 26000
                min(32760, 26000 + (excess * 6760) / (excess + 6760)).toShort()
            }
            else -> {
                val excess = -26000 - sample
                max(-32760, -26000 - (excess * 6760) / (excess + 6760)).toShort()
            }
        }
    }

    // параллельная предзагрузка всех 84 звуков при старте приложения
    private suspend fun preloadAllPcmSounds(
        context: Context,
        onProgress: (Float) -> Unit
    ) {
        initSoundTable()
        startMixerIfNeeded()
        val allLoaded = (0 until MAX_SOUNDS).all { pcmSamples[it]?.isNotEmpty() == true }
        if (pcmDecoded && allLoaded) {
            onProgress(1f)
            return
        }

        val appCtx = context.applicationContext ?: context
        val semaphore = Semaphore(3)

        coroutineScope {
            val jobs = (0 until MAX_SOUNDS).map { idx ->
                async(Dispatchers.Default) {
                    semaphore.withPermit {
                        if (pcmSamples[idx] == null || pcmSamples[idx]!!.isEmpty()) {
                            pcmSamples[idx] = decodeSoundToPcm(appCtx, idx, soundResIds[idx])
                        }
                        val done = decodedCount.incrementAndGet().coerceAtMost(MAX_SOUNDS)
                        onProgress(done.toFloat() / MAX_SOUNDS.toFloat())
                    }
                }
            }
            jobs.awaitAll()
        }

        // дозагрузка звуков если какой-то декодер на бюджетном телефоне вернул пустой буфер
        var complete = true
        for (i in 0 until MAX_SOUNDS) {
            if (pcmSamples[i] == null || pcmSamples[i]!!.isEmpty()) {
                val retry = decodeSoundToPcm(appCtx, i, soundResIds[i])
                pcmSamples[i] = retry
                if (retry.isEmpty()) complete = false
            }
        }
        pcmDecoded = complete
    }

    // синхронная проверка и перезапуск аудио-сессии при входе в комнату
    @JvmStatic
    fun ensureSoundsPreloaded(context: Context) {
        initSoundTable()
        synchronized(voiceLock) {
            for (i in 0 until MAX_SOUNDS) {
                nextAllowedPlayMs[i] = 0L
                lastTriggeredSlot[i] = -1
            }
        }
        forceTrackReset = true
        startMixerIfNeeded()
        val hasMissing = !pcmDecoded || (0 until MAX_SOUNDS).any { pcmSamples[it] == null || pcmSamples[it]!!.isEmpty() }
        if (hasMissing) {
            val appCtx = context.applicationContext ?: context
            Thread({
                var complete = true
                for (i in 0 until MAX_SOUNDS) {
                    if (pcmSamples[i] == null || pcmSamples[i]!!.isEmpty()) {
                        val decoded = decodeSoundToPcm(appCtx, i, soundResIds[i])
                        pcmSamples[i] = decoded
                        if (decoded.isEmpty()) complete = false
                    }
                }
                pcmDecoded = complete
            }, "StrikeAudioFallbackLoad").apply {
                isDaemon = true
                priority = Thread.MAX_PRIORITY
                start()
            }
        }
    }

    // воспроизведение звука в 64-канальном микшере с сохранением старой логики периодов speriod
    @JvmStatic
    fun playPcmSound(
        id: Int,
        volL: Float,
        volR: Float,
        priority: Int,
        loop: Boolean,
        rate: Float
    ): Int {
        if (id !in 0 until MAX_SOUNDS) return 0
        startMixerIfNeeded()
        val pcm = pcmSamples[id]
        if (pcm == null || pcm.isEmpty()) return 0

        val clampedL = volL.coerceIn(0f, 1.25f)
        val clampedR = volR.coerceIn(0f, 1.25f)
        if (!loop && clampedL <= 0.004f && clampedR <= 0.004f) return 0

        val now = SystemClock.uptimeMillis()
        val safeRate = rate.coerceIn(0.4f, 2.2f).toDouble()

        synchronized(voiceLock) {
            if (!loop) {
                val allowedAt = nextAllowedPlayMs[id]
                if (now < allowedAt) {
                    val lastSlot = lastTriggeredSlot[id]
                    if (lastSlot in 0 until MAX_VOICES && vActive[lastSlot] && vSoundId[lastSlot] == id) {
                        if (clampedL > vVolL[lastSlot] || clampedR > vVolR[lastSlot]) {
                            vVolL[lastSlot] = max(vVolL[lastSlot], clampedL)
                            vVolR[lastSlot] = max(vVolR[lastSlot], clampedR)
                        }
                        return vStreamId[lastSlot]
                    }
                    return 0
                }

                // расчет периода повтора звуков по старой логике
                val periodTicks = soundPeriods[id]
                val cooldownMs = when {
                    id == 12 || id == 43 || id == 44 || id == 56 || id == 57 -> {
                        periodTicks * 33L
                    }
                    (clampedL + clampedR) > 0.5f -> {
                        periodTicks * 33L
                    }
                    else -> {
                        16L
                    }
                }
                nextAllowedPlayMs[id] = now + cooldownMs
            }

            // поиск свободного канала микшера
            var targetSlot = -1

            if (targetSlot == -1) {
                for (v in 0 until MAX_VOICES) {
                    if (!vActive[v]) {
                        targetSlot = v
                        break
                    }
                }
            }

            // если все 64 канала заняты, вытесняем самый старый тихий звук, не трогая победу/поражение
            if (targetSlot == -1) {
                var worstScore = Float.MAX_VALUE
                for (v in 0 until MAX_VOICES) {
                    if (vLoop[v]) continue
                    val sId = vSoundId[v]
                    if (sId == 35 || sId == 36 || sId == 61) continue
                    val age = (now - vStartMs[v]).coerceAtLeast(0L)
                    val score = vPriority[v] * 10f + max(vVolL[v], vVolR[v]) * 4f - (age * 0.008f)
                    if (score < worstScore) {
                        worstScore = score
                        targetSlot = v
                    }
                }
            }

            if (targetSlot == -1) return 0

            val stream = ++nextStreamSeq
            vActive[targetSlot] = true
            vSoundId[targetSlot] = id
            vStreamId[targetSlot] = stream
            vPos[targetSlot] = 0.0
            vRate[targetSlot] = safeRate
            vVolL[targetSlot] = clampedL
            vVolR[targetSlot] = clampedR
            vPriority[targetSlot] = priority
            vLoop[targetSlot] = loop
            vStartMs[targetSlot] = now
            if (!loop) {
                lastTriggeredSlot[id] = targetSlot
            }
            return stream
        }
    }

    // изменение громкости активного потока
    @JvmStatic
    fun setStreamVolume(streamId: Int, volL: Float, volR: Float) {
        if (streamId <= 0) return
        val clampedL = volL.coerceIn(0f, 1.25f)
        val clampedR = volR.coerceIn(0f, 1.25f)
        synchronized(voiceLock) {
            for (v in 0 until MAX_VOICES) {
                if (vActive[v] && vStreamId[v] == streamId) {
                    vVolL[v] = clampedL
                    vVolR[v] = clampedR
                    break
                }
            }
        }
    }

    // изменение скорости воспроизведения активного потока
    @JvmStatic
    fun setStreamRate(streamId: Int, rate: Float) {
        if (streamId <= 0) return
        val safeRate = rate.coerceIn(0.4f, 2.2f).toDouble()
        synchronized(voiceLock) {
            for (v in 0 until MAX_VOICES) {
                if (vActive[v] && vStreamId[v] == streamId) {
                    vRate[v] = safeRate
                    break
                }
            }
        }
    }

    // остановка конкретного потока
    @JvmStatic
    fun stopStream(streamId: Int) {
        if (streamId <= 0) return
        synchronized(voiceLock) {
            for (v in 0 until MAX_VOICES) {
                if (vActive[v] && vStreamId[v] == streamId) {
                    vActive[v] = false
                    break
                }
            }
        }
    }

    // остановка всех активных потоков кроме победы и поражения
    @JvmStatic
    fun stopAllStreams() {
        synchronized(voiceLock) {
            for (v in 0 until MAX_VOICES) {
                if (vActive[v]) {
                    val sId = vSoundId[v]
                    if (sId != 35 && sId != 36) {
                        vActive[v] = false
                    }
                }
            }
            for (i in 0 until MAX_SOUNDS) {
                if (i != 35 && i != 36) {
                    nextAllowedPlayMs[i] = 0L
                }
            }
        }
    }

    // полная остановка всех звуков включая победу и поражение при выходе из матча
    @JvmStatic
    fun stopAllStreamsForce() {
        synchronized(voiceLock) {
            for (v in 0 until MAX_VOICES) {
                vActive[v] = false
            }
            for (i in 0 until MAX_SOUNDS) {
                nextAllowedPlayMs[i] = 0L
            }
        }
    }

    // геттеры параметров для java
    @JvmStatic
    fun getSoundIds(): IntArray {
        initSoundTable()
        return soundIds
    }

    @JvmStatic
    fun getSoundPeriods(): IntArray {
        initSoundTable()
        return soundPeriods
    }

    @JvmStatic
    fun getSoundPriorities(): IntArray {
        initSoundTable()
        return soundPriorities
    }

    @JvmStatic
    fun getSoundMaxDist(id: Int): Float = HEARING_RADIUS

    suspend fun preload(context: Context) {
        preloadWithProgress(context) { _, _ -> }
    }

    suspend fun preloadWithProgress(context: Context, onProgress: (Float, String) -> Unit) {
        withContext(Dispatchers.IO) {
            onProgress(0.0f, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_initsializatsiya_b7b5))
            initSoundTable()
            startMixerIfNeeded()
            delay(20)
            if (textures == null) {
                onProgress(0.1f, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zagruzka_osnovnyh_tekstur_1_4_df07))
                textures = loadRawBitmap(context, R.raw.map_tex)
            }
            if (upravleniye == null) {
                onProgress(0.3f, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zagruzka_elementov_upravleniya__39cf))
                upravleniye = loadRawBitmap(context, R.raw.rammov_tex)
            }
            if (atlasButtons == null) {
                atlasButtons = loadRawBitmap(context, R.raw.but_tex)
            }
            if (objects == null) {
                onProgress(0.5f, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zagruzka_obektov_i_okruzheniya__d2dc))
                objects = loadRawBitmap(context, R.raw.ob_tex)
            }
            if (unitsAtlas == null) {
                unitsAtlas = loadRawBitmap(context, R.raw.pl_tex)
            }
            if (fireAtlas == null) {
                fireAtlas = loadRawBitmap(context, R.raw.fire_tex)
            }
            if (snarAtlas == null) {
                snarAtlas = loadRawBitmap(context, R.raw.snar_tex)
            }
            if (smkAtlas == null) {
                smkAtlas = loadRawBitmap(context, R.raw.smk_tex)
            }
            if (carsAtlas == null) {
                carsAtlas = loadRawBitmap(context, R.raw.cars_tex)
            }
            if (bugsAtlas == null) {
                bugsAtlas = loadRawBitmap(context, R.raw.bugs_tex)
            }
            if (ptAtlas == null) {
                ptAtlas = loadRawBitmap(context, R.raw.pt_tex)
            }
            if (glAtlas == null) {
                glAtlas = loadRawBitmap(context, R.raw.gl_tex)
            }
            if (markersAtlas == null) {
                markersAtlas = loadRawBitmap(context, R.raw.markers_tex)
            }
            if (fonGame == null) {
                onProgress(0.72f, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zagruzka_fona_4_4_526b))
                fonGame = loadResourceBitmap(context, R.drawable.fonchik)
            }

            // декодирование всех 84 звуков в pcm память
            preloadAllPcmSounds(context) { sndProgress ->
                onProgress(
                    0.72f + sndProgress * 0.27f,
                    skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zagruzka_fona_4_4_526b)
                )
            }

            onProgress(1.0f, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zavershenie_f145))
            delay(20)
        }
    }

    fun getTextures(context: Context): ImageBitmap? {
        if (textures == null) {
            textures = loadRawBitmap(context, R.raw.map_tex)
        }
        return textures
    }

    fun getUpravleniye(context: Context): ImageBitmap? {
        if (upravleniye == null) {
            upravleniye = loadRawBitmap(context, R.raw.rammov_tex)
        }
        return upravleniye
    }

    fun getAtlasButtons(context: Context): ImageBitmap? {
        if (atlasButtons == null) {
            atlasButtons = loadRawBitmap(context, R.raw.but_tex)
        }
        return atlasButtons
    }

    fun getObjects(context: Context): ImageBitmap? {
        if (objects == null) {
            objects = loadRawBitmap(context, R.raw.ob_tex)
        }
        return objects
    }

    fun getUnitsAtlas(context: Context): ImageBitmap? {
        if (unitsAtlas == null) {
            unitsAtlas = loadRawBitmap(context, R.raw.pl_tex)
        }
        return unitsAtlas
    }

    fun getFireAtlas(context: Context): ImageBitmap? {
        if (fireAtlas == null) {
            fireAtlas = loadRawBitmap(context, R.raw.fire_tex)
        }
        return fireAtlas
    }

    fun getSnarAtlas(context: Context): ImageBitmap? {
        if (snarAtlas == null) {
            snarAtlas = loadRawBitmap(context, R.raw.snar_tex)
        }
        return snarAtlas
    }

    fun getSmkAtlas(context: Context): ImageBitmap? {
        if (smkAtlas == null) {
            smkAtlas = loadRawBitmap(context, R.raw.smk_tex)
        }
        return smkAtlas
    }

    fun getCarsAtlas(context: Context): ImageBitmap? {
        if (carsAtlas == null) {
            carsAtlas = loadRawBitmap(context, R.raw.cars_tex)
        }
        return carsAtlas
    }

    fun getBugsAtlas(context: Context): ImageBitmap? {
        if (bugsAtlas == null) {
            bugsAtlas = loadRawBitmap(context, R.raw.bugs_tex)
        }
        return bugsAtlas
    }

    fun getPtAtlas(context: Context): ImageBitmap? {
        if (ptAtlas == null) {
            ptAtlas = loadRawBitmap(context, R.raw.pt_tex)
        }
        return ptAtlas
    }

    fun getGlAtlas(context: Context): ImageBitmap? {
        if (glAtlas == null) {
            glAtlas = loadRawBitmap(context, R.raw.gl_tex)
        }
        return glAtlas
    }

    fun getMarkersAtlas(context: Context): ImageBitmap? {
        if (markersAtlas == null) {
            markersAtlas = loadRawBitmap(context, R.raw.markers_tex)
        }
        return markersAtlas
    }

    fun getFonGame(context: Context): ImageBitmap? {
        if (fonGame == null) {
            fonGame = loadResourceBitmap(context, R.drawable.fonchik)
        }
        return fonGame
    }

    private fun loadRawBitmap(context: Context, @RawRes rawId: Int): ImageBitmap? {
        return loadResourceBitmap(context, rawId)
    }

    private fun loadResourceBitmap(context: Context, resId: Int): ImageBitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inScaled = false }
            BitmapFactory.decodeResource(context.resources, resId, options)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
