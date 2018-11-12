package com.bbbtech.barcodescan;

/**
 * Created by levin.yu on 2018. 11. 7..
 */

public enum ElemarkISO {
    ISO_AUTO("auto"),
    ISO_100("100"),
    ISO_200("200"),
    ISO_400("400"),
    ISO_800("800"),
    ISO_1600("1600");

    private final String value;

    ElemarkISO(String v) {
        this.value = v;
    }

    public String getIso() {
        return this.value;
    }
}
