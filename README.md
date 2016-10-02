EoS Tools
=========
[Echo of Soul](https://en.wikipedia.org/wiki/Echo_of_Soul) game client tools.

Features
--------
* Decrypt System folder
* Patch unreal packages to open in tools like [UTPT]
* Patch StaticMeshes to open in [Umodel]
* Patch Sounds to open in [UE2Runtime]

Build
-----
`gradlew build`

Run
---
```
java -jar build/libs/eos_tools.jar -decrypt EOS/GameClient/System
java -jar build/libs/eos_tools.jar -patch EOS/GameClient
java -jar build/libs/eos_tools.jar -patch_usx EOS/Data/StaticMeshes
java -jar build/libs/eos_tools.jar -patch_uax EOS/Data/Sounds
```

[UTPT]: http://www.acordero.org/projects/unreal-tournament-package-tool/
[Umodel]: http://www.gildor.org/projects/umodel
[UE2Runtime]: http://web.archive.org/web/20130321074020/http://apacudn.epicgames.com/Two/UnrealEngine2Runtime22262002.html