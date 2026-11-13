package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Locale;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite (Minggu 11) - Rendering Faces pada Visual Realism.
 *
 * Kontrol:
 *   B = Phong <-> Blinn-Phong        F = Flat <-> Smooth Shading
 *   Z = Z-Buffer ON/OFF              C = Back-Face Culling ON/OFF
 *   T = Texture ON/OFF               N = Filter Nearest <-> Bilinear+Mipmap
 *   H = Shadow Mapping ON/OFF        M = Ganti model (Sphere/Torus/Knot/House)
 *   Spasi = pause rotasi objek       L = pause gerak cahaya
 *   Mouse drag / Panah = orbit kamera, Scroll / W,S = zoom, R = reset kamera, ESC = keluar
 */
public class VisualRealismSuite {
    private static final int WIN_W = 1280, WIN_H = 720;
    private static final String BASE_TITLE = "3D Visual Realism Suite";

    private static final float LIGHT_NEAR = 1.0f, LIGHT_FAR = 40.0f;
    private static final float GROUND_Y = -1.2f;
    private static final Vector3f LIGHT_TARGET = new Vector3f(0f, -0.5f, 0f);
    private static final Vector3f UP = new Vector3f(0f, 1f, 0f);

    private long window;
    private int fbWidth = WIN_W, fbHeight = WIN_H;

    // ---- status fitur ----
    private boolean blinnPhong = true;
    private boolean flatShading = false;
    private boolean depthTest = true;
    private boolean backFaceCulling = true;
    private boolean useTexture = true;
    private boolean mipmapFiltering = true;
    private boolean shadowsEnabled = true;
    private boolean autoRotate = true;
    private boolean lightAnimate = true;
    private int modelIndex = 0;

    private float modelAngle = 20f;
    private float lightAngle = 40f;
    private final Vector3f lightPos = new Vector3f();

    // ---- input ----
    private final OrbitCamera camera = new OrbitCamera();
    private boolean dragging = false;
    private double lastX, lastY;

    // ---- resources ----
    private ShaderProgram sceneShader, depthShader;
    private Texture2D objectTexture, groundTexture;
    private ShadowMapFBO shadowFBO;
    private Mesh[] models;
    private String[] modelNames;
    private Mesh groundMesh, lightMarker;

    private double fps = 0;

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    // =====================================================================
    // INISIALISASI
    // =====================================================================
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(WIN_W, WIN_H, BASE_TITLE, NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Gagal membuat window GLFW. Pastikan GPU/driver mendukung OpenGL 3.3+.");
        }

        glfwSetFramebufferSizeCallback(window, (w, width, height) -> {
            fbWidth = width;
            fbHeight = height;
        });
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                onKey(key);
            }
        });
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
            }
        });
        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (dragging) {
                camera.orbit(-(float) (x - lastX) * 0.4f, (float) (y - lastY) * 0.4f);
            }
            lastX = x;
            lastY = y;
        });
        glfwSetScrollCallback(window, (w, dx, dy) -> camera.zoom(-(float) dy * 0.6f));

        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vid != null) {
            glfwSetWindowPos(window, (vid.width() - WIN_W) / 2, (vid.height() - WIN_H) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            fbWidth = w.get(0);
            fbHeight = h.get(0);
        }

        glEnable(GL_MULTISAMPLE);

        // ---- load shader, tekstur, FBO ----
        sceneShader = new ShaderProgram("phong.vert", "phong.frag");
        depthShader = new ShaderProgram("shadow.vert", "shadow.frag");
        objectTexture = new Texture2D("/textures/object_texture.png");
        groundTexture = new Texture2D("/textures/ground_texture.png");
        shadowFBO = new ShadowMapFBO();

        // ---- buat mesh ----
        modelNames = new String[]{"Sphere", "Torus", "Torus Knot", "House"};
        models = new Mesh[]{
                MeshFactory.sphere(24, 16),
                MeshFactory.torus(32, 16, 1.0f, 0.42f),
                MeshFactory.torusKnot(160, 16, 0.55f),
                MeshFactory.house()
        };
        groundMesh = MeshFactory.groundPlane(9f, GROUND_Y, 2f);
        lightMarker = MeshFactory.sphere(12, 8);

        printHelp();
        updateTitle();
        glfwShowWindow(window);
    }

    private void printHelp() {
        System.out.println("=== " + BASE_TITLE + " ===");
        System.out.println("B  : Phong <-> Blinn-Phong");
        System.out.println("F  : Flat <-> Smooth Shading");
        System.out.println("Z  : Z-Buffer Depth Test ON/OFF");
        System.out.println("C  : Back-Face Culling ON/OFF");
        System.out.println("T  : Texture ON/OFF");
        System.out.println("N  : Texture Filter Nearest <-> Bilinear+Mipmap");
        System.out.println("H  : Shadow Mapping ON/OFF");
        System.out.println("M  : Ganti model (Sphere, Torus, Torus Knot, House)");
        System.out.println("Spasi : pause rotasi objek | L : pause gerak cahaya");
        System.out.println("Mouse drag / Panah : orbit kamera | Scroll / W,S : zoom | R : reset kamera | ESC : keluar");
    }

    private void onKey(int key) {
        switch (key) {
            case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
            case GLFW_KEY_B -> blinnPhong = !blinnPhong;
            case GLFW_KEY_F -> flatShading = !flatShading;
            case GLFW_KEY_Z -> depthTest = !depthTest;
            case GLFW_KEY_C -> backFaceCulling = !backFaceCulling;
            case GLFW_KEY_T -> useTexture = !useTexture;
            case GLFW_KEY_N -> {
                mipmapFiltering = !mipmapFiltering;
                objectTexture.setMipmapFiltering(mipmapFiltering);
                groundTexture.setMipmapFiltering(mipmapFiltering);
            }
            case GLFW_KEY_H -> shadowsEnabled = !shadowsEnabled;
            case GLFW_KEY_M -> modelIndex = (modelIndex + 1) % models.length;
            case GLFW_KEY_SPACE -> autoRotate = !autoRotate;
            case GLFW_KEY_L -> lightAnimate = !lightAnimate;
            case GLFW_KEY_R -> camera.reset();
            default -> {
                return;
            }
        }
        updateTitle();
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    private void updateTitle() {
        String t = String.format(Locale.US,
                "%s | %s | %s | %s | Z-Buffer:%s | Cull:%s | Tex:%s(%s) | Shadow:%s | FPS:%.0f",
                BASE_TITLE, modelNames[modelIndex],
                blinnPhong ? "Blinn-Phong" : "Phong",
                flatShading ? "Flat" : "Smooth",
                onOff(depthTest), onOff(backFaceCulling),
                onOff(useTexture), mipmapFiltering ? "Mipmap" : "Nearest",
                onOff(shadowsEnabled), fps);
        glfwSetWindowTitle(window, t);
    }

    // =====================================================================
    // LOOP UTAMA
    // =====================================================================
    private void loop() {
        double last = glfwGetTime();
        double fpsTimer = 0;
        int frames = 0;
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            double elapsed = now - last;
            float dt = (float) Math.min(elapsed, 0.1);
            last = now;

            glfwPollEvents();
            update(dt);
            render();
            glfwSwapBuffers(window);

            frames++;
            fpsTimer += elapsed;
            if (fpsTimer >= 1.0) {
                fps = frames / fpsTimer;
                frames = 0;
                fpsTimer = 0;
                updateTitle();
            }
        }
    }

    private void update(float dt) {
        if (autoRotate) modelAngle = (modelAngle + 30f * dt) % 360f;
        if (lightAnimate) lightAngle = (lightAngle + 35f * dt) % 360f;

        double a = Math.toRadians(lightAngle);
        lightPos.set(7.0f * (float) Math.cos(a), 6.5f, 7.0f * (float) Math.sin(a));

        float orbitSpeed = 90f * dt;
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS) camera.orbit(-orbitSpeed, 0f);
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) camera.orbit(orbitSpeed, 0f);
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) camera.orbit(0f, orbitSpeed * 0.7f);
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) camera.orbit(0f, -orbitSpeed * 0.7f);
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) camera.zoom(-6f * dt);
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) camera.zoom(6f * dt);
    }

    // =====================================================================
    // RENDER (Two-Pass Shadow Mapping)
    // =====================================================================
    private void render() {
        if (fbWidth <= 0 || fbHeight <= 0) {
            return; // window diminimalkan
        }

        Matrix4f lightView = new Matrix4f().lookAt(lightPos, LIGHT_TARGET, UP);
        Matrix4f lightProj = new Matrix4f().perspective((float) Math.toRadians(100.0), 1.0f, LIGHT_NEAR, LIGHT_FAR);
        Matrix4f lightSpace = new Matrix4f(lightProj).mul(lightView);

        Matrix4f objectModel = new Matrix4f().rotateY((float) Math.toRadians(modelAngle));
        Mesh object = models[modelIndex];

        // ---------- PASS 1: Depth Pass (sudut pandang cahaya) ----------
        if (shadowsEnabled) {
            shadowFBO.bindForWriting();
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
            glDisable(GL_CULL_FACE);
            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpace);
            depthShader.setMat4("model", objectModel);
            object.draw(false);
            shadowFBO.unbind(fbWidth, fbHeight);
        }

        // ---------- PASS 2: Lighting Pass (sudut pandang kamera) ----------
        glClearColor(0.10f, 0.12f, 0.17f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(backFaceCulling);

        float aspect = (float) fbWidth / (float) fbHeight;
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0), aspect, 0.5f, 80.0f);
        Matrix4f view = camera.getViewMatrix();

        sceneShader.use();
        sceneShader.setMat4("view", view);
        sceneShader.setMat4("projection", projection);
        sceneShader.setMat4("lightSpaceMatrix", lightSpace);
        sceneShader.setVec3("lightPos", lightPos);
        sceneShader.setVec3("viewPos", camera.getPosition());
        sceneShader.setVec3("lightColor", 1.0f, 1.0f, 1.0f);
        sceneShader.setBool("useBlinnPhong", blinnPhong);
        sceneShader.setBool("useShadows", shadowsEnabled);
        sceneShader.setBool("useTexture", useTexture);
        sceneShader.setBool("emissive", false);
        sceneShader.setFloat("lightNear", LIGHT_NEAR);
        sceneShader.setFloat("lightFar", LIGHT_FAR);
        sceneShader.setInt("textureSampler", 0);
        sceneShader.setInt("shadowMap", 1);

        shadowFBO.bindDepthTexture(1);

        // Lantai (penerima bayangan)
        sceneShader.setMat4("model", new Matrix4f());
        sceneShader.setVec3("objectColor", 0.55f, 0.60f, 0.70f);
        groundTexture.bind(0);
        groundMesh.draw(false);

        // Objek utama
        sceneShader.setMat4("model", objectModel);
        sceneShader.setVec3("objectColor", 0.90f, 0.55f, 0.20f);
        objectTexture.bind(0);
        object.draw(flatShading);

        // Penanda posisi sumber cahaya
        sceneShader.setBool("emissive", true);
        sceneShader.setMat4("model", new Matrix4f().translate(lightPos).scale(0.25f));
        lightMarker.draw(false);
        sceneShader.setBool("emissive", false);
    }

    // =====================================================================
    private void cleanup() {
        if (window != NULL) {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) {
            cb.free();
        }
    }
}
