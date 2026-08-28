package com.example.guidesticks_android;

final class SystemBarPadding {
    private SystemBarPadding() {
    }

    static Values resolve(
            int baseLeft, int baseTop, int baseRight, int baseBottom,
            int insetLeft, int insetTop, int insetRight, int insetBottom) {
        return new Values(
                baseLeft + insetLeft,
                baseTop + insetTop,
                baseRight + insetRight,
                baseBottom + insetBottom);
    }

    static final class Values {
        final int left;
        final int top;
        final int right;
        final int bottom;

        private Values(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
