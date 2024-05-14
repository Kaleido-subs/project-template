# The Kaleido SubKt Project template

Clone this repo
and copy all the files over
when making a new fansubbing project.

This template is designed to streamline the process
of creating and maintaining subtitles for [Kaleido-subs](https://github.com/Kaleido-subs) projects.

## Dependencies

- [SubKt](https://github.com/Myaamori/SubKt)
- [mkvmerge](https://mkvtoolnix.download/downloads.html)
- [JDK SE 14](https://www.oracle.com/java/technologies/javase/jdk14-archive-downloads.html)
  or [15](https://www.oracle.com/java/technologies/javase/jdk15-archive-downloads.html)

**Warning**:
Currently,
only JDK 14 or 15 appears to work out-of-the-box with SubKt.
Please ensure Gradle uses the correct version if you run into any issues.

## Directory Structure

The following files should be adjusted
on a per-project basis:

- **Root Directory:**

  - `build.gradle.kts`: SubKt Gradle build script.
  - `sub.properties`: Project properties config file.

- **01/**: Example of regular episode directory

  - `NewShow 01 - (Premux) [ABCDEF01].mkv`: Premux file.
  - `NewShow 01 - Dialogue.ass`: Subtitle file for dialogue.
  - `NewShow 01 - TS.ass`: Subtitle file for typesetting.
    - There can be multiple TS files. Example: `NewShow 01 - TS (Light).ass` & `NewShow 01 - TS (petzku).ass`
  - (Optional) `NewShow 01 - INS.ass`: Subtitle file for insert songs.
    - There can be multiple INS files. Example: `NewShow 01 - INS (OP).ass` & `NewShow 01 - INS (Song Name).ass`
  - (Optional) `NewShow 01 - Extra.ass`: Subtitle file for any extra subtitles that may not fit elsewhere.

- **common/**: Common resources for all episodes

  - `warning.ass`: A subtitle file that contains a warning to display in players that don't support ASS tags properly.
  - `fonts/`: Directory containing common fonts, such as dialogue fonts.

## Getting Started

To get started with this template:

1. **Use the Template Repository:**

   Click the following button in the top-right of the [project-template](https://github.com/Kaleido-subs/project-template) GitHub repo.

   ![button](https://i.imgur.com/zT0SLVM.png)

2. **Set up project files:**

   The project file should be updated on a per-project basis.
   This may also include the types of expected files,
   as well as metadata.

   For a fairly regular project,
   set up the following files:

   - Set up the `sub.properties` file.
     - Set the name and expected paths.
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
   ./gradlew mux.01   # For Unix
   gradlew.bat mux.01 # For Windows
   ```

   `mux` can be replaced with any of the following commands:

   - `chapters`: Create a chapters file from the episode's dialogue subtitle file.
   - `merge`: Merge the episode's subtitle files together, as well as optional OP and ED subtitle files.
   - `cleanmerge`: Same as merge, but clean up the merge output by removing ktemplates and empty lines.
   - `swap`: Create a swapped subtitle file. This is commonly used for honorific tracks.

   The output of these commands,
   minux `mux`,
   can be found in the `build/` directory.

## Contributing

Spot an issue in the build scripts?
Have any suggestions?
Contributions are welcome!
Please fork the repository
and create a pull request with your changes,
or create an issue.

For further support,
please contact either LightArrowsEXE or petzku
on the [Kaleido discord server](https://discord.gg/dk7aadV).
