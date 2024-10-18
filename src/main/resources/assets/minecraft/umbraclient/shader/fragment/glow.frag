#version 120

uniform sampler2D texture;
uniform vec2 texelSize;

uniform vec3 color;      // Kolor glow
uniform int radius;      // Promień glow
uniform float fade;      // Wartość fade (zasięg zanikania)
uniform float targetAlpha; // Docelowa przezroczystość

void main() {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].xy);

    if (centerCol.a != 0.0) {
        // Jeśli mamy kolor, ustaw przezroczystość do targetAlpha
        gl_FragColor = vec4(centerCol.rgb, targetAlpha);
    } else {
        // Oblicz glow
        float alpha = 0.0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                vec4 currentColor = texture2D(texture, gl_TexCoord[0].xy + vec2(texelSize.x * x, texelSize.y * y));
                int distanceSquared = x * x + y * y;
                if (currentColor.a != 0.0) {
                    if (fade > 0.0) {
                        alpha += max(0.0, (float(radius) - sqrt(float(distanceSquared))) / float(radius));
                    } else {
                        alpha += 1.0;
                    }
                }
            }
        }
        alpha /= fade;  // Podział przez fade, aby uzyskać efekt
        gl_FragColor = vec4(color, alpha);  // Kolor glow z odpowiednią alfą
    }
}