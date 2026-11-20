// 生成 Android 启动图标（纯 Node，无第三方依赖）：深蓝圆角底 + 白色 "E" 字标
import { deflateSync } from 'node:zlib'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const RES = join(HERE, '..', 'res')

const DENSITIES = [
  ['mipmap-mdpi', 48],
  ['mipmap-hdpi', 72],
  ['mipmap-xhdpi', 96],
  ['mipmap-xxhdpi', 144],
  ['mipmap-xxxhdpi', 192]
]

const BG = [31, 78, 140, 255]
const FG = [255, 255, 255, 255]

function crc32(buf) {
  let c = 0xffffffff
  for (let i = 0; i < buf.length; i++) {
    c ^= buf[i]
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
  }
  return (c ^ 0xffffffff) >>> 0
}

function chunk(type, data) {
  const len = Buffer.alloc(4)
  len.writeUInt32BE(data.length, 0)
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data])
  const crc = Buffer.alloc(4)
  crc.writeUInt32BE(crc32(body), 0)
  return Buffer.concat([len, body, crc])
}

function png(size, pixels) {
  const ihdr = Buffer.alloc(13)
  ihdr.writeUInt32BE(size, 0)
  ihdr.writeUInt32BE(size, 4)
  ihdr[8] = 8
  ihdr[9] = 6
  ihdr[10] = 0
  ihdr[11] = 0
  ihdr[12] = 0

  const raw = Buffer.alloc(size * (size * 4 + 1))
  for (let y = 0; y < size; y++) {
    const row = y * (size * 4 + 1)
    raw[row] = 0
    pixels.copy(raw, row + 1, y * size * 4, (y + 1) * size * 4)
  }
  const idat = deflateSync(raw, { level: 9 })

  const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])
  return Buffer.concat([sig, chunk('IHDR', ihdr), chunk('IDAT', idat), chunk('IEND', Buffer.alloc(0))])
}

function render(size) {
  const px = Buffer.alloc(size * size * 4)
  const radius = size * 0.19
  const put = (x, y, color) => {
    const i = (y * size + x) * 4
    px[i] = color[0]
    px[i + 1] = color[1]
    px[i + 2] = color[2]
    px[i + 3] = color[3]
  }
  const inside = (x, y) => {
    const cx = Math.min(x, size - 1 - x)
    const cy = Math.min(y, size - 1 - y)
    if (cx >= radius || cy >= radius) return true
    const dx = radius - cx
    const dy = radius - cy
    return dx * dx + dy * dy <= radius * radius
  }
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      if (inside(x, y)) put(x, y, BG)
    }
  }

  // 白色 E：竖笔画 + 三条横笔画（按 192 基准等比缩放）
  const s = size / 192
  const rect = (x0, y0, x1, y1) => {
    for (let y = Math.round(y0 * s); y < Math.round(y1 * s); y++) {
      for (let x = Math.round(x0 * s); x < Math.round(x1 * s); x++) {
        if (x >= 0 && y >= 0 && x < size && y < size) put(x, y, FG)
      }
    }
  }
  rect(56, 44, 80, 148)
  rect(56, 44, 140, 68)
  rect(56, 86, 122, 110)
  rect(56, 124, 140, 148)

  return png(size, px)
}

mkdirSync(RES, { recursive: true })
for (const [dir, size] of DENSITIES) {
  const target = join(RES, dir)
  mkdirSync(target, { recursive: true })
  writeFileSync(join(target, 'ic_launcher.png'), render(size))
  console.log(`icon ${dir}/ic_launcher.png (${size}x${size})`)
}
