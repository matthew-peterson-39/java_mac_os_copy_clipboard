# 📋 Clipboard Manager for macOS

A simple clipboard history manager that brings Windows+V functionality to macOS.

## Features

- **Global hotkey** - Press `Cmd+Shift+V` to open clipboard history
- **Smart positioning** - Window appears near your cursor, even in fullscreen apps
- **Instant paste** - Double-click any entry to paste it
- **Search** - Type to filter through your clipboard history
- **Memory-only** - No data saved to disk for privacy

## Quick Setup

1. **Download dependency:**
   ```bash
   mkdir lib
   curl -o lib/jnativehook-2.2.2.jar https://repo1.maven.org/maven2/com/github/kwhat/jnativehook/2.2.2/jnativehook-2.2.2.jar
   ```

2. **Compile and run:**
   ```bash
   javac -cp "lib/jnativehook-2.2.2.jar" src/*.java -d bin/
   java -cp "bin:lib/jnativehook-2.2.2.jar" App
   ```

3. **Grant permissions:**
   - System Preferences → Security & Privacy → Privacy → Accessibility
   - Add Java or Terminal to allowed apps
   - Restart the application

## Usage

- Copy text normally with `Cmd+C`
- Press `Cmd+Shift+V` to open clipboard history
- Double-click or press Enter to paste any entry
- Press Esc to close without pasting

## Requirements

- macOS 10.14+
- Java 11+

## Troubleshooting

**Hotkey not working?** Try the alternative hotkey `Cmd+Shift+C` via the system tray menu.

**Window appears behind other apps?** Right-click the tray icon → "Force Maximum Visibility".