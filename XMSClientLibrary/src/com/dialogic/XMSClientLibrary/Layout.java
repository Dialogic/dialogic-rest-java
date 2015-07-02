/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dialogic.XMSClientLibrary;

/**
 *
 * @author ssatyana
 */
public enum Layout {

    AUTO(0),
    TWO(2),
    FOUR(4),
    SIX(6),
    NINE(9);

    private final int value;

    private Layout(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
