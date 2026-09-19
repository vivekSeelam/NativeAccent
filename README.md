# NativeAccent

A pronunciation-practice Android app with phoneme-level scoring.

You read a sentence, optionally hear a coach voice say it (Android TextToSpeech, US English), and record
yourself. The take goes to Azure pronunciation assessment, which scores the whole sentence, each word,
and each sound. You get an overall score ring, the sentence coloured word by word, the sounds of any
word you tap, and a coach tip for the weakest sound. You can still play your take next to the coach at
1x or 0.5x.

---

## Quick start

```powershell
# from the project root, in PowerShell
.\gradlew.bat assembleDebug       # builds app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat installDebug        # builds + installs onto a connected device/emulator
.\gradlew.bat testDebugUnitTest   # 24 JVM unit tests
.\gradlew.bat lintDebug           # static analysis
```

In Git Bash use `./gradlew assembleDebug` instead.

Opening the project in Android Studio and pressing Run does the same thing — everything below about
`JAVA_HOME` only matters when you build from a terminal outside the IDE.

### Building from an external terminal

Gradle needs a JDK 21. Android Studio ships one; point at it before running the wrapper:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
```

---

## Azure Speech setup (needed for scores)

Without a key the app still works as record-and-compare: after recording it says scoring isn't set
up and offers **Compare without score**.

1. In the Azure portal, create a **Speech** resource (the free F0 tier is enough to try it).
2. Copy one of its **Keys** and its **Location/Region** (e.g. `eastus`).
3. Put them in `local.properties` at the project root. The placeholder lines are already there:

   ```properties
   AZURE_SPEECH_KEY=your-key-here
   AZURE_SPEECH_REGION=eastus
   ```

4. Rebuild. The values are compiled into `BuildConfig`.

**`local.properties` is gitignored — keep it that way.** This repo is public. Also note that a key in
`BuildConfig` can be extracted from a shipped APK. That's fine for development; before release, move
to short-lived tokens from a backend (see [Swapping the key source](#swapping-the-key-source)).

`local.properties` also holds `sdk.dir`. Android Studio regenerates that line if you clone this
somewhere else, but the Azure lines you add yourself.

---

## Testing

### Unit tests (no device, no key)

```powershell
.\gradlew.bat testDebugUnitTest
```

| Suite | Covers |
|---|---|
| `AzureAssessmentJsonParserTest` | Overall/word/phoneme scores, PronScore fallback, old flat JSON layout, insertions/omissions, empty or silent results, garbage input, clamping |
| `ReferenceAlignmentTest` | Mapping Azure's words back onto the sentence (punctuation kept, insertions skipped), fallback, score bands |
| `WavFormatTest` | The 44-byte WAV header, peak-amplitude reading |
| `StaticCoachTipProviderTest` | Tip picks the weakest phoneme; skipped-word, strong-word and sentence-level tips |

Android's `org.json` is a stub on the JVM, so tests use the real one (`testImplementation(libs.org.json)`).

**Not unit-tested:** the ViewModel and the Azure call itself. The ViewModel still constructs the real
`WavRecorder`, `AudioPlayer` and `CoachVoice`, which need Android. The assessment service and tip
provider are interfaces and already swappable; putting the three audio classes behind interfaces is the
next step to test the ViewModel on the JVM.

### On a phone

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

& $adb devices                  # want a line ending in "device"
.\gradlew.bat installDebug
& $adb shell am start -n com.example.nativeaccent/.MainActivity
```

`unauthorized` in `adb devices` means the RSA prompt is waiting on the phone screen. Nothing listed at
all means it isn't enumerating over USB — almost always a charge-only cable, or the USB mode set to
"No data transfer" instead of File Transfer.

Useful while testing:

```powershell
# the WAV takes on disk (debug builds only)
& $adb shell run-as com.example.nativeaccent ls -l cache/attempts

# app logs
& $adb logcat -s WavRecorder AzureAssessment CoachVoice AudioPlayer AndroidRuntime
```

A size that changes between two `ls -l` listings means a recording is still in progress, not that
something is wrong.

**Device status (Motorola Edge 40, Android 15):** v1 runs, and takes are valid 16 kHz mono 16-bit
WAVs (header checked). One real take was sent from the laptop to the live Azure endpoint with the
project key, and it scored. The response is the `enemy_rest.json` test fixture. **Not yet confirmed:**
the in-app score screen, and the error paths below. Worth walking through:

1. No key configured → record → Screen 1 should say scoring isn't set up and offer
   **Compare without score**.
2. Add a key → record a sentence → "Analyzing…" → the Analysis screen with a score ring.
3. Tap different words → their sound chips appear; **Coach tip** follows the focused word.
4. Tap the mic and stop straight away → "That was a bit short…" and no network call.
5. Record in silence → "We couldn't hear anything…".
6. Airplane mode → record → a network error with **Retry**, which works once you're back online.

---

## How it fits together

```
data/          PracticeItem + PracticeRepository — 50 reference sentences
audio/         WavRecorder (AudioRecord → 16 kHz mono WAV), WavFormat, AudioPlayer,
               CoachVoice (TTS), RecordingStore, PlaybackSpeed
assessment/    PronunciationAssessmentService (interface), PronunciationResult / WordResult /
               PhonemeResult, SpeechCredentialsProvider, ReferenceAlignment
assessment/azure/  AzurePronunciationAssessmentService, AzureAssessmentJsonParser
coaching/      CoachTipProvider (interface), StaticCoachTipProvider
viewmodel/     PracticeViewModel — the single source of truth
ui/            PracticeScreen (1), AnalysisScreen (2), AppNavigation, components/, theme/
```

Nothing in `audio/`, `assessment/` or `coaching/` imports Compose, and nothing in `ui/` touches
AudioRecord, TextToSpeech or the Azure SDK. The ViewModel is the only thing that knows about both.

### The flow

**Screen 1 (Practice).** Read the sentence (the hint names its tricky word), optionally tap the
speaker to hear the coach, then tap the mic to record and again to stop. Then:

- Shorter than 0.6 s, or near-silent → "try again" message; **nothing is sent to Azure**.
- Otherwise → "Analyzing…" with the mic disabled, and the take goes to
  `PronunciationAssessmentService.assess(path, sentence)`.
- Scored → the ViewModel caches the `PronunciationResult` and sends a one-shot event that opens Screen 2.
- Azure heard no speech → a retake prompt.
- Failed (network, quota, timeout…) → the message, **Retry** if it's worth retrying, and
  **Compare without score** either way.

**Screen 2 (Analysis).** Score ring (green ≥ 80, yellow 60–79, red < 60) with accuracy / fluency /
completeness under it. Below that is the sentence, each word coloured by its own score; skipped words
are struck through. Then the sound chips for the focused word. It starts on the weakest word that has
sound detail, and you tap any word to switch. **Coach tip** gives advice for that word's weakest sound.
The Coach / You / 1x row is pinned above the mic (re-record the same sentence) and the forward arrow
(next sentence).

Bookmark, settings, flag and camera are still icons only.

### Recordings

`AudioRecord` at 16 kHz, mono, 16-bit PCM, streamed to disk with the WAV header patched in at the end.
(`MediaRecorder` can't do this — every format it writes is compressed.) Takes stop at 30 s, which
matches Azure's single-shot limit.

`RecordingStore` writes `cacheDir/attempts/attempt_<itemId>_<timestamp>.wav` and deletes that item's
previous take first, so the cache holds at most one file per sentence. Old v0 `.m4a` takes are removed
the same way the next time that sentence is recorded.

### Azure details

`SpeechConfig` (en-US, 1.5 s segmentation silence so mid-sentence pauses don't cut the take short) +
`AudioConfig.fromWavFileInput` + `PronunciationAssessmentConfig.fromJson` (HundredMark, Phoneme,
miscue on, **dimension `Comprehensive`**) + `recognizeOnceAsync()`, on the IO dispatcher with a 30 s timeout. The blocking
`get()` can be interrupted, so re-recording or leaving cancels it. Every SDK object is created and closed
per call.

The dimension is set explicitly on purpose. With `Basic`, Azure returns accuracy only: no fluency,
completeness, overall score or skipped/extra-word flags. A live call with a real recording confirmed
this.

Scores are read from `NBest[0].PronunciationAssessment` (`PronScore` is the overall), then
`Words[].PronunciationAssessment.AccuracyScore` and `Words[].Phonemes[].PronunciationAssessment.AccuracyScore`.
Phoneme names are Azure's default en-US SAPI set (`k`, `ah`, `th`, `dh`, `er`…), shown in upper case.

With miscue detection on, Azure also reports skipped words (`Omission`) and extra words (`Insertion`).
`alignToReference` drops insertions and pairs the rest with the sentence's own tokens, so "check,"
keeps its comma and "I" keeps its capital.

### What it costs (checked Sept 2026, East US, list prices)

Azure bills **per second of audio sent**. Real-time speech-to-text is **$1.00/hour**. Microsoft's pricing
page also lists pronunciation assessment under "enhanced add-on features" (**$0.30/hour**), but a
Microsoft Q&A answer says there's no extra charge. Budget for **$1.30/hour** until your own bill shows
which meters you're charged under (Cost Management → Cost analysis, grouped by meter).

- One take is about 4–5 s of audio, so roughly **$0.0016 per take** (about $1.60 per 1,000).
- A heavy daily user (30 takes a day) sends about 1.25 audio hours a month, roughly **$1.60 per user per month**.
- Free tier: 5 audio hours a month, with low rate limits. For development only.
- Commitment tiers: 2,000 h for $1,600/month, 10,000 h for $6,500, 50,000 h for $25,000. The add-on has
  its own commitment tiers, from $480/month for 2,000 h.
- The coach voice is on-device Android TTS, so it's free. LLM coach tips would be a separate cost.

Recheck with the Azure Retail Prices API (`prices.azure.com`, product "Azure Speech"). The pricing page
itself renders its numbers with JavaScript.

### Swapping the key source

`AzurePronunciationAssessmentService` gets its credentials from a `SpeechCredentialsProvider`.
Today that's `EmbeddedKeyCredentialsProvider` (BuildConfig). For production, write a
`TokenEndpointCredentialsProvider` that asks your backend for a ~10-minute token and returns
`SpeechCredentials.AuthorizationToken`. The service already calls `SpeechConfig.fromAuthorizationToken`
for that case. Wire it in `PracticeViewModel.factory`. No UI changes.

### Swapping in an LLM coach

`CoachTipProvider.tipFor(sentence, focusedWord, result)` already receives everything a model needs:
the sentence, the word and per-phoneme scores. Implement it with an LLM call, fall back to
`StaticCoachTipProvider` on error, and wire it in `PracticeViewModel.factory`. The TODO in
`CoachTipProvider.kt` has the details. Keep the LLM key server-side, like the speech key.

---

## Toolchain, and why these versions

| | |
|---|---|
| AGP | 9.2.1 |
| Gradle | 9.4.1 |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Azure Speech SDK | 1.51.2 (pulls in `com.azure:azure-core`) |
| compileSdk | 36.1 · minSdk 26 · targetSdk 36 |

**The debug APK is now ~43 MB (was ~13 MB).** Almost all of that is the Speech SDK's native libraries
for arm64-v8a, armeabi-v7a and x86_64. Play's app bundles split by ABI, so users only download one. For
sideloading, `ndk { abiFilters += "arm64-v8a" }` in `defaultConfig` cuts it down, since every modern
phone is arm64. There's no 32-bit x86 build of the SDK, so scoring shows "not supported on this device"
on old x86 emulators. Use an x86_64 or arm64 image.

**Three dependencies are deliberately held back**: navigation-compose `2.9.8`, lifecycle `2.10.0` and
core-ktx `1.18.0`. Their current releases (2.10.1 / 2.11.0 / 1.19.0) all require `compileSdk 37`, and
only the `android-36.1` platform is installed on this machine. Install API 37 via the SDK Manager, bump
`compileSdk` in `app/build.gradle.kts`, and all three can move up. Lint will keep nagging about them
until then — that is expected, not a problem.

Icons in `res/drawable/ic_*.xml` are hand-written vector drawables rather than
`material-icons-extended`, which is deprecated and frozen at 1.7.8.
