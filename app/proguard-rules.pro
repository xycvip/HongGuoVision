# R8 keeps runtime-visible metadata used by Kotlin/Android libraries.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# The Android platform class is resolved reflectively by HyperOsNavigation;
# it belongs to the framework and is not part of the app's shrinkable program.
