# TACZ Durability - Multi-Loader Build System

## Statut Actuel

| Loader | Minecraft | Java | Statut | JAR Résultat |
|--------|-----------|------|--------|--------------|
| **Forge** | 1.20.1 | 17 | ✅ **FONCTIONNEL** | `gundurability-{version}-all.jar` |
| **NeoForge** | 1.20.1 | 17 | ✅ **COMPATIBLE** | Même JAR que Forge |
| **NeoForge** | 1.21.1 | 21 | 🔧 **À FAIRE** | Nécessite portage API |

## Compilation Actuelle

### Forge 1.20.1 (Défaut)
```bash
./gradlew build
```

**Sortie**: `build/libs/gundurability-{version}-all.jar`

**Compatible avec**:
- Forge 1.20.1 (47.x)
- NeoForge 1.20.1 (47.x) - ⚠️ **Test requis**

## Architecture Multi-Loader

```
src/main/java/com/tsumundev/gundurability/
├── platform/
│   ├── IPlatformHelper.java      # Interface d'abstraction
│   └── Platform.java              # Détection loader par réflexion
├── config/
│   ├── Config.java                # Config commune (Forge)
│   └── ConfigClient.java          # Config client (Forge)
├── events/
│   ├── TaczEvents.java            # Events gameplay
│   └── KeybindEvents.java         # Events clavier
├── network/
│   ├── InspectDurabilityMessage.java
│   ├── RepairGUIButtonMessage.java
│   └── S2CCleaningScreenPacket.java
└── Gundurability.java             # Classe principale (@Mod)
```

### Détection du Loader

```java
// Dans Platform.java
public static boolean isNeoForge() {
    return IS_NEOFORGE;  // Détection par Class.forName("net.neoforged.fml.ModLoader")
}

public static String getPlatformName() {
    return HELPER.getPlatformName();  // "Forge" ou "NeoForge"
}
```

## Pour NeoForge 1.21.1

### Changements d'API majeurs

1. **Packages**: `net.minecraftforge.*` → `net.neoforged.*`
2. **Config**: `ForgeConfigSpec` → `ModConfigSpec`
3. **Réseau**: `SimpleChannel` → Système `Payload`
4. **Events**:
   - `NetworkEvent.Context` → `IPayloadContext`
   - `FMLCommonSetupEvent` → `RegisterPayloadHandlerEvent`
5. **Gradle**: `net.minecraftforge.gradle` → `net.neoforged.moddev`

### Stratégie de Portage Recommandée

**Option A: Sourcesets séparés**
```
src/
├── main/java/          # Code commun (utils, NBT, items...)
├── forge/java/         # Forge 1.20.1 spécifique
└── neoforge/java/      # NeoForge 1.21+ spécifique
```

**Option B: Branche séparée**
- Branche `forge-1.20` (actuelle)
- Branche `neoforge-1.21` (à créer)

## Commandes Utiles

### Compiler uniquement
```bash
./gradlew build
```

### Compiler et lancer client
```bash
./gradlew runClient
```

### Compiler et lancer serveur
```bash
./gradlew runServer
```

### Nettoyer
```bash
./gradlew clean
```

## Fichiers de Configuration

| Fichier | Usage |
|---------|-------|
| `build.gradle` | Build Forge 1.20.1 |
| `settings.gradle` | Configuration Maven |
| `gradle.properties` | Versions (mod, minecraft, forge) |

## Dépendances

### Runtime
- Minecraft 1.20.1
- Forge 47.3.12 ou NeoForge 47.1.x
- TACZ (Timeless and Classics Zero) 1.1.4+

### Développement
- Java 17
- Gradle 8.8
- MixinExtras 0.4.1

## Prochaines Étapes pour NeoForge 1.21.1

1. ✅ Analyser les différences d'API
2. ⏳ Créer les classes ConfigNeo et ConfigClientNeo
3. ⏳ Porter les events vers NeoForge
4. ⏳ Porter le système de réseau (Payloads)
5. ⏳ Configurer ModDevGradle
6. ⏳ Tester le build

## Notes de Compatibilité

- NeoForge 1.20.1 maintient la compatibilité avec `net.minecraftforge.*`
- NeoForge 1.21+ casse cette compatibilité
- Les JARs compilés avec Forge 1.20.1 fonctionnent sur NeoForge 1.20.1
