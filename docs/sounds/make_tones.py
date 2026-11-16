import math
import os
import struct
import wave

RATE = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "raw")


def write(name, samples):
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, name + ".wav")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1.0, min(1.0, s)) * 32767)) for s in samples))
    print("%s: %.2f s" % (path, len(samples) / RATE))


def tone(freq, seconds, volume, decay=0.0, attack=0.02, harmonics=()):
    n = int(RATE * seconds)
    out = []
    for i in range(n):
        t = i / RATE
        env = min(1.0, t / attack) * math.exp(-decay * t)
        tail = min(1.0, (seconds - t) / 0.05)
        v = math.sin(2 * math.pi * freq * t)
        for mult, amp in harmonics:
            v += amp * math.sin(2 * math.pi * freq * mult * t)
        out.append(volume * env * tail * v)
    return out


def silence(seconds):
    return [0.0] * int(RATE * seconds)


def chime():
    return tone(880, 0.9, 0.45, decay=2.0, harmonics=((2, 0.2),)) + \
        tone(659.25, 1.2, 0.45, decay=1.6, harmonics=((2, 0.2),)) + silence(0.6)


def bell():
    return tone(880, 2.4, 0.5, decay=1.5, attack=0.005, harmonics=((2.76, 0.3), (5.4, 0.12))) + silence(0.3)


def beep():
    out = []
    for _ in range(3):
        out += tone(1000, 0.2, 0.4, attack=0.005) + silence(0.2)
    return out + silence(0.8)


if __name__ == "__main__":
    write("chime", chime())
    write("bell", bell())
    write("beep", beep())
