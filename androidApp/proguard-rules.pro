-verbose
-allowaccessmodification
-repackageclasses

-keepattributes SourceFile,
                LineNumberTable

-renamesourcefileattribute SourceFile

# Do not minify litert and litertlm files
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn com.google.ai.edge.litert.**
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**
