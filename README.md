# VMTU Libraries

VMTU Core Libraries.

## Modules

|     Module     | License | Usage                                                                                                                                                                      |
|:--------------:|:-------:|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|    `common`    | LGPL v3 | General-purpose library for modules.[^1]                                                                                                                                   |
|   `modpack`    | LGPL v3 | Parses `patchpackinfo.json` (with deprecated `modpackinfo.json` fallback) and metadata included in modpacks.                                                                |
| `resourcepack` | AGPL v3 | A library that downloads and automatically installs resource packs, and also includes a feature for automatically installing local resource packs. Fork of I18nUpdateMod3. |

[^1]: When importing other modules into your project, you must also import the common module.

## How to use on your project
We use Jitpack as our Maven repository.

```groovy
repositories {
    mavenCentral()
    maven { url 'https://www.jitpack.io' }
}

dependencies {
    implementation 'com.github.VM-Chinese-translate-group.VMTULibraries:${module_name}:${version}'
}
```

## Patch pack information

`modpack` reads the flat `patchpackinfo.json` format first and only falls back to
the deprecated `modpackinfo.json` when the new file is absent. Generated and
updated metadata is always written as `patchpackinfo.json`; the old format is
never generated.

VMTU extends the base patch pack fields with `id`, `language`, `version`
(translation version), and `modpackVersion` (the exact installed modpack
version). Both formats are exposed as the same `ModpackInfo` type:

```java
ModpackInfoHelper.readModpackInfo(true);
ModpackInfo info = ModpackInfoHelper.getModpackInfo();
String translationId = info.getId();
String translationVersion = info.getVersion();
String modpackVersion = info.getModpackVersion();
```

Callers reading a `patchpackinfo.json` entry from a ZIP can use
`ModpackInfoHelper.parseModpackInfo(reader)`. The library only parses metadata;
it does not extract the ZIP or apply `diff` operations.
