package com.grafkom.viewing3d;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Penampung geometri di CPU (daftar segitiga). Bertugas:
 *  - memastikan winding order CCW menghadap keluar (agar Back-Face Culling benar),
 *  - menghitung normal per-face (Flat Shading),
 *  - menghitung normal per-vertex dengan rata-rata normal semua face yang
 *    berbagi posisi vertex yang sama (Smooth Shading):
 *        N_vertex = (sum N_face_i) / || sum N_face_i ||
 */
public class MeshData {
    private record Key(long x, long y, long z) {
        static Key of(Vector3f p) {
            return new Key(Math.round(p.x * 10000.0), Math.round(p.y * 10000.0), Math.round(p.z * 10000.0));
        }
    }

    private final List<Vector3f[]> triPos = new ArrayList<>();
    private final List<Vector2f[]> triUv = new ArrayList<>();

    /** Tambah segitiga; urutan vertex dibalik otomatis bila arah normalnya berlawanan dengan 'outward'. */
    public void addTriangle(Vector3f a, Vector3f b, Vector3f c,
                            Vector2f ta, Vector2f tb, Vector2f tc, Vector3f outward) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        if (n.lengthSquared() < 1e-12f) {
            return; // segitiga degenerate (mis. di kutub bola)
        }
        if (n.dot(outward) < 0f) {
            Vector3f tp = b; b = c; c = tp;
            Vector2f tt = tb; tb = tc; tc = tt;
        }
        triPos.add(new Vector3f[]{new Vector3f(a), new Vector3f(b), new Vector3f(c)});
        triUv.add(new Vector2f[]{new Vector2f(ta), new Vector2f(tb), new Vector2f(tc)});
    }

    public void addQuad(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3,
                        Vector2f t0, Vector2f t1, Vector2f t2, Vector2f t3, Vector3f outward) {
        addTriangle(p0, p1, p2, t0, t1, t2, outward);
        addTriangle(p0, p2, p3, t0, t2, t3, outward);
    }

    /** Skala seragam sehingga dimensi terbesar = 2, pusat X/Z di 0, dan titik terendah di y = -1. */
    public void normalize() {
        Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);
        for (Vector3f[] t : triPos) {
            for (Vector3f p : t) {
                min.min(p);
                max.max(p);
            }
        }
        float ext = Math.max(max.x - min.x, Math.max(max.y - min.y, max.z - min.z));
        float s = 2.0f / ext;
        float cx = (min.x + max.x) * 0.5f, cz = (min.z + max.z) * 0.5f;
        for (Vector3f[] t : triPos) {
            for (Vector3f p : t) {
                p.set((p.x - cx) * s, (p.y - min.y) * s - 1.0f, (p.z - cz) * s);
            }
        }
    }

    public Mesh toMesh() {
        int triCount = triPos.size();
        float[] flat = new float[triCount * 3 * 8];
        float[] smooth = new float[triCount * 3 * 8];

        // Pass 1: akumulasi normal face (berbobot luas) pada setiap posisi vertex
        Map<Key, Vector3f> acc = new HashMap<>();
        Vector3f[] faceNormals = new Vector3f[triCount];
        for (int i = 0; i < triCount; i++) {
            Vector3f[] p = triPos.get(i);
            Vector3f fn = new Vector3f(p[1]).sub(p[0]).cross(new Vector3f(p[2]).sub(p[0]));
            faceNormals[i] = fn;
            for (Vector3f v : p) {
                acc.computeIfAbsent(Key.of(v), k -> new Vector3f()).add(fn);
            }
        }

        // Pass 2: tulis array vertex
        int o = 0;
        for (int i = 0; i < triCount; i++) {
            Vector3f[] p = triPos.get(i);
            Vector2f[] uv = triUv.get(i);
            Vector3f faceN = new Vector3f(faceNormals[i]).normalize();
            for (int k = 0; k < 3; k++) {
                Vector3f sn = new Vector3f(acc.get(Key.of(p[k])));
                if (sn.lengthSquared() < 1e-12f) {
                    sn.set(faceN);
                } else {
                    sn.normalize();
                    // pengaman: pada rusuk sangat tipis, rata-rata bisa berlawanan arah dengan face-nya
                    if (sn.dot(faceN) <= 0f) {
                        sn.set(faceN);
                    }
                }
                put(flat, o, p[k], faceN, uv[k]);
                put(smooth, o, p[k], sn, uv[k]);
                o += 8;
            }
        }
        return new Mesh(smooth, flat);
    }

    private static void put(float[] a, int o, Vector3f p, Vector3f n, Vector2f uv) {
        a[o] = p.x;     a[o + 1] = p.y; a[o + 2] = p.z;
        a[o + 3] = n.x; a[o + 4] = n.y; a[o + 5] = n.z;
        a[o + 6] = uv.x; a[o + 7] = uv.y;
    }
}
