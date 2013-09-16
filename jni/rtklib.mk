include $(CLEAR_VARS)

# path setting for NDK
NDK_PATH := /sdk/android-ndk-r9

RTKLIB_PATH := RTKLIB
RTKLIB_CFLAGS := -DENAGLO -DENAGAL -DENAQZS -DENACMP -DNFREQ=3 -DTRACE

LOCAL_MODULE    := rtklib

RTKLIB_CFLAGS += -DLAPACK
#LOCAL_STATIC_LIBRARIES += clapack blas f2c
LOCAL_STATIC_LIBRARIES += lapack blas

LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_CFLAGS += $(RTKLIB_CFLAGS)
LOCAL_EXPORT_CFLAGS := $(RTKLIB_CFLAGS)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(RTKLIB_PATH)/src
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(RTKLIB_PATH)/src

LOCAL_LDLIBS += -llog
#LOCAL_EXPORT_LDLIBS += -llog
# add fortran library
LOCAL_EXPORT_LDLIBS += -llog -l$(NDK_PATH)/toolchains/arm-linux-androideabi-4.8.0/prebuilt/linux-x86/arm-linux-androideabi/lib/armv7-a/libgfortran.a

LOCAL_SRC_FILES := \
        $(RTKLIB_PATH)/src/convkml.c \
        $(RTKLIB_PATH)/src/convrnx.c \
        $(RTKLIB_PATH)/src/datum.c \
        $(RTKLIB_PATH)/src/download.c \
        $(RTKLIB_PATH)/src/ephemeris.c \
        $(RTKLIB_PATH)/src/geoid.c \
        $(RTKLIB_PATH)/src/ionex.c \
        $(RTKLIB_PATH)/src/lambda.c \
        $(RTKLIB_PATH)/src/options.c \
        $(RTKLIB_PATH)/src/pntpos.c \
        $(RTKLIB_PATH)/src/postpos.c \
        $(RTKLIB_PATH)/src/ppp_ar.c \
        $(RTKLIB_PATH)/src/ppp.c \
        $(RTKLIB_PATH)/src/preceph.c \
        $(RTKLIB_PATH)/src/qzslex.c \
        $(RTKLIB_PATH)/src/rcvraw.c \
        $(RTKLIB_PATH)/src/rinex.c \
        $(RTKLIB_PATH)/src/rtcm2.c \
        $(RTKLIB_PATH)/src/rtcm3.c \
        $(RTKLIB_PATH)/src/rtcm3e.c \
        $(RTKLIB_PATH)/src/rtcm.c \
        $(RTKLIB_PATH)/src/rtkcmn.c \
        $(RTKLIB_PATH)/src/rtkpos.c \
        $(RTKLIB_PATH)/src/rtksvr.c \
        $(RTKLIB_PATH)/src/sbas.c \
        $(RTKLIB_PATH)/src/solution.c \
        $(RTKLIB_PATH)/src/stream.c \
        $(RTKLIB_PATH)/src/streamsvr.c \
        $(RTKLIB_PATH)/src/tle.c

LOCAL_SRC_FILES += \
        $(RTKLIB_PATH)/src/rcv/binex.c \
        $(RTKLIB_PATH)/src/rcv/crescent.c \
        $(RTKLIB_PATH)/src/rcv/gw10.c \
        $(RTKLIB_PATH)/src/rcv/javad.c \
        $(RTKLIB_PATH)/src/rcv/novatel.c \
        $(RTKLIB_PATH)/src/rcv/nvs.c \
        $(RTKLIB_PATH)/src/rcv/rcvlex.c \
        $(RTKLIB_PATH)/src/rcv/skytraq.c \
        $(RTKLIB_PATH)/src/rcv/sirf.c \
        $(RTKLIB_PATH)/src/rcv/ss2.c \
        $(RTKLIB_PATH)/src/rcv/ublox.c

LOCAL_SRC_FILES += log.c

TARGET-process-src-files-tags += $(call add-src-files-target-cflags, \
       $(RTKLIB_PATH)/src/rtkcmn.c )
#       $(RTKLIB_PATH)/src/rtkcmn.c, -Ddgemm_=f2c_dgemm)

include $(BUILD_STATIC_LIBRARY)

