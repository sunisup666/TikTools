"""Read public DEX method signatures for local compatibility work, without decompiling bodies."""
import argparse
from pathlib import Path
import struct
import zipfile


def signatures(data, terms):
    def u32(pos): return struct.unpack_from('<I', data, pos)[0]
    def leb(pos):
        value = shift = 0
        while True:
            b = data[pos]; pos += 1; value |= (b & 127) << shift
            if not b & 128: return value, pos
            shift += 7
    count, offset = u32(56), u32(60)
    strings = []
    for i in range(count):
        _, start = leb(u32(offset + i * 4))
        strings.append(data[start:data.index(b'\0', start)].decode('utf-8', errors='replace'))
    types = [strings[u32(u32(68) + i * 4)] for i in range(u32(64))]
    for i in range(u32(80)):
        owner, field_type, name = struct.unpack_from('<HHI', data, u32(84) + i * 8)
        if any(term in types[owner] for term in terms):
            print(types[owner], 'FIELD', strings[name], types[field_type])
    protos = []
    for i in range(u32(72)):
        pos = u32(76) + i * 12
        ret, params = types[u32(pos + 4)], u32(pos + 8)
        args = [] if not params else [types[struct.unpack_from('<H', data, params + 4 + j * 2)[0]] for j in range(u32(params))]
        protos.append('(' + ','.join(args) + ') -> ' + ret)
    for i in range(u32(88)):
        owner, proto, name = struct.unpack_from('<HHI', data, u32(92) + i * 8)
        cls = types[owner]
        if any(term in cls for term in terms):
            print(cls, strings[name], protos[proto])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('apk', type=Path); parser.add_argument('terms', nargs='+'); args = parser.parse_args()
    with zipfile.ZipFile(args.apk) as archive:
        for name in archive.namelist():
            if name.endswith('.dex'): signatures(archive.read(name), args.terms)
