package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

/** Membungkus kompilasi, linking, dan pengaturan uniform GLSL shader. */
public class ShaderProgram {
    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();
    private final float[] matBuf = new float[16];

    public ShaderProgram(String vertexFile, String fragmentFile) {
        int vs = compile(GL_VERTEX_SHADER, load(vertexFile), vertexFile);
        int fs = compile(GL_FRAGMENT_SHADER, load(fragmentFile), fragmentFile);
        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Link shader gagal (" + vertexFile + " + " + fragmentFile + "):\n"
                    + glGetProgramInfoLog(programId));
        }
        glDetachShader(programId, vs);
        glDetachShader(programId, fs);
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static String load(String file) {
        String path = "/shaders/" + file;
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Shader tidak ditemukan di classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca shader: " + path, e);
        }
    }

    private static int compile(int type, String source, String name) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Kompilasi shader gagal (" + name + "):\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    public void use() { glUseProgram(programId); }

    private int loc(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setInt(String name, int v) { glUniform1i(loc(name), v); }
    public void setBool(String name, boolean v) { glUniform1i(loc(name), v ? 1 : 0); }
    public void setFloat(String name, float v) { glUniform1f(loc(name), v); }
    public void setVec3(String name, Vector3f v) { glUniform3f(loc(name), v.x, v.y, v.z); }
    public void setVec3(String name, float x, float y, float z) { glUniform3f(loc(name), x, y, z); }
    public void setMat4(String name, Matrix4f m) { glUniformMatrix4fv(loc(name), false, m.get(matBuf)); }

    public void dispose() { glDeleteProgram(programId); }
}
