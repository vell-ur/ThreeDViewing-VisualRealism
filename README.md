# 3D Visual Realism Suite — Grafika Komputer Minggu 11

Project **Maven** (Java 17+, LWJGL 3.3.3, JOML, GLSL 330 core) yang memperluas project `ThreeDViewing`
(package `com.grafkom.viewing3d`). Semua kelas praktikum (`HSRManager`, `Texture2D`, `ShadowMapFBO`) dan
shader (`phong.vert`, `phong.frag`) mengikuti modul ajar, lalu dilengkapi menjadi aplikasi utuh.

## Cara menjalankan di NetBeans
1. Pastikan JDK **17 atau lebih baru** dan **koneksi internet** (Maven mengunduh LWJGL + JOML sekali saja).
2. NetBeans → **File → Open Project…** → pilih folder `ThreeDViewing` (ikon cangkir Maven) → Open.
3. Klik kanan project → **Clean and Build** (pertama kali: tunggu unduhan dependency selesai).
4. Klik **Run Project (F6)**. Main class: `com.grafkom.viewing3d.Main` (sudah diset di `pom.xml`).

Alternatif terminal: `mvn compile exec:java`

Catatan: native LWJGL dipilih otomatis sesuai OS (Windows/Linux/macOS, Intel/ARM). Di macOS program
menjalankan ulang dirinya dengan `-XstartOnFirstThread` secara otomatis. Perlu GPU/driver **OpenGL 3.3+**.

## Kontrol
| Tombol | Fungsi (Indikator) |
|---|---|
| **B** | Phong ↔ Blinn-Phong (1) |
| **F** | Flat ↔ Smooth Shading (2) |
| **Z** | Z-Buffer Depth Test ON/OFF (3) |
| **C** | Back-Face Culling ON/OFF (3) |
| **T** | Texture ON/OFF (4) |
| **N** | Filter Nearest ↔ Bilinear + Mipmap (4) |
| **H** | Shadow Mapping ON/OFF (5) |
| **M** | Ganti model: Sphere → Torus → Torus Knot → House |
| Spasi / **L** | Pause rotasi objek / pause gerak cahaya |
| Mouse drag, panah | Orbit kamera |
| Scroll, **W / S** | Zoom |
| **R** / **Esc** | Reset kamera / keluar |

Status semua fitur + **FPS** tampil di judul window (berguna untuk bagian laporan "kinerja rendering").

## Pemetaan ke spesifikasi & rubrik
| Indikator | Implementasi |
|---|---|
| 1. Shading Models | `phong.frag`: ambient + diffuse + specular; Phong (`reflect`, n=16) dan Blinn-Phong (halfway vector, n=32), per-pixel |
| 2. Flat vs Smooth | `MeshData.toMesh()` membuat dua VBO: normal per-face dan normal per-vertex (rata-rata normal face, `N = ΣN / ‖ΣN‖`, vertex di-weld berdasarkan posisi sehingga tanpa seam) |
| 3. HSR | `HSRManager` (`GL_DEPTH_TEST` + `GL_LESS`, `GL_CULL_FACE` + `GL_BACK` + `GL_CCW`); zNear/zFar = 0.5/80 untuk mengurangi Z-fighting. Winding semua mesh dijamin CCW menghadap keluar |
| 4. Texture Mapping | `Texture2D` (STBImage, flip vertikal, `GL_REPEAT`, mipmap); UV pada semua mesh; toggle Nearest vs `GL_LINEAR_MIPMAP_LINEAR` |
| 5. Shadow Mapping | `ShadowMapFBO` 2048×2048 depth texture; pass 1 dari sudut cahaya, pass 2 dari kamera; PCF 3×3; slope-scaled bias `max(0.15·(1−N·L), 0.03)` (satuan dunia, depth dilinearisasi) untuk mencegah Shadow Acne tanpa Peter Panning; shadow factor 0.2 |

## Saran pengujian untuk screenshot laporan
- **Phong vs Blinn-Phong**: Sphere, tekan **T** (matikan tekstur) lalu **B**; bandingkan bentuk kilauan.
- **Flat vs Smooth**: Sphere atau Torus, tekan **F**.
- **HSR**: Torus Knot, tekan **Z** (matikan) dan **C**; amati tumpang-tindih permukaan.
- **Texture filtering**: lantai dari sudut rendah (drag mouse ke bawah), tekan **N**; moiré muncul pada mode Nearest.
- **Shadow**: tekan **H** untuk membandingkan ON/OFF.

## Struktur
```
src/main/java/com/grafkom/viewing3d/
  Main.java, VisualRealismSuite.java   # window, input, loop, 2-pass render
  HSRManager.java, Texture2D.java, ShadowMapFBO.java   # kelas praktikum 2-4
  ShaderProgram.java, OrbitCamera.java
  Mesh.java, MeshData.java, MeshFactory.java           # geometri, normal, UV
src/main/resources/shaders/   phong.vert phong.frag shadow.vert shadow.frag
src/main/resources/textures/  object_texture.png ground_texture.png
```
