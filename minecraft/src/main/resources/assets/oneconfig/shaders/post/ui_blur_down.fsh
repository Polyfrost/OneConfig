#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / OutSize;
    vec2 texel = 1.0 / InSize;
    vec4 c = texture(InSampler, uv + vec2(-texel.x, -texel.y))
        + texture(InSampler, uv + vec2(texel.x, -texel.y))
        + texture(InSampler, uv + vec2(-texel.x, texel.y))
        + texture(InSampler, uv + vec2(texel.x, texel.y));
    fragColor = vec4(c.rgb * 0.25, 1.0);
}
