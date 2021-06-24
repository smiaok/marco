package com.fudan.intellij.plugin.marco.entity;

import lombok.Data;

@Data
public class ReviewTask {

    private long reviewTaskId;

    private long markReviewId;

    private String reviewer;
}
