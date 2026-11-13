package com.grafkom.viewing3d;

import org.joml.Vector2f;
import org.joml.Vector3f;

/** Pembuat model 3D prosedural: Sphere, Torus, Torus Knot, House, dan Ground Plane. */
public final class MeshFactory {
    private MeshFactory() {}

    private static final double TWO_PI = Math.PI * 2.0;

    /** Menjahit grid parametrik (U x V) menjadi segitiga. 'out' = arah keluar permukaan per vertex. */
    private static void addGrid(MeshData md, Vector3f[][] p, Vector3f[][] out, Vector2f[][] uv) {
        int nu = p.length - 1, nv = p[0].length - 1;
        for (int i = 0; i < nu; i++) {
            for (int j = 0; j < nv; j++) {
                int i1 = i + 1, j1 = j + 1;
                Vector3f h1 = new Vector3f(out[i][j]).add(out[i1][j]).add(out[i][j1]);
                Vector3f h2 = new Vector3f(out[i][j1]).add(out[i1][j]).add(out[i1][j1]);
                md.addTriangle(p[i][j], p[i1][j], p[i][j1], uv[i][j], uv[i1][j], uv[i][j1], h1);
                md.addTriangle(p[i][j1], p[i1][j], p[i1][j1], uv[i][j1], uv[i1][j], uv[i1][j1], h2);
            }
        }
    }

    public static Mesh sphere(int segments, int rings) {
        MeshData md = new MeshData();
        Vector3f[][] p = new Vector3f[rings + 1][segments + 1];
        Vector3f[][] n = new Vector3f[rings + 1][segments + 1];
        Vector2f[][] uv = new Vector2f[rings + 1][segments + 1];
        for (int i = 0; i <= rings; i++) {
            double theta = Math.PI * i / rings;
            for (int j = 0; j <= segments; j++) {
                double phi = TWO_PI * j / segments;
                Vector3f d = new Vector3f((float) (Math.sin(theta) * Math.cos(phi)),
                        (float) Math.cos(theta), (float) (Math.sin(theta) * Math.sin(phi)));
                p[i][j] = d;
                n[i][j] = new Vector3f(d);
                uv[i][j] = new Vector2f((float) j / segments * 2f, 1f - (float) i / rings);
            }
        }
        addGrid(md, p, n, uv);
        md.normalize();
        return md.toMesh();
    }

    public static Mesh torus(int majorSeg, int minorSeg, float majorR, float minorR) {
        MeshData md = new MeshData();
        Vector3f[][] p = new Vector3f[majorSeg + 1][minorSeg + 1];
        Vector3f[][] n = new Vector3f[majorSeg + 1][minorSeg + 1];
        Vector2f[][] uv = new Vector2f[majorSeg + 1][minorSeg + 1];
        for (int i = 0; i <= majorSeg; i++) {
            double u = TWO_PI * i / majorSeg;
            for (int j = 0; j <= minorSeg; j++) {
                double v = TWO_PI * j / minorSeg;
                float cu = (float) Math.cos(u), su = (float) Math.sin(u);
                float cv = (float) Math.cos(v), sv = (float) Math.sin(v);
                p[i][j] = new Vector3f((majorR + minorR * cv) * cu, minorR * sv, (majorR + minorR * cv) * su);
                n[i][j] = new Vector3f(cv * cu, sv, cv * su);
                uv[i][j] = new Vector2f((float) i / majorSeg * 4f, (float) j / minorSeg);
            }
        }
        addGrid(md, p, n, uv);
        md.normalize();
        return md.toMesh();
    }

    /** Torus knot (p=2, q=3 / trefoil) berupa tabung yang mengikuti kurva. */
    public static Mesh torusKnot(int tubularSeg, int radialSeg, float tubeRadius) {
        final int kp = 2, kq = 3;
        MeshData md = new MeshData();
        Vector3f[][] p = new Vector3f[tubularSeg + 1][radialSeg + 1];
        Vector3f[][] n = new Vector3f[tubularSeg + 1][radialSeg + 1];
        Vector2f[][] uv = new Vector2f[tubularSeg + 1][radialSeg + 1];
        double eps = 1e-3;
        for (int i = 0; i <= tubularSeg; i++) {
            double t = TWO_PI * i / tubularSeg;
            Vector3f c0 = knotPoint(t, kp, kq);
            Vector3f c1 = knotPoint(t + eps, kp, kq);
            Vector3f cm = knotPoint(t - eps, kp, kq);
            Vector3f tan = new Vector3f(c1).sub(cm).normalize();
            Vector3f acc = new Vector3f(c1).add(cm).sub(new Vector3f(c0).mul(2f));
            Vector3f nrm = new Vector3f(acc).sub(new Vector3f(tan).mul(acc.dot(tan))).normalize();
            Vector3f bin = new Vector3f(tan).cross(nrm).normalize();
            for (int j = 0; j <= radialSeg; j++) {
                double v = TWO_PI * j / radialSeg;
                float cv = (float) Math.cos(v), sv = (float) Math.sin(v);
                Vector3f dir = new Vector3f(nrm).mul(cv).add(new Vector3f(bin).mul(sv));
                p[i][j] = new Vector3f(c0).add(new Vector3f(dir).mul(tubeRadius));
                n[i][j] = dir;
                uv[i][j] = new Vector2f((float) i / tubularSeg * 16f, (float) j / radialSeg * 2f);
            }
        }
        addGrid(md, p, n, uv);
        md.normalize();
        return md.toMesh();
    }

    private static Vector3f knotPoint(double t, int pp, int qq) {
        double r = Math.cos(qq * t) + 2.0;
        float x = (float) (r * Math.cos(pp * t));
        float y = (float) (r * Math.sin(pp * t));
        float z = (float) (-Math.sin(qq * t));
        return new Vector3f(x, z, y); // sumbu Y dijadikan "atas"
    }

    /** Rumah 3D tertutup: dinding, atap pelana bertebal, dan alas. */
    public static Mesh house() {
        MeshData md = new MeshData();
        float hw = 1.0f, hd = 0.8f, wallH = 1.2f, apex = 1.95f;

        // Dinding depan & belakang (persegi + segitiga gable)
        for (int s = -1; s <= 1; s += 2) {
            float z = s * hd;
            Vector3f out = new Vector3f(0, 0, s);
            md.addQuad(new Vector3f(-hw, 0, z), new Vector3f(hw, 0, z), new Vector3f(hw, wallH, z), new Vector3f(-hw, wallH, z),
                    new Vector2f(0, 0), new Vector2f(2f * hw, 0), new Vector2f(2f * hw, wallH), new Vector2f(0, wallH), out);
            md.addTriangle(new Vector3f(-hw, wallH, z), new Vector3f(hw, wallH, z), new Vector3f(0, apex, z),
                    new Vector2f(0, wallH), new Vector2f(2f * hw, wallH), new Vector2f(hw, apex), out);
        }
        // Dinding kiri & kanan
        for (int s = -1; s <= 1; s += 2) {
            float x = s * hw;
            md.addQuad(new Vector3f(x, 0, -hd), new Vector3f(x, 0, hd), new Vector3f(x, wallH, hd), new Vector3f(x, wallH, -hd),
                    new Vector2f(0, 0), new Vector2f(2f * hd, 0), new Vector2f(2f * hd, wallH), new Vector2f(0, wallH),
                    new Vector3f(s, 0, 0));
        }
        // Alas
        md.addQuad(new Vector3f(-hw, 0, -hd), new Vector3f(hw, 0, -hd), new Vector3f(hw, 0, hd), new Vector3f(-hw, 0, hd),
                new Vector2f(0, 0), new Vector2f(2f * hw, 0), new Vector2f(2f * hw, 2f * hd), new Vector2f(0, 2f * hd),
                new Vector3f(0, -1, 0));

        // Atap pelana (tebal 0.08) dengan overhang
        float ex = 1.1f, ez = 0.9f, eaveY = 1.15f, ridgeY = 2.0f, th = 0.08f;
        for (int s = -1; s <= 1; s += 2) {
            // s = -1 : sisi kiri (x negatif), s = +1 : sisi kanan
            float exs = s * ex;
            Vector3f upN = new Vector3f(s * (ridgeY - eaveY), ex, 0); // normal atas lereng
            Vector3f dnN = new Vector3f(upN).negate();
            // permukaan atas
            md.addQuad(new Vector3f(exs, eaveY, -ez), new Vector3f(exs, eaveY, ez), new Vector3f(0, ridgeY, ez), new Vector3f(0, ridgeY, -ez),
                    new Vector2f(0, 0), new Vector2f(2f * ez, 0), new Vector2f(2f * ez, 1.4f), new Vector2f(0, 1.4f), upN);
            // permukaan bawah
            md.addQuad(new Vector3f(exs, eaveY - th, -ez), new Vector3f(exs, eaveY - th, ez), new Vector3f(0, ridgeY - th, ez), new Vector3f(0, ridgeY - th, -ez),
                    new Vector2f(0, 0), new Vector2f(2f * ez, 0), new Vector2f(2f * ez, 1.4f), new Vector2f(0, 1.4f), dnN);
            // tepi depan & belakang atap
            for (int zs = -1; zs <= 1; zs += 2) {
                float z = zs * ez;
                md.addQuad(new Vector3f(exs, eaveY, z), new Vector3f(0, ridgeY, z), new Vector3f(0, ridgeY - th, z), new Vector3f(exs, eaveY - th, z),
                        new Vector2f(0, 0), new Vector2f(1.4f, 0), new Vector2f(1.4f, 0.1f), new Vector2f(0, 0.1f), new Vector3f(0, 0, zs));
            }
            // tepi eave (sisi luar)
            md.addQuad(new Vector3f(exs, eaveY, -ez), new Vector3f(exs, eaveY, ez), new Vector3f(exs, eaveY - th, ez), new Vector3f(exs, eaveY - th, -ez),
                    new Vector2f(0, 0), new Vector2f(2f * ez, 0), new Vector2f(2f * ez, 0.1f), new Vector2f(0, 0.1f), new Vector3f(s, 0, 0));
        }
        md.normalize();
        return md.toMesh();
    }

    /** Bidang lantai horizontal pada ketinggian y dengan UV berulang tiap 'tileSize' satuan. */
    public static Mesh groundPlane(float halfSize, float y, float tileSize) {
        MeshData md = new MeshData();
        float h = halfSize, t = halfSize / tileSize;
        md.addQuad(new Vector3f(-h, y, -h), new Vector3f(-h, y, h), new Vector3f(h, y, h), new Vector3f(h, y, -h),
                new Vector2f(-t, -t), new Vector2f(-t, t), new Vector2f(t, t), new Vector2f(t, -t),
                new Vector3f(0, 1, 0));
        return md.toMesh(); // tanpa normalize(): posisi ditetapkan langsung
    }
}
