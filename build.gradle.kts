import myaa.subkt.ass.*
import myaa.subkt.tasks.*
import myaa.subkt.tasks.Mux.*
import myaa.subkt.tasks.Nyaa.*
import myaa.subkt.tasks.utils.*
import java.awt.Color
import java.io.File
import java.time.*

plugins {
    id("myaa.subkt")
}

// Retrieve the script's playRes values
fun ASSFile.getPlayRes(): Pair<Int?, Int?> {
    return this.scriptInfo.playResX to this.scriptInfo.playResY
}

fun Provider<String>.getPlayRes(): Pair<Int?, Int?> {
    return ASSFile(file(this.get())).getPlayRes()
}

// Check whether a string contains parts of a ktemplate
fun String.isKaraTemplate(): Boolean {
    return this.startsWith("code") || this.startsWith("template") || this.startsWith("mixin")
}

// Check whether a line is part of a ktemplate
fun EventLine.isKaraTemplate(): Boolean {
    return this.effect.isKaraTemplate()
}

// Check if a line is entirely blank (no text, actor, or effect)
fun EventLine.isBlank(): Boolean {
    return this.text.isEmpty() && this.actor.isEmpty() && this.effect.isEmpty()
}

// Check if a line has a negative duration. These will never render anyway and cause the mux to hang (subkt bug)
fun EventLine.isNegativeDuration(): Boolean {
    return this.end < this.start
}

// Check if a line has a zero duration.
fun EventLine.isZeroDuration(): Boolean {
    return this.end == this.start
}

// Check if a line is dialogue
fun EventLine.isDialogue(): Boolean {
    return this.style.matches(Regex("Main|Default|Alt"))
}

// Check if premux has multiple audio tracks with English as second track
fun includeForcedTrack(sortedTracks: List<MkvTrack>): Boolean {
    val audioTracks = sortedTracks.filter { it.type == "audio" }
    val hasMultipleAudio = audioTracks.size > 1
    val secondTrackIsEnglish = audioTracks.getOrNull(1)?.properties?.language == "eng"

    return hasMultipleAudio && secondTrackIsEnglish
}

// Get MkvInfo and return tracks sorted by type
fun getMkvInfoTracks(mkvPath: String): List<MkvTrack> {
    val mkvFile = File(projectDir, mkvPath)

    if (!mkvFile.exists()) {
        return emptyList()
    }

    val mkvFileInfo = getMkvInfo(mkvFile)
    val tracks = mkvFileInfo.tracks ?: emptyList()

    // Sort tracks by type (video first, then audio, then subtitles)
    val sortedTracks = tracks.sortedWith(compareBy<MkvTrack> { track ->
        when (track.type) {
            "video" -> 0
            "audio" -> 1
            "subtitles" -> 2
            else -> 3
        }
    }.thenBy { it.id })

    return sortedTracks
}

// Print the tracks information in a nice table
fun printMkvInfoTracks(outputPath: String) {
    val tracks = getMkvInfoTracks(outputPath)

    val fileName = File(outputPath).name
    val num_dashes = fileName.length + 15

    println("\n".repeat(2))
    println("+${"-".repeat(num_dashes)}")
    println("| Tracks for $fileName")

    tracks.forEach { track ->
        println("+${"-".repeat(num_dashes + 2)}")
        println("| Track #${track.id}  [${track.type}]  (${track.codec})")
        track.properties?.let { props ->
            val infoLines = mutableListOf<String>()
            infoLines += "Language         : ${props.language ?: "und"}"
            infoLines += "Name             : ${props.track_name ?: "N/A"}"
            infoLines += "Default          : ${props.default_track ?: false}"
            infoLines += "Forced           : ${props.forced_track ?: false}"

            if (props.original_language != null) {
                infoLines += "Original language: ${props.original_language}"
            }
            if (props.hearing_impaired_track != null) {
                infoLines += "Hearing impaired : ${props.hearing_impaired_track}"
            }

            when (track.type) {
                "video" -> {
                    infoLines += "Dimensions       : ${props.pixel_dimensions ?: "N/A"}"
                    infoLines += "Display          : ${props.display_dimensions ?: "N/A"}"
                }
                "audio" -> {
                    infoLines += "Channels         : ${props.audio_channels ?: "N/A"}"
                    infoLines += "Sample Rate      : ${props.audio_sampling_frequency ?: "N/A"}"
                    infoLines += "Bits per Sample  : ${props.audio_bits_per_sample ?: "N/A"}"
                }
            }
            infoLines.forEach { println("| $it") }
        }
    }

    println("+${"-".repeat(num_dashes + 1)}")
}


subs {
    readProperties("sub.properties", "../sekrit.properties")
    episodes(getList("episodes"))

    // default to TV
    // allow specifying using -Prelease=foo
    release(arg("release") ?: "default")

    val layerDialogue by task<ASS> {
        from(get("dialogue"))
        ass {
            events.lines.filter { !it.comment || it.effect == "***" }.forEach { it.layer += 69 }
        }
    }

    // Pre-merge OP and ED files so multiple components can be merged at once easily
    val premerge_op by task<Merge> {
        fromIfPresent(getList("OP"), ignoreMissingFiles = true)
    }

    val premerge_ed by task<Merge> {
        fromIfPresent(getList("ED"), ignoreMissingFiles = true)
    }

    // Merge all the individual script components
    merge {
        from(layerDialogue.item())

        fromIfPresent(getList("extra"), ignoreMissingFiles = true)
        fromIfPresent(getList("TS"), ignoreMissingFiles = true)

        if (propertyExists("OP")) {
            from(premerge_op.item()) {
                syncSourceLine("sync")
                syncTargetLine("opsync")
            }
        }

        if (propertyExists("ED")) {
            from(premerge_ed.item()) {
                syncSourceLine("sync")
                syncTargetLine("edsync")
            }
        }

        fromIfPresent(getList("INS"), ignoreMissingFiles = true) {
            incrementLayer(20)
        }

        fromIfPresent(get("render_warning"), ignoreMissingFiles = true)

        includeExtraData(false)
        includeProjectGarbage(false)

        val (resX, resY) = get("dialogue").getPlayRes()

        scriptInfo {
            title = get("group").get()
            scaledBorderAndShadow = true
            wrapStyle = WrapStyle.NO_WRAP
            values["LayoutResX"] = resX ?: -1
            values["LayoutResY"] = resY ?: -1
        }
    }

    // Remove ktemplate and empty lines from the final output
    val cleanmerge by task<ASS> {
        from(merge.item())
        // ass { events.lines.removeIf { it.isKaraTemplate() or it.isBlank() or it.isNegativeDuration() } }
        ass { events.lines.removeIf { it.isBlank() or it.isNegativeDuration() } }
    }

    // Generate chapters from dialogue file
    chapters {
        from(get("chapters"))
        chapterMarker("chapter")
    }

    // Run swapper script for honorifics and other swaps (keys: "*" and "***").
    swap {
        from(cleanmerge.item())

        styles(Regex(".*"))

        delimiter("*")
        lineMarker("***")
    }

    // Remove dialogue lines from forced Signs & Song tracks.
    val strip_dialogue by task<ASS> {
        from(cleanmerge.item())
        ass {events.lines.removeIf { it.isDialogue() } }
    }

    // Merge the forced track (if present) with the stripped dialogue.
    val forced_merge by task<Merge> {
        from(strip_dialogue.item())
        fromIfPresent(getList("forced"), ignoreMissingFiles = true)
    }

    // Run swaps for the forced track (keys: "/" and "///").
    val forced_swap by task<Swap> {
        from(forced_merge.item())

        styles(Regex(".*"))

        delimiter("/")
        lineMarker("///")
    }

    // Finally, mux following the conventions listed here: https://thewiki.moe/advanced/muxing/#correct-tagging
    mux {
        // TODO: Add automated format extraction from premux file and update title
        title(get("title"))

        // Optionally specify mkvmerge version to use
        if (propertyExists("mkvmerge")) {
            mkvmerge(get("mkvmerge"))
        }

        val premuxTracks = getMkvInfoTracks(get("premux").get())

        from(get("premux")) {
            tracks {
                include(track.type == TrackType.VIDEO || track.type == TrackType.AUDIO)
            }

            includeChapters(false)
            attachments { include(false) }
        }

        from(cleanmerge.item()) {
            tracks {
                lang("eng")
                name(get("strack_reg"))
                default(true)
                forced(false)
                compression(CompressionType.ZLIB)
            }
        }

        from(swap.item()) {
            subtitles {
                lang("enm")
                name(get("strack_hono"))
                default(true)
                forced(false)
                compression(CompressionType.ZLIB)
            }
        }

        if (includeForcedTrack(premuxTracks.filter { it.type == "audio" })) {
            from(forced_swap.item()) {
                tracks {
                    lang("eng")
                    name(get("strack_ss"))
                    default(false)
                    forced(true)
                    compression(CompressionType.ZLIB)
                }
            }
        }

        chapters(chapters.item()) { lang("eng") }

        // Fonts handling
        skipUnusedFonts(true)

        attach(get("common_fonts")) {
            includeExtensions("ttf", "otf")
        }

        attach(get("fonts")) {
            includeExtensions("ttf", "otf")
        }

        // Get OP/ED fonts if necessary
        if (propertyExists("OP")) {
            attach(get("opfonts")) {
                includeExtensions("ttf", "otf")
            }
        }

        if (propertyExists("ED")) {
            attach(get("edfonts")) {
                includeExtensions("ttf", "otf")
            }
        }

        out(get("muxout"))

        doLast {
            printMkvInfoTracks(get("muxout").get())
        }
    }

    // =================================================================================================================
    // NCOP/EDs
    tasks(getList("ncs")) {
        merge {
            fromIfPresent(getList("ncsubs"), ignoreMissingFiles = true)

            includeExtraData(false)
            includeProjectGarbage(false)

            val ncsubPath = getList("ncsubs").orNull?.firstOrNull()
            val (resX, resY) = ncsubPath?.let { ASSFile(File(it)).getPlayRes() } ?: (null to null)

            scriptInfo {
                title = get("group").get()
                scaledBorderAndShadow = true
                wrapStyle = WrapStyle.NO_WRAP
                values["LayoutResX"] = resX ?: -1
                values["LayoutResY"] = resY ?: -1
            }
        }

        // Remove ktemplate and empty lines from the final output
        val cleanmerge by task<ASS> {
            from(merge.item())
            // ass { events.lines.removeIf { it.isKaraTemplate() or it.isBlank() or it.isNegativeDuration() } }
            ass { events.lines.removeIf { it.isBlank() or it.isNegativeDuration() } }
        }

        // Run swapper script for honorifics and other swaps (keys: "*" and "***").
        swap {
            from(cleanmerge.item())

            styles(Regex(".*"))

            delimiter("*")
            lineMarker("***")
        }

        // Remove dialogue lines from forced Signs & Song tracks.
        val strip_dialogue by task<ASS> {
            from(cleanmerge.item())
            ass {events.lines.removeIf { it.isDialogue() } }
        }

        // Merge the forced track (if present) with the stripped dialogue.
        val forced_merge by task<Merge> {
            from(strip_dialogue.item())
            fromIfPresent(getList("forced"), ignoreMissingFiles = true)
        }

        // Run swaps for the forced track (keys: "/" and "///").
        val forced_swap by task<Swap> {
            from(forced_merge.item())

            styles(Regex(".*"))

            delimiter("/")
            lineMarker("///")
        }

        mux {
            // TODO: Add automated format extraction from premux file and update title
            title(get("title"))

            if (propertyExists("mkvmerge")) {
                mkvmerge(get("mkvmerge"))
            }

            val premuxTracks = getMkvInfoTracks(get("ncpremux").get())

            from(get("ncpremux")) {
                tracks {
                    include(track.type == TrackType.VIDEO || track.type == TrackType.AUDIO)
                }

                includeChapters(false)
                attachments { include(false) }
            }

            from(cleanmerge.item()) {
                tracks {
                    lang("eng")
                    name(get("strack_reg"))
                    default(true)
                    forced(false)
                    compression(CompressionType.ZLIB)
                }
            }

            // TODO: Add automated comparison that actually works to see if a swap is required.
            from(swap.item()) {
                subtitles {
                    lang("enm")
                    name(get("strack_hono"))
                    default(true)
                    forced(false)
                    compression(CompressionType.ZLIB)
                }
            }

            if (includeForcedTrack(premuxTracks.filter { it.type == "audio" })) {
                from(forced_swap.item()) {
                    tracks {
                        lang("eng")
                        name(get("strack_ss"))
                        default(false)
                        forced(true)
                        compression(CompressionType.ZLIB)
                    }
                }
            }

            skipUnusedFonts(true)

            attach(get("ncfonts")) {
                includeExtensions("ttf", "otf")
            }

            attach(get("common_fonts")) {
                includeExtensions("ttf", "otf")
            }

            out(get("ncmuxout"))

            doLast {
                printMkvInfoTracks(get("ncmuxout").get())
            }
        }
    }
}
