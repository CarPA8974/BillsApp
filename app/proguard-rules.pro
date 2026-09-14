# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- REGRAS PARA O GSON E PERSISTÊNCIA DE DADOS ---
# Se o R8 ofuscar o nome das variáveis dos modelos, o JSON vai quebrar
# ao tentar ler dados gravados em versões anteriores.
-keep class com.example.mybillsapp.model.** { *; }

# Mantém anotações e assinaturas vitais para o Gson funcionar via Reflection
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# Mantém a estrutura de enums intacta
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
