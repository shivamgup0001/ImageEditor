#include <jni.h>

JNIEXPORT __unused  void JNICALL
Java_com_example_imageeditor_MainActivity_blackAndWhite(JNIEnv *env, jclass clazz, jintArray pixels_,
                                                        jint width, jint height) {
    jint *pixels=(*env)->GetIntArrayElements(env,pixels_,NULL);

    unsigned char *colors=(unsigned char *)pixels;
    int pixelCount=width*height*4;

    //0==blue 1==green 2==red

    int i=0;
    while(i<pixelCount){
       unsigned char average =(colors[i]+colors[i+1]+colors[i+2])/3;
       colors[i]=average;
       colors[i+1]=average;
       colors[i+2]=average;
       i+=4;
    }

    (*env)->ReleaseIntArrayElements(env,pixels_,pixels,0);
}

