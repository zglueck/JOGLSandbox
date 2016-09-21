#version 150 core
in vec3 Color;
in vec2 TexCoord;
uniform sampler2D tex;
void main()
{
    gl_FragColor = vec4(Color, 1.0) * texture(tex, TexCoord);
}
