#version 330 core
// ==========================================================
// FILE: src/main/resources/shaders/phong.vert (Vertex Shader)
// Pass 2 (Lighting Pass) - dirender dari sudut pandang kamera
// ==========================================================
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoords;
out vec4 FragPosLightSpace;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;

void main() {
    FragPos = vec3(model * vec4(aPos, 1.0));
    Normal = mat3(transpose(inverse(model))) * aNormal; // Normal Matrix
    TexCoords = aTexCoords;
    FragPosLightSpace = lightSpaceMatrix * vec4(FragPos, 1.0); // Pos_light = P_light * V_light * M * Pos
    gl_Position = projection * view * vec4(FragPos, 1.0);
}
