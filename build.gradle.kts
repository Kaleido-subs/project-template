import myaa.subkt.ass.*
import myaa.subkt.tasks.*
import myaa.subkt.tasks.Mux.*
import myaa.subkt.tasks.Nyaa.*
import java.awt.Color
import java.time.*

plugins {
    id("myaa.subkt")
}

// Check whether a string contains parts of a ktemplate
fun String.isKaraTemplate(): Boolean {
    return this.startsWith("code") || this.startsWith("template") || this.startsWith("mixin")
}

// Check whether a line is part of a ktemplate
fun EventLine.isKaraTemplate(): Boolean {
    return this.comment && this.effect.isKaraTemplate()
}

subs {
    readProperties("sub.properties")
    episodes(getList("episodes"))

    // Merge all the individual script components
    merge {
        from(get("dialogue"))

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

        fromIfPresent(get("extra"), ignoreMissingFiles = true)
        fromIfPresent(getList("INS"), ignoreMissingFiles = true)
        fromIfPresent(getList("TS"), ignoreMissingFiles = true)

        includeExtraData(false)
        includeProjectGarbage(false)

        scriptInfo {
            title = get("group").get()
            scaledBorderAndShadow = true
            wrapStyle = WrapStyle.NO_WRAP
        }
    }

    // Remove ktemplate lines from the final output
    val cleanmerge by task<ASS> {
        from(merge.item())
        ass { events.lines.removeIf { it.isKaraTemplate() } }
    }

    // Generate chapters from dialogue file
    chapters {
        from(get("chapters"))
        chapterMarker("chapter")
    }

    // Run swapper script for honorifics
    swap { from(cleanmerge.item()) }

    // Finally, mux
    mux {
        title(get("title"))

        skipUnusedFonts(true)

        // Optionally specify mkvmerge version to use
        if (propertyExists("mkvmerge")) {
            mkvmerge(get("mkvmerge"))
        }

        from(get("premux")) {
            tracks {
                include(track.type == TrackType.VIDEO || track.type == TrackType.AUDIO)
            }

            video {
                lang("jpn")
                name(get("group").get())
                default(true)
            }
            audio {
                lang("jpn")
                name(get("group").get())
                default(true)
            }
            includeChapters(false)
            attachments { include(false) }
        }

        from(cleanmerge.item()) {
            tracks {
                lang("eng")
                name(get("group_reg"))
                default(true)
                forced(false)
                compression(CompressionType.ZLIB)
            }
        }

        from(swap.item()) {
            subtitles {
                lang("enm")
                name(get("group_hono"))
                default(true)
                forced(false)
                compression(CompressionType.ZLIB)
            }
        }

        chapters(chapters.item()) { lang("eng") }

        skipUnusedFonts(true)

        attach(get("common_fonts")) {
            includeExtensions("ttf", "otf", "ttc")
        }

        attach(get("fonts")) {
            includeExtensions("ttf", "otf", "ttc")
        }

        if (propertyExists("OP")) {
            attach(get("opfonts")) {
                includeExtensions("ttf", "otf", "ttc")
            }
        }

        if (propertyExists("ED")) {
            attach(get("edfonts")) {
                includeExtensions("ttf", "otf", "ttc")
            }
        }

        out(get("muxout"))
    }

    // =================================================================================================================
    // NCOP/EDs
    tasks(getList("ncs")) {
        merge {
            from(get("ncsubs"))

            includeExtraData(false)
            includeProjectGarbage(false)

            scriptInfo {
                title = get("group").get()
                originalScript = get("group").get()
                scaledBorderAndShadow = true
            }
        }

        val cleanncmerge by task<ASS> {
            from(merge.item())
            ass { events.lines.removeIf { it.isKaraTemplate() } }
        }

        chapters {
            from(cleanncmerge.item())
            chapterMarker("ncchapter")
        }

        mux {
            title(get("title"))

            from(get("ncpremux")) {
                tracks {
                    include(track.type == TrackType.VIDEO || track.type == TrackType.AUDIO)
                }

                video {
                    lang("jpn")
                    name(get("group").get())
                    default(true)
                }
                audio(0) {
                    lang("jpn")
                    name("Opus 5.1 @ 320kb/s")
                    default(true)
                }
                audio(1) {
                    lang("jpn")
                    name("Opus 2.0 @ 192kb/s")
                    default(true)
                }
                includeChapters(false)
                attachments { include(false) }
            }

            from(cleanncmerge.item()) {
                tracks {
                    lang("eng")
                    name(get("group"))
                    default(true)
                    forced(false)
                    compression(CompressionType.ZLIB)
                }
            }

            chapters(chapters.item()) { lang("eng") }

            skipUnusedFonts(true)

            attach(get("ncfonts")) {
                includeExtensions("ttf", "otf", "ttc")
            }

            attach(get("common_fonts")) {
                includeExtensions("ttf", "otf", "ttc")
            }

            out(get("ncmuxout"))
        }
    }
}
