package io.antmedia.filter;

public interface DataSizeCalculatorListener {
    void updateFileStats(String vodId , long dataWritten);

    public void getFileInfo(String vodId);

}
