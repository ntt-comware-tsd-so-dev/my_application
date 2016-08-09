package com.aylanetworks.agilelink;

public class RowPropertyHolder extends DevicePropertyHolder {

    public enum RowType {TOP, BOTTOM}
    public RowType mRowType;

    public RowPropertyHolder(RowType type) {
        mRowType = type;
    }
}
