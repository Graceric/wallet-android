LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := crypto

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_SRC_FILES := ./boringssl/lib/libcrypto_armeabi-v7a.a
else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    LOCAL_SRC_FILES := ./boringssl/lib/libcrypto_arm64-v8a.a
else ifeq ($(TARGET_ARCH_ABI),x86)
    LOCAL_SRC_FILES := ./boringssl/lib/libcrypto_x86.a
else ifeq ($(TARGET_ARCH_ABI),x86_64)
    LOCAL_SRC_FILES := ./boringssl/lib/libcrypto_x86_64.a
endif

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_ARM_MODE  := arm
LOCAL_MODULE := lz4
LOCAL_CFLAGS 	:= -w -std=c11 -O3

LOCAL_SRC_FILES     := \
./lz4/lz4.c \
./lz4/lz4frame.c \
./lz4/xxhash.c

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_ARM_MODE  := arm
LOCAL_MODULE := rlottie
LOCAL_CPPFLAGS := -DNDEBUG -Wall -std=c++14 -DANDROID -fno-rtti -DHAVE_PTHREAD -finline-functions -ffast-math -Os -fno-exceptions -fno-unwind-tables -fno-asynchronous-unwind-tables -Wnon-virtual-dtor -Woverloaded-virtual -Wno-unused-parameter -fvisibility=hidden
ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),armeabi-v7a))
 LOCAL_CFLAGS := -DUSE_ARM_NEON  -fno-integrated-as
 LOCAL_CPPFLAGS += -DUSE_ARM_NEON  -fno-integrated-as
else ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),arm64-v8a))
 LOCAL_CFLAGS := -DUSE_ARM_NEON -D__ARM64_NEON__ -fno-integrated-as
 LOCAL_CPPFLAGS += -DUSE_ARM_NEON -D__ARM64_NEON__ -fno-integrated-as
endif

LOCAL_C_INCLUDES := \
./jni/rlottie/inc \
./jni/rlottie/src/vector/ \
./jni/rlottie/src/vector/pixman \
./jni/rlottie/src/vector/freetype \
./jni/rlottie/src/vector/stb

LOCAL_SRC_FILES     := \
./rlottie/src/lottie/lottieanimation.cpp \
./rlottie/src/lottie/lottieitem.cpp \
./rlottie/src/lottie/lottiekeypath.cpp \
./rlottie/src/lottie/lottieloader.cpp \
./rlottie/src/lottie/lottiemodel.cpp \
./rlottie/src/lottie/lottieparser.cpp \
./rlottie/src/lottie/lottieproxymodel.cpp \
./rlottie/src/vector/freetype/v_ft_math.cpp \
./rlottie/src/vector/freetype/v_ft_raster.cpp \
./rlottie/src/vector/freetype/v_ft_stroker.cpp \
./rlottie/src/vector/pixman/vregion.cpp \
./rlottie/src/vector/stb/stb_image.cpp \
./rlottie/src/vector/vbezier.cpp \
./rlottie/src/vector/vbitmap.cpp \
./rlottie/src/vector/vbrush.cpp \
./rlottie/src/vector/vcompositionfunctions.cpp \
./rlottie/src/vector/vdasher.cpp \
./rlottie/src/vector/vdebug.cpp \
./rlottie/src/vector/vdrawable.cpp \
./rlottie/src/vector/vdrawhelper.cpp \
./rlottie/src/vector/vdrawhelper_neon.cpp \
./rlottie/src/vector/velapsedtimer.cpp \
./rlottie/src/vector/vimageloader.cpp \
./rlottie/src/vector/vinterpolator.cpp \
./rlottie/src/vector/vmatrix.cpp \
./rlottie/src/vector/vpainter.cpp \
./rlottie/src/vector/vpath.cpp \
./rlottie/src/vector/vpathmesure.cpp \
./rlottie/src/vector/vraster.cpp \
./rlottie/src/vector/vrect.cpp \
./rlottie/src/vector/vrle.cpp

ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),armeabi-v7a))
    LOCAL_SRC_FILES += ./rlottie/src/vector/pixman/pixman-arm-neon-asm.S.neon
else ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),arm64-v8a))
    LOCAL_SRC_FILES += ./rlottie/src/vector/pixman/pixman-arma64-neon-asm.S.neon
endif

LOCAL_STATIC_LIBRARIES := cpufeatures
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_ARM_MODE  := arm
LOCAL_MODULE := tonlib
LOCAL_CPPFLAGS := -DNDEBUG -Wall -std=c++14 -DANDROID -frtti -DHAVE_PTHREAD -finline-functions -ffast-math -Os -fexceptions -fno-unwind-tables -fno-asynchronous-unwind-tables -Wnon-virtual-dtor -Woverloaded-virtual -Wno-unused-parameter -fvisibility=hidden
LOCAL_STATIC_LIBRARIES := crypto
LOCAL_SHORT_COMMANDS := true
APP_SHORT_COMMANDS := true

LOCAL_C_INCLUDES := \
./jni/tonblockchain/config \
./jni/tonblockchain/auto \
./jni/tonblockchain/auto/crypto \
./jni/tonblockchain/auto/crypto/block \
./jni/boringssl/include \
./jni/tonblockchain/ton \
./jni/tonblockchain/ton/tl \
./jni/tonblockchain/auto/tl/generate \
./jni/tonblockchain/ton/tdnet \
./jni/tonblockchain/ton/tdactor \
./jni/tonblockchain/ton/tdutils \
./jni/tonblockchain/ton/tddb \
./jni/tonblockchain/ton/tonlib \
./jni/tonblockchain/ton/crypto \
./jni/tonblockchain/ton/crypto/block \
./jni/tonblockchain/ton/third-party \
./jni/tonblockchain/ton/third-party/crc32c/include

LOCAL_SRC_FILES     := \
./tonblockchain/ton/adnl/adnl-address-list.cpp \
./tonblockchain/ton/adnl/adnl-channel.cpp \
./tonblockchain/ton/adnl/adnl-ext-client.cpp \
./tonblockchain/ton/adnl/adnl-ext-connection.cpp \
./tonblockchain/ton/adnl/adnl-ext-server.cpp \
./tonblockchain/ton/adnl/adnl-local-id.cpp \
./tonblockchain/ton/adnl/adnl-message.cpp \
./tonblockchain/ton/adnl/adnl-network-manager.cpp \
./tonblockchain/ton/adnl/adnl-node.cpp \
./tonblockchain/ton/adnl/adnl-node-id.cpp \
./tonblockchain/ton/adnl/adnl-packet.cpp \
./tonblockchain/ton/adnl/adnl-peer.cpp \
./tonblockchain/ton/adnl/adnl-pong.cpp \
./tonblockchain/ton/adnl/adnl-proxy-types.cpp \
./tonblockchain/ton/adnl/adnl-proxy.cpp \
./tonblockchain/ton/adnl/adnl-query.cpp \
./tonblockchain/ton/adnl/adnl-static-nodes.cpp \
./tonblockchain/ton/adnl/adnl-test-loopback-implementation.cpp \
./tonblockchain/ton/adnl/utils.cpp \
./tonblockchain/auto/tl/generate/auto/tl/lite_api.cpp \
./tonblockchain/auto/tl/generate/auto/tl/ton_api.cpp \
./tonblockchain/auto/tl/generate/auto/tl/tonlib_api.cpp \
./tonblockchain/auto/tl/generate/auto/tl/ton_api_json.cpp \
./tonblockchain/auto/tl/generate/auto/tl/tonlib_api_json.cpp \
./tonblockchain/ton/common/errorlog.cpp \
./tonblockchain/ton/crypto/Ed25519.cpp \
./tonblockchain/ton/crypto/block/adjust-block.cpp \
./tonblockchain/ton/crypto/block/Binlog.cpp \
./tonblockchain/auto/crypto/block/block-auto.cpp \
./tonblockchain/ton/crypto/block/block-db.cpp \
./tonblockchain/ton/crypto/block/block-parse.cpp \
./tonblockchain/ton/crypto/block/block.cpp \
./tonblockchain/ton/crypto/block/check-proof.cpp \
./tonblockchain/ton/crypto/block/create-state.cpp \
./tonblockchain/ton/crypto/block/dump-block.cpp \
./tonblockchain/ton/crypto/block/mc-config.cpp \
./tonblockchain/ton/crypto/block/output-queue-merger.cpp \
./tonblockchain/ton/crypto/block/transaction.cpp \
./tonblockchain/ton/crypto/common/bigexp.cpp \
./tonblockchain/ton/crypto/common/bigint.cpp \
./tonblockchain/ton/crypto/common/bitstring.cpp \
./tonblockchain/ton/crypto/common/refcnt.cpp \
./tonblockchain/ton/crypto/common/refint.cpp \
./tonblockchain/ton/crypto/common/util.cpp \
./tonblockchain/ton/crypto/ellcurve/Ed25519.cpp \
./tonblockchain/ton/crypto/ellcurve/Fp25519.cpp \
./tonblockchain/ton/crypto/ellcurve/Montgomery.cpp \
./tonblockchain/ton/crypto/ellcurve/TwEdwards.cpp \
./tonblockchain/ton/crypto/fift/Dictionary.cpp \
./tonblockchain/ton/crypto/fift/Fift.cpp \
./tonblockchain/ton/crypto/fift/IntCtx.cpp \
./tonblockchain/ton/crypto/fift/SourceLookup.cpp \
./tonblockchain/ton/crypto/fift/fift-main.cpp \
./tonblockchain/ton/crypto/fift/utils.cpp \
./tonblockchain/ton/crypto/fift/words.cpp \
./tonblockchain/ton/crypto/func/abscode.cpp \
./tonblockchain/ton/crypto/func/analyzer.cpp \
./tonblockchain/ton/crypto/func/asmops.cpp \
./tonblockchain/ton/crypto/func/builtins.cpp \
./tonblockchain/ton/crypto/func/codegen.cpp \
./tonblockchain/ton/crypto/func/func.cpp \
./tonblockchain/ton/crypto/func/gen-abscode.cpp \
./tonblockchain/ton/crypto/func/keywords.cpp \
./tonblockchain/ton/crypto/func/optimize.cpp \
./tonblockchain/ton/crypto/func/parse-func.cpp \
./tonblockchain/ton/crypto/func/stack-transform.cpp \
./tonblockchain/ton/crypto/func/unify-types.cpp \
./tonblockchain/ton/crypto/openssl/bignum.cpp \
./tonblockchain/ton/crypto/openssl/rand.cpp \
./tonblockchain/ton/crypto/openssl/residue.cpp \
./tonblockchain/ton/crypto/parser/lexer.cpp \
./tonblockchain/ton/crypto/parser/srcread.cpp \
./tonblockchain/ton/crypto/parser/symtable.cpp \
./tonblockchain/ton/crypto/tl/tlbc.cpp \
./tonblockchain/ton/crypto/tl/tlblib.cpp \
./tonblockchain/ton/crypto/smc-envelope/GenericAccount.cpp \
./tonblockchain/ton/crypto/smc-envelope/HighloadWallet.cpp \
./tonblockchain/ton/crypto/smc-envelope/HighloadWalletV2.cpp \
./tonblockchain/ton/crypto/smc-envelope/ManualDns.cpp \
./tonblockchain/ton/crypto/smc-envelope/MultisigWallet.cpp \
./tonblockchain/ton/crypto/smc-envelope/PaymentChannel.cpp \
./tonblockchain/ton/crypto/smc-envelope/SmartContract.cpp \
./tonblockchain/ton/crypto/smc-envelope/SmartContractCode.cpp \
./tonblockchain/ton/crypto/smc-envelope/WalletInterface.cpp \
./tonblockchain/ton/crypto/smc-envelope/WalletV3.cpp \
./tonblockchain/ton/crypto/smc-envelope/GenericAccount.h \
./tonblockchain/ton/crypto/smc-envelope/HighloadWallet.h \
./tonblockchain/ton/crypto/smc-envelope/HighloadWalletV2.h \
./tonblockchain/ton/crypto/smc-envelope/ManualDns.h \
./tonblockchain/ton/crypto/smc-envelope/MultisigWallet.h \
./tonblockchain/ton/crypto/smc-envelope/SmartContract.h \
./tonblockchain/ton/crypto/smc-envelope/SmartContractCode.h \
./tonblockchain/ton/crypto/smc-envelope/WalletInterface.h \
./tonblockchain/ton/crypto/smc-envelope/WalletV3.h \
./tonblockchain/ton/crypto/vm/arithops.cpp \
./tonblockchain/ton/crypto/vm/atom.cpp \
./tonblockchain/ton/crypto/vm/boc.cpp \
./tonblockchain/ton/crypto/vm/cellops.cpp \
./tonblockchain/ton/crypto/vm/continuation.cpp \
./tonblockchain/ton/crypto/vm/contops.cpp \
./tonblockchain/ton/crypto/vm/cp0.cpp \
./tonblockchain/ton/crypto/vm/debugops.cpp \
./tonblockchain/ton/crypto/vm/dict.cpp \
./tonblockchain/ton/crypto/vm/dictops.cpp \
./tonblockchain/ton/crypto/vm/memo.cpp \
./tonblockchain/ton/crypto/vm/dispatch.cpp \
./tonblockchain/ton/crypto/vm/opctable.cpp \
./tonblockchain/ton/crypto/vm/stack.cpp \
./tonblockchain/ton/crypto/vm/stackops.cpp \
./tonblockchain/ton/crypto/vm/tonops.cpp \
./tonblockchain/ton/crypto/vm/tupleops.cpp \
./tonblockchain/ton/crypto/vm/utils.cpp \
./tonblockchain/ton/crypto/vm/vm.cpp \
./tonblockchain/ton/crypto/vm/cells/Cell.cpp \
./tonblockchain/ton/crypto/vm/cells/CellBuilder.cpp \
./tonblockchain/ton/crypto/vm/cells/CellHash.cpp \
./tonblockchain/ton/crypto/vm/cells/CellSlice.cpp \
./tonblockchain/ton/crypto/vm/cells/CellString.cpp \
./tonblockchain/ton/crypto/vm/cells/CellTraits.cpp \
./tonblockchain/ton/crypto/vm/cells/CellUsageTree.cpp \
./tonblockchain/ton/crypto/vm/cells/DataCell.cpp \
./tonblockchain/ton/crypto/vm/cells/LevelMask.cpp \
./tonblockchain/ton/crypto/vm/cells/MerkleProof.cpp \
./tonblockchain/ton/crypto/vm/cells/MerkleUpdate.cpp \
./tonblockchain/ton/crypto/vm/db/CellStorage.cpp \
./tonblockchain/ton/crypto/vm/db/DynamicBagOfCellsDb.cpp \
./tonblockchain/ton/crypto/vm/db/StaticBagOfCellsDb.cpp \
./tonblockchain/ton/crypto/vm/db/TonDb.cpp \
./tonblockchain/ton/emulator/emulator-emscripten.cpp \
./tonblockchain/ton/emulator/emulator-extern.cpp \
./tonblockchain/ton/emulator/transaction-emulator.cpp \
./tonblockchain/ton/keyring/keyring.cpp \
./tonblockchain/ton/keys/encryptor.cpp \
./tonblockchain/ton/keys/keys.cpp \
./tonblockchain/ton/lite-client/lite-client-common.cpp \
./tonblockchain/ton/tdactor/td/actor/MultiPromise.cpp \
./tonblockchain/ton/tdactor/td/actor/core/ActorExecutor.cpp \
./tonblockchain/ton/tdactor/td/actor/core/CpuWorker.cpp \
./tonblockchain/ton/tdactor/td/actor/core/IoWorker.cpp \
./tonblockchain/ton/tdactor/td/actor/core/Scheduler.cpp \
./tonblockchain/ton/tddb/td/db/MemoryKeyValue.cpp \
./tonblockchain/ton/tddb/td/db/binlog/Binlog.cpp \
./tonblockchain/ton/tddb/td/db/binlog/BinlogReaderHelper.cpp \
./tonblockchain/ton/tddb/td/db/utils/ChainBuffer.cpp \
./tonblockchain/ton/tddb/td/db/utils/CyclicBuffer.cpp \
./tonblockchain/ton/tddb/td/db/utils/FileSyncState.cpp \
./tonblockchain/ton/tddb/td/db/utils/FileToStreamActor.cpp \
./tonblockchain/ton/tddb/td/db/utils/StreamInterface.cpp \
./tonblockchain/ton/tddb/td/db/utils/StreamToFileActor.cpp \
./tonblockchain/ton/tdnet/td/net/FdListener.cpp \
./tonblockchain/ton/tdnet/td/net/TcpListener.cpp \
./tonblockchain/ton/tdnet/td/net/UdpServer.cpp \
./tonblockchain/ton/tdutils/td/utils/BigNum.cpp \
./tonblockchain/ton/tdutils/td/utils/BufferedUdp.cpp \
./tonblockchain/ton/tdutils/td/utils/FileLog.cpp \
./tonblockchain/ton/tdutils/td/utils/Gzip.cpp \
./tonblockchain/ton/tdutils/td/utils/GzipByteFlow.cpp \
./tonblockchain/ton/tdutils/td/utils/Hints.cpp \
./tonblockchain/ton/tdutils/td/utils/HttpUrl.cpp \
./tonblockchain/ton/tdutils/td/utils/JsonBuilder.cpp \
./tonblockchain/ton/tdutils/td/utils/MimeType.cpp \
./tonblockchain/ton/tdutils/td/utils/MpmcQueue.cpp \
./tonblockchain/ton/tdutils/td/utils/OptionParser.cpp \
./tonblockchain/ton/tdutils/td/utils/PathView.cpp \
./tonblockchain/ton/tdutils/td/utils/Random.cpp \
./tonblockchain/ton/tdutils/td/utils/SharedSlice.cpp \
./tonblockchain/ton/tdutils/td/utils/Slice.cpp \
./tonblockchain/ton/tdutils/td/utils/StackAllocator.cpp \
./tonblockchain/ton/tdutils/td/utils/Status.cpp \
./tonblockchain/ton/tdutils/td/utils/StringBuilder.cpp \
./tonblockchain/ton/tdutils/td/utils/Time.cpp \
./tonblockchain/ton/tdutils/td/utils/Timer.cpp \
./tonblockchain/ton/tdutils/td/utils/TsFileLog.cpp \
./tonblockchain/ton/tdutils/td/utils/base64.cpp \
./tonblockchain/ton/tdutils/td/utils/buffer.cpp \
./tonblockchain/ton/tdutils/td/utils/check.cpp \
./tonblockchain/ton/tdutils/td/utils/crypto.cpp \
./tonblockchain/ton/tdutils/td/utils/filesystem.cpp \
./tonblockchain/ton/tdutils/td/utils/find_boundary.cpp \
./tonblockchain/ton/tdutils/td/utils/logging.cpp \
./tonblockchain/ton/tdutils/td/utils/misc.cpp \
./tonblockchain/ton/tdutils/td/utils/tests.cpp \
./tonblockchain/ton/tdutils/td/utils/tl_parsers.cpp \
./tonblockchain/ton/tdutils/td/utils/translit.cpp \
./tonblockchain/ton/tdutils/td/utils/unicode.cpp \
./tonblockchain/ton/tdutils/td/utils/utf8.cpp \
./tonblockchain/ton/tdutils/td/utils/port/Clocks.cpp \
./tonblockchain/ton/tdutils/td/utils/port/FileFd.cpp \
./tonblockchain/ton/tdutils/td/utils/port/IPAddress.cpp \
./tonblockchain/ton/tdutils/td/utils/port/MemoryMapping.cpp \
./tonblockchain/ton/tdutils/td/utils/port/PollFlags.cpp \
./tonblockchain/ton/tdutils/td/utils/port/ServerSocketFd.cpp \
./tonblockchain/ton/tdutils/td/utils/port/SocketFd.cpp \
./tonblockchain/ton/tdutils/td/utils/port/Stat.cpp \
./tonblockchain/ton/tdutils/td/utils/port/StdStreams.cpp \
./tonblockchain/ton/tdutils/td/utils/port/UdpSocketFd.cpp \
./tonblockchain/ton/tdutils/td/utils/port/path.cpp \
./tonblockchain/ton/tdutils/td/utils/port/rlimit.cpp \
./tonblockchain/ton/tdutils/td/utils/port/signals.cpp \
./tonblockchain/ton/tdutils/td/utils/port/sleep.cpp \
./tonblockchain/ton/tdutils/td/utils/port/stacktrace.cpp \
./tonblockchain/ton/tdutils/td/utils/port/thread_local.cpp \
./tonblockchain/ton/tdutils/td/utils/port/user.cpp \
./tonblockchain/ton/tdutils/td/utils/port/wstring_convert.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/Epoll.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/EventFdBsd.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/EventFdLinux.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/EventFdWindows.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/Iocp.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/KQueue.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/NativeFd.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/Poll.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/Select.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/ThreadIdGuard.cpp \
./tonblockchain/ton/tdutils/td/utils/port/detail/WineventPoll.cpp \
./tonblockchain/ton/terminal/terminal.cpp \
./tonblockchain/ton/third-party/crc32c/src/crc32c_portable.cc \
./tonblockchain/ton/third-party/crc32c/src/crc32c.cc \
./tonblockchain/ton/tl/tl/tl_jni_object.cpp \
./tonblockchain/ton/tl-utils/lite-utils.cpp \
./tonblockchain/ton/tl-utils/tl-utils.cpp \
./tonblockchain/ton/tonlib/tonlib/Client.cpp \
./tonblockchain/ton/tonlib/tonlib/ClientActor.cpp \
./tonblockchain/ton/tonlib/tonlib/ClientJson.cpp \
./tonblockchain/ton/tonlib/tonlib/Config.cpp \
./tonblockchain/ton/tonlib/tonlib/ExtClient.cpp \
./tonblockchain/ton/tonlib/tonlib/ExtClientLazy.cpp \
./tonblockchain/ton/tonlib/tonlib/ExtClientOutbound.cpp \
./tonblockchain/ton/tonlib/tonlib/KeyStorage.cpp \
./tonblockchain/ton/tonlib/tonlib/KeyValue.cpp \
./tonblockchain/ton/tonlib/tonlib/LastBlock.cpp \
./tonblockchain/ton/tonlib/tonlib/LastBlockStorage.cpp \
./tonblockchain/ton/tonlib/tonlib/LastConfig.cpp \
./tonblockchain/ton/tonlib/tonlib/Logging.cpp \
./tonblockchain/ton/tonlib/tonlib/TonlibClient.cpp \
./tonblockchain/ton/tonlib/tonlib/tonlib_client_json.cpp \
./tonblockchain/ton/tonlib/tonlib/utils.cpp \
./tonblockchain/ton/tonlib/tonlib/keys/bip39.cpp \
./tonblockchain/ton/tonlib/tonlib/keys/DecryptedKey.cpp \
./tonblockchain/ton/tonlib/tonlib/keys/EncryptedKey.cpp \
./tonblockchain/ton/tonlib/tonlib/keys/Mnemonic.cpp \
./tonblockchain/ton/tonlib/tonlib/keys/SimpleEncryption.cpp

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_PRELINK_MODULE := false

LOCAL_MODULE 	:= tmessages.30
LOCAL_CFLAGS 	:= -w -std=c11 -Os -DNULL=0 -DSOCKLEN_T=socklen_t -DLOCALE_NOT_USED -D_LARGEFILE_SOURCE=1
LOCAL_CFLAGS 	+= -Drestrict='' -D__EMX__ -DOPUS_BUILD -DFIXED_POINT -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF -fno-math-errno
LOCAL_CFLAGS 	+= -DANDROID_NDK -DDISABLE_IMPORTGL -fno-strict-aliasing -fprefetch-loop-arrays -DAVOID_TABLES -DANDROID_TILE_BASED_DECODE -DANDROID_ARMV6_IDCT -ffast-math -D__STDC_CONSTANT_MACROS
LOCAL_CPPFLAGS 	:= -DBSD=1 -ffast-math -Os -funroll-loops -std=c++14 -DPACKAGE_NAME='"drinkless/org/ton"'
LOCAL_LDLIBS 	:= -ljnigraphics -llog -lz -lEGL -lGLESv2 -landroid
LOCAL_STATIC_LIBRARIES := lz4 rlottie tonlib

LOCAL_C_INCLUDES    := \
./jni/tonblockchain/config \
./jni/tonblockchain/auto \
./jni/tonblockchain/auto/crypto \
./jni/boringssl/include \
./jni/rlottie/inc \
./jni/tonblockchain/ton \
./jni/tonblockchain/ton/tonlib \
./jni/tonblockchain/ton/tl \
./jni/tonblockchain/auto/tl/generate \
./jni/tonblockchain/ton/tdutils \
./jni/tonblockchain/ton/crypto \
./jni/lz4

LOCAL_SRC_FILES     += \
./jni.c \
./lottie.cpp \
./tonlib.cpp

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/cpufeatures)