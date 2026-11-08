#!/usr/bin/env python3
"""
VLOM — пиксель-скринтюнинг (S4.1): сравнение скриншота с эталоном Vista MCE.

Использование:
    tools/diff_screens.py reference.png shot.png [порог_процент]

Метрика: mean absolute difference по каналам RGB (downscale до 640px — устойчивость
к масштабированию). Порог по умолчанию 8% — «визуально близко».
Вывод: diff-изображение (heat-мапа) в <shot>.diff.png + число в stdout.
Exit 0 = в пределах порога, 1 = отклонение.
"""
import sys

try:
    from PIL import Image, ImageChops
except ImportError:
    print("требуется Pillow: pip install Pillow")
    sys.exit(2)


def load_gray_scaled(path, size=(640, 360)):
    img = Image.open(path).convert("RGB")
    img = img.resize(size, Image.BILINEAR)
    return img


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(2)
    ref_path, shot_path = sys.argv[1], sys.argv[2]
    threshold = float(sys.argv[3]) if len(sys.argv) > 3 else 8.0

    ref = load_gray_scaled(ref_path)
    shot = load_gray_scaled(shot_path)

    if ref.size != shot.size:
        print(f"размеры различаются: ref={ref.size} shot={shot.size}")
        sys.exit(1)

    diff = ImageChops.difference(ref, shot)
    hist = diff.convert("L").histogram()
    total = sum(hist)
    mean_dev = sum(i * h for i, h in enumerate(hist)) / total / 255 * 100
    pct_similar = sum(h for i, h in enumerate(hist) if i <= 16) / total * 100

    diff.save(shot_path.rsplit(".", 1)[0] + ".diff.png")
    print(f"mean deviation: {mean_dev:.1f}% | пикселей близких (<=16/255): {pct_similar:.1f}%")
    print("VERDICT: " + ("PASS — в пределах порога" if mean_dev <= threshold else "TUNE — отличается сильнее порога"))
    sys.exit(0 if mean_dev <= threshold else 1)


if __name__ == "__main__":
    main()
