import myaa.subkt.ass.*
import myaa.subkt.tasks.*
import myaa.subkt.tasks.Mux.*
import myaa.subkt.tasks.Nyaa.*
import myaa.subkt.tasks.utils.*
import java.awt.Color
import java.time.*

plugins {
    id("myaa.subkt")
}

// Retrieve the script's playRes values
fun ASSFile.getPlayRes(): Pair<Int?, Int?> {
    return this.scriptInfo.playResX to this.scriptInfo.playResY
}

// Check if the original ASS file and swapped ASS file are not equal
fun assSwapNotEqual(ass1: ASS, ass2: Swap): Boolean {
    val ass1File = ASSFile(ass1.out.singleFile)
    val ass2File = ASSFile(ass2.out.singleFile)

    return ass1File.events.lines != ass2File.events.lines
}

// Get the playRes values from the ASS file
fun Provider<String>.getPlayRes(): Pair<Int?, Int?> {
    return ASSFile(File(this.get())).getPlayRes()
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


subs {
    readProperties("sub.properties", "../sekrit.properties")
    episodes(getList("episodes"))

    val layerDialogue by task<ASS> {
        from(get("dialogue"))
        ass {
            // could just use incrementLayer in merge.from,
            // but apparently comments having non-zero layer is ugly, so no
            events.lines.filter { !it.comment || it.effect == "***" }.forEach { it.layer += 69 }
        }
    }

    // Merge all the individual script components
    merge {
        from(layerDialogue.item())

        if (propertyExists("OP")) {
            from(get("OP")) {
                syncSourceLine("sync")
                syncTargetLine("opsync")
            }
        }

        if (propertyExists("ED")) {
            from(get("ED")) {
                syncSourceLine("sync")
                syncTargetLine("edsync")
            }
        }

        fromIfPresent(getList("extra"), ignoreMissingFiles = true)
        fromIfPresent(getList("TS"), ignoreMissingFiles = true)

        fromIfPresent(getList("INS"), ignoreMissingFiles = true) {
            incrementLayer(20)
        }

        fromIfPresent(get("render_warning"), ignoreMissingFiles = true)

        includeExtraData(false)
        includeProjectGarbage(false)

        scriptInfo {
            title = get("group").get()
            scaledBorderAndShadow = true
            wrapStyle = WrapStyle.NO_WRAP
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
        title(get("title"))

        // Optionally specify mkvmerge version to use
        if (propertyExists("mkvmerge")) {
            mkvmerge(get("mkvmerge"))
        }

        from(get("premux")) {
            tracks {
                include(track.type == TrackType.VIDEO || track.type == TrackType.AUDIO)
            }

            video {
                default(true)
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
    }

    // =================================================================================================================
    // NCOP/EDs
    tasks(getList("ncs")) {
        merge {
            from(get("ncsubs"))

            fromIfPresent(get("ncts"))

            includeExtraData(false)
            includeProjectGarbage(false)

            scriptInfo {
                title = get("group").get()
                scaledBorderAndShadow = true
                wrapStyle = WrapStyle.NO_WRAP
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
            title(get("title"))

            from(get("ncpremux")) {
                tracks {
                    include(track.type == TrackType.VIDEO || track.type == TrackType.AUDIO)
                }

                video {
                    default(true)
                }

                includeChapters(false)
                attachments { include(false) }
            }

            from(cleanncmerge.item()) {
                tracks {
                    lang("eng")
                    name(get("strack_reg"))
                    default(true)
                    forced(false)
                    compression(CompressionType.ZLIB)
                }
            }

            if (assSwapNotEqual(cleanmerge.item().get(), swap.item().get())) {
                from(swap.item()) {
                    subtitles {
                        lang("enm")
                        name(get("strack_hono"))
                        default(true)
                        forced(false)
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
        }
    }

    // Upload files to sneak-peek Plex server
    tasks(getList("episodes")) {
        fun FTP.configure() {
            host(get("ftp_host"))
            port(getAs<Int>("ftp_port"))

            username(get("ftp_user"))
            password(get("ftp_pass"))

            overwriteIf(OverwriteStrategy.ALWAYS)
        }

        val PlexUpload by task<FTP> {
            from(mux.item())
            configure()

            // TODO: Figure out why this is not working
            into(getRaw("ftp_filedir") + "/" + get("plexpath").get())
        }
    }
}
