#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float roundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

vec3 frosted(vec2 frag, vec2 texel) {
    vec3 c = textureLod(Sampler0, frag * texel, 0.0).rgb * 0.2;
    c += textureLod(Sampler0, (frag + vec2(2.5, 0.0)) * texel, 0.0).rgb * 0.1;
    c += textureLod(Sampler0, (frag - vec2(2.5, 0.0)) * texel, 0.0).rgb * 0.1;
    c += textureLod(Sampler0, (frag + vec2(0.0, 2.5)) * texel, 0.0).rgb * 0.1;
    c += textureLod(Sampler0, (frag - vec2(0.0, 2.5)) * texel, 0.0).rgb * 0.1;
    c += textureLod(Sampler0, (frag + vec2(1.8, 1.8)) * texel, 0.0).rgb * 0.1;
    c += textureLod(Sampler0, (frag - vec2(1.8, 1.8)) * texel, 0.0).rgb * 0.1;
    c += textureLod(Sampler0, (frag + vec2(1.8, -1.8)) * texel, 0.0).rgb * 0.1;
    c += textureLod(Sampler0, (frag - vec2(1.8, -1.8)) * texel, 0.0).rgb * 0.1;
    return c;
}

void main() {
    vec2 step = vec2(dFdx(texCoord0.x), dFdy(texCoord0.y));
    vec2 size = 1.0 / max(abs(step), vec2(1e-6));
    vec2 halfSize = size * 0.5;
    vec2 p = (texCoord0 - 0.5) * size;
    float radius = min(vertexColor.r * 255.0, min(halfSize.x, halfSize.y));
    float band = max(vertexColor.g * 255.0, 1.0);
    float strength = vertexColor.b;

    float d = roundedBox(p, halfSize, radius);
    float coverage = clamp(0.5 - d, 0.0, 1.0) * vertexColor.a;
    if (coverage <= 0.0) {
        discard;
    }

    float t = clamp(1.0 + d / band, 0.0, 1.0);
    float bend = 0.0;
    vec2 offset = vec2(0.0);
    if (t > 0.0) {
        vec2 e = vec2(0.5, 0.0);
        vec2 n = vec2(
            roundedBox(p + e.xy, halfSize, radius) - roundedBox(p - e.xy, halfSize, radius),
            roundedBox(p + e.yx, halfSize, radius) - roundedBox(p - e.yx, halfSize, radius)
        );
        n = n / max(length(n), 1e-4);
        bend = 1.0 - sqrt(max(1.0 - t * t, 0.0));
        offset = n * band * strength * bend * sign(step);
    }
    float spread = 0.12 * bend;

    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 frag = gl_FragCoord.xy;
    vec3 color = frosted(frag + offset, texel);
    if (bend > 0.02) {
        color.r = frosted(frag + offset * (1.0 + spread), texel).r;
        color.b = frosted(frag + offset * (1.0 - spread), texel).b;
    }
    fragColor = vec4(color, coverage) * ColorModulator;
}
