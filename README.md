# Pupil  
Shader support for Forge — revived, maintained, and moving forward.

## What is Pupil?

Pupil is a community-maintained continuation of Oculus, which was an unofficial port of Iris (Fabric shaders) to Forge.  
Since Oculus development has stalled, Pupil aims to keep shader support alive on Forge for modern Minecraft versions.

## Why "Pupil"?

The pupil is the part of the eye that lets light in — small, but essential.  
This fork is the same: a focused, community-driven project that keeps the light (and shaders) coming.

## Current Status

- Supports Minecraft 1.20.1 (Forge 47.2+)  
- Compatible with Rubidium and Embedium  
- Works with most OptiFine-format shader packs (BSL, Complementary, SEUS, etc.)  
- Actively maintained — bugfixes and version ports welcome!

## Dependencies

- **Forge** (for your Minecraft version)  
- **Rubidium** or **Embedium** (performance rendering backend)

## Installation

1. Install Forge for your Minecraft version  
2. Download the latest Pupil and Rubidium/Embedium jars  
3. Put them in your `mods` folder  

## Reporting Issues

Please use the [GitHub Issues](https://github.com/CarbonMC-CN/Pupil/issues ) page.  
Include your Minecraft version, Forge version, Pupil version, and any relevant logs or crash reports.

## Building from Source

```bash
git clone https://github.com/CarbonMC-CN/Pupil.git 
cd Pupil
./gradlew build
```
The built jar will be in `build/libs/`.

## License

Pupil is licensed under the LGPL-3.0, same as Oculus and Iris.  
This is an unofficial continuation — not affiliated with the original Oculus or Iris teams.

## Thanks

To the original Iris and Oculus contributors for laying the groundwork — this project stands on their shoulders.
    
