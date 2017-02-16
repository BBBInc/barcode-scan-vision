package com.bbbtech.barcodescan;

/**
 * Created by Wooseong Kim in BarcodeScan on 2017. 2. 16.
 *
 * CameraNullPointerException
 *  특정 기기에서 권한 등의 문제로 (6.0 미만 디바이스) 초기화하지 못해 camera = null 인 경우가 발생, 예외처리 필요
 */
public class CameraNullPointerException extends Exception {}
