package com.meridianenergy.etrm.batch;

public class BatchRunner {

    public static void main(String[] args) {
        System.out.println(new EodBatchJob().run());
    }
}
