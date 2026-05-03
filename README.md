# The Kaleido [SubKt](https://github.com/Myaamori/SubKt) Project template

[![SubKt Compatibility](https://img.shields.io/badge/SubKt-0.1.27-blue)](https://github.com/LightArrowsEXE/SubKt)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

Clone this repo
and copy all the files over
when making a new fansubbing project.

This template is designed to streamline the process
of creating and maintaining subtitles for [Kaleido-subs](https://github.com/Kaleido-subs) projects,
but is available publicly for other aspiring fansub projects to get started.

## Dependencies

- [mkvmerge](https://mkvtoolnix.download/downloads.html)
- [JDK SE 14](https://www.oracle.com/java/technologies/javase/jdk14-archive-downloads.html)

> [!IMPORTANT]
> Ensure all tools are properly added to your system's PATH:
>
> - Windows: Edit System Environment Variables > Path > Add the installation directories
> - Unix: Add export PATH="$PATH:/path/to/tool" to your `~/.bashrc` or `~/.zshrc`

> [!CAUTION]
> Currently, only JDK 14 appears to work out-of-the-box with SubKt (namely, for its ttorrent support).<br>
> Please ensure Gradle uses the correct version if you run into any issues.

## Directory Structure

```sh
project-root/
├── build.gradle.kts                                  # SubKt Gradle build script
├── sub.properties                                    # Project configuration
│
├── common/                                           # Shared resources between all episodes
│   ├── warning.ass                                   # Optional warning that displays in unsupported players
│   └── fonts/                                        # Common fonts (e.g. dialogue fonts)
│
├── 01/                                               # Episode directory
│   ├── NewShow - S01E01 - (Premux) [ABCDEF01].mkv
│   ├── NewShow - S01E01 - Dialogue.ass
│   ├── NewShow - S01E01 - TS.ass
│   ├── NewShow - S01E01 - TS (Author).ass            # Multiple TS allowed
│   ├── NewShow - S01E01 - INS (OP).ass               # Optional
│   ├── NewShow - S01E01 - INS (ED).ass               # Optional
│   ├── NewShow - S01E01 - Extra.ass                  # Optional
│   └── fonts/                                        # Episode-specific fonts
│                                                     # NC directories
├── NCED1/
│   ├── NewShow - NCED1 - Lyrics.ass
│   ├── NewShow - NCED1 - TS.ass                      # Optional
│   └── fonts/
├── NCOP1/
│   ├── NewShow - NCOP1 - Lyrics.ass
│   ├── NewShow - NCOP1 - TS.ass                      # Optional
│   └── fonts/
│
└── build/                                            # Episode build artifacts
    └── ...
```

### File Naming Requirements

#### Episode files

- **Video files:** `ShowName - SxxEyy - (Premux) [CRC32].mkv`
- **Dialogue:** `ShowName - SxxEyy - Dialogue.ass`
- **Typesetting:** `ShowName - SxxEyy - TS.ass` or `ShowName - XX - TS (Author).ass`
- **Insert Songs:** `ShowName - SxxEyy - INS (Song Name).ass`
- **Extra:** `ShowName - SxxEyy - Extra.ass`

> [!NOTE]
>
> - Replace `xx` with the season number (zero-padded) and `yy` with the episode number (zero-padded).<br>
> Refer to https://www.thetvdb.com/ for SxxEyy keys if necessary.
> - CRC32 in the premux file is optional, but recommended.

#### NCOP/NCED files

- `ShowName - NCxx - Lyrics.ass`

> [!NOTE]
>
> - Replace `xx` with the OP/ED number
> - It accepts any number of NCOP/NCED files, and they will all be merged together before being synced to the episode.

**Example of NCOP1 with a lyrics file and a TS file:**

- `NCOP1/NewShow - NCOP1 - Lyrics.ass`
- `NCOP1/NewShow - NCOP1 - TS.ass`

The following combination is also valid:

- `NCOP1/NewShow - NCOP1.ass`
- `NCOP1/NewShow - NCOP1 - TS.ass`

Only one file may have `sync` in the effect field.
It's highly recommended to set it in the `lyrics` file.

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

   > [!CAUTION]
   >
   > This build script will automatically add a Signs & Songs track if, and only if, the `format` key contains "Dual Audio".
   > Make sure to double-check your properties where necessary, as well as your muxed file to make sure it doesn't create one where it shouldn't!

3. **Build project:**

   Use the Gradle wrapper to run tasks to build the project:

   ```sh
   # Unix
   ./gradlew chapters.01      # Generate chapters from dialogue file
   ./gradlew merge.01         # Merge all subtitle files
   ./gradlew cleanmerge.01    # Clean merged subtitles
   ./gradlew mux.01           # Create final MKV
   ./gradlew torrent.01       # Create a torrent file for the final MKV
   ./gradlew nyaa.01          # Upload the torrent file to Nyaa (requires a `sekrit.properties` file with authorization details)

   # Windows
   gradlew.bat chapters.01    # Generate chapters from dialogue file
   gradlew.bat merge.01       # Merge all subtitle files
   gradlew.bat cleanmerge.01  # Clean merged subtitles
   gradlew.bat mux.01         # Create final MKV
   gradlew.bat torrent.01     # Create a torrent file for the final MKV
   gradlew.bat nyaa.01        # Upload the torrent file to Nyaa (requires a `sekrit.properties` file with authorization details)
   ```

   It's also possible to create specific types of releases.

   ```ini
   ncpremux=${episode}/*(Premux)*.mkv
   mini.*.ncpremux=${episode}/*(Mini Premux)*.mkv
   ```

   These can be used by adding the `-Prelease=xx` parameter before the task:

   ```sh
   # Unix
   ./gradlew -Prelease=mini mux.01

   # Windows
   gradlew.bat -Prelease=mini mux.01
   ```

   If your shell supports it,
   you can run multiple tasks iteratively:

   ```sh
   # Git Bash
   ./gradlew mux.{01..12}
   ```

## Scripts & Hooks

This template comes with multiple scripts and hooks.

### no-common-fonts

A pre-commit hook
that checks if any pushed fonts
match any fonts in `common/fonts`.
This is to prevent users from pushing duplicate fonts
and potentially overriding dialogue fonts
with different versions of the font.

### blank_dialogue

Replaces the given .ass subtitle file with:

- Custom metadata and two predefined styles (Default, Alt)
- A set of fixed chapter marker and comment events
- All Dialogue lines retained structurally but emptied of text

The original file is overwritten in-place.

This is used to blank out for example official scripts
while preserving their timing
to use as a base for original translations.

### project_setup

A simple shell script that updates the local git config
to filter commits using `scripts/clean_project_garbage.sh`.

This will filter out Aegisub project garbage metadata
so users won't keep overriding that
with their own premux locations and other local metadata.
As such, it's highly recommended
that everyone on the team runs this script.

### releasepost

A simple Python script to generate our common release post template.

Modify the base script to liking and run it.
It asks for nyaa and neko urls,
then creates a /release command for [Nino](https://github.com/9vult/nino)
and tweet for X or BlueSky, including hashtags.

### tl_memory

A simple Python script to help you find matches between the TL and CCs.

It will find all lines that match the search term in the TL and CCs,
and find an accompanying line in the other file.
You can look up both English and Japanese words,
and increase the context window to see more lines around the match,
among other options.

<details>
  <summary>Example usage</summary>

  ![tl_memory example](https://i.imgur.com/8I7g7tB.png)
  ![tl_memory example](https://i.imgur.com/AMgvUWM.png)

</details>

## local.properties

The `local.properties` file is not included in this repository.

If you want to use custom local settings,
you should create this file in the project root directory.
This allows you to override specific properties locally
without the risk of accidentally committing them to the repository.

This is read after the `sub.properties` file and before the `sekrit.properties`,
and overrides any properties set in that file.

### Example pointing premux directory to a network drive

```ini
# local.properties file in project root directory.
premux=Z:/share/private/Premuxes/show_title/premux/*${tvdb}*(Premux)*.mkv
{SP*}.premux=Z:/share/private/Premuxes/show_title/premux/Specials/*${tvdb}*(Premux)*.mkv
ncpremux=Z:/share/private/Premuxes/show_title/premux/Extras/*${episode}*(Premux)*.mkv

mini.*.premux=Z:/share/private/Premuxes/show_title/premux (mini)/*${tvdb}*(Mini Premux)*.mkv
mini.{SP*}.premux=Z:/share/private/Premuxes/show_title/premux (mini)/Specials/*${tvdb}*(Mini Premux)*.mkv
mini.*.ncpremux=Z:/share/private/Premuxes/show_title/premux (mini)/Extras/*${episode}*(Mini Premux)*.mkv
```

## sekrit.properties

The `sekrit.properties` file is not included in this repository.

This properties file is used to store your Nyaa username and password,
as well as other sensitive information.
By default, this file will be read from a directory above the project root directory.

This is read after the `sub.properties` and `local.properties` files,
and overrides any properties set in those files.

### Example setting Nyaa username and password

```ini
# sekrit.properties file in the directory above the project root directory.
nyaauser=YourUsername
nyaapass=YourPassword
```

## Contributing

Spot an issue in the build scripts?
Have any suggestions?
Contributions are welcome!<br>
Please fork the repository
and create a pull request with your changes,
or create an issue.

For further support,
please contact either @LightArrowsEXE or @petzku
on the [Kaleido discord server](https://discord.com/servers/stalleido-subs-443264565069742080).
