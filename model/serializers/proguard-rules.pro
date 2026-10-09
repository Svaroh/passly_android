# jsonschemafriend and its transitive dependencies (joni, jcodings)
# joni uses static initializers and reflection internally - R8 strips
# members needed by QuantifierNode.<clinit>, causing a startup crash.
-keep class org.joni.** { *; }
-keep class org.jcodings.** { *; }
-keep class net.jimblackler.jsonschemafriend.** { *; }
