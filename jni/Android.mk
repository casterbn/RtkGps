LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/rtklib.mk

include $(CLEAR_VARS)

LOCAL_MODULE    := rtkgps

LOCAL_CFLAGS += -fvisibility=hidden

LOCAL_SRC_FILES := \
	gtime.c \
	prcopt.c \
	rtkjni.c \
	rtkcommon.c \
        rtkserver.c \
	solopt.c

LOCAL_STATIC_LIBRARIES := rtklib

LOCAL_LDLIBS += jni/LAPACK/liblapack.a jni/LAPACK/libblas.a

include $(BUILD_SHARED_LIBRARY)

#$(call import-module,simonlynen_android_libs/lapack/jni)

