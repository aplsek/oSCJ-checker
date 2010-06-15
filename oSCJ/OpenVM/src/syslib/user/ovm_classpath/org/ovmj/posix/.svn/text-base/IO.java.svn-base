package org.ovmj.posix;

import java.io.IOException;

public class IO {
    private static IOException die(int reason) {
        return new IOException(getErrorMessage(reason));
    }

    /**
     * Poll value of errno and build the appropriate error message.
     */
    private static String getErrorMessage(int reason) {
        byte[] buf = new byte[128];
        int len = LibraryGlue
                .get_specific_error_string(reason, buf, buf.length);
        return new String(buf, 0, len);
    }

    private static int check(int ret) throws IOException {
        if (ret < 0)
            throw die(LibraryImports.getErrno());
        return ret;
    }

    private static int checkNB(int ret) throws IOException {
        if (ret < 0) {
            if (LibraryImports.errnoIsWouldBlock()) {
                return -1;
            } else {
                throw die(LibraryImports.getErrno());
            }
        } else {
            return ret;
        }
    }

    private static long check(long ret) throws IOException {
        if (ret < 0)
            throw die(LibraryImports.getErrno());
        return ret;
    }

    public static int open(String name, int flags, int mode) throws IOException {
        return check(LibraryImports.open(name, flags, mode));
    }

    public static int close(int fd) throws IOException {
        return check(LibraryImports.close(fd));
    }

    /** returns -2 for EOF and -1 for would-block */
    public static int readOneByte(int fd, boolean block) throws IOException {
        int res = LibraryImports.readOneByte(fd, block);
        if (res == -2) {
            return -2;
        } else {
            return checkNB(res);
        }
    }

    public static int read(int fd, byte[] array, int offset, int count,
            boolean block) throws IOException {
        return checkNB(LibraryImports.read(fd, array, offset, count, block));
    }

    public static int writeOneByte(int fd, int b, boolean block)
            throws IOException {
        return checkNB(LibraryImports.writeOneByte(fd, b, block));
    }

    public static int write(int fd, byte[] array, int offset, int count,
            boolean block) throws IOException {
        return checkNB(LibraryImports.write(fd, array, offset, count, block));
    }

    public static int OPEN(byte[] name, int flags, int mode) throws IOException {
        return (LibraryImports.OPEN2(flags, mode));
    }

    public static int CLOSE(int fd) throws IOException {
        return (LibraryImports.CLOSE(fd));
    }

    public static int WRITE(int fd, byte[] a, int l) throws IOException {
        return (LibraryGlue.write(fd, a, l));
    }

    public static int IOCTL(int d, int req, int[] arg) throws IOException {
        return check(LibraryGlue.ioctl(d, req, arg));
    }
}
