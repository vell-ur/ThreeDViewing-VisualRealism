package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * Praktikum 3 - Java Texture Loader & UV Mapping (Indikator 4).
 * Membaca PNG/JPG dari classpath dengan STBImage lalu memuatnya ke GPU.
 */
public class Texture2D {
    private final int textureID;

    public Texture2D(String resourcePath) {
        byte[] bytes;
        try (InputStream in = Texture2D.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Texture tidak ditemukan di classpath: " + resourcePath);
            }
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca texture: " + resourcePath, e);
        }

        ByteBuffer fileData = memAlloc(bytes.length);
        fileData.put(bytes);
        fileData.flip();

        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Texture Filtering & Wrap Parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        stbi_set_flip_vertically_on_load(true); // v=0 di bawah, v=1 di atas (konvensi OpenGL)
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = stbi_load_from_memory(fileData, width, height, channels, 4);
            if (data != null) {
                glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width.get(0), height.get(0), 0,
                        GL_RGBA, GL_UNSIGNED_BYTE, data);
                glGenerateMipmap(GL_TEXTURE_2D);
                stbi_image_free(data);
            } else {
                System.err.println("Failed to load texture: " + resourcePath + " -> " + stbi_failure_reason());
            }
        } finally {
            memFree(fileData);
        }
    }

    /** true = Bilinear + Mipmapping (trilinear), false = Nearest Neighbor. */
    public void setMipmapFiltering(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (mipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void dispose() {
        glDeleteTextures(textureID);
    }
}
