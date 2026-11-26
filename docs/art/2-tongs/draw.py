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

CX = 19.5


def ellipse(x, y, cx, cy, rx, ry):
    return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0


def in_blob(x, y):
    head = ellipse(x, y, CX, 8.0, 8.7, 7.25)
    belly = ellipse(x, y, CX, 21.2, 9.2, 5.35)
    waist = 11 <= y <= 18 and abs(x - CX) <= 4.2 + 0.18 * (y - 15) ** 2
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
for p in ((14, 3), (15, 2), (15, 3), (13, 5)):
    put(*p, WHITE)

# Squeezed face: eyes shut like a "greater than" and a "less than", and a strained mouth.
FACE = [
    "##........##",
    "..##....##..",
    "##........##",
]
for row, line in enumerate(FACE):
    for col, mark in enumerate(line):
        if mark == "#":
            put(14 + col, 4 + row, EYE)
MOUTH = [
    ".#..#.",
    "#.##.#",
]
for row, line in enumerate(MOUTH):
    for col, mark in enumerate(line):
        if mark == "#":
            put(17 + col, 8 + row, EYE)
for x, y in ((13, 8), (14, 8), (25, 8), (26, 8)):
    put(x, y, BLUSH)

# A drop of sweat.
layer({(29, 2): SWEAT, (29, 3): SWEAT, (28, 4): SWEAT, (29, 4): WHITE, (28, 5): SWEAT, (29, 5): SWEAT},
      SWEAT_LINE)


def segment(p, q, radius):
    """Pixels within a distance of the line from p to q, with the side each one is on."""
    (ax, ay), (bx, by) = p, q
    dx, dy = bx - ax, by - ay
    length = dx * dx + dy * dy
    for y in range(N):
        for x in range(N):
            k = max(0.0, min(1.0, ((x - ax) * dx + (y - ay) * dy) / length))
            ex, ey = x - (ax + k * dx), y - (ay + k * dy)
            if ex * ex + ey * ey <= radius * radius:
                yield (x, y), k, ex + ey


# Two nearly parallel handles that go to the lower left, each ending in a leather grip.
GRIP = (150, 72, 56, 255)
GRIP_LIGHT = (196, 108, 82, 255)
GRIP_DARK = (104, 46, 40, 255)
for start, end in (((10.0, 16.0), (1.6, 24.4)), ((12.6, 19.4), (5.6, 29.4))):
    handle = {}
    for p, k, side in segment(start, end, 1.35):
        if k > 0.40:
            handle[p] = GRIP_LIGHT if side < -0.6 else GRIP_DARK if side > 0.6 else GRIP
        else:
            handle[p] = IRON_LIGHT if side < -0.6 else IRON_DARK if side > 0.6 else IRON
    layer(handle, TOOL_LINE)


def curve(start, control, end, radius):
    """Pixels within a distance of a curved line."""
    points = []
    for i in range(121):
        k = i / 120
        points.append(tuple((1 - k) ** 2 * s + 2 * (1 - k) * k * c + k ** 2 * e
                            for s, c, e in zip(start, control, end)))
    for y in range(N):
        for x in range(N):
            if any((x - px) ** 2 + (y - py) ** 2 <= radius ** 2 for px, py in points):
                yield (x, y)


# The two jaws: one over the waist and one under it, both leaving the pivot and meeting on the other side.
for control in ((19.5, 9.6), (19.5, 23.6)):
    jaw = set(curve((11.8, 17.0), control, (27.6, 15.8), 1.25))
    painted = {}
    for x, y in jaw:
        painted[(x, y)] = (GOLD_LIGHT if (x, y - 1) not in jaw
                           else GOLD_DARK if (x, y + 1) not in jaw else GOLD)
    layer(painted, TOOL_LINE)

# The knuckle where the two tips lock together.
knuckle = {}
for y in range(N):
    for x in range(N):
        if ellipse(x, y, 27.6, 15.8, 2.1, 2.6):
            knuckle[(x, y)] = GOLD_LIGHT if y <= 14 else GOLD_DARK if y >= 18 else GOLD
for x in range(26, 30):
    knuckle[(x, 16)] = TOOL_LINE
layer(knuckle, TOOL_LINE)

# The pivot that holds the two jaws together, with its rivet.
pivot = {}
for y in range(N):
    for x in range(N):
        if ellipse(x, y, 11.5, 17.5, 2.4, 2.4):
            pivot[(x, y)] = GOLD_LIGHT if x + y <= 27 else GOLD_DARK if x + y >= 31 else GOLD
pivot[(11, 17)] = IRON_LIGHT
pivot[(12, 17)] = IRON
pivot[(11, 18)] = IRON
pivot[(12, 18)] = IRON_DARK
layer(pivot, TOOL_LINE)


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


def write_padded(path, scale, pad):
    """The drawing enlarged by a whole factor, with a clear border around it."""
    size = N * scale + 2 * pad
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            inside = pad <= x < size - pad and pad <= y < size - pad
            raw += bytes(image[(y - pad) // scale][(x - pad) // scale] if inside else CLEAR)
    header = struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)
    with open(path, 'wb') as out:
        out.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', header)
                  + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))


if sys.argv[1] == 'preview':
    write_png(sys.argv[2], 16, (139, 139, 139, 255))
    write_png(sys.argv[3], 2, (139, 139, 139, 255))
else:
    write_png(sys.argv[2], 1)
    write_png(sys.argv[3], 16)
    write_padded(sys.argv[4], 12, 8)
