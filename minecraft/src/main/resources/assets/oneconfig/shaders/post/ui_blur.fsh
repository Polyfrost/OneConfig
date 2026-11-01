#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurConfig {
    vec2 BlurDir;
};

// 26.3 needs explicit locations, but VulkanMod's GLSL parser rejects a bare `layout` on outputs
#define FRAG_OUT layout(location = 0) out
FRAG_OUT vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / OutSize;
    vec2 dir = BlurDir / InSize;
    vec3 c = texture(InSampler, uv).rgb * 0.19967563;
    c += (texture(InSampler, uv - dir * 1.4073334).rgb + texture(InSampler, uv + dir * 1.4073334).rgb) * 0.29732251;
    c += (texture(InSampler, uv - dir * 3.2942150).rgb + texture(InSampler, uv + dir * 3.2942150).rgb) * 0.09184834;
    c += (texture(InSampler, uv - dir * 5.2018132).rgb + texture(InSampler, uv + dir * 5.2018132).rgb) * 0.01099133;
    fragColor = vec4(c, 1.0);
}
