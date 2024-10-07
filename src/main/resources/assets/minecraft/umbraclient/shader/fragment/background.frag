#version 120

uniform float iTime;      // Czas w sekundach
uniform vec2 iResolution; // Rozdzielczość ekranu

// Funkcja generująca losową wartość
float random(float seed) {
    return fract(sin(seed) * 43758.5453);
}

// Funkcja generująca hałas
float noise(vec2 uv) {
    vec2 i = floor(uv);
    vec2 f = fract(uv);
    float a = random(i.x + i.y * 57.0);
    float b = random(i.x + 1.0 + i.y * 57.0);
    float c = random(i.x + (i.y + 1.0) * 57.0);
    float d = random(i.x + 1.0 + (i.y + 1.0) * 57.0);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

// Funkcja generująca gradient koloru cieczy, przypominającej "glizdy"
vec3 liquidColor(vec2 uv) {
    float n = noise(uv * 10.0 + vec2(iTime * 0.1, 0.0)); // Wolniejsze zmiany
    vec3 baseColor = vec3(0.1, 0.0, 0.2);  // Głęboki fiolet jako kolor bazowy
    vec3 highlightColor = vec3(0.6, 0.0, 0.8); // Jaśniejszy, intensywny fiolet
    return mix(baseColor, highlightColor, n);
}

// Funkcja tworząca efekt falowania i wijących się glizd
float wormEffect(vec2 uv) {
    float wave1 = sin(uv.y * 10.0 + iTime * 0.5) * 0.1;
    float wave2 = cos(uv.x * 15.0 - iTime * 0.3) * 0.05;
    float noiseWave = noise(uv * 5.0 + vec2(iTime * 0.5, -iTime * 0.3)) * 0.1;
    return wave1 + wave2 + noiseWave;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // Normalizacja współrzędnych
    vec2 uv = fragCoord.xy / iResolution.xy;

    // Dodanie efektu falowania do współrzędnych
    uv.x += wormEffect(uv);

    // Generowanie koloru cieczy
    vec3 color = liquidColor(uv);

    // Dodanie efektu ciemniejszych cieni, aby podkreślić efekt mroku
    float shadow = smoothstep(0.3, 0.6, abs(uv.y - 0.5));
    color *= 0.8 * (1.0 - shadow);

    // Ustawienie koloru fragmentu
    fragColor = vec4(color, 1.0);
}

// Punkt wejścia shaderu
void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy); // Wywołanie funkcji głównej
}
