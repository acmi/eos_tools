EoS Tools
=========
[Echo of Soul](https://en.wikipedia.org/wiki/Echo_of_Soul) game client tools.

Features
--------
* Decrypt System folder
* Patch unreal packages to open in tools like [UTPT]
* Patch StaticMeshes to open in [Umodel]

Build
-----
`gradlew build`

Run
---
```
java -jar build/libs/eos_tools.jar -decrypt EOS/GameClient/System
java -jar build/libs/eos_tools.jar -patch EOS/GameClient
java -jar build/libs/eos_tools.jar -patch_usx EOS/Data/StaticMeshes
```

[UTPT]: http://www.acordero.org/projects/unreal-tournament-package-tool/
[Umodel]: http://www.gildor.org/projects/umodel