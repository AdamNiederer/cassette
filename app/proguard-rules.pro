# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# jaudiotagger bundles jcodec, which instantiates MP4 boxes via untyped
# reflection (Utils.newInstance -> Class.getConstructor(Header)). R8 can't
# trace this and strips the (Header) constructors, breaking M4A/MP4 reads
# with NoSuchMethodException. Keep them.
-keepclasseswithmembers,includedescriptorclasses class org.jcodec.containers.mp4.boxes.** {
    public <init>(org.jcodec.containers.mp4.boxes.Header);
}