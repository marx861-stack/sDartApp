import os
import sys
import json
import shutil
import hashlib
import time
import threading
from pathlib import Path
from typing import Optional
import numpy as np

# Conditional import for Kokoro
try:
    from kokoro import KPipeline
except ImportError:
    KPipeline = None

try:
    import lameenc
except ImportError:
    lameenc = None


class KokoroAudioService:
    """
    Service to generate and cache high-quality neural speech using Kokoro-82M (Apache 2.0).
    """

    REPO_ID = "hexgrad/Kokoro-82M"
    LANG_CODE = "b"          # British English ('b' = en_GB)
    VOICE_MODEL = "bm_lewis" # Standard British Male voice
    SAMPLE_RATE = 24000

    def __init__(self, project_root: Optional[Path] = None):
        if project_root is None:
            project_root = Path(__file__).resolve().parent.parent
        self.project_root = project_root

        # Target directory for KMP (used by both Android assets and Desktop resources)
        self.assets_audio_dir = (
            self.project_root / "composeApp" / "src" / "androidMain" / "assets" / "audio"
        )
        self.assets_audio_dir.mkdir(parents=True, exist_ok=True)

        self.pipeline: Optional[KPipeline] = None
        self.lock = threading.Lock()
        self.ready_event = threading.Event()

        # Start initialization
        self._initialize_voice()

    def _initialize_voice(self):
        """Load the Kokoro TTS pipeline and warm up the voice."""
        with self.lock:
            if self.pipeline is not None:
                return

        try:
            if KPipeline is not None:
                loaded_pipeline = KPipeline(lang_code=self.LANG_CODE, repo_id=self.REPO_ID)
                loaded_pipeline.load_voice(self.VOICE_MODEL)
                with self.lock:
                    self.pipeline = loaded_pipeline
                print(f"[AUDIO] Kokoro voice '{self.VOICE_MODEL}' ({self.REPO_ID}) loaded successfully.")

                # Warmup Run
                try:
                    list(self.pipeline("Warmup", voice=self.VOICE_MODEL, speed=1.0))
                    print("[AUDIO] Kokoro engine warmed up.")
                except Exception as warmup_err:
                    print(f"[AUDIO] Warmup failed (non-critical): {warmup_err}")
            else:
                print("[AUDIO] Error: 'kokoro' package is not installed.")
        except Exception as e:
            print(f"[AUDIO] Error loading Kokoro voice: {e}")
        finally:
            self.ready_event.set()

    def wait_until_ready(self, timeout: Optional[float] = None) -> bool:
        """Wait until the voice model is loaded."""
        return self.ready_event.wait(timeout=timeout)

    @staticmethod
    def _number_to_british_words(n: int) -> str:
        """Converts 101-180 to British English with 'and' (e.g. 140 -> 'One hundred and forty')."""
        ones = [
            "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
            "seventeen", "eighteen", "nineteen"
        ]
        tens = ["", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"]
        if 101 <= n <= 199:
            rem = n - 100
            if rem < 20:
                return f"One hundred and {ones[rem]}"
            t, o = divmod(rem, 10)
            return f"One hundred and {tens[t]}" + (f"-{ones[o]}" if o else "")
        return str(n)

    @staticmethod
    def _compute_filename(text: str, excitement: int = 0) -> tuple[str, str, float]:
        """
        Computes the spoken text, speed, and deterministic filename for a given call and excitement level.
        """
        speed = 1.0
        spoken_text = text

        # Ensure British 'One hundred and ...' for 101-180 (Kokoro's default G2P uses US 'one hundred forty')
        if text.strip().isdigit():
            num = int(text.strip())
            if 101 <= num <= 180:
                spoken_text = KokoroAudioService._number_to_british_words(num)

        if excitement == 1:
            speed = 1.15  # Faster
        elif excitement >= 2:
            speed = 0.85  # Slower / more dramatic
            if "ONE HUNDRED AND EIGHTY" in text.upper() or text.strip() == "180":
                spoken_text = "ONE HUNDRED ... AND ... EIGHTY!"
            else:
                spoken_text = spoken_text.upper()
                if not spoken_text.endswith("!"):
                    spoken_text += "!"

        file_prefix = "".join(x for x in text if x.isalnum() or x in " -_").strip()
        text_hash = hashlib.md5(f"{spoken_text}_{speed}_{KokoroAudioService.VOICE_MODEL}".encode("utf-8")).hexdigest()[:8]
        filename = f"{file_prefix[:20]}_{text_hash}.mp3"
        return spoken_text, filename, speed

    def get_speech_file(self, text: str, excitement: int = 0) -> Optional[Path]:
        """
        Get the path to an audio file for the given text.
        Generates it in assets/audio if not already present.
        """
        start_time = time.perf_counter()
        if not text:
            return None

        spoken_text, filename, speed = self._compute_filename(text, excitement)
        output_path = self.assets_audio_dir / filename

        # Instant return if file already exists
        if output_path.exists():
            duration = (time.perf_counter() - start_time) * 1000
            print(f"[AUDIO] Cache match (Instant): '{text}' -> {filename} ({duration:.2f}ms)")
            return output_path

        if not self.ready_event.is_set():
            print(f"[AUDIO] Waiting for TTS engine to generate: '{text}'...")
            self.wait_until_ready(timeout=30.0)

        if self.pipeline is not None:
            try:
                print(f"[AUDIO] Generating: '{spoken_text}' (key='{text}', excitement={excitement}, speed={speed})")
                with self.lock:
                    chunks = []
                    for _, _, audio in self.pipeline(spoken_text, voice=self.VOICE_MODEL, speed=speed):
                        if audio is not None:
                            arr = audio.cpu().numpy() if hasattr(audio, "cpu") else np.asarray(audio)
                            chunks.append(arr)

                if not chunks:
                    print("[AUDIO] Error: No audio data generated.")
                    return None

                full_audio = np.concatenate(chunks)
                peak = np.max(np.abs(full_audio))
                if peak > 0:
                    full_audio = full_audio / max(peak, 1.0) * 0.95
                pcm_int16 = (np.clip(full_audio, -1.0, 1.0) * 32767).astype(np.int16)
                raw_audio = pcm_int16.tobytes()

                if lameenc:
                    encoder = lameenc.Encoder()
                    encoder.set_bit_rate(320)
                    encoder.set_in_sample_rate(self.SAMPLE_RATE)
                    encoder.set_channels(1)
                    encoder.set_quality(2)

                    mp3_data = encoder.encode(raw_audio)
                    mp3_data += encoder.flush()

                    with open(output_path, "wb") as f:
                        f.write(mp3_data)
                else:
                    import wave
                    wav_path = output_path.with_suffix(".wav")
                    with wave.open(str(wav_path), "wb") as wav_file:
                        wav_file.setnchannels(1)
                        wav_file.setsampwidth(2)
                        wav_file.setframerate(self.SAMPLE_RATE)
                        wav_file.writeframes(raw_audio)
                    return wav_path

                duration = (time.perf_counter() - start_time) * 1000
                print(f"[AUDIO] Generation complete: '{text}' -> {filename} ({duration:.2f}ms)")
                return output_path
            except Exception as e:
                print(f"[AUDIO] Synthesis/Encoding error for '{text}': {e}")
        else:
            print(f"[AUDIO] Voice model not available for synthesis: '{text}'")

        return None

    @staticmethod
    def get_standard_calls() -> list[str]:
        """Returns the list of all standard dart calls required by the KMP app."""
        # 1. Standard Numbers 0-180
        calls = [str(i) for i in range(181)]

        # 2. Key Game Events
        calls += [
            "Bust", "Bullseye", "Single Bull", "Winner", "Game Shot",
            "Next Player", "Game on", "No score", "Score", "Miss", "Twenty Five",
            "Leg to", "Sets to", "Match to", "You need", "Remaining", "Game Shot and the Win"
        ]

        # 3. Doubles and Triples
        for i in range(1, 21):
            calls.append(f"Double {i}")
            calls.append(f"Triple {i}")

        # 4. Cricket specific
        calls.append("open")
        calls.append("closed")

        return calls

    def pre_generate_common_audio(self) -> dict:
        """
        Pre-generates all common dart scores and calls and returns the manifest mapping.
        """
        if self.pipeline is None:
            print("[AUDIO] Cannot pre-generate: Kokoro pipeline not loaded.")
            return {}

        print("[AUDIO] Starting pre-generation of common calls with Kokoro...")
        standard_calls = self.get_standard_calls()
        mapping = {}
        count = 0

        # Generate excitement=0 for all standard calls
        for text in standard_calls:
            path = self.get_speech_file(text, excitement=0)
            if path and path.exists():
                mapping[f"{text}_0"] = path.name
                count += 1

        # Generate excitement=2 for dramatic 180 call (used by DartViewModel: triggerSound("... 180", 2))
        path_180_2 = self.get_speech_file("180", excitement=2)
        if path_180_2 and path_180_2.exists():
            mapping["180_2"] = path_180_2.name
            count += 1

        # Preserve static sound effects (bing.mp3, buzzer.mp3)
        if (self.assets_audio_dir / "bing.mp3").exists():
            mapping["bing_0"] = "bing.mp3"
        if (self.assets_audio_dir / "buzzer.mp3").exists():
            mapping["buzzer_0"] = "buzzer.mp3"

        print(f"[AUDIO] Pre-generation complete. {count} voice calls ready.")
        return mapping


def main():
    project_root = Path(__file__).resolve().parent.parent
    assets_audio_dir = project_root / "composeApp" / "src" / "androidMain" / "assets" / "audio"

    print("Initializing Kokoro Audio Service...")
    service = KokoroAudioService(project_root)
    service.wait_until_ready(timeout=60.0)

    if service.pipeline is None:
        print("Error: Kokoro voice pipeline could not be loaded! Exiting.")
        sys.exit(1)

    # Clear old voice MP3/WAV files in assets/audio while keeping sound effects (bing.mp3, buzzer.mp3)
    protected_files = {"bing.mp3", "buzzer.mp3"}
    print(f"Cleaning old voice files in {assets_audio_dir} (keeping {protected_files})...")
    if assets_audio_dir.exists():
        for ext in ("*.mp3", "*.wav"):
            for f in assets_audio_dir.glob(ext):
                if f.name not in protected_files:
                    f.unlink()

    # Pre-generate all calls and build manifest
    mapping = service.pre_generate_common_audio()

    # Save manifest mapping to JSON
    manifest_path = assets_audio_dir / "manifest.json"
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(mapping, f, indent=2)

    print(f"Saved {len(mapping)} entries to {manifest_path}")
    print("Done!")


if __name__ == "__main__":
    main()
