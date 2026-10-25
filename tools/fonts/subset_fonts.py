# Shrinks the Pretendard fonts to only the characters the app needs (1.5MB → 0.4MB each). The first screen shows faster.
# Usage: python3 tools/fonts/subset_fonts.py <folder with the original fonts>
#   Source: public/static/Pretendard-{Regular,Medium,SemiBold,Bold}.otf from a https://github.com/orioncactus/pretendard release
# Characters kept: the 2,350 Hangul syllables of KS X 1001 + every character in the app code and bundled data + Latin, punctuation, symbols, jamo.
# Other rare syllables (in announcement titles, etc.) are shown in the system font.
# License: Pretendard is SIL OFL 1.1, and "Pretendard" is a Reserved Font Name.
#   The subset fonts are modified versions, so they are saved under the name "CUK Moa Sans" (original copyright and license in core/PRETENDARD_OFL.txt).
import glob, os, subprocess, sys

from fontTools.ttLib import TTFont


def rename(path, style, family='CUK Moa Sans', ps='CUKMoaSans'):
    """예약 글꼴 이름(Pretendard)을 빼고 새 이름을 붙인다."""
    f = TTFont(path)
    name = f['name']
    for nid in (1, 2, 3, 4, 6, 16, 17, 18, 21, 22):
        name.removeNames(nameID=nid)
    full = f'{family} {style}'
    basic = style in ('Regular', 'Bold')
    for nid, val in [(1, family if basic else full), (2, style if basic else 'Regular'), (3, f'{ps}-{style};subset of Pretendard'),
                     (4, full), (6, f'{ps}-{style}'), (16, family), (17, style)]:
        name.setName(val, nid, 3, 1, 0x409)
        name.setName(val, nid, 1, 0, 0)
    if 'CFF ' in f:
        cff = f['CFF '].cff
        cff.fontNames = [f'{ps}-{style}']
        top = cff.topDictIndex[0]
        if hasattr(top, 'FullName'):
            top.FullName = full
        if hasattr(top, 'FamilyName'):
            top.FamilyName = family
    f.save(path)

src = sys.argv[1]
chars = set()
for hi in range(0xB0, 0xC9):
    for lo in range(0xA1, 0xFF):
        try:
            chars.add(bytes([hi, lo]).decode('euc-kr'))
        except UnicodeDecodeError:
            pass
for f in glob.glob('**/src/commonMain/**/*.kt', recursive=True) + glob.glob('shared/data/*.json'):
    chars.update(open(f, encoding='utf-8').read())
for a, b in [(0x20, 0x7E), (0xA0, 0xFF), (0x2000, 0x206F), (0x2190, 0x21FF), (0x2460, 0x24FF), (0x25A0, 0x25FF),
             (0x2600, 0x27BF), (0x3000, 0x303F), (0x3131, 0x318E), (0xFF01, 0xFF5E)]:
    chars.update(chr(c) for c in range(a, b + 1))
text = ''.join(sorted(c for c in chars if ord(c) >= 0x20))
open('/tmp/pretendard-chars.txt', 'w', encoding='utf-8').write(text)
for w, name in [('regular', 'Regular'), ('medium', 'Medium'), ('semibold', 'SemiBold'), ('bold', 'Bold')]:
    out = f'core/src/commonMain/composeResources/font/moasans_{w}.otf'
    subprocess.run(['pyftsubset', os.path.join(src, f'Pretendard-{name}.otf'), '--text-file=/tmp/pretendard-chars.txt',
                    "--layout-features=*", f'--output-file={out}'], check=True)
    rename(out, name)
print(f'{len(text)}자로 줄였어요')
