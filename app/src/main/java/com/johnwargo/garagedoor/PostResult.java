package com.johnwargo.garagedoor;

public class PostResult {
    private int mResultCode;
    private String mResultMessage;

    public PostResult(int resCode, String resultMessage) {
        mResultCode = resCode;
        mResultMessage = resultMessage;
    }

    public int getResultCode() {
        return mResultCode;
    }

    public void setResultCode(int resultCode) {
        mResultCode = resultCode;
    }

    public String getResultMessage() {
        return mResultMessage;
    }

    public void setResultMessage(String resultMessage) {
        mResultMessage = resultMessage;
    }
}
