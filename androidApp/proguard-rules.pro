-verbose
-allowaccessmodification
-repackageclasses

-keepattributes SourceFile,
                LineNumberTable

-renamesourcefileattribute SourceFile

# Do not minify litertlm files
-keep class com.google.ai.edge.litertlm.** { *; }
