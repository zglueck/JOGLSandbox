#version 150 core
in vec3 position;
in vec3 color;
in vec2 texCoord;
uniform mat4 mvp;
out vec3 Color;
out vec2 TexCoord;
void main()
{
    Color = color;
    TexCoord = texCoord;
    gl_Position = mvp * vec4(position, 1.0);
}
