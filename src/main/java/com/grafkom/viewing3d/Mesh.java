package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL33.*;

/**
 * Mesh di GPU. Layout vertex (interleaved, 8 float): pos(3) normal(3) uv(2).
 * Disimpan dalam DUA VAO/VBO: satu dengan normal per-face (Flat Shading) dan
 * satu dengan normal per-vertex hasil rata-rata (Smooth Shading).
 */
public class Mesh {
    private static final int FLOATS_PER_VERTEX = 8;

    private final int vaoSmooth, vboSmooth, vaoFlat, vboFlat;
    private final int vertexCount;

    public Mesh(float[] smoothData, float[] flatData) {
        vertexCount = smoothData.length / FLOATS_PER_VERTEX;
        int[] s = upload(smoothData);
        int[] f = upload(flatData);
        vaoSmooth = s[0]; vboSmooth = s[1];
        vaoFlat = f[0]; vboFlat = f[1];
    }

    private static int[] upload(float[] data) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return new int[]{vao, vbo};
    }

    public void draw(boolean flatShading) {
        glBindVertexArray(flatShading ? vaoFlat : vaoSmooth);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public int getTriangleCount() { return vertexCount / 3; }

    public void dispose() {
        glDeleteVertexArrays(vaoSmooth);
        glDeleteVertexArrays(vaoFlat);
        glDeleteBuffers(vboSmooth);
        glDeleteBuffers(vboFlat);
    }
}
