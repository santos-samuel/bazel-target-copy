# Bazel Target Copy

An IntelliJ plugin that adds a clickable inlay hint next to each Bazel rule in `BUILD.bazel` files. Click the hint to copy the fully-qualified target label to your clipboard.

https://github.com/user-attachments/assets/d96c631c-d007-4aac-aa88-b2485f7ec234

## Features

- Displays `⎘ //path/to/package:target_name` inline on the opening line of every Bazel rule
- Click to copy the target label to your clipboard
- Brief "Copied!" tooltip on click
- Works alongside both major Bazel plugins for IntelliJ

## Requirements

- **IntelliJ IDEA** 2024.1 or later
- **Java** 17 or later (for building the plugin)
- At least one of the following Bazel plugins installed in your IDE:
  - [Bazel for IntelliJ (EAP)](https://plugins.jetbrains.com/plugin/22972) by JetBrains
  - [Bazel for IntelliJ](https://plugins.jetbrains.com/plugin/8609) by Google

> **Note:** This plugin does **not** provide Bazel language support on its own. It extends the file type registered by one of the above plugins. At least one must be installed and enabled.

## Constraints

- Only works on files recognized by your installed Bazel plugin. The JetBrains EAP plugin registers `BUILD` and `*.bazel` under the `Starlark` language; the Google plugin registers `BUILD.bazel` under the `BUILD` language. Files not covered by either plugin's file type configuration will not show hints.
- The workspace root is detected by walking up the directory tree looking for a `WORKSPACE` or `WORKSPACE.bazel` file. If neither is found, the label will use only the immediate directory name.
- Target labels are extracted via a simple regex. Dynamically generated `name` attributes (e.g. computed via a macro) will not be resolved correctly.

## Installation

### From JetBrains Marketplace

Coming soon...

### From disk

1. [Download the latest release](https://github.com/santos-samuel/bazel-target-copy/releases) zip, or build it yourself (see below)
2. In IntelliJ: **Settings → Plugins → ⚙️ → Install Plugin from Disk...**
3. Select the `.zip` file
4. Restart IntelliJ

## Building from source

### Prerequisites

- JDK 17+
- No need to install Gradle — the included wrapper handles it

### Steps

```bash
git clone https://github.com/santos-samuel/bazel-target-copy.git
cd bazel-target-copy
./gradlew buildPlugin
```

The installable zip is produced at:

```
build/distributions/bazel-target-copy-1.0.0.zip
```

### Running in a sandboxed IDE (for development)

```bash
./gradlew runIde
```

This downloads a fresh IntelliJ instance and launches it with the plugin installed. Your real IDE is not affected. The first run takes a few minutes to download the IDE; subsequent runs are fast.

## Development

### Project structure

```
src/main/
├── java/com/github/santossamuel/bazeltargetcopy/
│   ├── BazelTargetInlayHintsProvider.java  # Registers the provider with IntelliJ
│   └── BazelTargetCollector.java           # Scans BUILD.bazel and renders hints
└── resources/META-INF/
    └── plugin.xml                          # Plugin descriptor and extension points
```

### Rebuilding after changes

```bash
./gradlew buildPlugin
```

Then reinstall from disk in your IDE (**Settings → Plugins → ⚙️ → Install Plugin from Disk...**) and restart.

## License

Apache 2.0
