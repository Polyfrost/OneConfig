package org.polyfrost.oneconfig.internal;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import com.sun.jna.*;
import com.sun.jna.ptr.LongByReference;

/**
 * Taken from owo-lib under MIT License.
 * Source: <a href="https://github.com/wisp-forest/owo-lib/blob/f84ff73af2b5ce87525d5d0c9db46a91ff429aee/src/main/java/io/wispforest/owo/renderdoc/RenderdocLibrary.java">RenderdocLibrary.java</a>
 */
interface RenderdocLibrary extends Library {

    int RENDERDOC_GetAPI(int version, PointerByReference out);

    @SuppressWarnings("unused")
    @Structure.FieldOrder({
            "GetAPIVersion", "SetCaptureOptionU32", "SetCaptureOptionF32", "GetCaptureOptionU32", "GetCaptureOptionF32", "SetFocusToggleKeys",
            "SetCaptureKeys", "GetOverlayBits", "MaskOverlayBits", "RemoveHooks", "UnloadCrashHandler", "SetCaptureFilePathTemplate", "GetCaptureFilePathTemplate",
            "GetNumCaptures", "GetCapture", "TriggerCapture", "IsTargetControlConnected", "LaunchReplayUI", "SetActiveWindow", "StartFrameCapture",
            "IsFrameCapturing", "EndFrameCapture", "TriggerMultiFrameCapture", "SetCaptureFileComments", "DiscardFrameCapture", "ShowReplayUI"
    })
    class RenderdocApi extends Structure {

        public pRENDERDOC_GetAPIVersion GetAPIVersion;
        public pRENDERDOC_SetCaptureOptionU32 SetCaptureOptionU32;
        public pRENDERDOC_SetCaptureOptionF32 SetCaptureOptionF32;
        public pRENDERDOC_GetCaptureOptionU32 GetCaptureOptionU32;
        public pRENDERDOC_GetCaptureOptionF32 GetCaptureOptionF32;
        public pRENDERDOC_SetFocusToggleKeys SetFocusToggleKeys;
        public pRENDERDOC_SetCaptureKeys SetCaptureKeys;
        public pRENDERDOC_GetOverlayBits GetOverlayBits;
        public pRENDERDOC_MaskOverlayBits MaskOverlayBits;
        public pRENDERDOC_RemoveHooks RemoveHooks;
        public pRENDERDOC_UnloadCrashHandler UnloadCrashHandler;
        public pRENDERDOC_SetCaptureFilePathTemplate SetCaptureFilePathTemplate;
        public pRENDERDOC_GetCaptureFilePathTemplate GetCaptureFilePathTemplate;
        public pRENDERDOC_GetNumCaptures GetNumCaptures;
        public pRENDERDOC_GetCapture GetCapture;
        public pRENDERDOC_TriggerCapture TriggerCapture;
        public pRENDERDOC_IsTargetControlConnected IsTargetControlConnected;
        public pRENDERDOC_LaunchReplayUI LaunchReplayUI;
        public pRENDERDOC_SetActiveWindow SetActiveWindow;
        public pRENDERDOC_StartFrameCapture StartFrameCapture;
        public pRENDERDOC_IsFrameCapturing IsFrameCapturing;
        public pRENDERDOC_EndFrameCapture EndFrameCapture;
        public pRENDERDOC_TriggerMultiFrameCapture TriggerMultiFrameCapture;
        public pRENDERDOC_SetCaptureFileComments SetCaptureFileComments;
        public pRENDERDOC_DiscardFrameCapture DiscardFrameCapture;
        public pRENDERDOC_ShowReplayUI ShowReplayUI;

        public RenderdocApi(Pointer data) {
            super(data);
            this.read();
        }

        public interface pRENDERDOC_GetAPIVersion extends Callback {
            void call(IntByReference major, IntByReference minor, IntByReference patch);
        }

        public interface pRENDERDOC_SetCaptureOptionU32 extends Callback {
            int call(int opt, uint32_t val);
        }

        public interface pRENDERDOC_SetCaptureOptionF32 extends Callback {
            int call(int opt, float val);
        }

        public interface pRENDERDOC_GetCaptureOptionU32 extends Callback {
            uint32_t call(int opt);
        }

        public interface pRENDERDOC_GetCaptureOptionF32 extends Callback {
            float call(int opt);
        }

        public interface pRENDERDOC_SetFocusToggleKeys extends Callback {
            void call(Pointer keys, int num);
        }

        public interface pRENDERDOC_SetCaptureKeys extends Callback {
            void call(int[] keys, int num);
        }

        public interface pRENDERDOC_GetOverlayBits extends Callback {
            uint32_t call();
        }

        public interface pRENDERDOC_MaskOverlayBits extends Callback {
            void call(uint32_t And, uint32_t Or);
        }

        public interface pRENDERDOC_RemoveHooks extends Callback {
            void call();
        }

        public interface pRENDERDOC_UnloadCrashHandler extends Callback {
            void call();
        }

        public interface pRENDERDOC_SetCaptureFilePathTemplate extends Callback {
            void call(String pathTemplate);
        }

        public interface pRENDERDOC_GetCaptureFilePathTemplate extends Callback {
            String call();
        }

        public interface pRENDERDOC_GetNumCaptures extends Callback {
            uint32_t call();
        }

        public interface pRENDERDOC_GetCapture extends Callback {
            uint32_t call(int idx, byte[] filename, IntByReference pathLength, LongByReference timestamp);
        }

        public interface pRENDERDOC_TriggerCapture extends Callback {
            void call();
        }

        public interface pRENDERDOC_IsTargetControlConnected extends Callback {
            uint32_t call();
        }

        public interface pRENDERDOC_LaunchReplayUI extends Callback {
            uint32_t call(uint32_t connectTargetControl, String cmdline);
        }

        public interface pRENDERDOC_ShowReplayUI extends Callback {
            uint32_t call();
        }

        public interface pRENDERDOC_SetActiveWindow extends Callback {
            void call(Pointer device, Pointer windowHandle);
        }

        public interface pRENDERDOC_StartFrameCapture extends Callback {
            void call(Pointer device, Pointer windowHandle);
        }

        public interface pRENDERDOC_IsFrameCapturing extends Callback {
            uint32_t call();
        }

        public interface pRENDERDOC_EndFrameCapture extends Callback {
            void call(Pointer device, Pointer windowHandle);
        }

        public interface pRENDERDOC_DiscardFrameCapture extends Callback {
            void call(Pointer device, Pointer windowHandle);
        }

        public interface pRENDERDOC_TriggerMultiFrameCapture extends Callback {
            void call(uint32_t numFrames);
        }

        public interface pRENDERDOC_SetCaptureFileComments extends Callback {
            void call(String filePath, String comments);
        }
    }

    class uint32_t extends IntegerType {
        public uint32_t() {
            this(0);
        }

        public uint32_t(int value) {
            super(4, value, true);
        }
    }
}
