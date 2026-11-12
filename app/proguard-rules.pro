# Keep source/line and generic/annotation metadata used by AndroidX, Kotlin and diagnostics.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# R8 already preserves Android components referenced from the manifest and performs
# whole-program analysis for directly referenced Kotlin/Java code. Avoid broad package
# keep rules so unused Compose, Media3, OkHttp and Bouncy Castle code can be removed.
