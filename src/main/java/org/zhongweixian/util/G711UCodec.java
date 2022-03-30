package org.zhongweixian.util;

/**
 * Created by caoliang on 2020/8/4
 */
public class G711UCodec {

    static final int ULAW = 1;
    static final int ALAW = 2;

    /* 16384 entries per table (16 bit) */
    static byte[] linear_to_ulaw = new byte[65536];

    /* 16384 entries per table (8 bit) */
    static short[] ulaw_to_linear = new short[256];

    static final int SIGN_BIT = 0x80;
    static final int QUANT_MASK = 0x0f;
    static final int NSEGS = 0x08;
    static final int SEG_SHIFT = 0x04;
    static final int SEG_MASK = 0x70;

    static final int BIAS = 0x84;
    static final int CLIP = 8159;

    static short[] seg_aend = {0x1F, 0x3F, 0x7F, 0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF};
    static short[] seg_uend = {0x3F, 0x7F, 0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF};

    int[] _u2a = {1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 27, 29, 31, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
            /* corrected:
                81,	82,	83,	84,	85,	86,	87,	88,
               should be: */
            80, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128};

    int[] _a2u = {1, 3, 5, 7, 9, 11, 13, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 32, 33, 33, 34, 34, 35, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 48, 49, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 64, 65, 66, 67, 68, 69, 70, 71, 72,
            /* corrected:
                73,	74,	75,	76,	77,	78,	79,	79,
               should be: */
            73, 74, 75, 76, 77, 78, 79, 80, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127};

    static {
        // 初始化ulaw表
        for (int i = 0; i < 256; i++) {
            ulaw_to_linear[i] = ulaw2linear((byte) i);
        }
        // 初始化ulaw2linear表
        for (int i = 0; i < 65535; i++) {
            linear_to_ulaw[i] = linear2ulaw((short) i);
        }
    }

    public static short ulaw2linear(byte u_val) {
        short t;
        u_val = (byte) (~u_val);
        t = (short) (((u_val & QUANT_MASK) << 3) + BIAS);
        t <<= (u_val & SEG_MASK) >>> SEG_SHIFT;

        return ((u_val & SIGN_BIT) > 0 ? (short) (BIAS - t) : (short) (t - BIAS));
    }

    public static byte linear2ulaw(short pcm_val) {
        short mask;
        short seg;
        byte uval;

        pcm_val = (short) (pcm_val >> 2);
        if (pcm_val < 0) {
            pcm_val = (short) (-pcm_val);
            mask = 0x7f;
        } else {
            mask = 0xff;
        }

        if (pcm_val > CLIP) pcm_val = CLIP;
        pcm_val += (BIAS >> 2);

        seg = search(pcm_val, seg_uend, (short) 8);

        if (seg >= 8) {
            return (byte) (0x7f ^ mask);
        } else {
            uval = (byte) ((seg << 4) | ((pcm_val >> (seg + 1)) & 0xF));
            return (byte) (uval ^ mask);
        }
    }

    static short search(short val, short[] table, short size) {
        for (short i = 0; i < size; i++) {
            if (val <= table[i]) {
                return i;
            }
        }
        return size;
    }

    static void ulaw_to_pcm16(int src_length, byte[] src_samples, byte[] dst_samples) {
        for (int i = 0, k = 0; i < src_length; i++) {
            short s = ulaw_to_linear[src_samples[i] & 0xff];
            dst_samples[k++] = (byte) (s & 0xff);
            dst_samples[k++] = (byte) ((s >> 8) & 0xff);
        }
    }

    static void pcm16_to_ulaw(int src_length, byte[] src_samples, byte[] dst_samples) {
        short[] s_samples = toShortArray(src_samples);
        for (int i = 0, k = 0; i < s_samples.length; i++) {
            dst_samples[k++] = linear2ulaw(s_samples[i]);
        }
    }

    /**
     * byte[] 转 short[]
     *
     * @param src
     * @return
     */
    private static short[] toShortArray(byte[] src) {
        short[] dst = new short[src.length / 2];
        for (int i = 0, k = 0; i < src.length; ) {
            dst[k++] = (short) ((src[i++] & 0xff) | ((src[i++] & 0xff) << 8));
        }
        return dst;
    }


    public byte[] toPCM(byte[] data) {
        byte[] temp;
        // 如果前四字节是00 01 52 00，则是海思头，需要去掉
        if (data[0] == 0x00 && data[1] == 0x01 && (data[2] & 0xff) == (data.length - 4) / 2 && data[3] == 0x00) {
            temp = new byte[data.length - 4];
            System.arraycopy(data, 4, temp, 0, temp.length);
        } else {
            temp = data;
        }

        byte[] dest = new byte[temp.length * 2];
        ulaw_to_pcm16(temp.length, temp, dest);
        return dest;
    }


}
