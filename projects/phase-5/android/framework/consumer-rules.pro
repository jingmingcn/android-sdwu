# Applied to the app when it uses R8 (minifyEnabled). ServiceLoader creates module providers
# by reflection, so keep their classes and no-arg constructors.
-keep class * implements edu.sdwu.teaching.framework.ModuleProvider { public <init>(); }
