package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit: mengelilingi target dengan yaw, pitch, dan jarak. */
public class OrbitCamera {
    private final Vector3f target = new Vector3f(0f, -0.2f, 0f);
    private float yaw = 25f, pitch = 25f, distance = 7.5f;

    public void reset() {
        yaw = 25f;
        pitch = 25f;
        distance = 7.5f;
    }

    public void orbit(float deltaYawDeg, float deltaPitchDeg) {
        yaw += deltaYawDeg;
        pitch = Math.max(-80f, Math.min(85f, pitch + deltaPitchDeg));
    }

    public void zoom(float delta) {
        distance = Math.max(3.0f, Math.min(30.0f, distance + delta));
    }

    public Vector3f getPosition() {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f(
                target.x + distance * (float) (Math.cos(p) * Math.sin(y)),
                target.y + distance * (float) Math.sin(p),
                target.z + distance * (float) (Math.cos(p) * Math.cos(y)));
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), target, new Vector3f(0f, 1f, 0f));
    }
}
