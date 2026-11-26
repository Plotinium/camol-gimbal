"""Enlarges a 32x32 RGBA PNG by whole pixels: preview on grey, 512 logo, 400 icon."""
import struct
import sys
import zlib

SIGNATURE = bytes([137, 80, 78, 71, 13, 10, 26, 10])


def read(path):
    data = open(path, 'rb').read()
    assert data[:8] == SIGNATURE
    pos, body = 8, b''
    while True:
        n = struct.unpack('>I', data[pos:pos + 4])[0]
        tag, part = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + n]
        if tag == b'IHDR':
            w, h, depth, kind = struct.unpack('>IIBB', part[:10])
            assert (depth, kind, part[12]) == (8, 6, 0)
        if tag == b'IDAT':
            body += part
        if tag == b'IEND':
            break
        pos += 12 + n
    raw = zlib.decompress(body)
    stride, rows, above = w * 4, [], bytearray(w * 4)
    for y in range(h):
        start = y * (stride + 1)
        method = raw[start]
        row = bytearray(raw[start + 1:start + 1 + stride])
        for i in range(stride):
            a = row[i - 4] if i >= 4 else 0
            b = above[i]
            c = above[i - 4] if i >= 4 else 0
            p = a + b - c
            paeth = a if abs(p - a) <= abs(p - b) and abs(p - a) <= abs(p - c) else b if abs(p - b) <= abs(p - c) else c
            row[i] = (row[i] + (0, a, b, (a + b) // 2, paeth)[method]) & 255
        rows.append([tuple(row[x * 4:x * 4 + 4]) for x in range(w)])
        above = row
    return rows


def chunk(tag, data):
    return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data))


def write(path, rows, scale, pad=0, background=None):
    size = len(rows) * scale + 2 * pad
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            inside = pad <= x < size - pad and pad <= y < size - pad
            colour = rows[(y - pad) // scale][(x - pad) // scale] if inside else (0, 0, 0, 0)
            if background and colour[3] == 0:
                colour = background
            raw += bytes(colour)
    with open(path, 'wb') as out:
        out.write(SIGNATURE + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0))
                  + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b''))


mode, source = sys.argv[1], sys.argv[2]
rows = read(source)
assert len(rows) == 32 and len(rows[0]) == 32
if mode == 'preview':
    write(sys.argv[3], rows, 16, background=(139, 139, 139, 255))
elif mode == 'icons':
    write(sys.argv[3], rows, 16)
    write(sys.argv[4], rows, 12, pad=8)
else:
    pixels = [p for row in rows for p in row]
    print('transparent', sum(p[3] == 0 for p in pixels), 'solid', sum(p[3] == 255 for p in pixels),
          'half transparent', sum(0 < p[3] < 255 for p in pixels), 'colours', len({p for p in pixels if p[3]}))
    for name in sys.argv[3:]:
        other = read(name)
        print(name, 'pixels that differ:', sum(rows[y][x] != other[y][x] and (rows[y][x][3] or other[y][x][3])
                                              for y in range(32) for x in range(32)))
