const C1 = 0xcc9e2d51;
const C2 = 0x1b873593;
const R1 = 15;
const R2 = 13;
const M = 5;
const N = 0xe6546b64;

function rotl(x: number, r: number): number {
  return (x << r) | (x >>> (32 - r));
}

function fmix(h: number): number {
  h ^= h >>> 16;
  h = Math.imul(h, 0x85ebca6b);
  h ^= h >>> 13;
  h = Math.imul(h, 0xc2b2ae35);
  h ^= h >>> 16;
  return h >>> 0;
}

/** murmur3_32(utf8) — mesma família usada em CnpjPartitioner (Guava). */
export function murmur3_32(input: string, seed = 0): number {
  const data = new TextEncoder().encode(input);
  let h1 = seed >>> 0;
  const nblocks = Math.floor(data.length / 4);

  for (let i = 0; i < nblocks; i++) {
    const i4 = i * 4;
    let k1 =
      data[i4] |
      (data[i4 + 1] << 8) |
      (data[i4 + 2] << 16) |
      (data[i4 + 3] << 24);

    k1 = Math.imul(k1, C1);
    k1 = rotl(k1, R1);
    k1 = Math.imul(k1, C2);

    h1 ^= k1;
    h1 = rotl(h1, R2);
    h1 = Math.imul(h1, M) + N;
  }

  const tail = nblocks * 4;
  let k1 = 0;
  switch (data.length & 3) {
    case 3:
      k1 ^= data[tail + 2] << 16;
      k1 ^= data[tail + 1] << 8;
      k1 ^= data[tail];
      k1 = Math.imul(k1, C1);
      k1 = rotl(k1, R1);
      k1 = Math.imul(k1, C2);
      h1 ^= k1;
      break;
    case 2:
      k1 ^= data[tail + 1] << 8;
      k1 ^= data[tail];
      k1 = Math.imul(k1, C1);
      k1 = rotl(k1, R1);
      k1 = Math.imul(k1, C2);
      h1 ^= k1;
      break;
    case 1:
      k1 ^= data[tail];
      k1 = Math.imul(k1, C1);
      k1 = rotl(k1, R1);
      k1 = Math.imul(k1, C2);
      h1 ^= k1;
      break;
  }

  h1 ^= data.length;
  h1 = fmix(h1);
  return h1 >>> 0;
}

export function bucketForCnpjBasico(cnpjBasico: string): number {
  const hash = murmur3_32(cnpjBasico);
  return ((hash % 256) + 256) % 256;
}

export function bucketLabel(cnpjBasico: string | undefined): string {
  if (!cnpjBasico) return '—';
  return String(bucketForCnpjBasico(cnpjBasico)).padStart(3, '0');
}
