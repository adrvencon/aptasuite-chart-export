# AptaSUITE Chart Exporter

A small Java extension that adds **Save chart as PNG...** and **Save chart as PDF...** to the right-click context menu of JavaFX charts in [AptaSUITE](https://github.com/drivenbyentropy/aptasuite), tested with the **v0.9.8** release.

This project is not affiliated with, endorsed by, or part of the official AptaSUITE project.

## Features

### PNG export

- Choose the resolution (1x, 2x, 4x, 6x or 8x the on-screen size). The dialog shows the resulting size in pixels and in centimeters at 300 dpi.
- Optional transparent background. This removes the chart and plot-area backgrounds, while the legend keeps its own.
- The resolution is stored inside the file (96 dpi times the scale).
- If the chosen resolution is too large for the chart or for the available memory, the export falls back to the next lower one and tells you.

### PDF export

- Bars, lines and axes are written as vector paths, and text is written as real, selectable text, so the figure scales without losing quality.
- The system font is used and embedded when it can be found. Otherwise Helvetica is used without embedding.
- The page has the size of the chart on screen (96 dpi) and no margins.
- It uses the iText library that AptaSUITE already ships. If iText is ever missing, the PDF entry is hidden and PNG export keeps working.

### General

- Works on any JavaFX chart. Right-click the chart or any of its child nodes.
- Suggests a filename based on the chart title.
- Remembers the last folder and the PNG choices between sessions.
- Includes launch scripts for Windows and Unix-like systems (Linux and macOS).

## Compatibility

| Component | Version / requirement |
|---|---|
| AptaSUITE | 0.9.8 |
| Java bytecode target | Java 11 (`--release 11`) |
| Compile-time JavaFX API | OpenJFX 17.0.2 |
| PDF export | iText 5.x, the copy shipped with AptaSUITE (compiled against 5.5.13.1 with Maven scope `provided`) |
| Operating systems | Windows, Linux and macOS launch scripts are provided. Runtime testing is still needed on each platform. |

**JavaFX runtime compatibility matters.** The extension compiles against OpenJFX 17.0.2 with Maven scope `provided`, and JavaFX is not bundled in the extension JAR. At launch, the JavaFX classes available to AptaSUITE must be compatible with the APIs used by the extension. The Java version and JavaFX runtime supplied by a particular AptaSUITE installation can vary by distribution. If the extension fails with a `NoSuchMethodError`, `ClassNotFoundException` or another JavaFX linkage error, check the runtime versions before reporting an issue.

This project currently targets AptaSUITE 0.9.8. Compatibility with other AptaSUITE versions has not been established.

## Requirements

- A working AptaSUITE 0.9.8 installation.
- A Java runtime compatible with that AptaSUITE installation. Java 11 or newer is not by itself a guarantee that the JavaFX runtime is compatible.
- Maven 3.8+ and a JDK 11+ to build from source.

## Build from source

Clone this repository and build the JAR:

```bash
git clone https://github.com/adrvencon/aptasuite-chart-export.git
cd aptasuite-chart-export
mvn clean package
```

The resulting file is `target/aptasuite-chart-export.jar`.

The build downloads the compile-time JavaFX and iText dependencies from Maven Central. They are marked `provided` and are not packaged in the resulting JAR. The iText version in the `pom.xml` should match the one found in the `lib` folder of your AptaSUITE installation.

## Installation

1. Build the project as described above, or download `aptasuite-chart-export.jar` from a project release.
2. Locate the folder containing your AptaSUITE JAR, for example `aptasuite-0.9.8.jar`.
3. Copy `aptasuite-chart-export.jar` into that same folder.
4. Copy the appropriate launcher script from this repository's `dist/` folder into that folder:
   - Windows: `run-aptasuite.bat`
   - Linux/macOS: `run-aptasuite.sh`
5. Launch AptaSUITE using the script instead of double-clicking the original JAR.

Example installation directory:

```text
AptaSUITE/
├── lib/
├── aptasuite-0.9.8.jar
├── aptasuite-chart-export.jar
├── run-aptasuite.bat
└── run-aptasuite.sh
```

The launcher expects exactly one file matching `aptasuite-*.jar` in the folder (excluding `aptasuite-chart-export.jar`). If multiple AptaSUITE versions are present, move the unused JARs elsewhere before launching.

To uninstall, delete the extension JAR and the launcher script. The original AptaSUITE files are never modified.

### Windows

Double-click `run-aptasuite.bat`. You can optionally set JVM options in the `JAVA_OPTS` environment variable, for example `-Xmx8G`.

### Linux and macOS

Open a terminal in the installation folder, make the script executable if needed, and run it:

```bash
chmod +x run-aptasuite.sh
./run-aptasuite.sh
```

Optional JVM options can be supplied with `JAVA_OPTS`:

```bash
JAVA_OPTS='-Xmx8G' ./run-aptasuite.sh
```

## Usage

1. Start AptaSUITE with the appropriate launcher script.
2. Open a view containing a chart.
3. Right-click the chart (or one of its plotted elements).
4. Select **Save chart as PNG...** or **Save chart as PDF...**.
5. For PNG, pick the resolution and whether the background should be transparent.
6. Choose the destination and filename in the save dialog.

Use PDF for papers and posters, where the figure may be resized, and PNG for slides and quick sharing. For print, a PNG at 4x or more is a good starting point.

## Acknowledgements

If AptaSUITE contributes to your research, please cite the original software and the relevant algorithms as requested by its authors. See the [official AptaSUITE repository](https://github.com/drivenbyentropy/aptasuite) for details.

## License

This project is licensed under the GNU General Public License v3.0.
See the [LICENSE](LICENSE) file for details.

## Disclaimer

AptaSUITE Chart Exporter is provided without warranty. Use it at your own risk and retain backups of important project data.
