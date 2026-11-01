# Proguard rules
-dontwarn java.sql.**
-dontwarn javax.sql.**
-dontwarn javax.naming.**
-keep class org.mariadb.jdbc.** { *; }
