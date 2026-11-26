"""Our own 32x32 drawing: a lilac blob squeezed at the waist by round golden tongs on an iron handle."""
import struct
import sys
import zlib

N = 32
CLEAR = (0, 0, 0, 0)
BLOB_LINE = (98, 82, 116, 255)
BLOB = (201, 182, 217, 255)
BLOB_LIGHT = (229, 213, 245, 255)
BLOB_SHADE = (166, 146, 188, 255)
WHITE = (255, 255, 255, 255)
EYE = (60, 46, 76, 255)
BLUSH = (226, 152, 190, 255)
SWEAT = (150, 208, 244, 255)
SWEAT_LINE = (70, 126, 176, 255)
TOOL_LINE = (44, 32, 26, 255)
GOLD = (247, 198, 62, 255)
GOLD_LIGHT = (255, 236, 150, 255)
GOLD_DARK = (178, 118, 26, 255)
IRON = (176, 184, 192, 255)
IRON_LIGHT = (232, 236, 240, 255)
IRON_DARK = (104, 110, 122, 255)

CX = 18.5


def ellipse(x, y, cx, cy, rx, ry):
    return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0


def in_blob(x, y):
    head = ellipse(x, y, CX, 9.0, 9.2, 7.8)
    belly = ellipse(x, y, CX, 24.0, 10.2, 6.2)
    waist = 12 <= y <= 21 and abs(x - CX) <= 4.6 + 0.16 * (y - 17) ** 2
    return head or belly or waist


image = [[CLEAR] * N for _ in range(N)]


def put(x, y, color):
    if 0 <= x < N and 0 <= y < N:
        image[y][x] = color


def layer(pixels, line):
    """Paints the pixels and, around them, a one pixel outline over whatever is below."""
    for (x, y) in pixels:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in pixels:
                put(x + dx, y + dy, line)
    for (x, y), color in pixels.items():
        put(x, y, color)


# The blob.
blob = {}
for y in range(1, N - 1):
    for x in range(1, N - 1):
        if not in_blob(x, y):
            continue
        color = BLOB
        if not in_blob(x + 2, y + 2) or not in_blob(x + 2, y):
            color = BLOB_SHADE
        elif not in_blob(x - 2, y - 1) or not in_blob(x - 1, y - 2):
            color = BLOB_LIGHT
        blob[(x, y)] = color
layer(blob, BLOB_LINE)
for p in ((13, 4), (14, 3), (14, 4), (12, 6)):
    put(*p, WHITE)

# Squeezed face: shut eyes and a strained mouth.
for x, y in ((13, 7), (14, 7), (15, 8), (16, 8), (13, 9), (14, 9),
             (23, 7), (24, 7), (21, 8), (22, 8), (23, 9), (24, 9),
             (16, 12), (17, 11), (18, 12), (19, 12), (20, 11), (21, 12)):
    put(x, y, EYE)
for x, y in ((12, 11), (13, 11), (24, 11), (25, 11)):
    put(x, y, BLUSH)

# A drop of sweat.
layer({(29, 3): SWEAT, (29, 4): SWEAT, (28, 5): SWEAT, (29, 5): WHITE, (28, 6): SWEAT, (29, 6): SWEAT},
      SWEAT_LINE)


def ring(x, y):
    """The round jaws: a flat ring around the waist."""
    outer = ellipse(x, y, CX, 17.0, 9.0, 4.6)
    inner = ellipse(x, y, CX, 16.2, 5.2, 1.9)
    return outer and not inner


# The handle, from the lower left up to the jaws.
handle = {}
for step in range(0, 12):
    x, y = 2 + step, 29 - step
    handle[(x, y)] = IRON
    handle[(x, y - 1)] = IRON_LIGHT
    handle[(x + 1, y)] = IRON_DARK
layer(handle, TOOL_LINE)

jaws = {}
for y in range(N):
    for x in range(N):
        if not ring(x, y):
            continue
        behind = y < 16
        if behind and in_blob(x, y) and abs(x - CX) < 4.8:
            continue  # the back of the ring is hidden by the blob
        if y >= 17 and 21 <= x <= 22:
            continue  # the gap between the two tips, in front
        color = GOLD
        if not ring(x, y + 1):
            color = GOLD_DARK
        elif not ring(x, y - 1):
            color = GOLD_LIGHT
        jaws[(x, y)] = color
layer(jaws, TOOL_LINE)

pommel = {(2, 28): GOLD_LIGHT, (3, 28): GOLD, (2, 29): GOLD, (3, 29): GOLD_DARK, (1, 29): GOLD,
          (2, 30): GOLD_DARK}
layer(pommel, TOOL_LINE)


def chunk(tag, data):
    return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data))


def write_png(path, scale, background=None):
    size = N * scale
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            color = image[y // scale][x // scale]
            if background and color[3] == 0:
                color = background
            raw += bytes(color)
    header = struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)
    with open(path, 'wb') as out:
        out.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', header)
                  + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))


if sys.argv[1] == 'preview':
    write_png(sys.argv[2], 16, (139, 139, 139, 255))
else:
    write_png(sys.argv[2], 1)
    write_png(sys.argv[3], 16)
