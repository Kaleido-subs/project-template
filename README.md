# The Kaleido SubKt Project template

[![SubKt Compatibility](https://img.shields.io/badge/SubKt-0.1.27-blue)](https://github.com/LightArrowsEXE/SubKt)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

Clone this repo
and copy all the files over
when making a new fansubbing project.

This template is designed to streamline the process
of creating and maintaining subtitles for [Kaleido-subs](https://github.com/Kaleido-subs) projects.

## Dependencies

- [SubKt](https://github.com/Myaamori/SubKt)
- [mkvmerge](https://mkvtoolnix.download/downloads.html)
- [JDK SE 14](https://www.oracle.com/java/technologies/javase/jdk14-archive-downloads.html),
  [15](https://www.oracle.com/java/technologies/javase/jdk15-archive-downloads.html),
  or [16](https://www.oracle.com/java/technologies/javase/jdk16-archive-downloads.html)

> [!IMPORTANT]
> Ensure all tools are properly added to your system's PATH:
>
> - Windows: Edit System Environment Variables → Path → Add the installation directories
> - Unix: Add export PATH="$PATH:/path/to/tool" to your ~/.bashrc or ~/.zshrc

> [!CAUTION]
> Currently, only JDK 14, and 15 appear to work out-of-the-box with SubKt, but support has been added to this template for 16 too.
> Please ensure Gradle uses the correct version if you run into any issues.

## Directory Structure

```
project-root/
├── build.gradle.kts                      # SubKt Gradle build script
├── sub.properties                        # Project configuration
├── common/                               # Shared resources
│   ├── warning.ass                       # Optional player warning
│   └── fonts/                            # Common fonts
├── 01/                                   # Episode directory
│   ├── NewShow - 01 - (Premux) [ABCDEF01].mkv
│   ├── NewShow - 01 - Dialogue.ass
│   ├── NewShow - 01 - TS.ass
│   ├── NewShow - 01 - TS (Author).ass    # Multiple TS allowed
│   ├── NewShow - 01 - INS (OP).ass       # Optional
│   ├── NewShow - 01 - INS (ED).ass       # Optional
│   ├── NewShow - 01 - Extra.ass          # Optional
│   └── fonts/                            # Episode-specific fonts
├── NCED1/                                # NCED1 directory
│   ├── NewShow - NCED1 - Lyrics.ass
│   ├── NewShow - NCED1 - TS.ass          # Optional
│   └── fonts/                            # NCED-specific fonts
├── NCOP1/                                # NCOP1 directory
│   ├── NewShow - NCOP1 - Lyrics.ass
│   ├── NewShow - NCOP1 - TS.ass          # Optional
│   └── fonts/                            # NCOP-specific fonts
└── build/                                # Generated output
    └── ...                               # Episode build artifacts
```

### File Naming Requirements

#### Episode files

- **Video files:** `ShowName - XX - (Premux) [CRC32].mkv`
- **Dialogue:** `ShowName - XX - Dialogue.ass`
- **Typesetting:** `ShowName - XX - TS.ass` or `ShowName - XX - TS (Author).ass`
- **Insert Songs:** `ShowName - XX - INS (Song Name).ass`
- **Extra:** `ShowName - XX - Extra.ass`

> [!NOTE]
>
> - Replace `XX` with the episode number (zero-padded)
> - CRC32 in the premux file is optional but recommended
> - Author tags in TS files should match contributor names

#### NCOP/NCED files

- `ShowName - XX.ass`

> [!NOTE]
>
> - Replace `XX` with the OP/ED number
> - It accepts any number of NCOP/NCED files, and they will all be merged together before being synced.

**Example of NCOP1 with a lyrics file and a TS file:**

- `NCOP1/NewShow - NCOP1 - Lyrics.ass`
- `NCOP1/NewShow - NCOP1 - TS.ass`

The following is also valid:

- `NCOP1/NewShow - NCOP1.ass`
- `NCOP1/NewShow - NCOP1 - TS.ass`

Only one file may have `opsync` or `edsync` in the effect field.
It's highly recommended to set it in the _lyrics_ file.

## Getting Started

### 1. Project Setup

1. **Use the Template Repository:**

   Click the following button in the top-right of the [project-template](https://github.com/Kaleido-subs/project-template) GitHub repo.

   ![Github "Use this template" button](https://i.imgur.com/zT0SLVM.png)

2. **Set up project files:**

   The project file should be updated on a per-project basis.
   This may also include the types of expected files,
   as well as metadata.

   For a fairly regular project,
   set up the following files:

   - Set up the `sub.properties` file.
     - Set the name of your group and the show.
     - Set the format of the video and audio.
     - Set the number of episodes, as well as any specials.
     - Set the episode ranges for the OP and EDs.
       If no episodes are set, expect it in every episode.
       If an episode has no OP or ED set, it will not be muxed.
   - (Optional) Place any common subtitle files or other resources in the `common/` directory.
   - Add your dialogue and chapters to the episode's dialogue subtitle file.
   - Add your typesetting to the episode's TS subtitle file.
   - (Optional) Set up INS and Extra subtitle files.
   - Collect all the fonts and put them in the `episode/fonts/` directory (i.e. `01/fonts/`).

3. **Build project:**

   Use the Gradle wrapper to build the project:

   ```sh
   # Unix
   ./gradlew chapters.01    # Generate chapters
   ./gradlew merge.01      # Merge subtitle files
   ./gradlew cleanmerge.01 # Clean merged subtitles
   ./gradlew mux.01        # Create final MKV

   # Windows
   gradlew.bat chapters.01
   gradlew.bat merge.01
   gradlew.bat cleanmerge.01
   gradlew.bat mux.01
   ```

### Troubleshooting

1. **Merge Failures:**

   - Check for syntax errors in ASS files
   - Verify font names match between files
   - Ensure no UTF-16 BOM in subtitle files

2. **Mux Errors:**

   - Verify mkvmerge is in PATH
   - Check premux file exists and is readable
   - Ensure all referenced fonts exist

### Scripts & Hooks

This template comes with multiple scripts and hooks.

#### no-common-fonts

A pre-commit hook
that checks if any pushed fonts
match any fonts in `common/fonts`.
This is to prevent users from pushing duplicate fonts
and potentially overriding dialogue fonts
with different versions of the font.

#### project_setup

A simple shell script that updates the local git config
to filter commits using `scripts/clean_project_garbage.sh`.
This will filter out Aegisub project garbage metadata
so users won't keep overriding this with their own premux locations
and other local metadata.

#### blank_dialogue

Replaces the given .ass subtitle file with:

- Custom metadata and two predefined styles (Default, Alt)
- A set of fixed chapter marker and comment events
- All Dialogue lines retained structurally but emptied of text

The original file is overwritten in-place.
This is used to blank out for example official scripts
while preserving their timing
to use for original translations.

## Contributing

Spot an issue in the build scripts?
Have any suggestions?
Contributions are welcome!<br>
Please fork the repository
and create a pull request with your changes,
or create an issue.

For further support,
please contact either LightArrowsEXE or petzku
on the [Kaleido discord server](https://discord.gg/dk7aadV).
