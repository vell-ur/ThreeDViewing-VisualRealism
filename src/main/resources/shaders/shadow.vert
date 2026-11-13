#version 330 core
// Pass 1 (Depth Pass) - dirender dari sudut pandang SUMBER CAHAYA
layout (location = 0) in vec3 aPos;

uniform mat4 lightSpaceMatrix;
uniform mat4 model;

void main() {
    gl_Position = lightSpaceMatrix * model * vec4(aPos, 1.0);
}
